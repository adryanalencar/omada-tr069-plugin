package com.openomada.plugin.tr069.faults;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Enterprise Fault Management Service for tracking, correlating,
 * and reporting TR-069 protocol and configuration faults.
 */
@Service
public class FaultManager {
    private static final Logger log = LoggerFactory.getLogger(FaultManager.class);

    private static final int MAX_FAULTS_PER_DEVICE = 50;
    private final Map<String, List<Tr069FaultRecord>> faultsByDevice = new ConcurrentHashMap<>();
    private final List<Tr069FaultRecord> globalFaults = new CopyOnWriteArrayList<>();

    public Tr069FaultRecord recordFault(String mac, int faultCode, String faultString, String paramName, String rpcName) {
        String normalizedMac = mac != null ? mac.toUpperCase() : "UNKNOWN";
        Tr069FaultRecord record = new Tr069FaultRecord(normalizedMac, faultCode, faultString, paramName, rpcName);

        log.warn("TR-069 Fault recorded for {}: Code={}, String='{}', Param='{}', RPC='{}'",
                normalizedMac, faultCode, faultString, paramName, rpcName);

        List<Tr069FaultRecord> list = faultsByDevice.computeIfAbsent(normalizedMac, k -> new CopyOnWriteArrayList<>());
        list.add(0, record);
        if (list.size() > MAX_FAULTS_PER_DEVICE) {
            list.remove(list.size() - 1);
        }

        globalFaults.add(0, record);
        if (globalFaults.size() > 500) {
            globalFaults.remove(globalFaults.size() - 1);
        }

        return record;
    }

    public List<Tr069FaultRecord> getFaultsForDevice(String mac, int limit) {
        if (mac == null) return Collections.emptyList();
        List<Tr069FaultRecord> list = faultsByDevice.get(mac.toUpperCase());
        if (list == null) return Collections.emptyList();

        int count = Math.min(limit > 0 ? limit : 20, list.size());
        return new ArrayList<>(list.subList(0, count));
    }

    public List<Tr069FaultRecord> getAllFaults(int limit) {
        int count = Math.min(limit > 0 ? limit : 50, globalFaults.size());
        return new ArrayList<>(globalFaults.subList(0, count));
    }

    public void clearFaults(String mac) {
        if (mac != null) {
            faultsByDevice.remove(mac.toUpperCase());
        }
    }
}
