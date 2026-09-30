package com.openomada.plugin.tr069.cwmp.rpc;

import com.openomada.plugin.tr069.cwmp.model.CWMPMessage;
import com.openomada.plugin.tr069.cwmp.model.DeviceId;
import com.openomada.plugin.tr069.cwmp.model.EventStruct;
import com.openomada.plugin.tr069.cwmp.model.ParameterValueStruct;

import java.util.ArrayList;
import java.util.List;

public class Inform extends CWMPMessage {
    private static final long serialVersionUID = 1L;

    private DeviceId deviceId = new DeviceId();
    private List<EventStruct> events = new ArrayList<>();
    private int maxEnvelopes = 1;
    private String currentTime;
    private int retryCount = 0;
    private List<ParameterValueStruct> parameterList = new ArrayList<>();

    public Inform() {
        super();
    }

    public Inform(String id) {
        super(id);
    }

    @Override
    public String getRpcName() {
        return "Inform";
    }

    public DeviceId getDeviceId() { return deviceId; }
    public void setDeviceId(DeviceId deviceId) { this.deviceId = deviceId != null ? deviceId : new DeviceId(); }

    public List<EventStruct> getEvents() { return events; }
    public void setEvents(List<EventStruct> events) { this.events = events != null ? events : new ArrayList<>(); }
    public void addEvent(EventStruct event) { if (event != null) this.events.add(event); }

    public int getMaxEnvelopes() { return maxEnvelopes; }
    public void setMaxEnvelopes(int maxEnvelopes) { this.maxEnvelopes = maxEnvelopes; }

    public String getCurrentTime() { return currentTime; }
    public void setCurrentTime(String currentTime) { this.currentTime = currentTime; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public List<ParameterValueStruct> getParameterList() { return parameterList; }
    public void setParameterList(List<ParameterValueStruct> parameterList) {
        this.parameterList = parameterList != null ? parameterList : new ArrayList<>();
    }
    public void addParameter(ParameterValueStruct param) {
        if (param != null) this.parameterList.add(param);
    }

    public String getParameterValue(String path) {
        if (path == null) return null;
        for (ParameterValueStruct p : parameterList) {
            if (path.equals(p.getName())) {
                return p.getValue();
            }
        }
        return null;
    }

    public boolean hasEvent(String eventCodePrefix) {
        if (eventCodePrefix == null) return false;
        for (EventStruct event : events) {
            if (event.getEventCode() != null && event.getEventCode().startsWith(eventCodePrefix)) {
                return true;
            }
        }
        return false;
    }
}
