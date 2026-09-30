package com.openomada.plugin.tr069.cwmp.model;

import java.io.Serializable;
import java.util.Objects;

public class ParameterInfoStruct implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private boolean writable;

    public ParameterInfoStruct() {}

    public ParameterInfoStruct(String name, boolean writable) {
        this.name = name;
        this.writable = writable;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isWritable() { return writable; }
    public void setWritable(boolean writable) { this.writable = writable; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParameterInfoStruct that = (ParameterInfoStruct) o;
        return writable == that.writable && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, writable);
    }

    @Override
    public String toString() {
        return "ParameterInfoStruct{" +
                "name='" + name + '\'' +
                ", writable=" + writable +
                '}';
    }
}
