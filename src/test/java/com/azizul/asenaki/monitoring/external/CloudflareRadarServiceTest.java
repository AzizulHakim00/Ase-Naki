package com.azizul.asenaki.monitoring.external;

import static org.assertj.core.api.Assertions.assertThat;

import com.azizul.asenaki.monitoring.MonitoringState;
import org.junit.jupiter.api.Test;

class CloudflareRadarServiceTest {

    @Test
    void missingTokenMakesCloudflareUnavailableWithoutRequestingData() {
        var service = new CloudflareRadarService(
                "https://example.test/cloudflare", "");

        var signal = service.checkBangladesh();

        assertThat(signal.state()).isEqualTo(MonitoringState.UNAVAILABLE);
        assertThat(signal.summary()).containsIgnoringCase("token");
    }

    @Test
    void successfulResponseWithoutActiveBangladeshOutageIsNormal() {
        var service = new CloudflareRadarService(
                "https://example.test/cloudflare", "token");

        var signal = service.parseResponse("""
                {"success":true,"result":{"annotations":[]}}
                """);

        assertThat(signal.state()).isEqualTo(MonitoringState.NORMAL);
    }

    @Test
    void activeBangladeshOutageIsPossibleDisruption() {
        var service = new CloudflareRadarService(
                "https://example.test/cloudflare", "token");
        String json = """
                {
                  "success": true,
                  "result": {
                    "annotations": [
                      {
                        "locations": ["BD"],
                        "endDate": null,
                        "scope": "Network-level outage",
                        "asnsDetails": [
                          {"asn":"58715","name":"Example ISP"}
                        ]
                      }
                    ]
                  }
                }
                """;

        var signal = service.parseResponse(json);

        assertThat(signal.state())
                .isEqualTo(MonitoringState.POSSIBLE_DISRUPTION);
        assertThat(signal.affectedNetwork()).isEqualTo("Example ISP");
    }

    @Test
    void malformedPayloadIsUnavailableRatherThanNormal() {
        var service = new CloudflareRadarService(
                "https://example.test/cloudflare", "token");

        var signal = service.parseResponse("not-json");

        assertThat(signal.state()).isEqualTo(MonitoringState.UNAVAILABLE);
    }
}
