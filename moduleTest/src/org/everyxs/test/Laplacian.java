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
import org.jblas.DoubleMatrix;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 *
 * @author Xiaoran
 */
public class Laplacian extends DynamicOperator {
    public static final String EIGENVECTOR = "eigenVector";
    public static final String EIGENVECTOR2 = "eigenRatioOrder";

    public Laplacian(HierarchicalGraph graph) {
        super(graph);
    }
    
    //constructor for unnormalized laplacian
    public Laplacian(HierarchicalGraph graph, boolean normalized) {
        super(graph);
        if (!normalized){
            double degreeMax = -Double.MAX_VALUE;;
            for (int i=0; i<degrees.length; i++) //find the max degree
                if (degrees[i] > degreeMax)
                    degreeMax = degrees[i];  
            for (int i=0; i<scale.length; i++) //find the max eigenvalue
                scale[i] = degreeMax;
        }
    }

    @Override
    public void execute(AttributeModel attributeModel) throws NotConvergedException {

        AttributeTable nodeTable = attributeModel.getNodeTable();
        AttributeColumn eigenCol = nodeTable.getColumn(EIGENVECTOR);
        AttributeColumn eigenCol2 = nodeTable.getColumn(EIGENVECTOR2);
        /* Test code
        AttributeColumn eigenCol0 = nodeTable.getColumn("eigenRatio");
        if (eigenCol0 == null) {
            eigenCol0 = nodeTable.addColumn("eigenRatio", "EigenRatio", AttributeType.DOUBLE, AttributeOrigin.COMPUTED, new Double(0));
        }*/
        if (eigenCol == null) {
            eigenCol = nodeTable.addColumn(EIGENVECTOR, "EigenVector", AttributeType.DOUBLE, AttributeOrigin.COMPUTED, new Double(0));
        }
        if (eigenCol2 == null) {
            eigenCol2 = nodeTable.addColumn(EIGENVECTOR2, "EigenRatioOrder", AttributeType.INT, AttributeOrigin.COMPUTED, new Integer(0));
        }

        int N = size;
        Progress.start(progress);
        
        double[][] laplacian = laplacianScale(adjMatrix);
        DenseMatrix A = new DenseMatrix(laplacian); //uniform scaling of normalized laplacianScale
        
        EVD eigen = new EVD(N);
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
                        temp[1] = temp [0]; //shifting queue
                        minID[1] = minID[0];
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
        System.out.println("eigen:"+temp[1]);
        
        NodeCompare[] list = new NodeCompare[N];
        for (int i = 0; i < N; i++) {
            Node s = indicies.get(i);
            list[i] = new NodeCompare(i,  Pi.get(i, minID[1])/Pi.get(i, minID[0]));
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

        Progress.finish(progress);
    }
    
    @Override
    public double reWeightedEdge(int u, int v) {
        return adjMatrix[u][v];
    }

    @Override
    public void executeLocal(AttributeModel attributeModel, int seed, double qualityBound) throws NotConvergedException {
        
        AttributeTable nodeTable = attributeModel.getNodeTable();
        AttributeColumn centrality = nodeTable.getColumn("centrality");
        if (centrality == null) {
            centrality = nodeTable.addColumn("centrality", "Centrality", AttributeType.DOUBLE, AttributeOrigin.COMPUTED, new Double(0));
        }
        AttributeColumn order = attributeModel.getNodeTable().getColumn("localOrder");
        if (order == null) {
            order = nodeTable.addColumn("localOrder", "LocalOrder", AttributeType.INT, AttributeOrigin.COMPUTED, new Integer(0));
        }

        DoubleMatrix A = null;
        double[][] temp = laplacianScale(adjMatrix);
        for (int i=0; i<size; i++)
            for (int j=0; j<size; j++) {
                temp[i][j] = -temp[i][j];
            }
        A = new DoubleMatrix(temp);

        double[] central = new double[size];
        central[seed] = 1; //seed node at index 1 (needs a better GUI for seed selection)
        //for (int i =0; i<N; i++)
        //    central[i] = 1.0/N;
        DoubleMatrix centralVector = new DoubleMatrix(central);
        double t = Double.MAX_VALUE; //find minimum t with given beta and quality bound
        for (int i=0; i<size; i++) {
            double tmp = Math.log(2)*scale[i]/degrees[i]/1; //decay =1
            if (tmp <t)
                t = tmp;
        }
        centralVector = org.jblas.MatrixFunctions.pow(org.jblas.MatrixFunctions.expm(A),t/qualityBound/qualityBound).mmul(centralVector);

        NodeCompare[] list = new NodeCompare[size];
        for (int i = 0; i < size; i++) {
            Node s = indicies.get(i);
            list[i] = new NodeCompare(invIndicies.get(s), centralVector.get(i)/Math.sqrt(scale[i])); 
            //Test code
            AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();
            row.setValue(centrality, list[i].getAttribute());
            if (isCanceled) {
                return;
            }
        }
        Arrays.sort(list);
        for (int i = 0; i < size; i++) {
            Node s = indicies.get(list[size-i-1].getID()); //picking from a descending order
            AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();
            row.setValue(order, i);
            if (isCanceled) {
                return;
            }
        }
    }
    
}