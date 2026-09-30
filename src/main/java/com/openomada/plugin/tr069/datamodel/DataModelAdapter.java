package com.openomada.plugin.tr069.datamodel;

import java.util.Collection;

/**
 * Enterprise adapter providing standardized parameter path resolution
 * across TR-098 (InternetGatewayDevice.) and TR-181 (Device.) CPE data models.
 */
public class DataModelAdapter {

    public static DataModelType detect(Collection<String> parameterNames) {
        if (parameterNames == null || parameterNames.isEmpty()) {
            return DataModelType.UNKNOWN;
        }
        for (String param : parameterNames) {
            if (param != null) {
                if (param.startsWith("Device.")) {
                    return DataModelType.TR181;
                }
                if (param.startsWith("InternetGatewayDevice.")) {
                    return DataModelType.TR098;
                }
            }
        }
        return DataModelType.UNKNOWN;
    }

    // --- Device Information ---

    public static String getManufacturerPath(DataModelType type) {
        return type == DataModelType.TR181 ? "Device.DeviceInfo.Manufacturer" : "InternetGatewayDevice.DeviceInfo.Manufacturer";
    }

    public static String getModelNamePath(DataModelType type) {
        return type == DataModelType.TR181 ? "Device.DeviceInfo.ModelNumber" : "InternetGatewayDevice.DeviceInfo.ModelName";
    }

    public static String getSoftwareVersionPath(DataModelType type) {
        return type == DataModelType.TR181 ? "Device.DeviceInfo.SoftwareVersion" : "InternetGatewayDevice.DeviceInfo.SoftwareVersion";
    }

    public static String getHardwareVersionPath(DataModelType type) {
        return type == DataModelType.TR181 ? "Device.DeviceInfo.HardwareVersion" : "InternetGatewayDevice.DeviceInfo.HardwareVersion";
    }

    public static String getUptimePath(DataModelType type) {
        return type == DataModelType.TR181 ? "Device.DeviceInfo.UpTime" : "InternetGatewayDevice.DeviceInfo.UpTime";
    }

    // --- Management Server ---

    public static String getConnectionRequestUrlPath(DataModelType type) {
        return type == DataModelType.TR181 ? "Device.ManagementServer.ConnectionRequestURL" : "InternetGatewayDevice.ManagementServer.ConnectionRequestURL";
    }

    public static String getConnectionRequestUsernamePath(DataModelType type) {
        return type == DataModelType.TR181 ? "Device.ManagementServer.ConnectionRequestUsername" : "InternetGatewayDevice.ManagementServer.ConnectionRequestUsername";
    }

    public static String getConnectionRequestPasswordPath(DataModelType type) {
        return type == DataModelType.TR181 ? "Device.ManagementServer.ConnectionRequestPassword" : "InternetGatewayDevice.ManagementServer.ConnectionRequestPassword";
    }

    public static String getPeriodicInformIntervalPath(DataModelType type) {
        return type == DataModelType.TR181 ? "Device.ManagementServer.PeriodicInformInterval" : "InternetGatewayDevice.ManagementServer.PeriodicInformInterval";
    }

    // --- WLAN / WiFi ---

    public static String getWlanSsidPath(DataModelType type, int radioIndex, int ssidIndex) {
        if (type == DataModelType.TR181) {
            return "Device.WiFi.SSID." + ssidIndex + ".SSID";
        } else {
            return "InternetGatewayDevice.LANDevice.1.WLANConfiguration." + ssidIndex + ".SSID";
        }
    }

    public static String getWlanEnablePath(DataModelType type, int radioIndex, int ssidIndex) {
        if (type == DataModelType.TR181) {
            return "Device.WiFi.SSID." + ssidIndex + ".Enable";
        } else {
            return "InternetGatewayDevice.LANDevice.1.WLANConfiguration." + ssidIndex + ".Enable";
        }
    }

    public static String getWlanPasswordPath(DataModelType type, int radioIndex, int ssidIndex) {
        if (type == DataModelType.TR181) {
            return "Device.WiFi.AccessPoint." + ssidIndex + ".Security.KeyPassphrase";
        } else {
            return "InternetGatewayDevice.LANDevice.1.WLANConfiguration." + ssidIndex + ".PreSharedKey.1.KeyPassphrase";
        }
    }

    public static String getWlanChannelPath(DataModelType type, int radioIndex) {
        if (type == DataModelType.TR181) {
            return "Device.WiFi.Radio." + radioIndex + ".Channel";
        } else {
            return "InternetGatewayDevice.LANDevice.1.WLANConfiguration." + radioIndex + ".Channel";
        }
    }

    public static String getWlanAssociatedDevicesCountPath(DataModelType type, int radioIndex, int ssidIndex) {
        if (type == DataModelType.TR181) {
            return "Device.WiFi.AccessPoint." + ssidIndex + ".AssociatedDeviceNumberOfEntries";
        } else {
            return "InternetGatewayDevice.LANDevice.1.WLANConfiguration." + ssidIndex + ".TotalAssociations";
        }
    }

    // --- WAN / Network ---

    public static String getWanExternalIpPath(DataModelType type) {
        if (type == DataModelType.TR181) {
            return "Device.IP.Interface.1.IPv4Address.1.IPAddress";
        } else {
            return "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.ExternalIPAddress";
        }
    }

    public static String getWanMacAddressPath(DataModelType type) {
        if (type == DataModelType.TR181) {
            return "Device.Ethernet.Interface.1.MACAddress";
        } else {
            return "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.MACAddress";
        }
    }

    // --- Connected Clients / Hosts ---

    public static String getHostsPath(DataModelType type) {
        return type == DataModelType.TR181 ? "Device.Hosts.Host." : "InternetGatewayDevice.LANDevice.1.Hosts.Host.";
    }

    public static String getWlanAssociatedDevicesPath(DataModelType type, int radioIndex) {
        if (type == DataModelType.TR181) {
            return "Device.WiFi.AccessPoint." + radioIndex + ".AssociatedDevice.";
        } else {
            return "InternetGatewayDevice.LANDevice.1.WLANConfiguration." + radioIndex + ".AssociatedDevice.";
        }
    }

    public static String[] getTrafficCounterPaths(DataModelType type) {
        if (type == DataModelType.TR181) {
            return new String[] {
                "Device.IP.Interface.1.Stats.BytesReceived",
                "Device.IP.Interface.1.Stats.BytesSent"
            };
        } else {
            return new String[] {
                "InternetGatewayDevice.WANDevice.1.WANCommonInterfaceConfig.TotalBytesReceived",
                "InternetGatewayDevice.WANDevice.1.WANCommonInterfaceConfig.TotalBytesSent"
            };
        }
    }

    // --- Optical / GPON Telemetry (Vendor extensions supported) ---

    public static String[] getOpticalRxPowerCandidatePaths(DataModelType type) {
        return new String[] {
            "Device.Optical.Interface.1.RxPower",
            "InternetGatewayDevice.WANDevice.1.WANDSLDiagnostics.RxPower",
            "InternetGatewayDevice.WANDevice.1.X_TP_GPON.RxPower",
            "InternetGatewayDevice.WANDevice.1.X_HW_PON.RxPower",
            "InternetGatewayDevice.WANDevice.1.X_ZTE_PON.RxPower",
            "InternetGatewayDevice.WANDevice.1.X_ZTE-COM_WANPONInterfaceConfig.RxPower",
            "InternetGatewayDevice.WANDevice.1.X_ZTE-COM_GponInterfaceConfig.RxPower",
            "InternetGatewayDevice.WANDevice.1.X_ZTE-COM_PON.RxPower",
            "Device.X_TP_PON.RxPower"
        };
    }

    public static String[] getOpticalTxPowerCandidatePaths(DataModelType type) {
        return new String[] {
            "Device.Optical.Interface.1.TxPower",
            "InternetGatewayDevice.WANDevice.1.WANDSLDiagnostics.TxPower",
            "InternetGatewayDevice.WANDevice.1.X_TP_GPON.TxPower",
            "InternetGatewayDevice.WANDevice.1.X_HW_PON.TxPower",
            "InternetGatewayDevice.WANDevice.1.X_ZTE_PON.TxPower",
            "InternetGatewayDevice.WANDevice.1.X_ZTE-COM_WANPONInterfaceConfig.TxPower",
            "InternetGatewayDevice.WANDevice.1.X_ZTE-COM_GponInterfaceConfig.TxPower",
            "InternetGatewayDevice.WANDevice.1.X_ZTE-COM_PON.TxPower",
            "Device.X_TP_PON.TxPower"
        };
    }
}
