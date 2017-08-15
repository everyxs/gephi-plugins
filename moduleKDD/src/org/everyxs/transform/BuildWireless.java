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
public class BuildWireless {
    double[] layerWeight;
    int[] delayType;
    double interType;
    GraphView oldView;
    HashMap<Integer, Node> indicies = new HashMap<Integer, Node>();
    HashMap<Node, Integer> invIndicies = new HashMap<Node, Integer>();
    ArrayList<Node> mergeCandidates = new ArrayList<Node>();
    ArrayList<Node> mergeCenters = new ArrayList<Node>();
    
    BuildWireless(double[] layerWeights, int[] layerDelayType, double interType, GraphModel graphModel) {
        layerWeight = layerWeights;
        delayType = layerDelayType;
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

    void build(GraphModel graphModel) {
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
            double degreeWeight = 0;
            
            if (layer == 0) {
                for (Edge e1: newGraph.getEdges(s))
                    degreeWeight += e1.getWeight();
                Edge e0 = graphModel.factory().newEdge(s, s, (float) ((newGraph.getDegree(s)-1)*degreeWeight), false);
                newGraph.addEdge(e0);
                degreeWeight += e0.getWeight();
            }
            else {
                Edge e0 = graphModel.factory().newEdge(s, s, (float) (float) 12/5, false);
                newGraph.addEdge(e0);
            }
            
            for (int j=i+1; j<newGraph.getNodeCount(); j++){
                Node t = indicies.get(j); //picking a target node
                AttributeRow row2 = (AttributeRow) t.getNodeData().getAttributes();   
                int layer2 = Integer.parseInt(row2.getValue(Layer).toString());
                if (layer2 == 1 && layer == 1) {
                    Edge e = newGraph.getEdge(s, t);
                    newGraph.removeEdge(e);
                    Edge e2 = graphModel.factory().newEdge(s, t, (float) 1/60, false);
                    newGraph.addEdge(e2);
                }/*
                if (name.equals(t.getNodeData().getLabel())) { //only diagonal entries are considered     
                    main = true;
                    if (layer2 - layer == 1) {
                        if (layer==0)
                            mergeCenters.add(s);
                        if (isDirected) {
                            Edge e = graphModel.factory().newEdge(s, t, (float) (degreeWeight*0.005), true);
                            //Edge e = graphModel.factory().newEdge(s, t, (float) 1, true);
                            newGraph.addEdge(e);
                        }
                        else {
                            Edge e = graphModel.factory().newEdge(s, t, (float) (degreeWeight*0.005), false);
                            //Edge e = graphModel.factory().newEdge(s, t, (float) 1, false);
                            newGraph.addEdge(e);
                        }
                        if (layer==0) {
                            t.getNodeData().setX(s.getNodeData().x());
                            t.getNodeData().setY(s.getNodeData().y());
                        }
                    }
                    //graphModel.setVisibleView(newView);       //Set the view as current visible view
                }*/
            }
            if (layer==0 && main == false) {//node s dose not show up in other layers
                mergeCandidates.add(s);
            }
        }
    }
    
    void reset(GraphModel graphModel) {
        GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
        //GraphView newView = graphModel.newView();     //Duplicate main view
        Graph newGraph = graphModel.getGraph();
        AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
        AttributeTable nodeTable = attributeModel.getNodeTable();
        AttributeColumn Layer = nodeTable.getColumn("Layer[Z]");

        //AttributeColumn names = nodeTable.getColumn("Label"); //for the same node in different layers
        //PartitionController partitionController = Lookup.getDefault().lookup(PartitionController.class);
        //Partition group = partitionController.buildPartition(names, newGraph); //node instances group
        //Adding inter-layer edges
        for (int i=0; i<newGraph.getNodeCount(); i++){
            Node s = indicies.get(i); //picking a source node
            AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();         
            int layer = Integer.parseInt(row.getValue(Layer).toString());
            
            for (int j=0; j<newGraph.getNodeCount(); j++){
                Node t = indicies.get(j); //picking a target node
                AttributeRow row2 = (AttributeRow) t.getNodeData().getAttributes();   
                int layer2 = Integer.parseInt(row2.getValue(Layer).toString());
                if (layer2 != layer) {
                    Edge e = newGraph.getEdge(s, t);
                    if (e != null )
                        newGraph.removeEdge(e);
                    e = newGraph.getEdge(t, s);
                    if (e != null )
                        newGraph.removeEdge(e);
                }
                else {
                    Edge e = newGraph.getEdge(s, t);
                    if (e != null ) {
                        newGraph.removeEdge(e);
                        Edge e2 = graphModel.factory().newEdge(s, t, (float) 1, false);
                        newGraph.addEdge(e2);
                    }
                }
                    
            }
        }
    }

    void merge(GraphModel graphModel) {
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