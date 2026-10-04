package com.cyphernex.demo;

import com.cyphernex.model.RawLog;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class DemoDataGenerator {

    public List<RawLog> generateSampleLogs() {
        List<RawLog> logs = new ArrayList<>();
        Instant baseTime = Instant.now().minusSeconds(300);

        // 1. JSON log
        logs.add(new RawLog(
                UUID.randomUUID().toString(),
                "auth-service",
                "{\"timestamp\":\"2026-09-16T10:00:00Z\",\"user\":\"john\",\"action\":\"LOGIN\",\"ip\":\"10.10.2.15\",\"status\":\"SUCCESS\",\"requestId\":\"REQ-701\",\"sessionId\":\"SES-99\"}",
                baseTime
        ));

        // 2. CSV log
        logs.add(new RawLog(
                UUID.randomUUID().toString(),
                "process-monitor",
                "2026-09-16T10:01:00Z,john,PROCESS_STARTED,HOST-01,powershell.exe",
                baseTime.plusSeconds(60)
        ));

        // 3. XML log
        logs.add(new RawLog(
                UUID.randomUUID().toString(),
                "file-auditor",
                "<log><timestamp>2026-09-16T10:02:00Z</timestamp><user>john.doe@company.com</user><action>FILE_ACCESS</action><host>HOST-01</host><file>/etc/shadow</file><requestId>REQ-701</requestId></log>",
                baseTime.plusSeconds(120)
        ));

        // 4. Syslog log
        logs.add(new RawLog(
                UUID.randomUUID().toString(),
                "firewall-01",
                "<134>1 2026-09-16T10:03:00Z fw01.corp sshd 4102 - - Failed password for invalid user admin from 192.168.1.50 port 22 ssh2",
                baseTime.plusSeconds(180)
        ));

        // 5. Plain text key-value log
        logs.add(new RawLog(
                UUID.randomUUID().toString(),
                "network-gateway",
                "timestamp=2026-09-16T10:04:00Z user=AD-USER-442 action=NETWORK_CONNECTION ip=198.51.100.4 host=HOST-01 requestId=REQ-701",
                baseTime.plusSeconds(240)
        ));

        // 6. Unknown vendor format log (triggers Parser Forge learning)
        logs.add(new RawLog(
                UUID.randomUUID().toString(),
                "custom-sensor",
                "VENDOR_EVT|2026-09-16T10:05:00Z|USR-999|172.16.0.4|DATABASE_QUERY|SUCCESS",
                baseTime.plusSeconds(300)
        ));

        return logs;
    }
}
