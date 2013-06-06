/*
 * File: SiteSessionExpiryFilterFactory.java
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
        String sSiteId    = CacheFactory.getCluster().getLocalMember().getSiteName();
        Filter siteFilter = new EqualsFilter(new SiteExtractor(), sSiteId);

        return new AndFilter(baseFilter, siteFilter);
    }
}
