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
import org.gephi.datalab.api.GraphElementsController;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.GraphView;
import org.gephi.graph.api.HierarchicalGraph;
import org.gephi.graph.api.Node;
import org.openide.util.Lookup;

/**
 *
 * @author everyan
 */
public class Geocoder {
    
    GraphView oldView;
    HierarchicalGraph graph;
    HashMap<Integer, Node> indicies = new HashMap<Integer, Node>();
    HashMap<Node, Integer> invIndicies = new HashMap<Node, Integer>();
    ArrayList<Node> mergeCenters = new ArrayList<Node>();
    
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
            if (graphModel.isDirected()) {
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
            BufferedReader cityBuffer =  new BufferedReader(cityReader);
            final Pattern p = Pattern.compile("(?:\"(?:[^\"\\\\]++|\\\\.)*+\"|[^\",]++)++|,"); //csv tokenizer
            try {
                    line = cityBuffer.readLine(); //skip header row
                    while ((line = cityBuffer.readLine()) != null) { //Read each row
                        Matcher m = p.matcher(line);
                        m.find();m.find();//Paper ID
                        m.find();m.find();//Pub year
                        m.find();m.find();//IU address
                        m.find();m.find();//city IU
                        m.find();m.find();//full address
                        m.find();m.find();//WoS city
                        m.find();//WoS state
                        word2 = m.group();
                        if (! word2.equals(",")) {
                            m.find();
                        }
                        else;
                        m.find();
                        word2 = m.group(); //WoS Country
                        m.find();
                        m.find();m.find();//skip address
                        m.find();//Latitude
                        try {
                                coordinates[0] = Double.parseDouble(m.group());
                        } catch (NumberFormatException e) {
                            System.out.println(count + " row error");
                        }
                        coordinates[0] = Double.parseDouble(m.group());
                        m.find();
                        m.find();//Longitude
                        coordinates[1] = Double.parseDouble(m.group());
                        m.find();m.find();
                        word = m.group();//Standard name
                        boolean exist = false;
                        for (int i=0; i<cityList.size(); i++) {
                            if (cityList.get(i).standardName.equals(m.group())) {
                                    exist = true;
                                    cityList.get(i).matchName.add(word);
                            }
                        }
                        if (!exist) {
                                City newCity = new City(word, word2, coordinates);
				newCity.matchName.add(word);
                                cityList.add(newCity);
                                count++;
                        }
                    }
            }
            catch (IOException ex) {
                    ex.printStackTrace();
            }
        }
        
        void map () {
            AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
            AttributeTable nodeTable = attributeModel.getNodeTable();
            AttributeColumn Longitude = nodeTable.getColumn("Lng");
            if (Longitude == null) {
                    Longitude = nodeTable.addColumn("Lng", "Lng", AttributeType.DOUBLE, AttributeOrigin.DATA, new Double(0));
                    JOptionPane.showMessageDialog(null, "'Lng' attribute has been added, please edit in the Data laboratory ", 
                            "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
               }
            AttributeColumn Latitude = nodeTable.getColumn("Lat");
            if (Latitude == null) {
                    Latitude = nodeTable.addColumn("Lat", "Lat", AttributeType.DOUBLE, AttributeOrigin.DATA, new Double(0));
                    JOptionPane.showMessageDialog(null, "'Lat' attribute has been added, please edit in the Data laboratory ", 
                            "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
               }
            AttributeColumn Country = nodeTable.getColumn("Country");
            if (Country == null) {
                    Country = nodeTable.addColumn("Country", "Country", AttributeType.STRING, AttributeOrigin.DATA, new String());
                    JOptionPane.showMessageDialog(null, "'Country' attribute has been added, please edit in the Data laboratory ", 
                            "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
               }
            AttributeColumn Layer = nodeTable.getColumn("layer[Z]");
            
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
            AttributeColumn Longitude = nodeTable.getColumn("Lng");
            if (Longitude == null) {
                    Longitude = nodeTable.addColumn("Lng", "Lng", AttributeType.DOUBLE, AttributeOrigin.DATA, new Double(0));
                    JOptionPane.showMessageDialog(null, "'Lng' attribute has been added, please edit in the Data laboratory ", 
                            "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
               }
            AttributeColumn Latitude = nodeTable.getColumn("Lat");
            if (Latitude == null) {
                    Latitude = nodeTable.addColumn("Lat", "Lat", AttributeType.DOUBLE, AttributeOrigin.DATA, new Double(0));
                    JOptionPane.showMessageDialog(null, "'Lat' attribute has been added, please edit in the Data laboratory ", 
                            "InfoBox: " + "Error", JOptionPane.INFORMATION_MESSAGE);
               }
            
            c.getNodeData().setLabel(countryName);
            AttributeRow row3 = (AttributeRow) group.getNodeData().getAttributes(); 
            row3.setValue(Country, countryName);
            row3.setValue(Longitude, row.getValue(Longitude));
            row3.setValue(Latitude, row.getValue(Latitude));
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
