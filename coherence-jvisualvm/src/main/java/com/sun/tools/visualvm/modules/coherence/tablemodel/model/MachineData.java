/*
 * File: MachineData.java
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

package com.sun.tools.visualvm.modules.coherence.tablemodel.model;

import com.sun.tools.visualvm.modules.coherence.VisualVMModel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

/**
 * A class to hold machine data.
 *
 * @author Tim Middleton
 */
public class MachineData extends AbstractData
{
    private static final long serialVersionUID = -4146745462482520312L;

    /**
     * Array index for machine name.
     */
    public static final int MACHINE_NAME = 0;

    /**
     * Array index for processor count.
     */
    public static final int PROCESSOR_COUNT = 1;

    /**
     * Array index for system load average.
     */
    public static final int SYSTEM_LOAD_AVERAGE = 2;

    /**
     * Array index for total physical memory.
     */
    public static final int TOTAL_PHYSICAL_MEMORY = 3;

    /**
     * Array index for free physical memory.
     */
    public static final int FREE_PHYSICAL_MEMORY = 4;

    /**
     * Array index for percent free memory
     */
    public static final int PERCENT_FREE_MEMORY = 5;

    /**
     * The logger object to use.
     */
    private static final Logger LOGGER = Logger.getLogger(MachineData.class.getName());


    /**
     * Constructor passing in the number of columns.
     *
     */
    public MachineData()
    {
        super(PERCENT_FREE_MEMORY + 1);
    }


    /**
     * {@inheritDoc}
     */
    public String getReporterReport()
    {
        return null;    // implement when Coherence version supports this
    }


    /**
     * {@inheritDoc}
     */
    public List<Map.Entry<Object, Data>> getJMXData(MBeanServerConnection server,
                                                    VisualVMModel         model)
    {
        SortedMap<Object, Data> mapData = new TreeMap<Object, Data>();
        Data                    data;

        // get the list of machines, along with a member id on that machine
        SortedMap<Pair<String, Integer>, Data> initialMachineMap = model.getInitialMachineMap();

        try
        {
            // loop through each of the entries and get the data for a given node only
            // as we know that nodes on the same machine will have the same results
            Iterator<Map.Entry<Pair<String, Integer>, Data>> iter = initialMachineMap.entrySet().iterator();

            while (iter.hasNext())
            {
                Map.Entry<Pair<String, Integer>, Data> entry = iter.next();

                String sQuery = "Coherence:type=Platform,Domain=java.lang,subType=OperatingSystem,nodeId="
                                + entry.getKey().getY() + ",*";

                Set<ObjectName> resultSet = server.queryNames(new ObjectName(sQuery), null);

                for (Iterator<ObjectName> iterResults = resultSet.iterator(); iterResults.hasNext(); )
                {
                    ObjectName objectName = (ObjectName) iterResults.next();

                    data = new MachineData();

                    data.setColumn(MachineData.MACHINE_NAME, entry.getKey().getX());
                    data.setColumn(MachineData.FREE_PHYSICAL_MEMORY,
                                   (Long) server.getAttribute(objectName, "FreePhysicalMemorySize"));
                    data.setColumn(MachineData.SYSTEM_LOAD_AVERAGE,
                                   (Double) server.getAttribute(objectName, "SystemLoadAverage"));
                    data.setColumn(MachineData.PROCESSOR_COUNT,
                                   (Integer) server.getAttribute(objectName, "AvailableProcessors"));
                    data.setColumn(MachineData.TOTAL_PHYSICAL_MEMORY,
                                   (Long) server.getAttribute(objectName, "TotalPhysicalMemorySize"));

                    data.setColumn(MachineData.PERCENT_FREE_MEMORY,
                                   ((Long) data.getColumn(MachineData.FREE_PHYSICAL_MEMORY) * 1.0f)
                                   / (Long) data.getColumn(MachineData.TOTAL_PHYSICAL_MEMORY));

                    // put it into the mapData with just a machine as the key
                    mapData.put(entry.getKey().getX(), data);

                }
            }

            return new ArrayList<Map.Entry<Object, Data>>(mapData.entrySet());
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "Error getting machine statistics: " + e.getMessage());

            return null;
        }
    }


    /**
     * {@inheritDoc}
     */
    public Data processReporterData(Object[]      aoColumns,
                                    VisualVMModel model)
    {
        return null;    // implement when Coherence version supports this
    }
}
