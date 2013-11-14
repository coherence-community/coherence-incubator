/*
 * File: ServiceData.java
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
import java.util.HashSet;
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
 * A class to hold basic service data.
 *
 * @author Tim Middleton
 *
 */
public class ServiceData extends AbstractData
{
    private static final long serialVersionUID = -9192162461585760564L;

    /**
     * Array index for service name.
     */
    public static final int SERVICE_NAME = 0;

    /**
     * Array index for statusHA.
     */
    public static final int STATUS_HA = 1;

    /**
     * Array index for members.
     */
    public static final int MEMBERS = 2;

    /**
     * Array index for storage members.
     */
    public static final int STORAGE_MEMBERS = 3;

    /**
     * Array index for partition count.
     */
    public static final int PARTITION_COUNT = 4;

    /**
     * Array index for partitions endangered.
     */
    public static final int PARTITIONS_ENDANGERED = 5;

    /**
     * Array index for partitions vulnerable.
     */
    public static final int PARTITIONS_VULNERABLE = 6;

    /**
     * Array index for partitions unbalanced.
     */
    public static final int PARTITIONS_UNBALANCED = 7;

    /**
     * Array index for partitions pending.
     */

    public static final int PARTITIONS_PENDING = 8;

    /**
     * The logger object to use.
     */
    private static final Logger LOGGER = Logger.getLogger(ServiceData.class.getName());


    /**
     * Constructor passing in the number of columns.
     *
     */
    public ServiceData()
    {
        super(PARTITIONS_PENDING + 1);
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
    @SuppressWarnings("rawtypes")
    public List<Map.Entry<Object, Data>> getJMXData(MBeanServerConnection server,
                                                    VisualVMModel         model)
    {
        SortedMap<Object, Data> mapData              = new TreeMap<Object, Data>();
        Set<String>             setDistributedCaches = new HashSet<String>();
        Data                    data;

        try
        {
            // firstly obtain the list of services
            Set<ObjectName> servicesSet = server.queryNames(new ObjectName("Coherence:type=Service,*"), null);

            for (Iterator<ObjectName> cacheNameIter = servicesSet.iterator(); cacheNameIter.hasNext(); )
            {
                ObjectName cacheNameObjName = cacheNameIter.next();

                String     sServiceName     = cacheNameObjName.getKeyProperty("name");

                data = new ServiceData();

                data.setColumn(ServiceData.SERVICE_NAME, sServiceName);
                data.setColumn(ServiceData.MEMBERS, new Integer(0));
                data.setColumn(ServiceData.STORAGE_MEMBERS, new Integer(0));

                mapData.put(sServiceName, data);

                // if its dist cache then save so we dont, double count size for repl
                // caches
                if (server.getAttribute(cacheNameObjName, "Type").equals("DistributedCache"))
                {
                    setDistributedCaches.add(sServiceName);
                }
            }

            // update the distributed caches in the model for use later in
            // CacheStats
            model.setDistributedCaches(setDistributedCaches);

            // now loop through each individual service
            for (Iterator<Object> cacheNameIter = mapData.keySet().iterator(); cacheNameIter.hasNext(); )
            {
                String sServiceName = (String) cacheNameIter.next();

                // select only the current service so we can determine the number of storage-enabled
                // members.
                Set resultSet = server.queryNames(new ObjectName("Coherence:type=Service,name=" + sServiceName + ",*"),
                                                  null);

                for (Iterator iter = resultSet.iterator(); iter.hasNext(); )
                {
                    ObjectName objName = (ObjectName) iter.next();

                    data = (ServiceData) mapData.get(sServiceName);

                    // only update the static information once as it will be the same across all members
                    if (data.getColumn(ServiceData.PARTITION_COUNT) == null)
                    {
                        data.setColumn(ServiceData.PARTITIONS_ENDANGERED,
                                       ((Integer) server.getAttribute(objName, "PartitionsEndangered")).intValue());
                        data.setColumn(ServiceData.PARTITIONS_UNBALANCED,
                                       ((Integer) server.getAttribute(objName, "PartitionsUnbalanced")).intValue());
                        data.setColumn(ServiceData.PARTITIONS_VULNERABLE,
                                       ((Integer) server.getAttribute(objName, "PartitionsVulnerable")).intValue());
                        data.setColumn(ServiceData.STATUS_HA, (String) server.getAttribute(objName, "StatusHA"));
                        data.setColumn(ServiceData.PARTITIONS_PENDING,
                                       (int) ((Long) server.getAttribute(objName, "RequestPendingCount")).longValue());
                        data.setColumn(ServiceData.PARTITION_COUNT,
                                       ((Integer) server.getAttribute(objName, "PartitionsAll")).intValue());
                    }

                    data.setColumn(ServiceData.MEMBERS, (Integer) data.getColumn(ServiceData.MEMBERS) + 1);

                    if (((Boolean) server.getAttribute(objName, "StorageEnabled")).booleanValue())
                    {
                        data.setColumn(ServiceData.STORAGE_MEMBERS,
                                       (Integer) data.getColumn(ServiceData.STORAGE_MEMBERS) + 1);
                    }

                    mapData.put(sServiceName, data);
                }
            }

            return new ArrayList<Map.Entry<Object, Data>>(mapData.entrySet());
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Error getting service statistics: " + e.getMessage());

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
