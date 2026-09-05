package com.azizul.asenaki.monitoring;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azizul.asenaki.monitoring.external.CloudflareRadarService;
import com.azizul.asenaki.monitoring.external.IodaService;
import com.azizul.asenaki.monitoring.external.PowerGridService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MonitoringRefreshServiceTest {

    @Test
    void refreshPersistsSuccessfulPowerAndInternetSnapshots() {
        var powerGrid = mock(PowerGridService.class);
        var cloudflare = mock(CloudflareRadarService.class);
        var ioda = mock(IodaService.class);
        var powerRepository = mock(PowerSnapshotRepository.class);
        var internetRepository = mock(InternetStatusSnapshotRepository.class);
        var aggregation = new MonitoringAggregationService();

        PowerSnapshot power = new PowerSnapshot();
        power.setObservedAt(LocalDateTime.of(2026, 9, 6, 1, 0));
        power.setFetchedAt(LocalDateTime.of(2026, 9, 6, 1, 5));
        power.setDemandMw(15000);
        power.setSupplyMw(14500);
        power.setLoadSheddingMw(500);
        power.setSource("Power Grid Bangladesh PLC");

        when(powerGrid.fetchLatest()).thenReturn(Optional.of(power));
        when(cloudflare.checkBangladesh()).thenReturn(
                new ProviderSignal(MonitoringState.NORMAL, "Cloudflare normal", null));
        when(ioda.checkBangladesh()).thenReturn(
                new ProviderSignal(MonitoringState.NORMAL, "IODA normal", null));

        var service = new MonitoringRefreshService(
                powerGrid, cloudflare, ioda,
                powerRepository, internetRepository, aggregation);

        service.refreshAll();

        verify(powerRepository).save(power);
        verify(internetRepository).save(any(InternetStatusSnapshot.class));
    }

    @Test
    void powerFailureDoesNotOverwritePreviousPowerSnapshot() {
        var powerGrid = mock(PowerGridService.class);
        var cloudflare = mock(CloudflareRadarService.class);
        var ioda = mock(IodaService.class);
        var powerRepository = mock(PowerSnapshotRepository.class);
        var internetRepository = mock(InternetStatusSnapshotRepository.class);

        when(powerGrid.fetchLatest()).thenReturn(Optional.empty());
        when(cloudflare.checkBangladesh()).thenReturn(
                ProviderSignal.unavailable("Cloudflare unavailable"));
        when(ioda.checkBangladesh()).thenReturn(
                new ProviderSignal(MonitoringState.NORMAL, "IODA normal", null));

        var service = new MonitoringRefreshService(
                powerGrid, cloudflare, ioda,
                powerRepository, internetRepository,
                new MonitoringAggregationService());

        service.refreshAll();

        verify(powerRepository, never()).save(any(PowerSnapshot.class));
        verify(internetRepository).save(any(InternetStatusSnapshot.class));
    }
}
