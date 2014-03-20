/*
 * File: CacheDetailData.java
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
 * A class to hold detailed cache data.
 *
 * @author Tim Middleton
 */
public class CacheDetailData extends AbstractData
{
    private static final long serialVersionUID = -7989960126715125202L;

    /**
     * Array index for node id.
     */
    public static final int NODE_ID = 0;

    /**
     * Array index for size.
     */
    public static final int SIZE = 1;

    /**
     * Array index for memory.
     */
    public static final int MEMORY_BYTES = 2;

    /**
     * Array index for total gets.
     */
    public static final int TOTAL_GETS = 3;

    /**
     * Array index for total puts.
     */
    public static final int TOTAL_PUTS = 4;

    /**
     * Array index for cache hits.
     */
    public static final int CACHE_HITS = 5;

    /**
     * Array index for cache misses.
     */
    public static final int CACHE_MISSES = 6;

    /**
     * Array index for hit probability.
     */
    public static final int HIT_PROBABILITY = 7;

    /**
     * Report for cache details data;
     */
    public static final String REPORT_CACHE_DETAIL = "jconsole-reports/cache-data-detail-report.xml";

    /**
     * The logger object to use.
     */
    private static final Logger LOGGER = Logger.getLogger(CacheDetailData.class.getName());


    /**
     * Constructor passing in the number of columns.
     *
     */
    public CacheDetailData()
    {
        super(HIT_PROBABILITY + 1);
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
        SortedMap<Object, Data> cacheMap = new TreeMap<Object, Data>();
        Data                    data;

        if (model.getSelectedCache() != null)
        {
            Pair<String, String> selectedCache = model.getSelectedCache();

            try
            {
                Set<ObjectName> resultSet = server.queryNames(new ObjectName("Coherence:type=Cache,service="
                                                                             + selectedCache.getX() + ",name="
                                                                             + selectedCache.getY() + ",*"),
                                                              null);

                for (Iterator<ObjectName> iter = resultSet.iterator(); iter.hasNext(); )
                {
                    ObjectName objName = (ObjectName) iter.next();

                    if (objName.getKeyProperty("tier").equals("back"))
                    {
                        String sNodeId = objName.getKeyProperty("nodeId");

                        data = new CacheDetailData();
                        data.setColumn(CacheDetailData.NODE_ID, new Integer(sNodeId));
                        data.setColumn(CacheDetailData.SIZE, (Integer) server.getAttribute(objName, "Size"));
                        data.setColumn(CacheDetailData.MEMORY_BYTES, (Integer) server.getAttribute(objName, "Units") *
                                                                     (Integer) server.getAttribute(objName, "UnitFactor"));
                        data.setColumn(CacheDetailData.CACHE_HITS, (Long) server.getAttribute(objName, "CacheHits"));
                        data.setColumn(CacheDetailData.CACHE_MISSES,
                                       (Long) server.getAttribute(objName, "CacheMisses"));
                        data.setColumn(CacheDetailData.TOTAL_GETS, (Long) server.getAttribute(objName, "TotalGets"));
                        data.setColumn(CacheDetailData.TOTAL_PUTS, (Long) server.getAttribute(objName, "TotalPuts"));
                        data.setColumn(CacheDetailData.HIT_PROBABILITY,
                                       (Double) server.getAttribute(objName, "HitProbability"));

                        cacheMap.put(data.getColumn(0), data);
                    }
                }

                return new ArrayList<Map.Entry<Object, Data>>(cacheMap.entrySet());
            }
            catch (Exception e)
            {
                LOGGER.log(Level.WARNING, "Error getting cache statistics: " + e.getMessage());

                return null;
            }
        }
        else
        {
            // no selected service, so don't query the detail data
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
