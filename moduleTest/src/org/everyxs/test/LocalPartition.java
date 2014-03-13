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
    public LocalPartition() {
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
        if (isDirected) {//get graph
            graph = gm.getHierarchicalDirectedGraphVisible();
        } else {
            graph = gm.getHierarchicalUndirectedGraphVisible();
        }
        int N = graph.getNodeCount();
        graph.readLock();
        Progress.start(progress);

        HashMap<Integer, Node> indicies = new HashMap<Integer, Node>();
        HashMap<Node, Integer> invIndicies = new HashMap<Node, Integer>();
        int count = 0; //indexing the nodes
        for (Node u : graph.getNodes()) {
            indicies.put(count, u);
            invIndicies.put(u, count);
            count++;
        }

        double[][] adjMatrix = new double[N][N];
        for (int i = 0; i < N; i++) { //build the adjacency Matrix
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
                adjMatrix[i][id] = e.getWeight();
                if (e.isDirected())
                    adjMatrix[id][i] = e.getWeight();
            }
        }

        AttributeTable nodeTable = am.getNodeTable();
        AttributeColumn centrality = nodeTable.getColumn("centrality");
        if (centrality == null) {
            centrality = nodeTable.addColumn("centrality", "Centrality", AttributeType.DOUBLE, AttributeOrigin.COMPUTED, new Double(0));
        }
        AttributeColumn order = am.getNodeTable().getColumn("localOrder");
        if (order == null) {
            order = nodeTable.addColumn("localOrder", "LocalOrder", AttributeType.INT, AttributeOrigin.COMPUTED, new Integer(0));
        }

        Laplacian operator = new Laplacian(graph); //L operator
        LinearTransforms transform = new LinearTransforms(adjMatrix);
        //DoubleMatrix A = new DoubleMatrix(transform.laplacianNorm());
        DoubleMatrix A = null;
        double[][] temp = transform.laplacianNorm(1);
        for (int i=0; i<N; i++)
            for (int j=0; j<N; j++)
                temp[i][j] = -temp[i][j];
        A = new DoubleMatrix(temp);
        /*
        try {
            A = new DoubleMatrix(transform.replicator()); //needs scaling implementation
        } catch (NotConvergedException ex) {
            Exceptions.printStackTrace(ex);
        }*/

        double[] central = new double[N];
        //central[0] = 1; //seed node at index 1 (needs a better GUI for seed selection)
        for (int i =0; i<N; i++)
            central[i] = 1.0/N;
        DoubleMatrix centralVector = new DoubleMatrix(central);
        double t = Double.MAX_VALUE; //find minimum t with given beta and quality bound
        for (int i=0; i<N; i++) {
            Node u = indicies.get(i);
            double tmp = Math.log(2)*graph.getDegree(u)/operator.scale[0]/1/1; //decay =1, quality bound=1 (needs a better GUI for parameter input)

            if (tmp <t)
                t = tmp;
        }
        centralVector = org.jblas.MatrixFunctions.pow(org.jblas.MatrixFunctions.expm(A),t*0.75).mmul(centralVector); //decay =1

        NodeCompare[] list = new NodeCompare[N];
        for (int i = 0; i < N; i++) {
            Node s = indicies.get(i);
            list[i] = new NodeCompare(invIndicies.get(s), centralVector.get(i)/Math.sqrt(operator.scale[0])); //scale needs to be an array with index i
            //Test code
            AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();
            row.setValue(centrality, centralVector.get(i)/Math.sqrt(operator.scale[0]));
            if (isCanceled) {
                return;
            }
        }
        Arrays.sort(list);
        for (int i = 0; i < N; i++) {
            Node s = indicies.get(list[N-i-1].getID()); //picking from a descending order
            AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();
            row.setValue(order, i);
            if (isCanceled) {
                return;
            }
        }

        // The sweep
        AttributeColumn part = nodeTable.getColumn("partition(local)");
        if (part == null) {
            part = nodeTable.addColumn("partition(local)", "LocalPartition", AttributeType.INT, AttributeOrigin.COMPUTED, new Integer(0));
        }
        AttributeColumn eigenCol0 = nodeTable.getColumn("sweepQuality");
        if (eigenCol0 == null) {
            eigenCol0 = nodeTable.addColumn("sweepQuality", "SweepQuality", AttributeType.DOUBLE, AttributeOrigin.COMPUTED, new Double(-1));
        }
        AttributeColumn eigenVmax = am.getNodeTable().getColumn("eigenVector");

        double minCut = Double.MAX_VALUE;
        int[] bestPartition = new int[N];
        double localVolume = 0;
        int sweep = 1;
        while (localVolume < graph.getEdgeCount()/3 && sweep<N) { //the local target volume = 1/4 total volume (needs a better GUI for parameter input)
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
                AttributeRow row1 = (AttributeRow) u.getNodeData().getAttributes();
                EdgeIterable iter;
                if (isDirected) {
                    iter = ((HierarchicalDirectedGraph) graph).getInEdgesAndMetaInEdges(u);
                } else {
                    iter = ((HierarchicalUndirectedGraph) graph).getEdgesAndMetaEdges(u);
                }
                for (Edge e : iter) {
                    Node v = graph.getOpposite(u, e);
                    AttributeRow row2 = (AttributeRow) v.getNodeData().getAttributes();
                    Integer id = invIndicies.get(v);
                    if (partitions[i]<1)
                        volumes[0] += e.getWeight()
                                //* Double.parseDouble(row1.getValue(eigenVmax).toString()) * Double.parseDouble(row2.getValue(eigenVmax).toString())
                                ;
                    else
                        volumes[1] += e.getWeight()
                                //* Double.parseDouble(row1.getValue(eigenVmax).toString()) * Double.parseDouble(row2.getValue(eigenVmax).toString())
                                ;
                    if (partitions[i] != partitions[id])
                        cut += e.getWeight()
                                //* Double.parseDouble(row1.getValue(eigenVmax).toString()) * Double.parseDouble(row2.getValue(eigenVmax).toString())
                                ;
                }
            }
            localVolume = volumes[1];
            double newCut = cut / Math.min(volumes[0], volumes[1]);
            Node s = indicies.get(sweepPoint); //picking from a descending order
            AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();
            row.setValue("sweepQuality", newCut);
            if (newCut < minCut) {
                minCut = newCut;
                for (int i=0; i<N; i++)
                    bestPartition[i] = partitions[i];
            }
            sweep++; //indexing
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
