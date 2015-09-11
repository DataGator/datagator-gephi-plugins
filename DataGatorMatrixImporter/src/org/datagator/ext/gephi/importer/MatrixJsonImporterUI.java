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

import java.awt.Cursor;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.table.TableModel;
import org.datagator.api.client.Matrix;
import org.gephi.desktop.mrufiles.api.MostRecentFiles;
import org.gephi.io.importer.api.Issue;
import org.gephi.io.importer.spi.Importer;
import org.gephi.io.importer.spi.ImporterUI;
import org.gephi.utils.longtask.api.LongTaskExecutor;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 * Main UI logic implementation.
 * This is the representation layer of the importer responsible for mapping UI
 * input / output to business logic settings / outcomes.
 * 
 * @author LIU Yu <liuyu@opencps.net>
 */
@ServiceProvider(service = ImporterUI.class)
public class MatrixJsonImporterUI
    implements ImporterUI
{

    private static Map<String, MatrixJsonImporter.ColumnRoleType> roleDict;

    static {
        roleDict = new HashMap<String, MatrixJsonImporter.ColumnRoleType>();
        roleDict.put(MatrixJsonImporterUIPanel.SOURCE_NODE,
            MatrixJsonImporter.ColumnRoleType.SOURCE_NODE);
        roleDict.put(MatrixJsonImporterUIPanel.TARGET_NODE,
            MatrixJsonImporter.ColumnRoleType.TARGET_NODE);
        roleDict.put(MatrixJsonImporterUIPanel.NODE,
            MatrixJsonImporter.ColumnRoleType.UNDIRECTED_NODE);
        roleDict.put(MatrixJsonImporterUIPanel.EDGE_WEIGHT,
            MatrixJsonImporter.ColumnRoleType.EDGE_WEIGHT);
        // roleDict.put(MatrixJsonImporterUIPanel.EDGE_LABEL,
        //     MatrixJsonImporter.ColumnRoleType.EDGE_LABEL);
        roleDict.put(MatrixJsonImporterUIPanel.TIME,
            MatrixJsonImporter.ColumnRoleType.TIME);
    }

    private MatrixJsonImporter importer;
    private MatrixJsonImporterUIPanel panel;
    private LongTaskExecutor executor;

    private void previewMatrixAsync()
    {
        // during `ImporterUI.setup()`, `importer.reader` is still `null`, the
        // only viable means to preview the file is through the MRF list.
        MostRecentFiles mrf = Lookup.getDefault().lookup(MostRecentFiles.class);
        String filePath = mrf.getMRUFileList().get(0);
        try {
            importer.setReader(new FileReader(filePath));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        // pre-fetch matrix columns for role annotation, for large files (i.e.
        // > 1 mil records), this may take more than 10 seconds, so we need
        // to run in an asynchronous task to avoid jamming the UI.
        Runnable asyncTask = new Runnable()
        {
            @Override
            public void run()
            {
                Cursor oldCursor = panel.getCursor();
                panel.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                Matrix preview = importer.getMatrixHeaders();
                panel.updateTableModel(preview.getRowsCount(),
                    preview.getColumnsCount(), preview.rows());
                panel.setCursor(oldCursor);
            }
        };
        executor.execute(null, asyncTask);
    }

    @Override
    public void setup(final Importer importer)
    {
        this.executor = new LongTaskExecutor(true,
            "MatrixJsonImporterUI", 10);
        this.importer = (MatrixJsonImporter) importer;
        previewMatrixAsync();
    }

    @Override
    public JPanel getPanel()
    {
        panel = new MatrixJsonImporterUIPanel();
        return (JPanel) panel;
    }

    @Override
    public void unsetup(boolean update)
    {
        if (update) {            
            // edge type: directed / undirected
            importer.setGraphType(panel.isDirectedGraph(),
                panel.isDynamicGraph());
            //
            TableModel model = panel.getTableModel();
            for (int r = 0, c = 1; r < model.getRowCount(); r++) {
                Object roleName = model.getValueAt(r, c);
                if (roleName == null) {
                    continue;
                }
                MatrixJsonImporter.ColumnRoleType roleType
                    = this.roleDict.get(roleName);
                if (roleType == null) {
                    importer.getReport().logIssue(new Issue(String.format(
                        NbBundle.getMessage(
                            MatrixJsonImporter.class,
                            "MatrixJsonImporter.msg_tmpl.bad_role"), roleName),
                        Issue.Level.SEVERE));
                }
                importer.setColumnRole(r, roleType);
            }
        }
        executor.cancel();
        executor = null;
        importer = null;
        panel = null;
    }

    @Override
    public String getDisplayName()
    {
        return NbBundle.getMessage(MatrixJsonImporterUI.class,
            "MatrixJsonImporterUI.text.title");
    }

    @Override
    public boolean isUIForImporter(Importer importer)
    {
        return importer instanceof MatrixJsonImporter;
    }
}
