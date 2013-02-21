/*
 * File: CollectionType.java
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

import com.oracle.coherence.common.serialization.annotations.PofField;
import com.oracle.coherence.common.serialization.annotations.PofType;
import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author coh 2010-05-23
 */
@PofType(id = 1004)
public class CollectionType
{
    /**
     * Field description
     */
    public final static List<String> s_listOfStrings = new ArrayList<String>();

    /**
     * Field description
     */
    public final static Map<String, String> s_mapOfStrings = new HashMap<String, String>();

    /**
     * Field description
     */
    public final static TreeMap<String, String> s_treemapOfStrings = new TreeMap<String, String>();

    @PofField(type = ArrayList.class)
    private List<String>                        listOfStrings;
    @PofField(type = ArrayList.class)
    private List<List<String>>                  listOfListOfStrings;
    @PofField
    private ArrayList<String>                   implicitListOfStrings;
    private Map<String, String>                 mapStringToString;
    @PofField(type = ConcurrentHashMap.class)
    private Map<String, String>                 explicitMapStringToString;
    @PofField
    private TreeMap<String, String>             treeMapStringToString;


    /**
     * Constructs ...
     *
     */
    public CollectionType()
    {
    }


    /**
     * Method description
     */
    public void init()
    {
        listOfStrings       = new ArrayList<String>(s_listOfStrings);

        listOfListOfStrings = new ArrayList<List<String>>();
        listOfListOfStrings.add(listOfStrings);
        listOfListOfStrings.add(listOfStrings);

        implicitListOfStrings     = new ArrayList<String>(s_listOfStrings);

        mapStringToString         = new ConcurrentHashMap<String, String>(s_mapOfStrings);

        explicitMapStringToString = new HashMap<String, String>(s_mapOfStrings);

        treeMapStringToString     = new TreeMap<String, String>(s_mapOfStrings);
    }


    /**
     * Method description
     */
    public void verify()
    {
        Assert.assertEquals(s_listOfStrings.getClass(), listOfStrings.getClass());
        Assert.assertEquals(s_listOfStrings, listOfStrings);

        Assert.assertEquals(s_listOfStrings.getClass(), listOfListOfStrings.getClass());
        Assert.assertEquals(s_listOfStrings, listOfListOfStrings.get(0));
        Assert.assertEquals(s_listOfStrings, listOfListOfStrings.get(1));

        Assert.assertEquals(s_listOfStrings.getClass(), implicitListOfStrings.getClass());
        Assert.assertEquals(s_listOfStrings, implicitListOfStrings);

        Assert.assertEquals(s_mapOfStrings.getClass(), mapStringToString.getClass());

        Assert.assertEquals(s_mapOfStrings, mapStringToString);

        Assert.assertEquals(ConcurrentHashMap.class, explicitMapStringToString.getClass());

        Assert.assertEquals(s_mapOfStrings, explicitMapStringToString);

        Assert.assertEquals(TreeMap.class, treeMapStringToString.getClass());

        Assert.assertEquals(s_treemapOfStrings, treeMapStringToString);
    }


    static
    {
        s_listOfStrings.add("one");
        s_listOfStrings.add("two");
        s_listOfStrings.add("three");
        s_listOfStrings.add("four");

        s_mapOfStrings.put("one", "ett");
        s_mapOfStrings.put("two", "tva");
        s_mapOfStrings.put("three", "tre");
        s_mapOfStrings.put("four", "fyra");

        s_treemapOfStrings.put("one", "ett");
        s_treemapOfStrings.put("two", "tva");
        s_treemapOfStrings.put("three", "tre");
        s_treemapOfStrings.put("four", "fyra");
    }
}
