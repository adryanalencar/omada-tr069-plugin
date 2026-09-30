package com.openomada.plugin.tr069.api;

import com.openomada.plugin.tr069.cache.ParameterCache;
import com.openomada.plugin.tr069.connectionrequest.ConnectionRequestResult;
import com.openomada.plugin.tr069.connectionrequest.ConnectionRequestService;
import com.openomada.plugin.tr069.device.Tr069Device;
import com.openomada.plugin.tr069.faults.FaultManager;
import com.openomada.plugin.tr069.faults.Tr069FaultRecord;
import com.openomada.plugin.tr069.jobs.JobEngine;
import com.openomada.plugin.tr069.jobs.JobType;
import com.openomada.plugin.tr069.jobs.Tr069Job;
import com.openomada.plugin.tr069.metrics.CwmpMetrics;
import com.openomada.plugin.tr069.repository.Tr069DeviceRepository;
import com.openomada.plugin.tr069.session.CwmpSessionManager;
import com.openomada.plugin.tr069.telemetry.TelemetryPipeline;
import com.openomada.plugin.tr069.telemetry.Tr069Telemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Enterprise REST API for TR-069 management in Omada Controller.
 * Consumed by the Omada React Plugin frontend and external management integrations.
 */
@RestController("tr069ApiController")
@RequestMapping("/api/v2/tr069")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class Tr069ApiController {
    private static final Logger log = LoggerFactory.getLogger(Tr069ApiController.class);

    private final Tr069DeviceRepository deviceRepository;
    private final JobEngine jobEngine;
    private final ConnectionRequestService connectionRequestService;
    private final TelemetryPipeline telemetryPipeline;
    private final FaultManager faultManager;
    private final ParameterCache parameterCache;
    private final CwmpSessionManager sessionManager;
    private final CwmpMetrics metrics;

    public Tr069ApiController(Tr069DeviceRepository deviceRepository,
                              JobEngine jobEngine,
                              ConnectionRequestService connectionRequestService,
                              TelemetryPipeline telemetryPipeline,
                              FaultManager faultManager,
                              ParameterCache parameterCache,
                              CwmpSessionManager sessionManager,
                              CwmpMetrics metrics) {
        this.deviceRepository = deviceRepository;
        this.jobEngine = jobEngine;
        this.connectionRequestService = connectionRequestService;
        this.telemetryPipeline = telemetryPipeline;
        this.faultManager = faultManager;
        this.parameterCache = parameterCache;
        this.sessionManager = sessionManager;
        this.metrics = metrics;
    }

    @GetMapping("/devices")
    public ResponseEntity<Map<String, Object>> listDevices() {
        List<Tr069Device> devices = deviceRepository.findAll();
        return ResponseEntity.ok(Map.of(
                "errorCode", 0,
                "msg", "Success",
                "result", Map.of(
                        "total", devices.size(),
                        "data", devices
                )
        ));
    }

    @GetMapping("/devices/{mac}")
    public ResponseEntity<Map<String, Object>> getDevice(@PathVariable("mac") String mac) {
        Optional<Tr069Device> opt = deviceRepository.findByMac(mac);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "errorCode", -1,
                    "msg", "Device not found: " + mac
            ));
        }
        return ResponseEntity.ok(Map.of(
                "errorCode", 0,
                "msg", "Success",
                "result", opt.get()
        ));
    }

    @GetMapping("/devices/{mac}/parameters")
    public ResponseEntity<Map<String, Object>> getParameters(@PathVariable("mac") String mac) {
        Map<String, String> params = parameterCache.getAllValues(mac);
        return ResponseEntity.ok(Map.of(
                "errorCode", 0,
                "msg", "Success",
                "result", params
        ));
    }

    @PostMapping("/devices/{mac}/parameters")
    public ResponseEntity<Map<String, Object>> setParameters(@PathVariable("mac") String mac,
                                                             @RequestBody Map<String, String> parameters) {
        Optional<Tr069Device> opt = deviceRepository.findByMac(mac);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("errorCode", -1, "msg", "Device not found"));
        }

        Tr069Job job = jobEngine.scheduleJob(mac, JobType.SET_PARAMETERS, parameters);
        triggerWakeupIfPossible(opt.get());

        return ResponseEntity.ok(Map.of(
                "errorCode", 0,
                "msg", "Job scheduled",
                "result", job
        ));
    }

    @PostMapping("/devices/{mac}/reboot")
    public ResponseEntity<Map<String, Object>> rebootDevice(@PathVariable("mac") String mac) {
        Optional<Tr069Device> opt = deviceRepository.findByMac(mac);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("errorCode", -1, "msg", "Device not found"));
        }

        Tr069Job job = jobEngine.scheduleJob(mac, JobType.REBOOT, Map.of());
        triggerWakeupIfPossible(opt.get());

        return ResponseEntity.ok(Map.of(
                "errorCode", 0,
                "msg", "Reboot scheduled",
                "result", job
        ));
    }

    @PostMapping("/devices/{mac}/factory-reset")
    public ResponseEntity<Map<String, Object>> factoryReset(@PathVariable("mac") String mac) {
        Optional<Tr069Device> opt = deviceRepository.findByMac(mac);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("errorCode", -1, "msg", "Device not found"));
        }

        Tr069Job job = jobEngine.scheduleJob(mac, JobType.FACTORY_RESET, Map.of());
        triggerWakeupIfPossible(opt.get());

        return ResponseEntity.ok(Map.of(
                "errorCode", 0,
                "msg", "Factory reset scheduled",
                "result", job
        ));
    }

    @PostMapping("/devices/{mac}/connection-request")
    public ResponseEntity<Map<String, Object>> triggerConnectionRequest(@PathVariable("mac") String mac) {
        Optional<Tr069Device> opt = deviceRepository.findByMac(mac);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("errorCode", -1, "msg", "Device not found"));
        }

        Tr069Device d = opt.get();
        metrics.incrementConnectionRequests();
        ConnectionRequestResult result = connectionRequestService.sendConnectionRequest(
                d.getConnectionRequestUrl(),
                d.getConnectionRequestUsername(),
                d.getConnectionRequestPassword()
        );

        return ResponseEntity.ok(Map.of(
                "errorCode", result.isSuccessful() ? 0 : -1,
                "msg", result.getMessage(),
                "result", result
        ));
    }

    @GetMapping("/devices/{mac}/telemetry")
    public ResponseEntity<Map<String, Object>> getTelemetry(@PathVariable("mac") String mac) {
        List<Tr069Telemetry> history = telemetryPipeline.getHistory(mac);
        Tr069Telemetry latest = telemetryPipeline.getLatest(mac);

        return ResponseEntity.ok(Map.of(
                "errorCode", 0,
                "msg", "Success",
                "result", Map.of(
                        "latest", latest != null ? latest : Map.of(),
                        "history", history
                )
        ));
    }

    @GetMapping("/devices/{mac}/faults")
    public ResponseEntity<Map<String, Object>> getDeviceFaults(@PathVariable("mac") String mac,
                                                               @RequestParam(value = "limit", defaultValue = "20") int limit) {
        List<Tr069FaultRecord> faults = faultManager.getFaultsForDevice(mac, limit);
        return ResponseEntity.ok(Map.of(
                "errorCode", 0,
                "msg", "Success",
                "result", faults
        ));
    }

    @GetMapping("/devices/{mac}/jobs")
    public ResponseEntity<Map<String, Object>> getDeviceJobs(@PathVariable("mac") String mac) {
        List<Tr069Job> jobs = jobEngine.getAllJobsForDevice(mac);
        return ResponseEntity.ok(Map.of(
                "errorCode", 0,
                "msg", "Success",
                "result", jobs
        ));
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        return ResponseEntity.ok(Map.of(
                "errorCode", 0,
                "msg", "Success",
                "result", Map.of(
                        "totalInforms", metrics.getTotalInformsReceived(),
                        "bootstrapEvents", metrics.getTotalBootstrapEvents(),
                        "periodicEvents", metrics.getTotalPeriodicEvents(),
                        "totalRpcSent", metrics.getTotalRpcSent(),
                        "totalRpcSuccess", metrics.getTotalRpcSuccess(),
                        "totalRpcFaults", metrics.getTotalRpcFaults(),
                        "activeSessions", sessionManager.getActiveSessionCount(),
                        "totalDevices", deviceRepository.count()
                )
        ));
    }

    private void triggerWakeupIfPossible(Tr069Device device) {
        if (device.getConnectionRequestUrl() != null && !device.getConnectionRequestUrl().isEmpty()) {
            metrics.incrementConnectionRequests();
            connectionRequestService.sendConnectionRequestAsync(
                    device.getConnectionRequestUrl(),
                    device.getConnectionRequestUsername(),
                    device.getConnectionRequestPassword()
            );
        }
    }
}
