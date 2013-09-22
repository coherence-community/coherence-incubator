/*
 * File: AbstractData.java
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

package com.sun.tools.visualvm.modules.coherence.tablemodel.model;

import com.sun.tools.visualvm.modules.coherence.VisualVMModel;

import java.io.Serializable;

import java.math.BigDecimal;

import java.util.Collection;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.management.openmbean.TabularData;

/**
 * An abstract representation of data to be shown on tables and graphs.
 *
 * @author Tim Middleton
 */
public abstract class AbstractData implements Data, DataRetriever, Serializable
{
    private static final long serialVersionUID = 1803170898872716122L;

    /**
     * The array of objects (statistics) for this instance.
     */
    protected Object[] oColumnValues = null;

    /**
     * The column count.
     */
    protected int nColumnCount;


    /**
     * Creates a new instance and initializes the required values.
     */
    public AbstractData(int nColumnCount)
    {
        this.nColumnCount = nColumnCount;

        oColumnValues     = new Object[nColumnCount];
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public SortedMap<Object, Data> getReporterData(TabularData   reportData,
                                                   VisualVMModel model)
    {
        SortedMap<Object, Data> mapData = new TreeMap<Object, Data>();
        Set<?>                  setKeys = reportData.keySet();
        Data                    data    = null;

        if (setKeys.size() > 0)
        {
            // loop through each key, which are the rows of data
            for (Object oKey : setKeys)
            {
                // get the columns as an array
                Object[] aoColumns = ((Collection<Object>) oKey).toArray();

                data = processReporterData(aoColumns, model);

                // save the newly created entry if it exists
                if (data != null)
                {
                    mapData.put(data.getColumn(0), data);
                }
            }
        }

        return mapData;
    }


    /**
     * {@inheritDoc}
     */
    public Object getColumn(int nColumn)
    {
        if (nColumn > nColumnCount - 1)
        {
            throw new IllegalArgumentException("Invalid column index " + nColumn);
        }

        return oColumnValues[nColumn];
    }


    /**
     * {@inheritDoc}
     */
    public void setColumn(int    nColumn,
                          Object oValue)
    {
        if (nColumn > nColumnCount - 1)
        {
            throw new IllegalArgumentException("Invalid column index nColumn=" + nColumn + " , nColumnCount="
                                               + nColumnCount + ", class=" + this.getClass().getName() + "\n"
                                               + this.toString());
        }

        oColumnValues[nColumn] = oValue;
    }


    /**
     * Turn a String number value that may have decimal points to one without.
     *
     * @param sValue  the String value which may actually have decimal placed
     *                or even be in exponential notation.
     *
     * @return the stripped String value
     */
    public static String getNumberValue(String sValue)
    {
        if (sValue != null)
        {
            String s = String.format("%d", new BigDecimal(sValue).longValue());

            return s;
        }

        return null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        StringBuffer sb = new StringBuffer("AbstractData: Class = " + this.getClass().getName());

        for (int i = 0; i < nColumnCount; i++)
        {
            sb.append(", Column ").append(i).append("=").append(oColumnValues[i] == null
                      ? "null" : oColumnValues[i].toString());
        }

        return sb.toString();
    }
}
