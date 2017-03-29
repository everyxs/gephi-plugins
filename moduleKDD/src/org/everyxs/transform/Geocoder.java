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
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
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
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.EdgeIterable;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.GraphView;
import org.gephi.graph.api.HierarchicalDirectedGraph;
import org.gephi.graph.api.HierarchicalGraph;
import org.gephi.graph.api.HierarchicalUndirectedGraph;
import org.gephi.graph.api.Node;
import org.openide.util.Lookup;

/**
 *
 * @author everyan
 */
public class Geocoder {
    boolean directed;
    GraphView oldView;
    HierarchicalGraph graph;
    HashMap<Integer, Node> indicies = new HashMap<Integer, Node>();
    HashMap<Node, Integer> invIndicies = new HashMap<Node, Integer>();
    ArrayList<Node> mergeCenters = new ArrayList<Node>();

    private void copyColumnDataToOtherColumn(AttributeTable edgeTable, AttributeColumn Weight, AttributeColumn Count) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    class City { //inner class Country
		String standardName;
		ArrayList<String> matchName;
		String country;
		boolean match;
		double latitude;
		double longitude;
		City (String name, double[] coord){
			standardName = name.trim();
			latitude = coord[0];
			longitude = coord[1];
		}

		City (String standardN, String countryName, double[] coord){
			standardName = standardN;
			matchName = new ArrayList<String>();
			matchName.add(standardName);
			match = false;
			country = countryName;
			latitude = coord[0];
			longitude = coord[1];
		}
	}
	ArrayList<City> cityList = new ArrayList<City>();
        
        /**
	 * This constructor builds the geo mapping from cityListState.csv
	 */	
	public Geocoder(GraphModel graphModel, String dir) {
            oldView = graphModel.getVisibleView();
            directed = graphModel.isDirected();
            if (directed) {
                graph = graphModel.getHierarchicalDirectedGraphVisible();
            } else {
                graph = graphModel.getHierarchicalUndirectedGraphVisible();
            }
            indicies = new HashMap<Integer, Node>();
            invIndicies = new HashMap<Node, Integer>();
            int count = 0;
            for (Node u : graph.getNodes()) {
                indicies.put(count, u);
                invIndicies.put(u, count);
                count++;
            }
        
            String line="";
            String word="";
            String word2 = "";
            String word3 = "";
            double[] coordinates = new double[2];
            //Build the standard city properties from GIS lists
            FileReader cityReader = null;
            try {
                    cityReader = new FileReader(dir);
            }
            catch (FileNotFoundException ex) {
                    ex.printStackTrace();
            }

                count = 0;
		int rowCount = 0;
		BufferedReader cityBuffer =  new BufferedReader(cityReader);
		final Pattern p = Pattern.compile("(?:\"(?:[^\"\\\\]++|\\\\.)*+\"|[^\",]++)++|,"); //csv tokenizer
		try {
			line = cityBuffer.readLine(); //skip header row
			while ((line = cityBuffer.readLine()) != null) { //Read each row
				rowCount++;
			    Matcher m = p.matcher(line);
			    m.find();//full address
			    m.find();
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
			    m.find();//standard name
			    word = m.group().replace("\"", "");
			    m.find();
			    m.find();//country
			    word2 = m.group().replace("\"", "");
			    if (word2.equals("USA"))
			    	word2 = "United States"; //Alias for USA
			    if (word2.equals("Peoples R China")) 
			    	word2 = "China"; //Alias for China
			    if (word2.equals("England")||word2.equals("Scotland")||word2.equals("Wales")||word2.equals("North Ireland")) 
			    	word2 = "United Kingdom"; //Alias for United Kingdom
			    m.find();
			    m.find();m.find();//number of affiliations
			    m.find();m.find();//merged city IDs
			    m.find();//merged city List
			    word3 = m.group();
			    City newCity = new City(word, word2, coordinates);
			    String matchStrings[] = word3.replace("\"", "").split("\\|"); //all cities for each author
			    for (int i=0; i<matchStrings.length; i++)
			    	newCity.matchName.add(matchStrings[i]);
			    cityList.add(newCity);
			    count++;
			}
			System.out.println(rowCount +" cities read.");
			System.out.println(count +" cities added.");
		}
            catch (IOException ex) {
                    ex.printStackTrace();
            }
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
        
        void map () {
            AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
            AttributeTable nodeTable = attributeModel.getNodeTable();

            AttributeColumn Latitude = nodeTable.getColumn("Lat");
            if (Latitude == null) {
                    Latitude = nodeTable.addColumn("Lat", "Lat", AttributeType.DOUBLE, AttributeOrigin.DATA, new Double(0));
                    JOptionPane.showMessageDialog(null, "'Lat' attribute has been added, please edit in the Data laboratory ", 
                            "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
               }
            AttributeColumn Longitude = nodeTable.getColumn("Lng");
            if (Longitude == null) {
                    Longitude = nodeTable.addColumn("Lng", "Lng", AttributeType.DOUBLE, AttributeOrigin.DATA, new Double(0));
                    JOptionPane.showMessageDialog(null, "'Lng' attribute has been added, please edit in the Data laboratory ", 
                            "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
               }
            AttributeColumn Country = nodeTable.getColumn("Country");
            if (Country == null) {
                    Country = nodeTable.addColumn("Country", "Country", AttributeType.STRING, AttributeOrigin.DATA, new String());
                    JOptionPane.showMessageDialog(null, "'Country' attribute has been added, please edit in the Data laboratory ", 
                            "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
               }
            AttributeColumn Layer = nodeTable.getColumn("layer[Z]");
            
            AttributeTable edgeTable = attributeModel.getEdgeTable();
            AttributeColumnsController impl = Lookup.getDefault().lookup(AttributeColumnsController.class);
            AttributeColumn Weight = edgeTable.getColumn("Weight");
            AttributeColumn Count = impl.duplicateColumn(edgeTable, Weight, "Count", DOUBLE);
            JOptionPane.showMessageDialog(null, "'Count' attribute has been copied to the edges, please edit in the Data laboratory ", 
                    "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
               
            
            for (int i=0; i<indicies.size(); i++){
                Node s = indicies.get(i); //picking a source node
                String matchName = s.getNodeData().getLabel().replace(" ", "");
                AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();         
                int layer = Integer.parseInt(row.getValue(Layer).toString());
                boolean match =false;
                if (layer == 0) {
                    for (int j=0; j<cityList.size(); j++) {
                        if (cityList.get(j).standardName.replace(" ", "").equalsIgnoreCase(matchName)) {
                            row.setValue(Longitude, cityList.get(j).longitude);
                            row.setValue(Latitude, cityList.get(j).latitude);
                            row.setValue(Country, cityList.get(j).country);
                            match = true;
                        }
                    }
                    if (!match)
                        System.out.println(" no match.");

                }
            }
            EdgeIterable iter;
                if (directed) {
                        iter = ((HierarchicalDirectedGraph) graph).getEdgesAndMetaEdges();
                    } else {
                        iter = ((HierarchicalUndirectedGraph) graph).getEdgesAndMetaEdges();
                    }
                Edge[] eList = iter.toArray();
                for (Edge e : eList) {
                    Node source = e.getSource();
                    Node target = e.getTarget();
                    AttributeRow row = (AttributeRow) source.getNodeData().getAttributes();  
                    AttributeRow row2 = (AttributeRow) target.getNodeData().getAttributes();    
                    Double lat1 = Double.parseDouble(row.getValue(Latitude).toString());
                    Double lng1 = Double.parseDouble(row.getValue(Longitude).toString());
                    Double lat2 = Double.parseDouble(row2.getValue(Latitude).toString());
                    Double lng2 = Double.parseDouble(row2.getValue(Longitude).toString());
                    int layer1 = Integer.parseInt(row.getValue(Layer).toString());
                    int layer2 = Integer.parseInt(row2.getValue(Layer).toString());                    
                    
                    if (layer1==0&&layer2==0) {
                        double theta = lng1 - lng2;
                        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2))
                                + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
                        dist = Math.acos(dist);
                        dist = rad2deg(dist);
                        dist = dist * 60 * 1.1515;
                        if (dist > 20000) //check if the distance is over half quator length
                            dist = 20000;
                        if (dist < 1) //need local node mergers conditioned on resolution
                            dist = 1;
                        e.setWeight((float) dist); 
                    }
                }
        }
        
        void countryGather (GraphModel graphModel) {
            AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
            AttributeTable nodeTable = attributeModel.getNodeTable();
            AttributeColumn degree = nodeTable.getColumn("Weighted degree");
            if (degree == null) {
                    degree = nodeTable.addColumn("Weighted degree", "Weighted degree", AttributeType.DOUBLE, AttributeOrigin.DATA, new Double(0));
                    JOptionPane.showMessageDialog(null, "'Weighted degree' attribute has been added, please edit in the Data laboratory ", 
                            "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
               }
            AttributeColumn Country = nodeTable.getColumn("Country");
            if (Country == null) {
                    Country = nodeTable.addColumn("Country", "Country", AttributeType.STRING, AttributeOrigin.DATA, new String());
                    JOptionPane.showMessageDialog(null, "'Country' attribute has been added, please edit in the Data laboratory ", 
                            "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
               }
            AttributeColumn Layer = nodeTable.getColumn("layer[Z]");
            ArrayList<Double> maxDegree = new ArrayList<Double>();
            ArrayList<String> countryList = new ArrayList<String>();
            ArrayList<Double> totalDegree = new ArrayList<Double>();
            
            for (int i=0; i<indicies.size(); i++){
                Node s = indicies.get(i); //picking a source node
                AttributeRow row = (AttributeRow) s.getNodeData().getAttributes();         
                int layer = Integer.parseInt(row.getValue(Layer).toString());
                if (layer == 0) {
                    String countryName =  row.getValue(Country).toString();
                    if (countryList.contains(countryName)) {
                        int idx = countryList.indexOf(countryName);
                        double temp = (Double)row.getValue(degree);
                        totalDegree.set(idx, totalDegree.get(idx)+temp);
                        if (temp > maxDegree.get(idx)){
                            mergeCenters.set(idx,s);
                            maxDegree.set(idx, temp);
                        }    
                    }
                    else {
                        mergeCenters.add(s);
                        countryList.add(countryName);
                        maxDegree.add((Double)row.getValue(degree));
                        totalDegree.add((Double)row.getValue(degree));
                    }

                }
            }
            
        //GraphView newView = graphModel.newView();     //Duplicate main view
        
        for (Node c : mergeCenters) {
            AttributeRow row = (AttributeRow) c.getNodeData().getAttributes();
            String countryName =  row.getValue(Country).toString();
            ArrayList<Node> mergeList = new ArrayList<Node>();
            mergeList.add(c);
            for (int i=0; i<indicies.size(); i++){
                Node s = indicies.get(i); //picking a source node
                AttributeRow row2 = (AttributeRow) s.getNodeData().getAttributes();   
                String countryName2 =  row2.getValue(Country).toString();
                int layer2 = Integer.parseInt(row2.getValue(Layer).toString());
                if (layer2==0 && countryName.equals(countryName2) && !mergeCenters.contains(s)) {
                    mergeList.add(s);
                }
            }
            Node[] mergeArray = new Node[mergeList.size()];
            mergeArray = mergeList.toArray(mergeArray);
            Node group = graph.groupNodes(mergeArray); 
            AttributeColumn Latitude = nodeTable.getColumn("Lat");
            if (Latitude == null) {
                    Latitude = nodeTable.addColumn("Lat", "Lat", AttributeType.DOUBLE, AttributeOrigin.DATA, new Double(0));
                    JOptionPane.showMessageDialog(null, "'Lat' attribute has been added, please edit in the Data laboratory ", 
                            "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
               }
            AttributeColumn Longitude = nodeTable.getColumn("Lng");
            if (Longitude == null) {
                    Longitude = nodeTable.addColumn("Lng", "Lng", AttributeType.DOUBLE, AttributeOrigin.DATA, new Double(0));
                    JOptionPane.showMessageDialog(null, "'Lng' attribute has been added, please edit in the Data laboratory ", 
                            "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
               }
            
            c.getNodeData().setLabel(countryName);
            AttributeRow row3 = (AttributeRow) group.getNodeData().getAttributes(); 
            row3.setValue(Latitude, row.getValue(Latitude));
            row3.setValue(Longitude, row.getValue(Longitude));
            row3.setValue(Country, countryName);
            row3.setValue(degree, totalDegree.get(mergeCenters.indexOf(c)));
        }
    }
        
    void countryReset(GraphModel graphModel) {
        Graph merged = graphModel.getGraphVisible();
        Node[] mergedL = merged.getNodes().toArray();
        for (Node u : mergedL) 
            graph.ungroupNodes(u);
    }
        
}
