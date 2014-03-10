/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.everyxs.test;

import no.uib.cipr.matrix.NotConvergedException;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.HierarchicalGraph;
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
    public HierarchicalGraph graph;
    public double[] scale; //needs poly treatment, graph as a field

    public DynamicOperator(HierarchicalGraph g) {
        GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
        if (graphController != null && graphController.getModel() != null) {
            isDirected = graphController.getModel().isDirected();
        }
        graph = g;
        scale = new double[g.getNodeCount()];
        for (int i=0; i<scale.length; i++)
            scale[i] = 1; //default: no scaling
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
            execute(attributeModel);
        } catch (NotConvergedException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    
    public void setScale(int tuner) { //unifrom scaling tuner for scaled laplacian (need a more flexible version)
        for (int i=0; i<scale.length; i++)
            scale[i] = tuner; //default: no scaling
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
   public abstract double reWeight(int u, int v);
   
}
