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
 * Graph transformer for intra- and inter-layer connections
 * @author everyxs
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
    /**
     * Global transformations for smaller graphs. 
     * @param graphModel
     * @param type
     * @param baseLayer 
     */
    public void globalTransform(GraphModel graphModel, int type, int baseLayer, double power) {
        
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
                    degree = Math.pow(degree, power); //raising to the bias power
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
                    double bias = Math.pow(Double.parseDouble(row.getValue(Bias).toString()), power); //raising to the bias power
                    
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
                EdgeIterable iter;
                if (isDirected)
                    iter = ((HierarchicalDirectedGraph) newGraph).getEdgesAndMetaEdges();
                else 
                    iter = ((HierarchicalUndirectedGraph) newGraph).getEdgesAndMetaEdges();
                Edge[] edgeList = iter.toArray();
                for (int i=0; i<edgeList.length; i++){
                    if (edgeList[i].getEdgeData().getLabel().equals("coauthor")) {
                        Node s = edgeList[i].getSource();
                        Node t = edgeList[i].getTarget();
                        Node s2 = indicies.get(i);
                        Node t2 = indicies.get(i+1);
                        AttributeRow row1 = (AttributeRow) s.getNodeData().getAttributes(); 
                        AttributeRow row2 = (AttributeRow) t.getNodeData().getAttributes(); 
                        boolean map = false;
                        
                        EdgeIterable iter2;
                        if (isDirected) 
                            iter2 = ((HierarchicalDirectedGraph) newGraph).getInEdgesAndMetaInEdges(s);
                        else 
                            iter2 = ((HierarchicalUndirectedGraph) newGraph).getEdgesAndMetaEdges(s);
                        
                        for (Edge e2 : iter2) {
                            if ("interEdge".equals(e2.getEdgeData().getLabel())) {
                                map = true;
                                s2 = e2.getTarget();
                            }
                        }
                        if (map) {
                            map = false;
                            if (isDirected) 
                                iter2 = ((HierarchicalDirectedGraph) newGraph).getInEdgesAndMetaInEdges(t);
                            else 
                                iter2 = ((HierarchicalUndirectedGraph) newGraph).getEdgesAndMetaEdges(t);

                            for (Edge e2 : iter2) {
                                if ("interEdge".equals(e2.getEdgeData().getLabel())) {
                                    map = true;
                                    t2 = e2.getTarget();
                                }
                            }
                        }
                        
                        double reweigh = 1;
                        if (map == true) { // if there is a complete multi-affliate map
                            if (newGraph.getEdge(s2, t2)!=null) {
                                if ("multiAffli".equals(newGraph.getEdge(s2, t2).getEdgeData().getLabel())) { // get the multi-affliate edge\
                                    if (s2.getId() == t2.getId())
                                        reweigh = 0.001; //same geolicaion boost
                                    else {
                                        Edge eInput = newGraph.getEdge(s2, t2);
                                        if (eInput != null)
                                            reweigh = 0.5; //raising to the bias power
                                    }
                                }
                            }
                        }
                        edgeList[i].setWeight((float) (edgeList[i].getWeight() * reweigh));
                    }
                }
            break;
        }
    }
    
    /**
     * Connecting base layer with input layer by interLayerMapping the inter-layer correspondence.
     * @param inputLayer
     * @param baseLayer 
     */
    public void interLayerMap(int inputLayer, int baseLayer) {
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
                        //System.out.println(layer + layer2 - baseLayer - inputLayer);
                        Edge e = graphModel.factory().newEdge(s, t, (float) 3.1415926, false);
                        e.getEdgeData().setLabel("interEdge");
                        newGraph.addEdge(e);
                    }
                }
            }
        }
    }
    
    /**
     * This function converts decimal degrees to radians	
     * @param deg double
     * @return double
     */
    private static double deg2rad(double deg) {
            return (deg * Math.PI / 180.0);
    }

    /**
     * This function converts radians to decimal degrees
     * @param rad double
     * @return double
     */
    private static double rad2deg(double rad) {
            return (rad * 180 / Math.PI);
    }
    
    /**
     * This function calculates geo distances based on longitudes and latitudes
     * need local node mergers conditioned on resolution
     * @param inputLayer 
     */
    public void distance(int inputLayer) {
        Graph newGraph = graphModel.getGraphVisible();
        AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
        AttributeTable nodeTable = attributeModel.getNodeTable();
        AttributeColumn Layer = nodeTable.getColumn("Layer[Z]");
        AttributeColumn Longitude = nodeTable.getColumn("Lng");
        if (Longitude == null) {
                Longitude = nodeTable.addColumn("Lng", "Lng", AttributeType.DOUBLE, AttributeOrigin.DATA, new Integer(1));
                JOptionPane.showMessageDialog(null, "'Lng' attribute has been added, please edit in the Data laboratory ", 
                        "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
           }
        AttributeColumn Latitude = nodeTable.getColumn("Lat");
        if (Latitude == null) {
                Latitude = nodeTable.addColumn("Lat", "Lat", AttributeType.DOUBLE, AttributeOrigin.DATA, new Integer(1));
                JOptionPane.showMessageDialog(null, "'Lat' attribute has been added, please edit in the Data laboratory ", 
                        "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
           }
        
        for (int i=0; i<newGraph.getNodeCount(); i++){
            Node s = indicies.get(i); //picking a source node
            AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();         
            int layer = Integer.parseInt(row.getValue(Layer).toString());
            
            if (layer == inputLayer) { //only input layer pairs are considered    
                for (int j=i+1; j<newGraph.getNodeCount(); j++){
                    Node t = indicies.get(j); //picking a source node
                    AttributeRow row2 = (AttributeRow) t.getNodeData().getAttributes();   
                    int layer2 = Integer.parseInt(row2.getValue(Layer).toString());
                    if (layer2 == inputLayer) { //only input layer pairs are considered    
                        double long1 = Double.parseDouble(row.getValue(Longitude).toString());
                        double long2 = Double.parseDouble(row2.getValue(Longitude).toString());
                        double lati1 = Double.parseDouble(row.getValue(Latitude).toString());
                        double lati2 = Double.parseDouble(row2.getValue(Latitude).toString());
                        
                        double theta = long1 - long2;
                        double dist = Math.sin(deg2rad(lati1)) * Math.sin(deg2rad(lati2)) 
                                + Math.cos(deg2rad(lati1)) * Math.cos(deg2rad(lati2)) * Math.cos(deg2rad(theta));
                        dist = Math.acos(dist);
                        dist = rad2deg(dist);
                        dist = dist * 60 * 1.1515;
                        if (dist > 20000) //check if the distance is over half quator length
                            dist = 20000;
                        if (dist < 1) //need local node mergers conditioned on resolution
                            dist = 1;
                        dist = 1.0/dist;
                        
                        Edge e = graphModel.factory().newEdge(s, t, (float) dist, false);
                        //if (isDirected) 
                          //  e = graphModel.factory().newEdge(s, t, (float) dist, true);
                        e.getEdgeData().setLabel("geoDistance");
                        newGraph.addEdge(e);
                        //if (isDirected) {
                          //  e = graphModel.factory().newEdge(t, s, (float) dist, true);
                            //e.getEdgeData().setLabel("geoDistance");
                            //newGraph.addEdge(e);
                        //}
                    }
                }
            }
        }
    }
    
    public void reset(GraphModel graphModel) {
       JOptionPane.showMessageDialog(null, "Under construction...", "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
       //Set the view as current visible view
    }

    void cooccur(int baseLayer, AttributeColumn entity, AttributeColumn venue) {
        Graph newGraph = graphModel.getGraphVisible();
        AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
        AttributeTable nodeTable = attributeModel.getNodeTable();
        AttributeColumn Layer = nodeTable.getColumn("Layer[Z]");
        if (Layer == null) {
                Layer = nodeTable.addColumn("Layer[Z]", "Layer[Z]", AttributeType.DOUBLE, AttributeOrigin.DATA, new Integer(1));
                JOptionPane.showMessageDialog(null, "'Layer[Z]' attribute has been added, please edit in the Data laboratory ", 
                        "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
           }
        AttributeColumn Longitude = nodeTable.getColumn("Lng");
        if (Longitude == null) {
                Longitude = nodeTable.addColumn("Lng", "Lng", AttributeType.DOUBLE, AttributeOrigin.DATA, new Integer(1));
                JOptionPane.showMessageDialog(null, "'Lng' attribute has been added, please edit in the Data laboratory ", 
                        "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
           }
        AttributeColumn Latitude = nodeTable.getColumn("Lat");
        if (Latitude == null) {
                Latitude = nodeTable.addColumn("Lat", "Lat", AttributeType.DOUBLE, AttributeOrigin.DATA, new Integer(1));
                JOptionPane.showMessageDialog(null, "'Lat' attribute has been added, please edit in the Data laboratory ", 
                        "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
           }
        
        for (int i=0; i<newGraph.getNodeCount(); i++){
            Node s = indicies.get(i); //picking a source entity
            AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();         
            int layer = Integer.parseInt(row.getValue(Layer).toString());
            
            if (layer == baseLayer) { //only input layer pairs are considered    
                for (int j=i+1; j<newGraph.getNodeCount(); j++){
                    Node t = indicies.get(j); //picking a source entity
                    AttributeRow row2 = (AttributeRow) t.getNodeData().getAttributes();   
                    int layer2 = Integer.parseInt(row2.getValue(Layer).toString());
                    String paper1 = row.getValue(venue).toString();
                    String paper2 = row2.getValue(venue).toString();
                    if (paper1 == paper2) { //paper match
                        if (layer2 == baseLayer) { //only input layer pairs are considered    
                            double long1 = Double.parseDouble(row.getValue(Longitude).toString());
                            double long2 = Double.parseDouble(row2.getValue(Longitude).toString());
                            double lati1 = Double.parseDouble(row.getValue(Latitude).toString());
                            double lati2 = Double.parseDouble(row2.getValue(Latitude).toString());

                            double theta = long1 - long2;
                            double dist = Math.sin(deg2rad(lati1)) * Math.sin(deg2rad(lati2)) 
                                    + Math.cos(deg2rad(lati1)) * Math.cos(deg2rad(lati2)) * Math.cos(deg2rad(theta));
                            if (Math.abs(dist)>1) //handling numeric error
                                dist = 1;
                            else {
                                dist = Math.acos(dist);
                                dist = rad2deg(dist);
                                dist = dist * 60 * 1.1515; //miles
                                if (dist > 20000) //check if the distance is over half quator length
                                    dist = 20000;
                                if (dist < 1) //need local node mergers conditioned on resolution
                                    dist = 1;
                            }
                            //dist = 1.0/dist;
                            Edge e = graphModel.factory().newEdge(s, t, (float) dist, false);
                            e.getEdgeData().setLabel("geoDistance");
                            newGraph.addEdge(e);
                        }
                    }
                }
            }
        }
    }
}
