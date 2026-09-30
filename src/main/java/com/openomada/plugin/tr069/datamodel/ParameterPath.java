package com.openomada.plugin.tr069.datamodel;

import java.util.Objects;

/**
 * Utility for manipulating and inspecting TR-069 CWMP hierarchical parameter paths.
 * E.g., "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.SSID"
 */
public class ParameterPath {
    private final String fullPath;

    public ParameterPath(String path) {
        this.fullPath = path != null ? path.trim() : "";
    }

    public static ParameterPath of(String path) {
        return new ParameterPath(path);
    }

    public String getFullPath() {
        return fullPath;
    }

    public boolean isObject() {
        return fullPath.endsWith(".");
    }

    public boolean isLeaf() {
        return !isObject() && !fullPath.isEmpty();
    }

    public boolean isTr098() {
        return fullPath.startsWith("InternetGatewayDevice.");
    }

    public boolean isTr181() {
        return fullPath.startsWith("Device.");
    }

    public String getLeafName() {
        if (fullPath.isEmpty()) return "";
        String s = isObject() ? fullPath.substring(0, fullPath.length() - 1) : fullPath;
        int lastDot = s.lastIndexOf('.');
        return lastDot >= 0 ? s.substring(lastDot + 1) : s;
    }

    public String getParentPath() {
        if (fullPath.isEmpty()) return "";
        String s = isObject() ? fullPath.substring(0, fullPath.length() - 1) : fullPath;
        int lastDot = s.lastIndexOf('.');
        return lastDot >= 0 ? s.substring(0, lastDot + 1) : "";
    }

    public boolean matches(String wildcardOrPrefix) {
        if (wildcardOrPrefix == null) return false;
        if (wildcardOrPrefix.endsWith(".")) {
            return fullPath.startsWith(wildcardOrPrefix);
        }
        return fullPath.equals(wildcardOrPrefix);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParameterPath that = (ParameterPath) o;
        return Objects.equals(fullPath, that.fullPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullPath);
    }

    @Override
    public String toString() {
        return fullPath;
    }
}
