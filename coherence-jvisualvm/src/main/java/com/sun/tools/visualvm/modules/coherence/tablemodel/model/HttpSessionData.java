/*
 * File: HttpSessionData.java
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

import java.util.Map.Entry;

import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

/**
 * A class to hold basic HTTP session data.
 *
 * @author Tim Middleton
 */
public class HttpSessionData extends AbstractData
{
    private static final long serialVersionUID = -4580215882770094894L;

    /**
     * Array index for application id.
     */
    public static final int APPLICATION_ID = 0;

    /**
     * Array index for platform. (WebLogic or Other)
     */
    public static final int PLATFORM = 1;

    /**
     * Array index for session timeout.
     */
    public static final int SESSION_TIMEOUT = 2;

    /**
     * Array index for session count.
     */
    public static final int SESSION_COUNT = 3;

    /**
     * Array index for overflow count.
     */
    public static final int OVERFLOW_COUNT = 4;

    /**
     * Array index for average session size.
     */
    public static final int AVG_SESSION_SIZE = 5;

    /**
     * Array index for total reaped sessions.
     */
    public static final int TOTAL_REAPED_SESSIONS = 6;

    /**
     * Array index for average reaped sessions.
     */
    public static final int AVG_REAPED_SESSIONS = 7;

    /**
     * Array index for average reap duration.
     */
    public static final int AVG_REAP_DURATION = 8;

    /**
     * Array index for last reap duration max.
     */
    public static final int LAST_REAP_DURATION_MAX = 9;

    /**
     * Array index for session updates.
     */
    public static final int SESSION_UPDATES = 10;

    /**
     * The logger object to use.
     */
    private static final Logger LOGGER = Logger.getLogger(HttpSessionData.class.getName());


    /**
     * Constructor passing in the number of columns.
     *
     */
    public HttpSessionData()
    {
        super(SESSION_UPDATES + 1);
    }


    /**
     * {@inheritDoc }
     */
    public String getReporterReport()
    {
        return null;    // implement when Coherence version supports this
    }


    /**
     * {@inheritDoc }
     */
    public List<Map.Entry<Object, Data>> getJMXData(MBeanServerConnection server,
                                                    VisualVMModel         model)
    {
        SortedMap<Object, Data> mapData          = new TreeMap<Object, Data>();
        boolean                 isWebLogicServer = false;
        Data                    data;

        try
        {
            // get the list of applications - search for WebLogic first
            Set<ObjectName> applicationSet =
                server.queryNames(new ObjectName("Coherence:type=WebLogicHttpSessionManager,*"),
                                  null);

            if (applicationSet != null && applicationSet.size() != 0)
            {
                isWebLogicServer = true;
            }
            else
            {
                applicationSet = server.queryNames(new ObjectName("Coherence:type=HttpSessionManager,*"), null);
            }

            for (Iterator<ObjectName> applicationIter = applicationSet.iterator(); applicationIter.hasNext(); )
            {
                ObjectName objName = (ObjectName) applicationIter.next();
                String     sAppId  = objName.getKeyProperty("appId");

                data = new HttpSessionData();

                data.setColumn(HttpSessionData.APPLICATION_ID, sAppId);
                data.setColumn(HttpSessionData.PLATFORM, isWebLogicServer ? "WebLogic" : "Other");
                data.setColumn(HttpSessionData.SESSION_TIMEOUT,
                               (Integer) server.getAttribute(objName, "SessionTimeout"));
                data.setColumn(HttpSessionData.LAST_REAP_DURATION_MAX, new Long(0L));
                data.setColumn(HttpSessionData.SESSION_UPDATES, new Integer(0));
                data.setColumn(HttpSessionData.AVG_SESSION_SIZE, new Integer(0));
                data.setColumn(HttpSessionData.AVG_REAP_DURATION, new Long(0L));
                data.setColumn(HttpSessionData.AVG_REAPED_SESSIONS, new Long(0L));
                data.setColumn(HttpSessionData.TOTAL_REAPED_SESSIONS, new Long(0L));

                // populate the session sizes by obtaining the "SessionCacheName" and querying this
                // from the already collected data
                String sSessionCacheName  = (String) server.getAttribute(objName, "SessionCacheName");
                String sOverflowCacheName = (String) server.getAttribute(objName, "OverflowCacheName");

                data.setColumn(HttpSessionData.SESSION_COUNT, getCacheCount(model, sSessionCacheName));
                data.setColumn(HttpSessionData.OVERFLOW_COUNT, getCacheCount(model, sOverflowCacheName));

                mapData.put(sAppId, data);
            }

            // loop through the individual entries and query the detail information
            String sQueryPredicate = isWebLogicServer
                                     ? "Coherence:type=WebLogicHttpSessionManager"
                                     : "Coherence:type=HttpSessionManager";

            int c = 0;

            for (Iterator<Object> iter = mapData.keySet().iterator(); iter.hasNext(); )
            {
                String sAppId = (String) iter.next();

                c++;

                Set<ObjectName> resultSet = server.queryNames(new ObjectName(sQueryPredicate + ",appId=" + sAppId
                                                                             + ",*"),
                                                              null);

                for (Iterator<ObjectName> iterDetail = resultSet.iterator(); iterDetail.hasNext(); )
                {
                    ObjectName objName = (ObjectName) iterDetail.next();

                    data = mapData.get(sAppId);

                    // update the max LastReapDuration
                    long nCurrentLastReapDuration = (Long) server.getAttribute(objName, "LastReapDuration");

                    if (nCurrentLastReapDuration
                        > ((Long) data.getColumn(HttpSessionData.LAST_REAP_DURATION_MAX)).longValue())
                    {
                        data.setColumn(HttpSessionData.LAST_REAP_DURATION_MAX, new Long(nCurrentLastReapDuration));
                    }

                    data.setColumn(HttpSessionData.SESSION_UPDATES,
                                   (Integer) data.getColumn(HttpSessionData.SESSION_UPDATES)
                                   + (Integer) server.getAttribute(objName, "SessionUpdates"));

                    data.setColumn(HttpSessionData.AVG_SESSION_SIZE,
                                   (Integer) data.getColumn(HttpSessionData.AVG_SESSION_SIZE)
                                   + (Integer) server.getAttribute(objName, "SessionAverageSize"));
                    data.setColumn(HttpSessionData.AVG_REAP_DURATION,
                                   (Long) data.getColumn(HttpSessionData.AVG_REAP_DURATION)
                                   + (Long) server.getAttribute(objName, "AverageReapDuration"));
                    data.setColumn(HttpSessionData.AVG_REAPED_SESSIONS,
                                   (Long) data.getColumn(HttpSessionData.AVG_REAPED_SESSIONS)
                                   + (Long) server.getAttribute(objName, "AverageReapedSessions"));
                    data.setColumn(HttpSessionData.TOTAL_REAPED_SESSIONS,
                                   (Long) data.getColumn(HttpSessionData.TOTAL_REAPED_SESSIONS)
                                   + (Long) server.getAttribute(objName, "ReapedSessions"));

                    mapData.put(sAppId, data);
                }

                // update the averages
                data = mapData.get(sAppId);

                if (c > 1)
                {
                    data.setColumn(HttpSessionData.AVG_SESSION_SIZE,
                                   (Integer) data.getColumn(HttpSessionData.AVG_SESSION_SIZE) / c);
                    data.setColumn(HttpSessionData.AVG_REAP_DURATION,
                                   (Long) data.getColumn(HttpSessionData.AVG_REAP_DURATION) / c);
                    data.setColumn(HttpSessionData.AVG_REAPED_SESSIONS,
                                   (Long) data.getColumn(HttpSessionData.AVG_REAPED_SESSIONS) / c);
                }
            }

            return new ArrayList<Map.Entry<Object, Data>>(mapData.entrySet());
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Error getting cache statistics: " + e.getMessage());

            return null;
        }
    }


    /**
     * Return the count of objects in the given cache name. If the
     * cache is in multiple services, which should not be the case
     * for Coherence*Web, the count will be the first entry. The reason
     * is that there is no service name storage against the MBean to
     * find this.
     *
     * @param model      the {@link VisualVMModel} from which to obtain the data
     * @param sCacheName the cache name
     *
     * @return he count of objects in the given cache name
     */
    @SuppressWarnings("unchecked")
    private static int getCacheCount(VisualVMModel model,
                                     String        sCacheName)
    {
        List<Entry<Object, Data>> cacheData = model.getData(VisualVMModel.DataType.CACHE);

        if (sCacheName != null)
        {
            for (Map.Entry<Object, Data> entry : cacheData)
            {
                Pair<String, String> key = (Pair<String, String>) entry.getKey();

                if (sCacheName.equals(key.getY()))
                {
                    return (Integer) entry.getValue().getColumn(CacheData.SIZE);
                }
            }
        }

        return 0;
    }


    /**
     * {@inheritDoc }
     */
    public Data processReporterData(Object[]      aoColumns,
                                    VisualVMModel model)
    {
        return null;    // implement when Coherence version supports this
    }
}
