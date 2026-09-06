package com.azizul.asenaki.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.azizul.asenaki.report.UtilityType;
import org.junit.jupiter.api.Test;

class UtilityProviderTest {

    @Test
    void broadbandProviderIsTrimmedAndCanonicalizedCaseInsensitively() {
        assertThat(UtilityProvider.normalize(UtilityType.BROADBAND, "  Link3  "))
                .isEqualTo("LINK3");
        assertThat(UtilityProvider.normalize(UtilityType.BROADBAND, "link3"))
                .isEqualTo("LINK3");
        assertThat(UtilityProvider.normalize(UtilityType.BROADBAND, "  "))
                .isNull();
    }

    @Test
    void mobileAliasesNormalizeToSupportedProviders() {
        assertThat(UtilityProvider.normalize(UtilityType.MOBILE_NETWORK, "gp"))
                .isEqualTo("GRAMEENPHONE");
        assertThat(UtilityProvider.normalize(UtilityType.MOBILE_NETWORK, "Robi"))
                .isEqualTo("ROBI");
        assertThat(UtilityProvider.normalize(UtilityType.MOBILE_NETWORK, "Banglalink"))
                .isEqualTo("BANGLALINK");
        assertThat(UtilityProvider.normalize(UtilityType.MOBILE_NETWORK, "Teletalk"))
                .isEqualTo("TELETALK");
    }

    @Test
    void unsupportedMobileProviderIsRejected() {
        assertThatThrownBy(() -> UtilityProvider.normalize(
                UtilityType.MOBILE_NETWORK, "Unknown Mobile"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mobile provider");
    }
}
