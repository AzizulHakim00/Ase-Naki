package com.azizul.asenaki.incident;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IncidentConfidence {
    LOW("Low confidence"),
    MEDIUM("Medium confidence"),
    HIGH("High confidence");

    private final String label;
}
