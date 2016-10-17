/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.everyxs.transform;

import java.util.ArrayList;
import java.util.HashMap;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.data.attributes.api.AttributeRow;
import org.gephi.data.attributes.api.AttributeTable;
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
                        double longitude = Double.parseDouble(row.getValue(Longitude).toString());
                        double latitude = Double.parseDouble(row.getValue(Latitude).toString());
                        double[] tempDist = new double[2];
                        tempDist[0] = Double.MAX_VALUE;
                        tempDist[1] = Double.MAX_VALUE;
                        Node[] tempNodes = new Node[2];
                        for (int j=0; j<newGraph.getNodeCount(); j++){
                            Node t = indicies.get(j); //picking a target node
                            AttributeRow row2 = (AttributeRow) t.getNodeData().getAttributes();   
                            int layer2 = Integer.parseInt(row2.getValue(Layer).toString());
                            double longitude2 = Double.parseDouble(row2.getValue(Longitude).toString());
                            double latitude2 = Double.parseDouble(row2.getValue(Latitude).toString());
                            if (layer2 == 2) {
                                double distance = Math.abs(longitude2 - longitude) + Math.abs(latitude2 - latitude);
                                if (distance < tempDist[0]){
                                    tempDist[0] = distance;
                                    tempNodes[0] = t;
                                }
                            }
                            if (layer2 == 1) {
                                double distance = Math.abs(longitude2 - longitude) + Math.abs(latitude2 - latitude);
                                if (distance < tempDist[1]){
                                    tempDist[1] = distance;
                                    tempNodes[1] = t;
                                }
                            }
                        }
                        Edge e2 = graphModel.factory().newEdge(s, tempNodes[0], (float) tempDist[0], false);
                        e2.getEdgeData().setLabel("interEdge");
                        newGraph.addEdge(e2);
                    }
                }
            break;
            case 1:
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
            break;
        }
    }
    
    public void reset(GraphModel graphModel) {
        graphModel.setVisibleView(oldView);       //Set the view as current visible view
    }
    
}
