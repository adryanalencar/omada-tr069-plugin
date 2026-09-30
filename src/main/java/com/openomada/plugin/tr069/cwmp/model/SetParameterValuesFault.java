package com.openomada.plugin.tr069.cwmp.model;

import java.io.Serializable;

public class SetParameterValuesFault implements Serializable {
    private static final long serialVersionUID = 1L;

    private String parameterName;
    private int faultCode;
    private String faultString;

    public SetParameterValuesFault() {}

    public SetParameterValuesFault(String parameterName, int faultCode, String faultString) {
        this.parameterName = parameterName;
        this.faultCode = faultCode;
        this.faultString = faultString;
    }

    public String getParameterName() { return parameterName; }
    public void setParameterName(String parameterName) { this.parameterName = parameterName; }

    public int getFaultCode() { return faultCode; }
    public void setFaultCode(int faultCode) { this.faultCode = faultCode; }

    public String getFaultString() { return faultString; }
    public void setFaultString(String faultString) { this.faultString = faultString; }

    @Override
    public String toString() {
        return "SetParameterValuesFault{" +
                "parameterName='" + parameterName + '\'' +
                ", faultCode=" + faultCode +
                ", faultString='" + faultString + '\'' +
                '}';
    }
}
