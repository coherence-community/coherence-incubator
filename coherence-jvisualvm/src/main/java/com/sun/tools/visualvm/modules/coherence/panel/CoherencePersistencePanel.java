/*
 * File: CoherencePersistencePanel.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of 
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting https://oss.oracle.com/licenses/CDDL
 *
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file LICENSE.txt.
 *
 * MODIFICATIONS:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 */

package com.sun.tools.visualvm.modules.coherence.panel;

import com.sun.tools.visualvm.charts.SimpleXYChartSupport;

import com.sun.tools.visualvm.modules.coherence.VisualVMModel;
import com.sun.tools.visualvm.modules.coherence.helper.GraphHelper;
import com.sun.tools.visualvm.modules.coherence.helper.RenderHelper;
import com.sun.tools.visualvm.modules.coherence.panel.util.ExportableJTable;
import com.sun.tools.visualvm.modules.coherence.tablemodel.AbstractCoherenceTableModel;
import com.sun.tools.visualvm.modules.coherence.tablemodel.PersistenceTableModel;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Data;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.PersistenceData;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import java.util.List;
import java.util.Map;

import java.util.Map.Entry;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

/**
 * An implementation of an {@link AbstractCoherencePanel} to
 * view summarized persistence data.<br>
 * <strong>Note:</strong> This data is experimental and may be
 * inaccurate and may change in a future release.
 *
 * @author Tim Middleton
 */
public class CoherencePersistencePanel extends AbstractCoherencePanel
{
    private static final long serialVersionUID = 230801760279336008L;

    /**
     * The total amount of active space used.
     */
    private JTextField txtTotalActiveSpaceUsed;

    /**
     * The current Max latency.
     */
    private JTextField txtMaxLatency;
    private long       cMaxMaxLatency = 0L;

    /**
     * The machine statistics data retrieved from the {@link VisualVMModel}.
     */
    private List<Map.Entry<Object, Data>> persistenceData;

    /**
     * The {@link AbstractCoherenceTableModel} to display persistence data.
     */
    protected AbstractCoherenceTableModel<Object, Data> tmodel;

    /**
     * The graph of persistence latency averages.
     */
    private SimpleXYChartSupport persistenceLatencyGraph;

    /**
     * The graph of persistence latency averages.
     */
    private SimpleXYChartSupport persistenceTotalSpaceGraph;


    /**
     * Create the layout for the {@link AbstractCoherencePanel}.
     *
     * @param model {@link VisualVMModel} to use for this panel
     */
    public CoherencePersistencePanel(VisualVMModel model)
    {
        super(new BorderLayout(), model);

        // create a split pane for resizing
        JSplitPane pneSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // Create the header panel
        JPanel pnlHeader = new JPanel();

        pnlHeader.setLayout(new FlowLayout());

        txtTotalActiveSpaceUsed = getTextField(10, JTextField.RIGHT);
        pnlHeader.add(getLocalizedLabel("LBL_total_active_space", txtTotalActiveSpaceUsed));
        pnlHeader.add(txtTotalActiveSpaceUsed);

        txtMaxLatency = getTextField(6, JTextField.RIGHT);
        pnlHeader.add(getLocalizedLabel("LBL_max_latency_across_services", txtMaxLatency));
        pnlHeader.add(txtMaxLatency);

        // create the table
        tmodel = new PersistenceTableModel(VisualVMModel.DataType.PERSISTENCE.getMetadata());

        ExportableJTable table = new ExportableJTable(tmodel);

        table.setPreferredScrollableViewportSize(new Dimension(500, 200));

        // define renderers for the columns
        RenderHelper.setColumnRenderer(table,
                                       PersistenceData.TOTAL_ACTIVE_SPACE_USED,
                                       new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(table, PersistenceData.MAX_LATENCY, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(table,
                                       PersistenceData.AVERAGE_LATENCY,
                                       new RenderHelper.DecimalRenderer(RenderHelper.MILLIS_FORMAT));
        RenderHelper.setColumnRenderer(table,
                                       PersistenceData.TOTAL_ACTIVE_SPACE_USED_MB,
                                       new RenderHelper.IntegerRenderer());
        RenderHelper.setHeaderAlignment(table, JLabel.CENTER);

        // Add some space
        table.setIntercellSpacing(new Dimension(6, 3));
        table.setRowHeight(table.getRowHeight() + 4);

        // Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);

        JPanel      pnlTop     = new JPanel();

        pnlTop.setLayout(new BorderLayout());

        pnlTop.add(pnlHeader, BorderLayout.PAGE_START);
        pnlTop.add(scrollPane, BorderLayout.CENTER);

        pneSplit.add(pnlTop);

        // create a chart for the machine load averages
        persistenceLatencyGraph    = GraphHelper.createPersistenceLatencyGraph();
        persistenceTotalSpaceGraph = GraphHelper.createPersistenceActiveTotalGraph();

        JSplitPane pneSplitPlotter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        pneSplitPlotter.setResizeWeight(0.5);

        pneSplitPlotter.add(persistenceTotalSpaceGraph.getChart());
        pneSplitPlotter.add(persistenceLatencyGraph.getChart());

        pneSplit.add(pneSplitPlotter);
        add(pneSplit);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void updateGUI()
    {
        long         cTotalMemory    = 0;
        final String MEM_FORMAT      = "%,10.2f";
        long         cLatencyMax     = 0L;
        float        cLatencyTotal   = 0.0f;
        float        cLatencyAverage = 0.0f;
        int          c               = 0;

        if (persistenceData != null)
        {
            for (Entry<Object, Data> entry : persistenceData)
            {
                long cTotalMem = (Long) entry.getValue().getColumn(PersistenceData.TOTAL_ACTIVE_SPACE_USED);

                cTotalMemory  += cTotalMem == -1 ? 0 : cTotalMem;
                cLatencyTotal += (Float) entry.getValue().getColumn(PersistenceData.AVERAGE_LATENCY);

                cLatencyMax   = (Long) entry.getValue().getColumn(PersistenceData.MAX_LATENCY);

                if (cLatencyMax > cMaxMaxLatency)
                {
                    cMaxMaxLatency = cLatencyMax;
                }

                c++;
            }

            txtTotalActiveSpaceUsed.setText(String.format(MEM_FORMAT, (cTotalMemory * 1.0f) / GraphHelper.MB));
            cLatencyAverage = cLatencyTotal / c;

        }
        else
        {
            txtTotalActiveSpaceUsed.setText(String.format(MEM_FORMAT, 0));
        }

        GraphHelper.addValuesToPersistenceActiveTotalGraph(persistenceTotalSpaceGraph, cTotalMemory / GraphHelper.MB);
        GraphHelper.addValuesToPersistenceLatencyGraph(persistenceLatencyGraph, cLatencyAverage * 1000.0f);

        txtMaxLatency.setText(Long.toString(cMaxMaxLatency));

        tmodel.fireTableDataChanged();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void updateData()
    {
        persistenceData = model.getData(VisualVMModel.DataType.PERSISTENCE);

        if (persistenceData != null)
        {
            tmodel.setDataList(persistenceData);
        }

    }
}
