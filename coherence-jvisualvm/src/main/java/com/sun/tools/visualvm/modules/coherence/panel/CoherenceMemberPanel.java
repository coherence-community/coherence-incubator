/*
 * File: CoherenceMemberPanel.java
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
import com.sun.tools.visualvm.modules.coherence.tablemodel.MemberTableModel;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.ClusterData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Data;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.MemberData;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

import java.util.List;

import java.util.Map.Entry;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

/**
 * An implementation of an {@link AbstractCoherencePanel} to
 * view summarized member data.
 *
 * @author Tim Middleton
 */
public class CoherenceMemberPanel extends AbstractCoherencePanel
{
    private static final long serialVersionUID = -7612569043492412546L;

    /**
     * The total number of members in the cluster.
     */
    private JTextField txtTotalMembers;

    /**
     * The total amount of memory allocated in the cluster by all storage-disabled
     * and storage-enabled members.
     */
    private JTextField txtTotalMemory;

    /**
     * The total amount of memory available in the cluster by all storage-disabled
     * and storage-enabled members.
     */
    private JTextField txtTotalMemoryAvail;

    /**
     * The total amount of memory used in the cluster by all storage-disabled
     * and storage-enabled members.
     */
    private JTextField txtTotalMemoryUsed;

    /**
     * The name of the cluster.
     */
    private JTextField txtClusterName;

    /**
     * The license mode of the cluster.
     */
    private JTextField txtLicenseMode;

    /**
     * The total number of members departed.
     */
    private JTextField txtDepartureCount;

    /**
     * The Coherence version of the cluster.
     */
    private JTextField txtVersion;

    /**
     * The graph of cluster memory.
     */
    private SimpleXYChartSupport memoryGraph;

    /**
     * The member statistics data retrieved from the {@link VisualVMModel}.
     */
    private List<Entry<Object, Data>> memberData;

    /**
     * The cluster statistics data retrieved from the {@link VisualVMModel}.
     */
    private List<Entry<Object, Data>> clusterData;

    /**
     * The {@link AbstractCoherenceTableModel} to display member data.
     */
    protected AbstractCoherenceTableModel<Object, Data> tmodel;


    /**
     * Create the layout for the {@link AbstractCoherencePanel}.
     *
     * @param model {@link VisualVMModel} to use for this panel
     */
    public CoherenceMemberPanel(VisualVMModel model)
    {
        super(new BorderLayout(), model);

        // create a split pane for resizing
        JSplitPane pneSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // Create the header panel
        JPanel     pnlHeader = new JPanel();

        GridLayout layHeader = new GridLayout(4, 5);

        layHeader.setHgap(30);
        layHeader.setVgap(2);
        pnlHeader.setLayout(layHeader);

        // row 1
        txtClusterName = getTextField(10, JTextField.LEFT);
        pnlHeader.add(getLocalizedLabel("LBL_cluster_name", txtClusterName));
        pnlHeader.add(txtClusterName);
        pnlHeader.add(getFiller());

        txtLicenseMode = getTextField(5, JTextField.LEFT);
        pnlHeader.add(getLocalizedLabel("LBL_license_mode", txtLicenseMode));
        pnlHeader.add(txtLicenseMode);

        // row 2
        txtVersion = getTextField(10, JTextField.LEFT);
        pnlHeader.add(getLocalizedLabel("LBL_version", txtVersion));
        pnlHeader.add(txtVersion);
        pnlHeader.add(getFiller());

        txtTotalMemory = getTextField(6);
        pnlHeader.add(getLocalizedLabel("LBL_total_cluster_memory", txtTotalMemory));
        pnlHeader.add(txtTotalMemory);

        // row 3
        txtTotalMembers = getTextField(4);
        pnlHeader.add(getLocalizedLabel("LBL_total_members"));
        pnlHeader.add(txtTotalMembers);
        pnlHeader.add(getFiller());

        txtTotalMemoryUsed = getTextField(6);
        pnlHeader.add(getLocalizedLabel("LBL_total_cluster_memory_used", txtTotalMemoryUsed));
        pnlHeader.add(txtTotalMemoryUsed);

        // row 4
        txtDepartureCount = getTextField(5);
        pnlHeader.add(getLocalizedLabel("LBL_member_departure_count", txtDepartureCount));
        pnlHeader.add(txtDepartureCount);
        pnlHeader.add(getFiller());

        txtTotalMemoryAvail = getTextField(6);
        pnlHeader.add(getLocalizedLabel("LBL_total_cluster_memory_avail", txtTotalMemoryAvail));
        pnlHeader.add(txtTotalMemoryAvail);

        pnlHeader.setBorder(new CompoundBorder(new TitledBorder(getLocalizedText("LBL_overview")),
                                               new EmptyBorder(10, 10, 10, 10)));

        // create the table
        tmodel = new MemberTableModel(VisualVMModel.DataType.MEMBER.getMetadata());

        ExportableJTable table = new ExportableJTable(tmodel);

        table.setPreferredScrollableViewportSize(new Dimension(500, 200));

        // define renderers for the columns
        RenderHelper.setColumnRenderer(table, MemberData.PUBLISHER_SUCCESS, new RenderHelper.SuccessRateRenderer());
        RenderHelper.setColumnRenderer(table, MemberData.RECEIVER_SUCCESS, new RenderHelper.SuccessRateRenderer());
        RenderHelper.setColumnRenderer(table, MemberData.SENDQ_SIZE, new RenderHelper.IntegerRenderer());

        RenderHelper.setHeaderAlignment(table, JLabel.CENTER);

        // Add some space
        table.setIntercellSpacing(new Dimension(6, 3));
        table.setRowHeight(table.getRowHeight() + 4);

        // Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);

        JPanel      topPanel   = new JPanel(new BorderLayout());

        topPanel.add(pnlHeader, BorderLayout.PAGE_START);
        topPanel.add(scrollPane, BorderLayout.CENTER);

        // create a chart for the total cluster memory
        memoryGraph = GraphHelper.createClusterMemoryGraph();

        JPanel pnlPlotter = new JPanel(new GridLayout(1, 1));

        pnlPlotter.add(memoryGraph.getChart());

        pneSplit.add(topPanel);
        pneSplit.add(pnlPlotter);

        add(pneSplit);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void updateGUI()
    {
        final String MEM_FORMAT       = "%,d";

        int          cTotalMemory     = 0;
        int          cTotalMemoryUsed = 0;

        if (memberData != null)
        {
            txtTotalMembers.setText(String.format("%5d", memberData.size()));

            for (Entry<Object, Data> entry : memberData)
            {
                cTotalMemory     += (Integer) entry.getValue().getColumn(MemberData.MAX_MEMORY);
                cTotalMemoryUsed += (Integer) entry.getValue().getColumn(MemberData.USED_MEMORY);
            }

            txtTotalMemory.setText(String.format(MEM_FORMAT, cTotalMemory));
            txtTotalMemoryUsed.setText(String.format(MEM_FORMAT, cTotalMemoryUsed));
            txtTotalMemoryAvail.setText(String.format(MEM_FORMAT, cTotalMemory - cTotalMemoryUsed));
        }

        else
        {
            txtTotalMembers.setText("");
            txtTotalMemory.setText(String.format(MEM_FORMAT, 0));
            txtTotalMemoryUsed.setText(String.format(MEM_FORMAT, 0));
            txtTotalMemoryAvail.setText(String.format(MEM_FORMAT, 0));
        }

        if (clusterData != null)
        {
            for (Entry<Object, Data> entry : clusterData)
            {
                txtClusterName.setText(entry.getValue().getColumn(ClusterData.CLUSTER_NAME).toString());
                txtLicenseMode.setText(entry.getValue().getColumn(ClusterData.LICENSE_MODE).toString());
                txtVersion.setText(entry.getValue().getColumn(ClusterData.VERSION).toString().replaceFirst(" .*$", ""));
                txtDepartureCount.setText(entry.getValue().getColumn(ClusterData.DEPARTURE_COUNT).toString());
            }
        }

        tmodel.fireTableDataChanged();

        // update the memory graph
        if (cTotalMemory != 0)
        {
            GraphHelper.addValuesToClusterMemoryGraph(memoryGraph, cTotalMemory, cTotalMemoryUsed);
        }

    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void updateData()
    {
        memberData  = model.getData(VisualVMModel.DataType.MEMBER);
        clusterData = model.getData(VisualVMModel.DataType.CLUSTER);

        if (memberData != null)
        {
            tmodel.setDataList(memberData);
        }

    }
}
