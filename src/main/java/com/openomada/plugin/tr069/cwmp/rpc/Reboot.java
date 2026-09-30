package com.openomada.plugin.tr069.cwmp.rpc;

import com.openomada.plugin.tr069.cwmp.model.CWMPMessage;

public class Reboot extends CWMPMessage {
    private static final long serialVersionUID = 1L;

    private String commandKey = "";

    public Reboot() {
        super();
    }

    public Reboot(String id) {
        super(id);
    }

    public Reboot(String id, String commandKey) {
        super(id);
        this.commandKey = commandKey != null ? commandKey : "";
    }

    @Override
    public String getRpcName() {
        return "Reboot";
    }

    public String getCommandKey() { return commandKey; }
    public void setCommandKey(String commandKey) { this.commandKey = commandKey != null ? commandKey : ""; }
}
