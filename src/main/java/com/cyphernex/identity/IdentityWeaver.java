package com.cyphernex.identity;

import com.cyphernex.model.IdentityRecord;
import com.cyphernex.model.NormalizedEvent;
import com.cyphernex.storage.IdentityRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IdentityWeaver {

    private final IdentityRepository identityRepository;
    private final Map<String, String> aliasToCanonicalMap = new ConcurrentHashMap<>();

    public IdentityWeaver(IdentityRepository identityRepository) {
        this.identityRepository = identityRepository;
    }

    public List<IdentityRecord> resolveIdentities(List<NormalizedEvent> events) {
        if (events == null || events.isEmpty()) {
            return List.of();
        }

        // Collect all distinct user identifiers and their associated events
        Map<String, List<NormalizedEvent>> userEventsMap = new LinkedHashMap<>();
        for (NormalizedEvent event : events) {
            String user = event.getUser();
            if (user != null && !user.isBlank()) {
                userEventsMap.computeIfAbsent(user.trim(), k -> new ArrayList<>()).add(event);
            }
        }

        List<String> users = new ArrayList<>(userEventsMap.keySet());
        if (users.isEmpty()) {
            return List.of();
        }

        // Adjacency / pairwise link evidence graph
        Map<String, Set<String>> adjacency = new HashMap<>();
        Map<String, Double> pairConfidence = new HashMap<>();
        Map<String, List<String>> pairEvidence = new HashMap<>();

        for (String u : users) {
            adjacency.put(u, new HashSet<>());
        }

        for (int i = 0; i < users.size(); i++) {
            String u1 = users.get(i);
            List<NormalizedEvent> events1 = userEventsMap.get(u1);

            for (int j = i + 1; j < users.size(); j++) {
                String u2 = users.get(j);
                List<NormalizedEvent> events2 = userEventsMap.get(u2);

                double score = 0.0;
                List<String> evidenceList = new ArrayList<>();

                // Evidence 1: Shared sessionId / requestId (+0.50)
                boolean sharedSessionOrRequest = false;
                for (NormalizedEvent e1 : events1) {
                    for (NormalizedEvent e2 : events2) {
                        boolean sharedSession = e1.getSessionId() != null && !e1.getSessionId().isBlank()
                                && e1.getSessionId().equals(e2.getSessionId());
                        boolean sharedRequest = e1.getRequestId() != null && !e1.getRequestId().isBlank()
                                && e1.getRequestId().equals(e2.getRequestId());

                        if (sharedSession || sharedRequest) {
                            sharedSessionOrRequest = true;
                            break;
                        }
                    }
                    if (sharedSessionOrRequest) break;
                }

                if (sharedSessionOrRequest) {
                    score += 0.50;
                    evidenceList.add("shared sessionId/requestId");
                }

                // Evidence 2: String similarity between identifiers (+0.25)
                if (hasStringSimilarity(u1, u2)) {
                    score += 0.25;
                    evidenceList.add("string similarity");
                }

                // Evidence 3: Close timestamps (+0.25)
                boolean closeTiming = false;
                for (NormalizedEvent e1 : events1) {
                    for (NormalizedEvent e2 : events2) {
                        if (e1.getTimestamp() != null && e2.getTimestamp() != null) {
                            long diffSec = Math.abs(Duration.between(e1.getTimestamp(), e2.getTimestamp()).toSeconds());
                            if (diffSec <= 300) { // within 5 minutes
                                closeTiming = true;
                                break;
                            }
                        }
                    }
                    if (closeTiming) break;
                }

                if (closeTiming) {
                    score += 0.25;
                    evidenceList.add("close timestamps");
                }

                double finalScore = Math.min(1.0, score);
                // Threshold: only merge when confidence >= 0.50
                if (finalScore >= 0.50) {
                    adjacency.get(u1).add(u2);
                    adjacency.get(u2).add(u1);
                    String key = makePairKey(u1, u2);
                    pairConfidence.put(key, finalScore);
                    pairEvidence.put(key, evidenceList);
                }
            }
        }

        // Build connected components / clusters
        Set<String> visited = new HashSet<>();
        List<IdentityRecord> resolvedRecords = new ArrayList<>();

        for (String u : users) {
            if (!visited.contains(u)) {
                Set<String> clusterAliases = new LinkedHashSet<>();
                List<String> clusterEvidence = new ArrayList<>();
                double maxConfidence = 0.50;

                // BFS / DFS
                List<String> queue = new ArrayList<>();
                queue.add(u);
                visited.add(u);

                while (!queue.isEmpty()) {
                    String curr = queue.remove(0);
                    clusterAliases.add(curr);

                    for (String neighbor : adjacency.get(curr)) {
                        if (!visited.contains(neighbor)) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                            String key = makePairKey(curr, neighbor);
                            Double pairScore = pairConfidence.get(key);
                            if (pairScore != null && pairScore > maxConfidence) {
                                maxConfidence = pairScore;
                            }
                            List<String> ev = pairEvidence.get(key);
                            if (ev != null) {
                                for (String e : ev) {
                                    if (!clusterEvidence.contains(e)) {
                                        clusterEvidence.add(e);
                                    }
                                }
                            }
                        }
                    }
                }

                // If single alias with no pair merge, confidence is 1.0 (exact identifier)
                if (clusterAliases.size() == 1) {
                    maxConfidence = 1.0;
                    clusterEvidence.add("direct identifier");
                }

                // Determine canonical identity (prefer shortest/cleanest identifier like "john" over "john.doe@domain.com" or "AD-USER-442")
                String canonical = chooseCanonical(clusterAliases);

                IdentityRecord record = new IdentityRecord(
                        canonical,
                        clusterAliases,
                        maxConfidence,
                        clusterEvidence
                );

                for (String alias : clusterAliases) {
                    aliasToCanonicalMap.put(alias, canonical);
                }

                if (identityRepository != null) {
                    identityRepository.save(record);
                }

                resolvedRecords.add(record);
            }
        }

        return resolvedRecords;
    }

    public String getCanonicalIdentity(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return "unknown";
        }
        return aliasToCanonicalMap.getOrDefault(identifier, identifier);
    }

    public Optional<IdentityRecord> resolveIdentity(NormalizedEvent event) {
        if (event == null || event.getUser() == null) {
            return Optional.empty();
        }
        List<IdentityRecord> records = resolveIdentities(List.of(event));
        return records.isEmpty() ? Optional.empty() : Optional.of(records.get(0));
    }

    public double calculateConfidence(boolean sharedId, boolean stringSimilarity, boolean closeTiming) {
        double score = 0.0;
        if (sharedId) score += 0.50;
        if (stringSimilarity) score += 0.25;
        if (closeTiming) score += 0.25;
        return Math.min(1.0, score);
    }

    public boolean hasStringSimilarity(String a, String b) {
        if (a == null || b == null) return false;
        String s1 = a.toLowerCase().trim();
        String s2 = b.toLowerCase().trim();

        if (s1.equals(s2)) return true;

        // Strip domain from email
        String u1 = s1.contains("@") ? s1.substring(0, s1.indexOf('@')) : s1;
        String u2 = s2.contains("@") ? s2.substring(0, s2.indexOf('@')) : s2;

        if (u1.equals(u2)) return true;

        // Prefix / substring match
        if (u1.startsWith(u2) || u2.startsWith(u1)) return true;
        if (u1.contains(u2) || u2.contains(u1)) return true;

        // Replace separators like . _ -
        String clean1 = u1.replaceAll("[._-]", "");
        String clean2 = u2.replaceAll("[._-]", "");
        if (clean1.equals(clean2) || clean1.contains(clean2) || clean2.contains(clean1)) return true;

        return false;
    }

    private String chooseCanonical(Set<String> aliases) {
        if (aliases == null || aliases.isEmpty()) {
            return "unknown";
        }
        // Prefer alias without '@' and without '-' prefix, then shortest
        return aliases.stream()
                .min(Comparator.comparingInt((String a) -> a.contains("@") ? 2 : 0)
                        .thenComparingInt(a -> a.startsWith("AD-") || a.startsWith("USR-") ? 1 : 0)
                        .thenComparingInt(String::length)
                        .thenComparing(String::compareTo))
                .orElse(aliases.iterator().next());
    }

    private String makePairKey(String a, String b) {
        return a.compareTo(b) < 0 ? a + "||" + b : b + "||" + a;
    }
}
