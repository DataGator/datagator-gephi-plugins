/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.datagator.ext.gephi.importer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import org.datagator.api.client.Matrix;
import org.datagator.api.client.SimpleMatrix;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeTable;
import org.gephi.data.attributes.api.AttributeType;
import org.gephi.dynamic.api.DynamicModel.TimeFormat;
import org.gephi.io.importer.api.ContainerLoader;
import org.gephi.io.importer.api.EdgeDefault;
import org.gephi.io.importer.api.EdgeDraft;
import org.gephi.io.importer.api.Issue;
import org.gephi.io.importer.api.NodeDraft;
import org.gephi.io.importer.api.Report;
import org.gephi.io.importer.spi.FileImporter;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.ProgressTicket;
import org.openide.util.NbBundle;

/**
 * @author LIU Yu <liuyu@opencps.net>
 * @date 2015/09/02
 */
public class MatrixJsonImporter
    implements FileImporter, LongTask
{

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat(
        "yyyy-MM-dd");

    private Reader reader;
    private ContainerLoader container;
    private Report report;
    private ProgressTicket progressTicket;
    private boolean cancel = false;

    private Matrix matrixHeaders = null;
    private int rowsCount = 0;

    private boolean isDirected = true;
    private boolean isDynamic = true;
    private boolean isEdgeWeighted = true;

    private AttributeColumn acWeight = null;

    public static enum ColumnRoleType
    {

        SOURCE_NODE,
        TARGET_NODE,
        UNDIRECTED_NODE,
        EDGE_WEIGHT,
        EDGE_LABEL,
        TIME,
    };

    private final ArrayList<Object[]> roleIndex = new ArrayList<Object[]>();

    public Matrix getMatrixHeaders()
    {
        if (this.matrixHeaders == null) {
            try {
                Matrix matrix = SimpleMatrix.create(this.reader);
                rowsCount = matrix.getRowsCount();
                this.matrixHeaders = matrix.columnHeaders();
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            } catch (RuntimeException re) {
                throw new RuntimeException(NbBundle.getMessage(
                    MatrixJsonImporter.class,
                    "MatrixJsonImporter.msg.bad_matrix"));
            }
        }
        return this.matrixHeaders;
    }

    public void setGraphType(boolean isDirected, boolean isDynamic)
    {
        this.isDirected = isDirected;
        this.isDynamic = isDynamic;
    }

    public void setColumnRole(int columnIndex, ColumnRoleType columnRole)
    {
        this.roleIndex.add(new Object[]{columnRole, columnIndex});
        if (columnRole.equals(ColumnRoleType.EDGE_WEIGHT)) {
            this.isEdgeWeighted = true;
        }
    }

    @Override
    public void setReader(Reader reader)
    {
        this.reader = reader;
    }

    @Override
    public boolean execute(ContainerLoader loader)
    {
        this.container = loader;
        this.report = new Report();

        try {
            progressTicket.start();
            if (isDirected) {
                container.setEdgeDefault(EdgeDefault.DIRECTED);
            } else {
                container.setEdgeDefault(EdgeDefault.UNDIRECTED);
            }
            container.setTimeFormat(TimeFormat.DATE);
            AttributeTable edgeTable
                = container.getAttributeModel().getEdgeTable();
            if (isDynamic) {
                if (isEdgeWeighted) {
                    if (edgeTable.hasColumn("weight")) {
                        acWeight = edgeTable.getColumn("weight");
                        if (acWeight.getType() != AttributeType.DYNAMIC_FLOAT) {
                            edgeTable.removeColumn(acWeight);
                            acWeight = null;
                        }
                    }
                    if (acWeight == null) {
                        acWeight = edgeTable.addColumn(
                            "weight", AttributeType.DYNAMIC_FLOAT);
                    }
                }
            } else {
                if (isEdgeWeighted) {
                    if (edgeTable.hasColumn("weight")) {
                        acWeight = edgeTable.getColumn("weight");
                        if (acWeight.getType() != AttributeType.FLOAT) {
                            edgeTable.removeColumn(acWeight);
                            acWeight = null;
                        }
                    }
                    if (acWeight == null) {
                        acWeight = edgeTable.addColumn(
                            "weight", AttributeType.FLOAT);
                    }
                }
            }
            progressTicket.switchToDeterminate(rowsCount);
            parseMatrix(this.reader);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            progressTicket.finish();
        }

        return !cancel;
    }

    @Override
    public ContainerLoader getContainer()
    {
        return container;
    }

    @Override
    public Report getReport()
    {
        return report;
    }

    @Override
    public boolean cancel()
    {
        cancel = true;
        return true;
    }

    @Override
    public void setProgressTicket(ProgressTicket progressTicket)
    {
        this.progressTicket = progressTicket;
    }

    private static TypeReference<Object[]> tr = new TypeReference<Object[]>()
    {
    };

    private void parseRows(JsonParser jp, int bodyRow, int bodyColumn)
        throws IOException
    {
        Set<String> edgeWeghtSet = new HashSet<String>();

        JsonToken token = jp.getCurrentToken(); // START_ARRAY
        if (!token.equals(JsonToken.START_ARRAY)) {
            report.logIssue(new Issue(
                String.format("Unexpected token %s", token),
                Issue.Level.CRITICAL));
            return;
        }

        int rowIndex = 0;
        token = jp.nextToken(); // START_ARRAY

        // skip matrix headers
        while (rowIndex < bodyRow) {
            while (!token.equals(JsonToken.END_ARRAY)) {
                token = jp.nextToken();
            }
            rowIndex += 1;
            token = jp.nextToken(); // START_ARRAY
        }

        if (rowIndex > 0) {
            report.logIssue(new Issue(String.format("Skipped %s header row(s).",
                Integer.toString(rowIndex)), Issue.Level.INFO));
        }

        while (token.equals(JsonToken.START_ARRAY)) {

            rowIndex += 1;
            progressTicket.progress(rowIndex);

            String[] nodePair = new String[]{null, null};
            int nodeIndex = 0;
            boolean reversed = false;

            // String label = null;
            String time = null;
            Object weight = null;

            Object[] vector = jp.readValueAs(tr);
            for (Object[] ri : roleIndex) {
                ColumnRoleType role = (ColumnRoleType) ri[0];
                int index = (Integer) ri[1];
                switch (role) {
                    case SOURCE_NODE:
                        if (nodeIndex > 0) {
                            reversed = true;
                        }
                    case TARGET_NODE:
                        if (nodeIndex < 1) {
                            reversed = true;
                        }
                    case UNDIRECTED_NODE:
                        if (nodeIndex > 1) {
                            report.logIssue(new Issue(
                                String.format(
                                    "Duplicated node role on line %s",
                                    Integer.toString(rowIndex)),
                                Issue.Level.CRITICAL));
                            continue;
                        }
                        String nodeLabel = String.valueOf(vector[index]).trim();
                        nodePair[nodeIndex++] = nodeLabel;
                        break;
                    // case EDGE_LABEL:
                    //    label = String.valueOf(vector[index]);
                    //    break;
                    case EDGE_WEIGHT:
                        weight = vector[index];
                        break;
                    case TIME:
                        time = String.valueOf(vector[index]);
                        break;
                }
            }

            if (nodeIndex != 2) {
                report.logIssue(new Issue(
                    "Invalid role assignment, need two NODE columns",
                    Issue.Level.CRITICAL));
                return;
            }

            String sourceLabel = nodePair[reversed ? 1 : 0];
            String targetLabel = nodePair[reversed ? 0 : 1];
            String sourceId = sourceLabel;
            String targetId = targetLabel;

            String edgeId = null;
            if (!isDirected && (sourceLabel.compareTo(targetLabel) > 0)) {
                edgeId = targetLabel + "/" + sourceLabel;
            } else {
                edgeId = sourceLabel + "/" + targetLabel;
            }

            Date start = null;
            Date end = null;
            if (time != null) {
                Date[] interval = parseTimeInterval(time);
                start = interval[0];
                end = interval[1];
                if ((start == null) || (end == null)) {
                    report.logIssue(new Issue(
                        String.format("Invalid time on line %s",
                            Integer.toString(rowIndex)),
                        Issue.Level.SEVERE));
                    token = jp.nextToken(); // START_ARRAY
                    continue;
                }
            }

            float edgeWeight = 0.0f;

            if (weight instanceof Double) {
                edgeWeight = ((Double) weight).floatValue();
            } else if (weight instanceof Integer) {
                edgeWeight = ((Integer) weight).floatValue();
            } else if (weight != null) {
                report.logIssue(new Issue(
                    String.format("Edge weight is non-numerical on line %s",
                        Integer.toString(rowIndex)),
                    Issue.Level.CRITICAL));
                token = jp.nextToken(); // START_ARRAY
                continue;
            }

            final NodeDraft source;
            final NodeDraft target;
            final EdgeDraft edge;

            if (container.nodeExists(sourceId)) {
                source = container.getNode(sourceId);
            } else {
                source = container.factory().newNodeDraft();
                source.setId(sourceId);
                source.setLabel(sourceLabel);
                container.addNode(source);
            }

            if (container.nodeExists(targetId)) {
                target = container.getNode(targetId);
            } else {
                target = container.factory().newNodeDraft();
                target.setId(targetId);
                target.setLabel(targetLabel);
                container.addNode(target);
            }

            if (container.edgeExists(edgeId)) {
                edge = container.getEdge(edgeId);
            } else {
                edge = container.factory().newEdgeDraft();
                edge.setSource(source);
                edge.setTarget(target);
                edge.setId(edgeId);
                container.addEdge(edge);
            }
            if (time != null) {
                edge.addTimeInterval(dateFormat.format(start),
                    dateFormat.format(end));
            }
            if (weight != null) {
                String edgeKey = edgeId;
                if (time != null) {
                    edgeKey += "/" + time;
                    if (!edgeWeghtSet.contains(edgeKey)) {
                        edgeWeghtSet.add(edgeKey);
                        edge.addAttributeValue(acWeight, edgeWeight,
                            dateFormat.format(start), dateFormat.format(end));
                    }
                } else {
                    if (!edgeWeghtSet.contains(edgeKey)) {
                        edgeWeghtSet.add(edgeKey);
                        edge.setWeight(edgeWeight);
                    }
                }
            }

            token = jp.nextToken(); // START_ARRAY
        }
    }

    private Date[] parseTimeInterval(String interval)
    {
        Date start = null;
        Date end = null;
        if (interval.contains(",")) {
            String[] tuple = interval.split(",");
            start = parseDate(tuple[0], true);
            end = parseDate(tuple[1], true);
        } else {
            start = parseDate(interval, true);
            end = parseDate(interval, false);
        }
        return new Date[]{start, end};
    }

    private Date parseDate(String date, boolean defaultFirst)
    {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UCT"));
        if (date.matches("^\\s*\\d{4}(?:-//d{1,2}(?:-//d{1,2})?)?\\s*$")) {
            String[] tuple = date.trim().split("-");
            int yyyy = Integer.parseInt(tuple[0]);
            int mm = (tuple.length > 1) ? Integer.parseInt(tuple[1])
                : ((defaultFirst) ? 0 : 11);
            int dd = (tuple.length > 2) ? Integer.parseInt(tuple[2])
                : ((defaultFirst) ? 1 : 31);
            cal.set(yyyy, mm, dd);
            return cal.getTime();
        }
        return null;
    }

    private void parseMatrix(Reader reader)
        throws Exception
    {
        JsonFactory json = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper();
        json.setCodec(mapper);
        JsonParser jp = json.createParser(reader);

        String kind = null;
        int rowsCount = -1;
        int columnsCount = -1;
        int bodyRow = -1;
        int bodyColumn = -1;

        JsonToken token = jp.nextToken(); // START_OBJECT
        if (!token.equals(JsonToken.START_OBJECT)) {
            report.logIssue(new Issue(
                String.format("Unexpected token %s", token),
                Issue.Level.CRITICAL));
            return;
        }

        token = jp.nextToken(); // FIELD_NAME
        if (!token.equals(JsonToken.FIELD_NAME)) {
            report.logIssue(new Issue(
                String.format("Unexpected token %s", token),
                Issue.Level.CRITICAL));
            return;
        }
        while (token.equals(JsonToken.FIELD_NAME)) {
            String name = jp.getText();
            token = jp.nextToken();
            if (name.equals("kind")) {
                if (!token.equals(JsonToken.VALUE_STRING)) {
                    report.logIssue(new Issue(
                        String.format("Unexpected token %s", token),
                        Issue.Level.CRITICAL));
                    return;
                }
                kind = jp.getText();
                if (!kind.equals("datagator#Matrix")) {
                    report.logIssue(new Issue(
                        String.format("Unexpected Entity kind %s", kind),
                        Issue.Level.CRITICAL));
                    return;
                }
            } else if (name.equals("columnHeaders")) {
                if (!token.equals(JsonToken.VALUE_NUMBER_INT)) {
                    report.logIssue(new Issue(
                        String.format("Unexpected token %s", token),
                        Issue.Level.CRITICAL));
                    return;
                }
                bodyRow = jp.getIntValue();
            } else if (name.equals("rowHeaders")) {
                if (!token.equals(JsonToken.VALUE_NUMBER_INT)) {
                    report.logIssue(new Issue(
                        String.format("Unexpected token %s", token),
                        Issue.Level.CRITICAL));
                    return;
                }
                bodyColumn = jp.getIntValue();
            } else if (name.equals("rows")) {
                if (bodyRow < 0 || bodyColumn < 0) {
                    report.logIssue(new Issue(
                        "Unexpected property order 'columnHeaders' and 'rowHeaders' should precede 'rows'.",
                        Issue.Level.CRITICAL));
                    return;
                }
                parseRows(jp, bodyRow, bodyColumn);
            } else if (name.equals("rowsCount")) {
                if (!token.equals(JsonToken.VALUE_NUMBER_INT)) {
                    report.logIssue(new Issue(
                        String.format("Unexpected token %s", token),
                        Issue.Level.CRITICAL));
                    return;
                }
                rowsCount = jp.getIntValue();
            } else if (name.equals("columnsCount")) {
                if (!token.equals(JsonToken.VALUE_NUMBER_INT)) {
                    report.logIssue(new Issue(
                        String.format("Unexpected token %s", token),
                        Issue.Level.CRITICAL));
                    return;
                }
                columnsCount = jp.getIntValue();
            } else {
                report.logIssue(new Issue(
                    String.format("Unexpected property %s", name),
                    Issue.Level.CRITICAL));
                return;
            }
            token = jp.nextToken(); // FIELD_NAME
        }

        if (!(0 <= bodyRow && bodyRow <= rowsCount)) {
            report.logIssue(new Issue(
                "Invalid Matrix shape",
                Issue.Level.CRITICAL));
        }

        if (!(0 <= bodyColumn && bodyColumn <= columnsCount)) {
            report.logIssue(new Issue(
                "Invalid Matrix shape",
                Issue.Level.CRITICAL));
        }
    }
}
