/*
 * File: InitParamProcessor.java
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

package com.oracle.coherence.patterns.eventdistribution.configuration;

import com.oracle.coherence.common.expression.SerializableExpressionHelper;
import com.oracle.coherence.common.expression.SerializableParameter;

import com.tangosol.coherence.config.CacheConfig;
import com.tangosol.coherence.config.CacheMapping;
import com.tangosol.coherence.config.CacheMappingRegistry;
import com.tangosol.coherence.config.ServiceSchemeRegistry;

import com.tangosol.coherence.config.builder.MapBuilder;

import com.tangosol.coherence.config.scheme.CachingScheme;
import com.tangosol.coherence.config.scheme.ClassScheme;
import com.tangosol.coherence.config.scheme.ExternalScheme;
import com.tangosol.coherence.config.scheme.FlashJournalScheme;
import com.tangosol.coherence.config.scheme.InvocationScheme;
import com.tangosol.coherence.config.scheme.LocalScheme;
import com.tangosol.coherence.config.scheme.ObservableCachingScheme;
import com.tangosol.coherence.config.scheme.OverflowScheme;
import com.tangosol.coherence.config.scheme.RamJournalScheme;
import com.tangosol.coherence.config.scheme.ReadWriteBackingMapScheme;
import com.tangosol.coherence.config.scheme.RemoteInvocationScheme;
import com.tangosol.coherence.config.scheme.ServiceScheme;

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.expression.Value;

import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;
import com.tangosol.util.UUID;

import java.util.Map;

/**
 * An {@link InitParamProcessor} is responsible for processing &lt;init-param&gt; {@link XmlElement}s to produce
 * {@link Parameter}s.
 *
 * @author Brian Oliver
 */
@XmlSimpleName("init-param")
public class InitParamProcessor implements ElementProcessor<SerializableParameter>
{
    /**
     * {@inheritDoc}
     */
    @Override
    public SerializableParameter process(ProcessingContext context,
                                         XmlElement        element) throws ConfigurationException
    {
        // when param-name is undefined, use a newly generated UUID
        String        sName = context.getOptionalProperty("param-name", String.class, new UUID().toString(), element);
        Expression<?> exprValue = context.getMandatoryProperty("param-value", Expression.class, element);
        String        sType     = context.getOptionalProperty("param-type", String.class, null, element);

        // handle the special case when the parameter type is a reference to another cache.
        Class<?> clzType = null;

        if (sType != null && sType.equals("{cache-ref}"))
        {
            Expression<?>        exprCacheName = exprValue;
            ClassLoader          loader        = context.getContextClassLoader();
            CacheMappingRegistry registry      = context.getCookie(CacheConfig.class).getCacheMappingRegistry();

            clzType   = NamedCache.class;
            exprValue = new CacheRefExpression(exprCacheName, loader, registry);
        }
        else if (sType != null && sType.equals("{scheme-ref}"))
        {
            Expression<?>         exprSchemeName = exprValue;
            ServiceSchemeRegistry registry       = context.getCookie(CacheConfig.class).getServiceSchemeRegistry();

            // we use Object.class here as we don't know the type of value the scheme-ref will produce.
            clzType   = Object.class;
            exprValue = new SchemeRefExpression(exprSchemeName, registry);
        }
        else
        {
            // attempt to load the specified class
            if (sType != null)
            {
                try
                {
                    clzType = context.getContextClassLoader().loadClass(sType);
                }
                catch (ClassNotFoundException e)
                {
                    throw new ConfigurationException(String
                        .format("Failed to resolve the specified param-type in [%s]", element),
                                                     "Please ensure that the specified class is publicly accessible via the class path",
                                                     e);
                }
            }
        }

        return new SerializableParameter(sName, clzType, SerializableExpressionHelper.ensureSerializable(exprValue));
    }


    /**
     * An {@link Expression} implementation that represents the use of
     * a {cache-ref} macro in a Cache Configuration File.
     */
    public static class CacheRefExpression implements Expression<NamedCache>
    {
        /**
         * The Expression that specifies the Cache Name to resolve.
         */
        private Expression<?> m_exprCacheName;

        /**
         * The ClassLoader to use to resolve/load the underlying Cache.
         */
        private ClassLoader m_classLoader;

        /**
         * The CacheMappingRegistry to use to lookup CacheMapping definitions.
         */
        private CacheMappingRegistry m_registryCacheMappings;


        /**
         * Constructs a CacheRefExpression.
         *
         * @param exprCacheName  the Expression representing the Cache Name
         * @param classLoader    the ClassLoader to use when loading the Cache
         * @param registry       the CacheMappingRegistry to locate CacheMapping
         *                       definitions
         */
        public CacheRefExpression(Expression<?>        exprCacheName,
                                  ClassLoader          classLoader,
                                  CacheMappingRegistry registry)
        {
            m_exprCacheName         = exprCacheName;
            m_classLoader           = classLoader;
            m_registryCacheMappings = registry;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public NamedCache evaluate(ParameterResolver resolver)
        {
            String sDesiredCacheName = new Value(m_exprCacheName.evaluate(resolver)).as(String.class);

            if (sDesiredCacheName.contains("*"))
            {
                // the desired cache name contains a wildcard * and as
                // such we need to replace that with the string that
                // matched the wildcard * in the parameterized cache-name

                // the parameter resolver will have the cache-name
                Parameter paramCacheName = resolver.resolve("cache-name");
                String sCacheName = paramCacheName == null ? null : paramCacheName.evaluate(resolver).as(String.class);

                if (sCacheName == null)
                {
                    // as we can't resolve the parameterized cache-name
                    // we'll just use the desired name as is.
                }
                else
                {
                    CacheMapping mapping = m_registryCacheMappings.findCacheMapping(sCacheName);

                    if (mapping.usesWildcard())
                    {
                        String sWildcardValue = mapping.getWildcardMatch(sCacheName);

                        sDesiredCacheName = sDesiredCacheName.replaceAll("\\*", sWildcardValue);
                    }
                    else
                    {
                        // as the parameterized cache-name doesn't use a wildcard
                        // we'll just use the desired name as is.
                    }
                }
            }

            return CacheFactory.getCache(sDesiredCacheName, m_classLoader);
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
        {
            return String.format("CacheRefExpression{exprCacheName=%s}", m_exprCacheName);
        }
    }


    /**
     * An {@link Expression} implementation that represents the use of
     * a {scheme-ref} macro in a Cache Configuration File.
     */
    public static class SchemeRefExpression implements Expression<Object>
    {
        /**
         * The Expression that specifies the Scheme Name to resolve.
         */
        private Expression<?> m_exprSchemeName;

        /**
         * The ServiceSchemeRegistry to use to lookup ServiceSchemes.
         */
        private ServiceSchemeRegistry m_registry;


        // ----- constructors ----------------------------------------------------

        /**
         * Constructs a SchemeRefExpression.
         *
         * @param exprSchemeName  the name of the ServiceScheme to resolve
         * @param registry        the ServiceSchemeRegistry to lookup ServiceSchemes
         */
        public SchemeRefExpression(Expression<?>         exprSchemeName,
                                   ServiceSchemeRegistry registry)
        {
            m_exprSchemeName = exprSchemeName;
            m_registry       = registry;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public Object evaluate(ParameterResolver resolver)
        {
            String        sSchemeName = new Value(m_exprSchemeName.evaluate(resolver)).as(String.class);
            ServiceScheme scheme      = m_registry.findSchemeBySchemeName(sSchemeName);

            // the parameter resolver will have the classloader
            Parameter paramClassLoader = resolver.resolve("class-loader");
            ClassLoader classLoader = paramClassLoader == null
                                      ? Base.getContextClassLoader()
                                      : paramClassLoader.evaluate(resolver).as(ClassLoader.class);

            if (scheme instanceof InvocationScheme || scheme instanceof RemoteInvocationScheme)
            {
                return ((InvocationScheme) scheme).realizeService(resolver, classLoader, CacheFactory.getCluster());
            }
            else if (scheme instanceof ClassScheme)
            {
                return ((ClassScheme) scheme).realize(resolver, classLoader, null);
            }
            else if (scheme instanceof CachingScheme)
            {
                CachingScheme cachingScheme = (CachingScheme) scheme;

                // construct MapBuilder.Dependencies so that we can realize the caching scheme/backing map
                ConfigurableCacheFactory ccf = CacheFactory.getConfigurableCacheFactory();

                // the parameter resolver will have the cache-name
                Parameter paramCacheName = resolver.resolve("cache-name");
                String sCacheName = paramCacheName == null ? null : paramCacheName.evaluate(resolver).as(String.class);

                // the parameter resolver will have the manager-context
                Parameter paramBMMC = resolver.resolve("manager-context");
                BackingMapManagerContext bmmc = paramBMMC == null
                                                ? null
                                                : paramBMMC.evaluate(resolver).as(BackingMapManagerContext.class);

                MapBuilder.Dependencies dependencies = new MapBuilder.Dependencies(ccf,
                                                                                   bmmc,
                                                                                   classLoader,
                                                                                   sCacheName,
                                                                                   cachingScheme.getServiceType());

                // the resulting map/cache
                Map map;

                if (scheme instanceof LocalScheme || scheme instanceof OverflowScheme
                    || scheme instanceof ExternalScheme || scheme instanceof ReadWriteBackingMapScheme
                    || scheme instanceof FlashJournalScheme || scheme instanceof RamJournalScheme)
                {
                    // for "local" schemes we realize a backing map
                    map = cachingScheme.realizeMap(resolver, dependencies);
                }
                else
                {
                    // for anything else, we realize the cache
                    // NOTE: this cache won't be registered or visible
                    // via ensureCache/getCache
                    map = cachingScheme.realizeCache(resolver, dependencies);
                }

                // add the listeners when the scheme is observable
                if (cachingScheme instanceof ObservableCachingScheme)
                {
                    ObservableCachingScheme observableScheme = (ObservableCachingScheme) cachingScheme;

                    observableScheme.establishMapListeners(map, resolver, dependencies);
                }

                return map;
            }
            else
            {
                throw new ConfigurationException(String.format("Failed to resolve the {scheme-ref} name [%s]",
                                                               sSchemeName),
                                                 "Please ensure that the specified {scheme-ref} refers to a defined <scheme-name>");
            }
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
        {
            return String.format("SchemeRefExpression{exprSchemeName=%s}", m_exprSchemeName);
        }
    }
}
