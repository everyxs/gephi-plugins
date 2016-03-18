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
public class BuildUKtraffic {

    double interType;
    GraphView oldView;
    HashMap<Integer, Node> indicies = new HashMap<Integer, Node>();
    HashMap<Node, Integer> invIndicies = new HashMap<Node, Integer>();
    ArrayList<Node> mergeCandidates = new ArrayList<Node>();
    ArrayList<Node> mergeCenters = new ArrayList<Node>();
    
    public BuildUKtraffic(double interType, GraphModel graphModel) {
        interType = interType;
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

    public void build(GraphModel graphModel) {
        GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
        Boolean isDirected = false;
        if (graphController != null && graphController.getModel() != null) {
            isDirected = graphController.getModel().isDirected();
        }
        //GraphView newView = graphModel.newView();     //Duplicate main view
        Graph newGraph = graphModel.getGraph();
        AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
        AttributeTable nodeTable = attributeModel.getNodeTable();
        AttributeColumn Layer = nodeTable.getColumn("layer[Z]");
        AttributeColumn Total = nodeTable.getColumn("totalDegree");
        AttributeColumn Longitude = nodeTable.getColumn("longitude");
        AttributeColumn Latitude = nodeTable.getColumn("latitude");

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
                double total = 0;
                if (!"null".equals(row.getValue(Total).toString()))
                     total = Double.parseDouble(row.getValue(Total).toString());
    /*            
                if (layer == 0) {
                    double degree = 0;
                    for (Edge e1: newGraph.getEdges(s))
                        degree += e1.getWeight();
                    Edge e0 = graphModel.factory().newEdge(s, s, (float) (degree*1.0-1), false);
                    newGraph.addEdge(e0);
                }
    */          double longitude = Double.parseDouble(row.getValue(Longitude).toString());
                double latitude = Double.parseDouble(row.getValue(Latitude).toString());
                double[] tempDist = new double[2];
                tempDist[0] = Double.MAX_VALUE;
                tempDist[1] = Double.MAX_VALUE;
                Node[] tempNodes = new Node[2];
     /*         for (int j=i+1; j<newGraph.getNodeCount(); j++){
                    Node t = indicies.get(j); //picking a target node
                    AttributeRow row2 = (AttributeRow) t.getNodeData().getAttributes();   
                    int layer2 = Integer.parseInt(row2.getValue(Layer).toString());
                    if (name.equals(t.getNodeData().getLabel())) { //only diagonal entries are considered 
                        if (layer2 != 0) {
                            Edge e = newGraph.getEdge(s, t);
                            if (e!=null) {
                                newGraph.removeEdge(e);
                            }
                            Edge e2 = graphModel.factory().newEdge(s, t, (float) (total), true);
                            newGraph.addEdge(e2);
                            Edge e3 = graphModel.factory().newEdge(t, s, (float) (total), true);
                            newGraph.addEdge(e3);
                        }
                    }
                } */
                for (int j=i+1; j<newGraph.getNodeCount(); j++){
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
                    if (layer2 == 4) {
                        double distance = Math.abs(longitude2 - longitude) + Math.abs(latitude2 - latitude);
                        if (distance < tempDist[1]){
                            tempDist[1] = distance;
                            tempNodes[1] = t;
                        }
                    }
                }
                
                if (1E-10< tempDist[0] && tempDist[0] < 5E-7) {
                    Edge e = newGraph.getEdge(s, tempNodes[0]);
                    if (e!=null) {
                        newGraph.removeEdge(e);
                    }
                    Edge e2 = graphModel.factory().newEdge(s, tempNodes[0], (float) (total/2), true);
                    newGraph.addEdge(e2);
                    Edge e3 = graphModel.factory().newEdge(tempNodes[0], s, (float) (total/2), true);
                    newGraph.addEdge(e3);
                }
                
                if (1E-10< tempDist[1] && tempDist[1] < 5E-7) {
                    Edge e = newGraph.getEdge(s, tempNodes[1]);
                    if (e!=null) {
                        newGraph.removeEdge(e);
                    }
                    Edge e2 = graphModel.factory().newEdge(s, tempNodes[1], (float) (total/2), true);
                    newGraph.addEdge(e2);
                    Edge e3 = graphModel.factory().newEdge(tempNodes[1], s, (float) (total/2), true);
                    newGraph.addEdge(e3);
                }
                        
                
                
            }
        }
    }
    
    public void reset(GraphModel graphModel) {
        graphModel.setVisibleView(oldView);       //Set the view as current visible view
    }

    public void merge(GraphModel graphModel) {
        //GraphView newView = graphModel.newView();     //Duplicate main view
        Graph newGraph = graphModel.getGraph();
        AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
        AttributeTable nodeTable = attributeModel.getNodeTable();
        AttributeColumn Layer = nodeTable.getColumn("Layer[Z]");
        GraphElementsController GEController = Lookup.getDefault().lookup(GraphElementsController.class);
        for (Node c : mergeCenters) {
            Node[] neighbors = newGraph.getNeighbors(c).toArray();
            ArrayList<Node> mergeList = new ArrayList<Node>();
            mergeList.add(c);
            for (Node n : neighbors) {
                AttributeRow row2 = (AttributeRow) n.getNodeData().getAttributes();         
                int layer2 = Integer.parseInt(row2.getValue(Layer).toString());
                if (layer2==0 && !mergeCenters.contains(n)) {
                    mergeList.add(n);
                }
            }
            Node[] mergeArray = new Node[mergeList.size()];
            mergeArray = mergeList.toArray(mergeArray);
            GEController.groupNodes(mergeArray);
            for (Node n : mergeArray)
                mergeCandidates.remove(n);
        }
        for (Node m : mergeCandidates) {
            Node[] neighbors = newGraph.getNeighbors(m).toArray();
            ArrayList<Node> mergeList = new ArrayList<Node>();
            mergeList.add(m);
            for (Node n : neighbors) {
                AttributeRow row2 = (AttributeRow) n.getNodeData().getAttributes();         
                int layer2 = Integer.parseInt(row2.getValue(Layer).toString());
                if (layer2==0) {
                    mergeList.add(n);
                }
            }
            Node[] mergeArray = new Node[mergeList.size()];
            mergeArray = mergeList.toArray(mergeArray);
            GEController.groupNodes(mergeArray);
        }
    }
    
}
