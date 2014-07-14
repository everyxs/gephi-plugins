package org.everyxs.test;

import java.util.Arrays;
import java.util.HashMap;
import no.uib.cipr.matrix.NotConvergedException;
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
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 *
 * @author everyan
 */
class GlobalPartition implements org.gephi.statistics.spi.Statistics, org.gephi.utils.longtask.spi.LongTask {
    private ProgressTicket progress;
    private boolean isCanceled;
    private boolean isDirected;
    private double scalePower;
    private double reweightPower;
    int inputMatrix;
    public GlobalPartition(double scaleP, double weightP, int input) {
        GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
        if (graphController != null && graphController.getModel() != null) {
            isDirected = graphController.getModel().isDirected();
        }
        scalePower = scaleP;
        reweightPower = weightP;
        inputMatrix = input;
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
        
        AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
        AttributeTable nodeTable = attributeModel.getNodeTable();
        AttributeColumn scale = nodeTable.getColumn("scale");
        if (scale == null) {
             scale = nodeTable.addColumn("scale", "scale", AttributeType.DOUBLE, AttributeOrigin.DATA, new Double(1.0));
        }
        graph.readLock();
        DynamicOperator dynamics = null;
        switch (inputMatrix) {
            case 0: //straight up Laplacian
                dynamics = new Laplacian(graph, false);
                break;
            case 2: //reweight by eigenvectors
                try {
                    dynamics = new Replicator(graph);
                } catch (NotConvergedException ex) {
                    Exceptions.printStackTrace(ex);
                }
                break;
            case 4: //reweight by sqrt of degrees
                dynamics = new UnbiasedAdj(graph);
                break;
            default: //normalized Laplacian
                dynamics = new Laplacian(graph, true);
                break;                
        }
        dynamics.setScale(scale);
        dynamics.setWeight(reweightPower);
        dynamics.execute(gm, am);
        
        AttributeColumn order = nodeTable.getColumn("eigenRatioOrder");
        AttributeColumn eigenVmax = nodeTable.getColumn("eigenVector");
        AttributeColumn part = nodeTable.getColumn("partition");
        if (part == null) {
            part = nodeTable.addColumn("partition", "GlobalPartition", AttributeType.INT, AttributeOrigin.COMPUTED, new Integer(0));
        }
        AttributeColumn part2 = nodeTable.getColumn("partition2");
        if (part2 == null) {
            part2 = nodeTable.addColumn("partition2", "GlobalPartition2", AttributeType.INT, AttributeOrigin.COMPUTED, new Integer(0));
        }
        AttributeColumn part3 = nodeTable.getColumn("partition3");
        if (part3 == null) {
            part3 = nodeTable.addColumn("partition3", "GlobalPartition3", AttributeType.INT, AttributeOrigin.COMPUTED, new Integer(0));
        }
        AttributeColumn eigenCol0 = nodeTable.getColumn("sweepQuality");
        if (eigenCol0 == null) {
            eigenCol0 = nodeTable.addColumn("sweepQuality", "SweepQuality", AttributeType.DOUBLE, AttributeOrigin.COMPUTED, new Double(-1));
        }
        
        double[] minCut = new double[3]; //keep track of 2 smallest eigen values
        for (int i=0; i<minCut.length; i++)
            minCut[i] = Double.MAX_VALUE;
        int[][] bestPartition = new int[N][3];
        double newCutOld = Double.MAX_VALUE; //for local minimum detection
        //int[] partitionsOld = new int[N]; //for local minimum detection
        int differenceSignOld = 1; //for local minimum detection
        int[] sweepOld = new int[2]; // window for local minimum detection
        for (int i=0; i<sweepOld.length; i++)
            sweepOld[i] = -N;
        NodeCompare[] localList = new NodeCompare[N];
        for (int i=0; i<N; i++)
            localList[i] = new NodeCompare();
        
        for (int sweep=1; sweep<N; sweep++) { //the sweep bisector
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
                    volumes[0] += dynamics.scale[i]*dynamics.degrees[i];
                else 
                    volumes[1] += dynamics.scale[i]*dynamics.degrees[i];
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
            double newCut = cut / Math.min(volumes[0], volumes[1]);
            Node s = dynamics.indicies.get(sweepPoint); //picking from a descending order
            AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();         
            row.setValue("sweepQuality", newCut);
            
            int differenceSign = 0;
            if (newCut < newCutOld) {
                newCutOld = newCut;
                //partitionsOld = partitions;
                sweepOld[0] = sweep;
                differenceSign = -1;
            }
            else if (newCut > newCutOld) {
                differenceSign = 1;
            }
            boolean flipSign = differenceSignOld <=0 && differenceSign >=0;
            //if (sweep==N-1 && differenceSign==-1)
                //flipSign = true; //for boundary condition at the end of sweep
            if (flipSign)
                localList[sweep] = new NodeCompare(sweepOld[0],  newCutOld);
            differenceSignOld = differenceSign;
            newCutOld = newCut; 
        }
        
        for (int h=0; h<3; h++) {
            Arrays.sort(localList); //resort the local minimums (after local window removal)
            if (localList[0].getAttribute() < Double.MAX_VALUE) {
                int sweep = localList[0].getID();//get the sweep point
                for (int i = 0; i < N; i++) {
                    Node u = dynamics.indicies.get(i); //picking from a descending order
                    AttributeRow row = (AttributeRow) u.getNodeData().getAttributes();
                    if (Integer.parseInt(row.getValue(order).toString()) < sweep) 
                        bestPartition[i][h] = 1;
                    if (localList[i].getID() !=-1 && Math.abs(localList[i].getID()- sweep) <= N*0.05 )
                        localList[i].setAttribute(Double.MAX_VALUE);
                }
            }
        }
        
        for (int i = 0; i < N; i++) {
            Node s = dynamics.indicies.get(i); //picking from a descending order
            AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();         
            row.setValue(part, bestPartition[i][0]); 
            row.setValue(part2, bestPartition[i][1]); 
            row.setValue(part3, bestPartition[i][2]);             
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
