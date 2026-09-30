package com.openomada.plugin.tr069.integration.omada;

import com.openomada.plugin.tr069.device.Tr069ConnectedClient;
import com.openomada.plugin.tr069.device.Tr069Device;
import com.openomada.plugin.tr069.telemetry.Tr069Telemetry;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

/**
 * Enterprise Native Integration Service for TP-Link Omada Controller.
 * Injects and synchronizes TR-069 CPEs directly into Omada's native MongoDB
 * 'device', 'connection', and 'deviceomadacrefer' collections, and synchronizes
 * native in-memory DeviceImage state so Omada marks the CPEs as Connected in the Web UI.
 */
@Service
public class OmadaIntegrationService {
    private static final Logger log = LoggerFactory.getLogger(OmadaIntegrationService.class);
    public static final String OMADA_DEVICE_COLLECTION = "device";

    private final MongoTemplate mongoTemplate;

    @Autowired
    public OmadaIntegrationService(@Autowired(required = false) MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
        if (mongoTemplate != null) {
            log.info("OmadaIntegrationService initialized with native MongoTemplate");
        } else {
            log.info("OmadaIntegrationService running in standalone mock mode (no MongoTemplate)");
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncAllOnStartup() {
        if (mongoTemplate == null) return;
        try {
            List<Document> devices = mongoTemplate.findAll(Document.class, "tr069_devices");
            log.info("Performing startup sync for {} TR-069 CPE devices into Omada topology and cache...", devices.size());
            for (Document doc : devices) {
                String mac = doc.getString("mac");
                if (mac == null) continue;
                Tr069Device dev = new Tr069Device();
                dev.setMac(mac);
                dev.setSerialNumber(doc.getString("serialNumber"));
                dev.setManufacturer(doc.getString("manufacturer"));
                dev.setModelName(doc.getString("modelName"));
                dev.setHardwareVersion(doc.getString("hardwareVersion"));
                dev.setSoftwareVersion(doc.getString("softwareVersion"));
                dev.setIp(doc.getString("ip"));
                dev.setSiteId(doc.getString("siteId"));
                Long uptime = doc.getLong("uptimeSeconds");
                if (uptime != null) dev.setUptimeSeconds(uptime);
                Integer clients = doc.getInteger("clientCount");
                if (clients != null) dev.setClientCount(clients);
                Double cpu = doc.getDouble("cpuUsage");
                if (cpu != null) dev.setCpuUsage(cpu);
                Double mem = doc.getDouble("memoryUsage");
                if (mem != null) dev.setMemoryUsage(mem);
                syncDeviceToOmada(dev);
            }
        } catch (Throwable t) {
            log.warn("Startup sync failed: {}", t.getMessage());
        }
    }

    /**
     * Synchronizes a Tr069Device into Omada's native 'device' collection.
     * Ensures Omada controller recognizes the CPE as an active device in its topology.
     */
    public boolean syncDeviceToOmada(Tr069Device device) {
        if (device == null || device.getMac() == null) return false;
        if (mongoTemplate == null) {
            log.debug("Standalone mode: simulated Omada sync for MAC {}", device.getMac());
            return true;
        }

        String mac = device.getMac().toUpperCase();
        try {
            Query query = new Query(Criteria.where("mac").is(mac));
            Document existing = mongoTemplate.findOne(query, Document.class, OMADA_DEVICE_COLLECTION);

            long now = System.currentTimeMillis();
            String displayModel = (device.getModelName() != null && !device.getModelName().isEmpty())
                    ? device.getModelName() + " (TR-069)"
                    : "TR-069 Device";

            String siteId = device.getSiteId();
            String omadacId = "89e1e067b3a71f48e62488cb07f14dc1";

            Document siteDoc = null;
            if (siteId != null && !siteId.equals("Default") && !siteId.isEmpty()) {
                try {
                    org.bson.types.ObjectId objId = new org.bson.types.ObjectId(siteId);
                    siteDoc = mongoTemplate.findOne(new Query(new Criteria().orOperator(
                            Criteria.where("_id").is(siteId),
                            Criteria.where("_id").is(objId)
                    )), Document.class, "site");
                } catch (Exception ignored) {
                    siteDoc = mongoTemplate.findOne(new Query(Criteria.where("_id").is(siteId)), Document.class, "site");
                }
            }
            if (siteDoc == null) {
                siteDoc = mongoTemplate.findOne(new Query(), Document.class, "site");
            }
            if (siteDoc != null) {
                Object sId = siteDoc.get("_id");
                siteId = sId != null ? sId.toString() : siteId;
                Object siteOmadac = siteDoc.get("omadac_id");
                if (siteOmadac != null && !siteOmadac.toString().isEmpty()) {
                    omadacId = siteOmadac.toString();
                }
                device.setSiteId(siteId);
            }

            String fwVer = device.getSoftwareVersion() != null ? device.getSoftwareVersion() : "1.0.0";
            String hwVer = device.getHardwareVersion() != null ? device.getHardwareVersion() : "1.0";
            String sn = device.getSerialNumber() != null ? device.getSerialNumber() : mac;

            if (existing == null) {
                // Device not yet adopted into Omada: create native AP document
                String omadaId = UUID.randomUUID().toString().replace("-", "").substring(0, 24);
                Document doc = new Document();
                doc.put("_id", omadaId);
                doc.put("_class", "ap");
                doc.put("type", "ap");
                doc.put("mac", mac);
                doc.put("site_id", siteId);
                doc.put("omadac_id", omadacId);
                doc.put("controller_id", omadacId);
                doc.put("model", "EAP225"); // Emulated native profile so Omada UI renders AP controls
                doc.put("show_model", displayModel);
                doc.put("compound_model", displayModel);
                doc.put("model_version", "1.0");
                doc.put("name", (device.getManufacturer() != null ? device.getManufacturer() + " " : "") + displayModel);
                doc.put("ip", device.getIp());
                doc.put("status", 1); // 1 = RUNNING / CONNECTED
                doc.put("active", true);
                doc.put("disconnect_time", 0L);
                doc.put("version", fwVer);
                doc.put("firmware_version", fwVer);
                doc.put("hw_version", hwVer);
                doc.put("uptime", device.getUptimeSeconds());
                doc.put("last_seen", now);
                doc.put("compatible", 0);
                doc.put("ecspVersion", "2.3.0");
                doc.put("manager_version", "6.3.0.45");
                doc.put("oemId", "");
                doc.put("hwId", "");
                doc.put("sn", sn);
                doc.put("client_num", device.getClientCount());
                if (device.getCpuUsage() != null) doc.put("cpu_utilization", device.getCpuUsage().intValue());
                if (device.getMemoryUsage() != null) doc.put("mem_utilization", device.getMemoryUsage().intValue());

                mongoTemplate.save(doc, OMADA_DEVICE_COLLECTION);
                device.setOmadaDeviceId(omadaId);
                log.info("Registered new TR-069 CPE {} as native Omada AP (ID: {}) in site {}", mac, omadaId, siteId);
            } else {
                // Update existing device status and telemetry
                Update update = new Update();
                update.set("ip", device.getIp());
                update.set("status", 1); // CONNECTED
                update.set("active", true);
                update.set("disconnect_time", 0L);
                update.set("last_seen", now);
                update.set("uptime", device.getUptimeSeconds());
                update.set("show_model", displayModel);
                update.set("compound_model", displayModel);
                update.set("model_version", "1.0");
                update.set("compatible", 0);
                update.set("ecspVersion", "2.3.0");
                update.set("manager_version", "6.3.0.45");
                update.set("oemId", "");
                update.set("hwId", "");
                update.set("sn", sn);
                if (siteId != null && !siteId.equals("Default")) update.set("site_id", siteId);
                if (omadacId != null) {
                    update.set("omadac_id", omadacId);
                    update.set("controller_id", omadacId);
                }
                update.set("version", fwVer);
                update.set("firmware_version", fwVer);
                update.set("hw_version", hwVer);
                update.set("client_num", device.getClientCount());
                if (device.getCpuUsage() != null) update.set("cpu_utilization", device.getCpuUsage().intValue());
                if (device.getMemoryUsage() != null) update.set("mem_utilization", device.getMemoryUsage().intValue());

                mongoTemplate.updateFirst(query, update, OMADA_DEVICE_COLLECTION);
                Object exId = existing.get("_id");
                device.setOmadaDeviceId(exId != null ? exId.toString() : null);
                log.debug("Synchronized heartbeat and telemetry for TR-069 device {} in Omada DB", mac);
            }

            // Sync Omada connection collection (used by Omada to check online connections)
            try {
                Query connQ = new Query(Criteria.where("mac").is(mac).and("omadac_id").is(omadacId));
                Update connU = new Update()
                        .set("mac", mac)
                        .set("omadac_id", omadacId)
                        .set("dst_ip", "2.24.202.99");
                mongoTemplate.upsert(connQ, connU, "connection");
            } catch (Exception ce) {
                log.debug("Notice updating connection collection: {}", ce.getMessage());
            }

            // Sync Omada deviceomadacrefer collection
            try {
                Query refQ = new Query(Criteria.where("mac").is(mac).and("omadac_id").is(omadacId));
                Update refU = new Update()
                        .set("mac", mac)
                        .set("omadac_id", omadacId)
                        .set("mesh_set", false)
                        .set("realloc_ctl", false)
                        .set("restore_for_msp", false)
                        .set("sn_set", false);
                mongoTemplate.upsert(refQ, refU, "deviceomadacrefer");
            } catch (Exception re) {
                log.debug("Notice updating deviceomadacrefer collection: {}", re.getMessage());
            }

            // Synchronize Omada in-memory DeviceImage cache so the Web UI immediately renders status as CONNECTED
            updateOmadaDeviceImage(mac, siteId, omadacId, device);

            return true;
        } catch (Exception e) {
            log.error("Failed to sync TR-069 device {} with Omada MongoDB: {}", mac, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Updates native Omada in-memory DeviceImage cache via reflection.
     * Ensures Omada's device grid API returns status = CONNECTED with live telemetry.
     */
    private void updateOmadaDeviceImage(String mac, String siteId, String omadacId, Tr069Device device) {
        try {
            Class<?> deviceMacClass = Class.forName("com.tplink.smb.omada.manager.device.domain.model.device.DeviceMac");
            Class<?> deviceTypeEnumClass = Class.forName("com.tplink.smb.omada.common.constant.DeviceTypeEnum");
            Class<?> omadacIdClass = Class.forName("com.tplink.smb.omada.manager.shared.domain.model.OmadacId");
            Class<?> siteIdClass = Class.forName("com.tplink.smb.omada.manager.shared.domain.model.SiteId");
            Class<?> deviceStatusEnumClass = Class.forName("com.tplink.smb.omada.common.constant.DeviceStatusEnum");
            Class<?> wiredApImageClass = Class.forName("com.tplink.smb.omada.manager.device.domain.model.deviceimage.ap.wired.WiredApImage");
            Class<?> wiredApImageStatusClass = Class.forName("com.tplink.smb.omada.manager.device.domain.model.deviceimage.ap.wired.WiredApImageStatus");
            Class<?> deviceImageCacheClass = Class.forName("com.tplink.smb.omada.manager.device.port.cache.device.g");
            Class<?> deviceImageClass = Class.forName("com.tplink.smb.omada.manager.device.domain.model.deviceimage.DeviceImage");

            Object deviceMac = deviceMacClass.getConstructor(String.class).newInstance(mac);
            Method getDeviceTypeMethod = deviceTypeEnumClass.getMethod("a", String.class);
            Object apType = getDeviceTypeMethod.invoke(null, "ap");
            Object omadacIdObj = omadacIdClass.getConstructor(String.class).newInstance(omadacId);
            Object siteIdObj = siteIdClass.getConstructor(String.class).newInstance(siteId);

            Method getInstance = deviceImageCacheClass.getMethod("getInstance");
            Object cacheInstance = getInstance.invoke(null);
            Method findMethod = deviceImageCacheClass.getMethod("findByOmadacIdAndMac", omadacIdClass, deviceMacClass);
            Optional<?> opt = (Optional<?>) findMethod.invoke(cacheInstance, omadacIdObj, deviceMac);

            Object wiredApImage;
            if (opt != null && opt.isPresent()) {
                wiredApImage = opt.get();
            } else {
                @SuppressWarnings("unchecked")
                Object connectedStatus = Enum.valueOf((Class<Enum>) deviceStatusEnumClass, "CONNECTED");
                Constructor<?> ctor = wiredApImageClass.getConstructor(
                        Class.forName("com.tplink.smb.omada.manager.device.domain.model.deviceimage.DeviceImageId"),
                        deviceMacClass,
                        deviceTypeEnumClass,
                        omadacIdClass,
                        siteIdClass,
                        deviceStatusEnumClass
                );
                wiredApImage = ctor.newInstance(null, deviceMac, apType, omadacIdObj, siteIdObj, connectedStatus);
                Object statusObj = wiredApImageStatusClass.getConstructor().newInstance();
                Method setDeviceStatus = wiredApImageClass.getMethod("setDeviceStatus", wiredApImageStatusClass);
                setDeviceStatus.invoke(wiredApImage, statusObj);
            }

            // Update status properties
            Method getDeviceStatus = wiredApImageClass.getMethod("getDeviceStatus");
            Object statusObj = getDeviceStatus.invoke(wiredApImage);
            if (statusObj != null) {
                Class<?> statusClass = statusObj.getClass();
                if (device.getIp() != null) {
                    Method setIp = statusClass.getMethod("setIp", String.class);
                    setIp.invoke(statusObj, device.getIp());
                }
                if (device.getCpuUsage() != null) {
                    Method setCpuUtil = statusClass.getMethod("setCpuUtil", Integer.class);
                    setCpuUtil.invoke(statusObj, device.getCpuUsage().intValue());
                }
                if (device.getMemoryUsage() != null) {
                    Method setMemUtil = statusClass.getMethod("setMemUtil", Integer.class);
                    setMemUtil.invoke(statusObj, device.getMemoryUsage().intValue());
                }
                if (device.getUptimeSeconds() > 0) {
                    Method setUptime = statusClass.getMethod("setUptime", String.class);
                    setUptime.invoke(statusObj, formatUptime(device.getUptimeSeconds()));
                }
                Method setLastSeen = statusClass.getMethod("setLastSeen", long.class);
                setLastSeen.invoke(statusObj, System.currentTimeMillis());
                Method setRefreshTimeout = statusClass.getMethod("setRefreshTimeout", Boolean.class);
                setRefreshTimeout.invoke(statusObj, Boolean.TRUE);
            }

            // Populate ApImageDescription required by toDeviceDTOForGrid
            try {
                Class<?> descClass = Class.forName("com.tplink.smb.omada.manager.device.domain.model.deviceimage.ap.ApImageDescription");
                String modelName = device.getModelName() != null ? device.getModelName() : "TR-069 Device";
                String fwVer = device.getSoftwareVersion() != null ? device.getSoftwareVersion() : "1.0.0";
                String hwVer = device.getHardwareVersion() != null ? device.getHardwareVersion() : "1.0";
                String showModel = modelName;
                String compoundModel = modelName;

                Constructor<?> descCtor = descClass.getConstructor(
                        String.class, String.class, String.class, String.class,
                        String.class, String.class, String.class, String.class,
                        String.class, String.class, String.class, String.class,
                        int.class
                );
                Object descObj = descCtor.newInstance(
                        "1.0",           // ecspVersion
                        modelName,       // model
                        "1.0",           // modelVersion
                        fwVer,           // firmwareVersion
                        hwVer,           // hwVersion
                        "",              // specialModel
                        compoundModel,   // compoundModel
                        "",              // oemId
                        "",              // hwId
                        showModel,       // showModel
                        fwVer,           // version
                        "1.0",           // certifiedVersion
                        0                // customizeRegion
                );

                try {
                    Class<?> ecspEnumClass = Class.forName("com.tplink.smb.omada.device.manage.message.EcspFirstVersionEnum");
                    @SuppressWarnings("unchecked")
                    Object ecspV1 = Enum.valueOf((Class<Enum>) ecspEnumClass, "V1");
                    Method setEcspMethod = descClass.getMethod("setEcspFirstVersionEnum", ecspEnumClass);
                    setEcspMethod.invoke(descObj, ecspV1);
                } catch (Throwable t) {
                    log.trace("setEcspFirstVersionEnum failed: {}", t.getMessage());
                }

                try {
                    Class<?> compEnumClass = Class.forName("com.tplink.smb.omada.common.constant.CompatibleTypeEnum");
                    @SuppressWarnings("unchecked")
                    Object comp = Enum.valueOf((Class<Enum>) compEnumClass, "COMPATIBLE");
                    Method setCompMethod = descClass.getMethod("setCompatible", compEnumClass);
                    setCompMethod.invoke(descObj, comp);
                } catch (Throwable t) {
                    log.trace("setCompatible failed: {}", t.getMessage());
                }

                Method setDescMethod = wiredApImageClass.getMethod("setDescription", descClass);
                setDescMethod.invoke(wiredApImage, descObj);
            } catch (Throwable t) {
                log.warn("Could not set ApImageDescription on WiredApImage for {}: {}", mac, t.getMessage());
            }

            // Populate ApImageCapability required by toDeviceDTOForGrid
            try {
                Class<?> capClass = Class.forName("com.tplink.smb.omada.manager.device.domain.model.deviceimage.ap.ApImageCapability");
                Class<?> builderSuperClass = Class.forName("com.tplink.smb.omada.manager.device.domain.model.deviceimage.ap.ApImageCapability$ApImageCapabilityBuilder");
                Method builderMethod = capClass.getMethod("builder");
                builderMethod.setAccessible(true);
                Object builderObj = builderMethod.invoke(null);

                try {
                    Method modelTypeMethod = builderSuperClass.getMethod("modelType", String.class);
                    modelTypeMethod.setAccessible(true);
                    modelTypeMethod.invoke(builderObj, "ap");
                } catch (Throwable ignored) {}

                Method buildMethod = builderSuperClass.getMethod("build");
                buildMethod.setAccessible(true);
                Object capObj = buildMethod.invoke(builderObj);

                Method setCapMethod = wiredApImageClass.getMethod("setCapability", capClass);
                setCapMethod.setAccessible(true);
                setCapMethod.invoke(wiredApImage, capObj);
            } catch (Throwable t) {
                log.warn("Could not set ApImageCapability on WiredApImage for {}: {}", mac, t.getMessage());
            }

            // Set connected status
            Method setConnectedStatus = wiredApImageClass.getMethod("setConnectedStatus");
            setConnectedStatus.invoke(wiredApImage);

            // Set timeout timestamp (10 minutes in the future, renewed on every inform)
            Method setTimeoutTimestamp = wiredApImageClass.getMethod("setTimeoutTimestamp", long.class);
            setTimeoutTimestamp.invoke(wiredApImage, System.currentTimeMillis() + 600_000L);

            // Set display name, sn, and flags
            String displayName = (device.getModelName() != null ? device.getModelName() : "TR-069") + " (TR-069)";
            Method setName = wiredApImageClass.getMethod("setName", String.class);
            setName.invoke(wiredApImage, displayName);

            try {
                Method setSn = wiredApImageClass.getMethod("setSn", String.class);
                setSn.invoke(wiredApImage, device.getSerialNumber() != null ? device.getSerialNumber() : mac);
            } catch (Throwable ignored) {}

            try {
                Method setNotInDb = wiredApImageClass.getMethod("setNotInDb", Boolean.class);
                setNotInDb.invoke(wiredApImage, Boolean.FALSE);
            } catch (Throwable ignored) {}

            try {
                Method setNotShowInDeviceList = wiredApImageClass.getMethod("setNotShowInDeviceList", Boolean.class);
                setNotShowInDeviceList.invoke(wiredApImage, Boolean.FALSE);
            } catch (Throwable ignored) {}

            // Save to primary cache: g.getInstance().a(wiredApImage)
            Method saveImage = deviceImageCacheClass.getMethod("a", deviceImageClass);
            saveImage.invoke(cacheInstance, wiredApImage);

            // Also update ap cache (a.a().a(apImage))
            try {
                Class<?> apCacheClass = Class.forName("com.tplink.smb.omada.manager.device.port.cache.device.a");
                Method getApCacheInstance = apCacheClass.getMethod("a");
                Object apCacheInstance = getApCacheInstance.invoke(null);
                Method putApImage = apCacheClass.getMethod("a", Class.forName("com.tplink.smb.omada.manager.device.domain.model.deviceimage.ap.ApImage"));
                putApImage.invoke(apCacheInstance, wiredApImage);
            } catch (Throwable t) {
                log.trace("Direct apCache update ignored: {}", t.getMessage());
            }

            log.info("Successfully synchronized Omada in-memory DeviceImage cache for TR-069 CPE {} as CONNECTED", mac);
        } catch (ClassNotFoundException e) {
            log.debug("Omada DeviceImage classes not present on classpath: {}", e.getMessage());
        } catch (Throwable t) {
            log.warn("Could not synchronize native Omada in-memory DeviceImage for {}: {}", mac, t.getMessage());
        }
    }

    private String formatUptime(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%d days %02d:%02d:%02d", days, hours, minutes, secs);
    }

    /**
     * Updates telemetry metrics (CPU, RAM, Clients, Optical Power) in Omada.
     */
    public void updateTelemetry(Tr069Device device, Tr069Telemetry telemetry) {
        if (device == null || telemetry == null || mongoTemplate == null) return;

        try {
            Query query = new Query(Criteria.where("mac").is(device.getMac().toUpperCase()));
            Update update = new Update();
            update.set("last_seen", System.currentTimeMillis());
            update.set("uptime", telemetry.getUptimeSeconds());
            update.set("client_num", telemetry.getClientCount());
            if (telemetry.getCpuUsage() != null) update.set("cpu_utilization", telemetry.getCpuUsage().intValue());
            if (telemetry.getMemoryUsage() != null) update.set("mem_utilization", telemetry.getMemoryUsage().intValue());

            // Custom metadata fields for GPON telemetry in document
            if (telemetry.getRxOpticalPowerDbm() != null) {
                update.set("tr069_rx_power_dbm", telemetry.getRxOpticalPowerDbm());
            }
            if (telemetry.getTxOpticalPowerDbm() != null) {
                update.set("tr069_tx_power_dbm", telemetry.getTxOpticalPowerDbm());
            }

            mongoTemplate.updateFirst(query, update, OMADA_DEVICE_COLLECTION);

            // Record time-series telemetry in omada_data
            recordTimeSeriesStats(device, telemetry);
        } catch (Exception e) {
            log.error("Error updating Omada telemetry for {}: {}", device.getMac(), e.getMessage());
        }
    }

    /**
     * Records 5-minute and hourly time-series statistics (Clients, CPU, Memory, Traffic)
     * in Omada's secondary time-series database ('omada_data').
     * Feeds Omada's historical graphs, performance charts, and network statistics reports.
     */
    public void recordTimeSeriesStats(Tr069Device device, Tr069Telemetry telemetry) {
        if (device == null || telemetry == null || mongoTemplate == null) return;

        try {
            String mac = device.getMac().toUpperCase();
            String siteId = device.getSiteId() != null && !device.getSiteId().equals("Default") ?
                    device.getSiteId() : "6a7d3116e8c53e3f4e298aba";
            String omadacId = device.getOmadacId() != null && !device.getOmadacId().isEmpty() ?
                    device.getOmadacId() : "89e1e067b3a71f48e62488cb07f14dc1";
            String displayName = (device.getModelName() != null ? device.getModelName() : "TR-069") + " (TR-069)";

            long nowMs = System.currentTimeMillis();
            long nowSec = nowMs / 1000L;
            long time5min = (nowSec / 300L) * 300L;
            long timeHour = (nowSec / 3600L) * 3600L;

            ZonedDateTime nowUtc = Instant.ofEpochMilli(nowMs).atZone(ZoneOffset.UTC);
            String yyyymmdd = nowUtc.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            WeekFields weekFields = WeekFields.ISO;
            int weekNum = nowUtc.get(weekFields.weekOfWeekBasedYear());
            int year = nowUtc.get(weekFields.weekBasedYear());
            String yyyywWW = String.format("%04dw%02d", year, weekNum);

            MongoDatabase omadaDataDb = mongoTemplate.getMongoDatabaseFactory().getMongoDatabase("omada_data");
            UpdateOptions upsertOpt = new UpdateOptions().upsert(true);

            // 1. Device Client 5-minute snapshot (deviceclient5min_YYYYMMDD)
            Document dcDoc = new Document();
            dcDoc.put("mac", mac);
            dcDoc.put("time", time5min);
            dcDoc.put("name", displayName);
            dcDoc.put("type", "ap");
            dcDoc.put("site_id", siteId);
            dcDoc.put("omadac_id", omadacId);
            int clientCount = telemetry.getClientCount();
            dcDoc.put("cl", clientCount);
            dcDoc.put("cl2g", clientCount);
            dcDoc.put("cl5g", 0);
            dcDoc.put("cl5g2", 0);
            dcDoc.put("cl6g", 0);
            dcDoc.put("fcn", 0);
            dcDoc.put("scn", 0);
            List<String> clientMacs = new ArrayList<>();
            if (device.getConnectedClients() != null) {
                for (Tr069ConnectedClient c : device.getConnectedClients()) {
                    clientMacs.add(c.getMac());
                }
            }
            dcDoc.put("cl_macs", clientMacs);

            omadaDataDb.getCollection("deviceclient5min_" + yyyymmdd)
                    .updateOne(Filters.and(Filters.eq("mac", mac), Filters.eq("time", time5min)),
                            new Document("$set", dcDoc), upsertOpt);

            // 2. AP Statistics 5-minute snapshot (apstat5min_YYYYMMDD)
            Document ap5m = new Document();
            ap5m.put("mac", mac);
            ap5m.put("time", time5min);
            ap5m.put("site", siteId);
            ap5m.put("omadac_id", omadacId);
            int cpu = telemetry.getCpuUsage() != null ? telemetry.getCpuUsage().intValue() : 0;
            int mem = telemetry.getMemoryUsage() != null ? telemetry.getMemoryUsage().intValue() : 0;
            ap5m.put("cpu", cpu);
            ap5m.put("mem", mem);
            ap5m.put("chu2g", 20);
            ap5m.put("inter2g", 10);
            ap5m.put("interUtil2g", 10);
            ap5m.put("rxUtil2g", 5);
            ap5m.put("txUtil2g", 5);
            ap5m.put("reboot", 0);

            Document t2g = new Document();
            t2g.put("rx", telemetry.getRxBytes());
            t2g.put("tx", telemetry.getTxBytes());
            t2g.put("mrx", 0L);
            t2g.put("mtx", 0L);
            t2g.put("dp", 0L);
            ap5m.put("t2g", t2g);

            omadaDataDb.getCollection("apstat5min_" + yyyymmdd)
                    .updateOne(Filters.and(Filters.eq("mac", mac), Filters.eq("time", time5min)),
                            new Document("$set", ap5m), upsertOpt);

            // 3. AP Statistics Hourly snapshot (apstathourly_YYYYwWW)
            Document aph = new Document();
            aph.put("mac", mac);
            aph.put("time", timeHour);
            aph.put("site", siteId);
            aph.put("omadac_id", omadacId);
            aph.put("cpu", cpu);
            aph.put("mem", mem);
            aph.put("chu2g", 20);
            aph.put("reboot", 0);
            Document t2gH = new Document();
            t2gH.put("rx", telemetry.getRxBytes());
            t2gH.put("tx", telemetry.getTxBytes());
            aph.put("t2g", t2gH);

            omadaDataDb.getCollection("apstathourly_" + yyyywWW)
                    .updateOne(Filters.and(Filters.eq("mac", mac), Filters.eq("time", timeHour)),
                            new Document("$set", aph), upsertOpt);

            log.debug("Recorded time-series stats for {} in omada_data (time5m={}, clients={}, cpu={}, mem={})",
                    mac, time5min, clientCount, cpu, mem);
        } catch (Exception e) {
            log.error("Failed to record time-series stats in omada_data for {}: {}", device.getMac(), e.getMessage());
        }
    }

    /**
     * Synchronizes connected client devices (WiFi stations & LAN hosts)
     * associated with a TR-069 CPE into Omada's native MongoDB 'client' collection.
     * Makes clients visible in Omada's Clients list and device detail tabs.
     */
    public void syncClientsToOmada(Tr069Device device, List<Tr069ConnectedClient> clients) {
        if (device == null || clients == null || clients.isEmpty() || mongoTemplate == null) return;

        String siteId = device.getSiteId() != null && !device.getSiteId().equals("Default") ?
                device.getSiteId() : "6a7d3116e8c53e3f4e298aba";
        String omadacId = device.getOmadacId() != null && !device.getOmadacId().isEmpty() ?
                device.getOmadacId() : "89e1e067b3a71f48e62488cb07f14dc1";
        String devMac = device.getMac().toUpperCase();
        String devName = (device.getModelName() != null ? device.getModelName() : "TR-069") + " (TR-069)";
        long now = System.currentTimeMillis();

        String rawSsid = device.getWlanSsid() != null && !device.getWlanSsid().isEmpty() ?
                device.getWlanSsid() : "Camera_da_Sala";
        String ssidId = lookupSsidId(siteId, rawSsid);
        int channel = device.getWlanChannel() > 0 ? device.getWlanChannel() : 6;

        for (Tr069ConnectedClient client : clients) {
            String clientMac = client.getMac();
            if (clientMac == null || clientMac.isEmpty()) continue;
            clientMac = clientMac.toUpperCase();

            try {
                String name = client.getHostName() != null && !client.getHostName().isEmpty() ?
                        client.getHostName() : clientMac;
                String vendor = deduceVendor(name);
                String clientType = client.isWireless() ? "Mobile" : "PC";

                Document thresholdSetting = new Document();
                thresholdSetting.put("start_time", now / 1000L);
                thresholdSetting.put("max_roaming_log", 100L);
                thresholdSetting.put("current_roaming_log", 0L);
                thresholdSetting.put("max_history", 100L);
                thresholdSetting.put("current_history", 0L);

                Document recognitionParam = new Document();
                recognitionParam.put("recognition_count", 1);

                Query query = new Query(Criteria.where("mac").is(clientMac));
                Update update = new Update();
                update.set("mac", clientMac);
                update.set("conn_dev_mac", devMac);
                update.set("conn_dev_name", devName);
                update.set("conn_dev", "ap");
                update.set("conn_dev_type", "ap");
                update.set("site", siteId);
                update.set("site_id", siteId);
                update.set("omadac_id", omadacId);
                update.set("wireless", client.isWireless());
                if (client.getIp() != null && !client.getIp().isEmpty()) {
                    update.set("ip_addr", client.getIp());
                }
                update.set("hostname", name);
                update.set("name", name);
                update.set("category", clientType);
                update.set("type", clientType);
                update.set("showing_type", clientType);
                update.set("vendor", vendor);
                update.set("showing_vendor", vendor);
                update.set("model", "");
                update.set("showing_model", "");
                update.set("os_name", "");
                update.set("showing_os_name", "");
                update.set("wifi_mode", 4);
                update.set("channel", channel);
                update.set("ssid", rawSsid);
                update.set("ssid_id", ssidId);
                update.set("radio_id", 0);
                update.set("connect_vid", 0);
                update.set("sub_vendor", 0);
                update.set("auth_info", Collections.emptyList());
                update.set("power_save", false);
                update.set("total_du", 60L);
                update.set("total_down", client.getRxBytes());
                update.set("total_up", client.getTxBytes());
                update.set("traffic", client.getRxBytes() + client.getTxBytes());
                update.set("last", now);
                update.set("change_time", now);
                update.set("active", client.isActive());
                update.set("block", false);
                update.set("guest", false);
                update.set("exist_setting", false);
                update.set("lock_to_ap_e", false);
                update.set("rate_limit_e", false);
                update.set("conn_to_wireless_router", false);
                update.set("threshold_setting", thresholdSetting);
                update.set("recognition_param", recognitionParam);

                update.setOnInsert("create", now);

                mongoTemplate.upsert(query, update, "client");

                // Update in-memory ClientImage cache so UI renders clients in grid
                updateOmadaClientImage(omadacId, siteId, devMac, devName, device, client, rawSsid, ssidId);

                log.debug("Synchronized client {} (dev: {}) to Omada MongoDB and ClientImage cache", clientMac, devMac);
            } catch (Exception e) {
                log.error("Failed to sync client {} to Omada: {}", clientMac, e.getMessage());
            }
        }
        log.info("Synchronized {} connected clients for TR-069 CPE {} into Omada client inventory and in-memory cache", clients.size(), devMac);
    }

    /**
     * Updates native Omada in-memory ClientImage cache via reflection.
     * Ensures Omada's client grid API (GET /{omadacId}/api/v2/sites/{siteId}/clients)
     * returns the connected clients in real-time.
     */
    private void updateOmadaClientImage(String omadacId, String siteId, String devMac, String devName,
                                        Tr069Device device, Tr069ConnectedClient client, String ssid, String ssidId) {
        if (client == null || client.getMac() == null) return;
        String clientMac = client.getMac().toUpperCase();

        try {
            Class<?> rClass = Class.forName("com.tplink.smb.omada.client.common.domain.model.shared.r");
            Class<?> zClass = Class.forName("com.tplink.smb.omada.client.common.domain.model.shared.z");
            Class<?> fClass = Class.forName("com.tplink.smb.omada.client.domain.model.clientimage.f");
            Class<?> clientImageClass = Class.forName("com.tplink.smb.omada.client.domain.model.clientimage.a");
            Class<?> trafficClass = Class.forName("com.tplink.smb.omada.client.domain.model.clientimage.l");
            Class<?> connectTypeClass = Class.forName("com.tplink.smb.omada.client.domain.model.clientimage.c");
            Class<?> cacheMemClass = Class.forName("com.tplink.smb.omada.client.port.cache.clientimage.a");

            Method getMapMethod = cacheMemClass.getMethod("a", String.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> cacheMap = (Map<String, Object>) getMapMethod.invoke(null, siteId);

            // If client is disconnected/inactive, remove from memory cache
            if (!client.isActive()) {
                if (cacheMap != null) {
                    cacheMap.remove(clientMac);
                }
                return;
            }

            Object rObj = rClass.getConstructor(String.class).newInstance(omadacId);
            Object zObj = zClass.getConstructor(String.class).newInstance(siteId);
            Object fObj = fClass.getConstructor(rClass, zClass, String.class).newInstance(rObj, zObj, clientMac);

            // Build ClientImage using a.k()
            Method builderMethod = clientImageClass.getMethod("k");
            Object builder = builderMethod.invoke(null);
            Class<?> bClass = builder.getClass();

            // Identity
            invokeMethod(builder, "a", fClass, fObj);
            invokeMethod(builder, "j", String.class, clientMac);
            invokeMethod(builder, "a", rClass, rObj);
            invokeMethod(builder, "a", zClass, zObj);
            invokeMethod(builder, "k", String.class, omadacId);
            invokeMethod(builder, "l", String.class, siteId);

            // Names & IP
            String hostName = client.getHostName() != null && !client.getHostName().isEmpty() ?
                    client.getHostName() : clientMac;
            invokeMethod(builder, "m", String.class, hostName);
            invokeMethod(builder, "n", String.class, hostName);
            if (client.getIp() != null && !client.getIp().isEmpty()) {
                invokeMethod(builder, "o", String.class, client.getIp());
            }

            // Traffic
            Object trafficObj = trafficClass.getConstructor().newInstance();
            invokeMethod(builder, "a", trafficClass, trafficObj);
            invokeMethod(builder, "h", Long.class, client.getRxBytes());
            invokeMethod(builder, "i", Long.class, client.getTxBytes());

            // Timing and stats
            long now = System.currentTimeMillis();
            long onlineTime = now - 60_000L;
            invokeMethod(builder, "g", Long.class, 60L);
            invokeMethod(builder, "p", Long.class, onlineTime);
            invokeMethod(builder, "o", Long.class, now);
            invokeMethod(builder, "n", Long.class, onlineTime); // firstSeen (resolves NullPointerException on aZ)
            invokeMethod(builder, "l", Long.class, now);        // activity
            invokeMethod(builder, "m", Long.class, now);        // uploadActivity
            invokeMethod(builder, "j", Long.class, 100L);       // downPacket
            invokeMethod(builder, "k", Long.class, 100L);       // upPacket
            invokeMethod(builder, "q", Long.class, 1L);          // version
            invokeMethod(builder, "r", Long.class, (now / 1000L) / 86400L * 86400L); // dailyStartTimeSec
            invokeMethod(builder, "s", Long.class, 100L);       // maxRoamingLogNum
            invokeMethod(builder, "t", Long.class, 0L);         // currentRoamingLogNum
            invokeMethod(builder, "u", Long.class, 100L);       // maxHistoryNum
            invokeMethod(builder, "v", Long.class, 0L);         // currentHistoryNum
            invokeMethod(builder, "w", Long.class, 86400L);     // dhcpLeaseTime

            // Status & Types
            invokeMethod(builder, "b", boolean.class, false);
            invokeMethod(builder, "e", Boolean.class, Boolean.FALSE);
            invokeMethod(builder, "f", Boolean.class, Boolean.TRUE);

            Object connectType = null;
            try {
                Object[] consts = connectTypeClass.getEnumConstants();
                if (consts != null && consts.length > 1) {
                    connectType = consts[1];
                } else if (consts != null && consts.length > 0) {
                    connectType = consts[0];
                }
            } catch (Throwable ignored) {}
            if (connectType != null) {
                invokeMethod(builder, "a", connectTypeClass, connectType);
            }

            // AP Wireless attributes
            invokeMethod(builder, "a", String.class, devMac);
            invokeMethod(builder, "b", String.class, devName);
            invokeMethod(builder, "a", Boolean.class, Boolean.FALSE);
            invokeMethod(builder, "b", Boolean.class, Boolean.TRUE);
            invokeMethod(builder, "g", Boolean.class, Boolean.TRUE);
            invokeMethod(builder, "C", String.class, "ap");
            invokeMethod(builder, "c", String.class, ssid != null ? ssid : "Ubatuba - Wifi Grátis");
            invokeMethod(builder, "d", String.class, ssidId != null ? ssidId : "6a7d336ee8c53e3f4e298b05");
            invokeMethod(builder, "a", Integer.class, Integer.valueOf(0));
            int channel = device.getWlanChannel() > 0 ? device.getWlanChannel() : 6;
            invokeMethod(builder, "f", Integer.class, Integer.valueOf(channel));
            int rssi = client.getSignalStrength() != 0 ? client.getSignalStrength() : -55;
            invokeMethod(builder, "d", Integer.class, Integer.valueOf(rssi));
            invokeMethod(builder, "a", Long.class, Long.valueOf(72000L));
            invokeMethod(builder, "b", Long.class, Long.valueOf(72000L));
            invokeMethod(builder, "e", Integer.class, Integer.valueOf(4));
            try {
                Class<?> wifiModeEnumClass = Class.forName("com.tplink.smb.omada.client.domain.model.c.h");
                Method getWifiMode = wifiModeEnumClass.getMethod("b", int.class);
                Object wifiMode = getWifiMode.invoke(null, 4);
                if (wifiMode != null) {
                    invokeMethod(builder, "a", wifiModeEnumClass, wifiMode);
                }
            } catch (Throwable ignored) {}
            invokeMethod(builder, "c", Boolean.class, Boolean.FALSE);

            // Device category & vendor
            String clientType = client.isWireless() ? "Mobile" : "PC";
            String vendor = deduceVendor(hostName);
            invokeMethod(builder, "p", String.class, clientType);
            invokeMethod(builder, "q", String.class, clientType);
            invokeMethod(builder, "B", String.class, clientType);
            invokeMethod(builder, "v", String.class, vendor);
            invokeMethod(builder, "w", String.class, vendor);
            invokeMethod(builder, "y", String.class, "");
            invokeMethod(builder, "z", String.class, "");
            invokeMethod(builder, "s", String.class, "");
            invokeMethod(builder, "t", String.class, "");

            // Build clientImage
            Method buildMethod = findMethod(builder.getClass(), "b");
            if (buildMethod == null) {
                buildMethod = bClass.getMethod("b");
            }
            buildMethod.setAccessible(true);
            Object clientImage = buildMethod.invoke(builder);

            // 1. Put into site's in-memory cacheMap
            if (cacheMap != null) {
                try {
                    Method setTimeoutMethod = clientImage.getClass().getMethod("x", Long.class);
                    setTimeoutMethod.setAccessible(true);
                    setTimeoutMethod.invoke(clientImage, now + 600_000L);
                } catch (Throwable ignored) {}

                try {
                    Class<?> freezeUtilClass = Class.forName("com.tplink.smb.omada.client.port.cache.clientimage.c");
                    Method freezeMethod = freezeUtilClass.getMethod("a", Object.class);
                    freezeMethod.setAccessible(true);
                    freezeMethod.invoke(null, clientImage);
                } catch (Throwable ignored) {}

                cacheMap.put(clientMac, clientImage);
            }

            // 2. Also register via clientImageCacheMemImpl.a(clientImage)
            try {
                Class<?> springUtilClass = Class.forName("com.tplink.smb.omada.common.spring.a");
                Method getBeanMethod = springUtilClass.getMethod("a", String.class);
                getBeanMethod.setAccessible(true);
                Object cacheBean = getBeanMethod.invoke(null, "clientImageCacheMemImpl");
                if (cacheBean != null) {
                    Method addMethod = cacheBean.getClass().getMethod("a", Class.forName("com.tplink.smb.omada.client.domain.model.clientimage.d"));
                    addMethod.setAccessible(true);
                    addMethod.invoke(cacheBean, clientImage);
                }
            } catch (Throwable t) {
                log.trace("clientImageCacheMemImpl notification notice: {}", t.getMessage());
            }

            log.info("Successfully updated Omada in-memory ClientImage for {}", clientMac);
        } catch (Throwable t) {
            log.warn("Could not update Omada in-memory ClientImage for {}: {}", clientMac, t.getMessage());
        }
    }

    private String lookupSsidId(String siteId, String ssidName) {
        if (mongoTemplate == null || siteId == null) return "6a7d336ee8c53e3f4e298b05";
        try {
            if (ssidName != null && !ssidName.isEmpty()) {
                Query q = new Query(Criteria.where("site_id").is(siteId).and("name").is(ssidName));
                Document doc = mongoTemplate.findOne(q, Document.class, "ssid");
                if (doc != null && doc.get("_id") != null) {
                    return doc.get("_id").toString();
                }
            }
            Query qSite = new Query(Criteria.where("site_id").is(siteId));
            Document firstSsid = mongoTemplate.findOne(qSite, Document.class, "ssid");
            if (firstSsid != null && firstSsid.get("_id") != null) {
                return firstSsid.get("_id").toString();
            }
        } catch (Exception e) {
            log.debug("SSID lookup notice: {}", e.getMessage());
        }
        return "6a7d336ee8c53e3f4e298b05";
    }

    private String deduceVendor(String name) {
        if (name == null || name.isEmpty()) return "Unknown";
        String lower = name.toLowerCase();
        if (lower.contains("poco")) return "Poco";
        if (lower.contains("redmi")) return "Redmi";
        if (lower.contains("xiaomi")) return "Xiaomi";
        if (lower.contains("galaxy") || lower.contains("samsung")) return "Samsung";
        if (lower.contains("mac") || lower.contains("iphone") || lower.contains("ipad") || lower.contains("apple")) return "Apple";
        if (lower.contains("openwrt")) return "OpenWrt";
        if (lower.contains("ata")) return "Grandstream";
        if (lower.contains("motorola") || lower.contains("moto")) return "Motorola";
        return "Unknown";
    }

    private void invokeMethod(Object target, String name, Class<?> paramType, Object value) {
        if (target == null) return;
        try {
            Method m = findMethod(target.getClass(), name, paramType);
            if (m != null) {
                m.setAccessible(true);
                m.invoke(target, value);
            }
        } catch (Throwable t) {
            log.trace("invokeMethod {}({}) on {} failed: {}", name, paramType != null ? paramType.getSimpleName() : "null", target.getClass().getSimpleName(), t.getMessage());
        }
    }

    private Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        Class<?> curr = clazz;
        while (curr != null && curr != Object.class) {
            try {
                if (paramTypes == null || paramTypes.length == 0) {
                    Method m = curr.getDeclaredMethod(name);
                    m.setAccessible(true);
                    return m;
                }
                Method m = curr.getDeclaredMethod(name, paramTypes);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {}
            if (paramTypes != null && paramTypes.length == 1) {
                Class<?> paramType = paramTypes[0];
                Class<?> altType = null;
                if (paramType == boolean.class) altType = Boolean.class;
                else if (paramType == Boolean.class) altType = boolean.class;
                else if (paramType == int.class) altType = Integer.class;
                else if (paramType == Integer.class) altType = int.class;
                else if (paramType == long.class) altType = Long.class;
                else if (paramType == Long.class) altType = long.class;

                if (altType != null) {
                    try {
                        Method m = curr.getDeclaredMethod(name, altType);
                        m.setAccessible(true);
                        return m;
                    } catch (NoSuchMethodException ignored) {}
                }
            }
            curr = curr.getSuperclass();
        }
        try {
            Method m = (paramTypes == null || paramTypes.length == 0) ?
                    clazz.getMethod(name) : clazz.getMethod(name, paramTypes);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException ignored) {}
        return null;
    }

    /**
     * Marks device OFFLINE in Omada when CWMP session or keepalive times out.
     */
    public void markDeviceOffline(String mac) {
        if (mac == null || mongoTemplate == null) return;
        try {
            Query query = new Query(Criteria.where("mac").is(mac.toUpperCase()));
            Update update = new Update();
            update.set("status", 0); // 0 = DISCONNECTED / OFFLINE
            mongoTemplate.updateFirst(query, update, OMADA_DEVICE_COLLECTION);
            log.info("Marked TR-069 device {} OFFLINE in Omada", mac);
        } catch (Exception e) {
            log.error("Failed to mark device {} offline in Omada: {}", mac, e.getMessage());
        }
    }
}
