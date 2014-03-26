/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.everyxs.test;

import java.util.HashMap;
import no.uib.cipr.matrix.NotConvergedException;
import org.gephi.data.attributes.api.AttributeModel;
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
import org.gephi.utils.progress.ProgressTicket;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 *
 * @author everyan
 */
public abstract class DynamicOperator implements Statistics, LongTask {
    ProgressTicket progress;
    boolean isCanceled;
    boolean isDirected;
    int size;
    public double[][] adjMatrix;
    public double[] scale; //scaling factor diagonal terms
    double[] degrees;
    HashMap<Integer, Node> indicies = new HashMap<Integer, Node>();
    HashMap<Node, Integer> invIndicies = new HashMap<Node, Integer>();
    
    public DynamicOperator(HierarchicalGraph graph) {
        GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
        if (graphController != null && graphController.getModel() != null) {
            isDirected = graphController.getModel().isDirected();
        }
        indicies = new HashMap<Integer, Node>();
        invIndicies = new HashMap<Node, Integer>();
        int count = 0;
        for (Node u : graph.getNodes()) {
            indicies.put(count, u);
            invIndicies.put(u, count);
            count++;
        }
        size = count;
        adjMatrix = new double[size][size];
        scale = new double[size];
        degrees = new double[size];
        for (int i = 0; i < size; i++) {
            Node u = indicies.get(i);
            scale[i] = graph.getDegree(u); //default: normalized laplaican
            degrees[i] = scale[i];
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
        try {
            execute(attributeModel);
        } catch (NotConvergedException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    
    public double[][] laplacianScale(double[][] inputMatrix) {
        double[][] laplacian = new double[size][size];
        double sum;
        for (int i=0; i<size; i++) {
            sum = 0;
            for (int j=0; j<size; j++)  {
                sum += inputMatrix[i][j];
                if (sum <=0)
                    sum = Double.MIN_VALUE;
            }
            for (int j=0; j<size; j++){
                if (j==i)
                    laplacian[i][j] = (sum - inputMatrix[i][j])/ Math.sqrt(scale[i]*scale[j]);
                else
                    laplacian[i][j] = - inputMatrix[i][j] / Math.sqrt(scale[i]*scale[j]);
             }
         }
        return laplacian;
    }
    
    public void setScale(double tuner) { //unifrom scaling tuner for scaled laplacianScale (need a more flexible version)
        for (int i=0; i<scale.length; i++)
            scale[i] = Math.pow(scale[i], tuner); //raise scale to powers
    }

    @Override
    public String getReport() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean cancel() {
        this.isCanceled = true;
        return true;
    }

    public void setProgressTicket(ProgressTicket progressTicket) {
        this.progress = progressTicket;

    }
   
   public abstract void execute(AttributeModel attributeModel) throws NotConvergedException;
   public abstract void executeLocal(AttributeModel attributeModel, int seed, double qualityBound) throws NotConvergedException;
   public abstract double reWeightedEdge(int u, int v);
   
}
