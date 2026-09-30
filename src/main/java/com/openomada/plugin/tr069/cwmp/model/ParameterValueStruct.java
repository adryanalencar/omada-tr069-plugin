package com.openomada.plugin.tr069.cwmp.model;

import java.io.Serializable;
import java.util.Objects;

public class ParameterValueStruct implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String value;
    private String type; // e.g. "xsd:string", "xsd:int", "xsd:boolean", "xsd:dateTime"

    public ParameterValueStruct() {}

    public ParameterValueStruct(String name, String value) {
        this(name, value, "xsd:string");
    }

    public ParameterValueStruct(String name, String value, String type) {
        this.name = name;
        this.value = value;
        this.type = type != null ? type : "xsd:string";
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParameterValueStruct that = (ParameterValueStruct) o;
        return Objects.equals(name, that.name) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }

    @Override
    public String toString() {
        return "ParameterValueStruct{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
