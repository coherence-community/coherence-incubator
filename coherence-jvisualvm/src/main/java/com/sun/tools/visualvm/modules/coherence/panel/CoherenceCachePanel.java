/*
 * File: CoherenceCachePanel.java
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

import com.sun.tools.visualvm.modules.coherence.VisualVMModel;
import com.sun.tools.visualvm.modules.coherence.helper.RenderHelper;
import com.sun.tools.visualvm.modules.coherence.panel.util.ExportableJTable;
import com.sun.tools.visualvm.modules.coherence.tablemodel.AbstractCoherenceTableModel;
import com.sun.tools.visualvm.modules.coherence.tablemodel.CacheTableModel;
import com.sun.tools.visualvm.modules.coherence.tablemodel.ServiceTableModel;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.CacheData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.CacheDetailData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.CacheStorageManagerData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Data;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Pair;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Tuple;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

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
 * An implementation of an {@link AbstractCoherencePanel} to view summarized cache
 * size statistics.
 *
 * @author Tim Middleton
 */
public class CoherenceCachePanel extends AbstractCoherencePanel
{
    private static final long serialVersionUID = -7612569043492412496L;

    /**
     * Total number of caches.
     */
    private JTextField txtTotalCaches;

    /**
     * Total primary copy memory used by caches.
     */
    private JTextField txtTotalMemory;

    /**
     * Selected Cache.
     */
    private JTextField txtSelectedCache;

    /**
     * Max query duration across nodes.
     */
    private JTextField txtMaxQueryDuration;

    /**
     * Max query description across nodes.
     */
    private JTextField txtMaxQueryDescription;

    /**
     * The {@link AbstractCoherenceTableModel} to display cache data.
     */
    protected AbstractCoherenceTableModel<Object, Data> tmodel;

    /**
     * The {@link AbstractCoherenceTableModel} to display detail cache data.
     */
    protected AbstractCoherenceTableModel<Object, Data> tmodelDetail;

    /**
     * The {@link AbstractCoherenceTableModel} to display cache storage data.
     */
    protected AbstractCoherenceTableModel<Object, Data> tmodelStorage;

    /**
     * The cache data retrieved from the {@link VisualVMModel}.
     */
    private List<Entry<Object, Data>> cacheData = null;

    /**
     * The detailed cache data retrieved from the {@link VisualVMModel}.
     */
    private List<Entry<Object, Data>> cacheDetailData = null;

    /**
     * The storage cache data retrieved from the {@link VisualVMModel}.
     */
    private List<Entry<Object, Data>> cacheStorageData = null;


    /**
     * Create the layout for the {@link AbstractCoherencePanel}.
     *
     * @param model {@link VisualVMModel} to use for this panel
     */
    public CoherenceCachePanel(VisualVMModel model)
    {
        super(new BorderLayout(), model);

        // create a split pane for resizing
        JSplitPane pneSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // Populate the header panel
        JPanel pnlTop    = new JPanel(new BorderLayout());
        JPanel pnlHeader = new JPanel();

        pnlHeader.setLayout(new FlowLayout());

        txtTotalCaches = getTextField(5);
        pnlHeader.add(getLocalizedLabel("LBL_total_caches", txtTotalCaches));
        pnlHeader.add(txtTotalCaches);

        txtTotalMemory = getTextField(10);
        pnlHeader.add(getLocalizedLabel("LBL_total_data", txtTotalMemory));
        pnlHeader.add(txtTotalMemory);

        pnlTop.add(pnlHeader, BorderLayout.PAGE_START);

        // create any table models required
        tmodel        = new CacheTableModel(VisualVMModel.DataType.CACHE.getMetadata());
        tmodelDetail  = new ServiceTableModel(VisualVMModel.DataType.CACHE_DETAIL.getMetadata());
        tmodelStorage = new ServiceTableModel(VisualVMModel.DataType.CACHE_STORAGE_MANAGER.getMetadata());

        final ExportableJTable table        = new ExportableJTable(tmodel);
        final ExportableJTable tableDetail  = new ExportableJTable(tmodelDetail);
        final ExportableJTable tableStorage = new ExportableJTable(tmodelStorage);

        table.setPreferredScrollableViewportSize(new Dimension(500, 150));
        tableDetail.setPreferredScrollableViewportSize(new Dimension(500, 150));
        tableStorage.setPreferredScrollableViewportSize(new Dimension(500, 150));

        // define renderers for the columns
        RenderHelper.setColumnRenderer(table, CacheData.SIZE, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(table, CacheData.AVG_OBJECT_SIZE, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(table, CacheData.MEMORY_USAGE_MB, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(table, CacheData.MEMORY_USAGE_BYTES, new RenderHelper.IntegerRenderer());

        RenderHelper.setHeaderAlignment(table, JLabel.CENTER);
        RenderHelper.setHeaderAlignment(tableDetail, JLabel.CENTER);
        RenderHelper.setHeaderAlignment(tableStorage, JLabel.CENTER);

        RenderHelper.setColumnRenderer(tableDetail, CacheDetailData.CACHE_HITS, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(tableDetail, CacheDetailData.CACHE_MISSES, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(tableDetail, CacheDetailData.MEMORY_BYTES, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(tableDetail, CacheDetailData.TOTAL_GETS, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(tableDetail, CacheDetailData.TOTAL_PUTS, new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(tableDetail, CacheDetailData.SIZE, new RenderHelper.IntegerRenderer());

        RenderHelper.setColumnRenderer(tableDetail,
                                       CacheDetailData.HIT_PROBABILITY,
                                       new RenderHelper.CacheHitProbabilityRateRenderer());

        RenderHelper.setColumnRenderer(tableStorage,
                                       CacheStorageManagerData.LOCKS_GRANTED,
                                       new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(tableStorage,
                                       CacheStorageManagerData.LOCKS_PENDING,
                                       new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(tableStorage,
                                       CacheStorageManagerData.LISTENER_REGISTRATIONS,
                                       new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(tableStorage,
                                       CacheStorageManagerData.MAX_QUERY_DURATION,
                                       new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(tableStorage,
                                       CacheStorageManagerData.NON_OPTIMIZED_QUERY_AVG,
                                       new RenderHelper.IntegerRenderer());
        RenderHelper.setColumnRenderer(tableStorage,
                                       CacheStorageManagerData.OPTIMIZED_QUERY_AVG,
                                       new RenderHelper.IntegerRenderer());

        table.setIntercellSpacing(new Dimension(6, 3));
        table.setRowHeight(table.getRowHeight() + 4);

        tableDetail.setIntercellSpacing(new Dimension(6, 3));
        tableDetail.setRowHeight(table.getRowHeight() + 4);

        tableStorage.setIntercellSpacing(new Dimension(6, 3));
        tableStorage.setRowHeight(table.getRowHeight() + 4);

        // Create the scroll pane and add the table to it.
        JScrollPane scrollPane        = new JScrollPane(table);
        JScrollPane scrollPaneDetail  = new JScrollPane(tableDetail);
        JScrollPane scrollPaneStorage = new JScrollPane(tableStorage);

        pnlTop.add(scrollPane, BorderLayout.CENTER);
        pneSplit.add(pnlTop);

        JPanel bottomPanel       = new JPanel(new BorderLayout());
        JPanel detailHeaderPanel = new JPanel();

        txtSelectedCache = getTextField(30, JTextField.LEFT);
        detailHeaderPanel.add(getLocalizedLabel("LBL_selected_service_cache", txtSelectedCache));
        detailHeaderPanel.add(txtSelectedCache);

        txtMaxQueryDuration = getTextField(5);
        detailHeaderPanel.add(getLocalizedLabel("LBL_max_query_millis", txtMaxQueryDuration));
        detailHeaderPanel.add(txtMaxQueryDuration);

        txtMaxQueryDescription = getTextField(30, JTextField.LEFT);
        detailHeaderPanel.add(getLocalizedLabel("LBL_max_query_desc", txtMaxQueryDescription));
        detailHeaderPanel.add(txtMaxQueryDescription);

        bottomPanel.add(detailHeaderPanel, BorderLayout.PAGE_START);

        JTabbedPane pneTab = new JTabbedPane();

        pneTab.addTab(getLocalizedText("TAB_cache"), scrollPaneDetail);
        pneTab.addTab(getLocalizedText("TAB_storage"), scrollPaneStorage);

        bottomPanel.add(pneTab, BorderLayout.CENTER);

        pneSplit.add(bottomPanel);

        add(pneSplit);

        // add a listener for the selected row
        ListSelectionModel rowSelectionModel = table.getSelectionModel();

        rowSelectionModel.addListSelectionListener(new SelectRowListSelectionListener(table));
    }


    /**
      * {@inheritDoc}
      */
    @Override
    public void updateData()
    {
        cacheData        = model.getData(VisualVMModel.DataType.CACHE);
        cacheDetailData  = model.getData(VisualVMModel.DataType.CACHE_DETAIL);
        cacheStorageData = model.getData(VisualVMModel.DataType.CACHE_STORAGE_MANAGER);

        if (cacheData != null)
        {
            tmodel.setDataList(cacheData);
        }

        if (cacheDetailData != null)
        {
            tmodelDetail.setDataList(cacheDetailData);
        }

        if (cacheStorageData != null)
        {
            tmodelStorage.setDataList(cacheStorageData);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void updateGUI()
    {
        if (cacheData != null)
        {
            txtTotalCaches.setText(String.format("%5d", cacheData.size()));

            float cTotalCacheSize = 0.0f;

            for (Entry<Object, Data> entry : cacheData)
            {
                cTotalCacheSize += new Float((Integer) entry.getValue().getColumn(CacheData.MEMORY_USAGE_MB));
            }

            txtTotalMemory.setText(String.format("%10.2f", cTotalCacheSize));
        }
        else
        {
            txtTotalCaches.setText("0");
            txtTotalMemory.setText(String.format("%10.2f", 0.0));
        }

        Tuple selectedCache = model.getSelectedCache();

        if (selectedCache == null)
        {
            txtSelectedCache.setText("");
        }
        else
        {
            txtSelectedCache.setText(selectedCache.toString());
        }

        if (cacheStorageData != null)
        {
            long   lMaxQueryMillis      = 0;
            String sMaxQueryDescription = "";

            for (Entry<Object, Data> entry : cacheStorageData)
            {
                long lMaxValue = (Long) entry.getValue().getColumn(CacheStorageManagerData.MAX_QUERY_DURATION);

                if (lMaxValue > lMaxQueryMillis)
                {
                    lMaxQueryMillis = lMaxValue;
                    sMaxQueryDescription =
                        (String) entry.getValue().getColumn(CacheStorageManagerData.MAX_QUERY_DESCRIPTION);
                }
            }

            txtMaxQueryDescription.setText(sMaxQueryDescription);
            txtMaxQueryDuration.setText(String.format("%5d", lMaxQueryMillis));
        }

        tmodel.fireTableDataChanged();
        tmodelDetail.fireTableDataChanged();
        tmodelStorage.fireTableDataChanged();
    }


    /**
     * Inner class to change the the information displayed on the detailModel
     * table when the master changes.
     */
    private class SelectRowListSelectionListener implements ListSelectionListener
    {
        private ExportableJTable table;


        /**
         * Create a new listener to changes the detail table.
         *
         * @param table  the table that is to be selected
         */
        public SelectRowListSelectionListener(ExportableJTable table)
        {
            this.table = table;
        }


        /**
         * Change and clear the detailModel on selection of a cache.
         *
         * @param e  the {@link ListSelectionEvent} to respond to
         */
        @SuppressWarnings("unchecked")
		public void valueChanged(ListSelectionEvent e)
        {
            if (e.getValueIsAdjusting())
            {
                return;
            }

            ListSelectionModel selectionModel = (ListSelectionModel) e.getSource();

            if (!selectionModel.isSelectionEmpty())
            {
                int selectedRow = selectionModel.getMinSelectionIndex();

                // get the service at the selected row, which is the first column
                Pair<String, String> selectedCache = (Pair<String, String>) table.getValueAt(selectedRow, 0);

                if (!selectedCache.equals(model.getSelectedCache()))
                {
                    model.setSelectedCache(selectedCache);
                    txtSelectedCache.setText(selectedCache.toString());
                    tmodelDetail.setDataList(null);
                    tmodelDetail.fireTableDataChanged();
                    txtMaxQueryDescription.setText("");
                    txtMaxQueryDuration.setText("");
                    cacheData = null;
                }
            }
        }
    }
}
