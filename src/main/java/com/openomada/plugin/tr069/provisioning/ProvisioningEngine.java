package com.openomada.plugin.tr069.provisioning;

import com.openomada.plugin.tr069.cache.ParameterCache;
import com.openomada.plugin.tr069.cwmp.model.EventStruct;
import com.openomada.plugin.tr069.cwmp.model.ParameterValueStruct;
import com.openomada.plugin.tr069.cwmp.rpc.GetParameterNames;
import com.openomada.plugin.tr069.cwmp.rpc.GetParameterValues;
import com.openomada.plugin.tr069.cwmp.rpc.Inform;
import com.openomada.plugin.tr069.cwmp.rpc.SetParameterValues;
import com.openomada.plugin.tr069.datamodel.DataModelAdapter;
import com.openomada.plugin.tr069.datamodel.DataModelType;
import com.openomada.plugin.tr069.device.Tr069Device;
import com.openomada.plugin.tr069.session.CWMPSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Enterprise Provisioning Engine that inspects TR-069 Inform events
 * (Bootstrap, Boot, Periodic, Value Change) and enqueues automated initializations,
 * parameter synchronizations, and rule-based configurations.
 */
@Service
public class ProvisioningEngine {
    private static final Logger log = LoggerFactory.getLogger(ProvisioningEngine.class);

    private final ParameterCache parameterCache;

    public ProvisioningEngine(ParameterCache parameterCache) {
        this.parameterCache = parameterCache;
    }

    /**
     * Evaluates incoming Inform and enqueues necessary RPCs into the active session.
     */
    public void evaluateInform(CWMPSession session, Inform inform, Tr069Device device) {
        if (session == null || inform == null || device == null) return;

        boolean isBootstrap = session.hasEvent("0 BOOTSTRAP");
        boolean isBoot = session.hasEvent("1 BOOT");
        boolean isPeriodic = session.hasEvent("2 PERIODIC");
        boolean isValueChange = session.hasEvent("4 VALUE CHANGE");

        DataModelType modelType = device.getDataModelType();

        if (isBootstrap) {
            log.info("CPE {} triggered 0 BOOTSTRAP - initiating automated initial provisioning", device.getMac());
            enqueueBootstrapProvisioning(session, device, modelType);
        } else if (isBoot) {
            log.info("CPE {} triggered 1 BOOT - refreshing device status and network settings", device.getMac());
            enqueueStatusRefresh(session, device, modelType);
        } else if (isValueChange) {
            log.info("CPE {} triggered 4 VALUE CHANGE - refreshing changed parameters", device.getMac());
            enqueueStatusRefresh(session, device, modelType);
        } else {
            // Periodic Inform or other heartbeat: refresh status, clients, and telemetry
            log.debug("CPE {} triggered heartbeat/periodic inform - refreshing status and telemetry", device.getMac());
            enqueueStatusRefresh(session, device, modelType);
        }
    }

    private void enqueueBootstrapProvisioning(CWMPSession session, Tr069Device device, DataModelType modelType) {
        // Query WiFi, WAN, and system telemetry configurations
        enqueueStatusRefresh(session, device, modelType);
    }

    private void enqueueStatusRefresh(CWMPSession session, Tr069Device device, DataModelType modelType) {
        GetParameterValues gpv = new GetParameterValues();
        List<String> queryPaths = new ArrayList<>();

        // Standard Device & System Info
        queryPaths.add(DataModelAdapter.getSoftwareVersionPath(modelType));
        queryPaths.add(DataModelAdapter.getHardwareVersionPath(modelType));
        queryPaths.add(DataModelAdapter.getUptimePath(modelType));

        // WiFi Radio & SSID
        queryPaths.add(DataModelAdapter.getWlanSsidPath(modelType, 1, 1));
        queryPaths.add(DataModelAdapter.getWlanEnablePath(modelType, 1, 1));
        queryPaths.add(DataModelAdapter.getWlanChannelPath(modelType, 1));
        queryPaths.add(DataModelAdapter.getWlanAssociatedDevicesCountPath(modelType, 1, 1));

        // Connected Hosts & WiFi Associated Device Subtrees
        queryPaths.add(DataModelAdapter.getHostsPath(modelType));
        queryPaths.add(DataModelAdapter.getWlanAssociatedDevicesPath(modelType, 1));
        queryPaths.add(DataModelAdapter.getWlanAssociatedDevicesPath(modelType, 2));

        // Traffic Counters
        queryPaths.addAll(List.of(DataModelAdapter.getTrafficCounterPaths(modelType)));

        // Add any previously discovered vendor metric paths (optical power, CPU, RAM)
        if (device.getDiscoveredMetricPaths() != null && !device.getDiscoveredMetricPaths().isEmpty()) {
            queryPaths.addAll(device.getDiscoveredMetricPaths());
        }

        gpv.setParameterNames(queryPaths);
        session.enqueueAcsRequest(gpv);

        // If vendor metrics not yet discovered, enqueue parameter discovery queries
        if (device.getDiscoveredMetricPaths() == null || device.getDiscoveredMetricPaths().isEmpty()) {
            String wanObj = (modelType == DataModelType.TR181) ? "Device.Optical." : "InternetGatewayDevice.WANDevice.";
            String devObj = (modelType == DataModelType.TR181) ? "Device.DeviceInfo." : "InternetGatewayDevice.DeviceInfo.";
            session.enqueueAcsRequest(new GetParameterNames(UUID.randomUUID().toString(), wanObj, false));
            session.enqueueAcsRequest(new GetParameterNames(UUID.randomUUID().toString(), devObj, false));
        }
    }
}
