package com.openomada.plugin.tr069.cwmp;

import com.openomada.plugin.tr069.cwmp.model.*;
import com.openomada.plugin.tr069.cwmp.parser.CWMPXmlParser;
import com.openomada.plugin.tr069.cwmp.rpc.*;
import com.openomada.plugin.tr069.cwmp.serializer.CWMPXmlSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CWMPXmlRoundtripTest {

    private CWMPXmlParser parser;
    private CWMPXmlSerializer serializer;

    @BeforeEach
    void setUp() {
        parser = new CWMPXmlParser();
        serializer = new CWMPXmlSerializer();
    }

    @Test
    @DisplayName("Serialize and parse Inform message")
    void testInformRoundtrip() throws Exception {
        Inform inform = new Inform("urn:uuid:msg-1234");
        inform.setDeviceId(new DeviceId("TP-Link", "00259E", "Archer-C6", "SN123456789"));
        inform.setEvents(List.of(new EventStruct("1 BOOT", ""), new EventStruct("2 PERIODIC", "")));
        inform.setCurrentTime("2026-09-28T04:00:00Z");
        inform.setRetryCount(0);
        inform.setMaxEnvelopes(1);
        inform.setParameterList(List.of(
                new ParameterValueStruct("Device.DeviceInfo.SoftwareVersion", "1.2.3", "xsd:string"),
                new ParameterValueStruct("Device.DeviceInfo.UpTime", "3600", "xsd:unsignedInt")
        ));

        String xml = serializer.serialize(inform);
        assertNotNull(xml);
        assertTrue(xml.contains("<cwmp:Inform>"));
        assertTrue(xml.contains("<OUI>00259E</OUI>"));
        assertTrue(xml.contains("<EventCode>1 BOOT</EventCode>"));

        CWMPMessage parsed = parser.parse(xml);
        assertNotNull(parsed);
        assertInstanceOf(Inform.class, parsed);
        Inform parsedInform = (Inform) parsed;

        assertEquals("urn:uuid:msg-1234", parsedInform.getId());
        assertEquals("TP-Link", parsedInform.getDeviceId().getManufacturer());
        assertEquals("00259E", parsedInform.getDeviceId().getOui());
        assertEquals("SN123456789", parsedInform.getDeviceId().getSerialNumber());
        assertEquals(2, parsedInform.getEvents().size());
        assertEquals("1 BOOT", parsedInform.getEvents().get(0).getEventCode());
        assertEquals(2, parsedInform.getParameterList().size());
        assertEquals("Device.DeviceInfo.SoftwareVersion", parsedInform.getParameterList().get(0).getName());
        assertEquals("1.2.3", parsedInform.getParameterList().get(0).getValue());
    }

    @Test
    @DisplayName("Serialize and parse GetParameterValues and Response")
    void testGetParameterValuesRoundtrip() throws Exception {
        GetParameterValues gpv = new GetParameterValues("msg-gpv-1");
        gpv.setParameterNames(List.of("InternetGatewayDevice.DeviceInfo.SoftwareVersion", "InternetGatewayDevice.WANDevice.1."));

        String xml = serializer.serialize(gpv);
        assertTrue(xml.contains("<cwmp:GetParameterValues>"));

        GetParameterValuesResponse resp = new GetParameterValuesResponse("msg-gpv-1");
        resp.setParameterList(List.of(
                new ParameterValueStruct("InternetGatewayDevice.DeviceInfo.SoftwareVersion", "V2.0.1", "xsd:string")
        ));
        String respXml = serializer.serialize(resp);
        CWMPMessage parsedResp = parser.parse(respXml);
        assertInstanceOf(GetParameterValuesResponse.class, parsedResp);
        GetParameterValuesResponse gpvResp = (GetParameterValuesResponse) parsedResp;
        assertEquals(1, gpvResp.getParameterList().size());
        assertEquals("V2.0.1", gpvResp.getParameterList().get(0).getValue());
    }

    @Test
    @DisplayName("Serialize and parse CWMPFault")
    void testCWMPFaultParsing() throws Exception {
        CWMPFault fault = new CWMPFault("msg-fault-1", 9005, "Invalid parameter name");
        fault.setSetParameterValuesFaults(List.of(
                new SetParameterValuesFault("Device.Invalid.Param", 9005, "Invalid parameter name")
        ));

        String xml = serializer.serialize(fault);
        assertTrue(xml.contains("<soapenv:Fault>"));
        assertTrue(xml.contains("<FaultCode>9005</FaultCode>"));

        CWMPMessage parsed = parser.parse(xml);
        assertInstanceOf(CWMPFault.class, parsed);
        CWMPFault parsedFault = (CWMPFault) parsed;
        assertEquals(9005, parsedFault.getFaultCode());
        assertEquals("Invalid parameter name", parsedFault.getFaultString());
        assertEquals(1, parsedFault.getSetParameterValuesFaults().size());
        assertEquals("Device.Invalid.Param", parsedFault.getSetParameterValuesFaults().get(0).getParameterName());
    }

    @Test
    @DisplayName("XXE Injection safety test")
    void testXxeDefense() {
        String xxePayload = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE foo [ <!ENTITY xxe SYSTEM "file:///etc/passwd"> ]>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:cwmp="urn:dslforum-org:cwmp-1-2">
                    <soapenv:Header><cwmp:ID>1</cwmp:ID></soapenv:Header>
                    <soapenv:Body>
                        <cwmp:Inform>
                            <CurrentTime>&xxe;</CurrentTime>
                        </cwmp:Inform>
                    </soapenv:Body>
                </soapenv:Envelope>
                """;
        // DTD is disabled in parser, so it should throw XMLStreamException or ignore DTD
        assertThrows(Exception.class, () -> parser.parse(xxePayload));
    }
}
