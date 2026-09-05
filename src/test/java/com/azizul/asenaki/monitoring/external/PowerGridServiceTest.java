package com.azizul.asenaki.monitoring.external;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PowerGridServiceTest {

    private final PowerGridService service =
            new PowerGridService("https://example.test/power");

    @Test
    void parsesLatestValidPowerGridRow() {
        String html = """
                <html><body>
                  <table><tbody>
                    <tr>
                      <td>04-09-2026</td>
                      <td>20:00:00 20:00</td>
                      <td>14,952</td>
                      <td>14738</td>
                      <td>214</td>
                      <td>Evening Peak</td>
                    </tr>
                  </tbody></table>
                </body></html>
                """;

        var snapshot = service.parseLatest(
                html, LocalDateTime.of(2026, 9, 6, 1, 0));

        assertThat(snapshot).isPresent();
        assertThat(snapshot.orElseThrow().getDemandMw()).isEqualTo(14952);
        assertThat(snapshot.orElseThrow().getSupplyMw()).isEqualTo(14738);
        assertThat(snapshot.orElseThrow().getLoadSheddingMw()).isEqualTo(214);
        assertThat(snapshot.orElseThrow().getObservedAt())
                .isEqualTo(LocalDateTime.of(2026, 9, 4, 20, 0));
        assertThat(snapshot.orElseThrow().getSource())
                .isEqualTo("Power Grid Bangladesh PLC");
    }

    @Test
    void malformedMarkupDoesNotInventPowerValues() {
        String html = "<html><body><table><tr><td>broken</td></tr></table></body></html>";

        var snapshot = service.parseLatest(
                html, LocalDateTime.of(2026, 9, 6, 1, 0));

        assertThat(snapshot).isEmpty();
    }
}
