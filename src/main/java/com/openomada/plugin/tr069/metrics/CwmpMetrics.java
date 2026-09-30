package com.openomada.plugin.tr069.metrics;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Enterprise metrics and telemetry counter for TR-069 operations.
 */
@Component
public class CwmpMetrics {
    private final AtomicLong totalInformsReceived = new AtomicLong(0);
    private final AtomicLong totalBootstrapEvents = new AtomicLong(0);
    private final AtomicLong totalPeriodicEvents = new AtomicLong(0);
    private final AtomicLong totalRpcSent = new AtomicLong(0);
    private final AtomicLong totalRpcSuccess = new AtomicLong(0);
    private final AtomicLong totalRpcFaults = new AtomicLong(0);
    private final AtomicLong totalConnectionRequestsSent = new AtomicLong(0);

    public void incrementInforms() { totalInformsReceived.incrementAndGet(); }
    public void incrementBootstrap() { totalBootstrapEvents.incrementAndGet(); }
    public void incrementPeriodic() { totalPeriodicEvents.incrementAndGet(); }
    public void incrementRpcSent() { totalRpcSent.incrementAndGet(); }
    public void incrementRpcSuccess() { totalRpcSuccess.incrementAndGet(); }
    public void incrementRpcFault() { totalRpcFaults.incrementAndGet(); }
    public void incrementConnectionRequests() { totalConnectionRequestsSent.incrementAndGet(); }

    public long getTotalInformsReceived() { return totalInformsReceived.get(); }
    public long getTotalBootstrapEvents() { return totalBootstrapEvents.get(); }
    public long getTotalPeriodicEvents() { return totalPeriodicEvents.get(); }
    public long getTotalRpcSent() { return totalRpcSent.get(); }
    public long getTotalRpcSuccess() { return totalRpcSuccess.get(); }
    public long getTotalRpcFaults() { return totalRpcFaults.get(); }
    public long getTotalConnectionRequestsSent() { return totalConnectionRequestsSent.get(); }
}
