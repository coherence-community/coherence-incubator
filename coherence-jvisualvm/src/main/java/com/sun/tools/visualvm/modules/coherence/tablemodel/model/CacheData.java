/*
 * File: CacheData.java
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
 * A class to hold basic cache size data.
 *
 * @author Tim Middleton
 */
public class CacheData extends AbstractData
{
    private static final long serialVersionUID = 6427775621469258645L;

    /**
     * Array index for cache name.
     */
    public static final int CACHE_NAME = 0;

    /**
     * Array index for cache size.
     */
    public static final int SIZE = 1;

    /**
     * Array index for memory usage in bytes.
     */
    public static final int MEMORY_USAGE_BYTES = 2;

    /**
     * Array index for memory usage in MB.
     */
    public static final int MEMORY_USAGE_MB = 3;

    /**
     * Array index for average object size.
     */
    public static final int AVG_OBJECT_SIZE = 4;

    /**
     * The logger object to use.
     */
    private static final Logger LOGGER = Logger.getLogger(CacheData.class.getName());


    /**
     * Constructor passing in the number of columns.
     *
     */
    public CacheData()
    {
        super(AVG_OBJECT_SIZE + 1);
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
            // get the list of caches
            Set<ObjectName> cacheNamesSet = server.queryNames(new ObjectName("Coherence:type=Cache,*"), null);

            for (Iterator<ObjectName> cacheNameIter = cacheNamesSet.iterator(); cacheNameIter.hasNext(); )
            {
                ObjectName           cacheNameObjName = (ObjectName) cacheNameIter.next();

                String               sCacheName       = cacheNameObjName.getKeyProperty("name");
                String               sServiceName     = cacheNameObjName.getKeyProperty("service");

                Pair<String, String> key              = new Pair<String, String>(sServiceName, sCacheName);

                data = new CacheData();

                data.setColumn(CacheData.SIZE, new Integer(0));
                data.setColumn(CacheData.MEMORY_USAGE_MB, new Integer(0));
                data.setColumn(CacheData.MEMORY_USAGE_BYTES, new Long(0));
                data.setColumn(CacheData.MEMORY_USAGE_MB, new Integer(0));
                data.setColumn(CacheData.CACHE_NAME, key);

                mapData.put(key, data);
            }

            // loop through each cache and find all the different node entries for the caches
            // and aggregate the information.

            for (Iterator cacheNameIter = mapData.keySet().iterator(); cacheNameIter.hasNext(); )
            {
                Pair<String, String> key                 = (Pair<String, String>) cacheNameIter.next();
                String               sCacheName          = key.getY();
                String               sServiceName        = key.getX();
                Set<String>          setDistributedCache = model.getDistributedCaches();

                if (setDistributedCache == null)
                {
                    throw new RuntimeException("setDistributedCache must not be null. Make sure SERVICE is before CACHE in enum.");
                }

                boolean fIsDistributedCache = setDistributedCache.contains(sServiceName);

                Set resultSet = server.queryNames(new ObjectName("Coherence:type=Cache,service=" + sServiceName
                                                                 + ",name=" + sCacheName + ",*"),
                                                  null);

                boolean fisSizeCounted = false;    // indicates if non dist cache size has been counted

                for (Iterator iter = resultSet.iterator(); iter.hasNext(); )
                {
                    ObjectName objectName = (ObjectName) iter.next();

                    if (objectName.getKeyProperty("tier").equals("back"))
                    {
                        data = (CacheData) mapData.get(key);

                        if (fIsDistributedCache || (!fIsDistributedCache &&!fisSizeCounted))
                        {
                            data.setColumn(CacheData.SIZE,
                                           (Integer) data.getColumn(CacheData.SIZE)
                                           + (Integer) server.getAttribute(objectName, "Size"));

                            if (!fisSizeCounted)
                            {
                                fisSizeCounted = true;
                            }
                        }

                        // memory is always going to be counted, even for replicated caches
                        data.setColumn(CacheData.MEMORY_USAGE_MB,
                                       (Integer) data.getColumn(CacheData.MEMORY_USAGE_MB)
                                       + (Integer) server.getAttribute(objectName, "Units"));

                        data.setColumn(CacheData.MEMORY_USAGE_BYTES,
                                       (Long) data.getColumn(CacheData.MEMORY_USAGE_BYTES)
                                       + (Integer) server.getAttribute(objectName, "Units"));

                        mapData.put(key, data);

                    }
                }

                // update the cache entry averages
                data = (CacheData) mapData.get(key);

                if ((Integer) data.getColumn(CacheData.SIZE) != 0)
                {
                    data.setColumn(CacheData.AVG_OBJECT_SIZE,
                                   (Long) data.getColumn(CacheData.MEMORY_USAGE_BYTES)
                                   / (Integer) data.getColumn(CacheData.SIZE));
                }

                Long nMemoryUsageMB = ((Long) data.getColumn(CacheData.MEMORY_USAGE_BYTES)) / 1024 / 1024;

                data.setColumn(CacheData.MEMORY_USAGE_MB, Integer.valueOf(nMemoryUsageMB.intValue()));

                mapData.put(key, data);
            }

            List<Map.Entry<Object, Data>> list = new ArrayList<Map.Entry<Object, Data>>(mapData.entrySet());

            return list;
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Error getting cache statistics: " + e.getMessage());

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
