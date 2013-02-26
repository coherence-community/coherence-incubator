/*
 * File: PropertiesBuilder.java
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

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * <strong>This package is now deprecated.  Please use the com.oracle.tools package instead.</strong>
 * <p>
 * A {@link PropertiesBuilder} defines a set of property definitions that when realized may be used as
 * a traditional {@link Map} of name value pair properties.
 * <p>
 * Unlike traditional {@link Map}-based implementations of properties, a {@link PropertiesBuilder} provides
 * the ability to specify an {@link Iterator} for named properties that will in turn be used to acquire
 * actual property values when the said {@link PropertiesBuilder} is realized.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@Deprecated
public class PropertiesBuilder
{
    /**
     * The properties defined by the {@link PropertiesBuilder}.
     */
    private LinkedHashMap<String, Property> m_properties;


    /**
     * Standard Constructor (produces an empty {@link PropertiesBuilder}).
     */
    public PropertiesBuilder()
    {
        m_properties = new LinkedHashMap<String, Property>();
    }


    /**
     * Standard Constructor.
     * <p>
     * The provided properties are added as default values to the {@link PropertiesBuilder}.
     *
     * @param properties The {@link Map} of properties to use as the basis for the {@link PropertiesBuilder}
     */
    public PropertiesBuilder(Map<String, String> properties)
    {
        this();

        for (String name : properties.keySet())
        {
            m_properties.put(name, new Property(name, null, properties.get(name)));
        }
    }


    /**
     * Standard Constructor.
     * <p>
     * The provided properties are added as default values to the {@link PropertiesBuilder}.
     *
     * @param properties The {@link Properties} to use as the basis for the {@link PropertiesBuilder}
     */
    public PropertiesBuilder(Properties properties)
    {
        this();

        for (String key : properties.stringPropertyNames())
        {
            m_properties.put(key, new Property(key, null, properties.getProperty(key)));
        }
    }


    /**
     * Standard Copy Constructor.
     *
     * @param propertiesBuilder The {@link PropertiesBuilder} on which to base the new {@link PropertiesBuilder}
     */
    public PropertiesBuilder(PropertiesBuilder propertiesBuilder)
    {
        this();

        for (String name : propertiesBuilder.getPropertyNames())
        {
            m_properties.put(name, new Property(propertiesBuilder.m_properties.get(name)));
        }
    }


    /**
     * Sets the specified named property to use an {@link Iterator} to provide successive property values when
     * the {@link PropertiesBuilder} is realized.
     *
     * @param name     The name of the property.
     *
     * @param iterator An iterator that will provide successive property values for the property
     *                 when the {@link PropertiesBuilder} is realized.
     *
     * @return The {@link PropertiesBuilder} to which the property was added so that further chained method calls, like
     *         to other <code>setProperty(...)</code> methods on this class may be used.
     */
    public PropertiesBuilder setProperty(String      name,
                                         Iterator<?> iterator)
    {
        if (containsProperty(name))
        {
            m_properties.get(name).setValue(iterator);
        }
        else
        {
            m_properties.put(name, new Property(name, iterator, null));
        }

        return this;
    }


    /**
     * Sets the specified named default property to use an {@link Iterator} to provide successive property values when
     * the {@link PropertiesBuilder} is realized.
     *
     * @param name              The name of the property.
     *
     * @param defaultIterator   The default {@link Iterator} that will provide successive property values for the
     *                          property when the {@link PropertiesBuilder} is realized.
     *
     * @return The {@link PropertiesBuilder} to which the property was added so that further chained method calls, like
     *         to other <code>setProperty(...)</code> methods on this class may be used.
     */
    public PropertiesBuilder setDefaultProperty(String      name,
                                                Iterator<?> defaultIterator)
    {
        if (containsProperty(name))
        {
            m_properties.get(name).setDefaultValue(defaultIterator);
        }
        else
        {
            m_properties.put(name, new Property(name, null, defaultIterator));
        }

        return this;
    }


    /**
     * Sets the specified named property to have the specified value.
     *
     * @param name      The name of the property
     * @param value     The value of the property
     *
     * @return The {@link PropertiesBuilder} to which the property was added so that further chained method calls, like
     *         to other <code>setProperty(...)</code> methods on this class may be used.
     */
    public PropertiesBuilder setProperty(String name,
                                         Object value)
    {
        if (containsProperty(name))
        {
            m_properties.get(name).setValue(value);
        }
        else
        {
            m_properties.put(name, new Property(name, value, null));
        }

        return this;
    }


    /**
     * Sets the specified named default property to have the specified value.
     *
     * @param name          The name of the property
     * @param defaultValue  The default value of the property
     *
     * @return The {@link PropertiesBuilder} to which the property was added so that further chained method calls, like
     *         to other <code>withProperty(...)</code> methods on this class may be used.
     */
    public PropertiesBuilder setDefaultProperty(String name,
                                                Object defaultValue)
    {
        if (containsProperty(name))
        {
            m_properties.get(name).setDefaultValue(defaultValue);
        }
        else
        {
            m_properties.put(name, new Property(name, null, defaultValue));
        }

        return this;
    }


    /**
     * Adds and/or overrides the properties defined in the {@link PropertiesBuilder} with those from the specified
     * {@link PropertiesBuilder}.
     *
     * @param propertiesBuilder The {@link PropertiesBuilder} containing the properties to add to this {@link PropertiesBuilder}.
     */
    public void setProperties(PropertiesBuilder propertiesBuilder)
    {
        m_properties.putAll(propertiesBuilder.m_properties);
    }


    /**
     * Adds and/or overrides the properties defined in the {@link PropertiesBuilder} with those from the specified
     * {@link PropertiesBuilder}.
     *
     * @param propertiesBuilder The {@link PropertiesBuilder} containing the properties to add to this {@link PropertiesBuilder}.
     *
     * @return The {@link PropertiesBuilder} to which the property was added so that further chained method calls, like
     *         to other <code>withProperty(...)</code> methods on this class may be used.
     */
    public PropertiesBuilder addProperties(PropertiesBuilder propertiesBuilder)
    {
        setProperties(propertiesBuilder);

        return this;
    }


    /**
     * Returns if the specified named property is defined by the {@link PropertiesBuilder}.
     *
     * @param name  The name of the property
     *
     * @return <code>true</code> if the property is defined by the {@link PropertiesBuilder}, <code>false</code> otherwise.
     */
    public boolean containsProperty(String name)
    {
        return m_properties.containsKey(name);
    }


    /**
     * Returns the current value of the specified property.  If the property has a value specified, that value will
     * be used.  If not the default value of the property will be used.  If the property is not known, <code>null</code>
     * will be returned.
     *
     * @param name The name of the property.
     *
     * @return An {@link Object}
     */
    public Object getProperty(String name)
    {
        if (m_properties.containsKey(name))
        {
            Property property = m_properties.get(name);

            return property.hasValue() ? property.getValue() : property.getDefaultValue();
        }
        else
        {
            return null;
        }
    }


    /**
     * Removes the specified named property from the {@link PropertiesBuilder}.  If the specified property is not
     * contained by the {@link PropertiesBuilder}, nothing happens.
     *
     * @param name The name of the property to remove.
     */
    public void removeProperty(String name)
    {
        m_properties.remove(name);
    }


    /**
     * Clears all of the currently defined properties from the {@link PropertiesBuilder}.
     */
    public void clear()
    {
        m_properties.clear();
    }


    /**
     * Returns an {@link Iterable} over the property names defined by the {@link PropertiesBuilder}.
     *
     * @return {@link Iterable}
     */
    public Iterable<String> getPropertyNames()
    {
        return m_properties.keySet();
    }


    /**
     * Creates a new {@link Properties} instance containing name, value pairs defined by the {@link PropertiesBuilder}.
     * If a property with in the {@link PropertiesBuilder} is defined as an {@link Iterator}, the next value from the
     * said {@link Iterator} is used as a value for the property.
     *
     * @param overrides (optional may be <code>null</code>) This {@link PropertiesBuilder} may be
     *                              specified to override the defined properties.
     *
     * @return A new {@link Properties} instance as defined by the {@link PropertiesBuilder}
     */
    public Properties realize(PropertiesBuilder overrides)
    {
        Properties result = new Properties();

        // add all of the override properties first
        if (overrides != null)
        {
            result.putAll(overrides.realize());
        }

        for (String name : getPropertyNames())
        {
            if (!result.containsKey(name))
            {
                Object value = getProperty(name);

                if (value != null)
                {
                    if (value instanceof Iterator<?>)
                    {
                        Iterator<?> iterator = (Iterator<?>) value;

                        if (iterator.hasNext())
                        {
                            result.put(name, iterator.next().toString());
                        }
                        else
                        {
                            throw new IndexOutOfBoundsException(String
                                .format("No more values available for the property [%s]", name));
                        }
                    }
                    else
                    {
                        result.put(name, value.toString());
                    }
                }
            }
        }

        return result;
    }


    /**
     * Creates a new {@link Properties} instance containing name, value pairs defined by the {@link PropertiesBuilder}.
     * If a property with in the {@link PropertiesBuilder} is defined as an {@link Iterator}, the next value from the
     * said {@link Iterator} is used as a value for the property.
     *
     * @return A new {@link Properties} instance as defined by the {@link PropertiesBuilder}
     */
    public Properties realize()
    {
        return realize(null);
    }


    /**
     * Constructs a {@link PropertiesBuilder} using the properties defined in the specified Java properties file.
     *
     * @param fileName The name of the file (including path if required) from which to load the properties
     *
     * @return A {@link PropertiesBuilder}
     *
     * @throws IOException Should a problem occur while loading the properties
     */
    public static PropertiesBuilder fromPropertiesFile(String fileName) throws IOException
    {
        Properties properties = new Properties();
        URL        url        = ClassLoader.getSystemResource(fileName);

        properties.load(url.openStream());

        return new PropertiesBuilder(properties);
    }


    /**
     * Constructs a {@link PropertiesBuilder} using the environment variables of the currently executing process.
     *
     * @return A {@link PropertiesBuilder}
     */
    public static PropertiesBuilder fromCurrentEnvironmentVariables()
    {
        return new PropertiesBuilder(System.getenv());
    }


    /**
     * Constructs a {@link PropertiesBuilder} using the current system properties the currently executing process.
     *
     * @return A {@link PropertiesBuilder}
     */
    public static PropertiesBuilder fromCurrentSystemProperties()
    {
        return new PropertiesBuilder(System.getProperties());
    }


    /**
     * A {@link Property} represents the defined value (with possible a default) for a specified named property.
     */
    private static class Property
    {
        private String name;
        private Object value;
        private Object defaultValue;


        /**
         * Constructs ...
         *
         *
         * @param property
         */
        public Property(Property property)
        {
            this.name         = property.getName();
            this.value        = property.getValue();
            this.defaultValue = property.getDefaultValue();
        }


        /**
         * Constructs ...
         *
         *
         * @param name
         * @param value
         * @param defaultValue
         */
        public Property(String name,
                        Object value,
                        Object defaultValue)
        {
            this.name         = name;
            this.value        = value;
            this.defaultValue = defaultValue;
        }


        /**
         * Method description
         *
         * @return
         */
        public String getName()
        {
            return name;
        }


        /**
         * Method description
         *
         * @return
         */
        public Object getValue()
        {
            return value;
        }


        /**
         * Method description
         *
         * @param value
         */
        public void setValue(Object value)
        {
            this.value = value;
        }


        /**
         * Method description
         *
         * @return
         */
        public boolean hasValue()
        {
            return value != null;
        }


        /**
         * Method description
         *
         * @return
         */
        public Object getDefaultValue()
        {
            return defaultValue;
        }


        /**
         * Method description
         *
         * @param defaultValue
         */
        public void setDefaultValue(Object defaultValue)
        {
            this.defaultValue = defaultValue;
        }


        /**
         * Method description
         *
         * @return
         */
        @Override
        public String toString()
        {
            return String.format("{name=%s, value=%s, defaultValue=%s}", name, value, defaultValue);
        }
    }
}
