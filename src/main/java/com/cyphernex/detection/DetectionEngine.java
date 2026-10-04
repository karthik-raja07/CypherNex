package com.cyphernex.detection;

import com.cyphernex.model.Detection;
import com.cyphernex.model.FormatFingerprint;
import com.cyphernex.model.NormalizedEvent;
import com.cyphernex.model.ParsedLog;
import com.cyphernex.model.RawLog;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class DetectionEngine {

    private final DriftDetector driftDetector;
    private final SilenceWatch silenceWatch;
    private final LogInjectionDetector logInjectionDetector;
    private final List<Detection> detections = new CopyOnWriteArrayList<>();

    public DetectionEngine(DriftDetector driftDetector, SilenceWatch silenceWatch, LogInjectionDetector logInjectionDetector) {
        this.driftDetector = driftDetector;
        this.silenceWatch = silenceWatch;
        this.logInjectionDetector = logInjectionDetector;
    }

    public synchronized void inspect(RawLog rawLog, ParsedLog parsedLog, NormalizedEvent normalizedEvent, FormatFingerprint fingerprint) {
        // 1. SilenceWatch: record arrival of event for its source
        String source = (normalizedEvent != null && normalizedEvent.getSource() != null)
                ? normalizedEvent.getSource()
                : (rawLog != null ? rawLog.getSource() : "default");
        Instant timestamp = (normalizedEvent != null && normalizedEvent.getTimestamp() != null)
                ? normalizedEvent.getTimestamp()
                : Instant.now();
        silenceWatch.recordEvent(source, timestamp);

        // 2. Drift Detection
        if (fingerprint != null) {
            Optional<Detection> driftDetection = driftDetector.inspect(source, fingerprint, rawLog);
            driftDetection.ifPresent(this::addDetection);
        }

        // 3. Log Injection Detection
        List<Detection> injectionDetections = logInjectionDetector.inspect(rawLog, parsedLog);
        for (Detection d : injectionDetections) {
            addDetection(d);
        }
    }

    public void addDetection(Detection detection) {
        if (detection != null) {
            detections.add(0, detection); // Latest first
        }
    }

    public List<Detection> getDetections() {
        return new ArrayList<>(detections);
    }

    public List<Detection> runAllDetections(List<NormalizedEvent> events) {
        return getDetections();
    }

    public void clear() {
        detections.clear();
        driftDetector.reset();
        silenceWatch.reset();
    }

    public DriftDetector getDriftDetector() {
        return driftDetector;
    }

    public SilenceWatch getSilenceWatch() {
        return silenceWatch;
    }

    public LogInjectionDetector getLogInjectionDetector() {
        return logInjectionDetector;
    }
}
