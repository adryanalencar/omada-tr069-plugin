package com.openomada.plugin.tr069.device;

import java.io.Serializable;

/**
 * Represents a connected client station (host, phone, laptop, IoT)
 * associated with a TR-069 CPE (ONT / Router) via Wi-Fi or Ethernet.
 */
public class Tr069ConnectedClient implements Serializable {
    private static final long serialVersionUID = 1L;

    private String mac;
    private String ip;
    private String hostName;
    private boolean active = true;
    private boolean wireless = true;
    private String ssid;
    private int signalStrength; // RSSI in dBm
    private long rxBytes;
    private long txBytes;
    private long lastSeen = System.currentTimeMillis();

    public Tr069ConnectedClient() {}

    public Tr069ConnectedClient(String mac, String ip, String hostName, boolean wireless) {
        this.mac = mac;
        this.ip = ip;
        this.hostName = hostName;
        this.wireless = wireless;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isWireless() {
        return wireless;
    }

    public void setWireless(boolean wireless) {
        this.wireless = wireless;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public int getSignalStrength() {
        return signalStrength;
    }

    public void setSignalStrength(int signalStrength) {
        this.signalStrength = signalStrength;
    }

    public long getRxBytes() {
        return rxBytes;
    }

    public void setRxBytes(long rxBytes) {
        this.rxBytes = rxBytes;
    }

    public long getTxBytes() {
        return txBytes;
    }

    public void setTxBytes(long txBytes) {
        this.txBytes = txBytes;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    @Override
    public String toString() {
        return "Tr069ConnectedClient{" +
                "mac='" + mac + '\'' +
                ", ip='" + ip + '\'' +
                ", hostName='" + hostName + '\'' +
                ", wireless=" + wireless +
                ", signalStrength=" + signalStrength +
                '}';
    }
}
