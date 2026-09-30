package com.openomada.plugin.tr069.cwmp.model;

import java.io.Serializable;
import java.util.Objects;

public class DeviceId implements Serializable {
    private static final long serialVersionUID = 1L;

    private String manufacturer;
    private String oui;
    private String productClass;
    private String serialNumber;

    public DeviceId() {}

    public DeviceId(String manufacturer, String oui, String productClass, String serialNumber) {
        this.manufacturer = manufacturer;
        this.oui = oui;
        this.productClass = productClass;
        this.serialNumber = serialNumber;
    }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getOui() { return oui; }
    public void setOui(String oui) { this.oui = oui; }

    public String getProductClass() { return productClass; }
    public void setProductClass(String productClass) { this.productClass = productClass; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getCompositeKey() {
        return (oui != null ? oui : "") + "-" +
               (productClass != null ? productClass : "") + "-" +
               (serialNumber != null ? serialNumber : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceId deviceId = (DeviceId) o;
        return Objects.equals(oui, deviceId.oui) &&
               Objects.equals(productClass, deviceId.productClass) &&
               Objects.equals(serialNumber, deviceId.serialNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oui, productClass, serialNumber);
    }

    @Override
    public String toString() {
        return "DeviceId{" +
                "manufacturer='" + manufacturer + '\'' +
                ", oui='" + oui + '\'' +
                ", productClass='" + productClass + '\'' +
                ", serialNumber='" + serialNumber + '\'' +
                '}';
    }
}
