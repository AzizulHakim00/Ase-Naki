package com.azizul.asenaki.monitoring;

import org.springframework.stereotype.Service;

@Service
public class MonitoringAggregationService {

    public MonitoringState aggregate(
            ProviderSignal cloudflare,
            ProviderSignal ioda) {
        MonitoringState first = cloudflare.state();
        MonitoringState second = ioda.state();

        if (first == MonitoringState.UNAVAILABLE
                && second == MonitoringState.UNAVAILABLE) {
            return MonitoringState.UNAVAILABLE;
        }

        boolean firstDisruption = isDisruption(first);
        boolean secondDisruption = isDisruption(second);

        if (firstDisruption && secondDisruption) {
            return MonitoringState.LIKELY_DISRUPTION;
        }
        if (firstDisruption || secondDisruption) {
            return MonitoringState.POSSIBLE_DISRUPTION;
        }
        return MonitoringState.NORMAL;
    }

    public String summarize(
            ProviderSignal cloudflare,
            ProviderSignal ioda,
            MonitoringState overall) {
        if (overall == MonitoringState.UNAVAILABLE) {
            return "Automatic internet monitoring is temporarily unavailable.";
        }
        if (overall == MonitoringState.LIKELY_DISRUPTION) {
            return "Cloudflare Radar and IODA both detected recent disruption signals.";
        }
        if (overall == MonitoringState.POSSIBLE_DISRUPTION) {
            return "One monitoring source detected a possible internet disruption.";
        }
        if (cloudflare.state() == MonitoringState.UNAVAILABLE
                || ioda.state() == MonitoringState.UNAVAILABLE) {
            return "No disruption was detected by the available source; one source is unavailable.";
        }
        return "No major internet disruption was detected by Cloudflare Radar or IODA.";
    }

    private boolean isDisruption(MonitoringState state) {
        return state == MonitoringState.POSSIBLE_DISRUPTION
                || state == MonitoringState.LIKELY_DISRUPTION;
    }
}
