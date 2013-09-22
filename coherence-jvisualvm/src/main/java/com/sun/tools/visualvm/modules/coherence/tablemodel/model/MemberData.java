/*
 * File: MemberData.java
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
 * A class to hold basic member data.
 *
 * @author Tim Middleton
 */
public class MemberData extends AbstractData
{
    private static final long serialVersionUID = -1079052422314214578L;

    /**
     * Array index for node id.
     */
    public static int NODE_ID = 0;

    /**
     * Array index for address.
     */
    public static int ADDRESS = 1;

    /**
     * Array index for machine name.
     */
    public static int PORT = 2;

    /**
     * Array index for role name.
     */
    public static int ROLE_NAME = 3;

    /**
     * Array index for publisher success rate.
     */
    public static int PUBLISHER_SUCCESS = 4;

    /**
     * Array index for receiver success rate.
     */
    public static int RECEIVER_SUCCESS = 5;

    /**
     * Array index for send queue size.
     */
    public static int SENDQ_SIZE = 6;

    /**
     * Array index for max memory.
     */
    public static int MAX_MEMORY = 7;

    /**
     * Array index for used memory.
     */
    public static int USED_MEMORY = 8;

    /**
     * Array index for free memory.
     */
    public static int FREE_MEMORY = 9;

    /**
     * The logger object to use.
     */
    private static final Logger LOGGER = Logger.getLogger(MemberData.class.getName());


    /**
     * Constructor passing in the number of columns.
     *
     */
    public MemberData()
    {
        super(FREE_MEMORY + 1);
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
        Data                    data;

        try
        {
            Set<ObjectName> setNodeNames = server.queryNames(new ObjectName("Coherence:type=Node,*"), null);

            for (Iterator<ObjectName> nodIter = setNodeNames.iterator(); nodIter.hasNext(); )
            {
                ObjectName nodeNameObjName = (ObjectName) nodIter.next();

                Integer    nodeId          = Integer.valueOf(nodeNameObjName.getKeyProperty("nodeId"));

                data = new MemberData();

                data.setColumn(MemberData.NODE_ID, nodeId);
                data.setColumn(MemberData.PUBLISHER_SUCCESS,
                               (Float) server.getAttribute(nodeNameObjName, "PublisherSuccessRate"));
                data.setColumn(MemberData.RECEIVER_SUCCESS,
                               (Float) server.getAttribute(nodeNameObjName, "ReceiverSuccessRate"));
                data.setColumn(MemberData.MAX_MEMORY, (Integer) server.getAttribute(nodeNameObjName, "MemoryMaxMB"));
                data.setColumn(MemberData.FREE_MEMORY,
                               (Integer) server.getAttribute(nodeNameObjName, "MemoryAvailableMB"));
                data.setColumn(MemberData.SENDQ_SIZE, (Integer) server.getAttribute(nodeNameObjName, "SendQueueSize"));

                data.setColumn(MemberData.USED_MEMORY,
                               (Integer) data.getColumn(MemberData.MAX_MEMORY)
                               - (Integer) data.getColumn(MemberData.FREE_MEMORY));

                data.setColumn(MemberData.ADDRESS, (String) server.getAttribute(nodeNameObjName, "UnicastAddress"));

                data.setColumn(MemberData.ROLE_NAME, (String) server.getAttribute(nodeNameObjName, "RoleName"));
                data.setColumn(MemberData.PORT, (Integer) server.getAttribute(nodeNameObjName, "UnicastPort"));

                mapData.put(nodeId, data);

            }

            List<Map.Entry<Object, Data>> list = new ArrayList<Map.Entry<Object, Data>>(mapData.entrySet());

            return list;
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Error getting member statistics: " + e.getMessage());

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
