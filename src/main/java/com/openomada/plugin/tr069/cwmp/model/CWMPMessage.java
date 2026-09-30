package com.openomada.plugin.tr069.cwmp.model;

import java.io.Serializable;
import java.util.UUID;

public abstract class CWMPMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private boolean holdRequests;
    private Integer sessionTimeout;

    protected CWMPMessage() {
        this.id = UUID.randomUUID().toString();
    }

    protected CWMPMessage(String id) {
        this.id = id != null ? id : UUID.randomUUID().toString();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public boolean isHoldRequests() { return holdRequests; }
    public void setHoldRequests(boolean holdRequests) { this.holdRequests = holdRequests; }

    public Integer getSessionTimeout() { return sessionTimeout; }
    public void setSessionTimeout(Integer sessionTimeout) { this.sessionTimeout = sessionTimeout; }

    public abstract String getRpcName();

    public boolean isResponse() {
        return getRpcName().endsWith("Response");
    }

    public boolean isFault() {
        return "Fault".equals(getRpcName());
    }
}
