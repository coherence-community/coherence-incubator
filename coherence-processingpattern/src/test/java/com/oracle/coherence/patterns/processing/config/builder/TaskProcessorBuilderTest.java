package com.oracle.coherence.patterns.processing.config.builder;


import com.oracle.coherence.patterns.processing.config.ProcessingPatternConfig;
import com.oracle.coherence.patterns.processing.config.xml.ProcessingPatternNamespaceHandler;
import com.oracle.coherence.patterns.processing.task.TaskProcessor;
import com.tangosol.coherence.config.CacheConfig;
import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import org.junit.Test;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.SystemMenuBar;

import static junit.framework.Assert.assertNotNull;

/**
 * TODO: Fill in class description.
 *
 * @author pfm  2013.06.10
 */
public class TaskProcessorBuilderTest
    {
    @Test
    public void testGroupMultiNode()
        {
                          /*
        String sXml = "" +
                "<cache-config xmlns:processing=\"class:com.oracle.coherence.patterns.processing.config.xml.ProcessingPatternNamespaceHandler\">" +
                "<processing:cluster-config>" +
                "      <processing:taskprocessors>"+
                "          <processing:taskprocessordefinition id=\"GridTaskProcessor\" displayname=\"Grid Task Processor\"" +
                "               type=\"GRID\">" +
                "               <processing:default-taskprocessor id=\"GridTaskProcessor\" threadpoolsize=\"2\" />" +
                "               <processing:attribute name=\"type\">grid</processing:attribute>" +
                "          </processing:taskprocessordefinition>" +
                "      </processing:taskprocessors>"+
                "   </processing:cluster-config>"+
                " </cache-config>" +
                "";          */



        String sXml = "" +
                "<cache-config>" +
                "<processing:cluster-config xmlns:processing=\"class:com.oracle.coherence.patterns.processing.config.xml.ProcessingPatternNamespaceHandler\">" +
                "      <processing:taskprocessors>"+
                "          <processing:taskprocessordefinition id=\"GridTaskProcessor\" displayname=\"Grid Task Processor\"" +
                "               type=\"GRID\">" +
                "               <processing:default-taskprocessor id=\"GridTaskProcessor\" threadpoolsize=\"10\" />" +
                "               <processing:attribute name=\"type\">grid</processing:attribute>" +
                "          </processing:taskprocessordefinition>" +
                "      </processing:taskprocessors>"+
                "   </processing:cluster-config>"+
                " </cache-config>" +
                "";


        XmlElement element = XmlHelper.loadXml(sXml);

        CacheConfig cacheConfig  =  (CacheConfig) CodiTestHelper.processXml(sXml);

       // ProcessingPatternConfig ppConfig = cacheConfig.g


      //  TaskProcessor taskProcessor = bldr.realize();
      //  assertNotNull(taskProcessor);
        }
    }

