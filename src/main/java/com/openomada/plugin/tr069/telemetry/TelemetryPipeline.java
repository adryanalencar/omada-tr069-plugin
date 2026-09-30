package com.openomada.plugin.tr069.telemetry;

import com.openomada.plugin.tr069.cwmp.model.ParameterValueStruct;
import com.openomada.plugin.tr069.device.Tr069ConnectedClient;
import com.openomada.plugin.tr069.device.Tr069Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ingestion and normalization pipeline for runtime TR-069 CPE telemetry metrics:
 * CPU, RAM, Uptime, GPON Optical RX/TX dBm, and WiFi client counts.
 */
@Service
public class TelemetryPipeline {
    private static final Logger log = LoggerFactory.getLogger(TelemetryPipeline.class);

    // In-memory ring buffer of recent telemetry snapshots per device MAC (up to 100 per device)
    private static final int MAX_HISTORY_PER_DEVICE = 100;
    private final Map<String, Deque<Tr069Telemetry>> telemetryHistory = new ConcurrentHashMap<>();

    public Tr069Telemetry process(Tr069Device device, List<ParameterValueStruct> parameters) {
        if (device == null || parameters == null || parameters.isEmpty()) {
            return null;
        }

        Tr069Telemetry telemetry = new Tr069Telemetry(device.getMac());
        telemetry.setTimestamp(System.currentTimeMillis());

        for (ParameterValueStruct p : parameters) {
            String name = p.getName();
            String value = p.getValue() != null ? p.getValue().trim() : "";

            if (name == null || value.isEmpty()) continue;

            try {
                // Uptime
                if (name.endsWith("UpTime") || name.endsWith("Uptime")) {
                    long uptime = Long.parseLong(value);
                    telemetry.setUptimeSeconds(uptime);
                    device.setUptimeSeconds(uptime);
                }
                // Optical Power (RX)
                else if (name.endsWith("RxPower") || name.endsWith("RXPower") || name.contains("OpticalPowerRx")) {
                    double rx = parseOpticalPower(value);
                    telemetry.setRxOpticalPowerDbm(rx);
                    device.setRxOpticalPowerDbm(rx);
                }
                // Optical Power (TX)
                else if (name.endsWith("TxPower") || name.endsWith("TXPower") || name.contains("OpticalPowerTx")) {
                    double tx = parseOpticalPower(value);
                    telemetry.setTxOpticalPowerDbm(tx);
                    device.setTxOpticalPowerDbm(tx);
                }
                // CPU Usage
                else if (name.endsWith("CPUUsage") || name.endsWith("CpuUsage") || name.endsWith("ProcessUsage")) {
                    double cpu = Double.parseDouble(value);
                    telemetry.setCpuUsage(cpu);
                    device.setCpuUsage(cpu);
                }
                // Memory Usage
                else if (name.endsWith("MemoryUsage") || name.endsWith("FreeMemoryPercentage")) {
                    double mem = Double.parseDouble(value);
                    telemetry.setMemoryUsage(mem);
                    device.setMemoryUsage(mem);
                }
                // Connected clients
                else if (name.endsWith("TotalAssociations") || name.endsWith("AssociatedDeviceNumberOfEntries")) {
                    int clients = Integer.parseInt(value);
                    telemetry.setClientCount(clients);
                    device.setClientCount(clients);
                }
                // Traffic counters
                else if (name.endsWith("TotalBytesReceived") || name.endsWith("BytesReceived") || name.endsWith("EthernetBytesReceived")) {
                    try {
                        long rx = Long.parseLong(value);
                        telemetry.setRxBytes(rx);
                    } catch (NumberFormatException ignored) {}
                }
                else if (name.endsWith("TotalBytesSent") || name.endsWith("BytesSent") || name.endsWith("EthernetBytesSent")) {
                    try {
                        long tx = Long.parseLong(value);
                        telemetry.setTxBytes(tx);
                    } catch (NumberFormatException ignored) {}
                }
                // WiFi SSID
                else if (name.endsWith(".SSID")) {
                    device.setWlanSsid(value);
                }
                // WiFi Channel
                else if (name.endsWith(".Channel")) {
                    try {
                        device.setWlanChannel(Integer.parseInt(value));
                    } catch (NumberFormatException ignored) {}
                }
                // WiFi Enable
                else if (name.endsWith(".Enable") && (name.contains("WLAN") || name.contains("WiFi"))) {
                    device.setWlanEnabled("1".equals(value) || "true".equalsIgnoreCase(value));
                }
            } catch (Exception e) {
                log.debug("Could not parse telemetry metric {}='{}': {}", name, value, e.getMessage());
            }
        }

        // Parse connected clients if host or associated device tables present
        parseConnectedClients(device, parameters, telemetry);

        // Store snapshot in history ring buffer
        Deque<Tr069Telemetry> history = telemetryHistory.computeIfAbsent(
                device.getMac().toUpperCase(), k -> new ArrayDeque<>()
        );
        synchronized (history) {
            if (history.size() >= MAX_HISTORY_PER_DEVICE) {
                history.pollFirst();
            }
            history.addLast(telemetry);
        }

        log.debug("Ingested telemetry for {}: Uptime={}s, RxPower={}dBm, Clients={}",
                device.getMac(), telemetry.getUptimeSeconds(), telemetry.getRxOpticalPowerDbm(), telemetry.getClientCount());
        return telemetry;
    }

    public List<Tr069Telemetry> getHistory(String mac) {
        if (mac == null) return Collections.emptyList();
        Deque<Tr069Telemetry> history = telemetryHistory.get(mac.toUpperCase());
        if (history == null) return Collections.emptyList();
        synchronized (history) {
            return new ArrayList<>(history);
        }
    }

    public Tr069Telemetry getLatest(String mac) {
        if (mac == null) return null;
        Deque<Tr069Telemetry> history = telemetryHistory.get(mac.toUpperCase());
        if (history == null) return null;
        synchronized (history) {
            return history.peekLast();
        }
    }

    private double parseOpticalPower(String value) {
        // Handle common formats: "-19.50", "-1950" (tenth or hundredth dBm), "-19.50 dBm"
        String cleaned = value.replaceAll("[^0-9.-]", "");
        double val = Double.parseDouble(cleaned);
        if (val < -100 || val > 100) {
            // E.g. -1950 -> -19.50 dBm or -19500 -> -19.50 dBm
            if (Math.abs(val) > 10000) {
                val = val / 1000.0;
            } else if (Math.abs(val) > 1000) {
                val = val / 100.0;
            } else if (Math.abs(val) > 100) {
                val = val / 10.0;
            }
        }
        return Math.round(val * 100.0) / 100.0;
    }

    private static final Pattern CLIENT_PATH_PATTERN = Pattern.compile("^(.+?\\.(?:Host|AssociatedDevice)\\.\\d+\\.)(.*)$");

    private void parseConnectedClients(Tr069Device device, List<ParameterValueStruct> parameters, Tr069Telemetry telemetry) {
        if (parameters == null || parameters.isEmpty()) return;

        Map<String, Map<String, String>> objectInstances = new LinkedHashMap<>();
        for (ParameterValueStruct p : parameters) {
            String name = p.getName();
            String val = p.getValue();
            if (name == null || val == null) continue;
            Matcher m = CLIENT_PATH_PATTERN.matcher(name);
            if (m.matches()) {
                String prefix = m.group(1);
                String prop = m.group(2);
                objectInstances.computeIfAbsent(prefix, k -> new HashMap<>()).put(prop, val.trim());
            }
        }

        if (objectInstances.isEmpty()) return;

        Map<String, Tr069ConnectedClient> clientsByMac = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, String>> entry : objectInstances.entrySet()) {
            String prefix = entry.getKey();
            Map<String, String> props = entry.getValue();

            String rawMac = props.getOrDefault("MACAddress",
                    props.getOrDefault("AssociatedDeviceMACAddress",
                    props.getOrDefault("PhysAddress",
                    props.getOrDefault("Chaddr", ""))));

            String normMac = normalizeMac(rawMac);
            if (normMac == null || normMac.equals("00-00-00-00-00-00")) continue;

            Tr069ConnectedClient client = clientsByMac.computeIfAbsent(normMac, k -> {
                Tr069ConnectedClient c = new Tr069ConnectedClient();
                c.setMac(k);
                return c;
            });

            String ip = props.getOrDefault("IPAddress", props.getOrDefault("AssociatedDeviceIPAddress", props.get("IPv4Address")));
            if (ip != null && !ip.isEmpty() && !"0.0.0.0".equals(ip)) {
                client.setIp(ip);
            }

            String hostName = props.getOrDefault("HostName", props.get("UserHostName"));
            if (hostName != null && !hostName.isEmpty()) {
                client.setHostName(hostName);
            }

            String activeStr = props.get("Active");
            if (activeStr != null) {
                client.setActive("true".equalsIgnoreCase(activeStr) || "1".equals(activeStr));
            }

            boolean isWireless = prefix.contains("WLAN") || prefix.contains("WiFi") || prefix.contains("AccessPoint")
                    || "802.11".equalsIgnoreCase(props.get("InterfaceType"))
                    || "WiFi".equalsIgnoreCase(props.get("InterfaceType"))
                    || (props.get("InterfaceType") != null && props.get("InterfaceType").toLowerCase().contains("wireless"));
            if (isWireless) {
                client.setWireless(true);
            }

            // Signal / RSSI
            String signal = props.getOrDefault("SignalStrength",
                    props.getOrDefault("RSSI",
                    props.getOrDefault("X_ZTE-COM_RSSI",
                    props.get("X_TP_SignalStrength"))));
            if (signal != null && !signal.isEmpty()) {
                try {
                    client.setSignalStrength(Integer.parseInt(signal.replaceAll("[^0-9.-]", "")));
                } catch (NumberFormatException ignored) {}
            }

            // Traffic
            String rx = props.getOrDefault("BytesReceived", props.getOrDefault("RxBytes", props.get("EthernetBytesReceived")));
            if (rx != null && !rx.isEmpty()) {
                try { client.setRxBytes(Long.parseLong(rx)); } catch (NumberFormatException ignored) {}
            }
            String tx = props.getOrDefault("BytesSent", props.getOrDefault("TxBytes", props.get("EthernetBytesSent")));
            if (tx != null && !tx.isEmpty()) {
                try { client.setTxBytes(Long.parseLong(tx)); } catch (NumberFormatException ignored) {}
            }

            client.setLastSeen(System.currentTimeMillis());
        }

        if (!clientsByMac.isEmpty()) {
            List<Tr069ConnectedClient> clientList = new ArrayList<>(clientsByMac.values());
            device.setConnectedClients(clientList);
            telemetry.setClientCount(clientList.size());
            device.setClientCount(clientList.size());
            log.info("Discovered and parsed {} connected clients for TR-069 CPE {}", clientList.size(), device.getMac());
        }
    }

    private String normalizeMac(String raw) {
        if (raw == null) return null;
        String cleaned = raw.replaceAll("[^a-fA-F0-9]", "").toUpperCase();
        if (cleaned.length() != 12) return null;
        return String.format("%s-%s-%s-%s-%s-%s",
                cleaned.substring(0, 2),
                cleaned.substring(2, 4),
                cleaned.substring(4, 6),
                cleaned.substring(6, 8),
                cleaned.substring(8, 10),
                cleaned.substring(10, 12));
    }
}
