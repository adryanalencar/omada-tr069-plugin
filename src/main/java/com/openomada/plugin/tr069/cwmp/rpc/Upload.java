package com.openomada.plugin.tr069.cwmp.rpc;

import com.openomada.plugin.tr069.cwmp.model.CWMPMessage;

public class Upload extends CWMPMessage {
    private static final long serialVersionUID = 1L;

    private String commandKey = "";
    private String fileType = "1 Vendor Configuration File";
    private String url = "";
    private String username = "";
    private String password = "";
    private int delaySeconds = 0;

    public Upload() {
        super();
    }

    public Upload(String id) {
        super(id);
    }

    @Override
    public String getRpcName() {
        return "Upload";
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

    public int getDelaySeconds() { return delaySeconds; }
    public void setDelaySeconds(int delaySeconds) { this.delaySeconds = delaySeconds; }
}
