package com.oracle.coherence.patterns.pushreplication.com.oracle.coherence.patterns.pushreplication.web;

import com.oracle.coherence.patterns.eventdistribution.events.DistributableEntry;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.extractor.EntryExtractor;

import java.util.Map;

/**
* Extracts site information from a {@link com.tangosol.util.Binary} entry value decorated
* by {@link com.oracle.coherence.patterns.pushreplication.PublishingCacheStore}.
*
* @author pp  2010.04.04
*/
@SuppressWarnings( { "serial" })
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
    * Extracts site information from {@link com.tangosol.util.Binary} entry value decorator.
    *
    * @return  site information extracted from decorator if it exists;
    *          otherwise return the Cluster's site name.
    */
    @SuppressWarnings("rawtypes")
    public Object extractFromEntry(Map.Entry entry)
    {
        BackingMapManagerContext ctx = ((BinaryEntry) entry).getContext();
        String sSite = ctx.getCacheService().getCluster().getLocalMember().getSiteName();
        Binary binSession = ((BinaryEntry) entry).getBinaryValue();
        Map mapDeco = (Map) ctx.getInternalValueDecoration(binSession, BackingMapManagerContext.DECO_CUSTOM);

        String sDecoSite = null;
        if (mapDeco != null)
        {
            sDecoSite = (String) mapDeco.get(DistributableEntry.CLUSTER_META_INFO_DECORATION_KEY);
        }

        // If the entry isn't decorated with a site, return the site this member
        // is running on
        return sDecoSite == null ? sSite : sDecoSite;
    }
}