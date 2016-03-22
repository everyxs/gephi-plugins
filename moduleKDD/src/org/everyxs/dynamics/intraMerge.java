/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.everyxs.dynamics;

import java.util.HashMap;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.data.attributes.api.AttributeRow;
import org.gephi.data.attributes.api.AttributeTable;
import org.gephi.datalab.api.GraphElementsController;
import org.gephi.datalab.spi.rows.merge.AttributeRowsMergeStrategy;
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
public class intraMerge {
    GraphView oldView;
    HashMap<Integer, Node> indicies = new HashMap<Integer, Node>();
    HashMap<Node, Integer> invIndicies = new HashMap<Node, Integer>();
    
    intraMerge(GraphModel graphModel) {
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

    void merge(GraphModel graphModel) {
        GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
        Boolean isDirected = false;
        if (graphController != null && graphController.getModel() != null) {
            isDirected = graphController.getModel().isDirected();
        }
        GraphView newView = graphModel.newView();     //Duplicate main view
        Graph newGraph = graphModel.getGraph(newView);
        AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
        AttributeTable nodeTable = attributeModel.getNodeTable();
        AttributeColumn Layer = nodeTable.getColumn("Layer[Z]");
        
        AttributeRowsMergeStrategy[] mergeStrategy = new AttributeRowsMergeStrategy[attributeModel.getNodeTable().getColumns().length];
        GraphElementsController GEController = Lookup.getDefault().lookup(GraphElementsController.class);
        //AttributeColumn names = nodeTable.getColumn("Label"); //for the same node in different layers
        //PartitionController partitionController = Lookup.getDefault().lookup(PartitionController.class);
        //Partition group = partitionController.buildPartition(names, newGraph); //node instances group
        //Adding inter-layer edges
        NodeIterable Nodes = newGraph.getNodes();
        for (Node s: Nodes){
            String name = s.getNodeData().getLabel();
            AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();         
            int layer = Integer.parseInt(row.getValue(Layer).toString());
            if (layer==0 && newGraph.getDegree(s)==2) {//needs extension to directed and multi-edge case
                    Node[] neighbors = newGraph.getNeighbors(s).toArray();
                    GEController.groupNodes(neighbors);
                    //GEController.mergeNodes(neighbors, s, mergeStrategy, true);     
                }
        }
        graphModel.setVisibleView(newView);       //Set the view as current visible view
    }
    
}
