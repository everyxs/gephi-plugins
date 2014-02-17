package org.everyxs.test;

import java.util.HashMap;
import static org.everyxs.test.Laplacian.EIGENVECTOR2;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.data.attributes.api.AttributeOrigin;
import org.gephi.data.attributes.api.AttributeRow;
import org.gephi.data.attributes.api.AttributeTable;
import org.gephi.data.attributes.api.AttributeType;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.EdgeIterable;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.HierarchicalDirectedGraph;
import org.gephi.graph.api.HierarchicalGraph;
import org.gephi.graph.api.HierarchicalUndirectedGraph;
import org.gephi.graph.api.Node;
import org.gephi.statistics.plugin.EigenvectorCentrality;
import org.gephi.statistics.spi.Statistics;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.openide.util.Lookup;

/**
 *
 * @author everyan
 */
class GlobalPartition implements org.gephi.statistics.spi.Statistics, org.gephi.utils.longtask.spi.LongTask {
    private ProgressTicket progress;
    private boolean isCanceled;
    private boolean isDirected;
    public GlobalPartition() {
        GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
        if (graphController != null && graphController.getModel() != null) {
            isDirected = graphController.getModel().isDirected();
        }
    }


    /**
     * 
     * @return
     */
    public boolean isDirected() {
        return isDirected;
    }

    /**
     * 
     * @param isDirected
     */
    public void setDirected(boolean isDirected) {
        this.isDirected = isDirected;
    }
    
    @Override
    public void execute(GraphModel gm, AttributeModel am) {      
        HierarchicalGraph graph;
        if (isDirected) {
            graph = gm.getHierarchicalDirectedGraphVisible();
        } else {
            graph = gm.getHierarchicalUndirectedGraphVisible();
        }
        int N = graph.getNodeCount();
        graph.readLock();
        DynamicOperator dynamics = new Replicator(graph);
        dynamics.execute(gm, am);
        //Replicator eigenRatioR = new Replicator(graph);
        //eigenRatioR.execute(gm, am); 
        
        AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
        AttributeColumn order = attributeModel.getNodeTable().getColumn("eigenRatioOrder");
        AttributeColumn eigenVmax = attributeModel.getNodeTable().getColumn("eigenVector");
        AttributeTable nodeTable = attributeModel.getNodeTable();
        AttributeColumn part = nodeTable.getColumn("partition");
        if (part == null) {
            part = nodeTable.addColumn("partition", "GlobalPartition", AttributeType.INT, AttributeOrigin.COMPUTED, new Integer(0));
        }
        AttributeColumn eigenCol0 = nodeTable.getColumn("sweepQuality");
        if (eigenCol0 == null) {
            eigenCol0 = nodeTable.addColumn("sweepQuality", "SweepQuality", AttributeType.DOUBLE, AttributeOrigin.COMPUTED, new Double(-1));
        }
        
        HashMap<Integer, Node> indicies = new HashMap<Integer, Node>();
        HashMap<Node, Integer> invIndicies = new HashMap<Node, Integer>();
        int count = 0;
        for (Node u : graph.getNodes()) {
            indicies.put(count, u);
            invIndicies.put(u, count);
            count++;
        }
        
        double minCut = Double.MAX_VALUE;
        int[] bestPartition = new int[N];
        for (int sweep=1; sweep<N; sweep++) { //the sweep bisector
            int sweepPoint = 1;
            int[] partitions = new int[N]; 
            double[] volumes = new double[2]; //for the demoninator of the quality function
            double cut = 0; //for the numerator of the quality function
            for (int i = 0; i < N; i++) {
                Node u = indicies.get(i); //picking from a descending order
                AttributeRow row = (AttributeRow) u.getNodeData().getAttributes();
                if (Integer.parseInt(row.getValue(order).toString()) < sweep) 
                    partitions[i] = 1;
                if (Integer.parseInt(row.getValue(order).toString()) == sweep)
                    sweepPoint = i;
                if (isCanceled) {
                    return;
                }
            }
            
            for (int i=0; i<N; i++) {
                Node u = indicies.get(i);
                EdgeIterable iter;
                if (isDirected) {
                        iter = ((HierarchicalDirectedGraph) graph).getInEdgesAndMetaInEdges(u);
                    } else {
                        iter = ((HierarchicalUndirectedGraph) graph).getEdgesAndMetaEdges(u);
                    }
                for (Edge e : iter) {
                    Node v = graph.getOpposite(u, e);
                    Integer id = invIndicies.get(v);
                    if (partitions[i]<1)
                        volumes[0] += dynamics.reWeight(i, id)
                                //* Double.parseDouble(row1.getValue(eigenVmax).toString()) * Double.parseDouble(row2.getValue(eigenVmax).toString())
                                ;
                    else 
                        volumes[1] += dynamics.reWeight(i, id)
                                //* Double.parseDouble(row1.getValue(eigenVmax).toString()) * Double.parseDouble(row2.getValue(eigenVmax).toString())
                                ;
                    if (partitions[i] != partitions[id])
                        cut += dynamics.reWeight(i, id)
                                //* Double.parseDouble(row1.getValue(eigenVmax).toString()) * Double.parseDouble(row2.getValue(eigenVmax).toString())
                                ;
                }
            }
            double newCut = cut / Math.min(volumes[0], volumes[1]);
            Node s = indicies.get(sweepPoint); //picking from a descending order
            AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();         
            row.setValue("sweepQuality", newCut); 
            if (newCut < minCut) {
                minCut = newCut;
                for (int i=0; i<N; i++)
                    bestPartition[i] = partitions[i];
            }
        }
        for (int i = 0; i < N; i++) {
            Node s = indicies.get(i); //picking from a descending order
            AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();         
            row.setValue(part, bestPartition[i]); 
            if (isCanceled) {
                return;
            }
        }
        graph.readUnlock();
        Progress.finish(progress);
    }

    @Override
    public String getReport() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean cancel() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setProgressTicket(ProgressTicket pt) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
