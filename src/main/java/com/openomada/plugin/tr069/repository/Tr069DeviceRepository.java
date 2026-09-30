package com.openomada.plugin.tr069.repository;

import com.openomada.plugin.tr069.device.Tr069Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enterprise Repository for Tr069Device with dual-mode persistence:
 * Uses Omada's native MongoDB when running inside the controller JVM,
 * with concurrent in-memory fallback for testing and isolation.
 */
@Repository
public class Tr069DeviceRepository {
    private static final Logger log = LoggerFactory.getLogger(Tr069DeviceRepository.class);
    public static final String COLLECTION_NAME = "tr069_devices";

    private final MongoTemplate mongoTemplate;
    private final Map<String, Tr069Device> memoryStore = new ConcurrentHashMap<>();

    @Autowired
    public Tr069DeviceRepository(@Autowired(required = false) MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
        if (mongoTemplate != null) {
            log.info("Tr069DeviceRepository initialized with native Omada MongoTemplate");
        } else {
            log.info("Tr069DeviceRepository initialized in in-memory fallback mode");
        }
    }

    public Tr069Device save(Tr069Device device) {
        if (device == null || device.getMac() == null) return null;
        String mac = device.getMac().toUpperCase();
        device.setMac(mac);

        memoryStore.put(mac, device);

        if (mongoTemplate != null) {
            try {
                Query query = new Query(Criteria.where("mac").is(mac));
                mongoTemplate.remove(query, COLLECTION_NAME);
                mongoTemplate.save(device, COLLECTION_NAME);
            } catch (Exception e) {
                log.error("Failed to persist Tr069Device to MongoDB: {}", e.getMessage(), e);
            }
        }
        return device;
    }

    public Optional<Tr069Device> findByMac(String mac) {
        if (mac == null) return Optional.empty();
        String normalized = mac.toUpperCase();

        if (mongoTemplate != null) {
            try {
                Query query = new Query(Criteria.where("mac").is(normalized));
                Tr069Device found = mongoTemplate.findOne(query, Tr069Device.class, COLLECTION_NAME);
                if (found != null) {
                    memoryStore.put(normalized, found);
                    return Optional.of(found);
                }
            } catch (Exception e) {
                log.debug("MongoDB lookup failed, falling back to memory: {}", e.getMessage());
            }
        }

        return Optional.ofNullable(memoryStore.get(normalized));
    }

    public List<Tr069Device> findAll() {
        if (mongoTemplate != null) {
            try {
                List<Tr069Device> list = mongoTemplate.findAll(Tr069Device.class, COLLECTION_NAME);
                for (Tr069Device d : list) {
                    if (d.getMac() != null) {
                        memoryStore.put(d.getMac().toUpperCase(), d);
                    }
                }
                return list;
            } catch (Exception e) {
                log.debug("MongoDB findAll failed, falling back to memory: {}", e.getMessage());
            }
        }
        return new ArrayList<>(memoryStore.values());
    }

    public void deleteByMac(String mac) {
        if (mac == null) return;
        String normalized = mac.toUpperCase();
        memoryStore.remove(normalized);

        if (mongoTemplate != null) {
            try {
                Query query = new Query(Criteria.where("mac").is(normalized));
                mongoTemplate.remove(query, COLLECTION_NAME);
            } catch (Exception e) {
                log.error("Failed to delete Tr069Device from MongoDB: {}", e.getMessage());
            }
        }
    }

    public long count() {
        if (mongoTemplate != null) {
            try {
                return mongoTemplate.count(new Query(), COLLECTION_NAME);
            } catch (Exception ignored) {}
        }
        return memoryStore.size();
    }
}
