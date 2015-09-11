/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
 *
 * @author liuyu
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
