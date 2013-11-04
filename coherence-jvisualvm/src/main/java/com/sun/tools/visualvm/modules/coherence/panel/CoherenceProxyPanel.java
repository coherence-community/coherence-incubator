/*
 * File: CoherenceProxyPanel.java
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import com.sun.tools.visualvm.charts.SimpleXYChartSupport;
import com.sun.tools.visualvm.modules.coherence.Localization;
import com.sun.tools.visualvm.modules.coherence.VisualVMModel;
import com.sun.tools.visualvm.modules.coherence.helper.GraphHelper;
import com.sun.tools.visualvm.modules.coherence.helper.RenderHelper;
import com.sun.tools.visualvm.modules.coherence.panel.util.ExportableJTable;
import com.sun.tools.visualvm.modules.coherence.tablemodel.ProxyTableModel;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Data;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.ProxyData;

/**
 * An implementation of an {@link AbstractCoherencePanel} to view
 * summarized proxy server data.
 *
 * @author Tim Middleton
 */
public class CoherenceProxyPanel extends AbstractCoherencePanel
{
    private static final long serialVersionUID = -7612569043492412546L;

    /**
     * The total number of proxy servers (tcp-acceptors) running in the cluster.
     */
    private JTextField txtTotalProxyServers;

    /**
     * The total number of proxy server connections.
     */
    private JTextField txtTotalConnections;

    /**
     * A check-box to indicate if the NameService should be included in the list of proxy servers.
     */
    private JCheckBox cbxIncludeNameService;

    /**
     * The graph of proxy server connections.
     */
    private SimpleXYChartSupport proxyGraph;

    /**
     * The proxy statistics data retrieved from the {@link VisualVMModel}.
     */
    private List<Entry<Object, Data>> proxyData;

    /**
     * The {@link ProxyTableModel} to display proxy data.
     */
    protected ProxyTableModel tmodel;


    /**
     * Create the layout for the {@link AbstractCoherencePanel}.
     *
     * @param model {@link VisualVMModel} to use for this panel
     */
    public CoherenceProxyPanel(VisualVMModel model)
    {
        super(new BorderLayout(), model);

        // create a split pane for resizing
        JSplitPane pneSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // Create the header panel
        JPanel pnlHeader = new JPanel();

        pnlHeader.setLayout(new FlowLayout());

        txtTotalProxyServers = getTextField(5, JTextField.RIGHT);
        pnlHeader.add(getLocalizedLabel("LBL_total_proxy_servers", txtTotalProxyServers));
        pnlHeader.add(txtTotalProxyServers);

        txtTotalConnections = getTextField(5, JTextField.RIGHT);
        pnlHeader.add(getLocalizedLabel("LBL_total_connections", txtTotalConnections));
        pnlHeader.add(txtTotalConnections);
        
        cbxIncludeNameService = new JCheckBox(Localization.getLocalText("LBL_include_name_service"));
        cbxIncludeNameService.setMnemonic(KeyEvent.VK_N); 
        cbxIncludeNameService.setSelected(false);

      	pnlHeader.add(cbxIncludeNameService);

        // create the table
        tmodel = new ProxyTableModel(VisualVMModel.DataType.PROXY.getMetadata());

        ExportableJTable table = new ExportableJTable(tmodel);

        table.setPreferredScrollableViewportSize(new Dimension(500, 300));

        // define renderers for the columns
        RenderHelper.setColumnRenderer(table, ProxyData.TOTAL_BYTES_RECEIVED, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(table, ProxyData.TOTAL_BYTES_SENT, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(table, ProxyData.TOTAL_MSG_RECEIVED, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(table, ProxyData.TOTAL_MSG_SENT, new RenderHelper.IntegerRenderer());
        RenderHelper.setHeaderAlignment(table, JLabel.CENTER);

        // Add some space
        table.setIntercellSpacing(new Dimension(6, 3));
        table.setRowHeight(table.getRowHeight() + 4);

        // Create the scroll pane and add the table to it.
        JScrollPane pneScroll = new JScrollPane(table);

        JPanel      pnlTop    = new JPanel(new BorderLayout());

        pnlTop.add(pnlHeader, BorderLayout.PAGE_START);
        pnlTop.add(pneScroll, BorderLayout.CENTER);

        // create a chart for the count of proxy server connections
        proxyGraph = GraphHelper.createTotalProxyConnectionsGraph();

        JPanel pnePlotter = new JPanel(new GridLayout(1, 1));

        pnePlotter.add(proxyGraph.getChart());

        pneSplit.add(pnlTop);
        pneSplit.add(pnePlotter);

        add(pneSplit);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void updateGUI()
    {
        final String MEM_FORMAT        = "%,d";

        int          cTotalConnections = 0;

        if (proxyData != null)
        {
            txtTotalProxyServers.setText(String.format("%5d", proxyData.size()));

            for (Entry<Object, Data> entry : proxyData)
            {
                cTotalConnections += (Integer) entry.getValue().getColumn(ProxyData.CONNECTION_COUNT);
            }

            txtTotalConnections.setText(String.format(MEM_FORMAT, cTotalConnections));
        }

        else
        {
            txtTotalProxyServers.setText(String.format(MEM_FORMAT, 0));
            txtTotalConnections.setText(String.format(MEM_FORMAT, 0));

        }

        tmodel.fireTableDataChanged();

        GraphHelper.addValuesToTotalProxyConnectionsGraph(proxyGraph, cTotalConnections);

    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void updateData()
    {
    	// update the model to indicate if we are going to include the NameService
    	model.setIncludeNameService(cbxIncludeNameService.isSelected());
   
        proxyData = model.getData(VisualVMModel.DataType.PROXY);

        tmodel.setDataList(proxyData);
    }
}
