/*
 * File: AbstractApplicationBuilder.java
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

package com.oracle.coherence.common.applications;

import com.oracle.coherence.common.resourcing.ResourcePreparer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <strong>This package is now deprecated.  Please use the com.oracle.tools package instead.</strong>
 * <p>
 * An {@link AbstractApplicationBuilder} is a base implementation of an {@link ApplicationBuilder}s that provides
 * access and management of environment variables, together with arguments for the said {@link ApplicationBuilder}s.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <A> The type of {@link Application} that the {@link AbstractApplicationBuilder} will create.
 * @param <T> The type of the {@link AbstractApplicationBuilder} from which default configuration may be retrieved.
 *
 * @see Application
 * @see ApplicationBuilder
 *
 * @author Brian Oliver
 */
@Deprecated
public abstract class AbstractApplicationBuilder<A extends Application, T extends ApplicationBuilder<A, ?>>
    implements ApplicationBuilder<A, T>
{
    /**
     * The {@link PropertiesBuilder} containing the environment variables to be used when realizing
     * the {@link Application}.
     */
    private PropertiesBuilder environmentVariablesBuilder;

    /**
     * The arguments for the application.
     */
    private ArrayList<String> arguments;

    /**
     * The {@link ResourcePreparer} for the {@link Application} that we'll use to ensure the application is prepared and
     * ready for use once built.  If this is <code>null</code> then we simply return the built {@link Application}.
     */
    private ResourcePreparer<A> preparer;


    /**
     * Standard Constructor.
     */
    public AbstractApplicationBuilder()
    {
        environmentVariablesBuilder = new PropertiesBuilder();
        arguments                   = new ArrayList<String>();
        preparer                    = null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public PropertiesBuilder getEnvironmentVariablesBuilder()
    {
        return environmentVariablesBuilder;
    }


    /**
     * Sets the specified environment variable to use an {@link Iterator} from which to retrieve it's values
     * when the {@link ApplicationBuilder} is realized.
     *
     * @param name The name of the environment variable.
     *
     * @param iterator An {@link Iterator} providing values for the environment variable.
     *
     * @return The modified {@link ApplicationBuilder} (so that we can perform method chaining).
     */
    @SuppressWarnings("unchecked")
    public T setEnvironmentVariable(String      name,
                                    Iterator<?> iterator)
    {
        environmentVariablesBuilder.setProperty(name, iterator);

        return (T) this;
    }


    /**
     * Sets the specified environment variable to the specified value that is then used when the
     * {@link ApplicationBuilder} is realized.
     *
     * @param name The name of the environment variable.
     *
     * @param value The value of the environment variable.
     *
     * @return The modified {@link ApplicationBuilder} (so that we can perform method chaining).
     */
    @SuppressWarnings("unchecked")
    public T setEnvironmentVariable(String name,
                                    Object value)
    {
        environmentVariablesBuilder.setProperty(name, value);

        return (T) this;
    }


    /**
     * Adds/Overrides the current environment variables with those specified by the {@link PropertiesBuilder}.
     *
     * @param environmentVariablesBuilder The environment variables to add/override on the {@link ApplicationBuilder}.
     *
     * @return The modified {@link ApplicationBuilder} (so that we can perform method chaining).
     */
    @SuppressWarnings("unchecked")
    public T setEnvironmentVariables(PropertiesBuilder environmentVariablesBuilder)
    {
        environmentVariablesBuilder.addProperties(environmentVariablesBuilder);

        return (T) this;
    }


    /**
     * Clears the currently registered environment variables from the {@link ApplicationBuilder}.
     *
     * @return The modified {@link ApplicationBuilder} (so that we can perform method chaining).
     */
    @SuppressWarnings("unchecked")
    public T clearEnvironmentVariables()
    {
        environmentVariablesBuilder.clear();

        return (T) this;
    }


    /**
     * Adds an argument to use when starting the {@link Application}.
     *
     * @param argument The argument for the {@link Application}.
     */
    public void addArgument(String argument)
    {
        arguments.add(argument);
    }


    /**
     * Returns the list of arguments defined for the {@link Application}.
     *
     * @return {@link List} of {@link String}s.
     */
    public List<String> getArguments()
    {
        return arguments;
    }


    /**
     * Adds an argument to use when starting the {@link Application}.
     *
     * @param argument The argument for the {@link Application}.
     *
     * @return The resulting {@link ApplicationBuilder}.
     */
    @SuppressWarnings("unchecked")
    public T setArgument(String argument)
    {
        addArgument(argument);

        return (T) this;
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public T setApplicationPreparer(ResourcePreparer<A> preparer)
    {
        this.preparer = preparer;

        return (T) this;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ResourcePreparer<A> getApplicationPreparer()
    {
        return preparer;
    }
}
