/*
 * File: AbstractApplicationSchema.java
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

package com.oracle.coherence.common.runtime;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * An {@link AbstractApplicationSchema} is a base implementation of an {@link ApplicationSchema}.
 * <p>
 * <strong>This package is now deprecated.  Please use the com.oracle.tools package instead.</strong>
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@Deprecated
public abstract class AbstractApplicationSchema<A extends Application, S extends ApplicationSchema<A, S>>
    implements ApplicationSchema<A, S>
{
    /**
     * The {@link PropertiesBuilder} containing the environment variables to be used when realizing
     * the {@link Application}.
     */
    private PropertiesBuilder m_propertiesBuilder;

    /**
     * The arguments for the {@link Application}.
     */
    private ArrayList<String> m_applicationArguments;


    /**
     * Construct an {@link AbstractApplicationSchema}.
     */
    public AbstractApplicationSchema()
    {
        m_propertiesBuilder    = new PropertiesBuilder();
        m_applicationArguments = new ArrayList<String>();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public PropertiesBuilder getEnvironmentVariablesBuilder()
    {
        return m_propertiesBuilder;
    }


    /**
     * Sets the specified environment variable to use an {@link Iterator} from which to retrieve it's values.
     *
     * @param name The name of the environment variable.
     *
     * @param iterator An {@link Iterator} providing values for the environment variable.
     *
     * @return The {@link ApplicationSchema} (so that we can perform method chaining).
     */
    @SuppressWarnings("unchecked")
    public S setEnvironmentVariable(String      name,
                                    Iterator<?> iterator)
    {
        m_propertiesBuilder.setProperty(name, iterator);

        return (S) this;
    }


    /**
     * Sets the specified environment variable to the specified value.
     *
     * @param name The name of the environment variable.
     *
     * @param value The value of the environment variable.
     *
     * @return The {@link ApplicationSchema} (so that we can perform method chaining).
     */
    @SuppressWarnings("unchecked")
    public S setEnvironmentVariable(String name,
                                    Object value)
    {
        m_propertiesBuilder.setProperty(name, value);

        return (S) this;
    }


    /**
     * Adds/Overrides the current environment variables with those specified by the {@link PropertiesBuilder}.
     *
     * @param environmentVariablesBuilder The environment variables to add/override on the {@link ApplicationBuilder}.
     *
     * @return The {@link ApplicationSchema} (so that we can perform method chaining).
     */
    @SuppressWarnings("unchecked")
    public S setEnvironmentVariables(PropertiesBuilder environmentVariablesBuilder)
    {
        m_propertiesBuilder.addProperties(environmentVariablesBuilder);

        return (S) this;
    }


    /**
     * Clears the currently registered environment variables from the {@link ApplicationBuilder}.
     *
     * @return The {@link ApplicationSchema} (so that we can perform method chaining).
     */
    @SuppressWarnings("unchecked")
    public S clearEnvironmentVariables()
    {
        m_propertiesBuilder.clear();

        return (S) this;
    }


    /**
     * Adds an argument to use when starting the {@link Application}.
     *
     * @param argument The argument for the {@link Application}.
     */
    public void addArgument(String argument)
    {
        m_applicationArguments.add(argument);
    }


    /**
     * {@inheritDoc}
     */
    public List<String> getArguments()
    {
        return m_applicationArguments;
    }


    /**
     * Adds an argument to use when starting the {@link Application}.
     *
     * @param argument The argument for the {@link Application}.
     *
     * @return The resulting {@link ApplicationBuilder}.
     */
    @SuppressWarnings("unchecked")
    public S setArgument(String argument)
    {
        addArgument(argument);

        return (S) this;
    }
}
