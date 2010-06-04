/*
Copyright 2008 WebAtlas
Authors : Mathieu Bastian, Mathieu Jacomy, Julian Bilcke
Website : http://www.gephi.org

This file is part of Gephi.

Gephi is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Gephi is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Gephi.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.gephi.streaming.api;

import org.gephi.streaming.api.event.EdgeAddedEvent;
import org.gephi.streaming.api.event.ElementAttributeEvent;
import org.gephi.streaming.api.event.ElementEvent;
import org.gephi.streaming.api.event.ElementType;
import org.gephi.streaming.api.event.EventType;
import org.gephi.streaming.api.event.GraphEvent;

/**
 * An OperationSupport implementation that send the events to an
 * GraphEventContainer
 * 
 * @author Andre' Panisson
 * @see GraphEventContainer
 *
 */
public class GraphEventOperationSupport implements OperationSupport {
    
    private Object source;
    private GraphEventContainer container;
    
    /**
     * @return the GraphEventContainer that will contain the events
     */
    public GraphEventContainer getContainer() {
        return container;
    }
    
    /**
     * @param container
     */
    public void setContainer(GraphEventContainer container) {
        this.container = container;
        this.source = container.getSource();
    }

    @Override
    public void edgeAdded(String edgeId, String fromNodeId, String toNodeId,
            boolean directed) {
        EdgeAddedEvent event = new EdgeAddedEvent(source, edgeId, fromNodeId, toNodeId, directed);
        fireEvent(event);
    }

    @Override
    public void edgeAttributeAdded(String edgeId, String attributeName, Object value) {
        ElementAttributeEvent event = new ElementAttributeEvent(source, EventType.ADD, ElementType.EDGE, edgeId, attributeName, value);
        fireEvent(event);
    }

    @Override
    public void edgeAttributeChanged(String edgeId,
            String attributeName, Object newValue) {
        ElementAttributeEvent event = new ElementAttributeEvent(source, EventType.CHANGE, ElementType.EDGE, edgeId, attributeName, newValue);
        fireEvent(event);
    }

    @Override
    public void edgeAttributeRemoved(String edgeId,
            String attributeName) {
        ElementAttributeEvent event = new ElementAttributeEvent(source, EventType.REMOVE, ElementType.EDGE, edgeId, attributeName, null);
        fireEvent(event);
    }

    @Override
    public void edgeRemoved(String edgeId) {
        GraphEvent event = new ElementEvent(source, EventType.REMOVE, ElementType.EDGE, edgeId);
        fireEvent(event);
    }

    @Override
    public void graphAttributeAdded(String attributeName,
            Object value) {
        ElementAttributeEvent event = new ElementAttributeEvent(source, EventType.ADD, ElementType.GRAPH, null, attributeName, value);
        fireEvent(event);
        
    }

    @Override
    public void graphAttributeChanged(String attributeName,
            Object newValue) {
        ElementAttributeEvent event = new ElementAttributeEvent(source, EventType.CHANGE, ElementType.GRAPH, null, attributeName, newValue);
        fireEvent(event);
    }

    @Override
    public void graphAttributeRemoved(String attributeName) {
        ElementAttributeEvent event = new ElementAttributeEvent(source, EventType.CHANGE, ElementType.GRAPH, null, attributeName, null);
        fireEvent(event);
    }

    @Override
    public void nodeAdded(String nodeId) {
        GraphEvent event = new ElementEvent(source, EventType.ADD, ElementType.NODE, nodeId);
        fireEvent(event);
    }

    @Override
    public void nodeAttributeAdded(String nodeId,
            String attributeName, Object value) {
        ElementAttributeEvent event = new ElementAttributeEvent(source, EventType.ADD, ElementType.NODE, nodeId, attributeName, value);
        fireEvent(event);
    }

    @Override
    public void nodeAttributeChanged(String nodeId,
            String attributeName, Object newValue) {
        ElementAttributeEvent event = new ElementAttributeEvent(source, EventType.CHANGE, ElementType.NODE, nodeId, attributeName, newValue);
        fireEvent(event);
    }

    @Override
    public void nodeAttributeRemoved(String nodeId,
            String attributeName) {
        ElementAttributeEvent event = new ElementAttributeEvent(source, EventType.REMOVE, ElementType.NODE, nodeId, attributeName, null);
        fireEvent(event);
    }

    @Override
    public void nodeRemoved(String nodeId) {
        GraphEvent event = new ElementEvent(source, EventType.REMOVE, ElementType.NODE, nodeId);
        fireEvent(event);
    }
    
    protected void fireEvent(GraphEvent event) {
        container.fireEvent(event);
    }
    
}