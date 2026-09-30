package com.openomada.plugin.tr069.cwmp.rpc;

import com.openomada.plugin.tr069.cwmp.model.CWMPMessage;

import java.util.ArrayList;
import java.util.List;

public class GetParameterAttributes extends CWMPMessage {
    private static final long serialVersionUID = 1L;

    private List<String> parameterNames = new ArrayList<>();

    public GetParameterAttributes() {
        super();
    }

    public GetParameterAttributes(String id) {
        super(id);
    }

    public GetParameterAttributes(String id, List<String> parameterNames) {
        super(id);
        if (parameterNames != null) {
            this.parameterNames = parameterNames;
        }
    }

    @Override
    public String getRpcName() {
        return "GetParameterAttributes";
    }

    public List<String> getParameterNames() { return parameterNames; }
    public void setParameterNames(List<String> parameterNames) {
        this.parameterNames = parameterNames != null ? parameterNames : new ArrayList<>();
    }
    public void addParameterName(String name) {
        if (name != null) this.parameterNames.add(name);
    }
}
