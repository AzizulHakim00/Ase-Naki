package com.azizul.asenaki.incident;

public enum IncidentSignalType {
    SAME_PROBLEM,
    WORKING_FOR_ME,
    STILL_OUT,
    RESTORED;

    public boolean isAffected() {
        return this == SAME_PROBLEM || this == STILL_OUT;
    }

    public boolean isWorking() {
        return this == WORKING_FOR_ME;
    }

    public boolean isRecovery() {
        return this == RESTORED;
    }
}
