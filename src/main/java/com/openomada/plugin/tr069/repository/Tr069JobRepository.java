package com.openomada.plugin.tr069.repository;

import com.openomada.plugin.tr069.jobs.Tr069Job;
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
 * Enterprise Repository for Tr069Job with dual-mode persistence (MongoDB + in-memory fallback).
 */
@Repository
public class Tr069JobRepository {
    private static final Logger log = LoggerFactory.getLogger(Tr069JobRepository.class);
    public static final String COLLECTION_NAME = "tr069_jobs";

    private final MongoTemplate mongoTemplate;
    private final Map<String, Tr069Job> memoryStore = new ConcurrentHashMap<>();

    @Autowired
    public Tr069JobRepository(@Autowired(required = false) MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Tr069Job save(Tr069Job job) {
        if (job == null || job.getId() == null) return null;
        memoryStore.put(job.getId(), job);

        if (mongoTemplate != null) {
            try {
                mongoTemplate.save(job, COLLECTION_NAME);
            } catch (Exception e) {
                log.error("Failed to persist Tr069Job to MongoDB: {}", e.getMessage());
            }
        }
        return job;
    }

    public Optional<Tr069Job> findById(String id) {
        if (id == null) return Optional.empty();
        if (mongoTemplate != null) {
            try {
                Query query = new Query(Criteria.where("id").is(id));
                Tr069Job found = mongoTemplate.findOne(query, Tr069Job.class, COLLECTION_NAME);
                if (found != null) {
                    memoryStore.put(id, found);
                    return Optional.of(found);
                }
            } catch (Exception ignored) {}
        }
        return Optional.ofNullable(memoryStore.get(id));
    }

    public List<Tr069Job> findByDeviceMac(String mac) {
        if (mac == null) return Collections.emptyList();
        String normalized = mac.toUpperCase();

        if (mongoTemplate != null) {
            try {
                Query query = new Query(Criteria.where("deviceMac").is(normalized));
                return mongoTemplate.find(query, Tr069Job.class, COLLECTION_NAME);
            } catch (Exception ignored) {}
        }

        List<Tr069Job> result = new ArrayList<>();
        for (Tr069Job j : memoryStore.values()) {
            if (normalized.equals(j.getDeviceMac())) {
                result.add(j);
            }
        }
        return result;
    }
}
