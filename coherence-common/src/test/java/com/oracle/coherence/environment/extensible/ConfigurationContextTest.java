/*
 * File: ConfigurationContextTest.java
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
import com.oracle.coherence.configuration.Mandatory;
import com.oracle.coherence.configuration.Property;
import com.oracle.coherence.configuration.caching.CacheMappingRegistry;
import com.oracle.coherence.environment.Environment;
import com.oracle.coherence.environment.extensible.ConfigurationContextTest.TestScheme.Switch;
import com.oracle.coherence.environment.extensible.namespaces.CoherenceNamespaceContentHandler;
import com.oracle.coherence.environment.extensible.namespaces.ValueNamespaceContentHandler;
import com.oracle.tools.junit.AbstractTest;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map;
import java.util.UnknownFormatConversionException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The unit tests for the {@link ConfigurationContext}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class ConfigurationContextTest extends AbstractTest
{
    /**
     * Ensure that namespaces can be registered and retrieved from a {@link ConfigurationContext}.
     *
     * @throws URISyntaxException if the referenced uri is malformed
     */
    @Test
    public void testNamespaceRegistration() throws URISyntaxException
    {
        Environment          env     = mock(Environment.class);

        ConfigurationContext context = new DefaultConfigurationContext(env);

        URI                  uri     = new URI(String.format("class:%s", ValueNamespaceContentHandler.class.getName()));

        context.ensureNamespaceContentHandler("value", uri);

        assertTrue(context.getNamespaceURI("value").equals(uri));
        assertTrue(context.getNamespaceContentHandler("value") instanceof ValueNamespaceContentHandler);
        assertTrue(context.getNamespaceContentHandler(uri) instanceof ValueNamespaceContentHandler);
        assertTrue(context.getNamespaceContentHandler("value") == context.getNamespaceContentHandler(uri));
    }


    /**
     * Ensure that we can use a {@link ConfigurationContext} to process an {@link XmlElement}
     * in that namespace.
     *
     * @throws URISyntaxException     if the referenced uri is malformed
     * @throws ConfigurationException if the configuration is wrong
     * @throws IOException            if there is an IO problem
     */
    @Test
    public void testProcessElement() throws URISyntaxException, ConfigurationException, IOException
    {
        Environment          env     = mock(Environment.class);

        ConfigurationContext context = new DefaultConfigurationContext(env);

        URI                  uri     = new URI(String.format("class:%s", ValueNamespaceContentHandler.class.getName()));

        context.ensureNamespaceContentHandler("value", uri);

        String     xml        = "<value:integer>5</value:integer>";
        XmlElement xmlElement = XmlHelper.loadXml(xml).getRoot();

        assertTrue(context.processElement(xmlElement).equals(new Integer(5)));
    }


    /**
     * Ensure that we can use a {@link ConfigurationContext} to process an {@link XmlElement}
     * in that namespace.
     *
     * @throws URISyntaxException     if the referenced uri is malformed
     * @throws ConfigurationException if the configuration is wrong
     */
    @Test
    public void testProcessElementOf() throws URISyntaxException, ConfigurationException
    {
        Environment          env     = mock(Environment.class);

        ConfigurationContext context = new DefaultConfigurationContext(env);

        URI                  uri     = new URI(String.format("class:%s", ValueNamespaceContentHandler.class.getName()));

        context.ensureNamespaceContentHandler("value", uri);

        String     xml        = "<cache-config><value:integer>5</value:integer></cache-config>";
        XmlElement xmlElement = XmlHelper.loadXml(xml).getRoot();

        assertTrue(context.processElementOf(xmlElement, new QualifiedName("value",
                                                                          "integer"), 10).equals(new Integer(5)));
        assertTrue(context.processElementOf(xmlElement, new QualifiedName("unknown",
                                                                          "integer"), 10).equals(new Integer(10)));
        assertTrue(context.processOnlyElementOf(xmlElement).equals(new Integer(5)));
    }


    /**
     * Ensure that we can use a {@link ConfigurationContext} to process an identified {@link XmlElement}.
     *
     * @throws URISyntaxException     if the referenced uri is malformed
     * @throws ConfigurationException if the configuration is wrong
     */
    @Test
    public void testProcessElementOfWithId() throws URISyntaxException, ConfigurationException
    {
        Environment          env     = mock(Environment.class);

        ConfigurationContext context = new DefaultConfigurationContext(env);

        URI                  uri     = new URI(String.format("class:%s", ValueNamespaceContentHandler.class.getName()));

        context.ensureNamespaceContentHandler("value", uri);

        context.ensureNamespaceContentHandler("",
                                              new URI(String.format("class:%s",
                                                                    CoherenceNamespaceContentHandler.class.getName())));

        String     xml        = "<cache-config><value:integer id=\"number\">5</value:integer></cache-config>";
        XmlElement xmlElement = XmlHelper.loadXml(xml).getRoot();

        assertTrue(context.processElementOf(xmlElement, "number", 10).equals(new Integer(5)));
        assertTrue(context.processElementOf(xmlElement, "unknown", 10).equals(new Integer(10)));
    }


    /**
     * Ensure that we can use a {@link ConfigurationContext} to process an identified {@link XmlElement}.
     *
     * @throws URISyntaxException     if the referenced uri is malformed
     * @throws ConfigurationException if the configuration is wrong
     */
    @Test
    public void testProcessElementsOf() throws URISyntaxException, ConfigurationException
    {
        Environment          env     = mock(Environment.class);

        ConfigurationContext context = new DefaultConfigurationContext(env);

        URI                  uri     = new URI(String.format("class:%s", ValueNamespaceContentHandler.class.getName()));

        context.ensureNamespaceContentHandler("value", uri);

        context.ensureNamespaceContentHandler("",
                                              new URI(String.format("class:%s",
                                                                    CoherenceNamespaceContentHandler.class.getName())));

        String xml = "<cache-config>" + "<value:integer id=\"first\">1</value:integer>"
                     + "<value:integer id=\"second\">2</value:integer>" + "<value:integer>3</value:integer>"
                     + "</cache-config>";
        XmlElement     xmlElement = XmlHelper.loadXml(xml).getRoot();

        Map<String, ?> result     = context.processElementsOf(xmlElement);

        assert result.size() == 3;

        Iterator<?> values = result.values().iterator();

        assertTrue(values.next().equals(new Integer(1)));
        assertTrue(values.next().equals(new Integer(2)));
        assertTrue(values.next().equals(new Integer(3)));
        assertFalse(values.hasNext());
    }


    /**
     * Ensure that "system-property" uses in custom-namespaces are resolved.
     *
     * @throws URISyntaxException     if the referenced uri is malformed
     * @throws ConfigurationException if the configuration is wrong
     */
    @Test
    public void testReplaceSystemProperties() throws URISyntaxException, ConfigurationException
    {
        System.setProperty("custom-property", "10");

        Environment          env     = mock(Environment.class);

        ConfigurationContext context = new DefaultConfigurationContext(env);

        URI                  uri     = new URI(String.format("class:%s", ValueNamespaceContentHandler.class.getName()));

        context.ensureNamespaceContentHandler("value", uri);

        context.ensureNamespaceContentHandler("",
                                              new URI(String.format("class:%s",
                                                                    CoherenceNamespaceContentHandler.class.getName())));

        String xml    = "<value:integer system-property=\"custom-property\">1</value:integer>";

        Object result = context.processDocument(XmlHelper.loadXml(xml));

        assertTrue(result.equals(new Integer(10)));
    }


    /**
     * Ensure that "system-property" uses in a document are resolved.
     *
     * @throws URISyntaxException     if the referenced uri is malformed
     * @throws ConfigurationException if the configuration is wrong
     */
    @Test
    public void testProcessDocumentString() throws URISyntaxException, ConfigurationException
    {
        System.setProperty("custom-property", "10");

        Environment          env     = mock(Environment.class);

        ConfigurationContext context = new DefaultConfigurationContext(env);

        URI                  uri     = new URI(String.format("class:%s", ValueNamespaceContentHandler.class.getName()));

        context.ensureNamespaceContentHandler("value", uri);

        context.ensureNamespaceContentHandler("",
                                              new URI(String.format("class:%s",
                                                                    CoherenceNamespaceContentHandler.class.getName())));

        String xml    = "<value:integer system-property=\"custom-property\">1</value:integer>";

        Object result = context.processDocument(xml);

        assertTrue(result.equals(new Integer(10)));
    }


    /**
     * Ensure that we can load and process a document given a string location.
     *
     * @throws URISyntaxException     if the referenced uri is malformed
     * @throws ConfigurationException if the configuration is wrong
     */
    @Test
    public void testProcessDocumentAtLocation() throws URISyntaxException, ConfigurationException
    {
        Environment          env                  = mock(Environment.class);

        CacheMappingRegistry cacheMappingRegistry = new CacheMappingRegistry();

        when(env.getResource(CacheMappingRegistry.class)).thenReturn(cacheMappingRegistry);

        BuilderRegistry schemeRegistry = new BuilderRegistry();

        when(env.getResource(BuilderRegistry.class)).thenReturn(schemeRegistry);

        ConfigurationContext context = new DefaultConfigurationContext(env);

        CoherenceNamespaceContentHandler namespaceHandler =
            (CoherenceNamespaceContentHandler) context.ensureNamespaceContentHandler("",
                                                                                     new URI(String
                                                                                         .format("class:%s",
                                                                                             CoherenceNamespaceContentHandler.class
                                                                                                 .getName())));

        context.processDocumentAt("coherence-common-cache-config.xml");

        assertTrue(namespaceHandler.isCacheNameDefined("coherence.common.sequencegenerators"));
        assertTrue(namespaceHandler.isSchemeNameDefined("distributed-scheme-for-sequence-generators"));
    }


    /**
     * Ensure that mandatory properties are set.
     */
    @Test
    public void testMandatoryProperty()
    {
        Environment          env        = mock(Environment.class);
        ConfigurationContext context    = new DefaultConfigurationContext(env);

        String               xml        = "<element/>";
        XmlElement           xmlElement = XmlHelper.loadXml(xml).getRoot();

        try
        {
            TestScheme scheme = new TestScheme();

            context.configure(scheme, new QualifiedName(xmlElement), xmlElement);
            assertFalse(true);
        }
        catch (ConfigurationException configurationException)
        {
            assertTrue(configurationException.toString().contains("mandatory"));
        }
    }


    /**
     * Ensure that we can set a Boolean property itaken from an xml attribute.
     *
     * @throws UnknownFormatConversionException if the format is unknown
     * @throws ConfigurationException if configuration is invalid
     */
    @Test
    public void testBooleanAttributeProperty() throws UnknownFormatConversionException, ConfigurationException
    {
        Environment          env        = mock(Environment.class);
        ConfigurationContext context    = new DefaultConfigurationContext(env);

        String               xml        = "<element boolean-property=\"true\" mandatory-property=\"funky\"/>";
        XmlElement           xmlElement = XmlHelper.loadXml(xml).getRoot();

        TestScheme           scheme     = new TestScheme();

        context.configure(scheme, new QualifiedName(xmlElement), xmlElement);

        assertTrue(scheme.isBooleanProperty());
    }


    /**
     * Ensure that we can set a Boolean property taken from an xml element.
     *
     * @throws UnknownFormatConversionException if the format is unknown
     * @throws ConfigurationException if configuration is invalid
     */
    @Test
    public void testBooleanElementProperty() throws UnknownFormatConversionException, ConfigurationException
    {
        Environment          env        = mock(Environment.class);
        ConfigurationContext context    = new DefaultConfigurationContext(env);

        String xml = "<element mandatory-property=\"funky\"><boolean-property>true</boolean-property></element>";
        XmlElement           xmlElement = XmlHelper.loadXml(xml).getRoot();

        TestScheme           scheme     = new TestScheme();

        context.configure(scheme, new QualifiedName(xmlElement), xmlElement);

        assertTrue(scheme.isBooleanProperty());
    }


    /**
     * Ensure that we can set a String property in taken from an xml attribute.
     *
     * @throws UnknownFormatConversionException if the format is unknown
     * @throws ConfigurationException if configuration is invalid
     */
    @Test
    public void testStringAttributeProperty() throws UnknownFormatConversionException, ConfigurationException
    {
        Environment          env        = mock(Environment.class);
        ConfigurationContext context    = new DefaultConfigurationContext(env);

        String               xml        = "<element string-property=\"hello world\" mandatory-property=\"funky\"/>";
        XmlElement           xmlElement = XmlHelper.loadXml(xml).getRoot();

        TestScheme           scheme     = new TestScheme();

        context.configure(scheme, new QualifiedName(xmlElement), xmlElement);

        assertTrue(scheme.getStringProperty().equals("hello world"));
    }


    /**
     * Ensure that we can set a String property in taken from an xml element.
     *
     * @throws UnknownFormatConversionException if the format is unknown
     * @throws ConfigurationException if configuration is invalid
     */
    @Test
    public void testStringElementProperty() throws UnknownFormatConversionException, ConfigurationException
    {
        Environment          env        = mock(Environment.class);
        ConfigurationContext context    = new DefaultConfigurationContext(env);

        String xml = "<element mandatory-property=\"funky\"><string-property>hello world</string-property></element>";
        XmlElement           xmlElement = XmlHelper.loadXml(xml).getRoot();

        TestScheme           scheme     = new TestScheme();

        context.configure(scheme, new QualifiedName(xmlElement), xmlElement);

        assertTrue(scheme.getStringProperty().equals("hello world"));
    }


    /**
     * Ensure that we can set an Enum property in taken from an xml attribute.
     *
     * @throws UnknownFormatConversionException if the format is unknown
     * @throws ConfigurationException if configuration is invalid
     */
    @Test
    public void testEnumAttributeProperty() throws UnknownFormatConversionException, ConfigurationException
    {
        Environment          env        = mock(Environment.class);
        ConfigurationContext context    = new DefaultConfigurationContext(env);

        String               xml        = "<element mandatory-property=\"funky\" enum-property=\"On\"/>";
        XmlElement           xmlElement = XmlHelper.loadXml(xml).getRoot();

        TestScheme           scheme     = new TestScheme();

        context.configure(scheme, new QualifiedName(xmlElement), xmlElement);

        assertTrue(scheme.getEnumProperty() == Switch.On);
    }


    /**
     * Ensure that we can set an Enum property in taken from an xml element.
     *
     * @throws UnknownFormatConversionException if the format is unknown
     * @throws ConfigurationException if configuration is invalid
     */
    @Test
    public void testEnumElementProperty() throws UnknownFormatConversionException, ConfigurationException
    {
        Environment          env        = mock(Environment.class);
        ConfigurationContext context    = new DefaultConfigurationContext(env);

        String               xml = "<element mandatory-property=\"funky\"><enum-property>Off</enum-property></element>";
        XmlElement           xmlElement = XmlHelper.loadXml(xml).getRoot();

        TestScheme           scheme     = new TestScheme();

        context.configure(scheme, new QualifiedName(xmlElement), xmlElement);

        assertTrue(scheme.getEnumProperty() == Switch.Off);
    }


    /**
     * A {@link TestScheme}.
     *
     * @author Brian Oliver
     */
    public static class TestScheme
    {
        /**
         * A {@link Switch}.
         *
         * @author Brian Oliver
         */
        public enum Switch
        {
            /**
             * The {@link Switch} is On.
             */
            On,

            /**
             * The {@link Switch} is Off.
             */
            Off
        }


        /**
         * A mandatory property.
         */
        private String mandatoryProperty;

        /**
         * A boolean property.
         */
        private boolean booleanProperty;

        /**
         * A string property.
         */
        private String stringProperty;

        ;

        /**
         * A {@link Switch} property.
         */
        private Switch enumProperty;


        /**
         * Returns mandatory property.
         *
         * @return the mandatory property
         */
        public String getMandatoryProperty()
        {
            return mandatoryProperty;
        }


        /**
         * Sets the mandatory property.
         *
         * @param mandatoryProperty the mandatory property
         */
        @Property("mandatory-property")
        @Mandatory
        public void setMandatoryProperty(String mandatoryProperty)
        {
            this.mandatoryProperty = mandatoryProperty;
        }


        /**
         * Returns the boolean property.
         *
         * @return the boolean property
         */
        public boolean isBooleanProperty()
        {
            return booleanProperty;
        }


        /**
         * Sets the boolean property.
         *
         * @param booleanProperty the new boolean property
         */
        @Property("boolean-property")
        public void setBooleanProperty(boolean booleanProperty)
        {
            this.booleanProperty = booleanProperty;
        }


        /**
         * Returns the string property.
         *
         * @return the string property
         */
        public String getStringProperty()
        {
            return stringProperty;
        }


        /**
         * Sets the string property.
         *
         * @param stringProperty the string property
         */
        @Property("string-property")
        public void setStringProperty(String stringProperty)
        {
            this.stringProperty = stringProperty;
        }


        /**
         * Returns the enum property.
         *
         * @return the enum property
         */
        public Switch getEnumProperty()
        {
            return enumProperty;
        }


        /**
         * Sets the enum property.
         *
         * @param enumProperty the enum property to set
         */
        @Property("enum-property")
        public void setEnumProperty(Switch enumProperty)
        {
            this.enumProperty = enumProperty;
        }
    }
}
