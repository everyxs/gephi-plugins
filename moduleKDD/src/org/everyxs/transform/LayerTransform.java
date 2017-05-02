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
import org.gephi.datalab.api.GraphElementsController;
import org.gephi.datalab.spi.rows.merge.AttributeRowsMergeStrategy;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.GraphView;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.NodeIterable;
import org.gephi.partition.api.Partition;
import org.gephi.partition.api.PartitionController;
import org.openide.util.Lookup;

/**
 *
 * @author everyan
 */
public class LayerTransform {

    GraphView oldView;
    HashMap<Integer, Node> indicies = new HashMap<Integer, Node>();
    HashMap<Node, Integer> invIndicies = new HashMap<Node, Integer>();
    ArrayList<Node> mergeCandidates = new ArrayList<Node>();
    ArrayList<Node> mergeCenters = new ArrayList<Node>();
    
    public LayerTransform(GraphModel graphModel) {
        oldView = graphModel.getVisibleView();
        Graph graph = graphModel.getGraph(oldView);
        
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
    
    public void build(GraphModel graphModel, int interType, int inputLayer, int baseLayer) {
        GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
        Boolean isDirected = false;
        if (graphController != null && graphController.getModel() != null) {
            isDirected = graphController.getModel().isDirected();
        }
        //GraphView newView = graphModel.newView();     //Duplicate main view
        Graph newGraph = graphModel.getGraph();
        AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
        AttributeTable nodeTable = attributeModel.getNodeTable();
        AttributeColumn Layer = nodeTable.getColumn("Layer[Z]");
        
        switch (interType) {
            case 3:
                AttributeColumn Longitude = nodeTable.getColumn("Lng");
                AttributeColumn Latitude = nodeTable.getColumn("Lat");
                AttributeColumn dist2Air = nodeTable.getColumn("Dist2Air");
                if (dist2Air == null) {
                        dist2Air = nodeTable.addColumn("Dist2Air", "Dist2Air", AttributeType.DOUBLE, AttributeOrigin.DATA, new Double(0));
                        JOptionPane.showMessageDialog(null, "'Dist2Air' attribute has been added, please edit in the Data laboratory ", 
                                "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
                }
                //AttributeColumn names = nodeTable.getColumn("Label"); //for the same node in different layers
                //PartitionController partitionController = Lookup.getDefault().lookup(PartitionController.class);
                //Partition group = partitionController.buildPartition(names, newGraph); //node instances group
                //Adding inter-layer edges
                for (int i=0; i<newGraph.getNodeCount(); i++){
                    Node s = indicies.get(i); //picking a source node
                    String name = s.getNodeData().getLabel();
                    boolean main = false;
                    AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();         
                    int layer = Integer.parseInt(row.getValue(Layer).toString());
                    if (layer == 0) {
                        double[] tempDist = new double[2];
                        tempDist[0] = Double.MAX_VALUE;
                        tempDist[1] = Double.MAX_VALUE;
                        Node[] tempNodes = new Node[2];
                        for (int j=0; j<newGraph.getNodeCount(); j++){
                            Node t = indicies.get(j); //picking a target node
                            AttributeRow row2 = (AttributeRow) t.getNodeData().getAttributes();   
                            int layer2 = Integer.parseInt(row2.getValue(Layer).toString());
                            if (layer2 == 2) {
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
                                if (dist < tempDist[0]){
                                    tempDist[0] = dist;
                                    tempNodes[0] = t;
                                }
                            }
                            /*
                            if (layer2 == 1) {
                                double distance = Math.abs(longitude2 - longitude) + Math.abs(latitude2 - latitude);
                                if (distance < tempDist[1]){
                                    tempDist[1] = distance;
                                    tempNodes[1] = t;
                                }
                            }*/
                        }
                        Edge e2 = graphModel.factory().newEdge(s, tempNodes[0], (float) tempDist[0], false);
                        e2.getEdgeData().setLabel("interEdge");
                        newGraph.addEdge(e2);
                        row.setValue(dist2Air, tempDist[0]);
                    }
                }
            break;
            case 1:
                for (int i=0; i<newGraph.getNodeCount(); i++){
                    Node s = indicies.get(i); //picking a source node
                    String name = s.getNodeData().getLabel();
                    name.replace("\"", "");
                    AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();         
                    int layer = Integer.parseInt(row.getValue(Layer).toString());
                    AttributeColumn wDegree = nodeTable.getColumn("Weighted Degree");
                    double degree = Double.parseDouble(row.getValue(wDegree).toString());

                    for (int j=0; j<newGraph.getNodeCount(); j++){
                        Node t = indicies.get(j); //picking a source node
                        if (name.equals(t.getNodeData().getLabel().replace("\"", ""))) { //only diagsonal entries are considered     
                            //System.out.println(name + t.getNodeData().getLabel());
                            AttributeRow row2 = (AttributeRow) t.getNodeData().getAttributes();   
                            int layer2 = Integer.parseInt(row2.getValue(Layer).toString());
                            if ((layer==inputLayer && layer2==baseLayer)||(layer2==inputLayer && layer==baseLayer)) {
                                //System.out.println(layer + layer2 - baseLayer - inputLayer);
                                double weight = degree*0.2;
                                Edge e = graphModel.factory().newEdge(s, t, (float) weight, false);
                                e.getEdgeData().setLabel("interEdge");
                                newGraph.addEdge(e);
                            }
                        }
                    }
                }
            break;
        }
    }
    
    public void reset(GraphModel graphModel) {
        graphModel.setVisibleView(oldView);       //Set the view as current visible view
    }
    
}
