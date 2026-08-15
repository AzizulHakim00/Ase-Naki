package com.azizul.asenaki.report;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UtilityType {
    ELECTRICITY("Electricity", "bi-lightning-charge-fill"),
    GAS("Gas", "bi-fire"),
    WATER("Water", "bi-droplet-fill"),
    BROADBAND("Broadband", "bi-wifi"),
    MOBILE_NETWORK("Mobile network", "bi-reception-4");

    private final String label;
    private final String iconClass;
}
