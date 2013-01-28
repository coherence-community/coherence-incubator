/*
 * File: NamedCacheModel.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of 
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting
 * or https://oss.oracle.com/licenses/CDDL
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

package com.oracle.coherence.patterns.pushreplication.examples;

import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.ContinuousQueryCache;

import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;

import com.tangosol.util.filter.AlwaysFilter;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

/**
 * A {@link NamedCacheModel} is an Swing {@link AbstractTableModel}
 * for {@link NamedCache}s.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class NamedCacheModel<K extends Comparable<K>, V> extends AbstractTableModel implements MapListener
{
    private NamedCache            namedCache;
    private RowDataProvider<K, V> rowProvider;
    private ArrayList<K>          rows;


    /**
     * Constructs a {@link NamedCacheModel} based on the specified {@link NamedCache}.
     *
     * @param namedCache
     */
    public NamedCacheModel(NamedCache namedCache)
    {
        this(namedCache, new DefaultRowDataProvider<K, V>());
    }


    /**
     * Constructs a {@link NamedCacheModel} based on the specified {@link NamedCache}
     * and provided {@link RowDataProvider}.
     *
     * @param namedCache
     * @param rowDataProvider
     */
    public NamedCacheModel(NamedCache            namedCache,
                           RowDataProvider<K, V> rowDataProvider)
    {
        this.rowProvider = rowDataProvider;
        this.rows        = new ArrayList<K>(namedCache.size());
        this.namedCache  = new ContinuousQueryCache(namedCache, AlwaysFilter.INSTANCE, this);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getColumnCount()
    {
        return rowProvider.getColumnCount();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnName(int column)
    {
        return rowProvider.getColumnName(column);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getRowCount()
    {
        return rows.size();
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object getValueAt(int rowIndex,
                             int columnIndex)
    {
        if (rowIndex < rows.size())
        {
            K key = rows.get(rowIndex);

            return rowProvider.getValueAt(key, (V) namedCache.get(key), columnIndex);
        }
        else
        {
            // no such entry
            return null;
        }
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void entryInserted(MapEvent mapEvent)
    {
        synchronized (rows)
        {
            rows.add((K) mapEvent.getKey());
        }

        super.fireTableDataChanged();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void entryDeleted(MapEvent mapEvent)
    {
        synchronized (rows)
        {
            rows.remove(mapEvent.getKey());
        }

        super.fireTableDataChanged();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void entryUpdated(MapEvent mapEvent)
    {
        super.fireTableDataChanged();
    }


    /**
     * A {@link RowDataProvider} provides row rendering information for a {@link NamedCacheModel}s.
     */
    public interface RowDataProvider<K, V>
    {
        /**
         * Obtains the number of columns being provided by the {@link RowDataProvider}.
         *
         * @return the number of columns
         */
        public int getColumnCount();


        /**
         * Obtains the name of a specific column.
         *
         * @param column  the column number
         *
         * @return the name of a specific column number
         */
        public String getColumnName(int column);


        /**
         * Obtains the {@link Class} of information contained in the
         * specified column.
         *
         * @param column  the column number
         *
         * @return the {@link Class} of information
         */
        public Class<?> getColumnClass(int column);


        /**
         * Obtains the value for a specific column given a {@link NamedCache}
         * entry.
         *
         * @param key
         * @param value
         * @param column
         *
         * @return  the value for a specific column
         */
        public Object getValueAt(K   key,
                                 V   value,
                                 int column);
    }


    /**
     * The default implementation of a {@link RowDataProvider}.
     */
    public static class DefaultRowDataProvider<K, V> implements RowDataProvider<K, V>
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public Class<?> getColumnClass(int columnIndex)
        {
            return String.class;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public int getColumnCount()
        {
            return 2;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public String getColumnName(int columnIndex)
        {
            return "Entry";
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public Object getValueAt(K   key,
                                 V   value,
                                 int column)
        {
            switch (column)
            {
            case 0 :
                return key;

            case 1 :
                return value;

            default :
                return null;
            }
        }
    }
}
