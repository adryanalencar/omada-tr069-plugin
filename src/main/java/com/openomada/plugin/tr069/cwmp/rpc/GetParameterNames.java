package com.openomada.plugin.tr069.cwmp.rpc;

import com.openomada.plugin.tr069.cwmp.model.CWMPMessage;

public class GetParameterNames extends CWMPMessage {
    private static final long serialVersionUID = 1L;

    private String parameterPath = "";
    private boolean nextLevel = false;

    public GetParameterNames() {
        super();
    }

    public GetParameterNames(String id) {
        super(id);
    }

    public GetParameterNames(String id, String parameterPath, boolean nextLevel) {
        super(id);
        this.parameterPath = parameterPath != null ? parameterPath : "";
        this.nextLevel = nextLevel;
    }

    @Override
    public String getRpcName() {
        return "GetParameterNames";
    }

    public String getParameterPath() { return parameterPath; }
    public void setParameterPath(String parameterPath) {
        this.parameterPath = parameterPath != null ? parameterPath : "";
    }

    public boolean isNextLevel() { return nextLevel; }
    public void setNextLevel(boolean nextLevel) { this.nextLevel = nextLevel; }
}
