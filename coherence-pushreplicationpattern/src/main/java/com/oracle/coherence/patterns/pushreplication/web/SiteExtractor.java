/*
 * File: SiteExtractor.java
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

package com.oracle.coherence.patterns.pushreplication.web;

import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntry;
import com.oracle.coherence.patterns.pushreplication.PublishingCacheStore;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.extractor.EntryExtractor;

import java.util.Map;

/**
 * Extracts site information from a {@link Binary} entry value decorated
 * by {@link PublishingCacheStore}.
 *
 * @author pp  2010.04.04
 */
@SuppressWarnings({"serial"})
public class SiteExtractor extends EntryExtractor
{
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public SiteExtractor()
    {
    }


    // ----- EntryExtractor -------------------------------------------------

    /**
     * Extracts site information from {@link Binary} entry value decorator.
     *
     * @return  site information extracted from decorator if it exists;
     *          otherwise return the Cluster's site name.
     */
    @SuppressWarnings("rawtypes")
    public Object extractFromEntry(Map.Entry entry)
    {
        BackingMapManagerContext ctx        = ((BinaryEntry) entry).getContext();
        String                   sSite      = ctx.getCacheService().getCluster().getLocalMember().getSiteName();
        Binary                   binSession = ((BinaryEntry) entry).getBinaryValue();
        Map mapDeco = (Map) ctx.getInternalValueDecoration(binSession, BackingMapManagerContext.DECO_CUSTOM);

        String                   sDecoSite  = null;

        if (mapDeco != null)
        {
            sDecoSite = (String) mapDeco.get(DistributableEntry.CLUSTER_META_INFO_DECORATION_KEY);
        }

        // TODO: this will be a bit verbose; should remove once testing is complete
        CacheFactory.log("Extracted site : " + sDecoSite + "; running on site " + sSite, 7);

        // If the entry isn't decorated with a site, return the site this member
        // is running on
        return sDecoSite == null ? sSite : sDecoSite;
    }
}
