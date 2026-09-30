package com.openomada.plugin.tr069.cwmp.serializer;

import com.openomada.plugin.tr069.cwmp.model.*;
import com.openomada.plugin.tr069.cwmp.rpc.*;

import java.util.List;

/**
 * Enterprise-grade serializer that generates standard TR-069 SOAP 1.1 envelopes
 * for all CWMP RPC requests, responses, and faults.
 */
public class CWMPXmlSerializer {

    public static final String SOAP_ENV_NS = CWMPConstants.SOAP_ENV_NS;
    public static final String SOAP_ENC_NS = CWMPConstants.SOAP_ENC_NS;
    public static final String XSD_NS = CWMPConstants.XSD_NS;
    public static final String XSI_NS = CWMPConstants.XSI_NS;
    public static final String CWMP_NS = CWMPConstants.CWMP_1_2_NS;

    public String serialize(CWMPMessage message) {
        if (message == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder(2048);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<soapenv:Envelope ");
        sb.append("xmlns:soapenv=\"").append(SOAP_ENV_NS).append("\" ");
        sb.append("xmlns:soapenc=\"").append(SOAP_ENC_NS).append("\" ");
        sb.append("xmlns:xsd=\"").append(XSD_NS).append("\" ");
        sb.append("xmlns:xsi=\"").append(XSI_NS).append("\" ");
        sb.append("xmlns:cwmp=\"").append(CWMP_NS).append("\">\n");

        // Header
        sb.append("  <soapenv:Header>\n");
        sb.append("    <cwmp:ID soapenv:mustUnderstand=\"1\">")
          .append(escapeXml(message.getId()))
          .append("</cwmp:ID>\n");
        if (message.isHoldRequests()) {
            sb.append("    <cwmp:HoldRequests soapenv:mustUnderstand=\"1\">1</cwmp:HoldRequests>\n");
        }
        if (message.getSessionTimeout() != null) {
            sb.append("    <cwmp:SessionTimeout soapenv:mustUnderstand=\"0\">")
              .append(message.getSessionTimeout())
              .append("</cwmp:SessionTimeout>\n");
        }
        sb.append("  </soapenv:Header>\n");

        // Body
        sb.append("  <soapenv:Body>\n");
        serializeBody(sb, message);
        sb.append("  </soapenv:Body>\n");

        sb.append("</soapenv:Envelope>\n");
        return sb.toString();
    }

    private void serializeBody(StringBuilder sb, CWMPMessage message) {
        if (message instanceof InformResponse ir) {
            serializeInformResponse(sb, ir);
        } else if (message instanceof GetParameterValues gpv) {
            serializeGetParameterValues(sb, gpv);
        } else if (message instanceof SetParameterValues spv) {
            serializeSetParameterValues(sb, spv);
        } else if (message instanceof GetParameterNames gpn) {
            serializeGetParameterNames(sb, gpn);
        } else if (message instanceof SetParameterAttributes spa) {
            serializeSetParameterAttributes(sb, spa);
        } else if (message instanceof GetParameterAttributes gpa) {
            serializeGetParameterAttributes(sb, gpa);
        } else if (message instanceof AddObject ao) {
            serializeAddObject(sb, ao);
        } else if (message instanceof DeleteObject doMsg) {
            serializeDeleteObject(sb, doMsg);
        } else if (message instanceof Reboot rb) {
            serializeReboot(sb, rb);
        } else if (message instanceof FactoryReset fr) {
            serializeFactoryReset(sb, fr);
        } else if (message instanceof Download dl) {
            serializeDownload(sb, dl);
        } else if (message instanceof Upload ul) {
            serializeUpload(sb, ul);
        } else if (message instanceof ScheduleInform si) {
            serializeScheduleInform(sb, si);
        } else if (message instanceof TransferCompleteResponse tcr) {
            serializeTransferCompleteResponse(sb, tcr);
        } else if (message instanceof AutonomousTransferCompleteResponse atcr) {
            serializeAutonomousTransferCompleteResponse(sb, atcr);
        } else if (message instanceof CWMPFault fault) {
            serializeCWMPFault(sb, fault);
        } else if (message instanceof Inform inf) {
            serializeInform(sb, inf);
        } else if (message instanceof GetParameterValuesResponse gpvr) {
            serializeGetParameterValuesResponse(sb, gpvr);
        } else if (message instanceof SetParameterValuesResponse spvr) {
            serializeSetParameterValuesResponse(sb, spvr);
        } else if (message instanceof GetParameterNamesResponse gpnr) {
            serializeGetParameterNamesResponse(sb, gpnr);
        } else if (message instanceof SetParameterAttributesResponse spar) {
            serializeSetParameterAttributesResponse(sb, spar);
        } else if (message instanceof GetParameterAttributesResponse gpar) {
            serializeGetParameterAttributesResponse(sb, gpar);
        } else if (message instanceof AddObjectResponse aor) {
            serializeAddObjectResponse(sb, aor);
        } else if (message instanceof DeleteObjectResponse dor) {
            serializeDeleteObjectResponse(sb, dor);
        } else if (message instanceof RebootResponse rbr) {
            serializeRebootResponse(sb, rbr);
        } else if (message instanceof FactoryResetResponse frr) {
            serializeFactoryResetResponse(sb, frr);
        } else if (message instanceof DownloadResponse dlr) {
            serializeDownloadResponse(sb, dlr);
        } else if (message instanceof UploadResponse ulr) {
            serializeUploadResponse(sb, ulr);
        } else if (message instanceof ScheduleInformResponse sir) {
            serializeScheduleInformResponse(sb, sir);
        } else if (message instanceof TransferComplete tc) {
            serializeTransferComplete(sb, tc);
        } else if (message instanceof AutonomousTransferComplete atc) {
            serializeAutonomousTransferComplete(sb, atc);
        } else {
            sb.append("    <cwmp:").append(message.getRpcName()).append(" />\n");
        }
    }

    private void serializeInformResponse(StringBuilder sb, InformResponse ir) {
        sb.append("    <cwmp:InformResponse>\n");
        sb.append("      <MaxEnvelopes>").append(ir.getMaxEnvelopes()).append("</MaxEnvelopes>\n");
        sb.append("    </cwmp:InformResponse>\n");
    }

    private void serializeGetParameterValues(StringBuilder sb, GetParameterValues gpv) {
        sb.append("    <cwmp:GetParameterValues>\n");
        List<String> names = gpv.getParameterNames();
        sb.append("      <ParameterNames soapenc:arrayType=\"xsd:string[").append(names.size()).append("]\">\n");
        for (String name : names) {
            sb.append("        <string>").append(escapeXml(name)).append("</string>\n");
        }
        sb.append("      </ParameterNames>\n");
        sb.append("    </cwmp:GetParameterValues>\n");
    }

    private void serializeSetParameterValues(StringBuilder sb, SetParameterValues spv) {
        sb.append("    <cwmp:SetParameterValues>\n");
        List<ParameterValueStruct> params = spv.getParameterList();
        sb.append("      <ParameterList soapenc:arrayType=\"cwmp:ParameterValueStruct[").append(params.size()).append("]\">\n");
        for (ParameterValueStruct p : params) {
            sb.append("        <ParameterValueStruct>\n");
            sb.append("          <Name>").append(escapeXml(p.getName())).append("</Name>\n");
            String type = p.getType() != null && !p.getType().isEmpty() ? p.getType() : "xsd:string";
            sb.append("          <Value xsi:type=\"").append(escapeXml(type)).append("\">")
              .append(escapeXml(p.getValue()))
              .append("</Value>\n");
            sb.append("        </ParameterValueStruct>\n");
        }
        sb.append("      </ParameterList>\n");
        sb.append("      <ParameterKey>").append(escapeXml(spv.getParameterKey())).append("</ParameterKey>\n");
        sb.append("    </cwmp:SetParameterValues>\n");
    }

    private void serializeGetParameterNames(StringBuilder sb, GetParameterNames gpn) {
        sb.append("    <cwmp:GetParameterNames>\n");
        sb.append("      <ParameterPath>").append(escapeXml(gpn.getParameterPath())).append("</ParameterPath>\n");
        sb.append("      <NextLevel>").append(gpn.isNextLevel() ? "1" : "0").append("</NextLevel>\n");
        sb.append("    </cwmp:GetParameterNames>\n");
    }

    private void serializeSetParameterAttributes(StringBuilder sb, SetParameterAttributes spa) {
        sb.append("    <cwmp:SetParameterAttributes>\n");
        List<SetParameterAttributes.ParameterAttributeChange> list = spa.getParameterList();
        sb.append("      <ParameterList soapenc:arrayType=\"cwmp:SetParameterAttributesStruct[").append(list.size()).append("]\">\n");
        for (SetParameterAttributes.ParameterAttributeChange p : list) {
            sb.append("        <SetParameterAttributesStruct>\n");
            sb.append("          <Name>").append(escapeXml(p.getName())).append("</Name>\n");
            sb.append("          <NotificationChange>").append(p.isNotificationChange() ? "1" : "0").append("</NotificationChange>\n");
            sb.append("          <Notification>").append(p.getNotification()).append("</Notification>\n");
            sb.append("          <AccessListChange>").append(p.isAccessListChange() ? "1" : "0").append("</AccessListChange>\n");
            List<String> acc = p.getAccessList();
            sb.append("          <AccessList soapenc:arrayType=\"xsd:string[").append(acc.size()).append("]\">\n");
            for (String a : acc) {
                sb.append("            <string>").append(escapeXml(a)).append("</string>\n");
            }
            sb.append("          </AccessList>\n");
            sb.append("        </SetParameterAttributesStruct>\n");
        }
        sb.append("      </ParameterList>\n");
        sb.append("    </cwmp:SetParameterAttributes>\n");
    }

    private void serializeGetParameterAttributes(StringBuilder sb, GetParameterAttributes gpa) {
        sb.append("    <cwmp:GetParameterAttributes>\n");
        List<String> names = gpa.getParameterNames();
        sb.append("      <ParameterNames soapenc:arrayType=\"xsd:string[").append(names.size()).append("]\">\n");
        for (String n : names) {
            sb.append("        <string>").append(escapeXml(n)).append("</string>\n");
        }
        sb.append("      </ParameterNames>\n");
        sb.append("    </cwmp:GetParameterAttributes>\n");
    }

    private void serializeAddObject(StringBuilder sb, AddObject ao) {
        sb.append("    <cwmp:AddObject>\n");
        sb.append("      <ObjectName>").append(escapeXml(ao.getObjectName())).append("</ObjectName>\n");
        sb.append("      <ParameterKey>").append(escapeXml(ao.getParameterKey())).append("</ParameterKey>\n");
        sb.append("    </cwmp:AddObject>\n");
    }

    private void serializeDeleteObject(StringBuilder sb, DeleteObject doMsg) {
        sb.append("    <cwmp:DeleteObject>\n");
        sb.append("      <ObjectName>").append(escapeXml(doMsg.getObjectName())).append("</ObjectName>\n");
        sb.append("      <ParameterKey>").append(escapeXml(doMsg.getParameterKey())).append("</ParameterKey>\n");
        sb.append("    </cwmp:DeleteObject>\n");
    }

    private void serializeReboot(StringBuilder sb, Reboot rb) {
        sb.append("    <cwmp:Reboot>\n");
        sb.append("      <CommandKey>").append(escapeXml(rb.getCommandKey())).append("</CommandKey>\n");
        sb.append("    </cwmp:Reboot>\n");
    }

    private void serializeFactoryReset(StringBuilder sb, FactoryReset fr) {
        sb.append("    <cwmp:FactoryReset />\n");
    }

    private void serializeDownload(StringBuilder sb, Download dl) {
        sb.append("    <cwmp:Download>\n");
        sb.append("      <CommandKey>").append(escapeXml(dl.getCommandKey())).append("</CommandKey>\n");
        sb.append("      <FileType>").append(escapeXml(dl.getFileType())).append("</FileType>\n");
        sb.append("      <URL>").append(escapeXml(dl.getUrl())).append("</URL>\n");
        sb.append("      <Username>").append(escapeXml(dl.getUsername())).append("</Username>\n");
        sb.append("      <Password>").append(escapeXml(dl.getPassword())).append("</Password>\n");
        sb.append("      <FileSize>").append(dl.getFileSize()).append("</FileSize>\n");
        sb.append("      <TargetFileName>").append(escapeXml(dl.getTargetFileName())).append("</TargetFileName>\n");
        sb.append("      <DelaySeconds>").append(dl.getDelaySeconds()).append("</DelaySeconds>\n");
        sb.append("      <SuccessURL>").append(escapeXml(dl.getSuccessUrl())).append("</SuccessURL>\n");
        sb.append("      <FailureURL>").append(escapeXml(dl.getFailureUrl())).append("</FailureURL>\n");
        sb.append("    </cwmp:Download>\n");
    }

    private void serializeUpload(StringBuilder sb, Upload ul) {
        sb.append("    <cwmp:Upload>\n");
        sb.append("      <CommandKey>").append(escapeXml(ul.getCommandKey())).append("</CommandKey>\n");
        sb.append("      <FileType>").append(escapeXml(ul.getFileType())).append("</FileType>\n");
        sb.append("      <URL>").append(escapeXml(ul.getUrl())).append("</URL>\n");
        sb.append("      <Username>").append(escapeXml(ul.getUsername())).append("</Username>\n");
        sb.append("      <Password>").append(escapeXml(ul.getPassword())).append("</Password>\n");
        sb.append("      <DelaySeconds>").append(ul.getDelaySeconds()).append("</DelaySeconds>\n");
        sb.append("    </cwmp:Upload>\n");
    }

    private void serializeScheduleInform(StringBuilder sb, ScheduleInform si) {
        sb.append("    <cwmp:ScheduleInform>\n");
        sb.append("      <DelaySeconds>").append(si.getDelaySeconds()).append("</DelaySeconds>\n");
        sb.append("      <CommandKey>").append(escapeXml(si.getCommandKey())).append("</CommandKey>\n");
        sb.append("    </cwmp:ScheduleInform>\n");
    }

    private void serializeTransferCompleteResponse(StringBuilder sb, TransferCompleteResponse tcr) {
        sb.append("    <cwmp:TransferCompleteResponse />\n");
    }

    private void serializeAutonomousTransferCompleteResponse(StringBuilder sb, AutonomousTransferCompleteResponse atcr) {
        sb.append("    <cwmp:AutonomousTransferCompleteResponse />\n");
    }

    private void serializeCWMPFault(StringBuilder sb, CWMPFault fault) {
        sb.append("    <soapenv:Fault>\n");
        sb.append("      <faultcode>").append(escapeXml(fault.getSoapFaultCode())).append("</faultcode>\n");
        sb.append("      <faultstring>").append(escapeXml(fault.getSoapFaultString())).append("</faultstring>\n");
        sb.append("      <detail>\n");
        sb.append("        <cwmp:Fault>\n");
        sb.append("          <FaultCode>").append(fault.getFaultCode()).append("</FaultCode>\n");
        sb.append("          <FaultString>").append(escapeXml(fault.getFaultString())).append("</FaultString>\n");
        if (fault.getSetParameterValuesFaults() != null && !fault.getSetParameterValuesFaults().isEmpty()) {
            for (SetParameterValuesFault pf : fault.getSetParameterValuesFaults()) {
                sb.append("          <SetParameterValuesFault>\n");
                sb.append("            <ParameterName>").append(escapeXml(pf.getParameterName())).append("</ParameterName>\n");
                sb.append("            <FaultCode>").append(pf.getFaultCode()).append("</FaultCode>\n");
                sb.append("            <FaultString>").append(escapeXml(pf.getFaultString())).append("</FaultString>\n");
                sb.append("          </SetParameterValuesFault>\n");
            }
        }
        sb.append("        </cwmp:Fault>\n");
        sb.append("      </detail>\n");
        sb.append("    </soapenv:Fault>\n");
    }

    private void serializeInform(StringBuilder sb, Inform inf) {
        sb.append("    <cwmp:Inform>\n");
        if (inf.getDeviceId() != null) {
            DeviceId d = inf.getDeviceId();
            sb.append("      <DeviceId>\n");
            sb.append("        <Manufacturer>").append(escapeXml(d.getManufacturer())).append("</Manufacturer>\n");
            sb.append("        <OUI>").append(escapeXml(d.getOui())).append("</OUI>\n");
            sb.append("        <ProductClass>").append(escapeXml(d.getProductClass())).append("</ProductClass>\n");
            sb.append("        <SerialNumber>").append(escapeXml(d.getSerialNumber())).append("</SerialNumber>\n");
            sb.append("      </DeviceId>\n");
        }
        List<EventStruct> events = inf.getEvents();
        sb.append("      <Event soapenc:arrayType=\"cwmp:EventStruct[").append(events.size()).append("]\">\n");
        for (EventStruct es : events) {
            sb.append("        <EventStruct>\n");
            sb.append("          <EventCode>").append(escapeXml(es.getEventCode())).append("</EventCode>\n");
            sb.append("          <CommandKey>").append(escapeXml(es.getCommandKey())).append("</CommandKey>\n");
            sb.append("        </EventStruct>\n");
        }
        sb.append("      </Event>\n");
        sb.append("      <MaxEnvelopes>").append(inf.getMaxEnvelopes()).append("</MaxEnvelopes>\n");
        sb.append("      <CurrentTime>").append(escapeXml(inf.getCurrentTime())).append("</CurrentTime>\n");
        sb.append("      <RetryCount>").append(inf.getRetryCount()).append("</RetryCount>\n");
        List<ParameterValueStruct> params = inf.getParameterList();
        sb.append("      <ParameterList soapenc:arrayType=\"cwmp:ParameterValueStruct[").append(params.size()).append("]\">\n");
        for (ParameterValueStruct p : params) {
            sb.append("        <ParameterValueStruct>\n");
            sb.append("          <Name>").append(escapeXml(p.getName())).append("</Name>\n");
            sb.append("          <Value xsi:type=\"").append(escapeXml(p.getType())).append("\">")
              .append(escapeXml(p.getValue()))
              .append("</Value>\n");
            sb.append("        </ParameterValueStruct>\n");
        }
        sb.append("      </ParameterList>\n");
        sb.append("    </cwmp:Inform>\n");
    }

    private void serializeGetParameterValuesResponse(StringBuilder sb, GetParameterValuesResponse gpvr) {
        sb.append("    <cwmp:GetParameterValuesResponse>\n");
        List<ParameterValueStruct> list = gpvr.getParameterList();
        sb.append("      <ParameterList soapenc:arrayType=\"cwmp:ParameterValueStruct[").append(list.size()).append("]\">\n");
        for (ParameterValueStruct p : list) {
            sb.append("        <ParameterValueStruct>\n");
            sb.append("          <Name>").append(escapeXml(p.getName())).append("</Name>\n");
            sb.append("          <Value xsi:type=\"").append(escapeXml(p.getType())).append("\">")
              .append(escapeXml(p.getValue()))
              .append("</Value>\n");
            sb.append("        </ParameterValueStruct>\n");
        }
        sb.append("      </ParameterList>\n");
        sb.append("    </cwmp:GetParameterValuesResponse>\n");
    }

    private void serializeSetParameterValuesResponse(StringBuilder sb, SetParameterValuesResponse spvr) {
        sb.append("    <cwmp:SetParameterValuesResponse>\n");
        sb.append("      <Status>").append(spvr.getStatus()).append("</Status>\n");
        sb.append("    </cwmp:SetParameterValuesResponse>\n");
    }

    private void serializeGetParameterNamesResponse(StringBuilder sb, GetParameterNamesResponse gpnr) {
        sb.append("    <cwmp:GetParameterNamesResponse>\n");
        List<ParameterInfoStruct> list = gpnr.getParameterList();
        sb.append("      <ParameterList soapenc:arrayType=\"cwmp:ParameterInfoStruct[").append(list.size()).append("]\">\n");
        for (ParameterInfoStruct p : list) {
            sb.append("        <ParameterInfoStruct>\n");
            sb.append("          <Name>").append(escapeXml(p.getName())).append("</Name>\n");
            sb.append("          <Writable>").append(p.isWritable() ? "1" : "0").append("</Writable>\n");
            sb.append("        </ParameterInfoStruct>\n");
        }
        sb.append("      </ParameterList>\n");
        sb.append("    </cwmp:GetParameterNamesResponse>\n");
    }

    private void serializeSetParameterAttributesResponse(StringBuilder sb, SetParameterAttributesResponse spar) {
        sb.append("    <cwmp:SetParameterAttributesResponse />\n");
    }

    private void serializeGetParameterAttributesResponse(StringBuilder sb, GetParameterAttributesResponse gpar) {
        sb.append("    <cwmp:GetParameterAttributesResponse>\n");
        List<ParameterAttributeStruct> list = gpar.getParameterList();
        sb.append("      <ParameterList soapenc:arrayType=\"cwmp:ParameterAttributeStruct[").append(list.size()).append("]\">\n");
        for (ParameterAttributeStruct p : list) {
            sb.append("        <ParameterAttributeStruct>\n");
            sb.append("          <Name>").append(escapeXml(p.getName())).append("</Name>\n");
            sb.append("          <Notification>").append(p.getNotification()).append("</Notification>\n");
            List<String> acc = p.getAccessList();
            sb.append("          <AccessList soapenc:arrayType=\"xsd:string[").append(acc.size()).append("]\">\n");
            for (String a : acc) {
                sb.append("            <string>").append(escapeXml(a)).append("</string>\n");
            }
            sb.append("          </AccessList>\n");
            sb.append("        </ParameterAttributeStruct>\n");
        }
        sb.append("      </ParameterList>\n");
        sb.append("    </cwmp:GetParameterAttributesResponse>\n");
    }

    private void serializeAddObjectResponse(StringBuilder sb, AddObjectResponse aor) {
        sb.append("    <cwmp:AddObjectResponse>\n");
        sb.append("      <InstanceNumber>").append(aor.getInstanceNumber()).append("</InstanceNumber>\n");
        sb.append("      <Status>").append(aor.getStatus()).append("</Status>\n");
        sb.append("    </cwmp:AddObjectResponse>\n");
    }

    private void serializeDeleteObjectResponse(StringBuilder sb, DeleteObjectResponse dor) {
        sb.append("    <cwmp:DeleteObjectResponse>\n");
        sb.append("      <Status>").append(dor.getStatus()).append("</Status>\n");
        sb.append("    </cwmp:DeleteObjectResponse>\n");
    }

    private void serializeRebootResponse(StringBuilder sb, RebootResponse rbr) {
        sb.append("    <cwmp:RebootResponse />\n");
    }

    private void serializeFactoryResetResponse(StringBuilder sb, FactoryResetResponse frr) {
        sb.append("    <cwmp:FactoryResetResponse />\n");
    }

    private void serializeDownloadResponse(StringBuilder sb, DownloadResponse dlr) {
        sb.append("    <cwmp:DownloadResponse>\n");
        sb.append("      <Status>").append(dlr.getStatus()).append("</Status>\n");
        sb.append("      <StartTime>").append(escapeXml(dlr.getStartTime())).append("</StartTime>\n");
        sb.append("      <CompleteTime>").append(escapeXml(dlr.getCompleteTime())).append("</CompleteTime>\n");
        sb.append("    </cwmp:DownloadResponse>\n");
    }

    private void serializeUploadResponse(StringBuilder sb, UploadResponse ulr) {
        sb.append("    <cwmp:UploadResponse>\n");
        sb.append("      <Status>").append(ulr.getStatus()).append("</Status>\n");
        sb.append("      <StartTime>").append(escapeXml(ulr.getStartTime())).append("</StartTime>\n");
        sb.append("      <CompleteTime>").append(escapeXml(ulr.getCompleteTime())).append("</CompleteTime>\n");
        sb.append("    </cwmp:UploadResponse>\n");
    }

    private void serializeScheduleInformResponse(StringBuilder sb, ScheduleInformResponse sir) {
        sb.append("    <cwmp:ScheduleInformResponse />\n");
    }

    private void serializeTransferComplete(StringBuilder sb, TransferComplete tc) {
        sb.append("    <cwmp:TransferComplete>\n");
        sb.append("      <CommandKey>").append(escapeXml(tc.getCommandKey())).append("</CommandKey>\n");
        if (tc.getFaultStruct() != null) {
            sb.append("      <FaultStruct>\n");
            sb.append("        <FaultCode>").append(tc.getFaultStruct().getFaultCode()).append("</FaultCode>\n");
            sb.append("        <FaultString>").append(escapeXml(tc.getFaultStruct().getFaultString())).append("</FaultString>\n");
            sb.append("      </FaultStruct>\n");
        } else {
            sb.append("      <FaultStruct>\n");
            sb.append("        <FaultCode>0</FaultCode>\n");
            sb.append("        <FaultString></FaultString>\n");
            sb.append("      </FaultStruct>\n");
        }
        sb.append("      <StartTime>").append(escapeXml(tc.getStartTime())).append("</StartTime>\n");
        sb.append("      <CompleteTime>").append(escapeXml(tc.getCompleteTime())).append("</CompleteTime>\n");
        sb.append("    </cwmp:TransferComplete>\n");
    }

    private void serializeAutonomousTransferComplete(StringBuilder sb, AutonomousTransferComplete atc) {
        sb.append("    <cwmp:AutonomousTransferComplete>\n");
        sb.append("      <AnnounceURL>").append(escapeXml(atc.getAnnounceUrl())).append("</AnnounceURL>\n");
        sb.append("      <TransferURL>").append(escapeXml(atc.getTransferUrl())).append("</TransferURL>\n");
        sb.append("      <IsDownload>").append(atc.isDownload() ? "1" : "0").append("</IsDownload>\n");
        sb.append("      <FileType>").append(escapeXml(atc.getFileType())).append("</FileType>\n");
        sb.append("      <FileSize>").append(atc.getFileSize()).append("</FileSize>\n");
        sb.append("      <TargetFileName>").append(escapeXml(atc.getTargetFileName())).append("</TargetFileName>\n");
        if (atc.getFaultStruct() != null) {
            sb.append("      <FaultStruct>\n");
            sb.append("        <FaultCode>").append(atc.getFaultStruct().getFaultCode()).append("</FaultCode>\n");
            sb.append("        <FaultString>").append(escapeXml(atc.getFaultStruct().getFaultString())).append("</FaultString>\n");
            sb.append("      </FaultStruct>\n");
        } else {
            sb.append("      <FaultStruct>\n");
            sb.append("        <FaultCode>0</FaultCode>\n");
            sb.append("        <FaultString></FaultString>\n");
            sb.append("      </FaultStruct>\n");
        }
        sb.append("      <StartTime>").append(escapeXml(atc.getStartTime())).append("</StartTime>\n");
        sb.append("      <CompleteTime>").append(escapeXml(atc.getCompleteTime())).append("</CompleteTime>\n");
        sb.append("    </cwmp:AutonomousTransferComplete>\n");
    }

    public static String escapeXml(String str) {
        if (str == null) return "";
        StringBuilder sb = new StringBuilder(str.length() + 16);
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '&' -> sb.append("&amp;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&apos;");
                default -> {
                    if (c < 0x20 && c != '\t' && c != '\r' && c != '\n') {
                        // ignore or drop invalid XML control characters
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
