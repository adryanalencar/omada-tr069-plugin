package com.openomada.plugin.tr069.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Enterprise thread-safe Session Manager for TR-069 CWMP sessions.
 * Manages active session lookup by SessionID and Device MAC/Key,
 * handles concurrency locking, and enforces automatic background session timeout cleanup.
 * Distinctly named CwmpSessionManager to prevent any collision with Shiro SessionManager.
 */
@Service("cwmpSessionManager")
public class CwmpSessionManager {
    private static final Logger log = LoggerFactory.getLogger(CwmpSessionManager.class);

    private final Map<String, CWMPSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, String> deviceKeyToSessionId = new ConcurrentHashMap<>();

    private final ScheduledExecutorService reaper = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "tr069-session-reaper");
        t.setDaemon(true);
        return t;
    });

    @PostConstruct
    public void start() {
        reaper.scheduleWithFixedDelay(this::cleanupExpiredSessions, 5, 5, TimeUnit.SECONDS);
        log.info("TR-069 CwmpSessionManager started with active session expiration reaper");
    }

    @PreDestroy
    public void stop() {
        reaper.shutdownNow();
        activeSessions.clear();
        deviceKeyToSessionId.clear();
        log.info("TR-069 CwmpSessionManager stopped");
    }

    public CWMPSession createSession() {
        return createSession(null);
    }

    public CWMPSession createSession(String requestedId) {
        CWMPSession session = new CWMPSession(requestedId);
        activeSessions.put(session.getSessionId(), session);
        log.debug("Created new TR-069 session: {}", session.getSessionId());
        return session;
    }

    public CWMPSession getSession(String sessionId) {
        if (sessionId == null) return null;
        CWMPSession session = activeSessions.get(sessionId);
        if (session != null && !session.isExpired()) {
            session.touch();
            return session;
        }
        return null;
    }

    public CWMPSession getSessionByDeviceKey(String deviceKey) {
        if (deviceKey == null) return null;
        String sid = deviceKeyToSessionId.get(deviceKey.toUpperCase());
        if (sid != null) {
            return getSession(sid);
        }
        return null;
    }

    public void bindDevice(CWMPSession session, String deviceKey) {
        if (session == null || deviceKey == null) return;
        String normalized = deviceKey.toUpperCase();
        deviceKeyToSessionId.put(normalized, session.getSessionId());
    }

    public void terminateSession(String sessionId) {
        if (sessionId == null) return;
        CWMPSession session = activeSessions.remove(sessionId);
        if (session != null) {
            session.setState(CWMPSessionState.TERMINATED);
            if (session.getMac() != null) {
                deviceKeyToSessionId.remove(session.getMac().toUpperCase());
            }
            if (session.getDeviceId() != null) {
                deviceKeyToSessionId.remove(session.getDeviceId().getCompositeKey().toUpperCase());
            }
            log.debug("Terminated TR-069 session: {}", sessionId);
        }
    }

    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    public Collection<CWMPSession> getActiveSessions() {
        return activeSessions.values();
    }

    private void cleanupExpiredSessions() {
        try {
            long now = System.currentTimeMillis();
            for (Map.Entry<String, CWMPSession> entry : activeSessions.entrySet()) {
                CWMPSession session = entry.getValue();
                if (session.isExpired()) {
                    session.setState(CWMPSessionState.TIMED_OUT);
                    activeSessions.remove(entry.getKey());
                    if (session.getMac() != null) {
                        deviceKeyToSessionId.remove(session.getMac().toUpperCase());
                    }
                    if (session.getDeviceId() != null) {
                        deviceKeyToSessionId.remove(session.getDeviceId().getCompositeKey().toUpperCase());
                    }
                    log.info("Expired and cleaned up idle TR-069 session {} (idle {}s)",
                            session.getSessionId(), (now - session.getLastActiveAt()) / 1000);
                }
            }
        } catch (Exception e) {
            log.error("Error during session cleanup: {}", e.getMessage(), e);
        }
    }
}
