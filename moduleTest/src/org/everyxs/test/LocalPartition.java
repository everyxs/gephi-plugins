package org.everyxs.test;

import java.util.Arrays;
import java.util.HashMap;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.EVD;
import no.uib.cipr.matrix.NotConvergedException;
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
import org.gephi.statistics.spi.Statistics;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.jblas.DoubleMatrix;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 *
 * @author everyan
 */
class LocalPartition implements org.gephi.statistics.spi.Statistics, org.gephi.utils.longtask.spi.LongTask {
    private ProgressTicket progress;
    private boolean isCanceled;
    private boolean isDirected;
    private double scalePower;
    int inputMatrix;
    double targetVolume;
    int seed;
    double qualityBound;
    public LocalPartition(double scaleP, int input, double targetV, int s, double qBound) {
        GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
        if (graphController != null && graphController.getModel() != null) {
            isDirected = graphController.getModel().isDirected();
        }
        scalePower = scaleP;
        inputMatrix = input;
        targetVolume = targetV;
        seed = s;
        qualityBound = qBound;
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
        DynamicOperator dynamics = null;
        switch (inputMatrix) {
            case 0: //straight up laplacianScale
                dynamics = new Laplacian(graph, false);
                break;
            case 2: //reweight by eigenvectors
                try {
                    dynamics = new Replicator(graph);
                } catch (NotConvergedException ex) {
                    Exceptions.printStackTrace(ex);
                }
                break;
            case 4: //reweight by degrees
                dynamics = new UnbiasedAdj(graph);
                break;
            default: //normalized laplacianScale
                dynamics = new Laplacian(graph);
                break;                
        }
        dynamics.setScale(scalePower);
        try {
            dynamics.executeLocal(am, seed, qualityBound);
        } catch (NotConvergedException ex) {
            Exceptions.printStackTrace(ex);
        }
        // The sweep
        AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
        AttributeColumn order = attributeModel.getNodeTable().getColumn("localOrder");
        AttributeColumn eigenVmax = attributeModel.getNodeTable().getColumn("centrality");
        AttributeTable nodeTable = attributeModel.getNodeTable();
        AttributeColumn part = nodeTable.getColumn("partition(local)");
        if (part == null) {
            part = nodeTable.addColumn("partition(local)", "LocalPartition", AttributeType.INT, AttributeOrigin.COMPUTED, new Integer(0));
        }
        AttributeColumn part2 = nodeTable.getColumn("partition2(local)");
        if (part2 == null) {
            part2 = nodeTable.addColumn("partition2(local)", "LocalPartition2", AttributeType.INT, AttributeOrigin.COMPUTED, new Integer(0));
        }
        AttributeColumn part3 = nodeTable.getColumn("partition3(local)");
        if (part3 == null) {
            part3 = nodeTable.addColumn("partition3(local)", "LocalPartition3", AttributeType.INT, AttributeOrigin.COMPUTED, new Integer(0));
        }        
        AttributeColumn eigenCol0 = nodeTable.getColumn("localQuality");
        if (eigenCol0 == null) {
            eigenCol0 = nodeTable.addColumn("localQuality", "LocalQuality", AttributeType.DOUBLE, AttributeOrigin.COMPUTED, new Double(-1));
        }

        double[] minCut = new double[3]; //keep track of 2 smallest eigen values
        for (int i=0; i<minCut.length; i++)
            minCut[i] = Double.MAX_VALUE;
        int[][] bestPartition = new int[N][3];
        double newCutOld = Double.MAX_VALUE; //for local minimum detection
        int[] partitionsOld = new int[N]; //for local minimum detection
        int differenceSignOld = 0; //for local minimum detection
        int[] sweepOld = new int[2]; // window for local minimum detection
        for (int i=0; i<sweepOld.length; i++)
            sweepOld[i] = -N;
        
        double localVolume = 0;
        double totalVolume = 0;
        for (int i=0; i<N; i++)
            totalVolume += dynamics.scale[i];
        int sweep = 1;
        while (localVolume < targetVolume*totalVolume && sweep<N) { // the sweep bisector with the localTargetVolume in [0,1]
            int sweepPoint = 1;
            int[] partitions = new int[N]; 
            double[] volumes = new double[2]; //for the demoninator of the quality function
            double cut = 0; //for the numerator of the quality function
            for (int i = 0; i < N; i++) {
                Node u = dynamics.indicies.get(i); //picking from a descending order
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
                Node u = dynamics.indicies.get(i);
                if (partitions[i]<1)
                    volumes[0] += dynamics.scale[i];
                else 
                    volumes[1] += dynamics.scale[i];
                EdgeIterable iter;
                if (isDirected) {
                        iter = ((HierarchicalDirectedGraph) graph).getInEdgesAndMetaInEdges(u);
                    } else {
                        iter = ((HierarchicalUndirectedGraph) graph).getEdgesAndMetaEdges(u);
                    }
                for (Edge e : iter) {
                    Node v = graph.getOpposite(u, e);
                    Integer id = dynamics.invIndicies.get(v);
                    if (partitions[i] != partitions[id])
                        cut += dynamics.reWeightedEdge(i, id);
                }
            }
            localVolume = volumes[1];
            double newCut = cut / Math.min(volumes[0], volumes[1]);
            Node s = dynamics.indicies.get(sweepPoint); //picking from a descending order
            AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();         
            row.setValue("localQuality", newCut);

            if (newCut < minCut[2]) {
                minCut[2] = newCut;
                for (int i=0; i<N; i++)
                    bestPartition[i][2] = partitions[i];
            }
            sweep++; //increment the counter
        }
        for (int i = 0; i < N; i++) {
            Node s = dynamics.indicies.get(i); //picking from a descending order
            AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();         
            row.setValue(part, bestPartition[i][2]); 
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
