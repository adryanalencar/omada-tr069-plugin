package com.openomada.plugin.tr069.cwmp.rpc;

import com.openomada.plugin.tr069.cwmp.model.CWMPMessage;
import com.openomada.plugin.tr069.cwmp.model.ParameterAttributeStruct;

import java.util.ArrayList;
import java.util.List;

public class GetParameterAttributesResponse extends CWMPMessage {
    private static final long serialVersionUID = 1L;

    private List<ParameterAttributeStruct> parameterList = new ArrayList<>();

    public GetParameterAttributesResponse() {
        super();
    }

    public GetParameterAttributesResponse(String id) {
        super(id);
    }

    public GetParameterAttributesResponse(String id, List<ParameterAttributeStruct> parameterList) {
        super(id);
        if (parameterList != null) {
            this.parameterList = parameterList;
        }
    }

    @Override
    public String getRpcName() {
        return "GetParameterAttributesResponse";
    }

    public List<ParameterAttributeStruct> getParameterList() { return parameterList; }
    public void setParameterList(List<ParameterAttributeStruct> parameterList) {
        this.parameterList = parameterList != null ? parameterList : new ArrayList<>();
    }
    public void addParameterAttribute(ParameterAttributeStruct struct) {
        if (struct != null) this.parameterList.add(struct);
    }
}
