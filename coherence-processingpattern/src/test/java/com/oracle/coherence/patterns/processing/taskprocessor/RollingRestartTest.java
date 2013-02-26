/*
 * File: RollingRestartTest.java
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

package com.oracle.coherence.patterns.processing.taskprocessor;

import com.oracle.coherence.common.network.AvailablePortIterator;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.UnknownHostException;
import java.util.Date;

/**
 * Rolling Restart Test
 * <p>
 * Copyright (c) 2009. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Noah Arliss
 */
public class RollingRestartTest
{
    private final static int             cServers     = 2;
    private final static int             cSubmissions = 250;
    private final static int             cIterations  = 5;
    private static AvailablePortIterator portIterator;
    private final static long            CLUSTER_JOIN_TIMEOUT = 10000;


    /**
     * Method description
     *
     * @throws UnknownHostException
     */
    @BeforeClass
    public static void setup() throws UnknownHostException
    {
        portIterator = new AvailablePortIterator(40000);
    }


    /**
     * Method description
     *
     * @throws Throwable
     */
    @Test
    public void testRollingRestart() throws Throwable
    {
//      JavaApplication servers[] = new JavaApplication[cServers];
//      try
//      {
//          System.setProperty("tangosol.coherence.log", "client.txt");
//          int clusterPort = portIterator.next();
//          CoherenceServerBuilder builder = new CoherenceServerBuilder()
//              .setEnvironmentVariables(PropertiesBuilder.fromCurrentEnvironmentVariables())
//              .setSystemProperties(PropertiesBuilder.fromCurrentSystemProperties()).setClusterName("testCluster")
//              .setCacheConfigURI("coherence-processingpattern-restart-cache-config.xml")
//              .setLocalHostAddress("127.0.0.1").setMulticastTTL(0)
//              .setPofConfigURI("coherence-processingpattern-test-pof-config.xml").setClusterPort(clusterPort)
//              .setLogLevel(6);
//          Properties props = new Properties();
//          props.putAll(builder.getSystemPropertiesBuilder().realize(null));
//          System.setProperties(props);
//          System.setProperty(CoherenceServerBuilder.PROPERTY_DISTRIBUTED_LOCALSTORAGE, "false");
//          for (int x = 0; x < cServers; x++)
//          {
//              servers[x] = builder.realize("TestServer" + x, new LogApplicationConsole("TestServer" + x + ".log"));
//          }
//          wait(CLUSTER_JOIN_TIMEOUT * cServers, "for test servers to start.");
//          
//          //setup processing session
//          ProcessingSession session = new DefaultProcessingSession(
//              StringBasedIdentifier.newInstance("TaskExecutionSample"
//                      + DateFormat.getDateTimeInstance().format(System.currentTimeMillis())));
//          SubmissionConfiguration config = new DefaultSubmissionConfiguration();
//          // Lets do cIterations rolling restarts
//          for (int x = 0; x < cIterations; ++x)
//          {
//              SubmissionOutcome[] results = new SubmissionOutcome[cSubmissions];
//              System.out.println("Submitting work to the grid...");
//              //Submit cSubmissions to the cluster for processing
//              for (int y = 0; y < cSubmissions; y++)
//              {
//                  String id = "Task[" + x + ":" + y + "]";
//                  results[y] = session.submit(new RestartTask(id), config, StringBasedIdentifier.newInstance(id), SubmissionRetentionPolicy.RemoveOnFinalState, null);
//              }
//              //wait for results to come back
//              for (int y = 0; y < cSubmissions; y++)
//              {
//                  System.out.println("Client received result from: " + results[y].get());
//              }
//              //Now lets do a rolling restart
//              for (int y = 0; y < cServers; y++)
//              {
//                  System.out.println("Stopping server " + y);
//                  servers[y].destroy();
//                  wait(CLUSTER_JOIN_TIMEOUT, "for test server " + y + " to stop");
//                  System.out.println("Starting server " + y);
//                  servers[y] = builder.realize("TestServer" + y, new LogApplicationConsole("TestServer"+y+".log"));
//                  wait(CLUSTER_JOIN_TIMEOUT, "for test server " + y + " to start");
//              }
//              //Now that we've done a rolling restart let the loop submit work again.
//              System.out.println("Iteration " + x + " done successfully...");
//          }
//          System.out.println("All done successfully...");
//      }
//      catch (Throwable e)
//      {
//          System.out.println(e);
//          e.printStackTrace();
//          throw e;
//      }
//      finally
//      {
//          for (int x = 0; x < cServers; x++)
//          {
//              servers[x].destroy();
//          }
//      }
    }


    /**
     * <p>A simple method to wait a specified amount of time, with a message to stdout.</p>
     *
     * @param timeMs The time to wait in ms.
     * @param rationale The rationale (message) for waiting.
     * @throws InterruptedException When interrupted while waiting.
     */
    public void wait(long   time,
                     String rationale) throws InterruptedException
    {
        System.out.printf("%s: Waiting %dms %s\n", new Date(), time, rationale);
        Thread.sleep(time);
    }
}
