/*
 * File: Average.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting
 * or https://oss.oracle.com/licenses/CDDL
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

package com.oracle.coherence.common.util;

/**
 * This class tracks the average of a set of data with a specified sampling size.
 * The samples can be tracked over time to return and items/second average.
 *
 * @author pfm  2013.02.19
 */
public class Average
    {
    /**
     * Construct an Average object.
     *
     * @param nSampleSize  the number of samples to save for the average.  These
     * samples are stored in a circular buffer and the oldest ones are overwritten
     * as needed.
     */
    public Average(int nSampleSize)
        {
        m_nSampleSize = nSampleSize;
        m_aSamples    = new Sample[nSampleSize];
        }

    /**
     * Return the average item count per second elapsed time based all the samples added. The
     * average uses the elapsed time from the oldest sample to the newest sample.  For example
     * if the first sample started at 1:10 and the last sample ended at 1:20 then the time would be
     * 10 minutes.This is different from calculating the duration millis of each sample then
     * averaging that value.
     *
     * @return the average value per second
     */
    public synchronized long getElapsedAveragePerSecond()
        {
        long cTotalItems    = 0;
        long ldtOldestStart = Long.MAX_VALUE;
        long ldtNewestEnd   = 0 ;

        for (int i=0; i < getNumSamplesInArray(); i++)
            {
            Sample sample = m_aSamples[i];
            cTotalItems += sample.getVal();

            long ldtStart  = sample.getStart();
            ldtOldestStart = ldtStart < ldtOldestStart ? ldtStart : ldtOldestStart;

            long ldtEnd    = sample.getEnd();
            ldtNewestEnd   = ldtEnd > ldtNewestEnd ? ldtEnd : ldtNewestEnd;
            }

        // return items per second for max duration of the sample period
        long cMillis = (ldtNewestEnd - ldtOldestStart);

        return cTotalItems * 1000 / (cMillis == 0 ? 1 : cMillis);
        }

    /**
     * Return the average item count per second for each individual item.
     *
     * @return the average value per second
     */
    public synchronized long getDiscreetAveragePerSecond()
        {
        long cItemsPerSec   = 0;
        long cSamples = getNumSamplesInArray();

        for (int i=0; i < cSamples; i++)
            {
            Sample sample  = m_aSamples[i];
            long   cMillis = sample.getDurationMillis();

            cItemsPerSec += sample.getVal() * 1000 / (cMillis == 0 ? 1 : cMillis);
            }

        return cSamples == 0 ? 0 : cItemsPerSec / cSamples;
        }

    /**
     * Return the average item count based all the samples added.
     *
     * @return the average value
     */
    public synchronized long getAverage()
        {
        long cItems   = 0;
        long cSamples = getNumSamplesInArray();

        for (int i=0; i < cSamples; i++)
            {
            Sample sample = m_aSamples[i];
            cItems += sample.getVal();
            }

        return cSamples == 0 ? 0 : cItems / cSamples;
        }

    /**
     * Return the minimum sample value.
     *
     * @return the minimum value.
     */
    public long getMin()
        {
        long nMin = Long.MAX_VALUE;
        for (int i=0; i < getNumSamplesInArray(); i++)
            {
            long nVal = m_aSamples[i].getVal();
            nMin = nVal < nMin ? nVal : nMin;
            }

        return nMin == Long.MAX_VALUE ? 0 : nMin;
        }

    /**
     * Return the maximum sample value.
     *
     * @return the maximum value.
     */
    public long getMax()
        {
        long nMax = 0;
        for (int i=0; i < getNumSamplesInArray(); i++)
            {
            long nVal = m_aSamples[i].getVal();
            nMax = nVal > nMax ? nVal : nMax;
            }

        return nMax;
        }

    /**
     * Add an item count to the sample array.  If the end of the array is
     * reached then new entries will be written to the start of the array.
     *
     * @param cItems     the item count
     */
    public synchronized void addSample(long cItems)
        {
        addSample(cItems, 0, 0);
        }

    /**
     * Add an item count and elapsed time to the sample array.  If the
     * end of the array is reached then new entries will be written to the
     * start of the array.
     *
     * @param cItems    the item count
     * @param ldtStart  the starting timestamp of sample period
     * @param ldtEnd    the ending timestamp of the sample period.
     */
    public synchronized void addSample(long cItems, long ldtStart, long ldtEnd)
        {
        int iNext = m_iNext;
        m_aSamples[iNext] = new Sample(cItems, ldtStart, ldtEnd);

        m_iNext = iNext == (m_nSampleSize -1) ? 0 : iNext + 1;
        m_cSamples++;
        }

    /**
     * Return the actual number of samples in the array.
     *
     * @return the number of samples in the array
     */
    protected int getNumSamplesInArray()
        {
        int cSamples   = m_cSamples;
        int cArraySize = m_aSamples.length;

        return cSamples > cArraySize ? cArraySize : cSamples;
        }

    /**
     * The Sample class contains an item count and elapse time.
     */
    protected class Sample
        {
        /**
         * Construct a Sample object.
         *
         * @param cItems    the item count
         * @param ldtStart  the starting timestamp of sample period
         * @param ldtEnd    the ending timestamp of the sample period.
         */
        public Sample(long cItems, long ldtStart, long ldtEnd)
            {
            m_cItems   = cItems;
            m_ldtStart = ldtStart;
            m_ldtEnd   = ldtEnd;
            }

        /**
         * Return the value.
         *
         * @return the value.
         */
        public long getVal()
            {
            return m_cItems;
            }

        /**
         * Return the sample time starting time.
         *
         * @return the starting time.
         */
        public long getStart()
            {
            return m_ldtStart;
            }

        /**
         * Return the sample time ending time.
         *
         * @return the starting time.
         */
        public long getEnd()
            {
            return m_ldtEnd;
            }

        /**
         * Return the duration of the sample in milliseconds.
         *
         * @return the duration in milliseconds
         */
        public long getDurationMillis()
            {
            return m_ldtEnd - m_ldtStart;
            }

        // ----- data members -----------------------------------------------

        /**
         * The item count.
         */
        private long m_cItems;

        /**
         * The sample timestamp start.
         */
        private long m_ldtStart;

        /**
         * The sample timestamp end.
         */
        private long m_ldtEnd;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The number of samples to average.
     */
    private final int m_nSampleSize;

    /**
     * The number of samples taken.
     */
    private int m_cSamples;

    /**
     * The next slot in the array.
     */
    private int m_iNext;

    /**
     * The array to hold samples.
     */
    private final Sample[] m_aSamples;
    }
