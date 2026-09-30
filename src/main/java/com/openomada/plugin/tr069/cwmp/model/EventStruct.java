package com.openomada.plugin.tr069.cwmp.model;

import java.io.Serializable;
import java.util.Objects;

public class EventStruct implements Serializable {
    private static final long serialVersionUID = 1L;

    private String eventCode;
    private String commandKey;

    public EventStruct() {}

    public EventStruct(String eventCode, String commandKey) {
        this.eventCode = eventCode;
        this.commandKey = commandKey != null ? commandKey : "";
    }

    public String getEventCode() { return eventCode; }
    public void setEventCode(String eventCode) { this.eventCode = eventCode; }

    public String getCommandKey() { return commandKey; }
    public void setCommandKey(String commandKey) { this.commandKey = commandKey; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventStruct that = (EventStruct) o;
        return Objects.equals(eventCode, that.eventCode) &&
               Objects.equals(commandKey, that.commandKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventCode, commandKey);
    }

    @Override
    public String toString() {
        return "EventStruct{" +
                "eventCode='" + eventCode + '\'' +
                ", commandKey='" + commandKey + '\'' +
                '}';
    }
}
