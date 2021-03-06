/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.everyxs.test;

import java.util.HashMap;
import no.uib.cipr.matrix.NotConvergedException;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.data.attributes.api.AttributeRow;
import org.gephi.data.attributes.api.AttributeTable;
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
    public double[] scale; //node delay scaling factor diagonal terms
    public double[] reweight; //reweighting factor diagonal terms 
    double[] degrees; //degrees vector of the interaction graph
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
        reweight = new double[size];
        degrees = new double[size];
        for (int i = 0; i < size; i++) {
            Node u = indicies.get(i);
            scale[i] =1; //default: normalized laplaican
            reweight[i] = 1;
            degrees[i] =  graph.getDegree(u);
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
    
    /**
     * This function implements the node delay factor scaling of the operator, with the constrain \tau_min = 1
     * @param inputMatrix double[][]
     */   
    public double[][] delayScale(double[][] inputMatrix) {
        double[][] laplacian = new double[size][size];
        for (int i=0; i<size; i++) {
            for (int j=0; j<size; j++){
                laplacian[i][j] = inputMatrix[i][j]/ Math.sqrt(scale[i]*scale[j]);
            }
        }
        return laplacian;
    }
    
    public double[][] laplacianNorm(double[][] reWeightedMatrix) {
        double[] sum = new double[size];
        for (int i=0; i<size; i++) {
            sum[i] = 0;
            for (int j=0; j<size; j++)
                sum[i] += reWeightedMatrix[i][j];
            degrees[i] = sum[i];
        }
        double[][] laplacian = new double[size][size];
        for (int i=0; i<laplacian.length; i++) {
            for (int j=0; j<laplacian.length; j++){
                if (j==i)
                    laplacian[i][j] = 1- reWeightedMatrix[i][j] / Math.sqrt(sum[i]*sum[i]);
                else //if (sum[i]*sum[j] >= 0)
                    laplacian[i][j] = - reWeightedMatrix[i][j] / Math.sqrt(sum[i]*sum[j]);
                //else
                    //laplacian[i][j] = reWeightedMatrix[i][j] / Math.sqrt(-sum[i]*sum[j]);
                }
        }
        return laplacian;
    }
         
    public double[][] reweight(double[][] inputMatrix) {
        double[][] outputMatrix = new double[size][size];
        for (int i=0; i<size; i++) {
            for (int j=0; j<size; j++)
                outputMatrix[i][j] = inputMatrix[i][j] * reweight[i]*reweight[j];
         }
        return outputMatrix;
    }
    
    public void setScale(double tuner) { //unifrom scaling tuner for scaled delayScale (need a more flexible version)
        for (int i=0; i<scale.length; i++) {
            if (tuner > 0)
                scale[i] = scale[i]* tuner; //raise scale to powers
            else
                scale[i] = 1;
        }
            
    }
    
    public void setScale(AttributeColumn input) { //unifrom scaling tuner for scaled laplacianScale (need a more flexible version)
        for (int i=0; i<scale.length; i++) {
            Node s = indicies.get(i); //picking from a descending order
            AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();
            scale[i] = scale[i]*Double.parseDouble(row.getValue(input).toString());
        }
    }

    public void setWeight(double tuner) { //unifrom scaling tuner for scaled delayScale (need a more flexible version)
        for (int i=0; i<reweight.length; i++)
            reweight[i] = Math.pow(reweight[i], tuner); //raise scale to powers
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
    public double reWeightedEdge(int u, int v) {    
        return adjMatrix[u][v]*reweight[u]*reweight[v];
    }
    
    public abstract void execute(AttributeModel attributeModel) throws NotConvergedException;
    public abstract void executeLocal(AttributeModel attributeModel, int seed, double qualityBound) throws NotConvergedException;
   
}
