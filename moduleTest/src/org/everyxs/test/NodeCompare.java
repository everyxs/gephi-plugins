/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.everyxs.test;

/**
 *
 * @author everyan
 */
public class NodeCompare implements Comparable<NodeCompare>{
    private final int id;
    private final Double attribute;
    
    public NodeCompare(int i, Double a) {
        id = i;
        attribute = a;
    }

    public Double getAttribute() {
        return attribute;
    }
    
    public int getID() {
        return id;
    }
    
    @Override
    public int compareTo(NodeCompare node) {
        //descending order
        return this.attribute.compareTo(node.getAttribute());
    }
}
