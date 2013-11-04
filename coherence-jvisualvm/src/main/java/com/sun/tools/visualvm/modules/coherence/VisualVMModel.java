/*
 * File: VisualVMModel.java
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

package com.sun.tools.visualvm.modules.coherence;

import com.sun.tools.visualvm.modules.coherence.helper.JMXUtils;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.CacheData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.CacheDetailData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.CacheStorageManagerData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.ClusterData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Data;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.DataRetriever;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.HttpSessionData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.MachineData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.MemberData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Pair;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.PersistenceData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.ProxyData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.ServiceData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.ServiceMemberData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Tuple;

import java.util.ArrayList;
import java.util.HashMap;
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

import javax.management.openmbean.TabularData;

/**
 * A class that is used to store and update Coherence cluster
 * JMX statistics. This is used to avoid placing too much stress on
 * the Management service of a cluster.
 *
 * @author Tim Middleton
 */
public class VisualVMModel
{
    /**
     * Defines the type of data we can collect.
     * Note: The order of these is important. Please do not change. e.g. cluster
     * need to go first so we can determine the version. Also service needs
     * to go before cache so we could setup the list of distributed caches.
     */
    public enum DataType
    {
        CLUSTER(ClusterData.class, CLUSTER_LABELS),
        SERVICE(ServiceData.class, SERVICE_LABELS),
        SERVICE_DETAIL(ServiceMemberData.class, SERVICE_DETAIL_LABELS),
        CACHE(CacheData.class, CACHE_LABELS),
        CACHE_DETAIL(CacheDetailData.class, CACHE_DETAIL_LABELS),
        CACHE_STORAGE_MANAGER(CacheStorageManagerData.class, CACHE_STORAGE_MANAGER_LABELS),
        MEMBER(MemberData.class, MEMBER_LABELS),
        MACHINE(MachineData.class, MACHINE_LABELS),
        PROXY(ProxyData.class, PROXY_LABELS),
        PERSISTENCE(PersistenceData.class, PERSISTENCE_LABELS),
        HTTP_SESSION(HttpSessionData.class, HTTP_SESSION_LABELS);

        /**
         * The {@link Class} associated with this enum.
         */
        private Class clazz;

        /**
         * The column name associated with this enum.
         */
        private String[] asMetadata;


        private DataType(Class    clz,
                         String[] asMeta)
        {
            clazz      = clz;
            asMetadata = asMeta;
        }


        /**
         * Returns the class for this enum.
         *
         * @return the class for this enum
         */
        public Class getClassName()
        {
            return clazz;
        }


        /**
         * Returns the column metadata for this enum.
         *
         * @return the column metadata for this enum
         */
        public String[] getMetadata()
        {
            return asMetadata;
        }
    }


    /**
     * Labels for cluster table. Note: No localization is done for these labels
     * as currently they are not displayed.
     */
    private static final String[] CLUSTER_LABELS = new String[] {"Cluster Name", "License Mode", "Version",
                                                                 "Departure Count", "Cluster Size"};

    /**
     * Labels for service table.
     */
    private static final String[] SERVICE_LABELS = new String[] {Localization.getLocalText("LBL_service_name"),
                                                                 Localization.getLocalText("LBL_status_ha"),
                                                                 Localization.getLocalText("LBL_members"),
                                                                 Localization.getLocalText("LBL_storage_enabled"),
                                                                 Localization.getLocalText("LBL_partitions"),
                                                                 Localization.getLocalText("LBL_endangered"),
                                                                 Localization.getLocalText("LBL_vulnerable"),
                                                                 Localization.getLocalText("LBL_unbalanced"),
                                                                 Localization.getLocalText("LBL_pending")};

    /**
     * Labels for service detail table.
     */
    private static final String[] SERVICE_DETAIL_LABELS = new String[] {Localization.getLocalText("LBL_node_id"),
                                                                        Localization.getLocalText("LBL_threads"),
                                                                        Localization.getLocalText("LBL_idle_threads"),
                                                                        Localization.getLocalText("LBL_thread_util"),
                                                                        Localization.getLocalText("LBL_task_average"),
                                                                        Localization.getLocalText("LBL_task_backlog"),
                                                                        Localization
                                                                            .getLocalText("LBL_request_average")};

    /**
     * Labels for cache table.
     */
    private static final String[] CACHE_LABELS = new String[] {Localization.getLocalText("LBL_service_cache_name"),
                                                               Localization.getLocalText("LBL_size"),
                                                               Localization.getLocalText("LBL_memory_bytes"),
                                                               Localization.getLocalText("LBL_memory_mb"),
                                                               Localization.getLocalText("LBL_average_object_size")};

    /**
     * Labels for cache detail table.
     */
    private static final String[] CACHE_DETAIL_LABELS = new String[] {Localization.getLocalText("LBL_node_id"),
                                                                      Localization.getLocalText("LBL_size"),
                                                                      Localization.getLocalText("LBL_memory_bytes"),
                                                                      Localization.getLocalText("LBL_total_gets"),
                                                                      Localization.getLocalText("LBL_total_puts"),
                                                                      Localization.getLocalText("LBL_cache_hits"),
                                                                      Localization.getLocalText("LBL_cache_misses"),
                                                                      Localization.getLocalText("LBL_hit_probability")};

    /**
     * Labels for storage manager table.
     */
    private static final String[] CACHE_STORAGE_MANAGER_LABELS = new String[] {Localization.getLocalText("LBL_node_id"),
                                                                               Localization
                                                                                   .getLocalText("LBL_locks_granted"),
                                                                               Localization
                                                                                   .getLocalText("LBL_locks_pending"),
                                                                               Localization
                                                                                   .getLocalText("LBL_listener_reg"),
                                                                               Localization
                                                                                   .getLocalText("LBL_max_query_millis"),
                                                                               Localization
                                                                                   .getLocalText("LBL_max_query_desc"),
                                                                               Localization
                                                                                   .getLocalText("LBL_non_opt_avge"),
                                                                               Localization
                                                                                   .getLocalText("LBL_opt_avge")};

    /**
     * Labels for member table.
     */
    private static final String[] MEMBER_LABELS = new String[] {Localization.getLocalText("LBL_node_id"),
                                                                Localization.getLocalText("LBL_unicast_address"),
                                                                Localization.getLocalText("LBL_port"),
                                                                Localization.getLocalText("LBL_role"),
                                                                Localization.getLocalText("LBL_publisher_rate"),
                                                                Localization.getLocalText("LBL_receiver_rate"),
                                                                Localization.getLocalText("LBL_send_q"),
                                                                Localization.getLocalText("LBL_max_memory"),
                                                                Localization.getLocalText("LBL_used_memory"),
                                                                Localization.getLocalText("LBL_free_memory")};

    /**
     * Labels for machine table.
     */
    private static final String[] MACHINE_LABELS = new String[] {Localization.getLocalText("LBL_machine_name"),
                                                                 Localization.getLocalText("LBL_core_count"),
                                                                 Localization.getLocalText("LBL_load_average"),
                                                                 Localization.getLocalText("LBL_total_physical_mem"),
                                                                 Localization.getLocalText("LBL_free_physical_mem"),
                                                                 Localization.getLocalText("LBL_percent_free_mem")};

    /**
     * Labels for proxy table.
     */
    private static final String[] PROXY_LABELS = new String[] {Localization.getLocalText("LBL_ip_port"),
                                                               Localization.getLocalText("LBL_service_name"),
                                                               Localization.getLocalText("LBL_node_id"),
                                                               Localization.getLocalText("LBL_connection_count"),
                                                               Localization.getLocalText("LBL_outgoing_msg_backlog"),
                                                               Localization.getLocalText("LBL_total_bytes_rcv"),
                                                               Localization.getLocalText("LBL_total_bytes_sent"),
                                                               Localization.getLocalText("LBL_total_msg_rcv"),
                                                               Localization.getLocalText("LBL_total_msg_sent")};

    /**
     * Labels for persistence table.
     */
    private static final String[] PERSISTENCE_LABELS = new String[] {Localization.getLocalText("LBL_service_name"),
                                                                     Localization.getLocalText("LBL_persistence_mode"),
                                                                     Localization
                                                                         .getLocalText("LBL_active_space_bytes"),
                                                                     Localization.getLocalText("LBL_active_space_mb"),
                                                                     Localization.getLocalText("LBL_avge_persistence"),
                                                                     Localization.getLocalText("LBL_max_persistence"),
                                                                     Localization.getLocalText("LBL_snapshot_count")};

    /**
     * Labels for persistence table.
     */
    private static final String[] HTTP_SESSION_LABELS = new String[] {Localization.getLocalText("LBL_application_id"),
                                                                      Localization.getLocalText("LBL_platform"),
                                                                      Localization.getLocalText("LBL_session_timeout"),
                                                                      Localization.getLocalText("LBL_session_count"),
                                                                      Localization.getLocalText("LBL_overflow_count"),
                                                                      Localization
                                                                          .getLocalText("LBL_avge_session_size"),
                                                                      Localization
                                                                          .getLocalText("LBL_total_reaped_sessions"),
                                                                      Localization
                                                                          .getLocalText("LBL_avge_reaped_sessions"),
                                                                      Localization
                                                                          .getLocalText("LBL_avge_reap_duration"),
                                                                      Localization.getLocalText("LBL_last_reap_max"),
                                                                      Localization.getLocalText("LBL_session_updates")};

    /**
     * Default refresh time of 30 seconds.
     */
    private static final long DEFAULT_REFRESH_TIME = 30 * 1000L;

    /**
     * The logger object to use.
     */
    private static final Logger LOGGER = Logger.getLogger(VisualVMModel.class.getName());

    /**
     * The time between refresh of JMX data. Defaults to DEFAULT_REFRESH_TIME.
     */
    private long nRefreshTime;

    /**
     * Last time statistics were updated.
     */
    private long ldtLastUpdate = -1L;

    /**
     * A {@link Map} of {@link List}s to store the retrieved data
     */
    private HashMap<DataType, List<Entry<Object, Data>>> mapCollectedData;

    /**
     * The selected service for detailed service data.
     */
    private String sSelectedService = null;

    /**
     * The selected cache for detailed cache data.
     */
    private Pair<String, String> selectedCache = null;

    /**
     * Defines if we can get statistics directly from reporter. This is only valid for
     * a coherence version >= 12.1.3. An initial null value indicates that we have
     * not yet determined if we can use the reporter.
     */
    private Boolean fReporterAvailable = null;

    /**
     * Defines if we are running Coherence 12.1.3 or above
     */
    private boolean fis1213AndAbove = true;

    /**
     * Defines if we want to include the NameService in the list of proxy servers.
     */
    private boolean fIncludeNameService = false;

    /**
     * Defines is proxy servers were present when we first collected stats.
     */
    private Boolean fIsCoherenceExtendConfigured = null;

    /**
     * Map of instances of data retrievers for execution of actual JMX queries.
     */
    private Map<Class, DataRetriever> mapDataRetrievers = new HashMap<Class, DataRetriever>();

    /**
     * The set of distributed caches so that we don't double count replicated
     * or optimistic caches.
     */
    private Set<String> setKnownDistributedCaches;


    /**
     * Returns an instance of the VisualVMModel.
     *
     * @return an instance of the initialized VisualVMModel
     */
    public static VisualVMModel getInstance()
    {
        VisualVMModel model = new VisualVMModel();

        model.init();

        return model;
    }


    /**
     * Initialize anything for this instance of the model.
     */
    private void init()
    {
        nRefreshTime = DEFAULT_REFRESH_TIME;

        String sRefreshTime      = System.getProperty("com.oracle.coherence.jvisualvm.refreshtime");
        String sReporterDisabled = System.getProperty("com.oracle.coherence.jvisualvm.reporter.disabled");

        if (sRefreshTime != null)
        {
            nRefreshTime = Long.parseLong(sRefreshTime) * 1000L;
        }

        // if this option is set we are specifically disabling the reporter even if Coherence
        // version >= 12.1.3
        if (sReporterDisabled != null && "true".equalsIgnoreCase(sReporterDisabled))
        {
            setReporterAvailable(false);
        }

        // force update on first time
        ldtLastUpdate = System.currentTimeMillis() - nRefreshTime - 1L;

        // populate mapCollectedData which contains an etry for each type
        mapCollectedData = new HashMap<DataType, List<Entry<Object, Data>>>();

        for (DataType type : DataType.values())
        {
            mapCollectedData.put(type, null);
        }

        // intialize the data retrievers map
        mapDataRetrievers.put(CacheData.class, new CacheData());
        mapDataRetrievers.put(ClusterData.class, new ClusterData());
        mapDataRetrievers.put(MemberData.class, new MemberData());
        mapDataRetrievers.put(ServiceData.class, new ServiceData());
        mapDataRetrievers.put(ServiceMemberData.class, new ServiceMemberData());
        mapDataRetrievers.put(ProxyData.class, new ProxyData());
        mapDataRetrievers.put(MachineData.class, new MachineData());
        mapDataRetrievers.put(CacheDetailData.class, new CacheDetailData());
        mapDataRetrievers.put(PersistenceData.class, new PersistenceData());
        mapDataRetrievers.put(CacheStorageManagerData.class, new CacheStorageManagerData());
        mapDataRetrievers.put(HttpSessionData.class, new HttpSessionData());
    }


    /**
     * Refresh the statistics from the given {@link MBeanServerConnection}
     * connection. This method will only refresh data if > REFRESH_TIME
     * has passed since last refresh.
     *
     * @param server the JMX connection to use
     */
    public void refreshJMXStatistics(MBeanServerConnection server)
    {
        if (System.currentTimeMillis() - ldtLastUpdate >= nRefreshTime)
        {
            // its important that the CACHE data is refreshed first and
            // as such we are relying on the order of types in the enum.
            for (DataType type : DataType.values())
            {
                mapCollectedData.put(type, getData(server, type.getClassName()));
            }

            ldtLastUpdate = System.currentTimeMillis();
        }
    }


    /**
     * This is a wrapper method which will call the underlying implementation
     * to get statistics. If statistics directly from the reporter are available
     * then run the particular report, otherwise do a JMX query.
     *
     * @param server  the {@link MBeanServerConnection} to use to query the report
     * @param clazz   the implementation of {@link DataRetriever} to get data for
     *
     * @return the {@link List} of data obtainer by either method
     */
    public List<Entry<Object, Data>> getData(MBeanServerConnection server,
                                             Class                 clazz)
    {
        boolean fFallBack = false;

        if (isReporterAvailable() != null && isReporterAvailable())
        {
            // retrieve the report for this class
            String sReport = getDataRetriverInstance(clazz).getReporterReport();

            if (sReport == null)
            {
                // this means there is no report for this class
                fFallBack = true;
            }

            if (!fFallBack)
            {
                try
                {
                    return getData(sReport, server, clazz);
                }
                catch (Exception e)
                {
                    // we received an error running the report, so mark as
                    // a fall back so it will be immeidatley run
                    fFallBack = true;
                }
            }
        }

        // this code path is for the following scenarios:
        // 1. If we need to fall-back as the reporter is being used but no report yet available
        // 2. The reporter is not available
        // 3. We have not yet decided is the reporter is available
        if (fFallBack || isReporterAvailable() == null ||!isReporterAvailable())
        {
            try
            {
                // get data the old fashioned way via JMX queries
                // ClusterData is a a special case as its used to determine if
                // we may be able to use the reporter
                if (clazz.equals(ClusterData.class))
                {
                    List<Entry<Object, Data>> clusterData = getDataRetriverInstance(clazz).getJMXData(server, this);

                    // if we have not yet evaluated if the reporter is available, e.g. value of null,
                    // then do it now
                    if (isReporterAvailable() == null)
                    {
                        // get the Coherence version. Easier to do if we are connected to a cluster,
                        // but we are have JMX connection as we have to look in data we collected.

                        if (clusterData != null)
                        {
                            for (Entry<Object, Data> entry : clusterData)
                            {
                                // there will only be one cluster entry
                                String sCoherenceVersion =
                                    entry.getValue().getColumn(ClusterData.VERSION).toString().replaceFirst(" .*$",
                                                                                                            "");
                                int nVersion = 0;

                                if (sCoherenceVersion.startsWith("3.5"))
                                {
                                    // manual check as version numbering changed after 35
                                    nVersion = 353;
                                }
                                else
                                {
                                    nVersion = new Integer(sCoherenceVersion.replaceAll("\\.", ""));
                                }

                                if (nVersion >= 121300)
                                {
                                    setReporterAvailable(true);
                                    fis1213AndAbove = true;
                                }
                                else
                                {
                                    setReporterAvailable(false);
                                    fis1213AndAbove = false;
                                }
                            }
                        }
                    }

                    return clusterData;
                }
                else
                {
                    return getDataRetriverInstance(clazz).getJMXData(server, this);
                }
            }
            catch (Exception e)
            {
                LOGGER.log(Level.WARNING, "Unable to get data: " + e.getMessage());
            }
        }

        return null;
    }


    /**
     * Returns a list of statistics for a given reporter report. Only applicable
     * if Coherence version >= 12.1.3.0.0.  The reports XML configuration files
     * are included in the generated plugin jar and must be added to all cache
     * servers classpath.
     *
     * If an error occurs in the running of queries, the fallback will be to
     * go back to the JMX query method. The usual reason for failure is the
     * reporter XML files not being available.
     *
     * @param sReport   the report to run
     * @param server    the {@link MBeanServerConnection} to use to query the report
     * @param clazz     the {@link Class} for the report
     *
     * @return the statistics for a given report.
     */
    private List<Entry<Object, Data>> getData(String                sReport,
                                              MBeanServerConnection server,
                                              Class                 clazz)
    {
        SortedMap<Object, Data> mapCollectedData = null;

        // get the local member id
        int nLocalMemberId = getLocalMemberId(server);

        if (nLocalMemberId != 0)
        {
            try
            {
                // run the given report
                TabularData reportData = (TabularData) server.invoke(new ObjectName("Coherence:type=Reporter,nodeId="
                                                                                    + nLocalMemberId),
                                                                     "runTabularReport",
                                                                     new Object[] {sReport},
                                                                     new String[] {"java.lang.String"});

                if (reportData != null)
                {
                    // now that we have output from the reporter, call the
                    // appropriate method in the class to populate
                    mapCollectedData = getDataRetriverInstance(clazz).getReporterData(reportData, this);
                }
            }
            catch (Exception e)
            {
                String sError = Localization.getLocalText("ERR_error_running_report",
                                                          new String[] {sReport, clazz.getCanonicalName(),
                                                                        e.getMessage()});

                LOGGER.log(Level.WARNING, sError);

                setReporterAvailable(false);

                // this exception is thrown so we can catch above and re-run the report
                // using the standard way
                throw new RuntimeException("Error running report");
            }
        }

        if (mapCollectedData != null)
        {
            return new ArrayList<Map.Entry<Object, Data>>(mapCollectedData.entrySet());
        }
        else
        {
            return null;
        }
    }


    /**
     * Retrieve the local member id from the Coherence Cluster MBean known as
     * Coherence:type=Cluster.
     *
     * @param server the {@link MBeanServerConnection} to use to query
     *
     * @return the local member id or 0 it no Coherence
     */
    private int getLocalMemberId(MBeanServerConnection server)
    {
        int memberId = 0;

        try
        {
            memberId = (Integer) JMXUtils.runJMXQuerySingleResult(server, "Coherence:type=Cluster",
                                                                  new JMXUtils.Attribute("LocalMemberId"));
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, Localization.getLocalText("ERR_local_member", new String[] {e.getMessage()}));
        }

        return memberId;
    }


    /**
     * Returns a unique list of addresses for the member data as we only want to
     * get information for each machine. We also store the machine and nodeId as
     * a key for querying individual nodes but will strip this later.
     *
     * @return the {@link SortedMap} of machines
     */
    public SortedMap<Pair<String, Integer>, Data> getInitialMachineMap()
    {
        SortedMap<Pair<String, Integer>, Data> initialMachineMap = new TreeMap<Pair<String, Integer>, Data>();

        // get a unique list of addresses for the member data as we only want to
        // get information for each machine. We also store the machine and nodeId as
        // a key but will strip this later

        if (mapCollectedData.get(DataType.MEMBER) != null)
        {
            for (Entry<Object, Data> entry : mapCollectedData.get(DataType.MEMBER))
            {
                Pair<String, Integer> key = new Pair<String,
                                                     Integer>((String) entry.getValue().getColumn(MemberData.ADDRESS),
                                                              (Integer) entry.getValue().getColumn(MemberData.NODE_ID));

                if (initialMachineMap.get(key) == null)
                {
                    initialMachineMap.put(key, (Data) null);
                }
            }
        }

        return initialMachineMap;
    }


    /**
     * Erase the current service member data as we have changed the
     * selected service.
     */
    public void eraseServiceMemberData()
    {
        mapCollectedData.put(DataType.SERVICE_DETAIL, null);
    }


    /**
     * Returns the currently selected service.
     *
     * @return the currently selected service
     */
    public String getSelectedService()
    {
        return sSelectedService;
    }


    /**
     * Sets the currently selected service.
     *
     * @param sService the currently selected service
     */
    public void setSelectedService(String sService)
    {
        sSelectedService = sService;
    }


    /**
     * Returns if the reporter is available for use.
     *
     * @return null if not yet evaluated or true/false if evaluated
     */
    public Boolean isReporterAvailable()
    {
        return fReporterAvailable;
    }


    /**
     * Return if we are running Coherence 12.1.3 or above.
     *
     * @return if we are running Coherence 12.1.3 or above
     */
    public boolean is1213AndAbove()
    {
        return fis1213AndAbove;
    }


    /**
     * Sets if the reporter is available.
     *
     * @param value  if the reporter is available
     */
    public void setReporterAvailable(Boolean value)
    {
        fReporterAvailable = value;
    }


    /**
     * Sets if we want to include the NameService in the list of
     * proxy servers.
     *
     * @param
     */
    public void setIncludeNameService(boolean fInclude)
    {
        fIncludeNameService = fInclude;
    }


    /**
     * Returns if we want to include the NameService in the list of
     * proxy servers.
     *
     * @return if we want to include the NameService
     */
    public boolean isIncludeNameService()
    {
        return fIncludeNameService;
    }


    /**
     * Sets the currently selected cache.
     *
     * @param selectedCache  the currently selected cache (service/cache name {@link Tuple}
     */
    public void setSelectedCache(Pair<String, String> selectedCache)
    {
        this.selectedCache = selectedCache;
    }


    /**
     * Returns the currently selected cache.
     *
     * @return the currently selected cache
     */
    public Pair<String, String> getSelectedCache()
    {
        return this.selectedCache;
    }


    /**
     * Returns if proxy servers are configured.
     *
     * @return true if proxy servers are configured.
     */
    public boolean isCoherenceExtendConfigured()
    {
        // if we have never set this flag, do it once only so that
        // the tab will always display and be updated
        if (fIsCoherenceExtendConfigured == null)
        {
            fIsCoherenceExtendConfigured = mapCollectedData.get(DataType.PROXY) != null
                                           && mapCollectedData.get(DataType.PROXY).size() != 0;
        }

        return fIsCoherenceExtendConfigured;
    }


    /**
     * Returns if Coherence*Web is configured.
     *
     * @return true if Coherence*Web is configured.
     */
    public boolean isCoherenceWebConfigured()
    {
        return mapCollectedData.get(DataType.HTTP_SESSION) != null
               && mapCollectedData.get(DataType.HTTP_SESSION).size() != 0;
    }


    /**
     * Returns the data for a given {@link DataType} enum.
     *
     * @param dataType the type of data to return
     */
    public List<Entry<Object, Data>> getData(DataType dataType)
    {
        return mapCollectedData.get(dataType);
    }


    /**
     * Returns an instance of the data retriever class for executing JMX calls on
     *
     * @param clazz the {@link Class} to get the instance for
     *
     * @return an instance of the data retriever class for executing JMX calls on
     */
    public DataRetriever getDataRetriverInstance(Class clazz)
    {
        DataRetriever retriever = mapDataRetrievers.get(clazz);

        if (retriever == null)
        {
            throw new IllegalArgumentException(Localization.getLocalText("ERR_instance",
                                                                         new String[] {clazz.getCanonicalName()}));
        }

        return retriever;
    }


    /**
     * Set the known distributed caches.
     *
     * @param  setCaches the {@link Set} of distributed caches.
     */
    public void setDistributedCaches(Set<String> setCaches)
    {
        this.setKnownDistributedCaches = setCaches;
    }


    /**
     * Returns the {@link Set} of distributed caches.
     *
     * @return the {@link Set} of distributed caches
     */
    public Set<String> getDistributedCaches()
    {
        return setKnownDistributedCaches;
    }
}
