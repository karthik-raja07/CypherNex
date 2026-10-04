package com.cyphernex.forge;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MetricsService {

    private final AtomicLong linesProcessed = new AtomicLong(0);
    private final AtomicLong llmCalls = new AtomicLong(0);
    private final AtomicLong parsersLearned = new AtomicLong(0);
    private final AtomicLong builtinParsersUsed = new AtomicLong(0);
    private final AtomicLong learnedParsersUsed = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);

    public void incrementLinesProcessed() {
        linesProcessed.incrementAndGet();
    }

    public void incrementLlmCalls() {
        llmCalls.incrementAndGet();
    }

    public void incrementParsersLearned() {
        parsersLearned.incrementAndGet();
    }

    public void incrementBuiltinParsersUsed() {
        builtinParsersUsed.incrementAndGet();
    }

    public void incrementLearnedParsersUsed() {
        learnedParsersUsed.incrementAndGet();
    }

    public void incrementCacheHits() {
        cacheHits.incrementAndGet();
    }

    public void incrementCacheMisses() {
        cacheMisses.incrementAndGet();
    }

    public long getLinesProcessed() {
        return linesProcessed.get();
    }

    public long getLlmCalls() {
        return llmCalls.get();
    }

    public long getParsersLearned() {
        return parsersLearned.get();
    }

    public long getBuiltinParsersUsed() {
        return builtinParsersUsed.get();
    }

    public long getLearnedParsersUsed() {
        return learnedParsersUsed.get();
    }

    public long getCacheHits() {
        return cacheHits.get();
    }

    public long getCacheMisses() {
        return cacheMisses.get();
    }

    public Map<String, Object> getMetrics(String llmMode) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("linesProcessed", linesProcessed.get());
        map.put("llmCalls", llmCalls.get());
        map.put("parsersLearned", parsersLearned.get());
        map.put("builtinParsersUsed", builtinParsersUsed.get());
        map.put("learnedParsersUsed", learnedParsersUsed.get());
        map.put("cacheHits", cacheHits.get());
        map.put("cacheMisses", cacheMisses.get());
        long totalLookups = cacheHits.get() + cacheMisses.get();
        double reuseRate = totalLookups > 0 ? ((double) cacheHits.get() / totalLookups) * 100.0 : 0.0;
        map.put("cacheReuseRate", Math.round(reuseRate * 10.0) / 10.0);
        map.put("llmMode", llmMode);
        return map;
    }

    public void reset() {
        linesProcessed.set(0);
        llmCalls.set(0);
        parsersLearned.set(0);
        builtinParsersUsed.set(0);
        learnedParsersUsed.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
    }
}

