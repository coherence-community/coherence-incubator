/*
 * File: GraphHelper.java
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

package com.sun.tools.visualvm.modules.coherence.helper;

import com.sun.tools.visualvm.charts.ChartFactory;
import com.sun.tools.visualvm.charts.SimpleXYChartDescriptor;
import com.sun.tools.visualvm.charts.SimpleXYChartSupport;

import com.sun.tools.visualvm.modules.coherence.Localization;

/**
 * Various helper methods for creating graphs based on JVisualVM graphs.
 *
 * @author Tim Middleton
 *
 */
public class GraphHelper
{
    /**
     * Static MB value.
     */
    public static final int MB = 1024 * 1024;

    /**
     * The number of values to hold for an individual graph. The default value of
     * 50000 represents approximately 1 day of data (but may vary). Setting this 
     * to a higher value will ultimately consume more memory for each graph.
     */
    public static final int VALUES_LIMIT = Integer.getInteger("com.oracle.coherence.jvisualvm.values.limit", 50000);


    /**
     * Create a graph representing the cluster memory.
     *
     * @return a {@link SimpleXYChartSupport} representing the graph
     */
    public static SimpleXYChartSupport createClusterMemoryGraph()
    {
        SimpleXYChartDescriptor sxycd = SimpleXYChartDescriptor.decimal(0, true, VALUES_LIMIT);

        sxycd.setChartTitle(getLocalText("GRPH_cluster_memory_details"));
        sxycd.addLineFillItems(getLocalText("GRPH_total_cluster_memory"), getLocalText("GRPH_used_clsuter_memory"));

        SimpleXYChartSupport factory = ChartFactory.createSimpleXYChart(sxycd);

        return factory;
    }


    /**
     * Add values to the cluster memory graph.
     *
     * @param graph            {@link SimpleXYChartSupport} to add values to
     * @param cTotalMemory     total memory available in cluster
     * @param cTotalMemoryUsed used memory available in cluster
     */
    public static void addValuesToClusterMemoryGraph(SimpleXYChartSupport graph,
                                                     int                  cTotalMemory,
                                                     int                  cTotalMemoryUsed)
    {
        graph.addValues(System.currentTimeMillis(), new long[] {cTotalMemory, cTotalMemoryUsed});
    }


    /**
     * Create a graph representing the publisher success rate.<br>
     * <strong>Note:</strong> Currently the JVisualVM tool does not allow for
     * more that 2 decimal places to be displayed.  Because of this the
     * publisher and receiver success rates will be shown in the tables as
     * 4 decimal places but only 2 in the graphs.  An enhancement request
     * has been logged for this.
     *
     * @return a {@link SimpleXYChartSupport} representing the graph
     */
    public static SimpleXYChartSupport createPublisherGraph()
    {
        SimpleXYChartDescriptor sxycd = SimpleXYChartDescriptor.decimal(1, 0.0001, true, VALUES_LIMIT);

        sxycd.setChartTitle(getLocalText("GRPH_packet_publisher"));
        sxycd.addLineFillItems(getLocalText("GRPH_current_average"), getLocalText("GRPH_current_maximum"));

        SimpleXYChartSupport factory = ChartFactory.createSimpleXYChart(sxycd);

        return factory;
    }


    /**
     * Add values to the publisher success rate graph.
     *
     * @param graph            {@link SimpleXYChartSupport} to add values to
     * @param cMinValue        current minimum value
     * @param cAverageValue    current average value
     * @param cMaxValue        current maximum value
     */
    public static void addValuesToPublisherGraph(SimpleXYChartSupport graph,
                                                 float                cMinValue,
                                                 float                cAverageValue,
                                                 float                cMaxValue)
    {
        graph.addValues(System.currentTimeMillis(),
                        new long[] {(long) (cAverageValue * 10000), (long) (cMinValue * 10000)});
    }


    /**
     * Create a graph representing the receiver success rate.
     *
     * @return a {@link SimpleXYChartSupport} representing the graph
     */
    public static SimpleXYChartSupport createReceiverGraph()
    {
        SimpleXYChartDescriptor sxycd = SimpleXYChartDescriptor.decimal(1, 0.0001, true, VALUES_LIMIT);

        sxycd.setChartTitle(getLocalText("GRPH_packet_receiver"));
        sxycd.addLineFillItems(getLocalText("GRPH_current_average"), getLocalText("GRPH_current_maximum"));

        SimpleXYChartSupport factory = ChartFactory.createSimpleXYChart(sxycd);

        return factory;
    }


    /**
     * Add values to the receiver success rate graph.
     *
     * @param graph            {@link SimpleXYChartSupport} to add values to
     * @param cMinValue        current minimum value
     * @param cAverageValue    current average value
     * @param cMaxValue        current maximum value
     */
    public static void addValuesToReceiverGraph(SimpleXYChartSupport graph,
                                                float                cMinValue,
                                                float                cAverageValue,
                                                float                cMaxValue)
    {
        graph.addValues(System.currentTimeMillis(),
                        new long[] {(long) (cAverageValue * 10000), (long) (cMinValue * 10000)});
    }


    /**
     * Create a graph representing the total cache size.
     *
     * @return        a {@link SimpleXYChartSupport} representing the graph
     */
    public static SimpleXYChartSupport createTotalCacheSizeGraph()
    {
        SimpleXYChartDescriptor sxycd = SimpleXYChartDescriptor.decimal(0, true, VALUES_LIMIT);

        sxycd.setChartTitle(getLocalText("GRPH_total_cache"));
        sxycd.addLineItems(getLocalText("GRPH_total_memory"));

        SimpleXYChartSupport factory = ChartFactory.createSimpleXYChart(sxycd);

        return factory;
    }


    /**
     * Add values to the total cache size graph.
     *
     * @param graph            {@link SimpleXYChartSupport} to add values to
     * @param cTotalCacheSize  total size of all caches
     */
    public static void addValuesToTotalCacheSizeGraph(SimpleXYChartSupport graph,
                                                      float                cTotalCacheSize)
    {
        graph.addValues(System.currentTimeMillis(), new long[] {(long) cTotalCacheSize});
    }


    /**
     * Create a graph representing the total proxy connections.
     *
     * @return a {@link SimpleXYChartSupport} representing the graph
     */
    public static SimpleXYChartSupport createTotalProxyConnectionsGraph()
    {
        SimpleXYChartDescriptor sxycd = SimpleXYChartDescriptor.decimal(0, true, VALUES_LIMIT);

        sxycd.setChartTitle(getLocalText("GRPH_total_connections"));
        sxycd.addLineItems(getLocalText("GRPH_connection_count"));

        SimpleXYChartSupport factory = ChartFactory.createSimpleXYChart(sxycd);

        return factory;
    }


    /**
     * Add values to the total proxy connections graph.
     *
     * @param graph             {@link SimpleXYChartSupport} to add values to
     * @param cTotalConnections total proxy server connections
     */
    public static void addValuesToTotalProxyConnectionsGraph(SimpleXYChartSupport graph,
                                                             int                  cTotalConnections)
    {
        graph.addValues(System.currentTimeMillis(), new long[] {(long) cTotalConnections});
    }


    /**
     * Create a graph representing the load average for cluster machines.
     *
     * @return a {@link SimpleXYChartSupport} representing the graph
     */
    public static SimpleXYChartSupport createMachineLoadAverageGraph()
    {
        SimpleXYChartDescriptor sxycd = SimpleXYChartDescriptor.decimal(0, 0.01, true, VALUES_LIMIT);

        sxycd.setChartTitle(getLocalText("GRPH_load_average"));
        sxycd.addLineFillItems(getLocalText("GRPH_current_maximum"), getLocalText("GRPH_current_average"));

        SimpleXYChartSupport factory = ChartFactory.createSimpleXYChart(sxycd);

        return factory;
    }


    /**
     * Add values to the load average graph.
     *
     * @param graph    {@link SimpleXYChartSupport} to add values to
     * @param cMax     current maximum load average
     * @param cAverage current average load average
     */
    public static void addValuesToLoadAverageGraph(SimpleXYChartSupport graph,
                                                   float                cMax,
                                                   float                cAverage)
    {
        graph.addValues(System.currentTimeMillis(), new long[] {(long) (cMax * 100), (long) (cAverage * 100)});
    }


    /**
     * Create a graph representing the thread utilization percent for a given
     * service.
     *
     * @return a {@link SimpleXYChartSupport} representing the graph
     */
    public static SimpleXYChartSupport createThreadUtilizationGraph(String sServiceName)
    {
        SimpleXYChartDescriptor sxycd = SimpleXYChartDescriptor.percent(true, 5000);

        sxycd.addLineFillItems(getLocalText("GRPH_thread_util"));
        sxycd.setChartTitle(Localization.getLocalText("GRPH_thread_util_percent", new String[] {sServiceName}));

        SimpleXYChartSupport factory = ChartFactory.createSimpleXYChart(sxycd);

        return factory;
    }


    /**
     * Add values to the thread utilization graph.
     *
     * @param graph    {@link SimpleXYChartSupport} to add values to
     * @param lPercent current thread utilization %
     */
    public static void addValuesToThreadUtilizationGraph(SimpleXYChartSupport graph,
                                                         long                 lPercent)
    {
        graph.addValues(System.currentTimeMillis(), new long[] {lPercent});
    }


    /**
     * Create a graph representing the Task average duration.
     *
     * @return a {@link SimpleXYChartSupport} representing the graph
     */
    public static SimpleXYChartSupport createTaskDurationGraph(String sServiceName)
    {
        SimpleXYChartDescriptor sxycd = SimpleXYChartDescriptor.decimal(1, 0.0001, true, 5000);

        sxycd.setChartTitle(Localization.getLocalText("GRPH_task_average", new String[] {sServiceName}));
        sxycd.addLineFillItems(getLocalText("GRPH_current_maximum"), getLocalText("GRPH_current_average"));

        SimpleXYChartSupport factory = ChartFactory.createSimpleXYChart(sxycd);

        return factory;
    }


    /**
     * Add values to the task duration graph.
     *
     * @param graph         {@link SimpleXYChartSupport} to add values to
     * @param cAverageValue current average value
     * @param cMaxValue     current maximum value
     */
    public static void addValuesToTaskDurationGraph(SimpleXYChartSupport graph,
                                                    float                cAverageValue,
                                                    float                cMaxValue)
    {
        graph.addValues(System.currentTimeMillis(),
                        new long[] {(long) (cAverageValue * 10000), (long) (cMaxValue * 10000)});
    }


    /**
     * Create a graph representing the Request average duration.
     *
     * @return a {@link SimpleXYChartSupport} representing the graph
     */
    public static SimpleXYChartSupport createRequestDurationGraph(String sServiceName)
    {
        SimpleXYChartDescriptor sxycd = SimpleXYChartDescriptor.decimal(1, 0.0001, true, 5000);

        sxycd.setChartTitle(Localization.getLocalText("GRPH_request_average", new String[] {sServiceName}));
        sxycd.addLineFillItems(getLocalText("GRPH_current_maximum"), getLocalText("GRPH_current_average"));

        SimpleXYChartSupport factory = ChartFactory.createSimpleXYChart(sxycd);

        return factory;
    }


    /**
     * Add values to the task duration graph.
     *
     * @param graph         {@link SimpleXYChartSupport} to add values to
     * @param cAverageValue current average value
     * @param cMaxValue     current maximum value
     */
    public static void addValuesToRequestDurationGraph(SimpleXYChartSupport graph,
                                                       float                cAverageValue,
                                                       float                cMaxValue)
    {
        graph.addValues(System.currentTimeMillis(),
                        new long[] {(long) (cAverageValue * 10000), (long) (cMaxValue * 10000)});
    }


    /**
     * Create a graph representing the task backlog for a selected service.
     *
     * @return a {@link SimpleXYChartSupport} representing the graph
     */
    public static SimpleXYChartSupport createTaskBacklogGraph(String sServiceName)
    {
        SimpleXYChartDescriptor sxycd = SimpleXYChartDescriptor.decimal(0, 0.01, true, 5000);

        sxycd.setChartTitle(Localization.getLocalText("GRPH_task_backlog", new String[] {sServiceName}));
        sxycd.addLineFillItems(getLocalText("GRPH_current_maximum"), getLocalText("GRPH_current_average"));

        SimpleXYChartSupport factory = ChartFactory.createSimpleXYChart(sxycd);

        return factory;
    }


    /**
     * Add values to the task backlog graph
     *
     * @param graph    {@link SimpleXYChartSupport} to add values to
     * @param cMax     current maximum task backlog
     * @param cAverage current average task backlog
     */
    public static void addValuesToTaskBacklogGraph(SimpleXYChartSupport graph,
                                                   float                cMax,
                                                   float                cAverage)
    {
        graph.addValues(System.currentTimeMillis(), new long[] {(long) (cMax * 100), (long) (cAverage * 100)});
    }


    /**
     * Create a graph representing the average and max persistence latency values.<br>
     * <strong>Note:</strong> Only experimental if displayed.
     *
     * @return a {@link SimpleXYChartSupport} representing the graph
     */
    public static SimpleXYChartSupport createPersistenceLatencyGraph()
    {
        SimpleXYChartDescriptor sxycd = SimpleXYChartDescriptor.decimal(1, 0.0001, true, VALUES_LIMIT);

        sxycd.setChartTitle(getLocalText("GRPH_persistence_latency"));
        sxycd.addLineFillItems(getLocalText("GRPH_current_average_persistence"));

        SimpleXYChartSupport factory = ChartFactory.createSimpleXYChart(sxycd);

        return factory;
    }


    /**
     * Add values to the persistence latency graph.
     *
     * @param graph    {@link SimpleXYChartSupport} to add values to
     * @param cAverage current average latency average
     */
    public static void addValuesToPersistenceLatencyGraph(SimpleXYChartSupport graph,
                                                          float                cAverage)
    {
        graph.addValues(System.currentTimeMillis(), new long[] {(long) (cAverage * 10000)});

    }


    /**
     *  Create a graph representing the total active persistence space used.
     *
     * @return a {@link SimpleXYChartSupport} representing the graph
     */
    public static SimpleXYChartSupport createPersistenceActiveTotalGraph()
    {
        SimpleXYChartDescriptor sxycd = SimpleXYChartDescriptor.decimal(0, true, VALUES_LIMIT);

        sxycd.setChartTitle(getLocalText("GRPH_total_active_space"));
        sxycd.addLineFillItems(getLocalText("GRPH_total_space"));

        SimpleXYChartSupport factory = ChartFactory.createSimpleXYChart(sxycd);

        return factory;
    }


    /**
     * Add values to the total active persistence space used.
     *
     * @param graph                   {@link SimpleXYChartSupport} to add values to
     * @param cTotalPersistenceSpace  total size of all caches
     */
    public static void addValuesToPersistenceActiveTotalGraph(SimpleXYChartSupport graph,
                                                              float                cTotalPersistenceSpace)
    {
        graph.addValues(System.currentTimeMillis(), new long[] {(long) cTotalPersistenceSpace});
    }


    /**
     * Helper method to return localized text.
     *
     * @param sKey the key of text to return
     *
     * @return the localized text
     */
    private static String getLocalText(String sKey)
    {
        return Localization.getLocalText(sKey);
    }


    /**
     * Create a graph representing the http session count over time.
     *
     * @return a {@link SimpleXYChartSupport} representing the graph
     */
    public static SimpleXYChartSupport createSessionCountGraph()
    {
        SimpleXYChartDescriptor sxycd = SimpleXYChartDescriptor.decimal(0, true, VALUES_LIMIT);

        sxycd.setChartTitle(getLocalText("GRPH_session_sizes"));
        sxycd.addLineItems(getLocalText("GRPH_total_session_count"), getLocalText("GRPH_total_overflow_count"));

        SimpleXYChartSupport factory = ChartFactory.createSimpleXYChart(sxycd);

        return factory;
    }


    /**
     * Add values to the HTTP session count graph.
     *
     * @param graph            {@link SimpleXYChartSupport} to add values to
     * @param cTotalSession    total number of sessions
     * @param cTotalOverflow   total number of sessions in overflow cache
     */
    public static void addValuesToSessionCountGraph(SimpleXYChartSupport graph,
                                                    int                  cTotalSession,
                                                    int                  cTotalOverflow)
    {
        graph.addValues(System.currentTimeMillis(), new long[] {cTotalSession, cTotalOverflow});

    }


    /**
     * Create a graph representing the reap duration.
     *
     * @return a {@link SimpleXYChartSupport} representing the graph
     */
    public static SimpleXYChartSupport createReapDurationGraph()
    {
        SimpleXYChartDescriptor sxycd = SimpleXYChartDescriptor.decimal(0, true, VALUES_LIMIT);

        sxycd.setChartTitle(getLocalText("GRPH_reap_druation"));
        sxycd.addLineFillItems(getLocalText("GRPH_current_maximum"), getLocalText("GRPH_current_average"));

        SimpleXYChartSupport factory = ChartFactory.createSimpleXYChart(sxycd);

        return factory;
    }


    /**
     * Add values to the reap duration graph.
     *
     *  @param graph          {@link SimpleXYChartSupport} to add values to
     * @param nCurrentMax     Current Max
     * @param nCurrentAverage Current Averahe
     */
    public static void addValuesToReapDurationGraph(SimpleXYChartSupport graph,
                                                    long                 nCurrentMax,
                                                    long                 nCurrentAverage)
    {
        graph.addValues(System.currentTimeMillis(), new long[] {nCurrentMax, nCurrentAverage});

    }
}
