package com.azizul.asenaki.monitoring.external;

import com.azizul.asenaki.monitoring.MonitoringState;
import com.azizul.asenaki.monitoring.ProviderSignal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class IodaService {

    private static final Logger log = LoggerFactory.getLogger(IodaService.class);
    private static final long LOOKBACK_SECONDS = 6 * 60 * 60;

    private final String url;
    private final RestClient restClient;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public IodaService(@Value("${app.monitoring.ioda-url:https://api.ioda.inetintel.cc.gatech.edu/v2/outages/alerts}") String url) {
        this.url = url;
        this.restClient = createRestClient();
    }

    public ProviderSignal checkBangladesh() {
        long until = Instant.now().getEpochSecond();
        long from = until - LOOKBACK_SECONDS;
        URI requestUri = UriComponentsBuilder.fromUriString(url)
                .queryParam("from", from)
                .queryParam("until", until)
                .queryParam("limit", 100)
                .queryParam("relatedTo", "country/BD")
                .build()
                .toUri();

        try {
            String json = restClient.get()
                    .uri(requestUri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
            return parseResponse(json);
        } catch (RuntimeException exception) {
            log.warn("IODA monitoring refresh failed: {}",
                    exception.getClass().getSimpleName());
            return ProviderSignal.unavailable("IODA monitoring is temporarily unavailable.");
        }
    }

    ProviderSignal parseResponse(String json) {
        try {
            JsonNode data = jsonMapper.readTree(json).path("data");
            if (!data.isArray()) {
                return ProviderSignal.unavailable("IODA returned an unexpected response.");
            }

            int criticalCount = 0;
            String affectedNetwork = null;
            for (JsonNode alert : data) {
                if (!"critical".equalsIgnoreCase(alert.path("level").asText())) {
                    continue;
                }
                criticalCount++;
                JsonNode entity = alert.path("entity");
                if (affectedNetwork == null
                        && "asn".equalsIgnoreCase(entity.path("type").asText())) {
                    String name = entity.path("name").asText();
                    if (!name.isBlank()) {
                        affectedNetwork = name;
                    }
                }
            }

            if (criticalCount > 0) {
                return new ProviderSignal(
                        MonitoringState.POSSIBLE_DISRUPTION,
                        "IODA detected " + criticalCount
                                + " recent critical outage signal(s) related to Bangladesh.",
                        affectedNetwork);
            }

            return new ProviderSignal(
                    MonitoringState.NORMAL,
                    "IODA detected no recent critical Bangladesh outage signals.",
                    null);
        } catch (Exception exception) {
            return ProviderSignal.unavailable("IODA returned an unreadable response.");
        }
    }

    private RestClient createRestClient() {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(8));
        return RestClient.builder().requestFactory(requestFactory).build();
    }
}
