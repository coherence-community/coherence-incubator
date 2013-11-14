/*
 * File: ProxyData.java
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
 * A class to hold basic proxy data.
 *
 * @author Tim Middleton
 */
public class ProxyData extends AbstractData
{
    private static final long serialVersionUID = 1789802484301825295L;

    /**
     * Array index for host/port.
     */
    public static final int HOST_PORT = 0;

    /**
     * Array index for service name.
     */
    public static final int SERVICE_NAME = 1;

    /**
     * Array index for Node id.
     */
    public static final int NODE_ID = 2;

    /**
     * Array index for connection count.
     */
    public static final int CONNECTION_COUNT = 3;

    /**
     * Array index for outgoing message backlog.
     */
    public static final int OUTGOING_MSG_BACKLOG = 4;

    /**
     * Array index for total bytes received.
     */
    public static final int TOTAL_BYTES_RECEIVED = 5;

    /**
     * Array index for total bytes sent.
     */
    public static final int TOTAL_BYTES_SENT = 6;

    /**
     * Array index for total messages received.
     */
    public static final int TOTAL_MSG_RECEIVED = 7;

    /**
     * Array index for total messages sent.
     */
    public static final int TOTAL_MSG_SENT = 8;

    /**
     * The logger object to use.
     */
    private static final Logger LOGGER = Logger.getLogger(ProxyData.class.getName());


    /**
     * Constructor passing in the number of columns.
     *
     */
    public ProxyData()
    {
        super(TOTAL_MSG_SENT + 1);
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
            Set<ObjectName> proxyNamesSet = server.queryNames(new ObjectName("Coherence:type=ConnectionManager,*"),
                                                              null);

            for (Iterator<ObjectName> nodIter = proxyNamesSet.iterator(); nodIter.hasNext(); )
            {
                ObjectName proxyNameObjName = (ObjectName) nodIter.next();

                String     sServiceName     = proxyNameObjName.getKeyProperty("name");

                // only include the NameService if the model tells us we should  and
                // if its not the NameService, include anyway
                if (("NameService".equals(sServiceName) && model.isIncludeNameService())
                    || !"NameService".equals(sServiceName))
                {
                    data = new ProxyData();

                    data.setColumn(ProxyData.NODE_ID, Integer.valueOf(proxyNameObjName.getKeyProperty("nodeId")));
                    data.setColumn(ProxyData.SERVICE_NAME, proxyNameObjName.getKeyProperty("name"));
                    data.setColumn(ProxyData.HOST_PORT, (String) server.getAttribute(proxyNameObjName, "HostIP"));
                    data.setColumn(ProxyData.CONNECTION_COUNT,
                                   (Integer) server.getAttribute(proxyNameObjName, "ConnectionCount"));
                    data.setColumn(ProxyData.OUTGOING_MSG_BACKLOG,
                                   (Long) server.getAttribute(proxyNameObjName, "OutgoingMessageBacklog"));
                    data.setColumn(ProxyData.TOTAL_BYTES_RECEIVED,
                                   (Long) server.getAttribute(proxyNameObjName, "TotalBytesReceived"));
                    data.setColumn(ProxyData.TOTAL_BYTES_SENT,
                                   (Long) server.getAttribute(proxyNameObjName, "TotalBytesSent"));
                    data.setColumn(ProxyData.TOTAL_MSG_RECEIVED,
                                   (Long) server.getAttribute(proxyNameObjName, "TotalMessagesReceived"));
                    data.setColumn(ProxyData.TOTAL_MSG_SENT,
                                   (Long) server.getAttribute(proxyNameObjName, "TotalMessagesSent"));

                    mapData.put((String) server.getAttribute(proxyNameObjName, "HostIP"), data);
                }

            }

            return new ArrayList<Map.Entry<Object, Data>>(mapData.entrySet());
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Error getting proxy statistics: " + e.getMessage());

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
