package com.openomada.plugin.tr069.device;

import com.openomada.plugin.tr069.datamodel.DataModelType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Domain entity representing a TR-069 CPE adopted and managed within the Omada Controller.
 */
public class Tr069Device implements Serializable {
    private static final long serialVersionUID = 1L;

    private String mac; // Normalized uppercase hyphenated, e.g. "AA-BB-CC-DD-EE-FF"
    private String serialNumber = "";
    private String oui = "";
    private String manufacturer = "";
    private String modelName = "";
    private String hardwareVersion = "";
    private String softwareVersion = "";

    private String ip = "";
    private String connectionRequestUrl = "";
    private String connectionRequestUsername = "";
    private String connectionRequestPassword = "";
    private int periodicInformInterval = 60;

    private DataModelType dataModelType = DataModelType.UNKNOWN;
    private String status = "OFFLINE"; // CONNECTED, ONLINE, OFFLINE

    private long firstSeen = System.currentTimeMillis();
    private long lastSeen = System.currentTimeMillis();
    private long uptimeSeconds = 0;

    // Telemetry & Radio
    private Double rxOpticalPowerDbm;
    private Double txOpticalPowerDbm;
    private Double cpuUsage;
    private Double memoryUsage;
    private int clientCount = 0;

    // WiFi Configuration
    private String wlanSsid = "";
    private int wlanChannel = 0;
    private boolean wlanEnabled = true;

    // Omada Native Binding
    private String siteId = "Default";
    private String omadaDeviceId = "";
    private boolean provisioned = false;
    private List<String> tags = new ArrayList<>();

    public Tr069Device() {}

    public Tr069Device(String mac) {
        this.mac = mac != null ? mac.toUpperCase() : "";
    }

    public String getMac() { return mac; }
    public void setMac(String mac) { this.mac = mac != null ? mac.toUpperCase() : ""; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber != null ? serialNumber : ""; }

    public String getOui() { return oui; }
    public void setOui(String oui) { this.oui = oui != null ? oui : ""; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer != null ? manufacturer : ""; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName != null ? modelName : ""; }

    public String getHardwareVersion() { return hardwareVersion; }
    public void setHardwareVersion(String hardwareVersion) { this.hardwareVersion = hardwareVersion != null ? hardwareVersion : ""; }

    public String getSoftwareVersion() { return softwareVersion; }
    public void setSoftwareVersion(String softwareVersion) { this.softwareVersion = softwareVersion != null ? softwareVersion : ""; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip != null ? ip : ""; }

    public String getConnectionRequestUrl() { return connectionRequestUrl; }
    public void setConnectionRequestUrl(String connectionRequestUrl) { this.connectionRequestUrl = connectionRequestUrl != null ? connectionRequestUrl : ""; }

    public String getConnectionRequestUsername() { return connectionRequestUsername; }
    public void setConnectionRequestUsername(String connectionRequestUsername) { this.connectionRequestUsername = connectionRequestUsername != null ? connectionRequestUsername : ""; }

    public String getConnectionRequestPassword() { return connectionRequestPassword; }
    public void setConnectionRequestPassword(String connectionRequestPassword) { this.connectionRequestPassword = connectionRequestPassword != null ? connectionRequestPassword : ""; }

    public int getPeriodicInformInterval() { return periodicInformInterval; }
    public void setPeriodicInformInterval(int periodicInformInterval) { this.periodicInformInterval = periodicInformInterval; }

    public DataModelType getDataModelType() { return dataModelType; }
    public void setDataModelType(DataModelType dataModelType) { this.dataModelType = dataModelType != null ? dataModelType : DataModelType.UNKNOWN; }

    public String getStatus() {
        long now = System.currentTimeMillis();
        long timeoutMs = Math.max(periodicInformInterval * 2500L, 180_000L); // 2.5x periodic or 3 min
        if (now - lastSeen > timeoutMs) {
            return "OFFLINE";
        }
        return status != null && !status.isEmpty() ? status : "CONNECTED";
    }
    public void setStatus(String status) { this.status = status != null ? status : "OFFLINE"; }

    public int getStatusCode() {
        return "CONNECTED".equalsIgnoreCase(getStatus()) || "ONLINE".equalsIgnoreCase(getStatus()) ? 1 : 0;
    }

    public long getFirstSeen() { return firstSeen; }
    public void setFirstSeen(long firstSeen) { this.firstSeen = firstSeen; }

    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }

    public long getLastInformTime() { return lastSeen; }

    public long getUptimeSeconds() { return uptimeSeconds; }
    public void setUptimeSeconds(long uptimeSeconds) { this.uptimeSeconds = uptimeSeconds; }

    public long getUptime() { return uptimeSeconds; }

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

    public String getWlanSsid() { return wlanSsid; }
    public void setWlanSsid(String wlanSsid) { this.wlanSsid = wlanSsid != null ? wlanSsid : ""; }

    public int getWlanChannel() { return wlanChannel; }
    public void setWlanChannel(int wlanChannel) { this.wlanChannel = wlanChannel; }

    public boolean isWlanEnabled() { return wlanEnabled; }
    public void setWlanEnabled(boolean wlanEnabled) { this.wlanEnabled = wlanEnabled; }

    public String getSiteId() { return siteId; }
    public void setSiteId(String siteId) { this.siteId = siteId != null ? siteId : "Default"; }

    public String getOmadaDeviceId() { return omadaDeviceId; }
    public void setOmadaDeviceId(String omadaDeviceId) { this.omadaDeviceId = omadaDeviceId != null ? omadaDeviceId : ""; }

    public boolean isProvisioned() { return provisioned; }
    public void setProvisioned(boolean provisioned) { this.provisioned = provisioned; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags != null ? tags : new ArrayList<>(); }

    private String omadacId = "";
    private List<Tr069ConnectedClient> connectedClients = new ArrayList<>();

    public String getOmadacId() { return omadacId; }
    public void setOmadacId(String omadacId) { this.omadacId = omadacId != null ? omadacId : ""; }

    public List<Tr069ConnectedClient> getConnectedClients() { return connectedClients; }
    public void setConnectedClients(List<Tr069ConnectedClient> connectedClients) {
        this.connectedClients = connectedClients != null ? connectedClients : new ArrayList<>();
    }

    private Set<String> discoveredMetricPaths = new HashSet<>();

    public Set<String> getDiscoveredMetricPaths() { return discoveredMetricPaths; }
    public void setDiscoveredMetricPaths(Set<String> discoveredMetricPaths) {
        this.discoveredMetricPaths = discoveredMetricPaths != null ? discoveredMetricPaths : new HashSet<>();
    }
    public boolean addDiscoveredMetricPath(String path) {
        if (path == null || path.trim().isEmpty()) return false;
        return this.discoveredMetricPaths.add(path.trim());
    }

    public String getCompositeKey() {
        return oui + "-" + serialNumber;
    }

    @Override
    public String toString() {
        return "Tr069Device{" +
                "mac='" + mac + '\'' +
                ", modelName='" + modelName + '\'' +
                ", manufacturer='" + manufacturer + '\'' +
                ", ip='" + ip + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
