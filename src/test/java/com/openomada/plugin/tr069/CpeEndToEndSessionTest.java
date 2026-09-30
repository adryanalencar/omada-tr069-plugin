package com.openomada.plugin.tr069;

import com.openomada.plugin.tr069.acs.AcsController;
import com.openomada.plugin.tr069.cache.ParameterCache;
import com.openomada.plugin.tr069.cwmp.model.CWMPMessage;
import com.openomada.plugin.tr069.cwmp.parser.CWMPXmlParser;
import com.openomada.plugin.tr069.cwmp.rpc.*;
import com.openomada.plugin.tr069.cwmp.serializer.CWMPXmlSerializer;
import com.openomada.plugin.tr069.device.Tr069Device;
import com.openomada.plugin.tr069.faults.FaultManager;
import com.openomada.plugin.tr069.integration.omada.OmadaIntegrationService;
import com.openomada.plugin.tr069.jobs.JobEngine;
import com.openomada.plugin.tr069.metrics.CwmpMetrics;
import com.openomada.plugin.tr069.provisioning.ProvisioningEngine;
import com.openomada.plugin.tr069.repository.Tr069DeviceRepository;
import com.openomada.plugin.tr069.security.RateLimiter;
import com.openomada.plugin.tr069.session.CwmpSessionManager;
import com.openomada.plugin.tr069.simulator.CpeSimulator;
import com.openomada.plugin.tr069.telemetry.TelemetryPipeline;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class CpeEndToEndSessionTest {

    private AcsController acsController;
    private CwmpSessionManager sessionManager;
    private Tr069DeviceRepository deviceRepository;
    private JobEngine jobEngine;
    private TelemetryPipeline telemetryPipeline;
    private CWMPXmlParser parser;
    private CWMPXmlSerializer serializer;

    @BeforeEach
    void setUp() {
        sessionManager = new CwmpSessionManager();
        sessionManager.start();

        deviceRepository = new Tr069DeviceRepository(null); // in-memory fallback
        OmadaIntegrationService omadaService = new OmadaIntegrationService(null);
        ParameterCache parameterCache = new ParameterCache();
        ProvisioningEngine provisioningEngine = new ProvisioningEngine(parameterCache);
        jobEngine = new JobEngine();
        telemetryPipeline = new TelemetryPipeline();
        FaultManager faultManager = new FaultManager();
        RateLimiter rateLimiter = new RateLimiter();
        CwmpMetrics metrics = new CwmpMetrics();

        acsController = new AcsController(
                sessionManager,
                deviceRepository,
                omadaService,
                provisioningEngine,
                jobEngine,
                telemetryPipeline,
                faultManager,
                parameterCache,
                rateLimiter,
                metrics
        );

        parser = new CWMPXmlParser();
        serializer = new CWMPXmlSerializer();
    }

    @Test
    @DisplayName("Complete TR-069 CPE handshake and provisioning lifecycle")
    void testFullCpeSessionLifecycle() throws Exception {
        CpeSimulator cpe = CpeSimulator.defaultGponOnu();
        Inform inform = cpe.buildBootstrapInform();
        String informXml = cpe.serialize(inform);

        // Step 1: CPE sends Inform
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        when(request.getRemoteAddr()).thenReturn(cpe.getIp());

        ResponseEntity<String> informResponse = acsController.handleCwmpPost(informXml, request, response);
        assertEquals(HttpStatus.OK, informResponse.getStatusCode());
        assertNotNull(informResponse.getBody());

        // Parse InformResponse from ACS
        CWMPMessage parsedMsg = parser.parse(informResponse.getBody());
        assertInstanceOf(InformResponse.class, parsedMsg);
        assertEquals(inform.getId(), parsedMsg.getId());

        // Verify device was registered into repository with normalized MAC and optical telemetry
        Optional<Tr069Device> devOpt = deviceRepository.findByMac(cpe.getMac());
        assertTrue(devOpt.isPresent(), "Device should be adopted in repository");
        Tr069Device device = devOpt.get();
        assertEquals("Huawei", device.getManufacturer());
        assertEquals("CONNECTED", device.getStatus());
        assertEquals(-21.20, device.getRxOpticalPowerDbm());
        assertEquals(8, device.getClientCount());

        // Extract session cookie sent to CPE
        String sessionCookieHeader = informResponse.getHeaders().getFirst("Set-Cookie");
        assertNotNull(sessionCookieHeader);
        String sessionId = sessionCookieHeader.split(";")[0].replace("tr069_session=", "").trim();

        // Step 2: CPE sends empty POST with its session cookie (ready to receive ACS requests)
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("tr069_session", sessionId)});
        ResponseEntity<String> acsReq1 = acsController.handleCwmpPost("", request, response);
        assertEquals(HttpStatus.OK, acsReq1.getStatusCode());

        CWMPMessage parsedAcsReq1 = parser.parse(acsReq1.getBody());
        assertNotNull(parsedAcsReq1);
        // ProvisioningEngine enqueued SetParameterValues or GetParameterValues
        assertTrue(parsedAcsReq1 instanceof SetParameterValues || parsedAcsReq1 instanceof GetParameterValues);

        // Step 3: CPE responds with RPC response
        CWMPMessage cpeReply;
        if (parsedAcsReq1 instanceof SetParameterValues) {
            cpeReply = new SetParameterValuesResponse(parsedAcsReq1.getId(), 0);
        } else {
            cpeReply = new GetParameterValuesResponse(parsedAcsReq1.getId());
        }
        String cpeReplyXml = serializer.serialize(cpeReply);

        ResponseEntity<String> acsReq2 = acsController.handleCwmpPost(cpeReplyXml, request, response);
        // Could be next queued request (GetParameterValues) or 204 if all finished
        assertNotNull(acsReq2);
        assertTrue(acsReq2.getStatusCode() == HttpStatus.OK || acsReq2.getStatusCode() == HttpStatus.NO_CONTENT);

        // Step 4: Drain any remaining ACS requests until session terminates with 204 NO_CONTENT
        ResponseEntity<String> finalResp = acsReq2;
        while (finalResp.getStatusCode() == HttpStatus.OK) {
            CWMPMessage pending = parser.parse(finalResp.getBody());
            CWMPMessage reply = (pending instanceof GetParameterNames) ?
                    new GetParameterNamesResponse(pending.getId()) :
                    new GetParameterValuesResponse(pending != null ? pending.getId() : "1");
            finalResp = acsController.handleCwmpPost(serializer.serialize(reply), request, response);
            if (finalResp.getStatusCode() == HttpStatus.OK) {
                finalResp = acsController.handleCwmpPost("", request, response);
            }
        }
        assertEquals(HttpStatus.NO_CONTENT, finalResp.getStatusCode());
    }
}
