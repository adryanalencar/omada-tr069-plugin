package com.openomada.plugin.tr069.cwmp.rpc;

import com.openomada.plugin.tr069.cwmp.model.CWMPMessage;
import com.openomada.plugin.tr069.cwmp.model.ParameterInfoStruct;

import java.util.ArrayList;
import java.util.List;

public class GetParameterNamesResponse extends CWMPMessage {
    private static final long serialVersionUID = 1L;

    private List<ParameterInfoStruct> parameterList = new ArrayList<>();

    public GetParameterNamesResponse() {
        super();
    }

    public GetParameterNamesResponse(String id) {
        super(id);
    }

    public GetParameterNamesResponse(String id, List<ParameterInfoStruct> parameterList) {
        super(id);
        if (parameterList != null) {
            this.parameterList = parameterList;
        }
    }

    @Override
    public String getRpcName() {
        return "GetParameterNamesResponse";
    }

    public List<ParameterInfoStruct> getParameterList() { return parameterList; }
    public void setParameterList(List<ParameterInfoStruct> parameterList) {
        this.parameterList = parameterList != null ? parameterList : new ArrayList<>();
    }
    public void addParameterInfo(ParameterInfoStruct info) {
        if (info != null) this.parameterList.add(info);
    }
}
