/*
 * File: DefaultConfigurationContext.java
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

import com.oracle.coherence.common.builders.ParameterizedBuilder;
import com.oracle.coherence.common.builders.ReflectiveBuilder;
import com.oracle.coherence.common.logging.LogHelper;
import com.oracle.coherence.common.util.AbstractEnrichmentSupport;
import com.oracle.coherence.common.util.EnrichmentSupport;
import com.oracle.coherence.common.util.Value;
import com.oracle.coherence.configuration.Mandatory;
import com.oracle.coherence.configuration.Property;
import com.oracle.coherence.configuration.SubType;
import com.oracle.coherence.configuration.Type;
import com.oracle.coherence.configuration.expressions.Constant;
import com.oracle.coherence.configuration.expressions.Expression;
import com.oracle.coherence.configuration.expressions.MacroParameterExpression;
import com.oracle.coherence.configuration.parameters.SystemPropertyParameterProvider;
import com.oracle.coherence.environment.Environment;
import com.oracle.coherence.environment.extensible.namespaces.NullNamespaceContentHandler;
import com.tangosol.net.DefaultConfigurableCacheFactory;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.run.xml.XmlValue;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.UUID;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The default implementation of a {@link ConfigurationContext}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class DefaultConfigurationContext implements ConfigurationContext, EnrichmentSupport
{
    /**
     * The {@link Logger} for this class.
     */
    private static final Logger logger = Logger.getLogger(DefaultConfigurationContext.class.getName());

    /**
     * The {@link Environment} that owns this {@link ConfigurationContext}.
     */
    private Environment environment;

    /**
     * The stack of {@link Scope}s, each of which maintains a collection of
     * registered {@link NamespaceContentHandler}s.  The top of the stack is the
     * first element of the {@link LinkedList}.
     */
    private LinkedList<Scope> scopes;


    /**
     * Standard Constructor.
     *
     * @param environment The {@link Environment} that owns this {@link ConfigurationContext}.
     */
    public DefaultConfigurationContext(Environment environment)
    {
        this.environment = environment;
        this.scopes      = new LinkedList<Scope>();
        this.scopes.addFirst(new Scope(this, null));
    }


    /**
     * {@inheritDoc}
     */
    public NamespaceContentHandler ensureNamespaceContentHandler(String prefix,
                                                                 URI    uri)
    {
        // find an existing NamespaceContentHandler exist for the uri
        NamespaceContentHandler namespaceContentHandler = getNamespaceContentHandler(uri);

        if (namespaceContentHandler == null)
        {
            // does the prefix already have a NamespaceContentHandler?
            namespaceContentHandler = getNamespaceContentHandler(prefix);

            // we can only allow a new NamespaceContentHandler to be loaded for non-default namespaces,
            // otherwise with Coherencee 3.7+ we may load a NullNamespaceContentHandler for http-based namespace
            // declarations, especially for the default namespace handler.
            if (namespaceContentHandler == null ||!prefix.isEmpty())
            {
                // establish a NamespaceContentHandler for the prefix and URI in the current scope
                namespaceContentHandler = getCurrentScope().establishNamespaceContentHandlerFor(prefix, uri);
            }
        }

        return namespaceContentHandler;
    }


    /**
     * {@inheritDoc}
     */
    public Environment getEnvironment()
    {
        return environment;
    }


    /**
     * {@inheritDoc}
     */
    public NamespaceContentHandler getNamespaceContentHandler(String prefix)
    {
        NamespaceContentHandler namespaceContentHandler = null;

        for (Iterator<Scope> scopeIterator = scopes.iterator();
            scopeIterator.hasNext() && namespaceContentHandler == null; )
        {
            Scope scope = scopeIterator.next();
            URI   uri   = scope.getNamespaceURI(prefix);

            namespaceContentHandler = uri == null ? null : getNamespaceContentHandler(uri);
        }

        return namespaceContentHandler;
    }


    /**
     * {@inheritDoc}
     */
    public NamespaceContentHandler getNamespaceContentHandler(URI uri)
    {
        NamespaceContentHandler namespaceContentHandler = null;

        for (Iterator<Scope> scopeIterator = scopes.iterator();
            scopeIterator.hasNext() && namespaceContentHandler == null; )
        {
            Scope scope = scopeIterator.next();

            namespaceContentHandler = scope.getNamespaceContentHandler(uri);
        }

        return namespaceContentHandler;
    }


    /**
     * {@inheritDoc}
     */
    public URI getNamespaceURI(String prefix)
    {
        return getCurrentScope().getNamespaceURI(prefix);
    }


    /**
     * {@inheritDoc}
     */
    public ClassLoader getClassLoader()
    {
        return getEnvironment().getClassLoader();
    }


    /**
     * Returns the current scope (the top of the stack).
     *
     * @return A {@link Scope} (which is the top of the stack).
     */
    private Scope getCurrentScope()
    {
        return this.scopes.getFirst();
    }


    /**
     * Pushes a new empty scope onto the scopes stack so that newly encountered namespaces
     * are managed in a new scope, thus overriding previously defined namespaces.
     *
     * @param xmlElement The {@link XmlElement} for which we are starting a new scope. (may be null)
     */
    private void startScope(XmlElement xmlElement)
    {
        this.scopes.addFirst(new Scope(this, xmlElement));
    }


    /**
     * Terminates the current scope (cleans up it's {@link NamespaceContentHandler}s)
     * and removes it from the stack.
     */
    private void endScope()
    {
        getCurrentScope().terminate();
        this.scopes.removeFirst();
    }


    /**
     * {@inheritDoc}
     */
    public Object processDocument(XmlElement xmlElement) throws ConfigurationException
    {
        // replace all of the "system-property" uses in the document root
        XmlHelper.replaceSystemProperties(xmlElement, "system-property");

        // now process the document root element
        return processElement(xmlElement);
    }


    /**
     * {@inheritDoc}
     */
    public Object processDocument(String xml) throws ConfigurationException
    {
        return processDocument(XmlHelper.loadXml(xml));
    }


    /**
     * {@inheritDoc}
     */
    public Object processDocumentAt(URI uri) throws ConfigurationException
    {
        return processDocument(DefaultConfigurableCacheFactory.loadConfigAsResource(uri.toString(), getClassLoader()));
    }


    /**
     * {@inheritDoc}
     */
    public Object processDocumentAt(String location) throws ConfigurationException
    {
        return processDocument(DefaultConfigurableCacheFactory.loadConfigAsResource(location, getClassLoader()));
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Object processElement(XmlElement xmlElement) throws ConfigurationException
    {
        // start a new namespace scope as we're processing a new xmlElement
        startScope(xmlElement);

        // pre-process the special xmlns attribute declarations
        for (String attributeName : ((Set<String>) xmlElement.getAttributeMap().keySet()))
        {
            QualifiedName attributeQName = new QualifiedName(attributeName);

            if (attributeQName.getPrefix().equals("xmlns")
                || (!attributeQName.hasPrefix() && attributeQName.getLocalName().equals("xmlns")))
            {
                // ensure we have a NamespaceContentHandler for the declared xmlns
                XmlValue uriValue = xmlElement.getAttribute(attributeName);

                try
                {
                    ensureNamespaceContentHandler(attributeQName.hasPrefix() ? attributeQName.getLocalName() : "",
                                                  new URI(uriValue.getString()));
                }
                catch (URISyntaxException uriSyntaxException)
                {
                    throw new RuntimeException(String.format("Invalid URI '%s' specified for XML namespace '%s'",
                                                             uriValue.getString(), attributeQName.getPrefix()),
                                               uriSyntaxException);
                }
            }
        }

        // determine the QualifiedName of the element
        QualifiedName elementQName = new QualifiedName(xmlElement);

        // locate an appropriate namespace content handler for the element
        NamespaceContentHandler namespaceContentHandler = getNamespaceContentHandler(elementQName.getPrefix());

        if (namespaceContentHandler == null ||!(namespaceContentHandler instanceof ElementContentHandler))
        {
            throw new RuntimeException(String
                .format("An ElementContentHandler is not available for the element [%s].  Please check that the namespace has been correctly defined",
                        elementQName));
        }

        // process each of the attributes for the element processed using their appropriate attribute content handlers
        Map<String, XmlValue> attributeMap = (Map<String, XmlValue>) (xmlElement.getAttributeMap());

        for (String key : attributeMap.keySet())
        {
            QualifiedName attributeQName = new QualifiedName(key);
            NamespaceContentHandler attributeNamespaceContentHandler =
                getNamespaceContentHandler(attributeQName.getPrefix());

            if (!attributeQName.getPrefix().equals("xmlns") && attributeNamespaceContentHandler == null)
            {
                throw new RuntimeException(String
                    .format("An AttributeContentHandler is not available for the attribute [%s] in the namespace [%s], in the element [%s].  Please check that the namespace has been correctly defined",
                            attributeQName, attributeQName.getPrefix(), elementQName));
            }
            else
            {
                if (attributeNamespaceContentHandler != null
                    && attributeNamespaceContentHandler instanceof AttributeContentHandler)
                {
                    ((AttributeContentHandler) attributeNamespaceContentHandler).onAttribute(this,
                                                                                             attributeQName,
                                                                                             attributeMap.get(key));
                }
            }
        }

        // handle the element itself to produce the result
        Object result = ((ElementContentHandler) namespaceContentHandler).onElement(this, elementQName, xmlElement);

        // end the current scope for the element
        endScope();

        return result;
    }


    /**
     * {@inheritDoc}
     */
    public Object processElement(String xml) throws ConfigurationException
    {
        return processElement(XmlHelper.loadXml(xml));
    }


    /**
     * {@inheritDoc}
     */
    public Object processOnlyElementOf(XmlElement xmlElement) throws ConfigurationException
    {
        // the xmlElement must only have a single child
        if (xmlElement.getElementList().size() == 1)
        {
            // determine the scheme from the child element
            return this.processElement((XmlElement) xmlElement.getElementList().get(0));
        }
        else
        {
            // expected only a single element in custom-provider
            throw new ConfigurationException(String.format("Only a single element is permitted in the %s element.",
                                                           xmlElement),
                                             String
                                             .format("Please consult the documentation regarding use of the '%s' namespace",
                                                 new QualifiedName(xmlElement).getPrefix()));
        }
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T> T processElementOf(XmlElement    xmlElement,
                                  QualifiedName qualifiedName,
                                  T             defaultValue) throws ConfigurationException
    {
        // find the child element using the qualified name
        XmlElement childElement = xmlElement.getElement(qualifiedName.toString());
        T          result;

        if (childElement == null)
        {
            // no such element, so use the default
            result = defaultValue;

            if (logger.isLoggable(Level.FINEST))
            {
                logger.config(String.format("Using the default value of [%s] for the xml element <%s> in element <%s>",
                                            defaultValue, qualifiedName.getName(), xmlElement.getName()));
            }
        }
        else
        {
            result = (T) processElement(childElement);
        }

        return result;
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T> T processElementOf(XmlElement xmlElement,
                                  String     id,
                                  T          defaultValue) throws ConfigurationException
    {
        // find the element with the specified id
        XmlElement childElement = null;

        for (Iterator<XmlElement> children = (Iterator<XmlElement>) xmlElement.getElementList().iterator();
            children.hasNext() && childElement == null; )
        {
            XmlElement anElement  = children.next();
            XmlValue   idXmlValue = anElement.getAttribute("id");

            if (idXmlValue != null && idXmlValue.getString().equals(id))
            {
                childElement = anElement;
            }
        }

        T result;

        if (childElement == null)
        {
            // no such element, so use the default
            result = defaultValue;

            if (logger.isLoggable(Level.FINEST))
            {
                logger.config(String
                    .format("Using the default value of [%s] for the xml element with id='%s' in element <%s>",
                            defaultValue, id, xmlElement.getName()));
            }
        }
        else
        {
            result = (T) processElement(childElement);
        }

        return result;
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Map<String, ?> processElementsOf(XmlElement       xmlElement,
                                            QualifiedName... except) throws ConfigurationException
    {
        HashSet<QualifiedName> exceptions = new HashSet<QualifiedName>();

        for (QualifiedName exception : except)
        {
            exceptions.add(exception);
        }

        // process all of the children of the xmlElement
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();

        for (Iterator<XmlElement> children = (Iterator<XmlElement>) xmlElement.getElementList().iterator();
            children.hasNext(); )
        {
            XmlElement    childElement = children.next();

            QualifiedName childQName   = new QualifiedName(childElement);

            if (!exceptions.contains(childQName))
            {
                String id = childElement.getAttributeMap().containsKey("id")
                            ? childElement.getAttribute("id").getString() : new UUID().toString();

                if (id.trim().length() == 0)
                {
                    id = new UUID().toString();
                }

                result.put(id, processElement(childElement));
            }
        }

        return result;
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> Map<String, T> processElementsOf(XmlElement xmlElement,
                                                Class<T>   expectedType) throws ConfigurationException
    {
        LinkedHashMap<String, T> result = new LinkedHashMap<String, T>();

        for (Iterator<XmlElement> children = (Iterator<XmlElement>) xmlElement.getElementList().iterator();
            children.hasNext(); )
        {
            XmlElement childElement = children.next();

            // determine a suitable id for the child
            String id = childElement.getAttributeMap().containsKey("id")
                        ? childElement.getAttribute("id").getString() : new UUID().toString();

            if (id.trim().length() == 0)
            {
                id = new UUID().toString();
            }

            // process the child
            Object child = processElement(childElement);

            // ensure it's of the expected type
            if (expectedType.isAssignableFrom(child.getClass()))
            {
                result.put(id, (T) child);
            }
            else
            {
                throw new ConfigurationException(String
                    .format("The child element %s of the %s element did not produce the expected type %s when processed.",
                            childElement, xmlElement, expectedType),
                                                 String
                                                 .format("Please consult the documentation regarding use of the '%s' namespace",
                                                     new QualifiedName(xmlElement).getPrefix()));
            }
        }

        return result;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPropertyDefined(String        propertyName,
                                     QualifiedName qualifiedName,
                                     XmlElement    xmlElement) throws ConfigurationException
    {
        // attempt to find an attribute with the specified name
        // when not found, attempt to find an element with the specified property name
        if (xmlElement.getAttribute(propertyName) == null)
        {
            // attempt to process the element with the specified name
            QualifiedName propertyQName = new QualifiedName(qualifiedName.getPrefix(), propertyName);

            // does the element exist (in the current namespace)?
            if (xmlElement.getElement(propertyQName.getName()) == null)
            {
                // does the element exist in the default namespace?
                propertyQName = new QualifiedName("", propertyName);

                return xmlElement.getElement(propertyQName.getName()) != null;
            }
            else
            {
                return true;
            }
        }
        else
        {
            return true;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(Object        object,
                          QualifiedName qualifiedName,
                          XmlElement    xmlElement) throws ConfigurationException
    {
        if (logger.isLoggable(Level.FINER))
        {
            LogHelper.entering(logger, DefaultConfigurationContext.class.getName(), "configure");
        }

        // use reflection to configure the object
        for (Method method : object.getClass().getMethods())
        {
            // can the method be called to configure a value?
            if (method.isAnnotationPresent(Property.class))
            {
                // ensure the method has a single parameter
                if (method.getParameterTypes().length == 1)
                {
                    // get the property name from the annotation
                    Property property     = method.getAnnotation(Property.class);
                    String   propertyName = property.value();

                    // is the property defined?
                    if (isPropertyDefined(propertyName, qualifiedName, xmlElement))
                    {
                        // determine the type of the property
                        Class<?> propertyType = method.getAnnotation(Type.class) == null
                                                ? method.getParameterTypes()[0]
                                                : method.getAnnotation(Type.class).value();

                        // get the sub-type for the property
                        Class<?> propertySubType = method.getAnnotation(SubType.class) == null
                                                   ? null : method.getAnnotation(SubType.class).value();

                        // attempt to get the value for the property
                        Object propertyValue = getMandatoryProperty(propertyName,
                                                                    propertyType,
                                                                    propertySubType,
                                                                    qualifiedName,
                                                                    xmlElement);

                        // now attempt to set the property in the object
                        try
                        {
                            method.invoke(object, propertyValue);
                        }
                        catch (Exception exception)
                        {
                            // could not set the property via reflection
                            throw new ConfigurationException(String
                                .format("Could not set the property '%s' using reflection against the annotated method '%s' of '%s'",
                                        propertyName, method, object.getClass().getName()),
                                                             String.format("Please resolve the causing exception.",
                                                                           getNamespaceContentHandler(qualifiedName
                                                                               .getPrefix()).getClass().getName()),
                                                             exception);
                        }
                    }
                    else if (isMandatory(method, xmlElement))
                    {
                        throw new ConfigurationException(String
                            .format("The mandatory property '%s' is not defined in element '%s'.", propertyName,
                                    xmlElement),
                                                         String
                                                         .format("Please consult the documentation for the use of the %s namespace",
                                                             qualifiedName.getPrefix()));
                    }
                    else
                    {
                        if (logger.isLoggable(Level.FINER))
                        {
                            logger.finer(String
                                .format("The property '%s' does not have a defined value in the element '%s'. Will use the default value.",
                                        propertyName, xmlElement));
                        }
                    }
                }
            }
        }

        if (logger.isLoggable(Level.FINER))
        {
            LogHelper.exiting(logger, DefaultConfigurationContext.class.getName(), "configure");
        }
    }


    /**
     * Determine whether a method is marked as mandatory via the {@link Mandatory}
     * annotation. iff a {@code scheme-ref} exists can the {@link Mandatory}
     * annotation be bypassed as the scheme definition is defined else where.
     *
     * @param m Method to check against
     * @param element xml configuration
     * @return whether the method is required
     */
    protected boolean isMandatory(final Method     m,
                                  final XmlElement element)
    {
        return m.getAnnotation(Mandatory.class) != null && element.getElement("scheme-ref") == null;
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getMandatoryProperty(String        propertyName,
                                      Class<T>      propertyType,
                                      Class<?>      propertySubType,
                                      QualifiedName qualifiedName,
                                      XmlElement    xmlElement) throws ConfigurationException
    {
        // attempt to find an attribute with the specified name
        Value value = new Value(xmlElement.getAttribute(propertyName));

        // when not found, attempt to find an element with the specified property name
        if (value.isNull())
        {
            // attempt to process the element with the specified name
            QualifiedName propertyQName = new QualifiedName(qualifiedName.getPrefix(), propertyName);

            // does the element exist (in the current namespace)?
            value = new Value(xmlElement.getElement(propertyQName.getName()));

            if (value.isNull())
            {
                // does the element exist in the default namespace?
                propertyQName = new QualifiedName("", propertyName);
                value         = new Value(xmlElement.getElement(propertyQName.getName()));
            }
        }

        if (value.isNull())
        {
            throw new ConfigurationException(String
                .format("The expected property '%s' is not defined in element '%s'.", propertyName, xmlElement),
                                             String
                                             .format("Please consult the documentation for the use of the %s namespace",
                                                 qualifiedName.getPrefix()));
        }
        else
        {
            T propertyValue;

            // can the value be coerced into the required property type automatically
            if (value.hasCoercerFor(propertyType))
            {
                try
                {
                    propertyValue = value.getValue(propertyType);
                }
                catch (ClassCastException classCastException)
                {
                    // the property value could not be coerced into the required type
                    throw new ConfigurationException(String
                        .format("Incompatible Types: The specified value '%s' for the property '%s' in element '%s' can not be converted into the required property type '%s'.",
                                value.getString(), propertyName, value.getObject(), propertyType),
                                                     String
                                                     .format("Please ensure a correct type of value is specified for the property '%s' in the namespace '%s'.",
                                                         propertyName, qualifiedName.getPrefix()),
                                                     classCastException);
                }
            }
            else if (propertyType == Expression.class)
            {
                // is there structure to the value? (we may need to process it)
                if (value.getObject() instanceof XmlElement
                    && ((XmlElement) value.getObject()).getElementList().size() == 1)
                {
                    try
                    {
                        // the value is an element, so process the element to produce the expression value
                        Object processedValue = processOnlyElementOf(((XmlElement) value.getObject()));

                        // when the processedValue is an expression, just use it as is.
                        if (processedValue instanceof Expression)
                        {
                            propertyValue = (T) processedValue;
                        }
                        else
                        {
                            // the processed value isn't an expression, so make it a constant expression
                            propertyValue = (T) new Constant(processedValue);
                        }
                    }
                    catch (ConfigurationException configurationException)
                    {
                        // the type of the property is unknown/unsupported
                        throw new ConfigurationException(String
                            .format("The type of the '%s' property is not supported for declarative configuration.",
                                    propertyName),
                                                         String
                                                         .format("The namespace implementation '%s' will need to programmatically configure the said property.",
                                                             getNamespaceContentHandler(qualifiedName.getPrefix())
                                                                 .getClass().getName()),
                                                         configurationException);
                    }
                }
                else
                {
                    // FUTURE: In the future we should use an ExpressionFactory to construct an implementation
                    // of an Expression.  This would allow us to replace the Expression evaluation engine.
                    // At the moment it's hard coded to be support only the Coherence Macro Parameters.

                    // the value isn't structured, so create an expression from the string of the value
                    propertyValue = (T) new MacroParameterExpression(value.getString());
                }
            }
            else
            {
                // attempt to resolve the value as an element
                if (value.getObject() instanceof XmlElement)
                {
                    try
                    {
                        // process the element to determine the property value
                        propertyValue = (T) processElement((XmlElement) value.getObject());
                    }
                    catch (ConfigurationException configurationException)
                    {
                        // the type of the property is unknown/unsupported
                        throw new ConfigurationException(String
                            .format("The type of the '%s' property is not supported for declarative configuration.",
                                    propertyName),
                                                         String
                                                         .format("The namespace implementation '%s' will need to programmatically configure the said property.",
                                                             getNamespaceContentHandler(qualifiedName.getPrefix())
                                                                 .getClass().getName()),
                                                         configurationException);
                    }

                    // ensure that the property value is compatible with the declared property type
                    if (propertyValue != null)
                    {
                        // is the property value compatible with the setter method property type?
                        if (propertyType.isAssignableFrom(propertyValue.getClass()))
                        {
                            // is an annotated property type specified
                            if (propertySubType != null)
                            {
                                // for ReflectiveScheme properties, ensure that the realized value
                                // is of a compatible type.
                                if ((propertyValue instanceof ReflectiveBuilder<?>
                                    &&!((ReflectiveBuilder<?>) propertyValue)
                                        .realizesClassOf(propertySubType,
                                            SystemPropertyParameterProvider.INSTANCE)) || (!(propertyValue
                                                instanceof ReflectiveBuilder<?>) &&!propertySubType
                                                    .isAssignableFrom(propertyValue.getClass())))
                                {
                                    // the property value does not match with the specified property type
                                    throw new ConfigurationException(String
                                        .format("Incompatible Types: The specified value '%s' for the property '%s' in element '%s' is incompatible with the required '%s' property type.",
                                                propertyValue, propertyName, value.getObject(), propertySubType),
                                                                     String
                                                                     .format("The namespace implementation '%s' will need to programmatically configure the said property.",
                                                                         getNamespaceContentHandler(qualifiedName
                                                                             .getPrefix()).getClass().getName()));
                                }
                            }
                        }

                        // is the property value a class scheme that can be realized now to produce the expected type?
                        else if (propertyValue instanceof ParameterizedBuilder<?>
                                 && ((ParameterizedBuilder<?>) propertyValue)
                                     .realizesClassOf(propertyType, SystemPropertyParameterProvider.INSTANCE))
                        {
                            propertyValue =
                                (T) ((ParameterizedBuilder<?>) propertyValue)
                                    .realize(SystemPropertyParameterProvider.INSTANCE);
                        }
                        else
                        {
                            // the type of the value does not match the type of the parameter
                            throw new ConfigurationException(String
                                .format("Incompatible Types: The specified value '%s' for the property '%s' in element %s is incompatible with the declared type (by the property setter method).",
                                        propertyValue, propertyName, value.getString()),
                                                             String
                                                             .format("The namespace implementation '%s' will need to programmatically configure the said property.",
                                                                 getNamespaceContentHandler(qualifiedName.getPrefix())
                                                                     .getClass().getName()));
                        }
                    }
                }
                else
                {
                    // the type of the property is unknown/unsupported
                    throw new ConfigurationException(String
                        .format("The type of the '%s' property for the element '%s' is not supported for declarative configuration.",
                                propertyName, xmlElement),
                                                     String
                                                     .format("The namespace implementation '%s' will need to programmatically configure the said property.",
                                                         getNamespaceContentHandler(qualifiedName.getPrefix())
                                                             .getClass().getName()));
                }
            }

            return (T) propertyValue;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getMandatoryProperty(String        propertyName,
                                      Class<T>      propertyType,
                                      QualifiedName qualifiedName,
                                      XmlElement    xmlElement) throws ConfigurationException
    {
        return getMandatoryProperty(propertyName, propertyType, null, qualifiedName, xmlElement);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getOptionalProperty(String             propertyName,
                                     java.lang.Class<T> propertyType,
                                     java.lang.Class<?> propertySubType,
                                     T                  defaultValue,
                                     QualifiedName      qualifiedName,
                                     XmlElement         xmlElement) throws ConfigurationException
    {
        if (isPropertyDefined(propertyName, qualifiedName, xmlElement))
        {
            return getMandatoryProperty(propertyName, propertyType, propertySubType, qualifiedName, xmlElement);
        }
        else
        {
            return defaultValue;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getOptionalProperty(String             propertyName,
                                     java.lang.Class<T> propertyType,
                                     T                  defaultValue,
                                     QualifiedName      qualifiedName,
                                     XmlElement         xmlElement) throws ConfigurationException
    {
        if (isPropertyDefined(propertyName, qualifiedName, xmlElement))
        {
            return getMandatoryProperty(propertyName, propertyType, qualifiedName, xmlElement);
        }
        else
        {
            return defaultValue;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public <T, N> boolean hasEnrichment(Class<T> type,
                                        N        decorationKey)
    {
        return getCurrentScope().hasEnrichment(type, decorationKey);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public <T, N> T getEnrichment(Class<T> type,
                                  N        decorationKey)
    {
        T decoration = null;

        for (Iterator<Scope> scopeIterator = scopes.iterator(); scopeIterator.hasNext() && decoration == null; )
        {
            Scope scope = scopeIterator.next();

            decoration = scope.getEnrichment(type, decorationKey);
        }

        return decoration;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<?> getEnrichmentKeys(Class<?> type)
    {
        return getCurrentScope().getEnrichmentKeys(type);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Iterable<T> getEnrichments(Class<T> type)
    {
        return getCurrentScope().getEnrichments(type);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<Class<?>> getEnrichmentTypes()
    {
        return getCurrentScope().getEnrichmentTypes();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public <T, N> void addEnrichment(Class<T> type,
                                     N        decorationKey,
                                     T        decoration)
    {
        getCurrentScope().addEnrichment(type, decorationKey, decoration);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void addEnrichmentsFrom(EnrichmentSupport decorationSupport)
    {
        getCurrentScope().addEnrichmentsFrom(decorationSupport);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        String result = "DefaultConfigurationContext{\n";

        for (Scope scope : scopes)
        {
            result += scope + "\n";
        }

        return result + "}";
    }


    /**
     * A {@link Scope} maintains the currently defined namespace prefixes and associated
     * {@link NamespaceContentHandler}s for an {@link XmlElement} that is being processed.
     */
    private class Scope extends AbstractEnrichmentSupport
    {
        /**
         * The {@link ConfigurationContext} in which the {@link Scope} exists.
         */
        private ConfigurationContext context;

        /**
         * The {@link XmlElement} for which this scope was created. (may be null).
         */
        private XmlElement xmlElement;

        /**
         * The xml namespaces registered in this {@link Scope}.  A mapping from prefix to URIs
         */
        private HashMap<String, URI> namespaces;

        /**
         * The {@link NamespaceContentHandler}s for the xml namespaces registered in this {@link Scope}.
         */
        private HashMap<URI, NamespaceContentHandler> namespaceContentHandlers;


        /**
         * Standard Constructor.
         *
         * @param context The {@link ConfigurationContext} that owns the {@link Scope}
         * @param xmlElement The {@link XmlElement} for which this {@link Scope} was created. (may be null)
         */
        Scope(ConfigurationContext context,
              XmlElement           xmlElement)
        {
            this.context                  = context;
            this.xmlElement               = xmlElement;
            this.namespaces               = new HashMap<String, URI>();
            this.namespaceContentHandlers = new HashMap<URI, NamespaceContentHandler>();
        }


        /**
         * Returns the {@link XmlElement} for which the {@link Scope} was created.
         *
         * @return An {@link XmlElement} or <code>null</code>
         */
        XmlElement getXmlElement()
        {
            return xmlElement;
        }


        /**
         * Determines the {@link URI} of the specified prefix registered in this {@link Scope}.
         *
         * @param prefix The prefix of the {@link URI} to determine.
         *
         * @return The {@link URI} of the registered prefix or <code>null</code> if none defined in the {@link Scope}.
         */
        URI getNamespaceURI(String prefix)
        {
            return namespaces.get(prefix);
        }


        /**
         * Determines the {@link NamespaceContentHandler} for the {@link URI} registered in the {@link Scope}.
         *
         * @param uri The {@link URI} of the {@link NamespaceContentHandler} to determine.
         *
         * @return The {@link NamespaceContentHandler} of the {@link URI} or <code>null</code> if none registered.
         */
        NamespaceContentHandler getNamespaceContentHandler(URI uri)
        {
            return namespaceContentHandlers.get(uri);
        }


        /**
         * Establishes (including loading) an appropriate {@link NamespaceContentHandler}
         * for the specified prefix and {@link URI}.
         *
         * @param prefix The prefix used to declare the namespace
         * @param uri The {@link URI} of which we need to establish a {@link NamespaceContentHandler}.
         *
         * @return The {@link NamespaceContentHandler} for the specified {@link URI}.
         */
        NamespaceContentHandler establishNamespaceContentHandlerFor(String prefix,
                                                                    URI    uri)
        {
            String scheme = uri.getScheme();

            // ensure that we don't already have a definition for the prefix in this scope
            if (namespaces.containsKey(prefix))
            {
                throw new RuntimeException(String
                    .format("Duplicate definition for the namespace prefix [%s] encountered with URI [%s]\n", prefix,
                            uri));
            }
            else if (scheme.equalsIgnoreCase("class"))
            {
                String fqcn = uri.getHost() == null ? uri.getSchemeSpecificPart() : uri.getHost();

                try
                {
                    Class<?> clazz = ExternalizableHelper.loadClass(fqcn, context.getClass().getClassLoader(), null);

                    // ensure that the class is a NamespaceHandler
                    if (NamespaceContentHandler.class.isAssignableFrom(clazz))
                    {
                        try
                        {
                            // instantiate and initialize the namespace handler
                            NamespaceContentHandler namespaceHandler = (NamespaceContentHandler) clazz.newInstance();

                            // tell the namespace about the context and it's uri
                            namespaceHandler.onStartScope(context, prefix, uri);

                            // register the NamespaceContentHandler with this scope
                            namespaceContentHandlers.put(uri, namespaceHandler);

                            // register the prefix for the namespace
                            namespaces.put(prefix, uri);

                            return namespaceHandler;
                        }
                        catch (Exception exception)
                        {
                            throw new RuntimeException(String
                                .format("Can't instantiate the NamespaceContentHandler [%s]\n", fqcn),
                                                       exception);
                        }
                    }
                    else
                    {
                        throw new RuntimeException(String
                            .format("The declared NamespaceContentHandler [%s] does not implement the %s interface\n",
                                    fqcn, NamespaceContentHandler.class.getName()));
                    }
                }
                catch (ClassNotFoundException classNotFoundException)
                {
                    throw new RuntimeException(String
                        .format("Can't instantiate the NamespaceContentHandler [%s] as the class is not found\n", fqcn),
                                               classNotFoundException);
                }
            }
            else if (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
            {
                // instantiate a NullNamespaceContentHandler for the namespace
                NamespaceContentHandler namespaceContentHandler = new NullNamespaceContentHandler();

                // register the
                namespaceContentHandlers.put(uri, namespaceContentHandler);

                // register the prefix for the namespace
                namespaces.put(prefix, uri);

                return namespaceContentHandler;
            }
            else
            {
                throw new RuntimeException(String
                    .format("Can't create a NamespaceContentHandler as the URI scheme [%s] in [%s] is not supported\n",
                            uri.getScheme(), uri));
            }
        }


        /**
         * Terminates and end's all of the {@link NamespaceContentHandler}s established in the {@link Scope}.
         */
        void terminate()
        {
            // end each of the namespace handlers defined in this scope
            for (String prefix : namespaces.keySet())
            {
                URI uri = namespaces.get(prefix);

                namespaceContentHandlers.get(uri).onEndScope(context, prefix, uri);
            }
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
        {
            String result = "";

            result += "Scope<" + (getXmlElement() == null ? "root" : xmlElement.getName()) + ">{";

            if (!namespaces.isEmpty())
            {
                result += "namespaces=" + namespaces;
            }

            if (!namespaceContentHandlers.isEmpty())
            {
                result += "handlers=" + namespaceContentHandlers;
            }

            return result + "}";
        }
    }
}
