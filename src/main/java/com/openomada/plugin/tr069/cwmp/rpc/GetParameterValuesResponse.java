package com.openomada.plugin.tr069.cwmp.rpc;

import com.openomada.plugin.tr069.cwmp.model.CWMPMessage;
import com.openomada.plugin.tr069.cwmp.model.ParameterValueStruct;

import java.util.ArrayList;
import java.util.List;

public class GetParameterValuesResponse extends CWMPMessage {
    private static final long serialVersionUID = 1L;

    private List<ParameterValueStruct> parameterList = new ArrayList<>();

    public GetParameterValuesResponse() {
        super();
    }

    public GetParameterValuesResponse(String id) {
        super(id);
    }

    public GetParameterValuesResponse(String id, List<ParameterValueStruct> parameterList) {
        super(id);
        if (parameterList != null) {
            this.parameterList = parameterList;
        }
    }

    @Override
    public String getRpcName() {
        return "GetParameterValuesResponse";
    }

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
}
