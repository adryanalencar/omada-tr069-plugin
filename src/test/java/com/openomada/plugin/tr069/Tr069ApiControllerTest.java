package com.openomada.plugin.tr069;

import com.openomada.plugin.tr069.api.Tr069ApiController;
import com.openomada.plugin.tr069.cache.ParameterCache;
import com.openomada.plugin.tr069.connectionrequest.ConnectionRequestService;
import com.openomada.plugin.tr069.device.Tr069Device;
import com.openomada.plugin.tr069.faults.FaultManager;
import com.openomada.plugin.tr069.jobs.JobEngine;
import com.openomada.plugin.tr069.metrics.CwmpMetrics;
import com.openomada.plugin.tr069.repository.Tr069DeviceRepository;
import com.openomada.plugin.tr069.session.CwmpSessionManager;
import com.openomada.plugin.tr069.telemetry.TelemetryPipeline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class Tr069ApiControllerTest {

    private Tr069ApiController controller;
    private Tr069DeviceRepository deviceRepository;
    private JobEngine jobEngine;

    @BeforeEach
    void setUp() {
        deviceRepository = new Tr069DeviceRepository(null);
        jobEngine = new JobEngine();
        ConnectionRequestService connReqService = new ConnectionRequestService();
        TelemetryPipeline telemetry = new TelemetryPipeline();
        FaultManager faultManager = new FaultManager();
        ParameterCache paramCache = new ParameterCache();
        CwmpSessionManager sessionManager = new CwmpSessionManager();
        CwmpMetrics metrics = new CwmpMetrics();

        controller = new Tr069ApiController(
                deviceRepository,
                jobEngine,
                connReqService,
                telemetry,
                faultManager,
                paramCache,
                sessionManager,
                metrics
        );

        // Prepopulate a device
        Tr069Device device = new Tr069Device("00-25-9E-AA-BB-CC");
        device.setManufacturer("TP-Link");
        device.setModelName("Archer-C6");
        device.setStatus("ONLINE");
        deviceRepository.save(device);
    }

    @Test
    @DisplayName("API: List devices returns populated device")
    void testListDevices() {
        ResponseEntity<Map<String, Object>> response = controller.listDevices();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().get("errorCode"));

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getBody().get("result");
        assertEquals(1, result.get("total"));
    }

    @Test
    @DisplayName("API: Get device by MAC")
    void testGetDevice() {
        ResponseEntity<Map<String, Object>> response = controller.getDevice("00-25-9E-AA-BB-CC");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Tr069Device d = (Tr069Device) response.getBody().get("result");
        assertEquals("TP-Link", d.getManufacturer());
        assertEquals("Archer-C6", d.getModelName());
    }

    @Test
    @DisplayName("API: Reboot device schedules Job")
    void testRebootDevice() {
        ResponseEntity<Map<String, Object>> response = controller.rebootDevice("00-25-9E-AA-BB-CC");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().get("errorCode"));
        assertEquals("Reboot scheduled", response.getBody().get("msg"));

        var jobs = jobEngine.getPendingJobs("00-25-9E-AA-BB-CC");
        assertEquals(1, jobs.size());
        assertEquals(com.openomada.plugin.tr069.jobs.JobType.REBOOT, jobs.get(0).getJobType());
    }

    @Test
    @DisplayName("API: Get Metrics")
    void testMetrics() {
        ResponseEntity<Map<String, Object>> response = controller.getMetrics();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getBody().get("result");
        assertTrue(result.containsKey("totalInforms"));
        assertTrue(result.containsKey("totalDevices"));
        assertEquals(1L, result.get("totalDevices"));
    }
}
