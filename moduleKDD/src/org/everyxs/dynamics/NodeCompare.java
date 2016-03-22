/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.everyxs.dynamics;

/**
 *
 * @author everyan
 */
public class NodeCompare implements Comparable<NodeCompare>{
    private final int id;
    private Double attribute;
    
    public NodeCompare() {
        id = -1;
        attribute = Double.MAX_VALUE;
    }
    
    public NodeCompare(int i, Double a) {
        id = i;
        attribute = a;
    }

    public Double getAttribute() {
        return attribute;
    }
    
    public void setAttribute(double newValue) {
        attribute = newValue;
    }
    
    public int getID() {
        return id;
    }
    
    @Override
    public int compareTo(NodeCompare node) {
        //ascending order
        return this.attribute.compareTo(node.getAttribute());
    }
}
