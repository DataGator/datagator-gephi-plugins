/*
 * Copyright 2015 University of Denver
 * Author(s) : LIU Yu <liuyu@opencps.net>
 * Website : http://github.com/DataGator/gephi-plugins
 *
 * This file is part of DataGator Gephi Plugins.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 University of Denver. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 3 only ("GPL") or the Common Development and
 * Distribution License("CDDL") (collectively, the "License"). You may not use
 * this file except in compliance with the License. You can obtain a copy of
 * the License at /cddl-1.0.txt and /gpl-3.0.txt. See the License for the
 * specific language governing permissions and limitations under the License.
 * When distributing the software, include this License Header Notice in each
 * file and include the License files at /cddl-1.0.txt and /gpl-3.0.txt.
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 *
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 3, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 3] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 3 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 3 code and therefore, elected the GPL
 * Version 3 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 */
package org.datagator.ext.gephi.importer;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import org.openide.util.NbBundle;

/**
 * Main UI widget implementation.
 * This is the internal logic of the main UI panel.
 *
 * @author LIU Yu <liuyu@opencps.net>
 */
class MatrixJsonImporterUIPanel
    extends javax.swing.JPanel
{

    // column role options

    public static final String SOURCE_NODE = NbBundle.getMessage(
        MatrixJsonImporterUI.class,
        "MatrixJsonImporterUI.role.node_src");
    public static final String TARGET_NODE = NbBundle.getMessage(
        MatrixJsonImporterUI.class,
        "MatrixJsonImporterUI.role.node_tgt");
    public static final String NODE = NbBundle.getMessage(
        MatrixJsonImporterUI.class, "MatrixJsonImporterUI.role.node");
    // public static final String EDGE_LABEL = NbBundle.getMessage(
    //     MatrixJsonImporterUI.class, "MatrixJsonImporterUI.role.edge_lbl");
    public static final String EDGE_WEIGHT = NbBundle.getMessage(
        MatrixJsonImporterUI.class, "MatrixJsonImporterUI.role.edge_wt");
    public static final String TIME = NbBundle.getMessage(
        MatrixJsonImporterUI.class, "MatrixJsonImporterUI.role.time");

    private static class RoleEditorWidget
        extends JComboBox
    {

        public RoleEditorWidget(boolean isDirectedGraph, boolean isDynamicGraph)
        {
            super();
            DefaultComboBoxModel model = (DefaultComboBoxModel) getModel();
            model.removeAllElements();
            if (isDirectedGraph) {
                model.addElement(SOURCE_NODE);
                model.addElement(TARGET_NODE);
            } else {
                model.addElement(NODE);
            }
            if (isDynamicGraph) {
                model.addElement(TIME);
            }
            model.addElement(EDGE_WEIGHT);
            // model.addElement(EDGE_LABEL);
            model.addElement(null);
        }
    };

    private static class RoleEditor
        extends DefaultCellEditor
    {

        private final boolean isDirected;
        private final boolean isDynamic;

        public RoleEditor(boolean isDirected, boolean isDynamic)
        {
            super(new RoleEditorWidget(isDirected, isDynamic));
            this.isDirected = isDirected;
            this.isDynamic = isDynamic;
        }

        @Override
        public Component getTableCellEditorComponent(final JTable table,
            final Object value, boolean isSelected, final int row,
            final int column)
        {
            assert (column == 1);
            
            final RoleEditorWidget widget
                = (RoleEditorWidget) super.getTableCellEditorComponent(
                    table, value, isSelected, row, column);
            final RoleEditor editor = this;

            for (ActionListener item : widget.getActionListeners()) {
                widget.removeActionListener(item);
            }

            // the role editor is shared by the entire table column, we need
            // this listener to keep track of the table cell being edited.
            widget.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    String oldRole = (String) value;
                    String newRole = (String) widget.getSelectedItem();
                    if (newRole != null) {
                        if (!newRole.equals(oldRole)) {
                            onRoleChange(table, row, newRole);
                        }
                    }
                    editor.stopCellEditing();
                }
            });
            return widget;
        }

        private void onRoleChange(JTable table, int row, String newRole)
        {
            assert (newRole != null);
            // for undirecte graph, the NODE role can appear twice (recur once),
            // other roles are exclusive, they cannot recur;
            final int maxRecurrence;
            if (!isDirected && newRole.equals(NODE)) {
                maxRecurrence = 1;
            } else {
                maxRecurrence = 0;
            }
            // clear recurrences of roles beyond the upper limit
            TableModel model = table.getModel();
            assert (model.getColumnCount() == 2);
            int observed = 0;
            for (int r = 0, c = 1; r < model.getRowCount(); r++) {
                if (r == row) {
                    continue;
                }
                if (newRole.equals(model.getValueAt(r, c))) {
                    observed += 1;
                    if (observed > maxRecurrence) {
                        model.setValueAt(null, r, c);
                    }
                }
            }
        }

    }

    /**
     * Creates new form MatrixJsonImporterWizardPanel
     */
    public MatrixJsonImporterUIPanel()
    {
        initComponents();
        updateCellEditor();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        jComboBox1 = new javax.swing.JComboBox();
        jComboBox2 = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();

        setPreferredSize(new java.awt.Dimension(500, 250));

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Directed", "Undirected" }));
        jComboBox1.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                onEdgeTypeChange(evt);
            }
        });

        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Dynamic", "Static" }));
        jComboBox2.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                onGraphTypeChange(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(MatrixJsonImporterUIPanel.class, "MatrixJsonImporterUI.text.graph_type")); // NOI18N

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][]
            {

            },
            new String []
            {
                "Matrix Column", "Role"
            }
        )
        {
            Class[] types = new Class []
            {
                java.lang.String.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean []
            {
                false, true
            };

            public Class getColumnClass(int columnIndex)
            {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex)
            {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(jTable1);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(jLabel1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jComboBox1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 124, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jComboBox2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 124, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(0, 0, Short.MAX_VALUE))
                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 488, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jComboBox1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jComboBox2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel1))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 205, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void updateCellEditor()
    {
        // terminate any on-going cell editing
        jTable1.removeEditor();
        // clear previously-specified roles
        TableModel model = jTable1.getModel();
        assert (model.getColumnCount() == 2);
        for (int r = 0, c = 1; r < model.getRowCount(); r++) {
            model.setValueAt(null, r, c);
        }
        // update cell editor
        TableColumn column = jTable1.getColumnModel().getColumn(1);
        column.setCellEditor(new RoleEditor(isDirectedGraph(), isDynamicGraph()));
    }

    public TableModel getTableModel()
    {
        return jTable1.getModel();
    }

    public void updateTableModel(int rowsCount, int columnsCount,
        Iterator<Object[]> rows)
    {
        if ((rowsCount <= 0) || (columnsCount <= 0)) {
            return;
        }

        Object[][] data = new Object[columnsCount][2];
        if (rows.hasNext()) {
            // non-empty matrix header
            Object[] matrixHeader = rows.next();
            if (matrixHeader.length != columnsCount) {
                return;
            }
            for (int c = 0; c < columnsCount; c++) {
                data[c][0] = matrixHeader[c];
                data[c][1] = null;
            }
        } else {
            // empty matrix header
            for (int c = 0; c < columnsCount; c++) {
                data[c][0] = Integer.toString(c + 1);
                data[c][1] = null;
            }
        }

        jTable1.setModel(new DefaultTableModel(
            data,
            new String[]{
                NbBundle.getMessage(MatrixJsonImporterUI.class,
                    "MatrixJsonImporterUI.table.attr"),
                NbBundle.getMessage(MatrixJsonImporterUI.class,
                    "MatrixJsonImporterUI.table.role")
            }
        )
        {
            Class[] types = new Class[]{
                java.lang.String.class, java.lang.Object.class
            };

            boolean[] canEdit = new boolean[]{
                false, true
            };

            public Class getColumnClass(int columnIndex)
            {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex)
            {
                return canEdit[columnIndex];
            }
        });
        updateCellEditor();
    }

    private void onGraphTypeChange(java.awt.event.ActionEvent evt)//GEN-FIRST:event_onGraphTypeChange
    {//GEN-HEADEREND:event_onGraphTypeChange
        updateCellEditor();
    }//GEN-LAST:event_onGraphTypeChange

    private void onEdgeTypeChange(java.awt.event.ActionEvent evt)//GEN-FIRST:event_onEdgeTypeChange
    {//GEN-HEADEREND:event_onEdgeTypeChange
        updateCellEditor();
    }//GEN-LAST:event_onEdgeTypeChange

    public boolean isDirectedGraph()
    {
        return this.jComboBox1.getSelectedIndex() == 0;
    }

    public boolean isDynamicGraph()
    {
        return this.jComboBox2.getSelectedIndex() == 0;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JComboBox jComboBox2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
