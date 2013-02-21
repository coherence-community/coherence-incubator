/*
 * File: InvokeMethodProcessorTest.java
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
import com.tangosol.util.InvocableMap.Entry;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertTrue;

/**
 * A {@link InvokeMethodProcessorTest} tests the {@link InvokeMethodProcessor}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Christer Fahlgren
 */
public class InvokeMethodProcessorTest
{
    /**
     * Tests an invocation supporting {@link ChangeIndication}.
     */
    @Test
    public void testProcess()
    {
        Entry                entry       = Mockito.mock(Entry.class);
        ChangeIndicationImpl valueObject = new ChangeIndicationImpl();

        Mockito.stub(entry.getValue()).toReturn(valueObject);
        valueObject.test(10);

        InvokeMethodProcessor processor = new InvokeMethodProcessor("test", new Object[] {10});

        processor.process(entry);
        Mockito.verify(entry).setValue(valueObject);

    }


    /**
     * Tests an invocation supporting {@link ChangeIndication} that didn't change.
     */
    @Test
    public void testProcessNoChange()
    {
        Entry                entry       = Mockito.mock(Entry.class);
        ChangeIndicationImpl valueObject = new ChangeIndicationImpl();

        Mockito.stub(entry.getValue()).toReturn(valueObject);
        valueObject.testNoChange();

        InvokeMethodProcessor processor = new InvokeMethodProcessor("testNoChange");

        processor.process(entry);
        Mockito.verify(entry, Mockito.never()).setValue(valueObject);

    }


    /**
     * Tests an invocation without support for {@link ChangeIndication}.
     */
    @Test
    public void testProcessNoChangeIndication()
    {
        Entry            entry       = Mockito.mock(Entry.class);
        PlainValueObject valueObject = new PlainValueObject();

        Mockito.stub(entry.getValue()).toReturn(valueObject);

        InvokeMethodProcessor processor = new InvokeMethodProcessor("test", new Object[] {10});

        processor.process(entry);
        Mockito.verify(entry).setValue(valueObject);

    }


    /**
     * Tests an invocation where there is no object.
     */
    @Test
    public void testProcessNoValue()
    {
        Entry entry = Mockito.mock(Entry.class);

        Mockito.stub(entry.getValue()).toReturn(null);

        InvokeMethodProcessor processor = new InvokeMethodProcessor("test", new Object[] {10});
        Object                result    = processor.process(entry);

        assertTrue(result.getClass().equals(NullPointerException.class));

    }


    /**
     * Tests the toString method.
     */
    @Test
    public void testToString()
    {
        InvokeMethodProcessor processor = new InvokeMethodProcessor("test", new Object[] {10});

        assertTrue(processor.toString().equals("test(10)"));
    }


    /**
     * A {@link ChangeIndicationImpl} for testing.
     *
     * @author Christer Fahlgren
     */
    static class ChangeIndicationImpl implements ChangeIndication
    {
        /**
         *
         */
        private boolean changed;


        /**
         * Test method.
         * @param i test parameter.
         */
        public void test(int i)
        {
            setChanged();
        }


        /**
         * Sets changed status.
         */
        private void setChanged()
        {
            changed = true;
        }


        /**
         * Test method.
         */
        public void testNoChange()
        {
        }


        /**
         * {@inheritDoc}
         */
        public void beforeChange()
        {
            changed = false;
        }


        /**
         * {@inheritDoc}
         */
        public boolean changed()
        {
            return changed;
        }
    }


    /**
     * A {@link PlainValueObject}.
     *
     * @author Christer Fahlgren
     */
    static class PlainValueObject
    {
        /**
         * Test method.
         *
         * @param i test parameter
         */
        public void test(int i)
        {
        }
    }
}
