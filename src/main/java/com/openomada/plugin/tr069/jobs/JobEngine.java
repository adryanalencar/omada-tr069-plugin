package com.openomada.plugin.tr069.jobs;

import com.openomada.plugin.tr069.cwmp.model.CWMPMessage;
import com.openomada.plugin.tr069.cwmp.model.ParameterValueStruct;
import com.openomada.plugin.tr069.cwmp.rpc.*;
import com.openomada.plugin.tr069.session.CWMPSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Enterprise Job Engine managing asynchronous operation queuing,
 * translation to TR-069 CWMP RPC messages, response correlation, and fault handling.
 */
@Service
public class JobEngine {
    private static final Logger log = LoggerFactory.getLogger(JobEngine.class);

    private final Map<String, List<Tr069Job>> jobsByMac = new ConcurrentHashMap<>();
    private final Map<String, Tr069Job> jobsById = new ConcurrentHashMap<>();
    private final Map<String, Tr069Job> jobsByCwmpMsgId = new ConcurrentHashMap<>();

    public Tr069Job scheduleJob(String mac, JobType type, Map<String, String> params) {
        if (mac == null || type == null) return null;
        String normalizedMac = mac.toUpperCase();

        Tr069Job job = new Tr069Job(normalizedMac, type);
        if (params != null) {
            job.setParameters(params);
        }

        jobsById.put(job.getId(), job);
        jobsByMac.computeIfAbsent(normalizedMac, k -> new CopyOnWriteArrayList<>()).add(job);
        log.info("Scheduled TR-069 job {} ({}) for device {}", job.getId(), type, normalizedMac);
        return job;
    }

    public List<Tr069Job> getPendingJobs(String mac) {
        if (mac == null) return Collections.emptyList();
        List<Tr069Job> list = jobsByMac.get(mac.toUpperCase());
        if (list == null) return Collections.emptyList();

        List<Tr069Job> pending = new ArrayList<>();
        for (Tr069Job job : list) {
            if (job.getStatus() == JobStatus.PENDING) {
                pending.add(job);
            }
        }
        return pending;
    }

    /**
     * Converts the next pending job for this device into a CWMPMessage RPC.
     */
    public CWMPMessage prepareNextRpc(String mac, CWMPSession session) {
        if (mac == null) return null;
        List<Tr069Job> pending = getPendingJobs(mac);
        if (pending.isEmpty()) return null;

        Tr069Job job = pending.get(0);
        job.setStatus(JobStatus.IN_PROGRESS);
        job.setStartedAt(System.currentTimeMillis());

        CWMPMessage rpc = buildCwmpMessageForJob(job);
        job.setCwmpMessageId(rpc.getId());
        jobsByCwmpMsgId.put(rpc.getId(), job);

        log.info("Prepared CWMP RPC {} for job {} (Device: {})", rpc.getRpcName(), job.getId(), mac);
        return rpc;
    }

    public void handleRpcResponse(String mac, CWMPMessage response) {
        if (response == null) return;
        Tr069Job job = jobsByCwmpMsgId.remove(response.getId());
        if (job != null) {
            job.setStatus(JobStatus.COMPLETED);
            job.setCompletedAt(System.currentTimeMillis());
            job.setResultMessage("Success: " + response.getRpcName());
            log.info("Job {} completed successfully via response {}", job.getId(), response.getRpcName());
        }
    }

    public void handleRpcFault(String mac, CWMPFault fault) {
        if (fault == null) return;
        Tr069Job job = jobsByCwmpMsgId.remove(fault.getId());
        if (job != null) {
            job.setFaultCode(fault.getFaultCode());
            job.setResultMessage("Fault: " + fault.getFaultCode() + " - " + fault.getFaultString());

            if (job.getRetryCount() < job.getMaxRetries()) {
                job.setRetryCount(job.getRetryCount() + 1);
                job.setStatus(JobStatus.PENDING); // Retry on next session
                log.warn("Job {} failed with CWMP fault {}. Retrying ({}/{})",
                        job.getId(), fault.getFaultCode(), job.getRetryCount(), job.getMaxRetries());
            } else {
                job.setStatus(JobStatus.FAILED);
                job.setCompletedAt(System.currentTimeMillis());
                log.error("Job {} permanently failed with CWMP fault {}: {}",
                        job.getId(), fault.getFaultCode(), fault.getFaultString());
            }
        }
    }

    public Tr069Job getJob(String jobId) {
        return jobId != null ? jobsById.get(jobId) : null;
    }

    public List<Tr069Job> getAllJobsForDevice(String mac) {
        if (mac == null) return Collections.emptyList();
        List<Tr069Job> list = jobsByMac.get(mac.toUpperCase());
        return list != null ? new ArrayList<>(list) : Collections.emptyList();
    }

    private CWMPMessage buildCwmpMessageForJob(Tr069Job job) {
        String msgId = UUID.randomUUID().toString();
        switch (job.getJobType()) {
            case REBOOT -> {
                Reboot rb = new Reboot(msgId);
                rb.setCommandKey("job-" + job.getId());
                return rb;
            }
            case FACTORY_RESET -> {
                return new FactoryReset(msgId);
            }
            case SET_PARAMETERS -> {
                SetParameterValues spv = new SetParameterValues(msgId);
                spv.setParameterKey("job-" + job.getId());
                List<ParameterValueStruct> pList = new ArrayList<>();
                for (Map.Entry<String, String> entry : job.getParameters().entrySet()) {
                    pList.add(new ParameterValueStruct(entry.getKey(), entry.getValue(), "xsd:string"));
                }
                spv.setParameterList(pList);
                return spv;
            }
            case GET_PARAMETERS -> {
                GetParameterValues gpv = new GetParameterValues(msgId);
                gpv.setParameterNames(new ArrayList<>(job.getParameters().keySet()));
                return gpv;
            }
            case DOWNLOAD_FIRMWARE -> {
                Download dl = new Download(msgId);
                dl.setCommandKey("job-" + job.getId());
                dl.setFileType("1 Firmware Upgrade Image");
                dl.setUrl(job.getParameters().getOrDefault("url", ""));
                dl.setUsername(job.getParameters().getOrDefault("username", ""));
                dl.setPassword(job.getParameters().getOrDefault("password", ""));
                return dl;
            }
            default -> {
                GetParameterNames gpn = new GetParameterNames(msgId);
                gpn.setParameterPath("InternetGatewayDevice.");
                return gpn;
            }
        }
    }
}
