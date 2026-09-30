package com.openomada.plugin.tr069.session;

import com.openomada.plugin.tr069.cwmp.model.CWMPMessage;
import com.openomada.plugin.tr069.cwmp.model.DeviceId;
import com.openomada.plugin.tr069.cwmp.model.EventStruct;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents an active TR-069 CWMP HTTP session with a CPE.
 * Manages conversation state, message queues, timeouts, and concurrency.
 */
public class CWMPSession implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String sessionId;
    private final long createdAt;
    private volatile long lastActiveAt;
    private volatile int sessionTimeoutSeconds = 30;

    private volatile CWMPSessionState state = CWMPSessionState.INITIAL;
    private DeviceId deviceId;
    private String mac;
    private String ip;

    private boolean holdRequests = false;
    private int maxEnvelopes = 1;
    private List<EventStruct> events = new ArrayList<>();

    private final Queue<CWMPMessage> pendingAcsRequests = new ConcurrentLinkedQueue<>();
    private volatile CWMPMessage awaitingResponseTo = null;
    private final List<CWMPMessage> completedMessages = Collections.synchronizedList(new ArrayList<>());

    private final transient ReentrantLock sessionLock = new ReentrantLock();

    public CWMPSession(String sessionId) {
        this.sessionId = sessionId != null ? sessionId : UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
        this.lastActiveAt = this.createdAt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getLastActiveAt() {
        return lastActiveAt;
    }

    public void touch() {
        this.lastActiveAt = System.currentTimeMillis();
    }

    public boolean isExpired() {
        long elapsedSeconds = (System.currentTimeMillis() - lastActiveAt) / 1000;
        return elapsedSeconds > sessionTimeoutSeconds;
    }

    public int getSessionTimeoutSeconds() {
        return sessionTimeoutSeconds;
    }

    public void setSessionTimeoutSeconds(int sessionTimeoutSeconds) {
        if (sessionTimeoutSeconds > 0) {
            this.sessionTimeoutSeconds = sessionTimeoutSeconds;
        }
    }

    public CWMPSessionState getState() {
        return state;
    }

    public void setState(CWMPSessionState state) {
        this.state = state;
    }

    public DeviceId getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(DeviceId deviceId) {
        this.deviceId = deviceId;
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

    public boolean isHoldRequests() {
        return holdRequests;
    }

    public void setHoldRequests(boolean holdRequests) {
        this.holdRequests = holdRequests;
    }

    public int getMaxEnvelopes() {
        return maxEnvelopes;
    }

    public void setMaxEnvelopes(int maxEnvelopes) {
        this.maxEnvelopes = maxEnvelopes;
    }

    public List<EventStruct> getEvents() {
        return events;
    }

    public void setEvents(List<EventStruct> events) {
        this.events = events != null ? new ArrayList<>(events) : new ArrayList<>();
    }

    public boolean hasEvent(String eventCodePrefix) {
        if (eventCodePrefix == null || events == null) return false;
        for (EventStruct es : events) {
            if (es.getEventCode() != null && es.getEventCode().startsWith(eventCodePrefix)) {
                return true;
            }
        }
        return false;
    }

    public void enqueueAcsRequest(CWMPMessage request) {
        if (request != null) {
            pendingAcsRequests.offer(request);
        }
    }

    public CWMPMessage pollNextAcsRequest() {
        CWMPMessage next = pendingAcsRequests.poll();
        this.awaitingResponseTo = next;
        return next;
    }

    public boolean hasPendingAcsRequests() {
        return !pendingAcsRequests.isEmpty();
    }

    public CWMPMessage getAwaitingResponseTo() {
        return awaitingResponseTo;
    }

    public void clearAwaitingResponseTo() {
        if (this.awaitingResponseTo != null) {
            this.completedMessages.add(this.awaitingResponseTo);
            this.awaitingResponseTo = null;
        }
    }

    public void recordCompletedMessage(CWMPMessage message) {
        if (message != null) {
            this.completedMessages.add(message);
        }
    }

    public List<CWMPMessage> getCompletedMessages() {
        return new ArrayList<>(completedMessages);
    }

    public ReentrantLock getLock() {
        return sessionLock;
    }

    @Override
    public String toString() {
        return "CWMPSession{" +
                "sessionId='" + sessionId + '\'' +
                ", state=" + state +
                ", mac='" + mac + '\'' +
                ", ip='" + ip + '\'' +
                ", deviceId=" + deviceId +
                ", pendingRequests=" + pendingAcsRequests.size() +
                '}';
    }
}
