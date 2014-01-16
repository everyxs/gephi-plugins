/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.everyxs.test;

import java.util.Arrays;
import java.util.HashMap;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.NotConvergedException;
import no.uib.cipr.matrix.EVD;
import org.gephi.data.attributes.api.AttributeColumn;
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
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 *
 * @author pjmcswee
 */
public class Replicator implements Statistics, LongTask {
    public static final String EIGENVECTOR = "eigenVector(R)";
    public static final String EIGENVECTOR2 = "eigenRatioOrder(R)";
    private ProgressTicket progress;
    private boolean isCanceled;
    private boolean isDirected;
    public double scale;

    public Replicator() {
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

    /**
     *
     * @param graphModel
     * @param attributeModel
     */
    @Override
    public void execute(GraphModel graphModel, AttributeModel attributeModel) {
        HierarchicalGraph graph;
        if (isDirected) {
            graph = graphModel.getHierarchicalDirectedGraphVisible();
        } else {
            graph = graphModel.getHierarchicalUndirectedGraphVisible();
        }
        try {
            execute(graph, attributeModel);
        } catch (NotConvergedException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public void execute(HierarchicalGraph hgraph, AttributeModel attributeModel) throws NotConvergedException {

        AttributeTable nodeTable = attributeModel.getNodeTable();
        AttributeColumn eigenCol = nodeTable.getColumn(EIGENVECTOR);
        AttributeColumn eigenCol2 = nodeTable.getColumn(EIGENVECTOR2);
        /* Test code
        AttributeColumn eigenCol0 = nodeTable.getColumn("eigenRatio");
        if (eigenCol0 == null) {
            eigenCol0 = nodeTable.addColumn("eigenRatio", "EigenRatio", AttributeType.DOUBLE, AttributeOrigin.COMPUTED, new Double(0));
        }*/
        if (eigenCol == null) {
            eigenCol = nodeTable.addColumn(EIGENVECTOR, "EigenVector(R)", AttributeType.DOUBLE, AttributeOrigin.COMPUTED, new Double(0));
        }
        if (eigenCol2 == null) {
            eigenCol2 = nodeTable.addColumn(EIGENVECTOR2, "EigenRatioOrder(R)", AttributeType.INT, AttributeOrigin.COMPUTED, new Integer(0));
        }

        int N = hgraph.getNodeCount();
        hgraph.readLock();

        Progress.start(progress);

        HashMap<Integer, Node> indicies = new HashMap<Integer, Node>();
        HashMap<Node, Integer> invIndicies = new HashMap<Node, Integer>();
        int count = 0;
        for (Node u : hgraph.getNodes()) {
            indicies.put(count, u);
            invIndicies.put(u, count);
            count++;
        }
        double[][] adjMatrix = new double[N][N];
        for (int i = 0; i < N; i++) {
            Node u = indicies.get(i);
            EdgeIterable iter;
            if (isDirected) {
                    iter = ((HierarchicalDirectedGraph) hgraph).getInEdgesAndMetaInEdges(u);
                } else {
                    iter = ((HierarchicalUndirectedGraph) hgraph).getEdgesAndMetaEdges(u);
                }
            for (Edge e : iter) {
                    Node v = hgraph.getOpposite(u, e);
                    Integer id = invIndicies.get(v);
                    adjMatrix[i][id] = e.getWeight();
                }
        }
        LinearTransforms replicator = new LinearTransforms(adjMatrix);     
        DenseMatrix A = new DenseMatrix(replicator.replicator());
        EVD eigen = new EVD(adjMatrix.length);
        eigen.factor(A);
        DenseMatrix Pi = eigen.getRightEigenvectors();
        double[] Lambda = eigen.getRealEigenvalues();
        int[] minID = new int[3]; //keep track of 2 smallest eigen values
        for (int i=0; i<minID.length; i++)
            minID[i] = -1;
        double[] temp = new double[2];
        for (int i=0; i<temp.length; i++)
            temp[i] = Double.MAX_VALUE;
        for (int i=0; i<Lambda.length; i++) {
            //if (Lambda[i] < temp[2]) {
                if (Lambda[i] < temp[1]) {
                    if (Lambda[i] < temp[0]) {
                        temp[0] = Lambda[i];
                        minID[0] = i;
                    }
                    else {
                        temp[1] = Lambda[i];
                        minID[1] = i;
                    }
                }/* for top 3 smalleest eigen values
                else {
                    temp[2] = Lambda[i];
                    minID[2] = i;
                }
            }*/
        }
        
        NodeCompare[] list = new NodeCompare[N];
        for (int i = 0; i < N; i++) {
            Node s = indicies.get(i);
            list[i] = new NodeCompare(invIndicies.get(s),  Pi.get(i, minID[1])/Pi.get(i, minID[0]));
            //Test code
            AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();         
            row.setValue(eigenCol, Pi.get(i, minID[0])); 
            if (isCanceled) {
                return;
            }
        }
        Arrays.sort(list);
        
        for (int i = 0; i < N; i++) {
            Node s = indicies.get(list[N-i-1].getID()); //picking from a descending order
            AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();         
            row.setValue(eigenCol2, i); 
            if (isCanceled) {
                return;
            }
        }
        hgraph.readUnlock();

        Progress.finish(progress);
    }

    @Override
    public String getReport() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean cancel() {
        this.isCanceled = true;
        return true;
    }

    public void setProgressTicket(ProgressTicket progressTicket) {
        this.progress = progressTicket;

    }
}

