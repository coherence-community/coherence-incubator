/*
 * File: JMXUtils.java
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

package com.sun.tools.visualvm.modules.coherence.helper;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

/**
 * Utility class for querying JMX.
 *
 * @author Tim Middleton
 */
public class JMXUtils
{
    /**
     * Retrieves a number of attributes from an MBean server query
     *
     * @param server       the {@link MBeanServerConnection} to query
     * @param sQuery       the query to run
     * @param aFields      the array of attributes to retrieve
     *
     * @return a {@link Set} of Object[] of results
     */
    public static Set<Object[]> runJMXQuery(MBeanServerConnection server,
                                            String                sQuery,
                                            JMXField[]            aFields)
    {
        if (aFields == null || aFields.length == 0)
        {
            throw new IllegalArgumentException("Please supply at least one JMXField");
        }

        Set<Object[]> setResults = new HashSet<Object[]>();

        try
        {
            Set<ObjectName> resultSet = server.queryNames(new ObjectName(sQuery), null);

            // loop through each result
            for (Iterator<ObjectName> restulsIter = resultSet.iterator(); restulsIter.hasNext(); )
            {
                ObjectName objectName   = restulsIter.next();
                Object[]   aoQueryResult = new Object[aFields.length];
                int        i             = 0;

                // go through each attribute and retrieve
                for (JMXField jmxField : aFields)
                {
                    if (jmxField instanceof Attribute)
                    {
                        aoQueryResult[i++] = server.getAttribute(objectName, jmxField.getName());
                    }
                    else
                    {
                        aoQueryResult[i++] = objectName.getKeyProperty(jmxField.getName());
                    }

                }

                setResults.add(aoQueryResult);

            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Unable to run query " + sQuery + " for fields " + aFields + " : "
                                       + e.getMessage());
        }

        return setResults;
    }


    /**
     * Run a JMX query expecting a single attribute
     *
     * @param server       the {@link MBeanServerConnection} to query
     * @param sQuery       the query to run
     * @param jmxField     the {@link JMXField} to retrieve
     *
     * @return a single {@link Object} result
     */
    public static Object runJMXQuerySingleResult(MBeanServerConnection server,
                                                 String                sQuery,
                                                 JMXField              jmxField)
    {
        Set<Object[]> setResults = runJMXQuery(server, sQuery, new JMXField[] {jmxField});

        if (setResults.size() != 1)
        {
            throw new RuntimeException("Returned " + setResults.size() + " results instead of 1 for query " + sQuery);
        }

        Object[] aoResult = setResults.iterator().next();

        return (aoResult == null ? null : aoResult[0]);
    }


    /**
     * A request for an attribute value.
     */
    public static class Attribute extends JMXField
    {
        /**
         * Construct a new attribute request for a given name.
         *
         * @param sName the name of the attribute
         */
        public Attribute(String sName)
        {
            super(sName);
        }
    }


    /**
     * A representation of a JMX Field request which can be an Attribute or Key.
     */
    public static abstract class JMXField
    {
        private String sName;


        /**
         * Construct a new field request for a given name.
         *
         * @param sName the name of the field
         */
        public JMXField(String sName)
        {
            this.sName = sName;
        }


        /**
         * Return the name of the field requested.
         *
         * @return the name of the field requested
         */
        public String getName()
        {
            return this.sName;
        }
    }


    /**
     * A request for a key value.
     */
    public static class Key extends JMXField
    {
        /**
         * Construct a new Key request for a given name.
         *
         * @param sName the name of the key
         */
        public Key(String sName)
        {
            super(sName);
        }
    }
}
