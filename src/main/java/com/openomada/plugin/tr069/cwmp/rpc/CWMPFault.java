package com.openomada.plugin.tr069.cwmp.rpc;

import com.openomada.plugin.tr069.cwmp.model.CWMPMessage;
import com.openomada.plugin.tr069.cwmp.model.FaultStruct;
import com.openomada.plugin.tr069.cwmp.model.SetParameterValuesFault;

import java.util.ArrayList;
import java.util.List;

public class CWMPFault extends CWMPMessage {
    private static final long serialVersionUID = 1L;

    private String soapFaultCode = "Client";
    private String soapFaultString = "CWMP fault";
    private int faultCode = 9002;
    private String faultString = "Internal error";
    private List<SetParameterValuesFault> setParameterValuesFaults = new ArrayList<>();

    public CWMPFault() {
        super();
    }

    public CWMPFault(String id) {
        super(id);
    }

    public CWMPFault(String id, int faultCode, String faultString) {
        super(id);
        this.faultCode = faultCode;
        this.faultString = faultString;
    }

    public CWMPFault(String id, FaultStruct faultStruct) {
        super(id);
        if (faultStruct != null) {
            this.faultCode = faultStruct.getFaultCode();
            this.faultString = faultStruct.getFaultString();
            this.setParameterValuesFaults = faultStruct.getParameterFaults();
        }
    }

    @Override
    public String getRpcName() {
        return "Fault";
    }

    @Override
    public boolean isFault() {
        return true;
    }

    @Override
    public boolean isResponse() {
        return true;
    }

    public String getSoapFaultCode() { return soapFaultCode; }
    public void setSoapFaultCode(String soapFaultCode) { this.soapFaultCode = soapFaultCode; }

    public String getSoapFaultString() { return soapFaultString; }
    public void setSoapFaultString(String soapFaultString) { this.soapFaultString = soapFaultString; }

    public int getFaultCode() { return faultCode; }
    public void setFaultCode(int faultCode) { this.faultCode = faultCode; }

    public String getFaultString() { return faultString; }
    public void setFaultString(String faultString) { this.faultString = faultString; }

    public List<SetParameterValuesFault> getSetParameterValuesFaults() { return setParameterValuesFaults; }
    public void setSetParameterValuesFaults(List<SetParameterValuesFault> setParameterValuesFaults) {
        this.setParameterValuesFaults = setParameterValuesFaults != null ? setParameterValuesFaults : new ArrayList<>();
    }

    public FaultStruct toFaultStruct() {
        FaultStruct fs = new FaultStruct(faultCode, faultString);
        fs.setParameterFaults(setParameterValuesFaults);
        return fs;
    }
}
