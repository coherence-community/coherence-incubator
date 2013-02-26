/*
 * File: ExtensibleEnvironment.java
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

package com.oracle.coherence.environment.extensible;

import com.oracle.coherence.common.builders.BuilderRegistry;
import com.oracle.coherence.common.builders.NoArgsBuilder;
import com.oracle.coherence.common.builders.ParameterizedBuilder;
import com.oracle.coherence.common.events.dispatching.EventDispatcher;
import com.oracle.coherence.common.events.dispatching.SimpleEventDispatcher;
import com.oracle.coherence.common.events.lifecycle.LifecycleStartedEvent;
import com.oracle.coherence.common.events.lifecycle.LifecycleStoppedEvent;
import com.oracle.coherence.common.events.lifecycle.NamedCacheStorageRealizedEvent;
import com.oracle.coherence.common.events.lifecycle.NamedCacheStorageReleasedEvent;
import com.oracle.coherence.common.logging.CoherenceLogHandler;
import com.oracle.coherence.common.logging.LogHelper;
import com.oracle.coherence.common.threading.ExecutorServiceFactory;
import com.oracle.coherence.common.threading.ThreadFactories;
import com.oracle.coherence.configuration.caching.CacheMapping;
import com.oracle.coherence.configuration.caching.CacheMappingRegistry;
import com.oracle.coherence.configuration.parameters.MutableParameterProvider;
import com.oracle.coherence.configuration.parameters.Parameter;
import com.oracle.coherence.configuration.parameters.ScopedParameterProvider;
import com.oracle.coherence.configuration.parameters.SystemPropertyParameterProvider;
import com.oracle.coherence.environment.Environment;
import com.oracle.coherence.environment.extensible.dependencies.DependencyTracker;
import com.oracle.coherence.environment.extensible.dependencies.DependentResource;
import com.oracle.coherence.environment.extensible.namespaces.CoherenceNamespaceContentHandler;
import com.tangosol.io.ClassLoaderAware;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.DefaultConfigurableCacheFactory;
import com.tangosol.net.Service;
import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.util.ServiceEvent;
import com.tangosol.util.ServiceListener;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An {@link ExtensibleEnvironment} is a {@link com.tangosol.net.ConfigurableCacheFactory} that provides;
 * <p>
 * <ol>
 *  <li>An implementation of an {@link Environment} to manage resources, and</li>
 *  <li>Support for user-defined custom namespaced xml elements with in a Coherence Cache Configuration file
 *      together with associated {@link NamespaceContentHandler}s to process said xml elements.</li>
 * </ol>
 * <p>
 * The following namespaces are implicitly defined for all Coherence Cache Configuration files.
 * {@link CoherenceNamespaceContentHandler} is the default namespace handler.
 * <p>
 * To use an {@link ExtensibleEnvironment} in your application, you must set the
 * "&lt;configurable-cache-factory-config&gt;" optional
 * override in your tangosol-coherence-override.xml file.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class ExtensibleEnvironment extends DefaultConfigurableCacheFactory implements Environment
{
    /**
     * The {@link Logger} for this class.
     */
    private static final Logger logger = Logger.getLogger(ExtensibleEnvironment.class.getName());

    /**
     * The {@link ConfigurationContext} being used by the thread loading the configuration.
     *
     * This is so that we can detect any re-entrancy when loading a configuration, and provide
     * precise information on the cause and location of the re-entrant call.
     */
    private static ThreadLocal<ConfigurationContext> configurationContext = new ThreadLocal<ConfigurationContext>();

    /**
     * The set of resources that have been registered with the environment, organized by {@link Class} and name
     */
    private ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Object>> resourcesByClass;

    /**
     * The map of named Coherence {@link Service}s, keyed by service name, that have been "ensured" by using the
     * {@link ExtensibleEnvironment}.
     *
     * We use this to keep track of {@link Service} life-cycles based on calls by
     * the {@link com.tangosol.net.DefaultCacheServer} to {@link #ensureService(XmlElement)}.
     */
    private ConcurrentHashMap<String, Service> trackedServices;

    /**
     * The set of {@link com.tangosol.net.NamedCache} names that have been realized by this {@link ExtensibleEnvironment}.
     *
     * We use this to keep track of the {@link com.oracle.coherence.common.events.lifecycle.NamedCacheLifecycleEvent}s to raise.
     */
    private HashSet<String> trackedNamedCaches;


    /**
     * Standard Constructor.
     */
    public ExtensibleEnvironment()
    {
        super();
    }


    /**
     * Standard Constructor with a specified configuration path.
     *
     * @param path The path to the configuration file.
     */
    public ExtensibleEnvironment(String path)
    {
        super(path);
    }


    /**
     * Standard Constructor with a root {@link XmlElement} document
     * as configuration.
     *
     * @param xmlConfig The {@link XmlElement} as the root of a configuration.
     */
    public ExtensibleEnvironment(XmlElement xmlConfig)
    {
        super(xmlConfig);
    }


    /**
     * Standard Constructor with a specified configuration path and {@link ClassLoader}.
     *
     * @param path The path to the configuration file.
     * @param loader The {@link ClassLoader} to use to load resources.
     */
    public ExtensibleEnvironment(String      path,
                                 ClassLoader loader)
    {
        super(path, loader);
    }


    /**
     * {@inheritDoc}
     */
    public <R> R getResource(Class<R> clazz)
    {
        return getResource(clazz, "");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public <R> R registerResource(Class<R> clazz,
                                  Object   resource)
    {
        return registerResource(clazz, "", resource);
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <R> R getResource(Class<R> clazz,
                             String   name)
    {
        if (resourcesByClass.containsKey(clazz))
        {
            return (R) resourcesByClass.get(clazz).get(name);
        }
        else
        {
            return null;
        }
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <R> Map<String, R> getResources(Class<R> clazz)
    {
        Map<String, Object> resourcesByName = resourcesByClass.get(clazz);

        return resourcesByName == null ? Collections.EMPTY_MAP : resourcesByName;
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public synchronized <R> R registerResource(Class<R>         clazz,
                                               String           name,
                                               NoArgsBuilder<R> builder)
    {
        if (name == null)
        {
            throw new IllegalArgumentException(String
                .format("Attempted to registerResource(%s, null) with a null name.  null names are not supported",
                        clazz));
        }
        else
        {
            ConcurrentHashMap<String, Object> resourcesByName;

            resourcesByName = resourcesByClass.get(clazz);

            if (resourcesByName == null)
            {
                resourcesByName = new ConcurrentHashMap<String, Object>();
                resourcesByClass.put(clazz, resourcesByName);
            }

            R resource = (R) resourcesByName.get(name);

            if (resource == null)
            {
                // create and register the resource
                resource = builder.realize();
                resourcesByName.put(name, resource);

                // ensure the resource has a dependency tracker (if it has dependencies)
                if (resource instanceof DependentResource)
                {
                    getResource(EventDispatcher.class).registerEventProcessor(LifecycleEventFilter.INSTANCE,
                                                                              new DependencyTracker(this,
                                                                                                    (DependentResource) resource));
                }
            }
            else
            {
                logger.warning(String
                    .format("Environment resource [%s] of type [%s] is already registered as [%s].  Skipping requested registration.",
                            name, clazz, resource));

            }

            return resource;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public <R> R registerResource(Class<R>     clazz,
                                  String       name,
                                  final Object resource)
    {
        return registerResource(clazz, name, new NoArgsBuilder<R>()
        {
            @Override
            @SuppressWarnings("unchecked")
            public R realize()
            {
                return (R) resource;
            }
        });
    }


    /**
     * {@inheritDoc}
     */
    public ClassLoader getClassLoader()
    {
        return getConfigClassLoader();
    }


    /**
     * An internal method to setup the {@link Environment}.
     */
    private void startup()
    {
        // let everyone know that they are using the Extensible Environment for Coherence cache configuration
        CacheFactory.log("", 0);
        CacheFactory.log("Using the Incubator Extensible Environment for Coherence Cache Configuration", 0);
        CacheFactory.log("Copyright (c) 2013, Oracle Corporation. All Rights Reserved.", 0);
        CacheFactory.log("", 0);

        // create the new map of resources
        this.resourcesByClass = new ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Object>>();

        // create the new map of services we're going to track
        this.trackedServices = new ConcurrentHashMap<String, Service>();

        // create the new map of named caches we're going to track
        this.trackedNamedCaches = new HashSet<String>();

        // register a SchemeRegistry resource
        // (we'll use this to track Schemes)
        registerResource(BuilderRegistry.class, new BuilderRegistry());

        // register a event dispatcher as a standard resource
        // (we'll use this to dispatch events in a variety of places)
        registerResource(EventDispatcher.class, new SimpleEventDispatcher(this));

        // TODO: replace the following with the initialization of the ExecutionServiceManager
        // (the following is just temporary until we have an ExecutionServiceManager)
        registerResource(ExecutorService.class,
                         ExecutorServiceFactory
                             .newSingleThreadExecutor(ThreadFactories
                                 .newThreadFactory(true, "Environment.Background.Executor", null)));
    }


    /**
     * An internal method to gracefully shutdown an {@link Environment}.
     */
    private void shutdown()
    {
        if (logger.isLoggable(Level.FINEST))
        {
            LogHelper.entering(logger, this.getClass().getName(), "shutdown");
        }

        // we're going to need the event dispatcher to send a bunch of
        // end-of-life life-cycle events
        EventDispatcher eventDispatcher = getResource(EventDispatcher.class);

        // send life-cycle events to release all of the named caches
        for (String cacheName : trackedNamedCaches)
        {
            eventDispatcher.dispatchEvent(new NamedCacheStorageReleasedEvent(cacheName));
        }

        // send life-cycle stopped events for all of the currently tracked services
        for (Service service : trackedServices.values())
        {
            eventDispatcher.dispatchEvent(new LifecycleStoppedEvent<Service>(service));
        }

        // let everyone know that the current environment has stopped
        eventDispatcher.dispatchEvent(new LifecycleStoppedEvent<Environment>(this));

        // TODO: replace the following with the shutdown of the ExecutionServiceManager
        // (the following is just temporary until we have an ExecutionServiceManager)
        getResource(ExecutorService.class).shutdown();

        if (logger.isLoggable(Level.FINEST))
        {
            LogHelper.exiting(logger, this.getClass().getName(), "shutdown");
        }
    }


    /**
     * Creates a String representation of the current stack trace of the specified {@link Thread}.
     *
     * @param thread The thread from which we shall create a stack trace.
     *
     * @return A string
     */
    private String fullStackTraceFor(Thread thread)
    {
        int           dropStackTraceElements = 2;
        StringBuilder result                 = new StringBuilder();

        result.append("Stack Trace\n");

        for (StackTraceElement stackTraceElement : thread.getStackTrace())
        {
            if (dropStackTraceElements == 0)
            {
                result.append(stackTraceElement);
                result.append("\n");
            }
            else
            {
                dropStackTraceElements--;
            }
        }

        return result.toString();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setConfig(XmlElement xmlConfig)
    {
        // detect if we're making a re-entrant attempt to load a configuration.  re-entrancy is not allowed.
        ConfigurationContext context;

        if (configurationContext.get() == null)
        {
            // construct the configuration context we're going to use to build our configuration
            context = new DefaultConfigurationContext(this);
            configurationContext.set(context);
        }
        else
        {
            String message =
                "An attempt to recursively load and process a Coherence Cache Configuration has occurred. "
                + "This is usually caused by accessing a NamedCache or Service (through the CacheFactory) "
                + "from a NamespaceContentHandler, " + "ElementContentHandler and/or AttributeContentHandler.";

            logger.severe(message);
            logger.severe(fullStackTraceFor(Thread.currentThread()));
            logger.severe(configurationContext.get().toString());

            throw new RuntimeException(message);
        }

        CoherenceLogHandler.initializeIncubatorLogging();

        if (logger.isLoggable(Level.FINEST))
        {
            LogHelper.entering(logger, this.getClass().getName(), "setConfig", xmlConfig);
        }

        // are we changing the configuration? if so we need to shutdown the previous one!
        if (this.resourcesByClass != null)
        {
            if (logger.isLoggable(Level.INFO))
            {
                logger.info("Extensible Environment XML configuration has been reset.  Will now restart it.");
            }

            shutdown();
        }

        // setup and initialize the environment
        startup();

        // automatically register the default namespace handler for coherence and the "introduce:" namespace
        CoherenceNamespaceContentHandler coherenceNamespaceContentHandler;

        try
        {
            coherenceNamespaceContentHandler =
                (CoherenceNamespaceContentHandler) context
                    .ensureNamespaceContentHandler("",
                                                   new URI(String
                                                       .format("class:%s",
                                                               CoherenceNamespaceContentHandler.class.getName())));
        }
        catch (URISyntaxException uriSyntaxException)
        {
            // ERROR: this means that our own URI is broken! we need to throw an exception
            throw new RuntimeException("FATAL ERROR: The internal URI created by the ExtensibleEnvironment is invalid.",
                                       uriSyntaxException);
        }

        // attempt to process the coherence cache configuration we've been provided with the configuration context
        try
        {
            context.processDocument(xmlConfig);

            // clean up the manually registered NamespaceContentHandlers
            coherenceNamespaceContentHandler.onEndScope(context, "", context.getNamespaceURI(""));

            // delegate responsibility for configuring Coherence to the DefaultConfigurableCacheFactory
            // (using the CoherenceNamespaceContentHandler to provide an appropriate "standard" coherence configuration)
            StringBuilder builder = new StringBuilder();

            coherenceNamespaceContentHandler.build(builder);

            XmlDocument xmlDocument = XmlHelper.loadXml(builder.toString());

            super.setConfig(xmlDocument);
        }
        catch (ConfigurationException configurationException)
        {
            logger.log(Level.SEVERE, configurationException.toString());

            throw ensureRuntimeException(configurationException);
        }
        finally
        {
            // we're done configuring now
            configurationContext.set(null);
        }

        // let everyone know that the Environment has now available (has started)
        // NOTE: we do asynchronously to ensure that there is no re-entrant calls caused by the
        // EventProcessors waiting for this event as it's highly likely they'll use Coherence directly and calls
        // made from this (Coherence) thread still believe configuration is still in progress.
        getResource(EventDispatcher.class)
            .dispatchEventLater(new LifecycleStartedEvent<Environment>(ExtensibleEnvironment.this));

        if (logger.isLoggable(Level.FINEST))
        {
            LogHelper.exiting(logger, this.getClass().getName(), "setConfig");
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Service ensureService(XmlElement element)
    {
        // grab the event dispatcher... we're going to use it to notify registered
        // EventProcessors about service life-cycles.
        final EventDispatcher eventDispatcher = this.getResource(EventDispatcher.class);

        // resolve any inherited/referenced scheme elements for the provided element
        // (we need to do this to find the scheme name of the service when using
        // scheme-refs)
        XmlElement serviceSchemeElement = resolveScheme(element, null, false, false);

        // determine if the service is already known to us
        final String serviceName      = serviceSchemeElement.getSafeElement("service-name").getString();
        boolean      isTrackedService = trackedServices.containsKey(serviceName);

        // delegate the actual "ensuring" the service to the super class
        // (as we don't know how to start it here)
        Service service     = super.ensureService(element);
        String  serviceType = service.getInfo().getServiceType();

        // now determine what happened
        // (we only care if we started/restarted a real named service)
        if (serviceName.length() > 0 &&!serviceType.equals("LocalCache"))
        {
            if (isTrackedService)
            {
                // determine if we actually restarted (a new instance) of an existing service
                Service previousService = trackedServices.get(serviceName);

                if (service != previousService)
                {
                    // notify everyone that the previous service has stopped
                    // (as it's no longer good for anyone to use)
                    eventDispatcher.dispatchEvent(new LifecycleStoppedEvent<Service>(previousService));

                    trackedServices.put(serviceName, service);

                    // now notify everyone that the new service has started
                    eventDispatcher.dispatchEvent(new LifecycleStartedEvent<Service>(service));
                }
            }
            else
            {
                trackedServices.put(serviceName, service);

                // notify everyone that the new service has started
                eventDispatcher.dispatchEvent(new LifecycleStartedEvent<Service>(service));

                // add a listener to the service to determine if it is programmatically shutdown
                // (so we can clean up)
                service.addServiceListener(new ServiceListener()
                {
                    /**
                     * {@inheritDoc}
                     */
                    public void serviceStopping(ServiceEvent serviceEvent)
                    {
                        // SKIP: we only care when the service is started or stopped
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void serviceStopped(ServiceEvent serviceEvent)
                    {
                        if (serviceEvent.getService() instanceof Service)
                        {
                            eventDispatcher
                                .dispatchEvent(new LifecycleStoppedEvent<Service>((Service) serviceEvent.getService()));
                            trackedServices.remove(serviceName);
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void serviceStarting(ServiceEvent serviceEvent)
                    {
                        // SKIP: we only care when the service is started or stopped
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void serviceStarted(ServiceEvent serviceEvent)
                    {
                        if (serviceEvent.getService() instanceof Service)
                        {
                            eventDispatcher
                                .dispatchEvent(new LifecycleStartedEvent<Service>((Service) serviceEvent.getService()));
                            trackedServices.put(serviceName, (Service) serviceEvent.getService());
                        }
                    }
                });
            }
        }

        return service;
    }


    /**
     * Tracks if the specified named cache has been seen before.  If it hasn't, it raises
     * a {@link NamedCacheStorageRealizedEvent}.
     *
     * @param cacheName The named cache
     */
    protected void trackNamedCacheStorage(String cacheName)
    {
        boolean raiseEvent = false;

        synchronized (trackedNamedCaches)
        {
            raiseEvent = !trackedNamedCaches.contains(cacheName);

            if (raiseEvent)
            {
                trackedNamedCaches.add(cacheName);
            }
        }

        if (raiseEvent)
        {
            this.getResource(EventDispatcher.class).dispatchEvent(new NamedCacheStorageRealizedEvent(cacheName));
        }
    }


    /**
     * Stops tracking the specified named cache.  If the cache has been tracked, it raises
     * a {@link NamedCacheStorageReleasedEvent}.
     *
     * @param cacheName The named cache
     */
    protected void untrackNamedCacheStorage(String cacheName)
    {
        boolean raiseEvent = false;

        synchronized (trackedNamedCaches)
        {
            raiseEvent = trackedNamedCaches.contains(cacheName);

            if (raiseEvent)
            {
                trackedNamedCaches.remove(cacheName);
            }
        }

        if (raiseEvent)
        {
            this.getResource(EventDispatcher.class).dispatchEvent(new NamedCacheStorageReleasedEvent(cacheName));
        }
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"rawtypes"})
    @Override
    protected void register(CacheService cacheService,
                            String       cacheName,
                            String       context,
                            Map          map)
    {
        super.register(cacheService, cacheName, context, map);

        // now track that cache - the cache storage must now exist
        trackNamedCacheStorage(cacheName);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void unregister(CacheService cacheService,
                              String       cacheName)
    {
        super.unregister(cacheService, cacheName);

        // now untrack the cache - the cache storage must have been released
        untrackNamedCacheStorage(cacheName);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void unregister(String cacheName,
                              String context)
    {
        super.unregister(cacheName, context);

        // now untrack the cache - the cache storage must have been released
        untrackNamedCacheStorage(cacheName);
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Object instantiateAny(CacheInfo                info,
                                 XmlElement               xmlClass,
                                 BackingMapManagerContext context,
                                 ClassLoader              loader)
    {
        // use a registered Scheme to produce the instance (if required)
        if (xmlClass.getName().equals("class-scheme") && xmlClass.getAttributeMap().containsKey("use-scheme"))
        {
            // determine the schemeId for the Scheme to use
            String schemeId = xmlClass.getAttribute("use-scheme").getString();

            try
            {
                // locate the builder
                ParameterizedBuilder<?> builder =
                    (ParameterizedBuilder<?>) getResource(BuilderRegistry.class).getBuilder(schemeId);

                // locate the cache mapping
                CacheMapping cacheMapping =
                    getResource(CacheMappingRegistry.class).findCacheMapping(info.getCacheName());

                // determine the parameter provider for the cache mapping
                MutableParameterProvider parameterProvider =
                    new ScopedParameterProvider(cacheMapping == null
                                                ? SystemPropertyParameterProvider.INSTANCE
                                                : cacheMapping.getParameterProvider());

                // Copy any attributes from the CacheInfo into the parameterProvider
                for (Map.Entry attributeEntry : (Set<Map.Entry>) info.getAttributes().entrySet())
                {
                    parameterProvider.addParameter(new Parameter(attributeEntry.getKey().toString(),
                                                                 attributeEntry.getValue()));
                }

                // add the standard coherence parameters to the parameter provider
                parameterProvider.addParameter(new Parameter("cache-name", info.getCacheName()));
                parameterProvider.addParameter(new Parameter("class-loader", loader));
                parameterProvider.addParameter(new Parameter("manager-context", context));

                // set the class loader if we have one
                if (builder instanceof ClassLoaderAware)
                {
                    ((ClassLoaderAware) builder).setContextClassLoader(loader);
                }

                return builder.realize(parameterProvider);
            }
            catch (ClassCastException classCastException)
            {
                throw new RuntimeException(String
                    .format("Cound not instantiate %s as the namespace did not return a ClassScheme.", xmlClass),
                                           classCastException);
            }
        }
        else
        {
            return super.instantiateAny(info, xmlClass, context, loader);
        }
    }
}
