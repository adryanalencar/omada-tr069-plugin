package com.openomada.plugin.tr069.cwmp.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FaultStruct implements Serializable {
    private static final long serialVersionUID = 1L;

    private int faultCode;
    private String faultString;
    private List<SetParameterValuesFault> setParameterValuesFaults = new ArrayList<>();

    public FaultStruct() {}

    public FaultStruct(int faultCode, String faultString) {
        this.faultCode = faultCode;
        this.faultString = faultString;
    }

    public int getFaultCode() { return faultCode; }
    public void setFaultCode(int faultCode) { this.faultCode = faultCode; }

    public String getFaultString() { return faultString; }
    public void setFaultString(String faultString) { this.faultString = faultString; }

    public List<SetParameterValuesFault> getSetParameterValuesFaults() { return setParameterValuesFaults; }
    public void setSetParameterValuesFaults(List<SetParameterValuesFault> list) {
        this.setParameterValuesFaults = list != null ? list : new ArrayList<>();
    }

    public List<SetParameterValuesFault> getParameterFaults() { return getSetParameterValuesFaults(); }
    public void setParameterFaults(List<SetParameterValuesFault> list) { setSetParameterValuesFaults(list); }

    public void addSetParameterValuesFault(SetParameterValuesFault fault) {
        if (fault != null) {
            this.setParameterValuesFaults.add(fault);
        }
    }

    @Override
    public String toString() {
        return "FaultStruct{" +
                "faultCode=" + faultCode +
                ", faultString='" + faultString + '\'' +
                ", setParameterValuesFaults=" + setParameterValuesFaults +
                '}';
    }
}
