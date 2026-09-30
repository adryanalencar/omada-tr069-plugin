package com.openomada.plugin.tr069.cwmp.rpc;

import com.openomada.plugin.tr069.cwmp.model.CWMPMessage;

public class DownloadResponse extends CWMPMessage {
    private static final long serialVersionUID = 1L;

    private int status; // 0=download completed and applied, 1=download not yet completed
    private String startTime;
    private String completeTime;

    public DownloadResponse() {
        super();
    }

    public DownloadResponse(String id) {
        super(id);
    }

    public DownloadResponse(String id, int status, String startTime, String completeTime) {
        super(id);
        this.status = status;
        this.startTime = startTime;
        this.completeTime = completeTime;
    }

    @Override
    public String getRpcName() {
        return "DownloadResponse";
    }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getCompleteTime() { return completeTime; }
    public void setCompleteTime(String completeTime) { this.completeTime = completeTime; }
}
