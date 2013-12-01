/*
 * File: CoherenceServicePanel.java
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
import com.sun.tools.visualvm.modules.coherence.tablemodel.ServiceMemberTableModel;
import com.sun.tools.visualvm.modules.coherence.tablemodel.ServiceTableModel;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Data;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.ServiceData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.ServiceMemberData;

import java.awt.BorderLayout;
import java.awt.Dimension;

import java.util.List;

import java.util.Map.Entry;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * An implementation of an {@link AbstractCoherencePanel} to
 * view summarized proxy server statistics.
 *
 * @author Tim Middleton
 */
public class CoherenceServicePanel extends AbstractCoherencePanel
{
    private static final long serialVersionUID = -7612569043492412396L;

    /**
     * The currently selected service from the service table.
     */
    private JTextField txtSelectedService;

    /**
     * The total number of threads available across all services.
     */
    private JTextField txtTotalThreads;

    /**
     * The total number of idle threads across the selected service
     */
    private JTextField txtTotalIdle;

    /**
     * The percentage thread utilization across the selected service.
     */
    private JTextField txtTotalThreadUtil;

    /**
     * The {@link ServiceTableModel} to display service data.
     */
    protected ServiceTableModel tmodel;

    /**
     * The {@link ServiceMemberTableModel} to display service member data.
     */
    protected ServiceMemberTableModel tmodelDetail;

    /**
     * The service statistics data retrieved from the {@link VisualVMModel}.
     */
    private List<Entry<Object, Data>> serviceData;

    /**
     * The service member statistics data retrieved from the {@link VisualVMModel}.
     */
    private List<Entry<Object, Data>> serviceMemberData;

    /**
     * The graph of thread utilization percent.
     */
    private SimpleXYChartSupport threadUtilGraph = null;

    /**
     * The graph of task average duration.
     */
    private SimpleXYChartSupport taskAverageGraph = null;

    /**
     * The graph of task backlog.
     */
    private SimpleXYChartSupport taskBacklogGraph = null;

    /**
     * The graph of request average.
     */
    private SimpleXYChartSupport requestAverageGraph = null;

    /**
     * The row selection listener.
     */
    private SelectRowListSelectionListener listener;


    /**
     * Create the layout for the {@link AbstractCoherencePanel}.
     */
    public CoherenceServicePanel(VisualVMModel model)
    {
        super(new BorderLayout(), model);

        // create a split pane for resizing
        JSplitPane pneSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        tmodel       = new ServiceTableModel(VisualVMModel.DataType.SERVICE.getMetadata());
        tmodelDetail = new ServiceMemberTableModel(VisualVMModel.DataType.SERVICE_DETAIL.getMetadata());

        final ExportableJTable table       = new ExportableJTable(tmodel);
        final ExportableJTable tableDetail = new ExportableJTable(tmodelDetail);

        // define renderers for the columns
        RenderHelper.setColumnRenderer(table, ServiceData.STATUS_HA, new RenderHelper.StatusHARenderer());
        RenderHelper.setIntegerRenderer(table, ServiceData.PARTITION_COUNT);
        RenderHelper.setIntegerRenderer(table, ServiceData.PARTITIONS_ENDANGERED);
        RenderHelper.setIntegerRenderer(table, ServiceData.PARTITIONS_VULNERABLE);
        RenderHelper.setIntegerRenderer(table, ServiceData.PARTITIONS_UNBALANCED);
        RenderHelper.setIntegerRenderer(table, ServiceData.PARTITIONS_PENDING);

        RenderHelper.setColumnRenderer(tableDetail,
                                       ServiceMemberData.REQUEST_AVERAGE_DURATION,
                                       new RenderHelper.DecimalRenderer());
        RenderHelper.setColumnRenderer(tableDetail,
                                       ServiceMemberData.TASK_AVERAGE_DURATION,
                                       new RenderHelper.DecimalRenderer());

        RenderHelper.setHeaderAlignment(table, JLabel.CENTER);
        RenderHelper.setHeaderAlignment(tableDetail, JLabel.CENTER);

        table.setPreferredScrollableViewportSize(new Dimension(500, 150));
        tableDetail.setPreferredScrollableViewportSize(new Dimension(700, 150));

        // Add some space
        table.setIntercellSpacing(new Dimension(6, 3));
        table.setRowHeight(table.getRowHeight() + 4);

        tableDetail.setIntercellSpacing(new Dimension(6, 3));
        tableDetail.setRowHeight(table.getRowHeight() + 4);

        // Create the scroll pane and add the table to it.
        JScrollPane pneScroll       = new JScrollPane(table);
        JScrollPane pneScrollDetail = new JScrollPane(tableDetail);

        pneSplit.add(pneScroll);

        // create the detail pane
        JPanel pneDetail = new JPanel();

        pneDetail.setLayout(new BorderLayout());

        JPanel detailHeaderPanel = new JPanel();

        txtSelectedService = getTextField(22, JTextField.LEFT);
        detailHeaderPanel.add(getLocalizedLabel("LBL_selected_service", txtSelectedService));
        detailHeaderPanel.add(txtSelectedService);

        txtTotalThreads = getTextField(5, JTextField.RIGHT);
        detailHeaderPanel.add(getLocalizedLabel("LBL_total_threads", txtTotalThreads));
        detailHeaderPanel.add(txtTotalThreads);

        txtTotalIdle = getTextField(5, JTextField.RIGHT);
        detailHeaderPanel.add(getLocalizedLabel("LBL_total_idle", txtTotalIdle));
        detailHeaderPanel.add(txtTotalIdle);

        txtTotalThreadUtil = getTextField(5, JTextField.RIGHT);
        detailHeaderPanel.add(getLocalizedLabel("LBL_total_utilization", txtTotalThreadUtil));
        detailHeaderPanel.add(txtTotalThreadUtil);

        final JSplitPane pneSplitDetail = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        pneDetail.add(detailHeaderPanel, BorderLayout.PAGE_START);
        pneSplitDetail.add(pneScrollDetail);

        final JTabbedPane pneDetailTabs = new JTabbedPane();

        populateTabs(pneDetailTabs, getLocalizedText("LBL_none_selected"));

        pneSplitDetail.add(pneDetailTabs);

        pneDetail.add(pneSplitDetail, BorderLayout.CENTER);

        pneSplit.add(pneDetail);
        add(pneSplit);

        // define renderers for the columns
        RenderHelper.setColumnRenderer(tableDetail,
                                       ServiceMemberData.THREAD_UTILISATION_PERCENT,
                                       new RenderHelper.ThreadUtilRenderer());

        // add a listener for the selected row
        ListSelectionModel rowSelectionModel = table.getSelectionModel();

        listener = new SelectRowListSelectionListener(table, pneDetailTabs);
        rowSelectionModel.addListSelectionListener(listener);
    }


    /**
     * Populate the tabs on a change of service name.
     *
     * @param pneDetailTabs  the {@link JTabbedPane} to update
     * @param sServiceName   the service name to display
     */
    private void populateTabs(JTabbedPane pneDetailTabs,
                              String      sServiceName)
    {
        // remove any existing tabs
        int cTabs = pneDetailTabs.getTabCount();

        for (int i = 0; i < cTabs; i++)
        {
            pneDetailTabs.removeTabAt(0);
        }

        threadUtilGraph = GraphHelper.createThreadUtilizationGraph(sServiceName);
        pneDetailTabs.addTab(getLocalizedText("LBL_thread_utilization"), threadUtilGraph.getChart());

        taskAverageGraph = GraphHelper.createTaskDurationGraph(sServiceName);
        pneDetailTabs.addTab(getLocalizedText("LBL_task_average_duration"), taskAverageGraph.getChart());

        taskBacklogGraph = GraphHelper.createTaskBacklogGraph(sServiceName);
        pneDetailTabs.addTab(getLocalizedText("LBL_task_backlog"), taskBacklogGraph.getChart());

        requestAverageGraph = GraphHelper.createRequestDurationGraph(sServiceName);
        pneDetailTabs.addTab(getLocalizedText("LBL_request_average_duration"), requestAverageGraph.getChart());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void updateGUI()
    {
        tmodel.fireTableDataChanged();
        tmodelDetail.fireTableDataChanged();

        if (model.getSelectedService() != null)
        {
            listener.updateRowSelection();
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void updateData()
    {
        serviceData = model.getData(VisualVMModel.DataType.SERVICE);

        if (serviceData != null)
        {
            tmodel.setDataList(serviceData);
        }

        serviceMemberData = model.getData(VisualVMModel.DataType.SERVICE_DETAIL);

        // the serviceMemberData is only populated if a service has been selected
        // in the first table
        if (serviceMemberData != null)
        {
            tmodelDetail.setDataList(serviceMemberData);

            int   nTotalThreads        = 0;
            int   nTotalIdle           = 0;
            float nTotalTaskAverage    = 0.0f;
            float nMaxTaskAverage      = 0.0f;
            int   cTotalTaskAverage    = 0;
            float nTotalRequestAverage = 0.0f;
            float nMaxRequestAverage   = 0.0f;
            float cAverage             = 0.0f;
            int   cTotalRequestAverage = 0;
            int   nTotalTaskBacklog    = 0;
            int   nMaxTaskBacklog      = 0;

            for (Entry<Object, Data> entry : serviceMemberData)
            {
                int cThread = (Integer) entry.getValue().getColumn(ServiceMemberData.THREAD_COUNT);
                int cIdle   = (Integer) entry.getValue().getColumn(ServiceMemberData.THREAD_IDLE_COUNT);

                nTotalThreads += (cThread != -1 ? cThread : 0);
                nTotalIdle    += (cIdle != -1 ? cIdle : 0);

                // only include task averages where there is a thread count
                if (cThread > 0)
                {
                    // update values for taks average duration
                    cTotalTaskAverage++;

                    cAverage          = (Float) entry.getValue().getColumn(ServiceMemberData.TASK_AVERAGE_DURATION);

                    nTotalTaskAverage += cAverage;

                    if (cAverage > nMaxTaskAverage)
                    {
                        nMaxTaskAverage = cAverage;
                    }

                    // update values for task backlog
                    int cTaskBacklog = (Integer) entry.getValue().getColumn(ServiceMemberData.TASK_BACKLOG);

                    nTotalTaskBacklog += cTaskBacklog;

                    if (cTaskBacklog > nMaxTaskBacklog)
                    {
                        nMaxTaskBacklog = cTaskBacklog;
                    }
                }

                // calculate request average always, as thread count no relevant
                cTotalRequestAverage++;
                cAverage             = (Float) entry.getValue().getColumn(ServiceMemberData.REQUEST_AVERAGE_DURATION);
                nTotalRequestAverage += cAverage;

                if (cAverage > nMaxRequestAverage)
                {
                    nMaxRequestAverage = cAverage;
                }
            }

            // update the totals
            float threadUtil = nTotalThreads == 0 ? 0f : (float) (nTotalThreads - nTotalIdle) / nTotalThreads;

            if (threadUtilGraph != null)
            {
                GraphHelper.addValuesToThreadUtilizationGraph(threadUtilGraph, (long) (threadUtil * 100));
            }

            if (taskAverageGraph != null)
            {
                GraphHelper.addValuesToTaskDurationGraph(taskAverageGraph,
                                                         nMaxTaskAverage,
                                                         (cTotalTaskAverage == 0
                                                          ? 0 : (nTotalTaskAverage / cTotalTaskAverage)));
            }

            if (taskBacklogGraph != null)
            {
                GraphHelper.addValuesToTaskBacklogGraph(taskBacklogGraph,
                                                        nMaxTaskBacklog,
                                                        (cTotalTaskAverage == 0
                                                         ? 0 : (nTotalTaskBacklog / cTotalTaskAverage)));
            }

            if (requestAverageGraph != null)
            {
                GraphHelper.addValuesToRequestDurationGraph(requestAverageGraph,
                                                            nMaxRequestAverage,
                                                            (cTotalRequestAverage == 0
                                                             ? 0 : (nTotalRequestAverage / cTotalRequestAverage)));
            }

            setThreadValues(nTotalThreads, nTotalIdle, threadUtil);

        }
        else
        {
            setThreadValues(0, 0, 0f);
        }

        String sSelectedService = model.getSelectedService();

        if (sSelectedService == null)
        {
            txtSelectedService.setText("");
        }
        else
        {
            txtSelectedService.setText(sSelectedService);
        }
    }


    /**
     * Update the thread values on the selected service pane.
     *
     * @param nTotalThreads  the total number of threads available across the selected service
     * @param nTotalIdle     the total number of idle threads across the selected service
     * @param nThreadUtil    the percentage thread utilization across the selected service
     */
    private void setThreadValues(int   nTotalThreads,
                                 int   nTotalIdle,
                                 float nThreadUtil)
    {
        txtTotalThreads.setText(String.format("%,5d", nTotalThreads));
        txtTotalIdle.setText(String.format("%,5d", nTotalIdle));
        txtTotalThreadUtil.setText(String.format("%3.1f%%", nThreadUtil * 100));
    }


    /**
     * Inner class to change the the information displayed on the detailModel
     * table when the master changes.
     */
    private class SelectRowListSelectionListener implements ListSelectionListener
    {
    	/**
    	 * The table for this listener.
    	 */
        private ExportableJTable table;
        
        /**
         * The panel that this listener belongs to.
         */
        private JTabbedPane      pneDetailTabs;
        
        /**
         * The currently selected row.
         */
        private int              nSelectedRow;


        /**
         * Create a new listener to changes the detail table.
         *
         * @param table         the {@link ExportableJTable} that is to be selected
         * @param pneDetailTabs the {@link JTabbedPane} to attach to
         */
        public SelectRowListSelectionListener(ExportableJTable table,
                                              JTabbedPane      pneDetailTabs)
        {
            this.table         = table;
            this.pneDetailTabs = pneDetailTabs;
        }


        /**
         * Re-select the last selected row.
         */
        public void updateRowSelection()
        {
            table.addRowSelectionInterval(nSelectedRow, nSelectedRow);
        }


        /**
         * React when a row is selected.
         *
         * @param e {@link ListSelectionEvent} to respond to
         */
        public void valueChanged(ListSelectionEvent e)
        {
            if (e.getValueIsAdjusting())
            {
                return;
            }

            ListSelectionModel selectionModel = (ListSelectionModel) e.getSource();

            if (!selectionModel.isSelectionEmpty())
            {
                nSelectedRow = selectionModel.getMinSelectionIndex();

                // get the service at the selected row, which is the first column
                String sSelectedService = (String) table.getValueAt(nSelectedRow, 0);

                if (!sSelectedService.equals(model.getSelectedService()))
                {
                    model.setSelectedService(sSelectedService);
                    model.eraseServiceMemberData();
                    tmodelDetail.setDataList(null);
                    tmodelDetail.fireTableDataChanged();

                    txtSelectedService.setText(sSelectedService);
                    setThreadValues(0, 0, 0f);

                    populateTabs(pneDetailTabs, sSelectedService);
                }
            }
        }
    }
}
