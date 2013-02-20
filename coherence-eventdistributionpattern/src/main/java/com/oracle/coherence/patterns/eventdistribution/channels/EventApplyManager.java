package com.oracle.coherence.patterns.eventdistribution.channels;


import com.oracle.coherence.common.util.Average;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.management.Registry;
import com.tangosol.util.SafeHashMap;

import java.util.concurrent.atomic.AtomicLong;


/**
 * This class manages the entry apply on the target cluster.
 *
 * @author pfm  2013.02.19
 */
public class EventApplyManager
        implements EventApplyManagerMBean
    {
    /**
     * Return an instance of the EventApplyManager.
     *
     * @return the manager
     */
    public static EventApplyManager getInstance()
        {
        return getInstance("");
        }

    /**
     * Return an instance of the EventApplyManager.
     *
     * @param id  the name of the manager or null for the root manager
     *
     * @return the manager
     */
    public static EventApplyManager getInstance(String id)
        {
        id = id == null ? "" : id;

        EventApplyManager instance =  (EventApplyManager) s_map.get(id);

        if (instance == null)
            {
            instance = new EventApplyManager();

            Registry registry = CacheFactory.getCluster().getManagement();
            if (registry != null)
                {
                String sSuffix = id == "" ? "" : ",id=" + "ApplyToMember-" + id;

              //  String mBeanName = registry.ensureGlobalName("type=EventApplyManager" + sSuffix);

                String mBeanName = "type=EventApplyManager" + sSuffix;

                System.out.println("Registering MBean name " + mBeanName);
                registry.register(mBeanName, instance);
                }

            s_map.put(id, instance);
            }

        return instance;
        }

    /**
     * Update the event stats over time.
     *
     * @param cEntries  the number of entries.
     * @param ldtStart  the start timestamp
     * @param ldtEnd    the end timestamp
     */
    public void updateEventStats(int cEntries, long ldtStart, long ldtEnd)
        {
        m_averageEvents.addSample(cEntries, ldtStart, ldtEnd);

        m_totalEventCount.addAndGet(cEntries);

        m_totalBatchCount.addAndGet(1);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getAverageEventsAppliedPerSecondAcrossBatches()
        {
        return m_averageEvents.getElapsedAveragePerSecond();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getAverageEventsAppliedPerSecond()
        {
        return m_averageEvents.getDiscreetAveragePerSecond();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getAverageEventsPerBatch()
        {
        return m_averageEvents.getAverage();
        }

    /**
     * Return the total event count
     *
     * @return  the total event count.
     */
    public long getTotalEventCount()
        {
        return m_totalEventCount.get();
        }

    /**
     * Return the total batch count.
     *
     * @return  the total batch count.
     */
    public long getTotalBatchCount()
        {
        return m_totalBatchCount.get();
        }

    /**
     * The average events stat.
     */
    private volatile Average m_averageEvents = new Average(10);

    /**
     * The total event count.
     */
    private final AtomicLong m_totalEventCount = new AtomicLong();

    /**
     * The total batch count.
     */
    private final AtomicLong m_totalBatchCount = new AtomicLong();

    /**
     * The singleton class.
     */
    private static SafeHashMap s_map = new SafeHashMap();
    }