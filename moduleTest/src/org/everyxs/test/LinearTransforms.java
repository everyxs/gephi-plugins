/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.everyxs.test;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.EVD;
import no.uib.cipr.matrix.NotConvergedException;

/**
 *
 * @author everyan
 */
public class LinearTransforms {
    private int size;
    public double[][] adjMatrix;
    
    public LinearTransforms(int n) {
        size = n;
        adjMatrix = new double[size][size];
    }
    
    public LinearTransforms(double[][] adjM) {
        size = adjM.length;
        adjMatrix = new double[size][size];
        for (int i=0; i<adjM.length; i++)
            for (int j=0; j<adjM.length; j++)
                adjMatrix[i][j] = adjM[i][j];
    }
    
    public double[][] laplacian() {
        double[][] laplacian = new double[size][size];
        double sum;
        for (int i=0; i<laplacian.length; i++) {
            sum = 0;
            for (int j=0; j<laplacian.length; j++) 
                sum += adjMatrix[i][j];
            for (int j=0; j<laplacian.length; j++){
                if (j==i)
                    laplacian[i][j] = sum - adjMatrix[i][j];
                else
                    laplacian[i][j] = - adjMatrix[i][j];
                }
            }
        return laplacian;
    }
    
     public double[][] laplacianNorm() {
        double[] sum = new double[size];
        for (int i=0; i<size; i++) {
            sum[i] = 0;
            for (int j=0; j<size; j++) 
                sum[i] += adjMatrix[i][j];
            }
        
        double[][] laplacian = new double[size][size];
        for (int i=0; i<laplacian.length; i++) {
            for (int j=0; j<laplacian.length; j++){
                if (j==i)
                    laplacian[i][j] = 1;
                else
                    laplacian[i][j] = - adjMatrix[i][j] / Math.sqrt(sum[i]*sum[j]);
                }
        }
        return laplacian;
    }
    
    public double[][] replicator() throws NotConvergedException {
        double[][] replicator = new double[size][size];
        DenseMatrix A = new DenseMatrix(adjMatrix);
        EVD eigen = new EVD(adjMatrix.length);
        eigen.factor(A);
        double[] Lambda = eigen.getRealEigenvalues();
        double lambdaMax = 0;
        for (int i=0; i<Lambda.length; i++) 
            if (Lambda[i] > lambdaMax)
                lambdaMax = Lambda[i];   
 
        for (int i=0; i<replicator.length; i++) {
            for (int j=0; j<replicator.length; j++){
                if (j==i)
                    replicator[i][j] = 1 - adjMatrix[i][j]/lambdaMax;
                else
                    replicator[i][j] = - adjMatrix[i][j]/lambdaMax;
                }
            }
        return replicator;
    }
    
    public double[][] unbiasedAdj() throws NotConvergedException {
        double[] sum = new double[size];
        for (int i=0; i<size; i++) {
            sum[i] = 0;
            for (int j=0; j<size; j++) 
                sum[i] += adjMatrix[i][j];
            }
        double[][] unbiasedAdj = new double[size][size];
        for (int i=0; i<unbiasedAdj.length; i++) 
            for (int j=0; j<unbiasedAdj.length; j++)
                unbiasedAdj[i][j] = - adjMatrix[i][j]/ Math.sqrt(sum[i]*sum[j]);
        
        for (int i=0; i<size; i++) {
            sum[i] = 0;
            for (int j=0; j<size; j++) 
                sum[i] += unbiasedAdj[i][j];
            }     
         for (int i=0; i<unbiasedAdj.length; i++) {
            for (int j=0; j<unbiasedAdj.length; j++){
                if (j==i)
                    unbiasedAdj[i][j] = sum[i] - unbiasedAdj[i][j];
                else
                    unbiasedAdj[i][j] = - unbiasedAdj[i][j];
                }
            }
        return unbiasedAdj;
    }
}
