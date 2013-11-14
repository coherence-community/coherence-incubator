/*
 * File: PersistenceData.java
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
import com.sun.tools.visualvm.modules.coherence.helper.GraphHelper;
import com.sun.tools.visualvm.modules.coherence.helper.JMXUtils;
import com.sun.tools.visualvm.modules.coherence.helper.JMXUtils.Attribute;

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
 * A class to hold persistence data.
 * <strong>Note:</strong> This data is experimental and may be
 * inaccurate and may change in a future release.
 *
 * @author Tim Middleton
 */
public class PersistenceData extends AbstractData
{
    private static final long serialVersionUID = 7769559573242105947L;

    /**
     * Array index for service name.
     */
    public static final int SERVICE_NAME = 0;

    /**
     * Array index for persistence mode.
     */
    public static final int PERSISTENCE_MODE = 1;

    /**
     * Array index for total active space used.
     */
    public static final int TOTAL_ACTIVE_SPACE_USED = 2;

    /**
     * Array index for total active space used MB.
     */
    public static final int TOTAL_ACTIVE_SPACE_USED_MB = 3;

    /**
     * Array index for average latency
     */
    public static final int AVERAGE_LATENCY = 4;

    /**
     * Array index for max latency.
     */
    public static final int MAX_LATENCY = 5;

    /**
     * Array index for number of snapshots.
     */
    public static final int SNAPSHOT_COUNT = 6;

    /**
     * The logger object to use.
     */
    private static final Logger LOGGER = Logger.getLogger(PersistenceData.class.getName());


    /**
     * Constructor passing in the number of columns.
     *
     */
    public PersistenceData()
    {
        super(SNAPSHOT_COUNT + 1);
    }


    /**
     * {@inheritDoc }
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
        Data                    data;

        if (!model.is1213AndAbove())
        {
            return null;
        }

        try
        {
            // firstly obtain the list of services that have PersistenceMode
            // not 'n/a'
            Set<ObjectName> servicesSet = server.queryNames(new ObjectName("Coherence:type=Service,*"), null);

            for (Iterator<ObjectName> serviceNameIter = servicesSet.iterator(); serviceNameIter.hasNext(); )
            {
                ObjectName serviceNameObjName = serviceNameIter.next();
                String     sPersistenceMode   = (String) server.getAttribute(serviceNameObjName, "PersistenceMode");

                // only include if PersistenceMode != 'n/a'
                if (!"n/a".equals(sPersistenceMode))
                {
                    data = null;

                    String serviceName = serviceNameObjName.getKeyProperty("name");

                    // try to get data
                    data = (PersistenceData) mapData.get(serviceName);

                    if (data == null)
                    {
                        // create the entry
                        data = new PersistenceData();

                        data.setColumn(PersistenceData.SERVICE_NAME, serviceName);
                        data.setColumn(PersistenceData.PERSISTENCE_MODE, sPersistenceMode);
                        data.setColumn(PersistenceData.TOTAL_ACTIVE_SPACE_USED, new Long(0L));
                        data.setColumn(PersistenceData.MAX_LATENCY, new Long(0L));
                        data.setColumn(PersistenceData.AVERAGE_LATENCY, new Float(0.0f));

                        // get the number of snapshots from the PersistenceManager MBean
                        String[] asSnapshots = (String[]) JMXUtils.runJMXQuerySingleResult(server,
                                                                                           "Coherence:type=PersistenceSnapshot,service="
                                                                                           + serviceName
                                                                                           + ",responsibility=SnapshotManager",
                                                                                           new Attribute("Snapshots"));

                        data.setColumn(PersistenceData.SNAPSHOT_COUNT, asSnapshots == null ? 0 : asSnapshots.length);

                        mapData.put(serviceName, data);
                    }
                }
            }

            // now loop through each individual service

            for (Iterator<Object> serviceNameIter = mapData.keySet().iterator(); serviceNameIter.hasNext(); )
            {
                String serviceName = (String) serviceNameIter.next();

                // select only the current service so we can determine the number of storage-enabled
                // members.
                Set<ObjectName> resultSet = server.queryNames(new ObjectName("Coherence:type=Service,name=" + serviceName + ",*"),
                                                  null);

                int cNodes = 0;

                data = null;

                for (Iterator<ObjectName> iter = resultSet.iterator(); iter.hasNext(); )
                {
                    ObjectName objectName = (ObjectName) iter.next();

                    // only include storage-enabled members
                    if ((Boolean) server.getAttribute(objectName, "StorageEnabled"))
                    {
                        data = (PersistenceData) mapData.get(serviceName);

                        // PersistenceActiveSpaceUsed is only valid for active persistence mode
                        if ("active".equals(data.getColumn(PersistenceData.PERSISTENCE_MODE)))
                        {
                            data.setColumn(PersistenceData.TOTAL_ACTIVE_SPACE_USED,
                                           (Long) data.getColumn(PersistenceData.TOTAL_ACTIVE_SPACE_USED)
                                           + (Long) server.getAttribute(objectName, "PersistenceActiveSpaceUsed"));
                        }

                        // update the max (of the max latencies)
                        long nMaxLatency = (Long) server.getAttribute(objectName, "PersistenceLatencyMax");

                        if (nMaxLatency > (Long) data.getColumn(PersistenceData.MAX_LATENCY))
                        {
                            data.setColumn(PersistenceData.MAX_LATENCY, new Long(nMaxLatency));
                        }

                        // count the nodes for working out the average at the end as we are adding up
                        // all of the averages, putting them in the average latency
                        // and average them
                        cNodes++;
                        data.setColumn(PersistenceData.AVERAGE_LATENCY,
                                       (Float) data.getColumn(PersistenceData.AVERAGE_LATENCY)
                                       + (Float) server.getAttribute(objectName, "PersistenceLatencyAverage"));

                        mapData.put(serviceName, data);
                    }
                }

                data = (PersistenceData) mapData.get(serviceName);

                // update the average of the averages only if > 1 service node
                if (cNodes > 0)
                {
                    data.setColumn(PersistenceData.AVERAGE_LATENCY,
                                   (Float) data.getColumn(PersistenceData.AVERAGE_LATENCY) / cNodes);
                }

                data.setColumn(PersistenceData.TOTAL_ACTIVE_SPACE_USED_MB,
                               (Long) data.getColumn(PersistenceData.TOTAL_ACTIVE_SPACE_USED) / GraphHelper.MB);

                mapData.put(serviceName, data);
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
