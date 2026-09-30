package com.openomada.plugin.tr069.cwmp.parser;

import com.openomada.plugin.tr069.cwmp.model.*;
import com.openomada.plugin.tr069.cwmp.rpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Enterprise-grade, XXE-safe, high-performance StAX parser for CWMP/TR-069 SOAP messages.
 * Handles any SOAP prefix, CWMP namespace variations (1-0 through 1-4), and malformed client payloads defensively.
 */
public class CWMPXmlParser {
    private static final Logger log = LoggerFactory.getLogger(CWMPXmlParser.class);

    private final XMLInputFactory inputFactory;

    public CWMPXmlParser() {
        this.inputFactory = createSecureInputFactory();
    }

    private static XMLInputFactory createSecureInputFactory() {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        try {
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
            factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.FALSE);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (IllegalArgumentException | UnsupportedOperationException e) {
            log.warn("Could not set all secure XMLInputFactory properties: {}", e.getMessage());
        }
        return factory;
    }

    public CWMPMessage parse(String xml) throws XMLStreamException {
        if (xml == null || xml.trim().isEmpty()) {
            return null;
        }
        if (xml.contains("<!DOCTYPE") || xml.contains("<!ENTITY") || xml.contains("<!doctype") || xml.contains("<!entity")) {
            throw new XMLStreamException("DOCTYPE and ENTITY declarations are strictly prohibited in CWMP messages (XXE defense)");
        }
        try {
            XMLStreamReader reader = inputFactory.createXMLStreamReader(new StringReader(xml));
            return parseDocument(reader);
        } catch (XMLStreamException e) {
            log.error("XML parse error on payload (preview: {}): {}", truncate(xml, 200), e.getMessage());
            throw e;
        }
    }

    public CWMPMessage parse(InputStream inputStream) throws XMLStreamException {
        if (inputStream == null) {
            return null;
        }
        XMLStreamReader reader = inputFactory.createXMLStreamReader(inputStream);
        return parseDocument(reader);
    }

    private CWMPMessage parseDocument(XMLStreamReader reader) throws XMLStreamException {
        String messageId = null;
        boolean holdRequests = false;
        Integer sessionTimeout = null;
        CWMPMessage parsedMessage = null;

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();

                if ("Header".equalsIgnoreCase(localName)) {
                    HeaderInfo headerInfo = parseHeader(reader);
                    messageId = headerInfo.id;
                    holdRequests = headerInfo.holdRequests;
                    sessionTimeout = headerInfo.sessionTimeout;
                } else if ("Body".equalsIgnoreCase(localName)) {
                    parsedMessage = parseBody(reader);
                }
            }
        }

        if (parsedMessage != null) {
            if (messageId != null && !messageId.isEmpty()) {
                parsedMessage.setId(messageId);
            }
            parsedMessage.setHoldRequests(holdRequests);
            if (sessionTimeout != null) {
                parsedMessage.setSessionTimeout(sessionTimeout);
            }
        }

        return parsedMessage;
    }

    private static class HeaderInfo {
        String id = "";
        boolean holdRequests = false;
        Integer sessionTimeout = null;
    }

    private HeaderInfo parseHeader(XMLStreamReader reader) throws XMLStreamException {
        HeaderInfo info = new HeaderInfo();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String name = reader.getLocalName();
                if ("ID".equalsIgnoreCase(name)) {
                    info.id = reader.getElementText().trim();
                } else if ("HoldRequests".equalsIgnoreCase(name)) {
                    String val = reader.getElementText().trim();
                    info.holdRequests = "1".equals(val) || "true".equalsIgnoreCase(val);
                } else if ("SessionTimeout".equalsIgnoreCase(name)) {
                    try {
                        info.sessionTimeout = Integer.parseInt(reader.getElementText().trim());
                    } catch (NumberFormatException ignored) {}
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "Header".equalsIgnoreCase(reader.getLocalName())) {
                break;
            }
        }
        return info;
    }

    private CWMPMessage parseBody(XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();
                return switch (localName) {
                    case "Inform" -> parseInform(reader);
                    case "InformResponse" -> parseInformResponse(reader);
                    case "GetParameterValues" -> parseGetParameterValues(reader);
                    case "GetParameterValuesResponse" -> parseGetParameterValuesResponse(reader);
                    case "SetParameterValues" -> parseSetParameterValues(reader);
                    case "SetParameterValuesResponse" -> parseSetParameterValuesResponse(reader);
                    case "GetParameterNames" -> parseGetParameterNames(reader);
                    case "GetParameterNamesResponse" -> parseGetParameterNamesResponse(reader);
                    case "SetParameterAttributes" -> parseSetParameterAttributes(reader);
                    case "SetParameterAttributesResponse" -> parseSetParameterAttributesResponse(reader);
                    case "GetParameterAttributes" -> parseGetParameterAttributes(reader);
                    case "GetParameterAttributesResponse" -> parseGetParameterAttributesResponse(reader);
                    case "AddObject" -> parseAddObject(reader);
                    case "AddObjectResponse" -> parseAddObjectResponse(reader);
                    case "DeleteObject" -> parseDeleteObject(reader);
                    case "DeleteObjectResponse" -> parseDeleteObjectResponse(reader);
                    case "Reboot" -> parseReboot(reader);
                    case "RebootResponse" -> parseRebootResponse(reader);
                    case "FactoryReset" -> parseFactoryReset(reader);
                    case "FactoryResetResponse" -> parseFactoryResetResponse(reader);
                    case "Download" -> parseDownload(reader);
                    case "DownloadResponse" -> parseDownloadResponse(reader);
                    case "Upload" -> parseUpload(reader);
                    case "UploadResponse" -> parseUploadResponse(reader);
                    case "ScheduleInform" -> parseScheduleInform(reader);
                    case "ScheduleInformResponse" -> parseScheduleInformResponse(reader);
                    case "TransferComplete" -> parseTransferComplete(reader);
                    case "TransferCompleteResponse" -> parseTransferCompleteResponse(reader);
                    case "AutonomousTransferComplete" -> parseAutonomousTransferComplete(reader);
                    case "AutonomousTransferCompleteResponse" -> parseAutonomousTransferCompleteResponse(reader);
                    case "Fault" -> parseSoapFault(reader);
                    default -> {
                        log.debug("Encountered unhandled or wrapper element in SOAP Body: {}", localName);
                        yield null;
                    }
                };
            } else if (event == XMLStreamConstants.END_ELEMENT && "Body".equalsIgnoreCase(reader.getLocalName())) {
                break;
            }
        }
        return null;
    }

    private GetParameterValues parseGetParameterValues(XMLStreamReader reader) throws XMLStreamException {
        GetParameterValues gpv = new GetParameterValues();
        List<String> names = new ArrayList<>();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("string".equalsIgnoreCase(reader.getLocalName())) {
                    names.add(reader.getElementText().trim());
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "GetParameterValues".equalsIgnoreCase(reader.getLocalName())) {
                break;
            }
        }
        gpv.setParameterNames(names);
        return gpv;
    }

    private SetParameterValues parseSetParameterValues(XMLStreamReader reader) throws XMLStreamException {
        SetParameterValues spv = new SetParameterValues();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String name = reader.getLocalName();
                if ("ParameterList".equalsIgnoreCase(name)) {
                    spv.setParameterList(parseParameterList(reader));
                } else if ("ParameterKey".equalsIgnoreCase(name)) {
                    spv.setParameterKey(reader.getElementText().trim());
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "SetParameterValues".equalsIgnoreCase(reader.getLocalName())) {
                break;
            }
        }
        return spv;
    }

    private GetParameterNames parseGetParameterNames(XMLStreamReader reader) throws XMLStreamException {
        GetParameterNames gpn = new GetParameterNames();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String name = reader.getLocalName();
                if ("ParameterPath".equalsIgnoreCase(name)) {
                    gpn.setParameterPath(reader.getElementText().trim());
                } else if ("NextLevel".equalsIgnoreCase(name)) {
                    String v = reader.getElementText().trim();
                    gpn.setNextLevel("1".equals(v) || "true".equalsIgnoreCase(v));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "GetParameterNames".equalsIgnoreCase(reader.getLocalName())) {
                break;
            }
        }
        return gpn;
    }

    private SetParameterAttributes parseSetParameterAttributes(XMLStreamReader reader) throws XMLStreamException {
        SetParameterAttributes spa = new SetParameterAttributes();
        List<SetParameterAttributes.ParameterAttributeChange> list = new ArrayList<>();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("SetParameterAttributesStruct".equalsIgnoreCase(reader.getLocalName())) {
                    SetParameterAttributes.ParameterAttributeChange pac = new SetParameterAttributes.ParameterAttributeChange();
                    while (reader.hasNext()) {
                        int e2 = reader.next();
                        if (e2 == XMLStreamConstants.START_ELEMENT) {
                            String n = reader.getLocalName();
                            if ("Name".equalsIgnoreCase(n)) {
                                pac.setName(reader.getElementText().trim());
                            } else if ("NotificationChange".equalsIgnoreCase(n)) {
                                String v = reader.getElementText().trim();
                                pac.setNotificationChange("1".equals(v) || "true".equalsIgnoreCase(v));
                            } else if ("Notification".equalsIgnoreCase(n)) {
                                try { pac.setNotification(Integer.parseInt(reader.getElementText().trim())); } catch (Exception ignored) {}
                            } else if ("AccessListChange".equalsIgnoreCase(n)) {
                                String v = reader.getElementText().trim();
                                pac.setAccessListChange("1".equals(v) || "true".equalsIgnoreCase(v));
                            } else if ("AccessList".equalsIgnoreCase(n)) {
                                pac.setAccessList(parseAccessList(reader));
                            }
                        } else if (e2 == XMLStreamConstants.END_ELEMENT && "SetParameterAttributesStruct".equalsIgnoreCase(reader.getLocalName())) {
                            list.add(pac);
                            break;
                        }
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "SetParameterAttributes".equalsIgnoreCase(reader.getLocalName())) {
                break;
            }
        }
        spa.setParameterList(list);
        return spa;
    }

    private GetParameterAttributes parseGetParameterAttributes(XMLStreamReader reader) throws XMLStreamException {
        GetParameterAttributes gpa = new GetParameterAttributes();
        List<String> names = new ArrayList<>();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("string".equalsIgnoreCase(reader.getLocalName())) {
                    names.add(reader.getElementText().trim());
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "GetParameterAttributes".equalsIgnoreCase(reader.getLocalName())) {
                break;
            }
        }
        gpa.setParameterNames(names);
        return gpa;
    }

    private AddObject parseAddObject(XMLStreamReader reader) throws XMLStreamException {
        AddObject ao = new AddObject();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String n = reader.getLocalName();
                if ("ObjectName".equalsIgnoreCase(n)) {
                    ao.setObjectName(reader.getElementText().trim());
                } else if ("ParameterKey".equalsIgnoreCase(n)) {
                    ao.setParameterKey(reader.getElementText().trim());
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "AddObject".equalsIgnoreCase(reader.getLocalName())) {
                break;
            }
        }
        return ao;
    }

    private DeleteObject parseDeleteObject(XMLStreamReader reader) throws XMLStreamException {
        DeleteObject d = new DeleteObject();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String n = reader.getLocalName();
                if ("ObjectName".equalsIgnoreCase(n)) {
                    d.setObjectName(reader.getElementText().trim());
                } else if ("ParameterKey".equalsIgnoreCase(n)) {
                    d.setParameterKey(reader.getElementText().trim());
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "DeleteObject".equalsIgnoreCase(reader.getLocalName())) {
                break;
            }
        }
        return d;
    }

    private Reboot parseReboot(XMLStreamReader reader) throws XMLStreamException {
        Reboot rb = new Reboot();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("CommandKey".equalsIgnoreCase(reader.getLocalName())) {
                    rb.setCommandKey(reader.getElementText().trim());
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "Reboot".equalsIgnoreCase(reader.getLocalName())) {
                break;
            }
        }
        return rb;
    }

    private FactoryReset parseFactoryReset(XMLStreamReader reader) throws XMLStreamException {
        drainUntilEnd(reader, "FactoryReset");
        return new FactoryReset();
    }

    private Download parseDownload(XMLStreamReader reader) throws XMLStreamException {
        Download dl = new Download();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String n = reader.getLocalName();
                switch (n) {
                    case "CommandKey" -> dl.setCommandKey(reader.getElementText().trim());
                    case "FileType" -> dl.setFileType(reader.getElementText().trim());
                    case "URL" -> dl.setUrl(reader.getElementText().trim());
                    case "Username" -> dl.setUsername(reader.getElementText().trim());
                    case "Password" -> dl.setPassword(reader.getElementText().trim());
                    case "FileSize" -> {
                        try { dl.setFileSize(Long.parseLong(reader.getElementText().trim())); } catch (Exception ignored) {}
                    }
                    case "TargetFileName" -> dl.setTargetFileName(reader.getElementText().trim());
                    case "DelaySeconds" -> {
                        try { dl.setDelaySeconds(Integer.parseInt(reader.getElementText().trim())); } catch (Exception ignored) {}
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "Download".equalsIgnoreCase(reader.getLocalName())) {
                break;
            }
        }
        return dl;
    }

    private Upload parseUpload(XMLStreamReader reader) throws XMLStreamException {
        Upload ul = new Upload();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String n = reader.getLocalName();
                switch (n) {
                    case "CommandKey" -> ul.setCommandKey(reader.getElementText().trim());
                    case "FileType" -> ul.setFileType(reader.getElementText().trim());
                    case "URL" -> ul.setUrl(reader.getElementText().trim());
                    case "Username" -> ul.setUsername(reader.getElementText().trim());
                    case "Password" -> ul.setPassword(reader.getElementText().trim());
                    case "DelaySeconds" -> {
                        try { ul.setDelaySeconds(Integer.parseInt(reader.getElementText().trim())); } catch (Exception ignored) {}
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "Upload".equalsIgnoreCase(reader.getLocalName())) {
                break;
            }
        }
        return ul;
    }

    private ScheduleInform parseScheduleInform(XMLStreamReader reader) throws XMLStreamException {
        ScheduleInform si = new ScheduleInform();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String n = reader.getLocalName();
                if ("DelaySeconds".equalsIgnoreCase(n)) {
                    try { si.setDelaySeconds(Integer.parseInt(reader.getElementText().trim())); } catch (Exception ignored) {}
                } else if ("CommandKey".equalsIgnoreCase(n)) {
                    si.setCommandKey(reader.getElementText().trim());
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "ScheduleInform".equalsIgnoreCase(reader.getLocalName())) {
                break;
            }
        }
        return si;
    }

    private Inform parseInform(XMLStreamReader reader) throws XMLStreamException {
        Inform inform = new Inform();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String name = reader.getLocalName();
                switch (name) {
                    case "DeviceId" -> inform.setDeviceId(parseDeviceId(reader));
                    case "Event" -> inform.setEvents(parseEvents(reader));
                    case "MaxEnvelopes" -> {
                        try {
                            inform.setMaxEnvelopes(Integer.parseInt(reader.getElementText().trim()));
                        } catch (Exception ignored) {}
                    }
                    case "CurrentTime" -> inform.setCurrentTime(reader.getElementText().trim());
                    case "RetryCount" -> {
                        try {
                            inform.setRetryCount(Integer.parseInt(reader.getElementText().trim()));
                        } catch (Exception ignored) {}
                    }
                    case "ParameterList" -> inform.setParameterList(parseParameterList(reader));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "Inform".equalsIgnoreCase(reader.getLocalName())) {
                break;
            }
        }
        return inform;
    }

    private DeviceId parseDeviceId(XMLStreamReader reader) throws XMLStreamException {
        DeviceId deviceId = new DeviceId();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String name = reader.getLocalName();
                switch (name) {
                    case "Manufacturer" -> deviceId.setManufacturer(reader.getElementText().trim());
                    case "OUI" -> deviceId.setOui(reader.getElementText().trim());
                    case "ProductClass" -> deviceId.setProductClass(reader.getElementText().trim());
                    case "SerialNumber" -> deviceId.setSerialNumber(reader.getElementText().trim());
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "DeviceId".equalsIgnoreCase(reader.getLocalName())) {
                break;
            }
        }
        return deviceId;
    }

    private List<EventStruct> parseEvents(XMLStreamReader reader) throws XMLStreamException {
        List<EventStruct> list = new ArrayList<>();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("EventStruct".equalsIgnoreCase(reader.getLocalName())) {
                    String eventCode = "";
                    String commandKey = "";
                    while (reader.hasNext()) {
                        int e2 = reader.next();
                        if (e2 == XMLStreamConstants.START_ELEMENT) {
                            String n2 = reader.getLocalName();
                            if ("EventCode".equalsIgnoreCase(n2)) {
                                eventCode = reader.getElementText().trim();
                            } else if ("CommandKey".equalsIgnoreCase(n2)) {
                                commandKey = reader.getElementText().trim();
                            }
                        } else if (e2 == XMLStreamConstants.END_ELEMENT && "EventStruct".equalsIgnoreCase(reader.getLocalName())) {
                            list.add(new EventStruct(eventCode, commandKey));
                            break;
                        }
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "Event".equalsIgnoreCase(reader.getLocalName())) {
                break;
            }
        }
        return list;
    }

    private List<ParameterValueStruct> parseParameterList(XMLStreamReader reader) throws XMLStreamException {
        List<ParameterValueStruct> list = new ArrayList<>();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("ParameterValueStruct".equalsIgnoreCase(reader.getLocalName())) {
                    String paramName = null;
                    String paramValue = "";
                    String paramType = "xsd:string";

                    while (reader.hasNext()) {
                        int e2 = reader.next();
                        if (e2 == XMLStreamConstants.START_ELEMENT) {
                            String n2 = reader.getLocalName();
                            if ("Name".equalsIgnoreCase(n2)) {
                                paramName = reader.getElementText().trim();
                            } else if ("Value".equalsIgnoreCase(n2)) {
                                for (int i = 0; i < reader.getAttributeCount(); i++) {
                                    if ("type".equalsIgnoreCase(reader.getAttributeLocalName(i))) {
                                        paramType = reader.getAttributeValue(i);
                                    }
                                }
                                paramValue = reader.getElementText().trim();
                            }
                        } else if (e2 == XMLStreamConstants.END_ELEMENT && "ParameterValueStruct".equalsIgnoreCase(reader.getLocalName())) {
                            if (paramName != null) {
                                list.add(new ParameterValueStruct(paramName, paramValue, paramType));
                            }
                            break;
                        }
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "ParameterList".equalsIgnoreCase(reader.getLocalName())) {
                break;
            }
        }
        return list;
    }

    private GetParameterValuesResponse parseGetParameterValuesResponse(XMLStreamReader reader) throws XMLStreamException {
        GetParameterValuesResponse response = new GetParameterValuesResponse();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT && "ParameterList".equalsIgnoreCase(reader.getLocalName())) {
                response.setParameterList(parseParameterList(reader));
            } else if (event == XMLStreamConstants.END_ELEMENT && "GetParameterValuesResponse".equalsIgnoreCase(reader.getLocalName())) {
                break;
            }
        }
        return response;
    }

    private SetParameterValuesResponse parseSetParameterValuesResponse(XMLStreamReader reader) throws XMLStreamException {
        SetParameterValuesResponse response = new SetParameterValuesResponse();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT && "Status".equalsIgnoreCase(reader.getLocalName())) {
                try {
                    response.setStatus(Integer.parseInt(reader.getElementText().trim()));
                } catch (Exception ignored) {}
            } else if (event == XMLStreamConstants.END_ELEMENT && "SetParameterValuesResponse".equalsIgnoreCase(reader.getLocalName())) {
                break;
            }
        }
        return response;
    }

    private GetParameterNamesResponse parseGetParameterNamesResponse(XMLStreamReader reader) throws XMLStreamException {
        GetParameterNamesResponse response = new GetParameterNamesResponse();
        List<ParameterInfoStruct> list = new ArrayList<>();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("ParameterInfoStruct".equalsIgnoreCase(reader.getLocalName())) {
                    String name = "";
                    boolean writable = false;
                    while (reader.hasNext()) {
                        int e2 = reader.next();
                        if (e2 == XMLStreamConstants.START_ELEMENT) {
                            String n2 = reader.getLocalName();
                            if ("Name".equalsIgnoreCase(n2)) {
                                name = reader.getElementText().trim();
                            } else if ("Writable".equalsIgnoreCase(n2)) {
                                String w = reader.getElementText().trim();
                                writable = "1".equals(w) || "true".equalsIgnoreCase(w);
                            }
                        } else if (e2 == XMLStreamConstants.END_ELEMENT && "ParameterInfoStruct".equalsIgnoreCase(reader.getLocalName())) {
                            list.add(new ParameterInfoStruct(name, writable));
                            break;
                        }
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "GetParameterNamesResponse".equalsIgnoreCase(reader.getLocalName())) {
                break;
            }
        }
        response.setParameterList(list);
        return response;
    }

    private GetParameterAttributesResponse parseGetParameterAttributesResponse(XMLStreamReader reader) throws XMLStreamException {
        GetParameterAttributesResponse response = new GetParameterAttributesResponse();
        List<ParameterAttributeStruct> list = new ArrayList<>();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("ParameterAttributeStruct".equalsIgnoreCase(reader.getLocalName())) {
                    String name = "";
                    int notification = 0;
                    List<String> accessList = new ArrayList<>();
                    while (reader.hasNext()) {
                        int e2 = reader.next();
                        if (e2 == XMLStreamConstants.START_ELEMENT) {
                            String n2 = reader.getLocalName();
                            if ("Name".equalsIgnoreCase(n2)) {
                                name = reader.getElementText().trim();
                            } else if ("Notification".equalsIgnoreCase(n2)) {
                                try {
                                    notification = Integer.parseInt(reader.getElementText().trim());
                                } catch (Exception ignored) {}
                            } else if ("AccessList".equalsIgnoreCase(n2)) {
                                accessList = parseAccessList(reader);
                            }
                        } else if (e2 == XMLStreamConstants.END_ELEMENT && "ParameterAttributeStruct".equalsIgnoreCase(reader.getLocalName())) {
                            list.add(new ParameterAttributeStruct(name, notification, accessList));
                            break;
                        }
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "GetParameterAttributesResponse".equalsIgnoreCase(reader.getLocalName())) {
                break;
            }
        }
        response.setParameterList(list);
        return response;
    }

    private List<String> parseAccessList(XMLStreamReader reader) throws XMLStreamException {
        List<String> list = new ArrayList<>();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("string".equalsIgnoreCase(reader.getLocalName())) {
                    list.add(reader.getElementText().trim());
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "AccessList".equalsIgnoreCase(reader.getLocalName())) {
                break;
            }
        }
        return list;
    }

    private AddObjectResponse parseAddObjectResponse(XMLStreamReader reader) throws XMLStreamException {
        AddObjectResponse response = new AddObjectResponse();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String name = reader.getLocalName();
                if ("InstanceNumber".equalsIgnoreCase(name)) {
                    try {
                        response.setInstanceNumber(Long.parseLong(reader.getElementText().trim()));
                    } catch (Exception ignored) {}
                } else if ("Status".equalsIgnoreCase(name)) {
                    try {
                        response.setStatus(Integer.parseInt(reader.getElementText().trim()));
                    } catch (Exception ignored) {}
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "AddObjectResponse".equalsIgnoreCase(reader.getLocalName())) {
                break;
            }
        }
        return response;
    }

    private DeleteObjectResponse parseDeleteObjectResponse(XMLStreamReader reader) throws XMLStreamException {
        DeleteObjectResponse response = new DeleteObjectResponse();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT && "Status".equalsIgnoreCase(reader.getLocalName())) {
                try {
                    response.setStatus(Integer.parseInt(reader.getElementText().trim()));
                } catch (Exception ignored) {}
            } else if (event == XMLStreamConstants.END_ELEMENT && "DeleteObjectResponse".equalsIgnoreCase(reader.getLocalName())) {
                break;
            }
        }
        return response;
    }

    private DownloadResponse parseDownloadResponse(XMLStreamReader reader) throws XMLStreamException {
        DownloadResponse response = new DownloadResponse();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String name = reader.getLocalName();
                if ("Status".equalsIgnoreCase(name)) {
                    try {
                        response.setStatus(Integer.parseInt(reader.getElementText().trim()));
                    } catch (Exception ignored) {}
                } else if ("StartTime".equalsIgnoreCase(name)) {
                    response.setStartTime(reader.getElementText().trim());
                } else if ("CompleteTime".equalsIgnoreCase(name)) {
                    response.setCompleteTime(reader.getElementText().trim());
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "DownloadResponse".equalsIgnoreCase(reader.getLocalName())) {
                break;
            }
        }
        return response;
    }

    private UploadResponse parseUploadResponse(XMLStreamReader reader) throws XMLStreamException {
        UploadResponse response = new UploadResponse();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String name = reader.getLocalName();
                if ("Status".equalsIgnoreCase(name)) {
                    try {
                        response.setStatus(Integer.parseInt(reader.getElementText().trim()));
                    } catch (Exception ignored) {}
                } else if ("StartTime".equalsIgnoreCase(name)) {
                    response.setStartTime(reader.getElementText().trim());
                } else if ("CompleteTime".equalsIgnoreCase(name)) {
                    response.setCompleteTime(reader.getElementText().trim());
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "UploadResponse".equalsIgnoreCase(reader.getLocalName())) {
                break;
            }
        }
        return response;
    }

    private TransferComplete parseTransferComplete(XMLStreamReader reader) throws XMLStreamException {
        TransferComplete tc = new TransferComplete();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String name = reader.getLocalName();
                if ("CommandKey".equalsIgnoreCase(name)) {
                    tc.setCommandKey(reader.getElementText().trim());
                } else if ("StartTime".equalsIgnoreCase(name)) {
                    tc.setStartTime(reader.getElementText().trim());
                } else if ("CompleteTime".equalsIgnoreCase(name)) {
                    tc.setCompleteTime(reader.getElementText().trim());
                } else if ("FaultStruct".equalsIgnoreCase(name)) {
                    tc.setFaultStruct(parseFaultStruct(reader));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "TransferComplete".equalsIgnoreCase(reader.getLocalName())) {
                break;
            }
        }
        return tc;
    }

    private AutonomousTransferComplete parseAutonomousTransferComplete(XMLStreamReader reader) throws XMLStreamException {
        AutonomousTransferComplete atc = new AutonomousTransferComplete();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String name = reader.getLocalName();
                switch (name) {
                    case "AnnounceURL" -> atc.setAnnounceUrl(reader.getElementText().trim());
                    case "TransferURL" -> atc.setTransferUrl(reader.getElementText().trim());
                    case "IsDownload" -> {
                        String v = reader.getElementText().trim();
                        atc.setDownload("1".equals(v) || "true".equalsIgnoreCase(v));
                    }
                    case "FileType" -> atc.setFileType(reader.getElementText().trim());
                    case "FileSize" -> {
                        try {
                            atc.setFileSize(Long.parseLong(reader.getElementText().trim()));
                        } catch (Exception ignored) {}
                    }
                    case "TargetFileName" -> atc.setTargetFileName(reader.getElementText().trim());
                    case "StartTime" -> atc.setStartTime(reader.getElementText().trim());
                    case "CompleteTime" -> atc.setCompleteTime(reader.getElementText().trim());
                    case "FaultStruct" -> atc.setFaultStruct(parseFaultStruct(reader));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "AutonomousTransferComplete".equalsIgnoreCase(reader.getLocalName())) {
                break;
            }
        }
        return atc;
    }

    private FaultStruct parseFaultStruct(XMLStreamReader reader) throws XMLStreamException {
        FaultStruct fs = new FaultStruct();
        List<SetParameterValuesFault> paramFaults = new ArrayList<>();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String name = reader.getLocalName();
                if ("FaultCode".equalsIgnoreCase(name)) {
                    try {
                        fs.setFaultCode(Integer.parseInt(reader.getElementText().trim()));
                    } catch (Exception ignored) {}
                } else if ("FaultString".equalsIgnoreCase(name)) {
                    fs.setFaultString(reader.getElementText().trim());
                } else if ("SetParameterValuesFault".equalsIgnoreCase(name)) {
                    paramFaults.add(parseSetParameterValuesFault(reader));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && ("FaultStruct".equalsIgnoreCase(reader.getLocalName()) || "Fault".equalsIgnoreCase(reader.getLocalName()))) {
                break;
            }
        }
        fs.setParameterFaults(paramFaults);
        return fs;
    }

    private SetParameterValuesFault parseSetParameterValuesFault(XMLStreamReader reader) throws XMLStreamException {
        SetParameterValuesFault pf = new SetParameterValuesFault();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String name = reader.getLocalName();
                if ("ParameterName".equalsIgnoreCase(name)) {
                    pf.setParameterName(reader.getElementText().trim());
                } else if ("FaultCode".equalsIgnoreCase(name)) {
                    try {
                        pf.setFaultCode(Integer.parseInt(reader.getElementText().trim()));
                    } catch (Exception ignored) {}
                } else if ("FaultString".equalsIgnoreCase(name)) {
                    pf.setFaultString(reader.getElementText().trim());
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "SetParameterValuesFault".equalsIgnoreCase(reader.getLocalName())) {
                break;
            }
        }
        return pf;
    }

    private CWMPFault parseSoapFault(XMLStreamReader reader) throws XMLStreamException {
        CWMPFault fault = new CWMPFault();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String name = reader.getLocalName();
                if ("faultcode".equalsIgnoreCase(name)) {
                    fault.setSoapFaultCode(reader.getElementText().trim());
                } else if ("faultstring".equalsIgnoreCase(name)) {
                    fault.setSoapFaultString(reader.getElementText().trim());
                } else if ("detail".equalsIgnoreCase(name)) {
                    while (reader.hasNext()) {
                        int de = reader.next();
                        if (de == XMLStreamConstants.START_ELEMENT && "Fault".equalsIgnoreCase(reader.getLocalName())) {
                            FaultStruct fs = parseFaultStruct(reader);
                            fault.setFaultCode(fs.getFaultCode());
                            fault.setFaultString(fs.getFaultString());
                            fault.setSetParameterValuesFaults(fs.getParameterFaults());
                            break;
                        } else if (de == XMLStreamConstants.END_ELEMENT && "detail".equalsIgnoreCase(reader.getLocalName())) {
                            break;
                        }
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "Fault".equalsIgnoreCase(reader.getLocalName())) {
                break;
            }
        }
        return fault;
    }

    private InformResponse parseInformResponse(XMLStreamReader reader) throws XMLStreamException {
        InformResponse res = new InformResponse();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT && "MaxEnvelopes".equalsIgnoreCase(reader.getLocalName())) {
                try {
                    res.setMaxEnvelopes(Integer.parseInt(reader.getElementText().trim()));
                } catch (Exception ignored) {}
            } else if (event == XMLStreamConstants.END_ELEMENT && "InformResponse".equalsIgnoreCase(reader.getLocalName())) {
                break;
            }
        }
        return res;
    }

    private SetParameterAttributesResponse parseSetParameterAttributesResponse(XMLStreamReader reader) throws XMLStreamException {
        drainUntilEnd(reader, "SetParameterAttributesResponse");
        return new SetParameterAttributesResponse();
    }

    private RebootResponse parseRebootResponse(XMLStreamReader reader) throws XMLStreamException {
        drainUntilEnd(reader, "RebootResponse");
        return new RebootResponse();
    }

    private FactoryResetResponse parseFactoryResetResponse(XMLStreamReader reader) throws XMLStreamException {
        drainUntilEnd(reader, "FactoryResetResponse");
        return new FactoryResetResponse();
    }

    private ScheduleInformResponse parseScheduleInformResponse(XMLStreamReader reader) throws XMLStreamException {
        drainUntilEnd(reader, "ScheduleInformResponse");
        return new ScheduleInformResponse();
    }

    private TransferCompleteResponse parseTransferCompleteResponse(XMLStreamReader reader) throws XMLStreamException {
        drainUntilEnd(reader, "TransferCompleteResponse");
        return new TransferCompleteResponse();
    }

    private AutonomousTransferCompleteResponse parseAutonomousTransferCompleteResponse(XMLStreamReader reader) throws XMLStreamException {
        drainUntilEnd(reader, "AutonomousTransferCompleteResponse");
        return new AutonomousTransferCompleteResponse();
    }

    private void drainUntilEnd(XMLStreamReader reader, String elementName) throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.END_ELEMENT && elementName.equalsIgnoreCase(reader.getLocalName())) {
                break;
            }
        }
    }

    private static String truncate(String text, int max) {
        if (text == null || text.length() <= max) return text;
        return text.substring(0, max) + "...";
    }
}
