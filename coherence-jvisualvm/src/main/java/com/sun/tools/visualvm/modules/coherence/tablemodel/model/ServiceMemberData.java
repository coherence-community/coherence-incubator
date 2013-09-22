/*
 * File: ServiceMemberData.java
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
 * A class to hold detailed service member data for a selected service.
 *
 * @author Tim Middleton
 */
public class ServiceMemberData extends AbstractData
{
    private static final long serialVersionUID = 255698662206697681L;

    /**
     * Array index for node id.
     */
    public static final int NODE_ID = 0;

    /**
     * Array index for thread count.
     */
    public static final int THREAD_COUNT = 1;

    /**
     * Array index for thread idle count.
     */
    public static final int THREAD_IDLE_COUNT = 2;

    /**
     * Array index for thread utilization percent.
     */
    public static final int THREAD_UTILISATION_PERCENT = 3;

    /**
     * Array index for task average duration.
     */
    public static final int TASK_AVERAGE_DURATION = 4;

    /**
     * Array index for task backlog.
     */
    public static final int TASK_BACKLOG = 5;

    /**
     * Array index for request average duration.
     */
    public static final int REQUEST_AVERAGE_DURATION = 6;

    /**
     * The logger object to use.
     */
    private static final Logger LOGGER = Logger.getLogger(ServiceMemberData.class.getName());


    /**
     * Constructor passing in the number of columns.
     *
     */
    public ServiceMemberData()
    {
        super(REQUEST_AVERAGE_DURATION + 1);
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
        String sSelectedService = model.getSelectedService();
        Data   data;

        if (sSelectedService != null)
        {
            SortedMap<Object, Data> mapData = new TreeMap<Object, Data>();

            try
            {
                Set<ObjectName> servicesSet = server.queryNames(new ObjectName("Coherence:type=Service,name="
                                                                               + sSelectedService + ",*"),
                                                                null);

                for (Iterator<ObjectName> serviceNameIter = servicesSet.iterator(); serviceNameIter.hasNext(); )
                {
                    ObjectName serviceNameObjName = serviceNameIter.next();

                    int        nodeId             = Integer.valueOf(serviceNameObjName.getKeyProperty("nodeId"));

                    data = new ServiceMemberData();

                    data.setColumn(ServiceMemberData.NODE_ID, nodeId);
                    data.setColumn(ServiceMemberData.TASK_BACKLOG,
                                   ((Integer) server.getAttribute(serviceNameObjName, "TaskBacklog")).intValue());
                    data.setColumn(ServiceMemberData.THREAD_COUNT,
                                   ((Integer) server.getAttribute(serviceNameObjName, "ThreadCount")).intValue());
                    data.setColumn(ServiceMemberData.THREAD_IDLE_COUNT,
                                   ((Integer) server.getAttribute(serviceNameObjName, "ThreadIdleCount")).intValue());
                    data.setColumn(ServiceMemberData.REQUEST_AVERAGE_DURATION,
                                   ((Float) server.getAttribute(serviceNameObjName,
                                                                "RequestAverageDuration")).floatValue());
                    data.setColumn(ServiceMemberData.TASK_AVERAGE_DURATION,
                                   ((Float) server.getAttribute(serviceNameObjName,
                                                                "TaskAverageDuration")).floatValue());

                    if ((Integer) data.getColumn(ServiceMemberData.THREAD_COUNT) > 1)
                    {
                        float threadUtil = ((float) ((Integer) data.getColumn(ServiceMemberData.THREAD_COUNT)
                                                     - (Integer) data
                                                         .getColumn(ServiceMemberData
                                                             .THREAD_IDLE_COUNT))) / (Integer) data
                                                                 .getColumn(ServiceMemberData.THREAD_COUNT);

                        data.setColumn(ServiceMemberData.THREAD_UTILISATION_PERCENT, threadUtil);
                    }

                    mapData.put(nodeId, data);
                }

                List<Map.Entry<Object, Data>> list = new ArrayList<Map.Entry<Object, Data>>(mapData.entrySet());

                return list;
            }
            catch (Exception e)
            {
                LOGGER.log(Level.WARNING, "Error getting service member statistics: " + e.getMessage());

                return null;
            }
        }
        else
        {
            // no selected service
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
