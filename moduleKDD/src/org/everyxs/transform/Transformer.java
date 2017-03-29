/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.everyxs.transform;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.EVD;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.NotConvergedException;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.OpenMapRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.data.attributes.api.AttributeOrigin;
import org.gephi.data.attributes.api.AttributeRow;
import org.gephi.data.attributes.api.AttributeTable;
import org.gephi.data.attributes.api.AttributeType;
import static org.gephi.data.attributes.api.AttributeType.DOUBLE;
import org.gephi.datalab.api.AttributeColumnsController;
import org.gephi.datalab.api.GraphElementsController;
import org.gephi.datalab.spi.rows.merge.AttributeRowsMergeStrategy;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.EdgeIterable;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.GraphView;
import org.gephi.graph.api.HierarchicalDirectedGraph;
import org.gephi.graph.api.HierarchicalUndirectedGraph;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.NodeIterable;
import org.openide.util.Lookup;

/**
 * Graph transformer for intra- and inter-layer connections
 * @author everyxs
 */
public class Transformer {
    private boolean directed;
    GraphModel graphModel;
    GraphView oldView;
    HashMap<Integer, Node> indicies = new HashMap<Integer, Node>();
    HashMap<Node, Integer> invIndicies = new HashMap<Node, Integer>();
    OpenMapRealMatrix flowMat;
    
    Transformer(GraphModel gModel) {
        graphModel = gModel;
        oldView = graphModel.getVisibleView();
        Graph graph = graphModel.getGraph(oldView);
        directed = graphModel.isDirected();
        flowMat = new OpenMapRealMatrix(graph.getNodeCount(),graph.getNodeCount());
                
        indicies = new HashMap<Integer, Node>();
        invIndicies = new HashMap<Node, Integer>();
        int count = 0;
        for (Node u : graph.getNodes()) {
            indicies.put(count, u);
            invIndicies.put(u, count);
            count++;
        }
    }
    /**
     * Global transformations for smaller graphs. 
     * @param graphModel
     * @param type
     * @param baseLayer 
     */
    public void globalTransform(GraphModel graphModel, int type, int baseLayer, int inputLayer, double power) throws NotConvergedException {
        
        Graph newGraph = graphModel.getGraphVisible();
        AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
        AttributeTable nodeTable = attributeModel.getNodeTable();
        AttributeTable edgeTable = attributeModel.getEdgeTable();
        AttributeColumn Layer = nodeTable.getColumn("Layer[Z]");
                if (Layer == null) {
                    Layer = nodeTable.addColumn("Layer[Z]", "Layer[Z]", AttributeType.DOUBLE, AttributeOrigin.DATA, new Integer(1));
                    JOptionPane.showMessageDialog(null, "'Layer[Z]' attribute has been added, please edit in the Data laboratory ", 
                            "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
                }
        
        switch (type) {
            case 10:
                for (int i=0; i<newGraph.getNodeCount(); i++){
                    Node s = indicies.get(i); //picking a source node
                    double degree = 0;
                    EdgeIterable iter;
                    if (directed) {
                            iter = ((HierarchicalDirectedGraph) newGraph).getInEdgesAndMetaInEdges(s);
                        } else {
                            iter = ((HierarchicalUndirectedGraph) newGraph).getEdgesAndMetaEdges(s);
                        }
                    for (Edge e : iter) {
                        degree += e.getWeight();
                    }

                    if (degree-1.0>0) {
                        double loop = (degree-1.0)*degree;
                        Edge e0 = graphModel.factory().newEdge(s, s, (float) (loop), false);
                        newGraph.addEdge(e0);
                    }
            }
            break;
            case 11: AttributeColumn Delay = nodeTable.getColumn("Delay");
                if (Delay == null) {
                        Delay = nodeTable.addColumn("Delay", "Delay", AttributeType.DOUBLE, AttributeOrigin.DATA, new Double(1.0));
                        JOptionPane.showMessageDialog(null, "'Delay' attribute has been added, please edit in the Data laboratory ", 
                                "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
                   }
                for (int i=0; i<newGraph.getNodeCount(); i++){
                    Node s = indicies.get(i); //picking a source node
                    double degree = 0;
                    EdgeIterable iter;
                    if (directed) {
                            iter = ((HierarchicalDirectedGraph) newGraph).getInEdgesAndMetaInEdges(s);
                        } else {
                            iter = ((HierarchicalUndirectedGraph) newGraph).getEdgesAndMetaEdges(s);
                        }
                    for (Edge e : iter) {
                        degree += e.getWeight();
                    }
                    AttributeRow row = (AttributeRow) s.getNodeData().getAttributes(); //get custom delay input    
                    double delay = Double.parseDouble(row.getValue(Delay).toString());
                    if (delay-1.0>0) {
                        double loop = (delay-1.0)*degree;
                        Edge e0 = graphModel.factory().newEdge(s, s, (float) (loop), false);
                        newGraph.addEdge(e0);
                    }
            }
            break;
            case 20:
                for (int i=0; i<newGraph.getNodeCount(); i++){
                    Node s = indicies.get(i); //picking a source node
                    double degree = 0;
                    EdgeIterable iter;
                    if (directed) {
                            iter = ((HierarchicalDirectedGraph) newGraph).getInEdgesAndMetaInEdges(s);
                        } else {
                            iter = ((HierarchicalUndirectedGraph) newGraph).getEdgesAndMetaEdges(s);
                        }
                    for (Edge e : iter) {
                        degree += e.getWeight();
                    }
                    degree = Math.pow(degree, power); //raising to the biasSource power
                    if (directed) {
                            iter = ((HierarchicalDirectedGraph) newGraph).getInEdgesAndMetaInEdges(s);
                            for (Edge e : iter) {
                                e.setWeight((float) (e.getWeight() * degree));
                            }
                        } else {
                            iter = ((HierarchicalUndirectedGraph) newGraph).getEdgesAndMetaEdges(s);
                            for (Edge e : iter) {
                                e.setWeight((float) (e.getWeight() * degree));
                            }
                        }
                }
            break;
            case 21: AttributeColumn Bias = nodeTable.getColumn("Bias");
                if (Bias == null) {
                    Bias = nodeTable.addColumn("Bias", "Bias", AttributeType.DOUBLE, AttributeOrigin.DATA, new Double(1.0));
                    JOptionPane.showMessageDialog(null, "'Bias' attribute has been added, please edit in the Data laboratory ", 
                            "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);                    
                }
                for (int i=0; i<newGraph.getNodeCount(); i++){
                    Node s = indicies.get(i); //picking a source node
                    AttributeRow row = (AttributeRow) s.getNodeData().getAttributes(); //get biasSource delay input    
                    double bias = Math.pow(Double.parseDouble(row.getValue(Bias).toString()), power); //raising to the biasSource power
                    
                    EdgeIterable iter;
                    if (directed) {
                            iter = ((HierarchicalDirectedGraph) newGraph).getInEdgesAndMetaInEdges(s);
                            for (Edge e : iter) {
                                e.setWeight((float) (e.getWeight() * bias));
                            }
                        } else {
                            iter = ((HierarchicalUndirectedGraph) newGraph).getEdgesAndMetaEdges(s);
                            for (Edge e : iter) {
                                e.setWeight((float) (e.getWeight() * bias));
                            }
                        }
                    
                }
            break;
            case 22: AttributeColumn DistAir = nodeTable.getColumn("Dist2Air");
                AttributeColumnsController impl = Lookup.getDefault().lookup(AttributeColumnsController.class);
                AttributeColumn Weight = edgeTable.getColumn("Weight");
                AttributeColumn Seats = impl.duplicateColumn(edgeTable, Weight, "Seats", DOUBLE);
                JOptionPane.showMessageDialog(null, "'Seats' attribute has been copied to the edges, 'Weight' now is Dist2AirEdge(sum of endpoints)", 
                    "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
                EdgeIterable iter;
                if (directed)
                    iter = ((HierarchicalDirectedGraph) newGraph).getEdgesAndMetaEdges();
                else 
                    iter = ((HierarchicalUndirectedGraph) newGraph).getEdgesAndMetaEdges();
                Edge[] edgeList = iter.toArray();
                
                for (int i=0; i<edgeList.length; i++){
                    if (edgeList[i].getEdgeData().getLabel().equalsIgnoreCase("Coauthor")) {
                        Node s = edgeList[i].getSource();
                        Node t = edgeList[i].getTarget();
                        AttributeRow row = (AttributeRow) s.getNodeData().getAttributes(); //get biasSource delay input from node table  
                        double biasSource = Math.pow(Double.parseDouble(row.getValue(DistAir).toString()), power); //raising to the biasSource power
                        AttributeRow row2 = (AttributeRow) t.getNodeData().getAttributes(); //get biasSource delay input from node table  
                        double biasTarget = Math.pow(Double.parseDouble(row2.getValue(DistAir).toString()), power); //raising to the biasSource power
                        
                        edgeList[i].setWeight((float) (biasSource*biasTarget)); 
                        /*
                        if (directed) {
                                iter = ((HierarchicalDirectedGraph) newGraph).getInEdgesAndMetaInEdges(s);
                                for (Edge e : iter) {
                                    e.setWeight((float) (e.getWeight() * biasSource));
                                }
                        } else {
                                iter = ((HierarchicalUndirectedGraph) newGraph).getEdgesAndMetaEdges(s);
                                for (Edge e : iter) {
                                    e.setWeight((float) (e.getWeight() * biasSource));
                                }
                        }*/
                    }
                }
            break;
                
            case 31: //customer reweight by flightFlow
                if (directed)
                    iter = ((HierarchicalDirectedGraph) newGraph).getEdgesAndMetaEdges();
                else 
                    iter = ((HierarchicalUndirectedGraph) newGraph).getEdgesAndMetaEdges();
                edgeList = iter.toArray();
                flightFlow(inputLayer, baseLayer);
                for (int i=0; i<edgeList.length; i++){
                    if (edgeList[i].getEdgeData().getLabel().equalsIgnoreCase("Coauthor")) {
                        Node s = edgeList[i].getSource();
                        Node t = edgeList[i].getTarget();
                        Node s2 = indicies.get(i);
                        Node t2 = indicies.get(i+1);
                        boolean map = false;
                        
                        EdgeIterable iter2;
                        if (directed) 
                            iter2 = ((HierarchicalDirectedGraph) newGraph).getInEdgesAndMetaInEdges(s);
                        else 
                            iter2 = ((HierarchicalUndirectedGraph) newGraph).getEdgesAndMetaEdges(s);
                        
                        for (Edge e2 : iter2) {
                            if ("interEdge".equalsIgnoreCase(e2.getEdgeData().getLabel())) {
                                map = true;
                                s2 = e2.getTarget();
                            }
                        }
                        if (map) {
                            map = false;
                            if (directed) 
                                iter2 = ((HierarchicalDirectedGraph) newGraph).getInEdgesAndMetaInEdges(t);
                            else 
                                iter2 = ((HierarchicalUndirectedGraph) newGraph).getEdgesAndMetaEdges(t);

                            for (Edge e2 : iter2) {
                                if ("interEdge".equalsIgnoreCase(e2.getEdgeData().getLabel())) {
                                    map = true;
                                    t2 = e2.getTarget();
                                }
                            }
                        }
                        
                        if (map == true) { // if there is a complete flight map
                            double reweigh = 0;
                            int source = invIndicies.get(s2);
                            int target = invIndicies.get(t2);
                            if (flowMat.getEntry(source, target)>0) 
                                reweigh = flowMat.getEntry(source, target);
                            //edgeList[i].setWeight((float) (edgeList[i].getWeight() * reweigh));
                            edgeList[i].setWeight((float) reweigh);
                        }
                        else // defualt to 0 for exponential regression
                            edgeList[i].setWeight((float) 0);
                    }
                }
            break;
            case 32: //reweighing coauthor by multi affliations
                if (directed)
                    iter = ((HierarchicalDirectedGraph) newGraph).getEdgesAndMetaEdges();
                else 
                    iter = ((HierarchicalUndirectedGraph) newGraph).getEdgesAndMetaEdges();
                edgeList = iter.toArray();
                for (int i=0; i<edgeList.length; i++){
                    if (edgeList[i].getEdgeData().getLabel().equalsIgnoreCase("Coauthor")) {
                        Node s = edgeList[i].getSource();
                        Node t = edgeList[i].getTarget();
                        Node s2 = indicies.get(i);
                        Node t2 = indicies.get(i+1);
                        AttributeRow row1 = (AttributeRow) s.getNodeData().getAttributes(); 
                        AttributeRow row2 = (AttributeRow) t.getNodeData().getAttributes(); 
                        boolean map = false;
                        
                        EdgeIterable iter2;
                        if (directed) 
                            iter2 = ((HierarchicalDirectedGraph) newGraph).getInEdgesAndMetaInEdges(s);
                        else 
                            iter2 = ((HierarchicalUndirectedGraph) newGraph).getEdgesAndMetaEdges(s);
                        
                        for (Edge e2 : iter2) {
                            if ("interEdge".equalsIgnoreCase(e2.getEdgeData().getLabel())) {
                                map = true;
                                s2 = e2.getTarget();
                            }
                        }
                        if (map) {
                            map = false;
                            if (directed) 
                                iter2 = ((HierarchicalDirectedGraph) newGraph).getInEdgesAndMetaInEdges(t);
                            else 
                                iter2 = ((HierarchicalUndirectedGraph) newGraph).getEdgesAndMetaEdges(t);

                            for (Edge e2 : iter2) {
                                if ("interEdge".equalsIgnoreCase(e2.getEdgeData().getLabel())) {
                                    map = true;
                                    t2 = e2.getTarget();
                                }
                            }
                        }
                        
                        if (map == true) { // if there is a complete multi-affliate map
                            double reweigh = 0;
                            if (newGraph.getEdge(s2, t2)!=null) {
                                if ("multiAffli".equalsIgnoreCase(newGraph.getEdge(s2, t2).getEdgeData().getLabel())) { // get the multi-affliate edge\
                                    /*if (s2.getId() == t2.getId())
                                        reweigh = 10; //same geolicaion boost
                                    else {
                                        Edge eInput = newGraph.getEdge(s2, t2);
                                        if (eInput != null)
                                            if (eInput.getWeight()>5)
                                            reweigh = 2; //raising to the biasSource power
                                    */
                                    reweigh = newGraph.getEdge(s2, t2).getWeight();
                                }
                            }
                            //edgeList[i].setWeight((float) (edgeList[i].getWeight() * reweigh));
                            edgeList[i].setWeight((float) reweigh);
                        }
                        else
                            edgeList[i].setWeight((float) -1);
                    }
                }
            break;
        }
    }
    
    /**
     * Connecting base layer with input layer by flightFlowping the inter-layer correspondence.
     * @param inputLayer
     * @param baseLayer 
     */
    public void flightFlow(int inputLayer, int baseLayer) throws NotConvergedException {
        Graph newGraph = graphModel.getGraphVisible();
        AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
        AttributeTable nodeTable = attributeModel.getNodeTable();
        AttributeTable edgeTable = attributeModel.getEdgeTable();
        AttributeColumn Layer = nodeTable.getColumn("Layer[Z]");
        if (Layer == null) {
                JOptionPane.showMessageDialog(null, "'Layer[Z]' attribute is missing ", 
                        "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
           }
        //AttributeColumn Flag = nodeTable.getColumn("check");
        AttributeColumn Distance = edgeTable.getColumn("Distance");
        
        for (int i=0; i<newGraph.getNodeCount(); i++){
            Node s = indicies.get(i); //picking a source node
            AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();         
            int layer = Integer.parseInt(row.getValue(Layer).toString());
                    
            if (layer == inputLayer) { //only input layer pairs are considered    
                for (int j=0; j<newGraph.getNodeCount(); j++){
                    Node t = indicies.get(j); //picking a target node
                    AttributeRow row2 = (AttributeRow) t.getNodeData().getAttributes();   
                    int layer2 = Integer.parseInt(row2.getValue(Layer).toString());
                    if (layer2 == inputLayer) { //only input layer pairs are considered   
                        if (newGraph.getEdge(s, t)!=null){
                            flowMat.setEntry(i, j, newGraph.getEdge(s, t).getWeight());
                            
                            AttributeRow edgeRow = (AttributeRow) newGraph.getEdge(s, t).getEdgeData().getAttributes();
                            double geoDist = Double.parseDouble(edgeRow.getValue(Distance).toString());
                            flowMat.setEntry(i, j, flowMat.getEntry(i, j));
                            //newGraph.getEdge(s, t).setWeight((float) flowMat.getEntry(i, j));
                        }
                    }
                }
            }
        }
        
       
        int size = flowMat.getColumnDimension();
        double[] eVector = new double[size];
        double[][] stochastic = new double[size][size];
        for (int i=0; i<size; i++) {
            for (int j=0; j<size; j++){
                stochastic[i][j] = flowMat.getEntry(i, j);
            }
        }
        double[] sum = new double[size];
        double tsum = 0;
        for (int i=0; i<size; i++) {
            sum[i] = 0;
            for (int j=0; j<size; j++)
                sum[i] += stochastic[j][i];
        }
        for (int i=0; i<size; i++) {
            tsum += sum[i];
            if (sum[i]>1)
                for (int j=0; j<size; j++)
                    stochastic[j][i] = stochastic[j][i]/sum[i]; //column stochastic matrix
        }
        for (int i=0; i<size; i++) {
            eVector[i] = sum[i]/tsum;
        }
        
        RealMatrix fundMat = new OpenMapRealMatrix(size,size);
        for (int i=0; i<size; i++) 
            for (int j=0; j<size; j++) {
                fundMat.setEntry(i, j, eVector[i] - stochastic[i][j]);
                if (i==j)
                    fundMat.addToEntry(i, j, 1);
            }
        fundMat = new LUDecomposition(fundMat).getSolver().getInverse();
        for (int i=0; i<size; i++) 
            for (int j=0; j<size; j++) 
                flowMat.setEntry(i, j, (fundMat.getEntry(j, j)-fundMat.getEntry(i, j))/ sum[j]);
                        
        /*
        RealMatrix flow2Mat = flowMat.multiply(flowMat).scalarMultiply(0.5);
        flowMat = flowMat.add(flow2Mat).add(flow2Mat.multiply(flowMat).scalarMultiply(0.5));
        
        for (int i=0; i<newGraph.getNodeCount(); i++){
            Node s = indicies.get(i); //picking a source node
            AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();
            int check = Integer.parseInt(row.getValue(Flag).toString());
            if (check==1) {
                for (int j=0; j<newGraph.getNodeCount(); j++){
                    Node t = indicies.get(j); //picking a target node
                    AttributeRow row2 = (AttributeRow) t.getNodeData().getAttributes();   
                    int layer2 = Integer.parseInt(row2.getValue(Layer).toString());
                    if (layer2 == inputLayer) { //only input layer pairs are considered   
                        if (newGraph.getEdge(s, t)!=null)
                            newGraph.getEdge(s, t).setWeight((float)flowMat.getEntry(i, j));
                        else {
                            Edge e = graphModel.factory().newEdge(s, t, (float)flowMat.getEntry(i, j), false);
                            if (directed) 
                                e = graphModel.factory().newEdge(s, t, (float)flowMat.getEntry(i, j), true);
                            e.getEdgeData().setLabel("Indy2stops+");
                            newGraph.addEdge(e);
                        }
                            
                    }
                }
            }
        }
        
        for (int i=0; i<newGraph.getNodeCount(); i++){
            Node s = indicies.get(i); //picking a source node
            AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();         
            int layer = Integer.parseInt(row.getValue(Layer).toString());
            
            if (layer == baseLayer) { //only input layer pairs are considered    
                for (int j=0; j<newGraph.getNodeCount(); j++){
                    Node t = indicies.get(j); //picking a target node
                    AttributeRow row2 = (AttributeRow) t.getNodeData().getAttributes();   
                    int layer2 = Integer.parseInt(row2.getValue(Layer).toString());
                    if (layer2 == baseLayer) { //only input layer pairs are considered   
                        if (newGraph.getEdge(s, t)!=null)
                            newGraph.getEdge(s, t).setWeight((float)flowMat.getEntry(i, j));
                        else {
                            Edge e = graphModel.factory().newEdge(s, t, (float)flowMat.getEntry(i, j), false);
                            if (directed) 
                                e = graphModel.factory().newEdge(s, t, (float)flowMat.getEntry(i, j), true);
                            e.getEdgeData().setLabel("2stops+");
                            newGraph.addEdge(e);
                        }
                            
                    }
                }
            }
        }*/
        JOptionPane.showMessageDialog(null, "Flight flow graph has been created", 
                        "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * This function converts decimal degrees to radians	
     * @param deg double
     * @return double
     */
    private static double deg2rad(double deg) {
            return (deg * Math.PI / 180.0);
    }

    /**
     * This function converts radians to decimal degrees
     * @param rad double
     * @return double
     */
    private static double rad2deg(double rad) {
            return (rad * 180 / Math.PI);
    }
    
    /**
     * This function calculates geo distances based on longitudes and latitudes
     * need local node mergers conditioned on resolution
     * @param inputLayer 
     */
    public void distance(int inputLayer) {
        Graph newGraph = graphModel.getGraphVisible();
        AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
        AttributeTable nodeTable = attributeModel.getNodeTable();
        AttributeColumn Layer = nodeTable.getColumn("Layer[Z]");
        AttributeColumn Longitude = nodeTable.getColumn("Lng");
        if (Longitude == null) {
                Longitude = nodeTable.addColumn("Lng", "Lng", AttributeType.DOUBLE, AttributeOrigin.DATA, new Double(1));
                JOptionPane.showMessageDialog(null, "'Lng' attribute has been added, please edit in the Data laboratory ", 
                        "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
           }
        AttributeColumn Latitude = nodeTable.getColumn("Lat");
        if (Latitude == null) {
                Latitude = nodeTable.addColumn("Lat", "Lat", AttributeType.DOUBLE, AttributeOrigin.DATA, new Double(1));
                JOptionPane.showMessageDialog(null, "'Lat' attribute has been added, please edit in the Data laboratory ", 
                        "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
           }
        
        AttributeTable edgeTable = attributeModel.getEdgeTable();
        AttributeColumnsController impl = Lookup.getDefault().lookup(AttributeColumnsController.class);
        AttributeColumn Count = edgeTable.getColumn("Seats");
        if (Count == null) {
            Count = edgeTable.addColumn("Seats", "Seats", AttributeType.DOUBLE, AttributeOrigin.DATA, new Double(1));
            JOptionPane.showMessageDialog(null, "'Count' attribute has been copied to the edges, please edit in the Data laboratory ", 
                    "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
        }
        AttributeColumn Distance = edgeTable.getColumn("Distance");
        if (Distance == null) {
            Distance = edgeTable.addColumn("Distance", "Distance", AttributeType.DOUBLE, AttributeOrigin.DATA, new Double(1));
            JOptionPane.showMessageDialog(null, "'Distance' attribute has been copied to the edges, please edit in the Data laboratory ", 
                    "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
        }
        EdgeIterable iter;
            if (directed)
                iter = ((HierarchicalDirectedGraph) newGraph).getEdgesAndMetaEdges();
            else 
                iter = ((HierarchicalUndirectedGraph) newGraph).getEdgesAndMetaEdges();
            Edge[] edgeList = iter.toArray();
            for (int i=0; i<edgeList.length; i++){
                if (edgeList[i].getEdgeData().getLabel().equalsIgnoreCase("weightedFlight")) {
                    Node s = edgeList[i].getSource();
                    Node t = edgeList[i].getTarget();
                    AttributeRow rowEdge = (AttributeRow) edgeList[i].getEdgeData().getAttributes();
                    rowEdge.setValue(Count, edgeList[i].getWeight());
                    
                    AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();         
                    int layer = Integer.parseInt(row.getValue(Layer).toString());
            
                    if (layer == inputLayer) { //only input layer pairs are considered 
                        AttributeRow row2 = (AttributeRow) t.getNodeData().getAttributes();   
                        int layer2 = Integer.parseInt(row2.getValue(Layer).toString());
                        if (layer2 == inputLayer) { //only input layer pairs are considered    
                            double long1 = Double.parseDouble(row.getValue(Longitude).toString());
                            double long2 = Double.parseDouble(row2.getValue(Longitude).toString());
                            double lati1 = Double.parseDouble(row.getValue(Latitude).toString());
                            double lati2 = Double.parseDouble(row2.getValue(Latitude).toString());

                            double theta = long1 - long2;
                            double dist = Math.sin(deg2rad(lati1)) * Math.sin(deg2rad(lati2)) 
                                    + Math.cos(deg2rad(lati1)) * Math.cos(deg2rad(lati2)) * Math.cos(deg2rad(theta));
                            dist = Math.acos(dist);
                            dist = rad2deg(dist);
                            dist = dist * 60 * 1.1515;
                            if (dist > 20000) //check if the distance is over half quator length
                                dist = 20000;
                            if (dist < 1) //need local node mergers conditioned on resolution
                                dist = 1;
                            rowEdge.setValue(Distance, dist);
                            //if (directed) {
                              //  e = graphModel.factory().newEdge(t, s, (float) dist, true);
                                //e.getEdgeData().setLabel("geoDistance");
                                //newGraph.addEdge(e);
                            //}
                        }
                    }
                }
        }
    }
    
    public void reset(GraphModel graphModel) {
       JOptionPane.showMessageDialog(null, "Under construction...", "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
       //Set the view as current visible view
    }

    void cooccur(int baseLayer, AttributeColumn entity, AttributeColumn venue) {
        Graph newGraph = graphModel.getGraphVisible();
        AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
        AttributeTable nodeTable = attributeModel.getNodeTable();
        AttributeColumn Layer = nodeTable.getColumn("Layer[Z]");
        if (Layer == null) {
                Layer = nodeTable.addColumn("Layer[Z]", "Layer[Z]", AttributeType.DOUBLE, AttributeOrigin.DATA, new Integer(1));
                JOptionPane.showMessageDialog(null, "'Layer[Z]' attribute has been added, please edit in the Data laboratory ", 
                        "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
           }
        AttributeColumn Longitude = nodeTable.getColumn("Lng");
        if (Longitude == null) {
                Longitude = nodeTable.addColumn("Lng", "Lng", AttributeType.DOUBLE, AttributeOrigin.DATA, new Integer(1));
                JOptionPane.showMessageDialog(null, "'Lng' attribute has been added, please edit in the Data laboratory ", 
                        "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
           }
        AttributeColumn Latitude = nodeTable.getColumn("Lat");
        if (Latitude == null) {
                Latitude = nodeTable.addColumn("Lat", "Lat", AttributeType.DOUBLE, AttributeOrigin.DATA, new Integer(1));
                JOptionPane.showMessageDialog(null, "'Lat' attribute has been added, please edit in the Data laboratory ", 
                        "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
           }
        
        for (int i=0; i<newGraph.getNodeCount(); i++){
            Node s = indicies.get(i); //picking a source entity
            AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();         
            int layer = Integer.parseInt(row.getValue(Layer).toString());
            
            if (layer == baseLayer) { //only input layer pairs are considered    
                for (int j=i+1; j<newGraph.getNodeCount(); j++){
                    Node t = indicies.get(j); //picking a source entity
                    AttributeRow row2 = (AttributeRow) t.getNodeData().getAttributes();   
                    int layer2 = Integer.parseInt(row2.getValue(Layer).toString());
                    String paper1 = row.getValue(venue).toString();
                    String paper2 = row2.getValue(venue).toString();
                    if (paper1 == paper2) { //paper match
                        if (layer2 == baseLayer) { //only input layer pairs are considered    
                            double long1 = Double.parseDouble(row.getValue(Longitude).toString());
                            double long2 = Double.parseDouble(row2.getValue(Longitude).toString());
                            double lati1 = Double.parseDouble(row.getValue(Latitude).toString());
                            double lati2 = Double.parseDouble(row2.getValue(Latitude).toString());

                            double theta = long1 - long2;
                            double dist = Math.sin(deg2rad(lati1)) * Math.sin(deg2rad(lati2)) 
                                    + Math.cos(deg2rad(lati1)) * Math.cos(deg2rad(lati2)) * Math.cos(deg2rad(theta));
                            if (Math.abs(dist)>1) //handling numeric error
                                dist = 1;
                            else {
                                dist = Math.acos(dist);
                                dist = rad2deg(dist);
                                dist = dist * 60 * 1.1515; //miles
                                if (dist > 20000) //check if the distance is over half quator length
                                    dist = 20000;
                                if (dist < 1) //need local node mergers conditioned on resolution
                                    dist = 1;
                            }
                            //dist = 1.0/dist;
                            Edge e = graphModel.factory().newEdge(s, t, (float) dist, false);
                            e.getEdgeData().setLabel("geoDistance");
                            newGraph.addEdge(e);
                        }
                    }
                }
            }
        }
    }

    /**
     * This method merges airports according to the same metro map we use for the cities
     * @param mergeMap 
     */
    void portMerge(String mergeMap) {
        GraphElementsController gec = Lookup.getDefault().lookup(GraphElementsController.class);
        AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
        AttributeTable nodeTable = attributeModel.getNodeTable();

        AttributeRowsMergeStrategy[] mergeStrategies = new AttributeRowsMergeStrategy[nodeTable.getColumns().length];
        String line="";
        String id="";
        double[] coordinates = new double[2];
        //Build the standard city properties from GIS lists
        FileReader cityReader = null;
        try {
                cityReader = new FileReader(mergeMap);
        }
        catch (FileNotFoundException ex) {
                ex.printStackTrace();
        }

        int mergeCount = 0;
        BufferedReader cityBuffer =  new BufferedReader(cityReader);
        final Pattern p = Pattern.compile("(?:\"(?:[^\"\\\\]++|\\\\.)*+\"|[^\",]++)++|,"); //csv tokenizer
        try {
                line = cityBuffer.readLine(); //skip header row
                while ((line = cityBuffer.readLine()) != null) { //Read each row
                    Matcher m = p.matcher(line);
                    m.find();// id for lookup
                    id = m.group();
                    m.find();
                    m.find();m.find();//airport name                    
                    m.find();m.find();//metro ID
                    m.find();//Latitude
                    if (!m.group().equals(",")) {
                            coordinates[0] = Double.parseDouble(m.group());
                            m.find();
                    }
                    else //if no geocoding
                        coordinates[0] = 500;
                    m.find();//Longitude
                    if (!m.group().equals(",")) {
                            coordinates[1] = Double.parseDouble(m.group());
                            m.find();
                    }
                    else  //if no geocoding
                        coordinates[1] = 500;
                    m.find();//merge list
                    List<String> portList = Arrays.asList(m.group().split("\\|"));
                    if (!portList.get(0).equals("0")) {
                        ArrayList<Node> list = new ArrayList<Node>();
                        Node center = indicies.get(0);
                        int count = 0;
                        for (Integer key : indicies.keySet()){
                            Node s = indicies.get(key); //picking a source node
                            if (portList.contains(s.getNodeData().getLabel())) {
                                list.add(s);
                                count++;
                                mergeCount++;
                            }
                            if (id.equals(s.getNodeData().getLabel()))
                                center = s;
                        }
                        for (int i=0; i<list.size(); i++) {
                            indicies.remove(invIndicies.get(list.get(i)));
                            invIndicies.remove(list.get(i));
                        }
                        Node[] listArray = list.toArray(new Node[list.size()]);
                        if (count>1)
                            gec.mergeNodes(listArray,center,mergeStrategies, true);
                    }
                }
                System.out.println(mergeCount +" cities read.");
        }
        catch (IOException ex) {
                ex.printStackTrace();
        }
    }
}
