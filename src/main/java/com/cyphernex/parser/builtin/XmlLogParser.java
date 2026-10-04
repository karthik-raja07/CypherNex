package com.cyphernex.parser.builtin;

import com.cyphernex.model.ParsedLog;
import com.cyphernex.model.RawLog;
import com.cyphernex.parser.LogParser;
import com.ctc.wstx.stax.WstxInputFactory;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class XmlLogParser implements LogParser {

    private final XMLInputFactory xmlInputFactory;

    public XmlLogParser() {
        this.xmlInputFactory = new WstxInputFactory();
        this.xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        this.xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    }

    @Override
    public String getName() {
        return "XmlLogParser";
    }

    @Override
    public String getVersion() {
        return "v1";
    }

    @Override
    public String getSupportedFormat() {
        return "XML";
    }

    @Override
    public boolean canParse(RawLog rawLog) {
        if (rawLog == null || rawLog.getRawContent() == null || rawLog.getRawContent().isBlank()) {
            return false;
        }
        String content = rawLog.getRawContent().trim();
        if (!content.startsWith("<") || !content.endsWith(">") || content.matches("^<\\d{1,3}>.*")) {
            return false;
        }
        try {
            XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(new StringReader(content));
            while (reader.hasNext()) {
                reader.next();
            }
            reader.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public ParsedLog parse(RawLog rawLog) {
        if (rawLog == null || rawLog.getRawContent() == null) {
            return new ParsedLog("unknown", getName(), getVersion(), new LinkedHashMap<>(), 0.0, "INVALID");
        }

        Map<String, Object> fields = new LinkedHashMap<>();
        try {
            XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(new StringReader(rawLog.getRawContent().trim()));
            Deque<String> tagStack = new ArrayDeque<>();
            StringBuilder textBuffer = new StringBuilder();

            while (reader.hasNext()) {
                int event = reader.next();
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        String localName = reader.getLocalName();
                        tagStack.push(localName);
                        textBuffer.setLength(0);

                        // Extract attributes if present
                        for (int i = 0; i < reader.getAttributeCount(); i++) {
                            fields.put(localName + "@" + reader.getAttributeLocalName(i), reader.getAttributeValue(i));
                        }
                        break;

                    case XMLStreamConstants.CHARACTERS:
                    case XMLStreamConstants.CDATA:
                        textBuffer.append(reader.getText());
                        break;

                    case XMLStreamConstants.END_ELEMENT:
                        String tag = tagStack.isEmpty() ? "" : tagStack.pop();
                        String text = textBuffer.toString().trim();
                        if (!text.isEmpty() && !tag.isEmpty()) {
                            fields.put(tag, text);
                        }
                        textBuffer.setLength(0);
                        break;
                }
            }
            reader.close();

            return new ParsedLog(
                    rawLog.getSource() != null ? rawLog.getSource() : "unknown",
                    getName(),
                    getVersion(),
                    fields,
                    1.0,
                    "VALID"
            );
        } catch (Exception e) {
            fields.put("raw", rawLog.getRawContent());
            fields.put("error", e.getMessage());
            return new ParsedLog(
                    rawLog.getSource() != null ? rawLog.getSource() : "unknown",
                    getName(),
                    getVersion(),
                    fields,
                    0.0,
                    "INVALID"
            );
        }
    }
}
