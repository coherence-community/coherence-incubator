/*
 * File: SerializableInstanceBuilder.java
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

package com.oracle.coherence.common.builders;


import com.oracle.coherence.common.expression.SerializableExpressionHelper;
import com.oracle.coherence.common.expression.SerializableLiteralExpression;
import com.oracle.coherence.common.expression.SerializableResolvableParameterList;
import com.oracle.coherence.common.expression.SerializableSimpleParameterList;
import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Constructor;

import static com.tangosol.coherence.config.builder.ParameterizedBuilderHelper.getAssignableValue;

/**
 * An {@link SerializableInstanceBuilder} is a serializable {@link ParameterizedBuilder}
 * implementation that additionally supports injection based on Coherence
 * &lt;instance%gt; or &lt;class-scheme&gt; configurations.
 * <p/>
 * While supporting injection this class may also be used in situations where
 * a {@link ParameterizedBuilder} is required (must be passed) for a known
 * type of the class.
 * <p/>
 * For example, if you need a {@link ParameterizedBuilder} for a
 * java.awt.Point class, but you don't want to create an anonymous {@link
 * ParameterizedBuilder} implementation for a Point, you can use the
 * following:
 * <p/>
 * <code>new InstanceBuilder(Point.class);</code>
 * <p/>
 * Further you may also provide constructor parameters as follows:
 * <p/>
 * <code>new InstanceBuilder(Point.class, 10, 12);</code>
 *
 * @author Brian Oliver
 * @author Paul Mackin
 */
public class SerializableInstanceBuilder<T> implements ParameterizedBuilder<T>,
                                                       ParameterizedBuilder.ReflectionSupport,
                                                       ExternalizableLite,
                                                       PortableObject
{
    /**
     * An expression that when evaluated will return the name of the
     * class to realize.
     */
    private Expression<String> exprCacheName;

    /**
     * The {@link ParameterList} containing the constructor {@link
     * Parameter}s for realizing the class.
     */
    private ParameterList constructorParameters;


    /**
     * Construct an {@link SerializableInstanceBuilder}.
     */
    public SerializableInstanceBuilder()
    {
        exprCacheName         = new SerializableLiteralExpression<String>("undefined");
        constructorParameters = new SerializableResolvableParameterList();
    }


    /**
     * Constructs a {@link SerializableInstanceBuilder}
     * that will realize an instance of the specified {@link Class}.
     *
     * @param clzToRealize           the {@link Class} to realize
     * @param aConstructorParameters the optional constructor parameters
     */
    public SerializableInstanceBuilder(Class<?>  clzToRealize,
                                       Object... aConstructorParameters)
    {
        exprCacheName         = new SerializableLiteralExpression<String>(clzToRealize.getName());
        constructorParameters = new SerializableSimpleParameterList(aConstructorParameters);
    }


    /**
     * Constructs a {@link SerializableInstanceBuilder}
     * that will realize an instance of the specifically named class.
     *
     * @param exprClassName          an {@link Expression} that when
     *                               evaluated will return the class name to
     *                               realize
     * @param aConstructorParameters the optional constructor parameters
     */
    public SerializableInstanceBuilder(Expression<String> exprClassName,
                                       Object...          aConstructorParameters)
    {
        exprCacheName         = exprClassName;
        constructorParameters = new SerializableSimpleParameterList(aConstructorParameters);
    }


    /**
     * Constructs a {@link SerializableInstanceBuilder}
     * that will realize an instance of the specifically named class.
     *
     * @param sClassName             the name of the class to realize
     * @param aConstructorParameters the optional constructor parameters
     */
    public SerializableInstanceBuilder(String    sClassName,
                                       Object... aConstructorParameters)
    {
        exprCacheName         = new SerializableLiteralExpression<String>(sClassName);
        constructorParameters = new SerializableSimpleParameterList(aConstructorParameters);
    }


    /**
     * Return the expression representing the name of the class this builder
     * will attempt to instantiate.
     *
     * @return an expression representing the class name
     */
    public Expression<String> getClassName()
    {
        return exprCacheName;
    }


    /**
     * Sets the {@link Expression} that when evaluated will produce the name
     * of the class to realize.
     *
     * @param exprClassName the {@link Expression}
     */
    @Injectable("class-name")
    public void setClassName(Expression<String> exprClassName)
    {
        exprCacheName = SerializableExpressionHelper.ensureSerializable(exprClassName);
    }


    /**
     * Return the {@link ParameterList} to be used for constructor parameters
     * when realizing the class.
     *
     * @return the {@link ParameterList} for the constructor
     */
    public ParameterList getConstructorParameterList()
    {
        return constructorParameters;
    }


    /**
     * Sets the {@link ParameterList} to be used for constructor parameters
     * when realizing the class.
     *
     * @param listParameters the {@link ParameterList} for the constructor
     */
    @Injectable("init-params")
    public void setConstructorParameterList(ParameterList listParameters)
    {
        constructorParameters = listParameters;
    }


    /**
     * Ensures we have a non-null {@link ClassLoader} that we can use for
     * loading classes.
     *
     * @param loader the proposed {@link ClassLoader}, which may be
     *               <code>null</code>
     *
     * @return a non-null {@link ClassLoader}
     */
    protected ClassLoader ensureClassLoader(ClassLoader loader)
    {
        return (loader == null) ? getClass().getClassLoader() : loader;
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public T realize(ParameterResolver resolver,
                     ClassLoader       loader,
                     ParameterList     listConstructorParameters)
    {
        try
        {
            // ensure we have a classloader
            loader = ensureClassLoader(loader);

            // resolve the actual class name
            String sClassName = exprCacheName.evaluate(resolver);

            // attempt to load the class
            Class<?> clzClass = loader.loadClass(sClassName);

            // determine which list of parameters to use
            ParameterList listParameters = (listConstructorParameters == null)
                                           ? constructorParameters : listConstructorParameters;

            // find a constructor that is compatible with the constructor parameters
            Constructor<?>[] aConstructors          = clzClass.getConstructors();
            int              cConstructors          = aConstructors.length;
            int              cConstructorParameters = listParameters.size();
            Object[] aConstructorParameters = (cConstructorParameters == 0) ? null : new Object[cConstructorParameters];

            // assume a compatible constructor is not found
            Constructor<?> constructor = null;

            for (int i = 0; (i < cConstructors) && (constructor == null); i++)
            {
                if (aConstructors[i].getParameterTypes().length == cConstructorParameters)
                {
                    // ensure that the constructor parameter types are compatible
                    try
                    {
                        Class<?>[] aParameterTypes = aConstructors[i].getParameterTypes();
                        int        j               = 0;

                        for (Parameter parameter : listParameters)
                        {
                            aConstructorParameters[j] = getAssignableValue(aParameterTypes[j], parameter, resolver,
                                                                           loader);
                            j++;
                        }

                        constructor = aConstructors[i];
                    }
                    catch (Exception e)
                    {
                        // an exception means the constructor is not compatible
                    }
                }
            }

            // did we find a compatible constructor?
            if (constructor == null)
            {
                throw new NoSuchMethodException(String
                    .format("Unable to find a compatible constructor for [%s] with the parameters [%s]", sClassName,
                            listParameters));
            }
            else
            {
                return (T) constructor.newInstance(aConstructorParameters);
            }
        }
        catch (Exception e)
        {
            throw Base.ensureRuntimeException(e);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean realizes(Class<?>          clzClass,
                            ParameterResolver resolver,
                            ClassLoader       loader)
    {
        try
        {
            // ensure we have a classloader
            loader = ensureClassLoader(loader);

            // attempt to load the class name, but don't initialize it
            Class<?> clz = loader.loadClass(exprCacheName.evaluate(resolver));

            // ensure the class is assignment compatible with the requested class
            return clzClass.isAssignableFrom(clz);
        }
        catch (ClassNotFoundException e)
        {
            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "SerializableInstanceBuilder{" + "className=" + exprCacheName + ", " + "constructorParameters="
               + constructorParameters + '}';
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
    {
        exprCacheName         = (Expression) ExternalizableHelper.readObject(in);
        constructorParameters = (ParameterList) ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, exprCacheName);
        ExternalizableHelper.writeObject(out, constructorParameters);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
    {
        exprCacheName         = (Expression) reader.readObject(0);
        constructorParameters = (ParameterList) reader.readObject(1);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(0, exprCacheName);
        writer.writeObject(1, constructorParameters);
    }
}
