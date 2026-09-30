package com.openomada.plugin.tr069.simulator;

import com.openomada.plugin.tr069.cwmp.model.DeviceId;
import com.openomada.plugin.tr069.cwmp.model.EventStruct;
import com.openomada.plugin.tr069.cwmp.model.ParameterValueStruct;
import com.openomada.plugin.tr069.cwmp.rpc.*;
import com.openomada.plugin.tr069.cwmp.serializer.CWMPXmlSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * High-fidelity TR-069 CPE Simulator for testing and validation.
 * Emulates full CWMP lifecycles for TR-098 and TR-181 devices (ONUs, Routers, APs).
 */
public class CpeSimulator {
    private final CWMPXmlSerializer serializer = new CWMPXmlSerializer();

    private final String manufacturer;
    private final String oui;
    private final String productClass;
    private final String serialNumber;
    private final String mac;
    private final String ip;
    private final boolean isTr181;

    public CpeSimulator(String manufacturer, String oui, String productClass, String serialNumber, String mac, String ip, boolean isTr181) {
        this.manufacturer = manufacturer;
        this.oui = oui;
        this.productClass = productClass;
        this.serialNumber = serialNumber;
        this.mac = mac;
        this.ip = ip;
        this.isTr181 = isTr181;
    }

    public static CpeSimulator defaultGponOnu() {
        return new CpeSimulator("Huawei", "00259E", "HG8245H", "HWTC12345678", "00-25-9E-12-34-56", "192.168.1.50", false);
    }

    public static CpeSimulator defaultTr181Ap() {
        return new CpeSimulator("TP-Link", "E4C32A", "EAP610", "TPLK98765432", "E4-C3-2A-98-76-54", "192.168.1.51", true);
    }

    public Inform buildBootstrapInform() {
        return buildInform("0 BOOTSTRAP", "1 BOOT");
    }

    public Inform buildPeriodicInform() {
        return buildInform("2 PERIODIC");
    }

    public Inform buildInform(String... eventCodes) {
        Inform inform = new Inform(UUID.randomUUID().toString());
        inform.setDeviceId(new DeviceId(manufacturer, oui, productClass, serialNumber));
        inform.setRetryCount(0);
        inform.setMaxEnvelopes(1);
        inform.setCurrentTime("2026-09-28T05:00:00Z");

        List<EventStruct> events = new ArrayList<>();
        for (String ec : eventCodes) {
            events.add(new EventStruct(ec, ""));
        }
        inform.setEvents(events);

        List<ParameterValueStruct> params = new ArrayList<>();
        if (isTr181) {
            params.add(new ParameterValueStruct("Device.DeviceInfo.Manufacturer", manufacturer, "xsd:string"));
            params.add(new ParameterValueStruct("Device.DeviceInfo.ModelNumber", productClass, "xsd:string"));
            params.add(new ParameterValueStruct("Device.DeviceInfo.SoftwareVersion", "V1.0.4 Build 20260901", "xsd:string"));
            params.add(new ParameterValueStruct("Device.DeviceInfo.HardwareVersion", "v1.0", "xsd:string"));
            params.add(new ParameterValueStruct("Device.DeviceInfo.UpTime", "12540", "xsd:unsignedInt"));
            params.add(new ParameterValueStruct("Device.ManagementServer.ConnectionRequestURL", "http://" + ip + ":7547/tr069/conn_req", "xsd:string"));
            params.add(new ParameterValueStruct("Device.ManagementServer.ConnectionRequestUsername", "acsuser", "xsd:string"));
            params.add(new ParameterValueStruct("Device.ManagementServer.ConnectionRequestPassword", "acspass", "xsd:string"));
            params.add(new ParameterValueStruct("Device.Ethernet.Interface.1.MACAddress", mac, "xsd:string"));
            params.add(new ParameterValueStruct("Device.Optical.Interface.1.RxPower", "-19.45", "xsd:string"));
            params.add(new ParameterValueStruct("Device.Optical.Interface.1.TxPower", "2.15", "xsd:string"));
            params.add(new ParameterValueStruct("Device.WiFi.SSID.1.SSID", "OpenOmada_WiFi_5G", "xsd:string"));
            params.add(new ParameterValueStruct("Device.WiFi.AccessPoint.1.AssociatedDeviceNumberOfEntries", "12", "xsd:unsignedInt"));
        } else {
            params.add(new ParameterValueStruct("InternetGatewayDevice.DeviceInfo.Manufacturer", manufacturer, "xsd:string"));
            params.add(new ParameterValueStruct("InternetGatewayDevice.DeviceInfo.ModelName", productClass, "xsd:string"));
            params.add(new ParameterValueStruct("InternetGatewayDevice.DeviceInfo.SoftwareVersion", "V3R017C10S115", "xsd:string"));
            params.add(new ParameterValueStruct("InternetGatewayDevice.DeviceInfo.HardwareVersion", "HG8245H_V1", "xsd:string"));
            params.add(new ParameterValueStruct("InternetGatewayDevice.DeviceInfo.UpTime", "86400", "xsd:unsignedInt"));
            params.add(new ParameterValueStruct("InternetGatewayDevice.ManagementServer.ConnectionRequestURL", "http://" + ip + ":7547/tr069/conn_req", "xsd:string"));
            params.add(new ParameterValueStruct("InternetGatewayDevice.ManagementServer.ConnectionRequestUsername", "telecomadmin", "xsd:string"));
            params.add(new ParameterValueStruct("InternetGatewayDevice.ManagementServer.ConnectionRequestPassword", "admintelecom", "xsd:string"));
            params.add(new ParameterValueStruct("InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.MACAddress", mac, "xsd:string"));
            params.add(new ParameterValueStruct("InternetGatewayDevice.WANDevice.1.WANDSLDiagnostics.RxPower", "-21.20", "xsd:string"));
            params.add(new ParameterValueStruct("InternetGatewayDevice.WANDevice.1.WANDSLDiagnostics.TxPower", "2.50", "xsd:string"));
            params.add(new ParameterValueStruct("InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.SSID", "Huawei-Fiber-WiFi", "xsd:string"));
            params.add(new ParameterValueStruct("InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.TotalAssociations", "8", "xsd:unsignedInt"));
        }
        inform.setParameterList(params);
        return inform;
    }

    public String serialize(com.openomada.plugin.tr069.cwmp.model.CWMPMessage msg) {
        return serializer.serialize(msg);
    }

    public String getMac() { return mac; }
    public String getIp() { return ip; }
}
