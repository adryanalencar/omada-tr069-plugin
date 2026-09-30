package com.openomada.plugin.tr069.cwmp.rpc;

import com.openomada.plugin.tr069.cwmp.model.CWMPMessage;

public class Download extends CWMPMessage {
    private static final long serialVersionUID = 1L;

    private String commandKey = "";
    private String fileType = "1 Firmware Upgrade Image"; // e.g. 1 Firmware Upgrade Image, 2 Web Content, 3 Vendor Configuration File
    private String url = "";
    private String username = "";
    private String password = "";
    private long fileSize = 0;
    private String targetFileName = "";
    private int delaySeconds = 0;
    private String successUrl = "";
    private String failureUrl = "";

    public Download() {
        super();
    }

    public Download(String id) {
        super(id);
    }

    @Override
    public String getRpcName() {
        return "Download";
    }

    public String getCommandKey() { return commandKey; }
    public void setCommandKey(String commandKey) { this.commandKey = commandKey != null ? commandKey : ""; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType != null ? fileType : ""; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url != null ? url : ""; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username != null ? username : ""; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password != null ? password : ""; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getTargetFileName() { return targetFileName; }
    public void setTargetFileName(String targetFileName) { this.targetFileName = targetFileName != null ? targetFileName : ""; }

    public int getDelaySeconds() { return delaySeconds; }
    public void setDelaySeconds(int delaySeconds) { this.delaySeconds = delaySeconds; }

    public String getSuccessUrl() { return successUrl; }
    public void setSuccessUrl(String successUrl) { this.successUrl = successUrl != null ? successUrl : ""; }

    public String getFailureUrl() { return failureUrl; }
    public void setFailureUrl(String failureUrl) { this.failureUrl = failureUrl != null ? failureUrl : ""; }
}
