package com.openomada.plugin.tr069.telemetry;

import java.io.Serializable;

/**
 * Snapshot of runtime telemetry metrics collected from a TR-069 device.
 */
public class Tr069Telemetry implements Serializable {
    private static final long serialVersionUID = 1L;

    private String mac;
    private long timestamp = System.currentTimeMillis();
    private long uptimeSeconds;
    private Double rxOpticalPowerDbm;
    private Double txOpticalPowerDbm;
    private Double cpuUsage;
    private Double memoryUsage;
    private int clientCount;
    private long rxBytes;
    private long txBytes;

    public Tr069Telemetry() {}

    public Tr069Telemetry(String mac) {
        this.mac = mac != null ? mac.toUpperCase() : "";
    }

    public String getMac() { return mac; }
    public void setMac(String mac) { this.mac = mac != null ? mac.toUpperCase() : ""; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public long getUptimeSeconds() { return uptimeSeconds; }
    public void setUptimeSeconds(long uptimeSeconds) { this.uptimeSeconds = uptimeSeconds; }

    public Double getRxOpticalPowerDbm() { return rxOpticalPowerDbm; }
    public void setRxOpticalPowerDbm(Double rxOpticalPowerDbm) { this.rxOpticalPowerDbm = rxOpticalPowerDbm; }

    public Double getTxOpticalPowerDbm() { return txOpticalPowerDbm; }
    public void setTxOpticalPowerDbm(Double txOpticalPowerDbm) { this.txOpticalPowerDbm = txOpticalPowerDbm; }

    public Double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(Double cpuUsage) { this.cpuUsage = cpuUsage; }

    public Double getMemoryUsage() { return memoryUsage; }
    public void setMemoryUsage(Double memoryUsage) { this.memoryUsage = memoryUsage; }

    public int getClientCount() { return clientCount; }
    public void setClientCount(int clientCount) { this.clientCount = clientCount; }

    public long getRxBytes() { return rxBytes; }
    public void setRxBytes(long rxBytes) { this.rxBytes = rxBytes; }

    public long getTxBytes() { return txBytes; }
    public void setTxBytes(long txBytes) { this.txBytes = txBytes; }

    @Override
    public String toString() {
        return "Tr069Telemetry{" +
                "mac='" + mac + '\'' +
                ", uptime=" + uptimeSeconds + "s" +
                ", rxPower=" + rxOpticalPowerDbm + "dBm" +
                ", cpu=" + cpuUsage + "%" +
                ", clients=" + clientCount +
                '}';
    }
}
