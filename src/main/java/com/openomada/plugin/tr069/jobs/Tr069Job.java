package com.openomada.plugin.tr069.jobs;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents an asynchronous operation scheduled for execution on a TR-069 CPE.
 */
public class Tr069Job implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String deviceMac; // MAC address of target device
    private JobType jobType;
    private JobStatus status = JobStatus.PENDING;
    private Map<String, String> parameters = new HashMap<>();
    private String cwmpMessageId; // Correlated CWMP RPC ID

    private long createdAt = System.currentTimeMillis();
    private long startedAt = 0;
    private long completedAt = 0;

    private int retryCount = 0;
    private int maxRetries = 3;
    private String resultMessage = "";
    private int faultCode = 0;

    public Tr069Job() {
        this.id = UUID.randomUUID().toString();
    }

    public Tr069Job(String deviceMac, JobType jobType) {
        this.id = UUID.randomUUID().toString();
        this.deviceMac = deviceMac != null ? deviceMac.toUpperCase() : "";
        this.jobType = jobType;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDeviceMac() { return deviceMac; }
    public void setDeviceMac(String deviceMac) { this.deviceMac = deviceMac != null ? deviceMac.toUpperCase() : ""; }

    public JobType getJobType() { return jobType; }
    public void setJobType(JobType jobType) { this.jobType = jobType; }

    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }

    public Map<String, String> getParameters() { return parameters; }
    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
    }

    public String getCwmpMessageId() { return cwmpMessageId; }
    public void setCwmpMessageId(String cwmpMessageId) { this.cwmpMessageId = cwmpMessageId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getStartedAt() { return startedAt; }
    public void setStartedAt(long startedAt) { this.startedAt = startedAt; }

    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public String getResultMessage() { return resultMessage; }
    public void setResultMessage(String resultMessage) { this.resultMessage = resultMessage; }

    public int getFaultCode() { return faultCode; }
    public void setFaultCode(int faultCode) { this.faultCode = faultCode; }

    @Override
    public String toString() {
        return "Tr069Job{" +
                "id='" + id + '\'' +
                ", mac='" + deviceMac + '\'' +
                ", type=" + jobType +
                ", status=" + status +
                '}';
    }
}
