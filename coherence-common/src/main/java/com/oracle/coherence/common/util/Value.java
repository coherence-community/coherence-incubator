/*
 * File: Value.java
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

package com.oracle.coherence.common.util;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.run.xml.XmlValue;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.UnknownFormatConversionException;

/**
 * A {@link Value} is an immutable object that represents a value whose type will only become known at runtime
 * <p>
 * Much like a <a href="http://en.wikipedia.org/wiki/Variant_type">Variant</a> (wikipedia) it allows for runtime
 * coercion into other types, as and when required.
 * <p>
 * Note: {@link Value}s are very different from Generic types in that a). they retain their type information at
 * runtime and b). may be cast/coerced into specific concrete types at runtime. {@link Value}s are thus often to used
 * to represent values of potentially unknown types at runtime, simply because {@link Value}s provide an
 * easy-to-use mechanism for coersion.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public class Value implements ExternalizableLite, PortableObject
{
    /**
     * The table of {@link Coercer} implementations for coercing {@link Value} values.
     */
    private static HashMap<Class<?>, Coercer<?>> COERCERS;

    /**
     * The value of the {@link Value}.
     */
    private Object value;


    /**
     * Standard Constructor for a <code>null</code> {@link Value}.
     */
    public Value()
    {
        this.value = null;
    }


    /**
     * Standard Constructor for a {@link Boolean}-based {@link Value}.
     *
     * @param value the boolean value of this instance
     */
    public Value(boolean value)
    {
        this.value = value;
    }


    /**
     * Standard Constructor for an {@link Object} based on another {@link Value}.
     *
     * @param value the object being the value for this {@link Value} object
     */
    public Value(Object value)
    {
        this.value = value;
    }


    /**
     * Standard Constructor for a {@link String}-based {@link Value}.
     *
     * @param value the String being the value for this {@link Value} object
     */
    public Value(String value)
    {
        this.value = value.trim();
    }


    /**
     * Standard Constructor for a {@link Value} based on another {@link Value}.
     *
     * @param value the {@link Value} embedded as the value for this {@link Value} object
     */
    public Value(Value value)
    {
        this.value = value.value;
    }


    /**
     * Standard Constructor for an {@link XmlValue}-based {@link Value}.
     *
     * @param xmlValue the {@link XmlValue} being the value for this {@link Value} object
     */
    public Value(XmlValue xmlValue)
    {
        this.value = xmlValue;
    }


    /**
     * Return if the {@link Value} represents a <code>null</code> value.
     *
     * @return <code>true</code> if the value of the {@link Value} is <code>null</code>, otherwise <code>false</code>
     */
    public boolean isNull()
    {
        return value == null;
    }


    /**
     * Return the underlying {@link Object} representation of the {@link Value} value.
     *
     * @return The {@link Object} representation of the {@link Value}. (may be <code>null</code>).
     */
    public Object getObject()
    {
        return value;
    }


    /**
     * Attempts to coerce and return the value of the {@link Value} as a {@link BigDecimal}.
     *
     * @return The value as a {@link BigDecimal}.
     *
     * @throws NumberFormatException If the value of the {@link Value} can't be cast to the require type.
     */
    public BigDecimal getBigDecimal() throws NumberFormatException
    {
        return getValue(BigDecimal.class);
    }


    /**
     * Attempts to coerce and return the value of the {@link Value} as a boolean.
     *
     * @return The value as a boolean.
     *
     * @throws UnknownFormatConversionException If the value of the {@link Value} can't be cast to the require type.
     */
    public boolean getBoolean() throws UnknownFormatConversionException
    {
        return getValue(Boolean.class);
    }


    /**
     * Attempts to coerce and return the value of the {@link Value} as a byte.
     *
     * @return The value as a byte.
     *
     * @throws NumberFormatException If the value of the {@link Value} can't be cast to the require type.
     */
    public byte getByte() throws NumberFormatException
    {
        return getValue(Byte.class);
    }


    /**
     * Attempts to coerce and return the value of the {@link Value} as a double.
     *
     * @return The value as a double.
     *
     * @throws NumberFormatException If the value of the {@link Value} can't be cast to the require type.
     */
    public double getDouble() throws NumberFormatException
    {
        return getValue(Double.class);
    }


    /**
     * Attempts to coerce and return the value of the {@link Value} as a float.
     *
     * @return The value as a float.
     *
     * @throws NumberFormatException If the value of the {@link Value} can't be cast to the require type.
     */
    public float getFloat() throws NumberFormatException
    {
        return getValue(Float.class);
    }


    /**
     * Attempts to coerce and return the value of the {@link Value} as an int.
     *
     * @return The value as a int.
     *
     * @throws NumberFormatException If the value of the {@link Value} can't be cast to the require type.
     */
    public int getInt() throws NumberFormatException
    {
        return getValue(Integer.class);
    }


    /**
     * Attempts to coerce and return the value of the {@link Value} as a long.
     *
     * @return The value as a long.
     *
     * @throws NumberFormatException If the value of the {@link Value} can't be cast to the require type.
     */
    public long getLong() throws NumberFormatException
    {
        return getValue(Long.class);
    }


    /**
     * Attempts to coerce and return the value of the {@link Value} as a short.
     *
     * @return The value as a short.
     *
     * @throws NumberFormatException If the value of the {@link Value} can't be cast to the require type.
     */
    public short getShort() throws NumberFormatException
    {
        return getValue(Short.class);
    }


    /**
     * Attempts to coerce and return the value of the {@link Value} into a String.
     *
     * @return The value as a String.
     */
    public String getString()
    {
        return getValue(String.class);
    }


    /**
     * Attempts to coerce and return the value of the {@link Value} as an {@link XmlValue}.
     *
     * @return The value as an {@link XmlValue}.
     */
    public XmlValue getXmlValue()
    {
        return getValue(XmlValue.class);
    }


    /**
     * Determines if the {@link Value} supports coercion with a {@link Coercer} to the specified type.
     * <p>
     * <strong>NOTE:</strong> This does not test whether the {@link Value} can be coerced without an exception. It
     * simply determines if a {@link Coercer} for the specified type is known to the {@link Value}.
     *
     * @param clazz The type to which the {@link Value} should be coerced.
     * @return <code>true</code> if type is a known {@link Value} {@link Coercer}.  <code>false</code> otherwise
     */
    public boolean hasCoercerFor(Class<?> clazz)
    {
        return clazz.isEnum() || clazz.isPrimitive() || COERCERS.containsKey(clazz);
    }


    /**
     * Attempts to return the value of the {@link Value} coerced to a specified type.
     *
     * @param <T> The expected type of the value.
     * @param clazz The expected type of the value (the value to coerce to)
     *
     * @return The {@link Value} coerced in the required type.
     *
     * @throws ClassCastException If the value of the {@link Value} can't be cast to the specified type.
     * @throws NumberFormatException If the value of the {@link Value} can't be cast to the specified type.
     * @throws UnknownFormatConversionException If the value of the {@link Value} can't be cast to the specified type.
     */
    @SuppressWarnings({"unchecked"})
    public <T> T getValue(Class<T> clazz)
        throws ClassCastException, UnknownFormatConversionException, NumberFormatException
    {
        if (isNull())
        {
            return null;
        }
        else if (clazz.equals(value.getClass()))
        {
            return (T) value;
        }
        else if (clazz.isAssignableFrom(value.getClass()))
        {
            return (T) value;
        }
        else if (clazz.isEnum())
        {
            String enumString = (value instanceof XmlValue ? ((XmlValue) value).getString() : value.toString()).trim();

            // find the enumerated value that matches the specified string
            for (Enum<?> enumValue : (Enum<?>[]) clazz.getEnumConstants())
            {
                if (enumValue.name().equalsIgnoreCase(enumString))
                {
                    return (T) enumValue;
                }
            }

            // couldn't find the enum value!
            throw new ClassCastException(String.format("The specified enumuerated value '%s' is unknown.", enumString));
        }
        else
        {
            if (COERCERS.containsKey(clazz))
            {
                Coercer<T> coercer = (Coercer<T>) COERCERS.get(clazz);

                return coercer.coerce(this);
            }
            else
            {
                throw new ClassCastException(String.format("Can't coerce %s into a %s.", value, clazz.toString()));
            }
        }
    }


    /**
     * Determines if the {@link Value} is serializable.
     *
     * @return <code>true</code> if the expression can be serialized, <code>false</code> otherwise.
     */
    public boolean isSerializable()
    {
        return value == null || value instanceof Serializable || value instanceof ExternalizableLite
               || value instanceof PortableObject;
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
    {
        this.value = ExternalizableHelper.readObject(in);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
    {
        ExternalizableHelper.writeObject(out, value);
    }


    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader reader) throws IOException
    {
        this.value = reader.readObject(1);
    }


    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter writer) throws IOException
    {
        writer.writeObject(1, value);
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return String.format("Value{%s}", value);
    }


    /**
     * A {@link Coercer} is responsible for performing type coercion on a {@link Value} to obtain a required type
     * of value.
     */
    public static interface Coercer<T>
    {
        /**
         * Attempts to coerce the specified {@link Value} into the required type.
         *
         * @param value The {@link Value} to coerce. (will never be <code>null</code>)
         *
         * @return The value of the {@link Value} coerced into the required type.
         *
         * @throws ClassCastException should the coercion fail.
         */
        public T coerce(Value value) throws ClassCastException;
    }


    /**
     * A {@link BigDecimalCoercer} is a {@link BigDecimal}-based implementation of a {@link Coercer}.
     */
    private static class BigDecimalCoercer implements Coercer<BigDecimal>
    {
        /**
         * {@inheritDoc}
         */
        public BigDecimal coerce(Value value) throws NumberFormatException
        {
            if (value.value instanceof XmlValue)
            {
                return ((XmlValue) value.value).getDecimal();
            }
            else
            {
                return new BigDecimal(value.value.toString());
            }
        }
    }


    /**
     * A {@link BooleanCoercer} is a {@link Boolean}-based implementation of a {@link Coercer}.
     */
    private static class BooleanCoercer implements Coercer<Boolean>
    {
        /**
         * {@inheritDoc}
         */
        public Boolean coerce(Value value) throws UnknownFormatConversionException
        {
            String proposedBoolean;

            if (value.value instanceof XmlValue)
            {
                proposedBoolean = ((XmlValue) value.value).getString().trim();
            }
            else
            {
                proposedBoolean = value.value.toString().trim();
            }

            if (proposedBoolean.equalsIgnoreCase("true") || proposedBoolean.equalsIgnoreCase("yes")
                || proposedBoolean.equalsIgnoreCase("on"))
            {
                return true;
            }
            else if (proposedBoolean.equalsIgnoreCase("false") || proposedBoolean.equalsIgnoreCase("no")
                     || proposedBoolean.equalsIgnoreCase("off"))
            {
                return false;
            }
            else
            {
                throw new UnknownFormatConversionException(String
                    .format("The value [%s] is not a boolean (true, yes, on, false, no, off, args)", proposedBoolean));
            }
        }
    }


    /**
     * A {@link ByteCoercer} is a {@link Byte}-based implementation of a {@link Coercer}.
     */
    private static class ByteCoercer implements Coercer<Byte>
    {
        /**
         * {@inheritDoc}
         */
        public Byte coerce(Value value) throws NumberFormatException
        {
            if (value.value instanceof XmlValue)
            {
                return Byte.parseByte(((XmlValue) value.value).getString());
            }
            else
            {
                return Byte.parseByte(value.value.toString());
            }
        }
    }


    /**
     * A {@link DoubleCoercer} is a {@link Double}-based implementation of a {@link Coercer}.
     */
    private static class DoubleCoercer implements Coercer<Double>
    {
        /**
         * {@inheritDoc}
         */
        public Double coerce(Value value) throws NumberFormatException
        {
            if (value.value instanceof XmlValue)
            {
                return ((XmlValue) value.value).getDouble();
            }
            else
            {
                return Double.parseDouble(value.value.toString());
            }
        }
    }


    /**
     * A {@link FloatCoercer} is a {@link Double}-based implementation of a {@link Coercer}.
     */
    private static class FloatCoercer implements Coercer<Float>
    {
        /**
         * {@inheritDoc}
         */
        public Float coerce(Value value) throws NumberFormatException
        {
            if (value.value instanceof XmlValue)
            {
                return (float) ((XmlValue) value.value).getDouble();
            }
            else
            {
                return Float.parseFloat(value.value.toString());
            }
        }
    }


    /**
     * A {@link IntegerCoercer} is a {@link Integer}-based implementation of a {@link Coercer}.
     */
    private static class IntegerCoercer implements Coercer<Integer>
    {
        /**
         * {@inheritDoc}
         */
        public Integer coerce(Value value) throws NumberFormatException
        {
            if (value.value instanceof XmlValue)
            {
                return ((XmlValue) value.value).getInt();
            }
            else
            {
                return Integer.parseInt(value.value.toString());
            }
        }
    }


    /**
     * A {@link LongCoercer} is a {@link Long}-based implementation of a {@link Coercer}.
     */
    private static class LongCoercer implements Coercer<Long>
    {
        /**
         * {@inheritDoc}
         */
        public Long coerce(Value value) throws NumberFormatException
        {
            if (value.value instanceof XmlValue)
            {
                return ((XmlValue) value.value).getLong();
            }
            else
            {
                return Long.parseLong(value.value.toString());
            }
        }
    }


    /**
     * A {@link ShortCoercer} is a {@link Short}-based implementation of a {@link Coercer}.
     */
    private static class ShortCoercer implements Coercer<Short>
    {
        /**
         * {@inheritDoc}
         */
        public Short coerce(Value value) throws NumberFormatException
        {
            if (value.value instanceof XmlValue)
            {
                return (short) ((XmlValue) value.value).getInt();
            }
            else
            {
                return Short.parseShort(value.value.toString());
            }
        }
    }


    /**
     * A {@link StringCoercer} is a {@link String}-based implementation of a {@link Coercer}.
     */
    private static class StringCoercer implements Coercer<String>
    {
        /**
         * {@inheritDoc}
         */
        public String coerce(Value value)
        {
            if (value.value instanceof XmlValue)
            {
                return ((XmlValue) value.value).getString();
            }
            else
            {
                return value.value.toString();
            }
        }
    }


    /**
     * A {@link XmlCoercer} is a {@link XmlValue}-based implementation of a {@link Coercer}.
     */
    private static class XmlCoercer implements Coercer<XmlValue>
    {
        /**
         * {@inheritDoc}
         */
        public XmlValue coerce(Value value)
        {
            if (value.value instanceof XmlValue)
            {
                return (XmlValue) value.value;
            }
            else if (value.value instanceof XmlElement)
            {
                return (XmlElement) value.value;
            }
            else
            {
                return XmlHelper.loadXml(value.value.toString());
            }
        }
    }


    /**
     * Static initialization to build map of known {@link Coercer}s.
     */
    static
    {
        // initialize the known coercers based on their type
        COERCERS = new HashMap<Class<?>, Coercer<?>>();
        COERCERS.put(BigDecimal.class, new BigDecimalCoercer());
        COERCERS.put(Boolean.class, new BooleanCoercer());
        COERCERS.put(Boolean.TYPE, new BooleanCoercer());
        COERCERS.put(Byte.class, new ByteCoercer());
        COERCERS.put(Byte.TYPE, new ByteCoercer());
        COERCERS.put(Double.class, new DoubleCoercer());
        COERCERS.put(Double.TYPE, new DoubleCoercer());
        COERCERS.put(Float.class, new FloatCoercer());
        COERCERS.put(Float.TYPE, new FloatCoercer());
        COERCERS.put(Integer.class, new IntegerCoercer());
        COERCERS.put(Integer.TYPE, new IntegerCoercer());
        COERCERS.put(Long.class, new LongCoercer());
        COERCERS.put(Long.TYPE, new LongCoercer());
        COERCERS.put(Short.class, new ShortCoercer());
        COERCERS.put(Short.TYPE, new ShortCoercer());
        COERCERS.put(String.class, new StringCoercer());
        COERCERS.put(XmlValue.class, new XmlCoercer());
        COERCERS.put(XmlElement.class, new XmlCoercer());
    }
}
