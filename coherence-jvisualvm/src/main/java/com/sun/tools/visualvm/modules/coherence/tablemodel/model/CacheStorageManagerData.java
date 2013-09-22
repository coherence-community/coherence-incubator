/*
 * File: CacheStorageManagerData.java
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

/**
 * A class to hold detailed cache storage data.
 *
 * @author Tim Middleton
 */
public class CacheStorageManagerData extends AbstractData
{
    private static final long serialVersionUID = -7989560126715725202L;

    /**
     * Array index for node id.
     */
    public static final int NODE_ID = 0;

    /**
     * Array index for locks granted.
     */
    public static final int LOCKS_GRANTED = 1;

    /**
     * Array index for locks pending.
     */
    public static final int LOCKS_PENDING = 2;

    /**
     * Array index for listener registrations.
     */
    public static final int LISTENER_REGISTRATIONS = 3;

    /**
     * Array index for max query duration.
     */
    public static final int MAX_QUERY_DURATION = 4;

    /**
     * Array index for max query descriptions.
     */
    public static final int MAX_QUERY_DESCRIPTION = 5;

    /**
     * Array index for non-optimized query avg.
     */
    public static final int NON_OPTIMIZED_QUERY_AVG = 6;

    /**
     * Array index for optimized query avg.
     */
    public static final int OPTIMIZED_QUERY_AVG = 7;

    /**
     * The logger object to use.
     */
    private static final Logger LOGGER = Logger.getLogger(CacheStorageManagerData.class.getName());


    /**
     * Constructor passing in the number of columns.
     *
     */
    public CacheStorageManagerData()
    {
        super(OPTIMIZED_QUERY_AVG + 1);
    }


    /**
     * {@inheritDoc}
     */
    public String getReporterReport()
    {
        return null;
    }


    /**
     * {@inheritDoc}
     */
    public List<Map.Entry<Object, Data>> getJMXData(MBeanServerConnection server,
                                                    VisualVMModel         model)
    {
        SortedMap<Object, Data> mapData = new TreeMap<Object, Data>();
        Data                    data;

        if (model.getSelectedCache() != null)
        {
            Pair<String, String> selectedCache = model.getSelectedCache();

            try
            {
                Set<ObjectName> resultSet = server.queryNames(new ObjectName("Coherence:type=StorageManager,service="
                                                                             + selectedCache.getX() + ",cache="
                                                                             + selectedCache.getY() + ",*"),
                                                              null);

                for (Iterator<ObjectName> iter = resultSet.iterator(); iter.hasNext(); )
                {
                    ObjectName objName = (ObjectName) iter.next();
                    String     sNodeId = objName.getKeyProperty("nodeId");

                    data = new CacheStorageManagerData();
                    data.setColumn(CacheStorageManagerData.NODE_ID, new Integer(sNodeId));
                    data.setColumn(CacheStorageManagerData.LOCKS_GRANTED,
                                   (Integer) server.getAttribute(objName, "LocksGranted"));
                    data.setColumn(CacheStorageManagerData.LOCKS_PENDING,
                                   (Integer) server.getAttribute(objName, "LocksPending"));
                    data.setColumn(CacheStorageManagerData.LISTENER_REGISTRATIONS,
                                   (Long) server.getAttribute(objName, "ListenerRegistrations"));
                    data.setColumn(CacheStorageManagerData.MAX_QUERY_DURATION,
                                   (Long) server.getAttribute(objName, "MaxQueryDurationMillis"));
                    data.setColumn(CacheStorageManagerData.MAX_QUERY_DESCRIPTION,
                                   (String) server.getAttribute(objName, "MaxQueryDescription"));
                    data.setColumn(CacheStorageManagerData.NON_OPTIMIZED_QUERY_AVG,
                                   (Long) server.getAttribute(objName, "NonOptimizedQueryAverageMillis"));
                    data.setColumn(CacheStorageManagerData.OPTIMIZED_QUERY_AVG,
                                   (Long) server.getAttribute(objName, "OptimizedQueryAverageMillis"));

                    mapData.put(data.getColumn(0), data);
                }

                return new ArrayList<Map.Entry<Object, Data>>(mapData.entrySet());
            }
            catch (Exception e)
            {
                LOGGER.log(Level.WARNING, "Error getting cache storage managed statistics: " + e.getMessage());

                return null;
            }
        }
        else
        {
            // no selected service, so don't query the storage manager data
            return null;
        }
    }


    /**
     * {@inheritDoc}
     */
    public Data processReporterData(Object[]      aoColumns,
                                    VisualVMModel model)
    {
        return null;    // implement when Coherence version supports this
    }
}
