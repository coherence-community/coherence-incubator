package com.oracle.coherence.patterns.eventdistribution.channels;

/**
 * The MBean manages the applying of data at the target.
 *
 * @author pfm  2013.02.19
 */
public interface EventApplyManagerMBean
    {
    /**
     * Return the average number of events applied per second
     *
     * @return  the average number of events applied per second.
     */
    public long getAverageEventsAppliedPerSecondAcrossBatches();

    /**
     * Return the average millis to apply a single event.
     *
     * @return  the average millis to apply a single event.
     */
    public long getAverageEventsAppliedPerSecond();

    /**
     * Return the average number entries per batch.
     *
     * @return  the average number of entries per batch.
     */
    public long getAverageEventsPerBatch();

    /**
     * Return the total event count
     *
     * @return  the total event count.
     */
    public long getTotalEventCount();

    /**
     * Return the total batch count.
     *
     * @return  the total batch count.
     */
    public long getTotalBatchCount();
    }
