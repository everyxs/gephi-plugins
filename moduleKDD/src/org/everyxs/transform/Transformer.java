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
import org.gephi.graph.api.NodeIterable;
import org.openide.util.Lookup;

/**
 *
 * @author everyan
 */
public class Transformer {
    private boolean isDirected;
    GraphModel graphModel;
    GraphView oldView;
    HashMap<Integer, Node> indicies = new HashMap<Integer, Node>();
    HashMap<Node, Integer> invIndicies = new HashMap<Node, Integer>();

    Transformer(GraphModel gModel) {
        graphModel = gModel;
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

    public void rebuild(GraphModel graphModel, int type, int baseLayer) {
        
        Graph newGraph = graphModel.getGraphVisible();
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
            case 32: AttributeColumn Layer = nodeTable.getColumn("Layer[Z]");
                if (Layer == null) {
                    Layer = nodeTable.addColumn("Layer[Z]", "Layer[Z]", AttributeType.DOUBLE, AttributeOrigin.DATA, new Integer(1));
                    JOptionPane.showMessageDialog(null, "'Layer[Z]' attribute has been added, please edit in the Data laboratory ", 
                            "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
                }
                AttributeColumn Longitude = nodeTable.getColumn("longitude");
                if (Longitude == null) {
                        Longitude = nodeTable.addColumn("longitude", "longitude", AttributeType.DOUBLE, AttributeOrigin.DATA, new Integer(1));
                        JOptionPane.showMessageDialog(null, "'longitude' attribute has been added, please edit in the Data laboratory ", 
                                "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
                   }
                AttributeColumn Latitude = nodeTable.getColumn("latitude");
                if (Latitude == null) {
                        Latitude = nodeTable.addColumn("latitude", "latitude", AttributeType.DOUBLE, AttributeOrigin.DATA, new Integer(1));
                        JOptionPane.showMessageDialog(null, "'latitude' attribute has been added, please edit in the Data laboratory ", 
                            "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
                   }
                for (int i=0; i<newGraph.getNodeCount(); i++){
                    Node s = indicies.get(i); //picking a source node
                    AttributeRow row = (AttributeRow) s.getNodeData().getAttributes(); //get bias delay input    
                    int layer = Integer.parseInt(row.getValue(Layer).toString());
                    if (layer == baseLayer) {
                        EdgeIterable iter;
                        if (isDirected) 
                            iter = ((HierarchicalDirectedGraph) newGraph).getInEdgesAndMetaInEdges(s);
                        else 
                            iter = ((HierarchicalUndirectedGraph) newGraph).getEdgesAndMetaEdges(s);
                        
                        boolean map = false;
                        for (Edge e : iter) {
                            Node input1 = e.getTarget();
                            if (e.getEdgeData().getLabel()=="interEdge") {
                                input1 = e.getTarget();
                                map = true;
                            }
                        }
                       
                        double reweigh = 1;
                        if (map == true) { // if there is a geo map for s
                            for (Edge e : iter) {
                                if (e.getEdgeData().getLabel()!="interEdge") { // for base layer connections
                                    Node t = e.getTarget();
                                    EdgeIterable iter2;
                                    if (isDirected) 
                                        iter2 = ((HierarchicalDirectedGraph) newGraph).getInEdgesAndMetaInEdges(t);
                                    else 
                                        iter2 = ((HierarchicalUndirectedGraph) newGraph).getEdgesAndMetaEdges(t);
                                    for (Edge e2 : iter2) {
                                        if (e2.getEdgeData().getLabel()=="interEdge") {
                                            Node input2 = e2.getTarget();
                                            AttributeRow row2 = (AttributeRow) input2.getNodeData().getAttributes(); //get bias delay input    
                                            //AttributeRow row3 = (AttributeRow) input1.getNodeData().getAttributes(); //get bias delay input    
                                            double longtitude1 = Double.parseDouble(row.getValue(Longitude).toString());

                                            //reweigh = input2.getNodeData().
                                        }
                                    }
                                    e.setWeight((float) (e.getWeight() * reweigh));
                                }
                            }
                        }
                        
                    } 
                }
            break;
        }
    }
        
    public void map(int inputLayer, int baseLayer) {
        Graph newGraph = graphModel.getGraphVisible();
        AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
        AttributeTable nodeTable = attributeModel.getNodeTable();
        AttributeColumn Layer = nodeTable.getColumn("Layer[Z]");
        
        for (int i=0; i<newGraph.getNodeCount(); i++){
            Node s = indicies.get(i); //picking a source node
            String name = s.getNodeData().getLabel();
            name.replace(".", "");
            AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();         
            int layer = Integer.parseInt(row.getValue(Layer).toString());
            
            for (int j=0; j<newGraph.getNodeCount(); j++){
                Node t = indicies.get(j); //picking a source node
                if (name.equals(t.getNodeData().getLabel().replace(".", ""))) { //only diagsonal entries are considered     
                    //System.out.println(name + t.getNodeData().getLabel());
                    AttributeRow row2 = (AttributeRow) t.getNodeData().getAttributes();   
                    int layer2 = Integer.parseInt(row2.getValue(Layer).toString());
                    if ((layer==inputLayer && layer2==baseLayer)||(layer2==inputLayer && layer==baseLayer)) {
                        System.out.println(layer + layer2 - baseLayer - inputLayer);
                        Edge e = graphModel.factory().newEdge(s, t, (float) 3.1415926, false);
                        e.getEdgeData().setLabel("interEdge");
                        newGraph.addEdge(e);
                    }
                }
            }
        }
    }
    
    public void reset(GraphModel graphModel) {
       JOptionPane.showMessageDialog(null, "Under construction...", "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
       //Set the view as current visible view
    }
}
