package com.openomada.plugin.tr069.cache;

import com.openomada.plugin.tr069.cwmp.model.ParameterValueStruct;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe parameter cache for TR-069 devices with TTL support.
 * Stores recently fetched parameter values to minimize redundant WAN RPC round-trips.
 */
@Component
public class ParameterCache {

    public static class CachedParam {
        private final String value;
        private final String type;
        private final long timestamp;

        public CachedParam(String value, String type, long timestamp) {
            this.value = value;
            this.type = type != null ? type : "xsd:string";
            this.timestamp = timestamp;
        }

        public String getValue() { return value; }
        public String getType() { return type; }
        public long getTimestamp() { return timestamp; }

        public boolean isExpired(long ttlMs) {
            return (System.currentTimeMillis() - timestamp) > ttlMs;
        }
    }

    private final Map<String, Map<String, CachedParam>> cacheByDevice = new ConcurrentHashMap<>();
    private long defaultTtlMs = 5 * 60 * 1000; // 5 minutes

    public void put(String deviceKey, String parameterName, String value, String type) {
        if (deviceKey == null || parameterName == null) return;
        String dk = deviceKey.toUpperCase();
        cacheByDevice.computeIfAbsent(dk, k -> new ConcurrentHashMap<>())
                .put(parameterName, new CachedParam(value, type, System.currentTimeMillis()));
    }

    public void putAll(String deviceKey, List<ParameterValueStruct> parameters) {
        if (deviceKey == null || parameters == null) return;
        String dk = deviceKey.toUpperCase();
        Map<String, CachedParam> map = cacheByDevice.computeIfAbsent(dk, k -> new ConcurrentHashMap<>());
        long now = System.currentTimeMillis();
        for (ParameterValueStruct p : parameters) {
            if (p != null && p.getName() != null) {
                map.put(p.getName(), new CachedParam(p.getValue(), p.getType(), now));
            }
        }
    }

    public CachedParam get(String deviceKey, String parameterName) {
        if (deviceKey == null || parameterName == null) return null;
        Map<String, CachedParam> map = cacheByDevice.get(deviceKey.toUpperCase());
        if (map == null) return null;
        CachedParam param = map.get(parameterName);
        if (param != null && !param.isExpired(defaultTtlMs)) {
            return param;
        }
        return null;
    }

    public String getValue(String deviceKey, String parameterName) {
        CachedParam p = get(deviceKey, parameterName);
        return p != null ? p.getValue() : null;
    }

    public Map<String, String> getAllValues(String deviceKey) {
        if (deviceKey == null) return Collections.emptyMap();
        Map<String, CachedParam> map = cacheByDevice.get(deviceKey.toUpperCase());
        if (map == null) return Collections.emptyMap();
        Map<String, String> result = new HashMap<>();
        long now = System.currentTimeMillis();
        for (Map.Entry<String, CachedParam> entry : map.entrySet()) {
            if (!entry.getValue().isExpired(defaultTtlMs)) {
                result.put(entry.getKey(), entry.getValue().getValue());
            }
        }
        return result;
    }

    public void clear(String deviceKey) {
        if (deviceKey != null) {
            cacheByDevice.remove(deviceKey.toUpperCase());
        }
    }

    public void clearAll() {
        cacheByDevice.clear();
    }

    public void setDefaultTtlMs(long defaultTtlMs) {
        if (defaultTtlMs > 0) {
            this.defaultTtlMs = defaultTtlMs;
        }
    }
}
