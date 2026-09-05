package com.azizul.asenaki.monitoring.external;

import static org.assertj.core.api.Assertions.assertThat;

import com.azizul.asenaki.monitoring.MonitoringState;
import org.junit.jupiter.api.Test;

class IodaServiceTest {

    private final IodaService service =
            new IodaService("https://example.test/ioda");

    @Test
    void successfulResponseWithoutCriticalAlertsIsNormal() {
        var signal = service.parseResponse("{\"data\":[]}");

        assertThat(signal.state()).isEqualTo(MonitoringState.NORMAL);
    }

    @Test
    void criticalBangladeshRelatedAlertIsPossibleDisruption() {
        String json = """
                {
                  "data": [
                    {
                      "datasource": "bgp",
                      "entity": {
                        "code": "BD",
                        "name": "Bangladesh",
                        "type": "country"
                      },
                      "time": 1788640000,
                      "level": "critical",
                      "condition": "below",
                      "value": 10,
                      "historyValue": 100,
                      "method": "median"
                    }
                  ]
                }
                """;

        var signal = service.parseResponse(json);

        assertThat(signal.state())
                .isEqualTo(MonitoringState.POSSIBLE_DISRUPTION);
        assertThat(signal.summary()).containsIgnoringCase("IODA");
    }

    @Test
    void malformedPayloadIsUnavailableRatherThanNormal() {
        var signal = service.parseResponse("not-json");

        assertThat(signal.state()).isEqualTo(MonitoringState.UNAVAILABLE);
    }
}
