package com.openomada.plugin.tr069.cwmp.rpc;

import com.openomada.plugin.tr069.cwmp.model.CWMPMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GetParameterValues extends CWMPMessage {
    private static final long serialVersionUID = 1L;

    private List<String> parameterNames = new ArrayList<>();

    public GetParameterValues() {
        super();
    }

    public GetParameterValues(String id) {
        super(id);
    }

    public GetParameterValues(String id, List<String> parameterNames) {
        super(id);
        if (parameterNames != null) {
            this.parameterNames = parameterNames;
        }
    }

    public GetParameterValues(String id, String... parameterNames) {
        super(id);
        if (parameterNames != null) {
            this.parameterNames.addAll(Arrays.asList(parameterNames));
        }
    }

    @Override
    public String getRpcName() {
        return "GetParameterValues";
    }

    public List<String> getParameterNames() { return parameterNames; }
    public void setParameterNames(List<String> parameterNames) {
        this.parameterNames = parameterNames != null ? parameterNames : new ArrayList<>();
    }
    public void addParameterName(String name) {
        if (name != null) this.parameterNames.add(name);
    }
}
