package com.openomada.plugin.tr069.cwmp.rpc;

import com.openomada.plugin.tr069.cwmp.model.CWMPMessage;
import com.openomada.plugin.tr069.cwmp.model.ParameterValueStruct;

import java.util.ArrayList;
import java.util.List;

public class SetParameterValues extends CWMPMessage {
    private static final long serialVersionUID = 1L;

    private List<ParameterValueStruct> parameterList = new ArrayList<>();
    private String parameterKey = "";

    public SetParameterValues() {
        super();
    }

    public SetParameterValues(String id) {
        super(id);
    }

    public SetParameterValues(String id, List<ParameterValueStruct> parameterList, String parameterKey) {
        super(id);
        if (parameterList != null) {
            this.parameterList = parameterList;
        }
        this.parameterKey = parameterKey != null ? parameterKey : "";
    }

    @Override
    public String getRpcName() {
        return "SetParameterValues";
    }

    public List<ParameterValueStruct> getParameterList() { return parameterList; }
    public void setParameterList(List<ParameterValueStruct> parameterList) {
        this.parameterList = parameterList != null ? parameterList : new ArrayList<>();
    }
    public void addParameter(ParameterValueStruct param) {
        if (param != null) this.parameterList.add(param);
    }

    public String getParameterKey() { return parameterKey; }
    public void setParameterKey(String parameterKey) {
        this.parameterKey = parameterKey != null ? parameterKey : "";
    }
}
