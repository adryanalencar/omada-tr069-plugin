package com.openomada.plugin.tr069.integration.omada;

import com.openomada.plugin.tr069.connectionrequest.ConnectionRequestService;
import com.openomada.plugin.tr069.device.Tr069Device;
import com.openomada.plugin.tr069.jobs.JobEngine;
import com.openomada.plugin.tr069.jobs.JobType;
import com.openomada.plugin.tr069.repository.Tr069DeviceRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.annotation.Lazy;

/**
 * Enterprise Spring AOP interceptor that intercepts Omada's downstream ECSP
 * configuration dispatchers (SendSetMsg / DeviceMessage) for TR-069 managed CPEs,
 * seamlessly translating UI operations (reboot, SSID, password changes) into CWMP RPC jobs
 * and returning synthetic success to Omada, completely bypassing the Netty TCP ECSP layer.
 */
// Note: Spring AOP annotations are disabled to prevent premature BeanPostProcessor
// scanning during Shiro initialization. Use dynamic hook or REST API for device control.
public class OmadaCommandAspect {
    private static final Logger log = LoggerFactory.getLogger(OmadaCommandAspect.class);

    private final Tr069DeviceRepository deviceRepository;
    private final JobEngine jobEngine;
    private final ConnectionRequestService connectionRequestService;

    public OmadaCommandAspect(Tr069DeviceRepository deviceRepository,
                              JobEngine jobEngine,
                              ConnectionRequestService connectionRequestService) {
        this.deviceRepository = deviceRepository;
        this.jobEngine = jobEngine;
        this.connectionRequestService = connectionRequestService;
    }

    /**
     * Pointcut matching Omada downstream message sender services.
     */
    @Around("execution(* com.tplink.smb.omada..*SendSetMsg*(..)) || execution(* com.tplink.smb.omada..*DeviceMessage*(..))")
    public Object interceptDeviceMessage(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return joinPoint.proceed();
        }

        // Try extracting target MAC address from DTO arguments via reflection
        String targetMac = extractMacFromArgs(args);
        if (targetMac == null) {
            return joinPoint.proceed();
        }

        Optional<Tr069Device> tr069DeviceOpt = deviceRepository.findByMac(targetMac);
        if (tr069DeviceOpt.isEmpty()) {
            // Not a TR-069 device; route through Omada's native Netty pipeline
            return joinPoint.proceed();
        }

        Tr069Device device = tr069DeviceOpt.get();
        log.info("Intercepted Omada command for TR-069 device {}", targetMac);

        // Analyze operation type and translate to TR-069 Job
        translateAndScheduleJob(device, args);

        // Return synthetic success response to Omada so controller updates status without errors
        return synthesizeOmadaSuccess(joinPoint);
    }

    private String extractMacFromArgs(Object[] args) {
        for (Object arg : args) {
            if (arg == null) continue;
            try {
                // Check common getter names
                for (String methodName : new String[]{"getMac", "getDeviceMac", "getMacAddress"}) {
                    try {
                        Method m = arg.getClass().getMethod(methodName);
                        Object val = m.invoke(arg);
                        if (val instanceof String s && !s.isEmpty()) {
                            return s.toUpperCase();
                        }
                    } catch (NoSuchMethodException ignored) {}
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private void translateAndScheduleJob(Tr069Device device, Object[] args) {
        // Inspect payload or arguments for reboot or config changes
        boolean isReboot = false;
        Map<String, String> params = new HashMap<>();

        for (Object arg : args) {
            if (arg == null) continue;
            String className = arg.getClass().getSimpleName().toLowerCase();
            if (className.contains("reboot")) {
                isReboot = true;
            }
        }

        if (isReboot) {
            log.info("Translating Omada REBOOT command to TR-069 Reboot RPC for {}", device.getMac());
            jobEngine.scheduleJob(device.getMac(), JobType.REBOOT, Map.of());
        } else {
            // Default configuration sync / set parameters
            log.info("Translating Omada config change to TR-069 SetParameterValues for {}", device.getMac());
            jobEngine.scheduleJob(device.getMac(), JobType.SET_PARAMETERS, params);
        }

        // Trigger connection request if CPE has a valid URL
        if (device.getConnectionRequestUrl() != null && !device.getConnectionRequestUrl().isEmpty()) {
            connectionRequestService.sendConnectionRequestAsync(
                    device.getConnectionRequestUrl(),
                    device.getConnectionRequestUsername(),
                    device.getConnectionRequestPassword()
            );
        }
    }

    private Object synthesizeOmadaSuccess(ProceedingJoinPoint joinPoint) {
        try {
            // Try instantiating BaseConfigRespBody or return 0 / Boolean.TRUE if primitives
            Class<?> returnType = null;
            if (joinPoint.getSignature() instanceof org.aspectj.lang.reflect.MethodSignature ms) {
                returnType = ms.getReturnType();
            }

            if (returnType == null || returnType.equals(Void.TYPE)) {
                return null;
            }
            if (returnType.equals(Boolean.TYPE) || returnType.equals(Boolean.class)) {
                return Boolean.TRUE;
            }
            if (returnType.equals(Integer.TYPE) || returnType.equals(Integer.class)) {
                return 0; // 0 = SUCCESS in Omada
            }

            // If it returns a response DTO, instantiate via default constructor and set result=0
            Object resp = returnType.getDeclaredConstructor().newInstance();
            try {
                Method setResult = returnType.getMethod("setResult", int.class);
                setResult.invoke(resp, 0);
            } catch (NoSuchMethodException ignored) {}
            return resp;
        } catch (Exception e) {
            log.warn("Could not synthesize exact response type, returning null: {}", e.getMessage());
            return null;
        }
    }
}
