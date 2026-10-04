package com.cyphernex.correlation;

import com.cyphernex.model.GraphEdge;
import com.cyphernex.model.GraphNode;
import com.cyphernex.model.NormalizedEvent;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class StoryGraph {

    // Internal JGraphT graph storing node IDs
    private final Graph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
    private final Map<String, GraphNode> nodeMap = new LinkedHashMap<>();
    private final Set<GraphEdge> edgeSet = new LinkedHashSet<>();
    private final List<NormalizedEvent> correlatedEvents = new ArrayList<>();

    public synchronized void addNode(GraphNode node) {
        if (node == null || node.getId() == null) return;
        graph.addVertex(node.getId());
        nodeMap.put(node.getId(), node);
    }

    public synchronized void addNode(String id, String type, String label) {
        if (id == null) return;
        GraphNode node = new GraphNode(id, type, label != null ? label : id);
        addNode(node);
    }

    public synchronized void addEdge(String sourceId, String targetId, String relationship) {
        if (sourceId == null || targetId == null || relationship == null) return;

        // Ensure vertices exist
        if (!nodeMap.containsKey(sourceId)) {
            addNode(sourceId, "UNKNOWN", sourceId);
        }
        if (!nodeMap.containsKey(targetId)) {
            addNode(targetId, "UNKNOWN", targetId);
        }

        graph.addVertex(sourceId);
        graph.addVertex(targetId);
        graph.addEdge(sourceId, targetId);

        edgeSet.add(new GraphEdge(sourceId, targetId, relationship));
    }

    public synchronized List<GraphNode> getNodes() {
        return new ArrayList<>(nodeMap.values());
    }

    public synchronized List<GraphEdge> getEdges() {
        return new ArrayList<>(edgeSet);
    }

    public synchronized void setCorrelatedEvents(List<NormalizedEvent> events) {
        correlatedEvents.clear();
        if (events != null) {
            correlatedEvents.addAll(events);
        }
    }

    public synchronized List<NormalizedEvent> getCorrelatedEvents() {
        return new ArrayList<>(correlatedEvents);
    }

    public synchronized void clear() {
        nodeMap.clear();
        edgeSet.clear();
        correlatedEvents.clear();
        for (String v : new ArrayList<>(graph.vertexSet())) {
            graph.removeVertex(v);
        }
    }

    public Graph<String, DefaultEdge> getUnderlyingGraph() {
        return graph;
    }

    public synchronized String generateNarrative() {
        if (correlatedEvents.isEmpty()) {
            return "No correlated security events recorded in StoryGraph.";
        }

        StringBuilder sb = new StringBuilder();
        NormalizedEvent first = correlatedEvents.get(0);
        String user = first.getUser() != null ? first.getUser() : "An unknown user";
        String initialHost = first.getHost() != null ? (" on " + first.getHost()) : "";
        String initialIp = first.getSourceIp() != null ? (" from " + first.getSourceIp()) : "";

        sb.append("User ").append(user);

        List<String> actions = new ArrayList<>();
        for (NormalizedEvent event : correlatedEvents) {
            String act = event.getActivity();
            String host = event.getHost();
            String ip = event.getSourceIp() != null ? event.getSourceIp() : event.getDestinationIp();
            String proc = event.getProcess();

            if (act != null) {
                switch (act.toUpperCase()) {
                    case "LOGIN", "AUTHENTICATE", "USERLOGIN" -> {
                        String from = (ip != null) ? (" from " + ip) : "";
                        actions.add("logged in" + from);
                    }
                    case "PROCESS_STARTED", "PROCESS_CREATE", "EXEC" -> {
                        String p = (proc != null) ? (" " + proc) : " a process";
                        String h = (host != null) ? (" on " + host) : "";
                        actions.add("started" + p + h);
                    }
                    case "FILE_ACCESS", "FILE_READ", "FILE_WRITE" -> {
                        actions.add("accessed a sensitive file");
                    }
                    case "NETWORK_CONNECTION", "CONNECT", "SOCKET" -> {
                        String dest = (event.getDestinationIp() != null) ? (" to " + event.getDestinationIp()) : " a network connection";
                        actions.add("initiated " + dest);
                    }
                    case "LOGOUT" -> {
                        actions.add("logged out");
                    }
                    default -> {
                        actions.add("performed " + act.toLowerCase().replace('_', ' '));
                    }
                }
            }
        }

        if (actions.isEmpty()) {
            return "User " + user + " participated in " + correlatedEvents.size() + " correlated security events.";
        }

        // Stitch actions together
        for (int i = 0; i < actions.size(); i++) {
            if (i == 0) {
                sb.append(" ").append(actions.get(i));
            } else if (i == actions.size() - 1) {
                sb.append(", and then ").append(actions.get(i));
            } else {
                sb.append(", ").append(actions.get(i));
            }
        }
        sb.append(".");

        return sb.toString();
    }
}
