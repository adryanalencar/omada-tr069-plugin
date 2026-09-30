package com.openomada.plugin.tr069.cwmp.rpc;

public final class CWMPConstants {
    private CWMPConstants() {}

    public static final String SOAP_ENV_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    public static final String SOAP_ENC_NS = "http://schemas.xmlsoap.org/soap/encoding/";
    public static final String XSD_NS = "http://www.w3.org/2001/XMLSchema";
    public static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";

    public static final String CWMP_1_0_NS = "urn:dslforum-org:cwmp-1-0";
    public static final String CWMP_1_1_NS = "urn:dslforum-org:cwmp-1-1";
    public static final String CWMP_1_2_NS = "urn:dslforum-org:cwmp-1-2";
    public static final String CWMP_1_4_NS = "urn:dslforum-org:cwmp-1-4";
    public static final String DEFAULT_CWMP_NS = CWMP_1_2_NS;

    // CWMP Event Codes
    public static final String EVENT_BOOTSTRAP = "0 BOOTSTRAP";
    public static final String EVENT_BOOT = "1 BOOT";
    public static final String EVENT_PERIODIC = "2 PERIODIC";
    public static final String EVENT_SCHEDULED = "3 SCHEDULED";
    public static final String EVENT_VALUE_CHANGE = "4 VALUE CHANGE";
    public static final String EVENT_KICKED = "5 KICKED";
    public static final String EVENT_CONNECTION_REQUEST = "6 CONNECTION REQUEST";
    public static final String EVENT_TRANSFER_COMPLETE = "7 TRANSFER COMPLETE";
    public static final String EVENT_DIAGNOSTICS_COMPLETE = "8 DIAGNOSTICS COMPLETE";
    public static final String EVENT_AUTONOMOUS_TRANSFER_COMPLETE = "10 AUTONOMOUS TRANSFER COMPLETE";

    // CWMP Fault Codes
    public static final int FAULT_METHOD_NOT_SUPPORTED = 9000;
    public static final int FAULT_REQUEST_DENIED = 9001;
    public static final int FAULT_INTERNAL_ERROR = 9002;
    public static final int FAULT_INVALID_ARGUMENTS = 9003;
    public static final int FAULT_RESOURCES_EXCEEDED = 9004;
    public static final int FAULT_INVALID_PARAMETER_NAME = 9005;
    public static final int FAULT_INVALID_PARAMETER_TYPE = 9006;
    public static final int FAULT_INVALID_PARAMETER_VALUE = 9007;
    public static final int FAULT_PARAMETER_READ_ONLY = 9008;
    public static final int FAULT_NOTIFICATION_REJECTED = 9009;
    public static final int FAULT_DOWNLOAD_FAILURE = 9010;
    public static final int FAULT_UPLOAD_FAILURE = 9011;
    public static final int FAULT_FILE_TRANSFER_SERVER_AUTH = 9012;
    public static final int FAULT_FILE_TRANSFER_UNSUPPORTED_PROTOCOL = 9013;
    public static final int FAULT_FILE_TRANSFER_CONTACT_FAIL = 9014;
    public static final int FAULT_FILE_TRANSFER_ACCESS_FAIL = 9015;
    public static final int FAULT_FILE_TRANSFER_INCOMPLETE = 9016;
    public static final int FAULT_FILE_TRANSFER_CORRUPTED = 9017;
    public static final int FAULT_FILE_TRANSFER_TIME_TIMEOUT = 9018;

    // HTTP & Headers
    public static final String HEADER_CWMP_ID = "ID";
    public static final String HEADER_CWMP_HOLD_REQUESTS = "HoldRequests";
    public static final String HEADER_CWMP_SESSION_TIMEOUT = "SessionTimeout";
}
