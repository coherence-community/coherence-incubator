/*
 * File: SerializableParameterMacroExpression.java
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

package com.oracle.coherence.common.expression;


import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.expression.Value;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.CacheFactory;
import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@link SerializableParameterMacroExpression} is an {@link Expression} representing the
 * use of a Coherence Parameter Macro, typically occurring with in a Coherence Cache Configuration file.
 * <p>
 * Coherence Macro Parameters are syntactically represented as follows:
 * <p>
 * <code>{parameter-name [default-value]}</code>
 * <p>
 * When a {@link SerializableParameterMacroExpression} is evaluated the parameter-name and it's associated value
 * is resolved by consulting the provided {@link ParameterResolver}.  If the parameter is resolvable, the value of the
 * resolved parameter is returned.  If it's not resolvable the default value is returned.
 * <p>
 * Note: Returned values are always coerced into the type defined by the {@link Expression}.
 *
 * @author Brian Oliver
 * @author Paul Mackin
 */
public class SerializableParameterMacroExpression<T> implements Expression<T>, PortableObject, ExternalizableLite
{
    /**
     * The type of value to be returned by the {@link Expression}.
     */
    private Class<T> clzResultType;

    /**
     * The expression to be evaluated.
     */
    private String sExpression;


    /**
     * Needed for serialization.
     */
    public SerializableParameterMacroExpression()
    {
    }


    /**
     * Construct a {@link SerializableParameterMacroExpression}.
     *
     * @param sExpression    a string representation of the {@link Expression}
     * @param clzResultType  the type of value the {@link Expression} will return when evaluated
     */
    public SerializableParameterMacroExpression(String   sExpression,
                                                Class<T> clzResultType)
    {
        this.sExpression   = sExpression;
        this.clzResultType = clzResultType;
    }


    /**
     * {@inheritDoc}
     */
    public T evaluate(ParameterResolver resolver)
    {
        // there are two styles of usage for parameter macros with coherence.
        // i).  the expression contains a single parameter macro usage (and nothing else) in which the parameter
        // is evaluated and returned.
        //
        // ii). the expression contains zero or more parameter macros in a string in which case we have to
        // resolve each parameter and replace them in the string, after which we have to evaluate the string.

        // assume we don't have a result
        Value result = null;

        // assume we  require the string based result
        boolean fUseStringBasedResult = true;

        // we may need to build a string containing the result, before we can produce a result
        StringBuilder bldr = new StringBuilder();

        for (int idx = 0; idx < sExpression.length(); idx++)
        {
            if (sExpression.startsWith("\\{", idx))
            {
                bldr.append("{");
                idx++;
                fUseStringBasedResult = true;
            }
            else if (sExpression.startsWith("\\}", idx))
            {
                bldr.append("}");
                idx++;
                fUseStringBasedResult = true;
            }
            else if (sExpression.charAt(idx) == '{')
            {
                String sParameterName;
                String sDefaultValue;
                int    idxDefaultValue = sExpression.indexOf(" ", idx + 1);

                if (idxDefaultValue >= 0)
                {
                    sParameterName = sExpression.substring(idx + 1, idxDefaultValue).trim();

                    int idxEndMacro = sExpression.indexOf("}", idxDefaultValue);

                    if (idxEndMacro > idxDefaultValue)
                    {
                        sDefaultValue = sExpression.substring(idxDefaultValue, idxEndMacro).trim();
                        idx           = idxEndMacro;
                    }
                    else
                    {
                        throw new IllegalArgumentException(String
                            .format("Invalid parameter macro definition in [%s].  Missing closing brace '}'.",
                                    sExpression));
                    }
                }
                else
                {
                    int idxEndMacro = sExpression.indexOf("}", idx + 1);

                    if (idxEndMacro > idx + 1)
                    {
                        sParameterName = sExpression.substring(idx + 1, idxEndMacro).trim();
                        sDefaultValue  = null;
                        idx            = idxEndMacro;
                    }
                    else
                    {
                        throw new IllegalArgumentException(String
                            .format("Invalid parameter macro definition in [%s].  Missing closing brace '}'.",
                                    sExpression));
                    }
                }

                Parameter parameter = resolver.resolve(sParameterName);
                Value     value;

                if (parameter == null)
                {
                    if (sDefaultValue == null)
                    {
                        throw new IllegalArgumentException(String
                            .format("The specified parameter name '%s' in the macro parameter '%s' is unknown and not resolvable",
                                    sParameterName, sExpression));
                    }
                    else
                    {
                        value = new Value(sDefaultValue);
                    }
                }
                else
                {
                    value = parameter.evaluate(resolver);
                }

                result = value == null ? new Value() : value;

                if (fUseStringBasedResult)
                {
                    try
                    {
                        bldr.append(value.get().toString());
                    }
                    catch (Exception e)
                    {
                        CacheFactory
                            .log("ParameterMacroExpression evaluation resulted in a toString Exception for class "
                                    + value.get().getClass().getName(),
                                    CacheFactory.LOG_WARN);

                        throw Base.ensureRuntimeException(e);
                    }
                }
            }
            else
            {
                bldr.append(sExpression.charAt(idx));
                fUseStringBasedResult = true;
            }
        }

        return fUseStringBasedResult
               ? new Value(bldr.toString()).as(clzResultType) : result == null ? null : result.as(clzResultType);
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("SerializableParameterMacroExpression Expression=%s ResultType=%s",
                             sExpression,
                             clzResultType);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
    {
        this.sExpression = (String) ExternalizableHelper.readUTF(in);

        String clzName = (String) ExternalizableHelper.readUTF(in);

        try
        {
            clzResultType = (Class<T>) Class.forName(clzName);
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
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeUTF(out, sExpression);
        ExternalizableHelper.writeUTF(out, clzResultType.getName());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader in) throws IOException
    {
        sExpression = in.readString(1);

        String clzName = in.readString(2);

        try
        {
            clzResultType = (Class<T>) Class.forName(clzName);
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
    public void writeExternal(PofWriter out) throws IOException
    {
        out.writeString(1, sExpression);
        out.writeString(2, clzResultType.getName());
    }
}
