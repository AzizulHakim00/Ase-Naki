package com.azizul.asenaki.monitoring.external;

import com.azizul.asenaki.monitoring.MonitoringState;
import com.azizul.asenaki.monitoring.ProviderSignal;
import java.net.URI;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class CloudflareRadarService {

    private static final Logger log =
            LoggerFactory.getLogger(CloudflareRadarService.class);

    private final String url;
    private final String token;
    private final RestClient restClient;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public CloudflareRadarService(
            @Value("${app.monitoring.cloudflare-url:https://api.cloudflare.com/client/v4/radar/annotations/outages}") String url,
            @Value("${app.monitoring.cloudflare-token:${CLOUDFLARE_API_TOKEN:}}") String token) {
        this.url = url;
        this.token = token == null ? "" : token.trim();
        this.restClient = createRestClient();
    }

    public ProviderSignal checkBangladesh() {
        if (token.isBlank()) {
            return ProviderSignal.unavailable(
                    "Cloudflare Radar token is not configured.");
        }

        URI requestUri = UriComponentsBuilder.fromUriString(url)
                .queryParam("dateRange", "1d")
                .queryParam("limit", 100)
                .queryParam("format", "json")
                .queryParam("location", "BD")
                .build()
                .toUri();

        try {
            String json = restClient.get()
                    .uri(requestUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
            return parseResponse(json);
        } catch (RuntimeException exception) {
            log.warn("Cloudflare Radar refresh failed: {}",
                    exception.getClass().getSimpleName());
            return ProviderSignal.unavailable(
                    "Cloudflare Radar monitoring is temporarily unavailable.");
        }
    }

    ProviderSignal parseResponse(String json) {
        try {
            JsonNode root = jsonMapper.readTree(json);
            if (!root.path("success").asBoolean()) {
                return ProviderSignal.unavailable(
                        "Cloudflare Radar returned an unsuccessful response.");
            }

            JsonNode annotations = root.path("result").path("annotations");
            if (!annotations.isArray()) {
                return ProviderSignal.unavailable(
                        "Cloudflare Radar returned an unexpected response.");
            }

            int activeCount = 0;
            String affectedNetwork = null;
            for (JsonNode annotation : annotations) {
                if (!isBangladesh(annotation) || !isActive(annotation)) {
                    continue;
                }
                activeCount++;
                if (affectedNetwork == null) {
                    affectedNetwork = firstNetworkName(annotation);
                }
            }

            if (activeCount > 0) {
                return new ProviderSignal(
                        MonitoringState.POSSIBLE_DISRUPTION,
                        "Cloudflare Radar reports " + activeCount
                                + " active Bangladesh outage event(s).",
                        affectedNetwork);
            }

            return new ProviderSignal(
                    MonitoringState.NORMAL,
                    "Cloudflare Radar reports no active Bangladesh outage event.",
                    null);
        } catch (Exception exception) {
            return ProviderSignal.unavailable(
                    "Cloudflare Radar returned an unreadable response.");
        }
    }

    private boolean isBangladesh(JsonNode annotation) {
        JsonNode locations = annotation.path("locations");
        if (locations.isArray()) {
            for (JsonNode location : locations) {
                if ("BD".equalsIgnoreCase(location.asText())) {
                    return true;
                }
            }
        }

        JsonNode details = annotation.path("locationsDetails");
        if (details.isArray()) {
            for (JsonNode location : details) {
                if ("BD".equalsIgnoreCase(location.path("code").asText())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isActive(JsonNode annotation) {
        JsonNode endDate = annotation.path("endDate");
        return endDate.isMissingNode() || endDate.isNull()
                || endDate.asText().isBlank();
    }

    private String firstNetworkName(JsonNode annotation) {
        JsonNode details = annotation.path("asnsDetails");
        if (details.isArray() && !details.isEmpty()) {
            String name = details.get(0).path("name").asText();
            if (!name.isBlank()) {
                return name;
            }
        }
        String scope = annotation.path("scope").asText();
        return scope.isBlank() ? null : scope;
    }

    private RestClient createRestClient() {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(8));
        return RestClient.builder().requestFactory(requestFactory).build();
    }
}
