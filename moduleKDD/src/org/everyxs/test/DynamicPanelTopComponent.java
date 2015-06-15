/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.everyxs.test;

import java.io.File;
import java.io.IOException;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.partition.api.Partition;
import org.gephi.partition.api.PartitionController;
import org.gephi.partition.plugin.NodeColorTransformer;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.Lookup;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//org.everyxs.test//DynamicPanel//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "DynamicPanelTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "rankingmode", openAtStartup = true)
@ActionID(category = "Window", id = "org.everyxs.test.DynamicPanelTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_DynamicPanelAction",
        preferredID = "DynamicPanelTopComponent"
)
@Messages({
    "CTL_DynamicPanelAction=DynamicPanel",
    "CTL_DynamicPanelTopComponent=DynamicPanel",
    "HINT_DynamicPanelTopComponent=This is a DynamicPanel window"
})
public final class DynamicPanelTopComponent extends TopComponent {
    
    double scalePower = 1; //default: no power scaling
    double reweightPower = 1; //default: no reweight scaling
    int inputMatrix = 0; //default: adjacency matrix -> laplacian
    boolean localSwitch = false; //default: global algorithm
    double targetVolume = 1;
    int seed = 0;
    double qualityBound = 1;
    
    public DynamicPanelTopComponent() {
        initComponents();
        setName(Bundle.CTL_DynamicPanelTopComponent());
        setToolTipText(Bundle.HINT_DynamicPanelTopComponent());
        
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jTextField1 = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jRadioButtonLaplacian = new javax.swing.JRadioButton();
        jRadioButtonReplicator = new javax.swing.JRadioButton();
        jButtonGlobal = new javax.swing.JButton();
        jButtonLocal = new javax.swing.JButton();
        NLaplacian = new javax.swing.JRadioButton();
        UnbiasedAdj = new javax.swing.JRadioButton();
        jTextField2 = new javax.swing.JTextField();
        jTextField3 = new javax.swing.JTextField();
        jTextField4 = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jTextField5 = new javax.swing.JTextField();

        jTextField1.setText(org.openide.util.NbBundle.getMessage(DynamicPanelTopComponent.class, "DynamicPanelTopComponent.jTextField1.text")); // NOI18N
        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(DynamicPanelTopComponent.class, "DynamicPanelTopComponent.jLabel1.text")); // NOI18N

        buttonGroup1.add(jRadioButtonLaplacian);
        org.openide.awt.Mnemonics.setLocalizedText(jRadioButtonLaplacian, org.openide.util.NbBundle.getMessage(DynamicPanelTopComponent.class, "DynamicPanelTopComponent.jRadioButtonLaplacian.text")); // NOI18N
        jRadioButtonLaplacian.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButtonLaplacianActionPerformed(evt);
            }
        });

        buttonGroup1.add(jRadioButtonReplicator);
        org.openide.awt.Mnemonics.setLocalizedText(jRadioButtonReplicator, org.openide.util.NbBundle.getMessage(DynamicPanelTopComponent.class, "DynamicPanelTopComponent.jRadioButtonReplicator.text")); // NOI18N
        jRadioButtonReplicator.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButtonReplicatorActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jButtonGlobal, org.openide.util.NbBundle.getMessage(DynamicPanelTopComponent.class, "DynamicPanelTopComponent.jButtonGlobal.text")); // NOI18N
        jButtonGlobal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonGlobalActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jButtonLocal, org.openide.util.NbBundle.getMessage(DynamicPanelTopComponent.class, "DynamicPanelTopComponent.jButtonLocal.text")); // NOI18N
        jButtonLocal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLocalActionPerformed(evt);
            }
        });

        buttonGroup1.add(NLaplacian);
        org.openide.awt.Mnemonics.setLocalizedText(NLaplacian, org.openide.util.NbBundle.getMessage(DynamicPanelTopComponent.class, "DynamicPanelTopComponent.NLaplacian.text")); // NOI18N
        NLaplacian.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NLaplacianActionPerformed(evt);
            }
        });

        buttonGroup1.add(UnbiasedAdj);
        org.openide.awt.Mnemonics.setLocalizedText(UnbiasedAdj, org.openide.util.NbBundle.getMessage(DynamicPanelTopComponent.class, "DynamicPanelTopComponent.UnbiasedAdj.text")); // NOI18N
        UnbiasedAdj.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                UnbiasedAdjActionPerformed(evt);
            }
        });

        jTextField2.setText(org.openide.util.NbBundle.getMessage(DynamicPanelTopComponent.class, "DynamicPanelTopComponent.jTextField2.text")); // NOI18N
        jTextField2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField2ActionPerformed(evt);
            }
        });

        jTextField3.setText(org.openide.util.NbBundle.getMessage(DynamicPanelTopComponent.class, "DynamicPanelTopComponent.jTextField3.text")); // NOI18N
        jTextField3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField3ActionPerformed(evt);
            }
        });

        jTextField4.setText(org.openide.util.NbBundle.getMessage(DynamicPanelTopComponent.class, "DynamicPanelTopComponent.jTextField4.text")); // NOI18N
        jTextField4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField4ActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(DynamicPanelTopComponent.class, "DynamicPanelTopComponent.jLabel2.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(DynamicPanelTopComponent.class, "DynamicPanelTopComponent.jLabel3.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel4, org.openide.util.NbBundle.getMessage(DynamicPanelTopComponent.class, "DynamicPanelTopComponent.jLabel4.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel5, org.openide.util.NbBundle.getMessage(DynamicPanelTopComponent.class, "DynamicPanelTopComponent.jLabel5.text")); // NOI18N

        jTextField5.setText(org.openide.util.NbBundle.getMessage(DynamicPanelTopComponent.class, "DynamicPanelTopComponent.jTextField5.text")); // NOI18N
        jTextField5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField5ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jRadioButtonReplicator)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel1))
                            .addComponent(UnbiasedAdj))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 187, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButtonLocal, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButtonGlobal))
                        .addGap(74, 74, 74))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(jLabel4))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(NLaplacian)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel3))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jRadioButtonLaplacian)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel2))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel5)
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jTextField2, javax.swing.GroupLayout.DEFAULT_SIZE, 67, Short.MAX_VALUE)
                            .addComponent(jTextField3)
                            .addComponent(jTextField4))
                        .addGap(85, 85, 85))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(78, 78, 78)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jRadioButtonLaplacian)
                            .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel2)))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel5))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(NLaplacian)
                    .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jRadioButtonReplicator)
                    .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(UnbiasedAdj)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 59, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jButtonLocal)
                        .addGap(54, 54, 54))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jButtonGlobal)
                        .addContainerGap())))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        String text = jTextField1.getText();
        scalePower = Double.parseDouble(text);
    }//GEN-LAST:event_jTextField1ActionPerformed

    private void jRadioButtonLaplacianActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButtonLaplacianActionPerformed
        inputMatrix = 0; 
    }//GEN-LAST:event_jRadioButtonLaplacianActionPerformed

    private void jRadioButtonReplicatorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButtonReplicatorActionPerformed
        inputMatrix = 2; //change: reweight adjacency matrix by eigenvector -> replicator
    }//GEN-LAST:event_jRadioButtonReplicatorActionPerformed

    private void jButtonLocalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLocalActionPerformed
        //Get the current graph model
        GraphController gc = Lookup.getDefault().lookup(GraphController.class);
        GraphModel graphModel = gc.getModel();       
        AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
        
        //See if graph is well imported
        Graph graph = graphModel.getGraph();
        System.out.println("Nodes: " + graph.getNodeCount());
        System.out.println("Edges: " + graph.getEdgeCount());
        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        PartitionController partitionController = Lookup.getDefault().lookup(PartitionController.class);
        localSwitch = true;

        //Run local community detection
        if (localSwitch) {
            LocalPartition gPart = new LocalPartition(scalePower, inputMatrix, targetVolume, seed, qualityBound);
            gPart.execute(graphModel, attributeModel);
        }
        //Partition with 'partition', just created by global spectral algorithm
        AttributeColumn modColumn = attributeModel.getNodeTable().getColumn("partition(local)");
        Partition p2 = partitionController.buildPartition(modColumn, graph);
        System.out.println(p2.getPartsCount() + " partitions found");
        NodeColorTransformer nodeColorTransformer2 = new NodeColorTransformer();
        nodeColorTransformer2.randomizeColors(p2);
        partitionController.transform(p2, nodeColorTransformer2);

        //Export
        try {
            ec.exportFile(new File("partition2.pdf"));
        } catch (IOException ex) {
            return;
        }
    }//GEN-LAST:event_jButtonLocalActionPerformed

    private void jButtonGlobalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGlobalActionPerformed
        //Get the current graph model
        GraphController gc = Lookup.getDefault().lookup(GraphController.class);
        GraphModel graphModel = gc.getModel();       
        AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
        
        //See if graph is well imported
        Graph graph = graphModel.getGraph();
        System.out.println("Nodes: " + graph.getNodeCount());
        System.out.println("Edges: " + graph.getEdgeCount());
        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        PartitionController partitionController = Lookup.getDefault().lookup(PartitionController.class);
        localSwitch = false;
        
        //Run global community detection
        if (!localSwitch) {
            GlobalPartition gPart = new GlobalPartition(scalePower, reweightPower, inputMatrix);
            gPart.execute(graphModel, attributeModel);
        }
        //Partition with 'partition', just created by global spectral algorithm
        AttributeColumn modColumn = attributeModel.getNodeTable().getColumn("partition");
        Partition p2 = partitionController.buildPartition(modColumn, graph);
        System.out.println(p2.getPartsCount() + " partitions found");
        NodeColorTransformer nodeColorTransformer2 = new NodeColorTransformer();
        nodeColorTransformer2.randomizeColors(p2);
        partitionController.transform(p2, nodeColorTransformer2);

        //Export
        try {
            ec.exportFile(new File("partition1.pdf"));
        } catch (IOException ex) {
            return;
        }
    }//GEN-LAST:event_jButtonGlobalActionPerformed

    private void NLaplacianActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NLaplacianActionPerformed
        inputMatrix = 1; 
    }//GEN-LAST:event_NLaplacianActionPerformed

    private void UnbiasedAdjActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_UnbiasedAdjActionPerformed
        inputMatrix = 4; 
    }//GEN-LAST:event_UnbiasedAdjActionPerformed

    private void jTextField2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField2ActionPerformed
        String text = jTextField2.getText();
        targetVolume = Double.parseDouble(text);
    }//GEN-LAST:event_jTextField2ActionPerformed

    private void jTextField3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField3ActionPerformed
        String text = jTextField3.getText();
        seed = Integer.parseInt(text);
    }//GEN-LAST:event_jTextField3ActionPerformed

    private void jTextField4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField4ActionPerformed
        String text = jTextField4.getText();
        qualityBound = Double.parseDouble(text);
    }//GEN-LAST:event_jTextField4ActionPerformed

    private void jTextField5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField5ActionPerformed
        String text = jTextField5.getText();
        reweightPower = Double.parseDouble(text);
    }//GEN-LAST:event_jTextField5ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JRadioButton NLaplacian;
    private javax.swing.JRadioButton UnbiasedAdj;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton jButtonGlobal;
    private javax.swing.JButton jButtonLocal;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JRadioButton jRadioButtonLaplacian;
    private javax.swing.JRadioButton jRadioButtonReplicator;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    // End of variables declaration//GEN-END:variables
    @Override
    public void componentOpened() {
        // TODO add custom code on component opening
    }

    @Override
    public void componentClosed() {
        // TODO add custom code on component closing
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }
}