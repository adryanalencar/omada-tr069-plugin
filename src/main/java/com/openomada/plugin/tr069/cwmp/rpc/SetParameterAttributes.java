package com.openomada.plugin.tr069.cwmp.rpc;

import com.openomada.plugin.tr069.cwmp.model.CWMPMessage;
import com.openomada.plugin.tr069.cwmp.model.ParameterAttributeStruct;

import java.util.ArrayList;
import java.util.List;

public class SetParameterAttributes extends CWMPMessage {
    private static final long serialVersionUID = 1L;

    public static class ParameterAttributeChange {
        private String name;
        private boolean notificationChange;
        private int notification;
        private boolean accessListChange;
        private List<String> accessList = new ArrayList<>();

        public ParameterAttributeChange() {}

        public ParameterAttributeChange(String name, boolean notificationChange, int notification, boolean accessListChange, List<String> accessList) {
            this.name = name;
            this.notificationChange = notificationChange;
            this.notification = notification;
            this.accessListChange = accessListChange;
            if (accessList != null) this.accessList = accessList;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public boolean isNotificationChange() { return notificationChange; }
        public void setNotificationChange(boolean notificationChange) { this.notificationChange = notificationChange; }

        public int getNotification() { return notification; }
        public void setNotification(int notification) { this.notification = notification; }

        public boolean isAccessListChange() { return accessListChange; }
        public void setAccessListChange(boolean accessListChange) { this.accessListChange = accessListChange; }

        public List<String> getAccessList() { return accessList; }
        public void setAccessList(List<String> accessList) { this.accessList = accessList; }
    }

    public static class SetParameterAttributeStruct extends ParameterAttributeChange {
        public SetParameterAttributeStruct() { super(); }
        public SetParameterAttributeStruct(String name, boolean notificationChange, int notification, boolean accessListChange, List<String> accessList) {
            super(name, notificationChange, notification, accessListChange, accessList);
        }
    }

    private List<ParameterAttributeChange> parameterList = new ArrayList<>();

    public SetParameterAttributes() {
        super();
    }

    public SetParameterAttributes(String id) {
        super(id);
    }

    @Override
    public String getRpcName() {
        return "SetParameterAttributes";
    }

    public List<ParameterAttributeChange> getParameterList() { return parameterList; }
    public void setParameterList(List<ParameterAttributeChange> parameterList) {
        this.parameterList = parameterList != null ? parameterList : new ArrayList<>();
    }
    public void addParameterAttributeChange(ParameterAttributeChange change) {
        if (change != null) this.parameterList.add(change);
    }
}
