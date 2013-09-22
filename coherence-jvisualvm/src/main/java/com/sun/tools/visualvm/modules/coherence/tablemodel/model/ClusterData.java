/*
 * File: ClusterData.java
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
 * A class to hold basic cluster data.
 *
 * @author Tim Middleton
 */
public class ClusterData extends AbstractData
{
    private static final long serialVersionUID = 7685333150479196716L;

    /**
     * Array index for cluster name.
     */
    public static final int CLUSTER_NAME = 0;

    /**
     * Array index for license mode.
     */
    public static final int LICENSE_MODE = 1;

    /**
     * Array index for version.
     */
    public static final int VERSION = 2;

    /**
     * Array index for departure count.
     */
    public static final int DEPARTURE_COUNT = 3;

    /**
     * Array index for cluster size;
     */
    public static final int CLUSTER_SIZE = 4;

    /**
     * The logger object to use.
     */
    private static final Logger LOGGER = Logger.getLogger(ClusterData.class.getName());


    /**
     * Constructor passing in the number of columns.
     *
     */
    public ClusterData()
    {
        super(CLUSTER_SIZE + 1);
    }


    /**
     * {@inheritDoc}
     */
    public String getReporterReport()
    {
        return null;    // implement when Coherence version supports this
    }


    /**
     * {@inheritDoc}
     */
    public List<Map.Entry<Object, Data>> getJMXData(MBeanServerConnection server,
                                                    VisualVMModel         model)
    {
        SortedMap<Object, Data> mapData = new TreeMap<Object, Data>();

        try
        {
            Set<ObjectName> clusterSet = server.queryNames(new ObjectName("Coherence:type=Cluster"), null);

            for (Iterator<ObjectName> cacheNameIter = clusterSet.iterator(); cacheNameIter.hasNext(); )
            {
                ClusterData data           = new ClusterData();

                ObjectName  clusterObjName = cacheNameIter.next();

                data.setColumn(ClusterData.CLUSTER_NAME, server.getAttribute(clusterObjName, "ClusterName"));
                data.setColumn(ClusterData.LICENSE_MODE, server.getAttribute(clusterObjName, "LicenseMode"));
                data.setColumn(ClusterData.VERSION, server.getAttribute(clusterObjName, "Version"));
                data.setColumn(ClusterData.DEPARTURE_COUNT,
                               server.getAttribute(clusterObjName, "MembersDepartureCount"));
                data.setColumn(ClusterData.CLUSTER_SIZE, server.getAttribute(clusterObjName, "ClusterSize"));

                mapData.put(data.getColumn(ClusterData.CLUSTER_NAME), data);
            }

            List<Map.Entry<Object, Data>> list = new ArrayList<Map.Entry<Object, Data>>(mapData.entrySet());

            return list;
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Error getting cluster statistics: " + e.getMessage());

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
