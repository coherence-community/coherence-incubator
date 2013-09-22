/*
 * File: AbstractCoherenceTableModel.java
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

package com.sun.tools.visualvm.modules.coherence.tablemodel;

import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Data;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.JTable;

import javax.swing.table.AbstractTableModel;

/**
 * An abstract implementation of {@link AbstractTableModel} which is extended
 * to provide the data for the {@link JTable} implementations.
 *
 * @param <K>  The key object for each row - the type of this will determine sorting
 * @param <V>  The value object for each row
 *
 * @author Tim Middleton
 */
public abstract class AbstractCoherenceTableModel<K, V> extends AbstractTableModel
{
    private static final long serialVersionUID = 8366117699998286805L;

    /**
     * The data list for the model.
     */
    protected List<Map.Entry<K, V>> dataList = Collections.emptyList();

    /**
     * The column names for the model.
     */
    protected String[] asColumnNames;


    /**
     * Creates the mode with the string of column names.
     *
     * @param asColumnNames  the column names
     */
    public AbstractCoherenceTableModel(String[] asColumnNames)
    {
        this.asColumnNames = asColumnNames;
    }


    /**
     * Returns the column count for this model.
     *
     * @return the column count for this model
     */
    public int getColumnCount()
    {
        if (asColumnNames == null)
        {
            throw new IllegalStateException("No definition of AbstractMeasures for this model. " + this.getClass());
        }

        return asColumnNames.length;
    }


    /**
     * {@inheritDoc}
     */
    public int getRowCount()
    {
        return dataList == null ? 0 : dataList.size();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnName(int col)
    {
        if (asColumnNames == null)
        {
            throw new IllegalStateException("No definition of AbstractMeasures for this model. " + this.getClass());
        }

        return asColumnNames[col];
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getColumnClass(int col)
    {
        if (getValueAt(0, col) != null)
        {
            return getValueAt(0, col).getClass();
        }
        else
        {
            return (String.class);
        }
    }


    /**
     * {@inheritDoc}
     */
    public Object getValueAt(int row,
                             int col)
    {
        Map.Entry<K, V> entry = dataList.get(row);

        if (entry != null)
        {
            Object value = entry.getValue();

            if (value instanceof Data)
            {
                return ((Data) value).getColumn(col);
            }
            else
            {
                return null;
            }
        }
        else
        {
            return null;
        }
    }


    /**
     * Returns the data list for this model.
     *
     * @param dataList the data list for this model
     */
    public void setDataList(List<Map.Entry<K, V>> dataList)
    {
        this.dataList = dataList;
    }


    /**
     * Returns the column names for this model.
     *
     * @return the column names for this model
     */
    public String[] getColumnNames()
    {
        return asColumnNames;
    }
}
