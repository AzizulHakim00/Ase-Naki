package com.azizul.asenaki.incident;

import com.azizul.asenaki.report.UtilityType;
import java.util.Locale;

public final class UtilityProvider {

    private UtilityProvider() {
    }

    public static String normalize(UtilityType utilityType, String rawProvider) {
        if (rawProvider == null || rawProvider.isBlank()) {
            if (utilityType == UtilityType.MOBILE_NETWORK) {
                throw new IllegalArgumentException("Please choose a supported mobile provider");
            }
            return null;
        }

        String normalized = rawProvider.trim()
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);

        if (utilityType == UtilityType.MOBILE_NETWORK) {
            return switch (normalized) {
                case "GP", "GRAMEENPHONE" -> "GRAMEENPHONE";
                case "ROBI" -> "ROBI";
                case "BANGLALINK", "BANGLA LINK" -> "BANGLALINK";
                case "TELETALK", "TELE TALK" -> "TELETALK";
                default -> throw new IllegalArgumentException(
                        "Unsupported mobile provider: " + rawProvider.trim());
            };
        }

        if (utilityType == UtilityType.BROADBAND) {
            return normalized;
        }

        return null;
    }
}
