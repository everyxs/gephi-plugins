/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.everyxs.test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.partition.api.Partition;
import org.gephi.partition.api.PartitionController;
import org.gephi.partition.plugin.NodeColorTransformer;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Edit",
        id = "org.everyxs.test.groundTruth"
)
@ActionRegistration(
        displayName = "#CTL_groundTruth"
)
@ActionReference(path = "Menu/Plugins", position = 3133)
@Messages("CTL_groundTruth=True Partition")
public final class TruthButton implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        //Get the current graph model
        GraphController gc = Lookup.getDefault().lookup(GraphController.class);
        GraphModel graphModel = gc.getModel();       
        AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
        
        //See if graph is well imported
        Graph graph = graphModel.getGraph();
        System.out.println("Nodes: " + graph.getNodeCount());
        System.out.println("Edges: " + graph.getEdgeCount());
        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        
        PartitionController partitionController = Lookup.getDefault().lookup(PartitionController.class);

        //Run modularity algorithm - community detection
        TruePartition gPart = new TruePartition();
        gPart.execute(graphModel, attributeModel);

        //Partition with 'modularity_class', just created by Modularity algorithm
        AttributeColumn modColumn = attributeModel.getNodeTable().getColumn("partition");
        Partition p2 = partitionController.buildPartition(modColumn, graph);
        System.out.println(p2.getPartsCount() + " partitions found");
        NodeColorTransformer nodeColorTransformer2 = new NodeColorTransformer();
        nodeColorTransformer2.randomizeColors(p2);
        partitionController.transform(p2, nodeColorTransformer2);

        //Export
        try {
            ec.exportFile(new File("partition2.pdf"));
        } catch (IOException ex) {
            return;
        }
    }
}
