/*
 * File: DefaultReflectedSerializer.java
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

package com.oracle.coherence.common.serialization;

import com.oracle.coherence.common.logging.Logger;
import com.oracle.coherence.common.serialization.annotations.PofField;
import com.oracle.coherence.common.serialization.annotations.PofIgnore;
import com.oracle.coherence.common.serialization.annotations.PofType;
import com.tangosol.io.pof.PofContext;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link DefaultReflectedSerializer} is a {@link ReflectedSerializer} that
 * has been generated at runtime through reflection.
 * <p>
 * Note: You should never directly configure an instance of this class to be a
 * serializer for a user-defined type. Instead you should use the
 * {@link ReflectiveSerializer} that will create, manage and call instances of
 * this class for you.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Charlie Helin
 */
final class DefaultReflectedSerializer implements ReflectedSerializer
{
    /**
     * The arguments for the default constructor.
     */
    private static final Class<?>[] DEFAULT_PARAMETER_TYPES = new Class<?>[]
    {
    };

    /**
     * The type (as a {@link Class}) for which this {@link ReflectedSerializer}
     * has be generated.
     */
    private Class<?> type;

    /**
     * The User Type Identity of the {@link Class} in the {@link ReflectedContext}.
     */
    private int userTypeId;

    /**
     * The Pof version number that will be used to serialize the type {@link Class} if none is specified or read
     * with a {@link PofRemainder}.
     */
    private int version;

    /**
     * The detected Pof version number of the type, based on the highest version number of the annotate
     * {@link PofField}s and {@link PofType}.
     */
    private int detectedVersion;

    /**
     * (optional) The {@link Field} that stores the {@link PofRemainder} (if the type is evolvable).  If no remainder
     * is determined, we know that the type is not evolable, in which case the remainder will be <code>null</code>.
     */
    private Field remainderField;

    /**
     * A {@link Map} from version numbers (of the type) to all of the {@link Field}s included in the said versions
     * (where the {@link Field}s themselves are arranged in ascending order of pof indicies based on their alphabetic
     * field names).
     */
    private Map<Integer, FieldMetaInfo[]> fieldMetaInfoByVersion;

    /**
     * The {@link ReflectedContext} in which this {@link PofSerializer} was defined.
     */
    private ReflectedContext reflectedPofSerializer;

    /**
     * True iff the type implements {@link PortableObject}.
     */
    private boolean implementsPortableObject = false;


    /**
     * Standard Constructor.
     *
     * @param type                  The type for which the {@link PofSerializer} should be created.
     * @param reflectedPofContext   The {@link ReflectedContext} in which this {@link PofSerializer} exists.
     * @param pofContext
     */
    protected DefaultReflectedSerializer(Class<?>         type,
                                         ReflectedContext reflectedPofContext,
                                         PofContext       pofContext)
    {
        this.type                     = type;
        this.reflectedPofSerializer   = reflectedPofContext;
        this.implementsPortableObject = false;

        // check if the type already is implementing
        // PortableObject, if so we will use that implementation
        // instead of weaving our own
        for (Class<?> iface : type.getInterfaces())
        {
            if (iface.equals(PortableObject.class))
            {
                // this type already has a specified serializer
                this.implementsPortableObject = true;

                break;
            }
        }

        // ensure that the super-class is in the pof context
        if (type.getSuperclass().isAnnotationPresent(PofType.class))
        {
            reflectedPofContext.ensurePofSerializer(type.getSuperclass(), pofContext);
        }

        // start building the serialization information using reflection
        PofType pofType = type.getAnnotation(PofType.class);

        this.userTypeId = pofType == null ? pofContext.getUserTypeIdentifier(type) : pofType.id();

        if (!implementsPortableObject)
        {
            this.fieldMetaInfoByVersion = new HashMap<Integer, FieldMetaInfo[]>();

            detectedVersion             = pofType == null ? 0 : pofType.version();
            setVersion(detectedVersion);

            scanFields(reflectedPofContext.getFieldSerializationProvider(), detectedVersion);
        }
    }


    /**
     * Scans and add fields on a version basis. The fields will added
     * alphabetically either to their name or any potential @PofField name
     * attribute.
     *
     * @param fieldSerializer the serializer used to serialize induvidual fields
     *            with
     * @param version the version of the type to scan
     */
    private void scanFields(FieldSerializationProvider fieldSerializer,
                            int                        version)
    {
        // extract all of the field information for the type using
        // reflection

        // step 1: Sort the declared fields (this doesn't include the super
        // class) alphabetically
        // (we do this to work out Pof indicies of the fields.
        Field[] alphabeticalFields = type.getDeclaredFields();

        Arrays.sort(alphabeticalFields, new Comparator<Field>()
        {
            @Override
            public int compare(Field field1,
                               Field field2)
            {
                PofField pofField1 = field1.getAnnotation(PofField.class);
                String fieldName1 = pofField1 == null
                                    ? field1.getName()
                                    : pofField1.name().isEmpty() ? field1.getName() : pofField1.name();

                PofField pofField2 = field2.getAnnotation(PofField.class);
                String fieldName2 = pofField2 == null
                                    ? field2.getName()
                                    : pofField2.name().isEmpty() ? field2.getName() : pofField2.name();

                return fieldName1.compareTo(fieldName2);
            }
        });

        // step 3: determine field serialization meta info for each of the
        // versions

        // we always start pof indexes at 1 (0 is reserved for the super
        // class)
        int                 nextPofIndex   = 1;

        List<FieldMetaInfo> fieldMetaInfos = new ArrayList<FieldMetaInfo>();

        for (int iSince = 0; iSince <= version; iSince++)
        {
            for (Field field : alphabeticalFields)
            {
                if (field.getType().equals(PofRemainder.class))
                {
                    this.remainderField = field;
                    this.remainderField.setAccessible(true);
                }
                else if (includeField(field, iSince))
                {
                    PofField annotation = field.getAnnotation(PofField.class);

                    // determine the preferred Field type
                    Class<?> preferredType = annotation == null ? field.getType() : annotation.type();

                    // determine the preferred Field name
                    String preferredName = annotation == null ? field.getName() : annotation.name();

                    // determine the appropriate FieldSerializer for the
                    // Field
                    FieldSerializer serializer = fieldSerializer.getFieldSerializer(field, preferredType);

                    // capture the meta information concerning
                    // serialization of the field
                    FieldMetaInfo fieldMetaInfo = new FieldMetaInfo(field,
                                                                    nextPofIndex++,
                                                                    preferredName,
                                                                    preferredType,
                                                                    iSince,
                                                                    serializer);

                    // force the field to be accessible so that we can
                    // set values on the field
                    field.setAccessible(true);

                    fieldMetaInfos.add(fieldMetaInfo);

                }
            }

            fieldMetaInfoByVersion.put(iSince, fieldMetaInfos.toArray(new FieldMetaInfo[0]));
        }
    }


    /**
     * @param field
     * @return
     */
    private boolean includeField(Field field,
                                 int   iSince)
    {
        PofField annotation = field.getAnnotation(PofField.class);

        return (field.getModifiers() & (Modifier.TRANSIENT | Modifier.STATIC | Modifier.FINAL)) == 0
               &&!field.isAnnotationPresent(PofIgnore.class)
               && ((annotation == null && iSince == 0) || (annotation != null && annotation.since() == iSince));
    }


    /**
     * @{inheritDoc}
     */
    @Override
    public Class<?> getType()
    {
        return type;
    }


    /**
     * Determines the User Type Id for the {@link Class} that this {@link PofSerializer} can serialize/deserialize.
     *
     * @return User Type Id
     */
    public int getUserTypeId()
    {
        return userTypeId;
    }


    /**
     * @{inheritDoc}
     */
    @Override
    public int getVersion()
    {
        return version;
    }


    /**
     * @{inheritDoc}
     */
    @Override
    public void setVersion(int version)
    {
        this.version = version;

        if (version > this.version)
        {
            detectedVersion = version;
            scanFields(reflectedPofSerializer.getFieldSerializationProvider(), version);
        }
    }


    /**
     * Sets (forces) the Pof version number that will be used for serializing the type when a
     * {@link PofRemainder#getFromVersion()} is unavailable to the {@link #getDetectedVersion()}.
     */
    public void resetVersion()
    {
        this.version = detectedVersion;
    }


    /**
     * The detected (highest) version number of the type (based on the {@link PofType} and {@link PofField#since()}
     * version numbers).  This is the default version number if {@link #setVersion(int)} has not been called
     * to override the {@link DefaultReflectedSerializer#getVersion()} number.
     *
     * @return An Integer
     */
    public int getDetectedVersion()
    {
        return detectedVersion;
    }


    /**
     * @{inheritDoc}
     */
    @Override
    public Object deserialize(PofReader reader) throws IOException
    {
        try
        {
            // create an instance of this type (using the default constructor)
            Object instance = getType().getConstructor(DEFAULT_PARAMETER_TYPES).newInstance();

            if (implementsPortableObject)
            {
                ((PortableObject) instance).readExternal(reader);
            }
            else
            {
                // initialize the fields of the instance directly from the pof
                // stream
                deserializeInto(reader, instance);

                // save any remaining un-read part of the POF stream into the
                // remainder
                readRemainder(reader, instance);
            }

            // we're done!
            return instance;
        }
        catch (IllegalArgumentException e)
        {
            throw Base.ensureRuntimeException(e);
        }
        catch (SecurityException e)
        {
            throw Base.ensureRuntimeException(e);
        }
        catch (InstantiationException e)
        {
            throw Base.ensureRuntimeException(e);
        }
        catch (IllegalAccessException e)
        {
            throw Base.ensureRuntimeException(e);
        }
        catch (InvocationTargetException e)
        {
            throw Base.ensureRuntimeException(e);
        }
        catch (NoSuchMethodException e)
        {
            throw Base.ensureRuntimeException(e);
        }
    }


    /**
     * Recursively initialize an instance of the represented type. The
     * difference between this read and the single parameter read method is that
     * this method requires an instance of the type.
     *
     * @param reader
     *            the PofReader where information about the instance is stored
     * @param object
     *            the instance to initialize
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws IOException
     * @throws SecurityException
     * @throws InstantiationException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    public void deserializeInto(PofReader reader,
                                Object    object)
                                    throws IllegalArgumentException, IllegalAccessException, IOException,
                                           SecurityException, InstantiationException, InvocationTargetException,
                                           NoSuchMethodException
    {
        // deserialize the super class (iff it's annotated as a PofType)
        if (type.getSuperclass().isAnnotationPresent(PofType.class))
        {
            DefaultReflectedSerializer pofSerializer =
                (DefaultReflectedSerializer) reflectedPofSerializer
                    .getPofSerializer(reflectedPofSerializer.getUserTypeIdentifier(type.getSuperclass()));
            PofReader nestedPofReader = reader.createNestedPofReader(0);

            pofSerializer.deserializeInto(nestedPofReader, object);
            pofSerializer.readRemainder(nestedPofReader, object);
        }

        // deserialize the fields for the minimum version available (between the reflected version and stream)
        for (FieldMetaInfo fieldMetaInfo : fieldMetaInfoByVersion.get(Math.min(version, reader.getVersionId())))
        {
            fieldMetaInfo.getFieldSerializer().readField(object,
                                                         fieldMetaInfo.getField(),
                                                         reader,
                                                         fieldMetaInfo.getPofIndex());
        }
    }


    /**
     * @{inheritDoc}
     */
    @Override
    public void serialize(PofWriter writer,
                          Object    object) throws IOException
    {
        if (implementsPortableObject)
        {
            ((PortableObject) object).writeExternal(writer);
        }
        else
        {
            // determine and set the version of the object to write to the
            // stream
            int serializingVersion = 0;

            if (remainderField == null)
            {
                // when there is no remainder, always use the specified version
                serializingVersion = version;
            }
            else
            {
                try
                {
                    PofRemainder pofRemainder = (PofRemainder) remainderField.get(object);

                    serializingVersion = pofRemainder == null ? version : pofRemainder.getFromVersion();
                }
                catch (IllegalArgumentException e)
                {
                    Base.ensureRuntimeException(e, "While attempting to setVersionId() on the PofWriter");
                }
                catch (IllegalAccessException e)
                {
                    Base.ensureRuntimeException(e, "While attempting to setVersionId() on the PofWriter");
                }
            }

            writer.setVersionId(serializingVersion);

            // serialize the super class (iff it's annotated as a PofType)
            if (type.getSuperclass().isAnnotationPresent(PofType.class))
            {
                DefaultReflectedSerializer pofSerializer =
                    (DefaultReflectedSerializer) reflectedPofSerializer
                        .getPofSerializer(reflectedPofSerializer.getUserTypeIdentifier(type.getSuperclass()));
                PofWriter nestedPofWriter = writer.createNestedPofWriter(0);

                pofSerializer.serialize(nestedPofWriter, object);
            }

            // serialize the Fields
            for (FieldMetaInfo fieldMetaInfo : fieldMetaInfoByVersion.get(serializingVersion))
            {
                try
                {
                    fieldMetaInfo.getFieldSerializer().writeField(object,
                                                                  fieldMetaInfo.getField(),
                                                                  writer,
                                                                  fieldMetaInfo.getPofIndex());
                }
                catch (Exception e)
                {
                    Logger.log(Logger.ERROR, "Failure assigning field %s: %s", fieldMetaInfo.getName(), e.getMessage());

                    throw new IllegalStateException(e);
                }
            }
        }

        writeRemainder(writer, object);
    }


    /**
     * Read the remainder from the PofReader and store it in the PofRemainder
     * marked field.
     *
     * @param reader
     *            the PofReader
     * @param object
     *            the object to initialize
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws IOException
     */
    private void readRemainder(PofReader reader,
                               Object    object) throws IOException
    {
        if (remainderField == null)
        {
            reader.readRemainder();
        }
        else
        {
            try
            {
                Binary       remainder = reader.readRemainder();

                PofRemainder pofRemainder;

                if (remainder == null)
                {
                    pofRemainder = null;
                }
                else
                {
                    pofRemainder = new PofRemainder(reader.getVersionId(), remainder);
                }

                remainderField.set(object, pofRemainder);
            }
            catch (IllegalArgumentException e)
            {
                Base.ensureRuntimeException(e);
            }
            catch (IllegalAccessException e)
            {
                Base.ensureRuntimeException(e);
            }
        }
    }


    /**
     * Write the remainder from the field marked by the PofRemainder annotation.
     *
     * @param writer
     *            the writer to write the remainder
     * @param object
     *            the instance of the object
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws IOException
     */
    private void writeRemainder(PofWriter writer,
                                Object    object) throws IOException
    {
        // when there is no remainder or the version of the type has been forced to something less than detected
        // we always write an empty remainder
        if (remainderField == null || writer.getVersionId() < getDetectedVersion())
        {
            writer.writeRemainder(null);
        }
        else
        {
            Binary binary;

            try
            {
                PofRemainder pofRemainder = (PofRemainder) remainderField.get(object);

                if (pofRemainder == null)
                {
                    binary = null;
                }
                else if (pofRemainder.getBinary() == null)
                {
                    binary = new Binary();
                    remainderField.set(object, binary);
                }
                else
                {
                    binary = pofRemainder.getBinary();
                }

                writer.writeRemainder(binary);
            }
            catch (IllegalArgumentException e)
            {
                Base.ensureRuntimeException(e);
            }
            catch (IllegalAccessException e)
            {
                Base.ensureRuntimeException(e);
            }
        }
    }


    /**
     * A {@link FieldMetaInfo} captures runtime information concerning the serialization of a single {@link Field} with
     * in the type for which this {@link PofSerializer} has been generated.
     * <p>
     * This information is generated based on a combination of the {@link PofField} annotation and the {@link Field}
     * declaration itself.
     */
    final class FieldMetaInfo
    {
        /**
         * The {@link Field} for which this meta information is related.
         */
        private Field field;

        /**
         * The Pof Index assigned to the {@link Field}.
         */
        private int pofIndex;

        /**
         * The name of the {@link Field}.  This will be the value assigned by the {@link PofField#name()} annotation
         * or the {@link Field#getName()} if the name is left undefined in the said annotation.
         */
        private String name;

        /**
         * The version of the enclosing type when the {@link Field} became available.
         */
        private int sinceVersion;

        /**
         * The type of the {@link Field}.  This will be the value assigned by the {@link PofField#type()} annotation
         * or the {@link Field#getType()} if the type was left undefined in the said annotation.
         */
        private Class<?> type;

        /**
         * The {@link FieldSerializer} that will be used to serialize/deserialize values for the {@link Field}.
         */
        private FieldSerializer fieldSerializer;


        /**
         * Standard Constructor.
         *
         * @param field             The {@link Field} for which this {@link FieldMetaInfo} has been generated.
         * @param pofIndex          The Pof Index of the {@link Field} in a Pof Stream.
         * @param name              The desired name of the {@link Field}.
         * @param sinceVersion      The version of the outer type in which the {@link Field} was introduced.
         * @param type              The desired type of the {@link Field} values.
         * @param fieldSerializer   The {@link FieldSerializer} to use for serialization of the {@link Field}.
         */
        public FieldMetaInfo(Field           field,
                             int             pofIndex,
                             String          name,
                             Class<?>        type,
                             int             sinceVersion,
                             FieldSerializer fieldSerializer)
        {
            this.field           = field;
            this.pofIndex        = pofIndex;
            this.name            = name;
            this.sinceVersion    = sinceVersion;
            this.type            = type;
            this.fieldSerializer = fieldSerializer;
        }


        /**
         * Determines the {@link Field} from which the {@link FieldMetaInfo} was generated.
         *
         * @return A {@link Field}
         */
        public Field getField()
        {
            return field;
        }


        /**
         * Determines the Pof Index at which the {@link Field} will be serialized/deserialized.
         *
         * @return An Integer
         */
        public int getPofIndex()
        {
            return pofIndex;
        }


        /**
         * Determines the name of the {@link Field}.
         *
         * @return A String
         */
        public String getName()
        {
            return name;
        }


        /**
         * Determines the version of the type in which the {@link Field} was first defined.
         *
         * @return An Integer
         */
        public int getSinceVersion()
        {
            return sinceVersion;
        }


        /**
         * Determines the concrete {@link Class} to instantiate for {@link Field} values.
         *
         * @return A {@link Class}
         */
        public Class<?> getType()
        {
            return type;
        }


        /**
         * Determines the {@link FieldSerializer} to be used to serialize/deserialize values for the {@link Field}
         * from Pof streams.
         *
         * @return A {@link FieldSerializer}
         */
        public FieldSerializer getFieldSerializer()
        {
            return fieldSerializer;
        }
    }
}
