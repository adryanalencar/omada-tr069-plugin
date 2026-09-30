package com.openomada.plugin.tr069.cwmp.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ParameterAttributeStruct implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private int notification; // 0=none, 1=passive, 2=active
    private List<String> accessList = new ArrayList<>();

    public ParameterAttributeStruct() {}

    public ParameterAttributeStruct(String name, int notification, List<String> accessList) {
        this.name = name;
        this.notification = notification;
        if (accessList != null) {
            this.accessList = accessList;
        }
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getNotification() { return notification; }
    public void setNotification(int notification) { this.notification = notification; }

    public List<String> getAccessList() { return accessList; }
    public void setAccessList(List<String> accessList) { this.accessList = accessList; }
}
