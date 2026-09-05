package com.azizul.asenaki.monitoring.external;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
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

    @Test
    void bundledPowerGridIntermediateCaHasExpectedFingerprint() throws Exception {
        var certificate = PowerGridService.loadPinnedIntermediateCertificate();
        byte[] fingerprint = MessageDigest.getInstance("SHA-256")
                .digest(certificate.getEncoded());

        assertThat(HexFormat.of().withUpperCase().formatHex(fingerprint))
                .isEqualTo("58F0F756758E93FB0B6B17A36A3850475D68BC0D6C99CBE22A1B18351C89FF1F");
        assertThat(certificate.getSubjectX500Principal().getName())
                .contains("SSL2BUY EMEA RSA Domain Validation Secure Server CA");
    }
}
