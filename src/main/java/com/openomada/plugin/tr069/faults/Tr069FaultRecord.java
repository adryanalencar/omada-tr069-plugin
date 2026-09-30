package com.openomada.plugin.tr069.faults;

import java.io.Serializable;
import java.util.UUID;

public class Tr069FaultRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String deviceMac;
    private int faultCode;
    private String faultString;
    private String parameterName;
    private String rpcName;
    private long timestamp = System.currentTimeMillis();
    private String severity = "ERROR";

    public Tr069FaultRecord() {
        this.id = UUID.randomUUID().toString();
    }

    public Tr069FaultRecord(String deviceMac, int faultCode, String faultString, String parameterName, String rpcName) {
        this.id = UUID.randomUUID().toString();
        this.deviceMac = deviceMac != null ? deviceMac.toUpperCase() : "";
        this.faultCode = faultCode;
        this.faultString = faultString != null ? faultString : "";
        this.parameterName = parameterName != null ? parameterName : "";
        this.rpcName = rpcName != null ? rpcName : "";
        this.timestamp = System.currentTimeMillis();
        this.severity = faultCode >= 9005 ? "ERROR" : "WARNING";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDeviceMac() { return deviceMac; }
    public void setDeviceMac(String deviceMac) { this.deviceMac = deviceMac != null ? deviceMac.toUpperCase() : ""; }

    public int getFaultCode() { return faultCode; }
    public void setFaultCode(int faultCode) { this.faultCode = faultCode; }

    public String getFaultString() { return faultString; }
    public void setFaultString(String faultString) { this.faultString = faultString; }

    public String getParameterName() { return parameterName; }
    public void setParameterName(String parameterName) { this.parameterName = parameterName; }

    public String getRpcName() { return rpcName; }
    public void setRpcName(String rpcName) { this.rpcName = rpcName; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    @Override
    public String toString() {
        return "Tr069FaultRecord{" +
                "deviceMac='" + deviceMac + '\'' +
                ", faultCode=" + faultCode +
                ", faultString='" + faultString + '\'' +
                ", rpcName='" + rpcName + '\'' +
                '}';
    }
}
