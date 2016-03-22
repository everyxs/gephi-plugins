/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.everyxs.transform;

import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JOptionPane;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.data.attributes.api.AttributeOrigin;
import org.gephi.data.attributes.api.AttributeRow;
import org.gephi.data.attributes.api.AttributeTable;
import org.gephi.data.attributes.api.AttributeType;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.EdgeIterable;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.GraphView;
import org.gephi.graph.api.HierarchicalDirectedGraph;
import org.gephi.graph.api.HierarchicalUndirectedGraph;
import org.gephi.graph.api.Node;
import org.openide.util.Lookup;

/**
 *
 * @author everyan
 */
public class Transformer {
    private boolean isDirected;
    GraphView oldView;
    HashMap<Integer, Node> indicies = new HashMap<Integer, Node>();
    HashMap<Node, Integer> invIndicies = new HashMap<Node, Integer>();

    Transformer(GraphModel graphModel) {
        oldView = graphModel.getVisibleView();
        Graph graph = graphModel.getGraph(oldView);
        isDirected = graphModel.isDirected();
                
        indicies = new HashMap<Integer, Node>();
        invIndicies = new HashMap<Node, Integer>();
        int count = 0;
        for (Node u : graph.getNodes()) {
            indicies.put(count, u);
            invIndicies.put(u, count);
            count++;
        }
    }

    public void rebuild(GraphModel graphModel, int type) {
        
        Graph newGraph = graphModel.getGraph();
        AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
        AttributeTable nodeTable = attributeModel.getNodeTable();
        
        switch (type) {
            case 10:
                for (int i=0; i<newGraph.getNodeCount(); i++){
                    Node s = indicies.get(i); //picking a source node
                    double degree = 0;
                    EdgeIterable iter;
                    if (isDirected) {
                            iter = ((HierarchicalDirectedGraph) newGraph).getInEdgesAndMetaInEdges(s);
                        } else {
                            iter = ((HierarchicalUndirectedGraph) newGraph).getEdgesAndMetaEdges(s);
                        }
                    for (Edge e : iter) {
                        degree += e.getWeight();
                    }

                    if (degree-1.0>0) {
                        double loop = (degree-1.0)*degree;
                        Edge e0 = graphModel.factory().newEdge(s, s, (float) (loop), false);
                        newGraph.addEdge(e0);
                    }
            }
            break;
            case 11: AttributeColumn Delay = nodeTable.getColumn("Delay");
                if (Delay == null) {
                        Delay = nodeTable.addColumn("Delay", "Delay", AttributeType.DOUBLE, AttributeOrigin.DATA, new Double(1.0));
                        JOptionPane.showMessageDialog(null, "'Delay' attribute has been added, please edit in the Data laboratory ", 
                                "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
                   }
                for (int i=0; i<newGraph.getNodeCount(); i++){
                    Node s = indicies.get(i); //picking a source node
                    double degree = 0;
                    EdgeIterable iter;
                    if (isDirected) {
                            iter = ((HierarchicalDirectedGraph) newGraph).getInEdgesAndMetaInEdges(s);
                        } else {
                            iter = ((HierarchicalUndirectedGraph) newGraph).getEdgesAndMetaEdges(s);
                        }
                    for (Edge e : iter) {
                        degree += e.getWeight();
                    }
                    AttributeRow row = (AttributeRow) s.getNodeData().getAttributes(); //get custom delay input    
                    double delay = Double.parseDouble(row.getValue(Delay).toString());
                    if (delay-1.0>0) {
                        double loop = (delay-1.0)*degree;
                        Edge e0 = graphModel.factory().newEdge(s, s, (float) (loop), false);
                        newGraph.addEdge(e0);
                    }
            }
            break;
            case 20:
                for (int i=0; i<newGraph.getNodeCount(); i++){
                    Node s = indicies.get(i); //picking a source node
                    double degree = 0;
                    EdgeIterable iter;
                    if (isDirected) {
                            iter = ((HierarchicalDirectedGraph) newGraph).getInEdgesAndMetaInEdges(s);
                        } else {
                            iter = ((HierarchicalUndirectedGraph) newGraph).getEdgesAndMetaEdges(s);
                        }
                    for (Edge e : iter) {
                        degree += e.getWeight();
                    }
                    if (isDirected) {
                            iter = ((HierarchicalDirectedGraph) newGraph).getInEdgesAndMetaInEdges(s);
                            for (Edge e : iter) {
                                e.setWeight((float) (e.getWeight() * degree));
                            }
                        } else {
                            iter = ((HierarchicalUndirectedGraph) newGraph).getEdgesAndMetaEdges(s);
                            for (Edge e : iter) {
                                e.setWeight((float) (e.getWeight() * degree));
                            }
                        }
                }
            break;
            case 21: AttributeColumn Bias = nodeTable.getColumn("Bias");
                if (Bias == null) {
                    Bias = nodeTable.addColumn("Bias", "Bias", AttributeType.DOUBLE, AttributeOrigin.DATA, new Double(1.0));
                    JOptionPane.showMessageDialog(null, "'Bias' attribute has been added, please edit in the Data laboratory ", 
                            "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);                    
                }
                for (int i=0; i<newGraph.getNodeCount(); i++){
                    Node s = indicies.get(i); //picking a source node
                    
                    AttributeRow row = (AttributeRow) s.getNodeData().getAttributes(); //get bias delay input    
                    double bias = Double.parseDouble(row.getValue(Bias).toString());
                    EdgeIterable iter;
                    if (isDirected) {
                            iter = ((HierarchicalDirectedGraph) newGraph).getInEdgesAndMetaInEdges(s);
                            for (Edge e : iter) {
                                e.setWeight((float) (e.getWeight() * bias));
                            }
                        } else {
                            iter = ((HierarchicalUndirectedGraph) newGraph).getEdgesAndMetaEdges(s);
                            for (Edge e : iter) {
                                e.setWeight((float) (e.getWeight() * bias));
                            }
                        }
                    
                }
            break;
            
        }
    }
        
    
    
    public void reset(GraphModel graphModel) {
       JOptionPane.showMessageDialog(null, "Under construction...", "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
       //Set the view as current visible view
    }
}
