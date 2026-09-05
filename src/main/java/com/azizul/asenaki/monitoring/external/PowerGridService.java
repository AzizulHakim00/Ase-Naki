package com.azizul.asenaki.monitoring.external;

import com.azizul.asenaki.monitoring.PowerSnapshot;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PowerGridService {

    private static final Logger log = LoggerFactory.getLogger(PowerGridService.class);
    private static final String SOURCE = "Power Grid Bangladesh PLC";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-uuuu");

    private final String url;
    private final RestClient restClient;

    public PowerGridService(@Value("${app.monitoring.power-grid-url:https://erp.powergrid.gov.bd/web/generations/view_demand_supply_loadshed?page=1}") String url) {
        this.url = url;
        this.restClient = createRestClient();
    }

    public Optional<PowerSnapshot> fetchLatest() {
        try {
            String html = restClient.get()
                    .uri(url)
                    .accept(MediaType.TEXT_HTML)
                    .retrieve()
                    .body(String.class);
            return parseLatest(html, LocalDateTime.now());
        } catch (RuntimeException exception) {
            log.warn("Power Grid Bangladesh data refresh failed: {}",
                    exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    Optional<PowerSnapshot> parseLatest(String html, LocalDateTime fetchedAt) {
        if (html == null || html.isBlank()) {
            return Optional.empty();
        }

        for (Element row : Jsoup.parse(html).select("table tr")) {
            var cells = row.select("td");
            if (cells.size() < 5) {
                continue;
            }

            try {
                LocalDate date = LocalDate.parse(cells.get(0).text().trim(), DATE_FORMAT);
                String timeToken = cells.get(1).text().trim().split("\\s+")[0];
                LocalDateTime observedAt = parseObservedAt(date, timeToken);

                PowerSnapshot snapshot = new PowerSnapshot();
                snapshot.setObservedAt(observedAt);
                snapshot.setFetchedAt(fetchedAt);
                snapshot.setDemandMw(parseMw(cells.get(2).text()));
                snapshot.setSupplyMw(parseMw(cells.get(3).text()));
                snapshot.setLoadSheddingMw(parseMw(cells.get(4).text()));
                snapshot.setSource(SOURCE);
                return Optional.of(snapshot);
            } catch (RuntimeException ignored) {
                // Skip header-like or malformed rows and try the next one.
            }
        }

        return Optional.empty();
    }

    private LocalDateTime parseObservedAt(LocalDate date, String timeToken) {
        if (timeToken.startsWith("24:")) {
            return LocalDateTime.of(date.plusDays(1), LocalTime.MIDNIGHT);
        }
        return LocalDateTime.of(date, LocalTime.parse(timeToken));
    }

    private int parseMw(String value) {
        String normalized = value
                .replace(",", "")
                .replace("\u00A0", "")
                .trim();
        return Integer.parseInt(normalized);
    }

    private RestClient createRestClient() {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(8));
        return RestClient.builder().requestFactory(requestFactory).build();
    }
}
