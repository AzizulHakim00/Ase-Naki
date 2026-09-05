package com.azizul.asenaki.monitoring.external;

import com.azizul.asenaki.monitoring.PowerSnapshot;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PowerGridService {

    private static final Logger log = LoggerFactory.getLogger(PowerGridService.class);
    private static final String SOURCE = "Power Grid Bangladesh PLC";
    private static final String INTERMEDIATE_CA =
            "certificates/powergrid-intermediate.pem";
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
            Optional<PowerSnapshot> snapshot = parseLatest(html, LocalDateTime.now());
            snapshot.ifPresent(value -> log.info(
                    "Power Grid Bangladesh refreshed: observedAt={}, demandMw={}, supplyMw={}, loadSheddingMw={}",
                    value.getObservedAt(), value.getDemandMw(),
                    value.getSupplyMw(), value.getLoadSheddingMw()));
            return snapshot;
        } catch (RuntimeException exception) {
            log.warn("Power Grid Bangladesh data refresh failed: {} ({})",
                    exception.getClass().getSimpleName(), safeMessage(exception));
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

    static X509Certificate loadPinnedIntermediateCertificate() {
        try (InputStream input = new ClassPathResource(INTERMEDIATE_CA).getInputStream()) {
            return (X509Certificate) CertificateFactory
                    .getInstance("X.509")
                    .generateCertificate(input);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not load the pinned Power Grid intermediate CA", exception);
        }
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
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .sslContext(createPowerGridSslContext())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(12));
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    private SSLContext createPowerGridSslContext() {
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            trustStore.setCertificateEntry(
                    "powergrid-intermediate", loadPinnedIntermediateCertificate());

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
            return sslContext;
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not configure Power Grid TLS validation", exception);
        }
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "no detail";
        }
        return message.length() > 180 ? message.substring(0, 180) : message;
    }
}
