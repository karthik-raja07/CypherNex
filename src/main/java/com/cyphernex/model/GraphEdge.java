package com.cyphernex.model;

import java.util.Objects;

public class GraphEdge {
    private String source;
    private String target;
    private String relationship;

    public GraphEdge() {}

    public GraphEdge(String source, String target, String relationship) {
        this.source = source;
        this.target = target;
        this.relationship = relationship;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphEdge graphEdge = (GraphEdge) o;
        return Objects.equals(source, graphEdge.source) &&
                Objects.equals(target, graphEdge.target) &&
                Objects.equals(relationship, graphEdge.relationship);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target, relationship);
    }
}
