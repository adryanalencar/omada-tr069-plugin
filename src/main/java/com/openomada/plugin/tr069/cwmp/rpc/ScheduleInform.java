package com.openomada.plugin.tr069.cwmp.rpc;

import com.openomada.plugin.tr069.cwmp.model.CWMPMessage;

public class ScheduleInform extends CWMPMessage {
    private static final long serialVersionUID = 1L;

    private int delaySeconds = 0;
    private String commandKey = "";

    public ScheduleInform() {
        super();
    }

    public ScheduleInform(String id) {
        super(id);
    }

    public ScheduleInform(String id, int delaySeconds, String commandKey) {
        super(id);
        this.delaySeconds = delaySeconds;
        this.commandKey = commandKey != null ? commandKey : "";
    }

    @Override
    public String getRpcName() {
        return "ScheduleInform";
    }

    public int getDelaySeconds() { return delaySeconds; }
    public void setDelaySeconds(int delaySeconds) { this.delaySeconds = delaySeconds; }

    public String getCommandKey() { return commandKey; }
    public void setCommandKey(String commandKey) { this.commandKey = commandKey != null ? commandKey : ""; }
}
