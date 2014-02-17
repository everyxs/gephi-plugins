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
    
    public double[][] laplacian(double[] scale) {
        double[][] laplacian = new double[size][size];
        double sum;
        double maxSum = 0;
        for (int i=0; i<laplacian.length; i++) {
            sum = 0;
            for (int j=0; j<laplacian.length; j++) 
                sum += adjMatrix[i][j];
            if (sum > maxSum)
                maxSum = sum;
            for (int j=0; j<laplacian.length; j++){
                if (j==i)
                    laplacian[i][j] = (sum - adjMatrix[i][j])/ Math.sqrt(scale[i]*scale[j]);
                else
                    laplacian[i][j] = - adjMatrix[i][j] / Math.sqrt(scale[i]*scale[j]);
                }
            }
        for (int i=0; i<laplacian.length; i++) {
            for (int j=0; j<laplacian.length; j++){
                 laplacian[i][j] =  laplacian[i][j] / maxSum;
            }
        }
        return laplacian;
    }
    
     public double[][] laplacianNorm(double scale, double[][] reWeightedMatrix) {
        double[] sum = new double[size];
        for (int i=0; i<size; i++) {
            sum[i] = 0;
            for (int j=0; j<size; j++) 
                sum[i] += reWeightedMatrix[i][j];
            }
        
        double[][] laplacian = new double[size][size];
        for (int i=0; i<laplacian.length; i++) {
            for (int j=0; j<laplacian.length; j++){
                if (j==i)
                    laplacian[i][j] = 1/ scale;
                else
                    laplacian[i][j] = - reWeightedMatrix[i][j] / Math.sqrt(sum[i]*sum[j])/ scale;
                }
        }
        return laplacian;
    }
    
    public double[][] replicator() throws NotConvergedException {
        double[][] replicator = new double[size][size];
        DenseMatrix A = new DenseMatrix(adjMatrix);
        EVD eigen = new EVD(adjMatrix.length);
        eigen.factor(A);
        DenseMatrix Pi = eigen.getRightEigenvectors();
        double[] Lambda = eigen.getRealEigenvalues();
        double lambdaMax = -Double.MAX_VALUE;;
        int maxID = -1;
        for (int i=0; i<Lambda.length; i++) 
            if (Lambda[i] > lambdaMax) {
                lambdaMax = Lambda[i];  
                maxID = i;
            }
 
        for (int i=0; i<replicator.length; i++) {
            for (int j=0; j<replicator.length; j++){
                replicator[i][j] = adjMatrix[i][j] * Pi.get(i, maxID) * Pi.get(j, maxID);
            }
        }
        return replicator;
    }
    
        public double[][] replicator2() throws NotConvergedException {
        double[][] replicator = new double[size][size];
        DenseMatrix A = new DenseMatrix(adjMatrix);
        EVD eigen = new EVD(adjMatrix.length);
        eigen.factor(A);
        DenseMatrix Pi = eigen.getRightEigenvectors();
        double[] Lambda = eigen.getRealEigenvalues();
        double[] lambdaMax = new double[2];
        int[] maxID = new int[2];
        for (int i=0; i<lambdaMax.length; i++)
            lambdaMax[i] = -Double.MAX_VALUE;
        for (int i=0; i<Lambda.length; i++)
                if (Lambda[i] > lambdaMax[1]) {
                    if (Lambda[i] > lambdaMax[0]) {
                        lambdaMax[0] = Lambda[i];
                        maxID[0] = i;
                    }
                    else {
                        lambdaMax[1] = Lambda[i];
                        maxID[1] = i;
                    }
                }
 
        for (int i=0; i<replicator.length; i++) {
            for (int j=0; j<replicator.length; j++){
                    replicator[i][j] =  adjMatrix[i][j] * Pi.get(i, maxID[1]) * Pi.get(j, maxID[1]);
                }
            }
        return replicator;
    }
    
    public double[][] unbiasedAdj(double scale) throws NotConvergedException {
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
