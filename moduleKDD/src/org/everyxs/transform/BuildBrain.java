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
public class BuildBrain {
    double[] layerWeight;
    int[] delayType;
    double interType;
    GraphView oldView;
    HashMap<Integer, Node> indicies = new HashMap<Integer, Node>();
    HashMap<Node, Integer> invIndicies = new HashMap<Node, Integer>();
    ArrayList<Node> mergeCandidates = new ArrayList<Node>();
    ArrayList<Node> mergeCenters = new ArrayList<Node>();
    
    BuildBrain(double[] layerWeights, int[] layerDelayType, double interType, GraphModel graphModel) {
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
            if (layer == 0) {
                    for (Edge e:newGraph.getEdges(s))
                        e.setWeight((float) (e.getWeight()/3.04*3.6));
                }
        }
        
        for (int i=0; i<newGraph.getNodeCount(); i++){
            Node s = indicies.get(i); //picking a source node
            String name = s.getNodeData().getLabel();
            boolean main = false;
            AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();         
            int layer = Integer.parseInt(row.getValue(Layer).toString());
            
            for (int j=i+1; j<newGraph.getNodeCount(); j++){
                Node t = indicies.get(j); //picking a target node
                AttributeRow row2 = (AttributeRow) t.getNodeData().getAttributes();   
                int layer2 = Integer.parseInt(row2.getValue(Layer).toString());
                
                if (name.equals(t.getNodeData().getLabel())) { //only diagonal entries are considered     
                    main = true;
                    /*
                    if (layer2 != layer) {
                        Edge e = newGraph.getEdge(s, t);
                        newGraph.removeEdge(e);
                    }
                    if (layer2 == 3 && layer == 3) {
                        Edge e = newGraph.getEdge(s, t);
                        Edge e2 = graphModel.factory().newEdge(s, t, 2*e.getWeight(), false);
                        newGraph.removeEdge(e);
                        newGraph.addEdge(e2);
                    }*/
                    if (layer2==1 && layer==0) {
                        if (layer==0)
                            mergeCenters.add(s);
                        
                        Node[] neighbors1 = newGraph.getNeighbors(s).toArray();
                        Node[] neighbors2 = newGraph.getNeighbors(t).toArray();
                        float distance= 0;
                        for (Node n1 : neighbors1){
                            for (Node n2 : neighbors2) {
                                if (n2.getNodeData().getLabel().equals(n1.getNodeData().getLabel())) {
                                    Edge e1 = newGraph.getEdge(s, n1);
                                    if (e1 == null)
                                        e1 = newGraph.getEdge(n1, s);
                                    Edge e2 = newGraph.getEdge(t, n2);
                                    if (e2 == null)
                                        e2 = newGraph.getEdge(n2, t);
                                    distance += Math.abs(e1.getWeight() - e2.getWeight());
                                }
                                else
                                    distance += 0.01;
                            }
                        }
                        
                        //Edge e = graphModel.factory().newEdge(s, t, (float) (newGraph.getDegree(t)*0.2), false);
                        Edge e = graphModel.factory().newEdge(s, t, (float) 0.5/distance, false);
                        newGraph.addEdge(e);
                        /*
                        if (layer==0) {
                            t.getNodeData().setX(s.getNodeData().x());
                            t.getNodeData().setY(s.getNodeData().y());
                        }*/
                    }
                    //graphModel.setVisibleView(newView);       //Set the view as current visible view
                }
            }
            if (layer==0 && main == false) {//node s dose not show up in other layers
                mergeCandidates.add(s);
            }
        }
    }
    
    void reset(GraphModel graphModel) {
        graphModel.setVisibleView(oldView);       //Set the view as current visible view
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
