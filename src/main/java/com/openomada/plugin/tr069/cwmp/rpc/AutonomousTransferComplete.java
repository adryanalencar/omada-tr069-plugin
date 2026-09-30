package com.openomada.plugin.tr069.cwmp.rpc;

import com.openomada.plugin.tr069.cwmp.model.CWMPMessage;
import com.openomada.plugin.tr069.cwmp.model.FaultStruct;

public class AutonomousTransferComplete extends CWMPMessage {
    private static final long serialVersionUID = 1L;

    private String announceUrl = "";
    private String transferUrl = "";
    private boolean isDownload;
    private String fileType = "";
    private long fileSize;
    private String targetFileName = "";
    private FaultStruct faultStruct;
    private String startTime;
    private String completeTime;

    public AutonomousTransferComplete() {
        super();
    }

    public AutonomousTransferComplete(String id) {
        super(id);
    }

    @Override
    public String getRpcName() {
        return "AutonomousTransferComplete";
    }

    public String getAnnounceUrl() { return announceUrl; }
    public void setAnnounceUrl(String announceUrl) { this.announceUrl = announceUrl != null ? announceUrl : ""; }

    public String getTransferUrl() { return transferUrl; }
    public void setTransferUrl(String transferUrl) { this.transferUrl = transferUrl != null ? transferUrl : ""; }

    public boolean isDownload() { return isDownload; }
    public void setDownload(boolean download) { isDownload = download; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType != null ? fileType : ""; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getTargetFileName() { return targetFileName; }
    public void setTargetFileName(String targetFileName) { this.targetFileName = targetFileName != null ? targetFileName : ""; }

    public FaultStruct getFaultStruct() { return faultStruct; }
    public void setFaultStruct(FaultStruct faultStruct) { this.faultStruct = faultStruct; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getCompleteTime() { return completeTime; }
    public void setCompleteTime(String completeTime) { this.completeTime = completeTime; }
}
