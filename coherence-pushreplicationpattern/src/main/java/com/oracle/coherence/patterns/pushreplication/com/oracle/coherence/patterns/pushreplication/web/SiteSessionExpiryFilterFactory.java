package com.oracle.coherence.patterns.pushreplication.com.oracle.coherence.patterns.pushreplication.web;

import com.tangosol.coherence.servlet.SessionExpiryFilterFactory;
import com.tangosol.net.CacheFactory;
import com.tangosol.util.Filter;
import com.tangosol.util.filter.AndFilter;
import com.tangosol.util.filter.EqualsFilter;

/**
 * Implementation of {@link SessionExpiryFilterFactory} that creates a filter
 * to exclude sessions not managed by this site from session expiry/cleanup.
 *
 * @author pp  2010.04.29
 */
public class SiteSessionExpiryFilterFactory implements SessionExpiryFilterFactory
{

    /**
     * Creates a new session expiry filter that excludes sessions not managed
     * by this site.
     *
     * @param baseFilter  The "default" filter used to identify expired
     *                    sessions; this filter will be included in the new
     *                    filter.
     *
     * @return a filter that excludes sessions that are not managed by this site
     */
    public Filter createSessionExpiryFilter(Filter baseFilter)
    {
        String sSiteId = CacheFactory.getCluster().getLocalMember().getSiteName();
        Filter siteFilter = new EqualsFilter(new SiteExtractor(), sSiteId);

        return new AndFilter(baseFilter, siteFilter);
    }

}
