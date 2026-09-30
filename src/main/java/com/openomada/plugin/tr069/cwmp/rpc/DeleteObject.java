package com.openomada.plugin.tr069.cwmp.rpc;

import com.openomada.plugin.tr069.cwmp.model.CWMPMessage;

public class DeleteObject extends CWMPMessage {
    private static final long serialVersionUID = 1L;

    private String objectName = "";
    private String parameterKey = "";

    public DeleteObject() {
        super();
    }

    public DeleteObject(String id) {
        super(id);
    }

    public DeleteObject(String id, String objectName, String parameterKey) {
        super(id);
        this.objectName = objectName != null ? objectName : "";
        this.parameterKey = parameterKey != null ? parameterKey : "";
    }

    @Override
    public String getRpcName() {
        return "DeleteObject";
    }

    public String getObjectName() { return objectName; }
    public void setObjectName(String objectName) { this.objectName = objectName != null ? objectName : ""; }

    public String getParameterKey() { return parameterKey; }
    public void setParameterKey(String parameterKey) { this.parameterKey = parameterKey != null ? parameterKey : ""; }
}
