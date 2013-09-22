/*
 * File: TestPanelMain.java
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

package com.sun.tools.visualvm.modules.coherence;

import com.sun.tools.visualvm.modules.coherence.panel.CoherenceCachePanel;
import com.sun.tools.visualvm.modules.coherence.panel.CoherenceClusterOverviewPanel;
import com.sun.tools.visualvm.modules.coherence.panel.CoherenceMachinePanel;
import com.sun.tools.visualvm.modules.coherence.panel.CoherenceMemberPanel;
import com.sun.tools.visualvm.modules.coherence.panel.CoherenceProxyPanel;
import com.sun.tools.visualvm.modules.coherence.panel.CoherenceServicePanel;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import java.io.IOException;

import java.net.MalformedURLException;

import java.util.Timer;
import java.util.TimerTask;

import javax.management.MBeanServerConnection;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import javax.swing.border.EmptyBorder;

/**
 * Test driver for panels.
 *
 * @author Tim Middleton.
 */
public class TestPanelMain
{
    /**
     * The singleton which holds and refreshes data.
     */
    private static VisualVMModel clusterStats = null;

    /**
     * Connection to JMX MBean server.
     */
    private static MBeanServerConnection server;


    private static void createAndShowGUI(JPanel jtop)
    {
        // Create and set up the window.
        JFrame frame = new JFrame("Coherence");

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create and set up the content pane.
        JComponent contentPane = (JComponent) frame.getContentPane();

        contentPane.add(jtop, BorderLayout.CENTER);
        contentPane.setOpaque(true);    // content panes must be opaque
        contentPane.setBorder(new EmptyBorder(12, 12, 12, 12));
        frame.setContentPane(contentPane);

        // Display the window.
        frame.pack();
        frame.setVisible(true);
    }


    /**
     * Main entry point to test the plugin. This will create a panel with the
     * tabs to display the various statistics outside of JVisualVM.
     * Provide a hostname and port to connect to.  The default is localhost and 10001.<br>
     * 
     * Note: You must also supply the jvisualvm dependencies (identified in pom.xml), on the classpath
     * to run this.
     *
     * @param args arguments to main  hostname and port
     *
     * @throws Exception if timer is interrupted
     */
    public static void main(String[] args) throws Exception
    {
    	String sHostname = "localhost";
    	int    nPort     = 10001;
    	
    	if (args.length == 2) {
    		sHostname = args[0];
    		nPort     = Integer.parseInt(args[1]);
    	}

        server = connect(sHostname, nPort);

        VisualVMModel                       model                = VisualVMModel.getInstance();

        final CoherenceClusterOverviewPanel clusterOverviewPanel = new CoherenceClusterOverviewPanel(model);
        final CoherenceMemberPanel          memberPanel          = new CoherenceMemberPanel(model);
        final CoherenceServicePanel         servicePanel         = new CoherenceServicePanel(model);
        final CoherenceCachePanel           cachePanel           = new CoherenceCachePanel(model);
        final CoherenceProxyPanel           proxyPanel           = new CoherenceProxyPanel(model);
        final CoherenceMachinePanel         machinePanel         = new CoherenceMachinePanel(model);

        cachePanel.setMBeanServerConnection(server);
        memberPanel.setMBeanServerConnection(server);
        clusterOverviewPanel.setMBeanServerConnection(server);
        servicePanel.setMBeanServerConnection(server);
        proxyPanel.setMBeanServerConnection(server);
        machinePanel.setMBeanServerConnection(server);

        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Coherence Overview", clusterOverviewPanel);
        tabbedPane.addTab("Coherence Machines", machinePanel);
        tabbedPane.addTab("Coherence Members", memberPanel);
        tabbedPane.addTab("Coherence Services", servicePanel);
        tabbedPane.addTab("Coherence Caches", cachePanel);
        tabbedPane.addTab("Coherence Proxies", proxyPanel);

        final JPanel top = new JPanel(new GridLayout(1, 1));

        top.add(tabbedPane);

        // A timer task to update GUI per each interval
        TimerTask timerTask = new TimerTask()
        {
            @Override
            public void run()
            {
                // Schedule the SwingWorker to update the GUI
                clusterStats.refreshJMXStatistics(server);

                clusterOverviewPanel.updateData();
                clusterOverviewPanel.updateGUI();
                memberPanel.updateData();
                memberPanel.updateGUI();
                servicePanel.updateData();
                servicePanel.updateGUI();
                cachePanel.updateData();
                cachePanel.updateGUI();
                proxyPanel.updateData();
                proxyPanel.updateGUI();
                machinePanel.updateData();
                machinePanel.updateGUI();
            }
        };

        // Create the standalone window with TestPanel panel
        // by the event dispatcher thread
        SwingUtilities.invokeAndWait(new Runnable()
        {
            public void run()
            {
                createAndShowGUI(top);
            }

        });

        // refresh every 15 seconds
        Timer timer = new Timer("TestPanel Sampling thread");

        timer.schedule(timerTask, 0, 15000);

    }


    /**
     * Create a {@link MBeanServerConnection} given a hostname and port
     *
     * @param hostname  the hostname to connect to
     * @param port      the port to connect to
     *
     * @return a new {@link MBeanServerConnection}
     */
    private static MBeanServerConnection connect(String hostname,
                                                 int    port)
    {
        String                urlPath = "/jndi/rmi://" + hostname + ":" + port + "/jmxrmi";
        MBeanServerConnection server  = null;

        try
        {
            JMXServiceURL url  = new JMXServiceURL("rmi", "", 0, urlPath);
            JMXConnector  jmxc = JMXConnectorFactory.connect(url);

            server = jmxc.getMBeanServerConnection();
        }
        catch (MalformedURLException e)
        {
        }
        catch (IOException e)
        {
            System.err.println("\nCommunication error: " + e.getMessage());
            System.exit(1);
        }

        return server;
    }
}
