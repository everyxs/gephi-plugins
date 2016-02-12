/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.everyxs.test;

import java.util.ArrayList;
import java.util.HashMap;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.data.attributes.api.AttributeRow;
import org.gephi.data.attributes.api.AttributeTable;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.GraphView;
import org.gephi.graph.api.Node;
import org.openide.util.Lookup;

/**
 *
 * @author everyan
 */
public class Transformer {
    private boolean isDirected;
    private double scalePower;
    private double reweightPower;
    int specialCase = 0;
    GraphView oldView;
    HashMap<Integer, Node> indicies = new HashMap<Integer, Node>();
    HashMap<Node, Integer> invIndicies = new HashMap<Node, Integer>();
    
    Transformer(GraphModel graphModel, double scaleP, double reweightP, int inputMatrix) {
        oldView = graphModel.getVisibleView();
        Graph graph = graphModel.getGraph(oldView);
        isDirected = graphModel.isDirected();
        scalePower = scaleP;
        reweightPower = reweightP;
        specialCase = inputMatrix;
                
        indicies = new HashMap<Integer, Node>();
        invIndicies = new HashMap<Node, Integer>();
        int count = 0;
        for (Node u : graph.getNodes()) {
            indicies.put(count, u);
            invIndicies.put(u, count);
            count++;
        }
    }

    void rebuild(GraphModel graphModel) {

        Graph newGraph = graphModel.getGraph();
        AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
        AttributeTable nodeTable = attributeModel.getNodeTable();
        AttributeColumn Delay = nodeTable.getColumn("Delay");
        AttributeColumn Bias = nodeTable.getColumn("Bias");

        for (int i=0; i<newGraph.getNodeCount(); i++){
            Node s = indicies.get(i); //picking a source node
            AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();         
            double delay = Double.parseDouble(row.getValue(Delay).toString());
            double bias = Double.parseDouble(row.getValue(Bias).toString());
            for (Edge e:newGraph.getEdges(s))
                e.setWeight((float) (e.getWeight() * bias));
            
            if (delay-1.0>0) {
                Edge e0 = graphModel.factory().newEdge(s, s, (float) (delay), false);
                newGraph.addEdge(e0);
                }
            }
    }
        
    
    
    void reset(GraphModel graphModel) {
        graphModel.setVisibleView(oldView);       //Set the view as current visible view
    }
}
