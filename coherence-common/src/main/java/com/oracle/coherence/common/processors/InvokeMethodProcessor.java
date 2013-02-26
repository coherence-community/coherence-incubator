/*
 * File: InvokeMethodProcessor.java
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

package com.oracle.coherence.common.processors;

import com.oracle.coherence.common.util.ChangeIndication;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap.Entry;
import com.tangosol.util.processor.AbstractProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.NotActiveException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * The {@link InvokeMethodProcessor} is an EntryProcessor that encapsulates a method
 * call to an entry in a NamedCache.
 * <p>
 * The Method is invoked using reflection on the node where the entry lives.
 * <p>
 * If an invocation fails (throws an exception), the object is left unmodified (rollback). Note that this is potentially
 * a different semantic from calling a JVM local object where potential modifications to the object that happened before
 * the exception was thrown may remain.
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
@SuppressWarnings("serial")
public class InvokeMethodProcessor extends AbstractProcessor implements ExternalizableLite, PortableObject
{
    /**
     * The name of the method to invoke.
     */
    protected String methodName;

    /**
     * The parameter array.
     */
    protected Object parameters[];

    /**
     * A cached reflection method (to avoid repetitive look-ups).
     */
    private transient Method cachedMethod;


    /**
     * Default constructor (necessary for the ExternalizableLite and PortableObject interface).
     */
    public InvokeMethodProcessor()
    {
    }


    /**
     * Construct a MethodInvokerProcessor based on a method name.
     *
     * @param method the name of the method to invoke via reflection
     */
    public InvokeMethodProcessor(String method)
    {
        this(method, null);
    }


    /**
     * Construct a MethodInvokerProcessor based on a method name and optional parameters.
     *
     * @param methodName the name of the method to invoke via reflection
     * @param parameters the array of arguments to be used in the method invocation; may be null
     */
    public InvokeMethodProcessor(String methodName,
                                 Object parameters[])
    {
        azzert(methodName != null);
        this.methodName = methodName;
        this.parameters = parameters;
    }


    /**
     * Calculate the class array based on the parameter array.
     *
     * @return the class array based on the parameter array
     */
    private Class<?>[] getClassArray()
    {
        if (parameters != null)
        {
            int cParams = parameters.length;

            if (cParams > 0)
            {
                Class<?>[] aClass = new Class[cParams];

                for (int i = 0; i < cParams; i++)
                {
                    Object oParam = parameters[i];

                    if (oParam != null)
                    {
                        aClass[i] = oParam.getClass();
                    }
                }

                return aClass;
            }
        }

        return ClassHelper.VOID_PARAMS;
    }


    /**
     * Return the name of the method to invoke.
     *
     * @return the name of the method to invoke using reflection
     */
    public String getMethodName()
    {
        return methodName;
    }


    /**
     * Return the array of arguments used to invoke the method.
     *
     * @return the array of arguments used to invoke the method
     */
    public Object[] getParameters()
    {
        return parameters;
    }


    /**
     * Invoke the the desired method on the target class.
     *
     * @param oTarget the target class to invoke
     *
     * @return the result from invoking the target
     *
     * @throws Throwable Invoking a method may throw any number of exceptions!
     */
    private Object invoke(Object oTarget) throws Throwable
    {
        Class<?> clz = oTarget.getClass();

        try
        {
            Method method = cachedMethod;

            if (method == null || method.getDeclaringClass() != clz)
            {
                cachedMethod = method = ClassHelper.findMethod(clz, getMethodName(), getClassArray(), false);
            }

            return method.invoke(oTarget, parameters);
        }
        catch (NullPointerException e)
        {
            throw new RuntimeException(e.getMessage());
        }
        catch (InvocationTargetException ex)
        {
            throw ex.getCause();
        }
        catch (Exception e)
        {
            throw ensureRuntimeException(e, clz.getName() + this + '(' + oTarget + ')');
        }
    }


    /**
     * {@inheritDoc}
     */
    public Object process(Entry entry)
    {
        try
        {
            Object valueObject = entry.getValue();

            if (valueObject != null)
            {
                // If the object supports ChangeIndication we will use that interface
                // as a hint to decide whether we should call setValue or not
                if (valueObject instanceof ChangeIndication)
                {
                    ChangeIndication objectThatMayChange = (ChangeIndication) valueObject;

                    objectThatMayChange.beforeChange();

                    Object returnValue = invoke(objectThatMayChange);

                    if (objectThatMayChange.changed())
                    {
                        entry.setValue(objectThatMayChange);
                    }

                    return returnValue;
                }
                else
                {
                    Object returnValue = invoke(valueObject);

                    entry.setValue(valueObject);

                    return returnValue;
                }

            }
            else
            {
                return new NullPointerException("Tried to execute method:" + methodName + " but Entry is null, key:"
                                                + entry.getKey());
            }
        }
        catch (Throwable e)
        {
            return new RuntimeException(e);
        }
    }


    /**
     * Provide a human-readable description of this {@link InvokeMethodProcessor} object.
     *
     * @return a human-readable description of this {@link InvokeMethodProcessor} object
     */
    @Override
    public String toString()
    {
        Object[]     aoParam = parameters;
        int          cParams = aoParam == null ? 0 : aoParam.length;

        StringBuffer sb      = new StringBuffer(methodName);

        sb.append('(');

        for (int i = 0; i < cParams; i++)
        {
            if (i != 0)
            {
                sb.append(", ");
            }

            sb.append(String.valueOf(aoParam[i]));
        }

        sb.append(')');

        return sb.toString();
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        methodName = ExternalizableHelper.readUTF(in);

        int      cParams = ExternalizableHelper.readInt(in);
        Object[] aoParam = cParams == 0 ? null : new Object[cParams];

        for (int i = 0; i < cParams; i++)
        {
            aoParam[i] = ExternalizableHelper.readObject(in);
        }

        parameters = aoParam;
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader in) throws IOException
    {
        methodName = in.readString(0);
        parameters = in.readObjectArray(1, null);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        String sMethod = methodName;

        if (sMethod == null)
        {
            throw new NotActiveException("MethodInvokerProcessor was constructed without a method name");
        }

        Object[] aoParam = parameters;
        int      cParams = aoParam == null ? 0 : aoParam.length;

        ExternalizableHelper.writeUTF(out, sMethod);
        ExternalizableHelper.writeInt(out, cParams);

        for (int i = 0; i < cParams; i++)
        {
            ExternalizableHelper.writeObject(out, aoParam[i]);
        }
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter out) throws IOException
    {
        String sMethod = methodName;

        if (sMethod == null)
        {
            throw new NotActiveException("MethodInvokerProcessor was constructed without a method name");
        }

        out.writeString(0, sMethod);
        out.writeObjectArray(1, parameters);
    }
}
