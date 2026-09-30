package com.openomada.plugin.tr069.acs;

import com.openomada.plugin.tr069.cache.ParameterCache;
import com.openomada.plugin.tr069.cwmp.model.*;
import com.openomada.plugin.tr069.cwmp.parser.CWMPXmlParser;
import com.openomada.plugin.tr069.cwmp.rpc.*;
import com.openomada.plugin.tr069.cwmp.serializer.CWMPXmlSerializer;
import com.openomada.plugin.tr069.datamodel.DataModelAdapter;
import com.openomada.plugin.tr069.datamodel.DataModelType;
import com.openomada.plugin.tr069.device.Tr069Device;
import com.openomada.plugin.tr069.faults.FaultManager;
import com.openomada.plugin.tr069.integration.omada.OmadaIntegrationService;
import com.openomada.plugin.tr069.jobs.JobEngine;
import com.openomada.plugin.tr069.metrics.CwmpMetrics;
import com.openomada.plugin.tr069.provisioning.ProvisioningEngine;
import com.openomada.plugin.tr069.repository.Tr069DeviceRepository;
import com.openomada.plugin.tr069.security.CredentialMasker;
import com.openomada.plugin.tr069.security.RateLimiter;
import com.openomada.plugin.tr069.session.CWMPSession;
import com.openomada.plugin.tr069.session.CWMPSessionState;
import com.openomada.plugin.tr069.session.CwmpSessionManager;
import com.openomada.plugin.tr069.telemetry.TelemetryPipeline;
import com.openomada.plugin.tr069.telemetry.Tr069Telemetry;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * Enterprise TR-069 / CWMP ACS Controller embedded directly in Omada's Tomcat server.
 * Handles HTTP/HTTPS SOAP interactions with CPEs:
 * Informs, parameter configuration, diagnostics, reboot, firmware transfers, and session state.
 */
@RestController("cwmpAcsController")
@RequestMapping({"/tr069", "/tr069/", "/tr069/service", "/tr069/acs"})
public class AcsController {
    private static final Logger log = LoggerFactory.getLogger(AcsController.class);
    public static final String SESSION_COOKIE_NAME = "tr069_session";

    private final CWMPXmlParser xmlParser = new CWMPXmlParser();
    private final CWMPXmlSerializer xmlSerializer = new CWMPXmlSerializer();

    private final CwmpSessionManager sessionManager;
    private final Tr069DeviceRepository deviceRepository;
    private final OmadaIntegrationService omadaService;
    private final ProvisioningEngine provisioningEngine;
    private final JobEngine jobEngine;
    private final TelemetryPipeline telemetryPipeline;
    private final FaultManager faultManager;
    private final ParameterCache parameterCache;
    private final RateLimiter rateLimiter;
    private final CwmpMetrics metrics;

    public AcsController(CwmpSessionManager sessionManager,
                         Tr069DeviceRepository deviceRepository,
                         OmadaIntegrationService omadaService,
                         ProvisioningEngine provisioningEngine,
                         JobEngine jobEngine,
                         TelemetryPipeline telemetryPipeline,
                         FaultManager faultManager,
                         ParameterCache parameterCache,
                         RateLimiter rateLimiter,
                         CwmpMetrics metrics) {
        this.sessionManager = sessionManager;
        this.deviceRepository = deviceRepository;
        this.omadaService = omadaService;
        this.provisioningEngine = provisioningEngine;
        this.jobEngine = jobEngine;
        this.telemetryPipeline = telemetryPipeline;
        this.faultManager = faultManager;
        this.parameterCache = parameterCache;
        this.rateLimiter = rateLimiter;
        this.metrics = metrics;
    }

    @GetMapping
    public ResponseEntity<String> handleCwmpGet() {
        return ResponseEntity.ok("OpenOmada TR-069 ACS Endpoint Active. Use HTTP POST with SOAP CWMP XML payload.\n");
    }

    @PostMapping(consumes = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE, "*/*"})
    public ResponseEntity<String> handleCwmpPost(@RequestBody(required = false) String requestBody,
                                                 HttpServletRequest request,
                                                 HttpServletResponse response) {
        String clientIp = getClientIp(request);
        if (!rateLimiter.tryAcquire(clientIp)) {
            log.warn("Rate limited TR-069 request from {}", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        String sessionId = extractSessionId(request);

        // Case 1: Empty HTTP POST from CPE (CPE finished its requests, waiting for ACS requests)
        if (requestBody == null || requestBody.trim().isEmpty()) {
            return handleEmptyCpePost(sessionId, clientIp);
        }

        // Case 2: Non-empty HTTP POST (CPE sending SOAP message)
        try {
            CWMPMessage message = xmlParser.parse(requestBody);
            if (message == null) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }

            log.debug("Received CWMP {} (ID: {}) from {}", message.getRpcName(), message.getId(), clientIp);

            if (message instanceof Inform inform) {
                return handleInform(inform, sessionId, clientIp, response);
            } else if (message instanceof TransferComplete tc) {
                return handleTransferComplete(tc, sessionId, response);
            } else if (message instanceof AutonomousTransferComplete atc) {
                return handleAutonomousTransferComplete(atc, sessionId, response);
            } else if (message instanceof CWMPFault fault) {
                return handleFault(fault, sessionId);
            } else {
                // RPC Response from CPE (e.g. GetParameterValuesResponse, SetParameterValuesResponse, RebootResponse)
                return handleCpeRpcResponse(message, sessionId, clientIp);
            }
        } catch (Exception e) {
            log.error("Error processing TR-069 payload from {}: {}", clientIp, e.getMessage(), e);
            CWMPFault fault = new CWMPFault(UUID.randomUUID().toString(), 9002, "Internal error: " + e.getMessage());
            return buildXmlResponse(fault, sessionId, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ResponseEntity<String> handleInform(Inform inform, String existingSessionId, String clientIp, HttpServletResponse response) {
        metrics.incrementInforms();

        // 1. Establish session
        CWMPSession session = sessionManager.createSession();
        session.setIp(clientIp);
        session.setEvents(inform.getEvents());
        session.setDeviceId(inform.getDeviceId());
        session.setState(CWMPSessionState.INFORM_RECEIVED);

        // Set session cookie
        Cookie cookie = new Cookie(SESSION_COOKIE_NAME, session.getSessionId());
        cookie.setPath("/tr069");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        // 2. Identify or create Tr069Device
        String mac = resolveMacAddress(inform);
        session.setMac(mac);
        sessionManager.bindDevice(session, mac);
        if (inform.getDeviceId() != null) {
            sessionManager.bindDevice(session, inform.getDeviceId().getCompositeKey());
        }

        Tr069Device device = deviceRepository.findByMac(mac).orElseGet(() -> new Tr069Device(mac));
        updateDeviceFromInform(device, inform, clientIp);
        deviceRepository.save(device);

        // 3. Cache parameters & ingest telemetry
        if (inform.getParameterList() != null) {
            parameterCache.putAll(mac, inform.getParameterList());
            Tr069Telemetry telemetry = telemetryPipeline.process(device, inform.getParameterList());
            if (telemetry != null) {
                omadaService.updateTelemetry(device, telemetry);
            }
            if (device.getConnectedClients() != null && !device.getConnectedClients().isEmpty()) {
                omadaService.syncClientsToOmada(device, device.getConnectedClients());
            }
        }

        // 4. Sync device to Omada native MongoDB
        omadaService.syncDeviceToOmada(device);

        // 5. Evaluate Provisioning Rules (Bootstrap, Boot, Periodic, Value Change)
        provisioningEngine.evaluateInform(session, inform, device);

        // 6. Return InformResponse
        InformResponse informResponse = new InformResponse(inform.getId());
        informResponse.setMaxEnvelopes(1);
        session.setState(CWMPSessionState.INFORM_RESPONDED);

        return buildXmlResponse(informResponse, session.getSessionId(), HttpStatus.OK);
    }

    private ResponseEntity<String> handleEmptyCpePost(String sessionId, String clientIp) {
        CWMPSession session = sessionManager.getSession(sessionId);
        if (session == null) {
            log.debug("Empty POST with no active session from {}. Returning 204.", clientIp);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        session.getLock().lock();
        try {
            // Check for pending jobs in JobEngine or session queue
            CWMPMessage nextRpc = session.pollNextAcsRequest();
            if (nextRpc == null && session.getMac() != null) {
                nextRpc = jobEngine.prepareNextRpc(session.getMac(), session);
            }

            if (nextRpc != null) {
                metrics.incrementRpcSent();
                session.setState(CWMPSessionState.ACS_REQUESTING);
                log.info("Sending ACS RPC {} to CPE {} in session {}", nextRpc.getRpcName(), session.getMac(), session.getSessionId());
                return buildXmlResponse(nextRpc, session.getSessionId(), HttpStatus.OK);
            }

            // No more requests to send: End of session
            log.debug("No pending RPCs for CPE {}. Terminating session {}.", session.getMac(), session.getSessionId());
            sessionManager.terminateSession(session.getSessionId());
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } finally {
            session.getLock().unlock();
        }
    }

    private ResponseEntity<String> handleCpeRpcResponse(CWMPMessage response, String sessionId, String clientIp) {
        CWMPSession session = sessionManager.getSession(sessionId);
        String mac = session != null ? session.getMac() : null;

        metrics.incrementRpcSuccess();

        // 1. Process payload if parameter values returned
        if (response instanceof GetParameterValuesResponse gpvr && mac != null) {
            parameterCache.putAll(mac, gpvr.getParameterList());
            deviceRepository.findByMac(mac).ifPresent(d -> {
                Tr069Telemetry telemetry = telemetryPipeline.process(d, gpvr.getParameterList());
                if (telemetry != null) {
                    omadaService.updateTelemetry(d, telemetry);
                }
                if (d.getConnectedClients() != null && !d.getConnectedClients().isEmpty()) {
                    omadaService.syncClientsToOmada(d, d.getConnectedClients());
                }
                deviceRepository.save(d);
            });
        } else if (response instanceof GetParameterNamesResponse gpnr && mac != null) {
            deviceRepository.findByMac(mac).ifPresent(d -> {
                boolean foundNew = false;
                List<String> toQuery = new ArrayList<>();
                if (gpnr.getParameterList() != null) {
                    for (ParameterInfoStruct info : gpnr.getParameterList()) {
                        String name = info.getName();
                        if (name == null || name.endsWith(".")) continue;
                        String lower = name.toLowerCase();
                        if (lower.contains("rxpower") || lower.contains("txpower")
                                || lower.contains("opticalpower") || lower.contains("ponpower")
                                || lower.contains("cpuusage") || lower.contains("processusage")
                                || lower.contains("memoryusage") || lower.contains("freememory")) {
                            if (d.addDiscoveredMetricPath(name)) {
                                foundNew = true;
                                toQuery.add(name);
                            }
                        }
                    }
                }
                if (foundNew) {
                    deviceRepository.save(d);
                    log.info("Discovered {} vendor metric paths for {}: {}", toQuery.size(), mac, toQuery);
                    if (!toQuery.isEmpty() && session != null) {
                        GetParameterValues gpv = new GetParameterValues();
                        gpv.setParameterNames(toQuery);
                        session.enqueueAcsRequest(gpv);
                    }
                }
            });
        }

        // 2. Correlate with JobEngine
        if (mac != null) {
            jobEngine.handleRpcResponse(mac, response);
        }

        if (session != null) {
            session.clearAwaitingResponseTo();
            session.recordCompletedMessage(response);

            // 3. Send next pending RPC if available
            session.getLock().lock();
            try {
                CWMPMessage nextRpc = session.pollNextAcsRequest();
                if (nextRpc == null && mac != null) {
                    nextRpc = jobEngine.prepareNextRpc(mac, session);
                }

                if (nextRpc != null) {
                    metrics.incrementRpcSent();
                    return buildXmlResponse(nextRpc, session.getSessionId(), HttpStatus.OK);
                }

                // All RPCs completed: Terminate session
                sessionManager.terminateSession(session.getSessionId());
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            } finally {
                session.getLock().unlock();
            }
        }

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private ResponseEntity<String> handleFault(CWMPFault fault, String sessionId) {
        metrics.incrementRpcFault();
        CWMPSession session = sessionManager.getSession(sessionId);
        String mac = session != null ? session.getMac() : "UNKNOWN";

        faultManager.recordFault(mac, fault.getFaultCode(), fault.getFaultString(),
                fault.getSetParameterValuesFaults().isEmpty() ? "" : fault.getSetParameterValuesFaults().get(0).getParameterName(),
                "RPC");

        if (session != null && mac != null) {
            jobEngine.handleRpcFault(mac, fault);
            session.clearAwaitingResponseTo();
        }

        // Return empty 204 or continue
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private ResponseEntity<String> handleTransferComplete(TransferComplete tc, String sessionId, HttpServletResponse response) {
        TransferCompleteResponse resp = new TransferCompleteResponse(tc.getId());
        return buildXmlResponse(resp, sessionId, HttpStatus.OK);
    }

    private ResponseEntity<String> handleAutonomousTransferComplete(AutonomousTransferComplete atc, String sessionId, HttpServletResponse response) {
        AutonomousTransferCompleteResponse resp = new AutonomousTransferCompleteResponse(atc.getId());
        return buildXmlResponse(resp, sessionId, HttpStatus.OK);
    }

    private ResponseEntity<String> buildXmlResponse(CWMPMessage message, String sessionId, HttpStatus status) {
        String xml = xmlSerializer.serialize(message);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("text/xml; charset=utf-8"));
        if (sessionId != null) {
            headers.add(HttpHeaders.SET_COOKIE, SESSION_COOKIE_NAME + "=" + sessionId + "; Path=/tr069; HttpOnly");
        }
        return new ResponseEntity<>(xml, headers, status);
    }

    private String resolveMacAddress(Inform inform) {
        // Try locating MAC in Inform parameters first
        if (inform.getParameterList() != null) {
            for (ParameterValueStruct p : inform.getParameterList()) {
                String n = p.getName();
                if (n != null && (n.endsWith(".MACAddress") || n.endsWith(".MacAddress") || n.endsWith(".MACAddressBase"))) {
                    String val = p.getValue();
                    if (val != null && isValidMac(val)) {
                        return normalizeMac(val);
                    }
                }
            }
        }

        // Fallback: Derivation from OUI and SerialNumber
        if (inform.getDeviceId() != null) {
            DeviceId d = inform.getDeviceId();
            String serial = d.getSerialNumber() != null ? d.getSerialNumber().replaceAll("[^0-9A-Fa-f]", "") : "";
            if (serial.length() >= 6 && d.getOui() != null && d.getOui().length() == 6) {
                String full = (d.getOui() + serial.substring(serial.length() - 6)).toUpperCase();
                return normalizeMac(full);
            }
            if (d.getSerialNumber() != null && isValidMac(d.getSerialNumber())) {
                return normalizeMac(d.getSerialNumber());
            }
        }

        return "00-00-00-00-00-00";
    }

    private void updateDeviceFromInform(Tr069Device device, Inform inform, String clientIp) {
        device.setLastSeen(System.currentTimeMillis());
        device.setStatus("CONNECTED");
        device.setIp(clientIp);

        if (inform.getDeviceId() != null) {
            DeviceId d = inform.getDeviceId();
            device.setManufacturer(d.getManufacturer());
            device.setOui(d.getOui());
            device.setModelName(d.getProductClass());
            device.setSerialNumber(d.getSerialNumber());
        }

        if (inform.getParameterList() != null) {
            List<String> names = inform.getParameterList().stream().map(ParameterValueStruct::getName).toList();
            device.setDataModelType(DataModelAdapter.detect(names));

            for (ParameterValueStruct p : inform.getParameterList()) {
                String name = p.getName();
                String val = p.getValue();
                if (name == null || val == null) continue;

                if (name.endsWith("ConnectionRequestURL")) {
                    device.setConnectionRequestUrl(val);
                } else if (name.endsWith("ConnectionRequestUsername")) {
                    device.setConnectionRequestUsername(val);
                } else if (name.endsWith("ConnectionRequestPassword")) {
                    device.setConnectionRequestPassword(val);
                } else if (name.endsWith("SoftwareVersion")) {
                    device.setSoftwareVersion(val);
                } else if (name.endsWith("HardwareVersion")) {
                    device.setHardwareVersion(val);
                } else if (name.endsWith("PeriodicInformInterval")) {
                    try {
                        device.setPeriodicInformInterval(Integer.parseInt(val));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
    }

    private String extractSessionId(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if (SESSION_COOKIE_NAME.equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        return request.getHeader("X-TR069-SessionID");
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isValidMac(String val) {
        String cleaned = val.replaceAll("[^0-9A-Fa-f]", "");
        return cleaned.length() == 12;
    }

    private String normalizeMac(String raw) {
        String cleaned = raw.replaceAll("[^0-9A-Fa-f]", "").toUpperCase();
        if (cleaned.length() != 12) return raw.toUpperCase();
        return String.format("%s-%s-%s-%s-%s-%s",
                cleaned.substring(0, 2), cleaned.substring(2, 4), cleaned.substring(4, 6),
                cleaned.substring(6, 8), cleaned.substring(8, 10), cleaned.substring(10, 12));
    }
}
