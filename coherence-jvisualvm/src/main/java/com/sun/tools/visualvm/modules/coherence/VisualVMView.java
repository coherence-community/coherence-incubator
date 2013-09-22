/*
 * File: VisualVMView.java
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

import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;

import com.sun.tools.visualvm.application.Application;

import com.sun.tools.visualvm.core.options.GlobalPreferences;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;

import com.sun.tools.visualvm.modules.coherence.panel.CoherenceCachePanel;
import com.sun.tools.visualvm.modules.coherence.panel.CoherenceClusterOverviewPanel;
import com.sun.tools.visualvm.modules.coherence.panel.CoherenceHttpSessionPanel;
import com.sun.tools.visualvm.modules.coherence.panel.CoherenceMachinePanel;
import com.sun.tools.visualvm.modules.coherence.panel.CoherenceMemberPanel;
import com.sun.tools.visualvm.modules.coherence.panel.CoherencePersistencePanel;
import com.sun.tools.visualvm.modules.coherence.panel.CoherenceProxyPanel;
import com.sun.tools.visualvm.modules.coherence.panel.CoherenceServicePanel;

import com.sun.tools.visualvm.tools.jmx.JmxModel;
import com.sun.tools.visualvm.tools.jmx.JmxModelFactory;

import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.logging.Logger;

import javax.management.MBeanServerConnection;

import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.Timer;

/**
 * The implementation of the {@link DataSourceView} for displaying the
 * Coherence Cluster Snapshot tab.
 *
 * @author Tim Middleton
 */
public class VisualVMView extends DataSourceView
{
    /**
     * The Coherence standard icon to use.
     */
    private static final String IMAGE_PATH = "com/sun/tools/visualvm/modules/coherence/coherence_grid_icon.png";    // NOI18N

    /**
     * The logger object to use.
     */
    private static final Logger LOGGER = Logger.getLogger(VisualVMView.class.getName());

    /**
     * Component used to display the tabs.
     */
    private DataViewComponent dvc;

    /**
     * Timer used to refresh the screen
     */
    private Timer       timer;
    private Application application;

    /**
     * Indicates if the refresh is running.
     */
    private boolean refreshRunning;

    /**
     * Connection to the MBean server.
     */
    private MBeanServerConnection server = null;


    /**
     * Creates the new instance of the tab.
     *
     * @param application
     */
    public VisualVMView(Application application)
    {
        super(application,
              "Oracle Coherence",
              new ImageIcon(Utilities.loadImage(IMAGE_PATH, true)).getImage(),
              60,
              false);
        this.application = application;
    }


    /**
     * Create the new {@link DataViewComponent} which will display all the
     * Coherence related information.
     */
    protected DataViewComponent createComponent()
    {
        final VisualVMModel model = VisualVMModel.getInstance();

        // Data area for master view
        JEditorPane       generalDataArea  = new JEditorPane();
        final JTabbedPane pneCoherenceTabs = new JTabbedPane();

        pneCoherenceTabs.setOpaque(false);

        final CoherenceClusterOverviewPanel pnlClusterOverview = new CoherenceClusterOverviewPanel(model);
        final CoherenceMachinePanel         pnlMachine         = new CoherenceMachinePanel(model);
        final CoherenceMemberPanel          pnlMember          = new CoherenceMemberPanel(model);
        final CoherenceServicePanel         pnlService         = new CoherenceServicePanel(model);
        final CoherenceCachePanel           pnlCache           = new CoherenceCachePanel(model);
        final CoherenceProxyPanel           pnlProxy           = new CoherenceProxyPanel(model);
        final CoherencePersistencePanel     pnlPersistence     = new CoherencePersistencePanel(model);
        final CoherenceHttpSessionPanel     pnlHttpSession     = new CoherenceHttpSessionPanel(model);

        if (application == null)
        {
            throw new RuntimeException("Application is null");
        }

        JmxModel jmx = JmxModelFactory.getJmxModelFor(application);

        server = jmx.getMBeanServerConnection();

        // do an initial refresh of the data so we can see if we need to display
        // the proxy server tab
        model.refreshJMXStatistics(server);

        pneCoherenceTabs.addTab(Localization.getLocalText("LBL_cluster_overview"), pnlClusterOverview);
        pneCoherenceTabs.addTab(Localization.getLocalText("LBL_machines"), pnlMachine);
        pneCoherenceTabs.addTab(Localization.getLocalText("LBL_members"), pnlMember);
        pneCoherenceTabs.addTab(Localization.getLocalText("LBL_services"), pnlService);
        pneCoherenceTabs.addTab(Localization.getLocalText("LBL_caches"), pnlCache);

        // only add the proxy tab if we have proxy servers
        if (model.isCoherenceExtendConfigured())
        {
            pneCoherenceTabs.addTab(Localization.getLocalText("LBL_proxy_servers"), pnlProxy);
        }

        if (model.is1213AndAbove())
        {
            pneCoherenceTabs.addTab(Localization.getLocalText("LBL_persistence"), pnlPersistence);
        }

        if (model.isCoherenceWebConfigured())
        {
            pneCoherenceTabs.addTab(Localization.getLocalText("LBL_Coherence_web"), pnlHttpSession);
        }

        final JPanel panel = new JPanel(new GridLayout(1, 1));

        panel.add(pneCoherenceTabs);

        // Master view:
        DataViewComponent.MasterView masterView =
            new DataViewComponent.MasterView(Localization.getLocalText("LBL_cluster_information"),
                                             null,
                                             generalDataArea);

        // Configuration of master view:
        DataViewComponent.MasterViewConfiguration masterConfiguration =
            new DataViewComponent.MasterViewConfiguration(false);

        // Add the master view and configuration view to the component:
        dvc = new DataViewComponent(masterView, masterConfiguration);

        // Add detail views to the component:
        dvc.addDetailsView(new DataViewComponent.DetailsView(null, null, 10, panel, null), DataViewComponent.TOP_LEFT);

        // update the JMX server connection
        pnlClusterOverview.setMBeanServerConnection(server);
        pnlMachine.setMBeanServerConnection(server);
        pnlMember.setMBeanServerConnection(server);
        pnlService.setMBeanServerConnection(server);
        pnlCache.setMBeanServerConnection(server);
        pnlProxy.setMBeanServerConnection(server);
        pnlPersistence.setMBeanServerConnection(server);
        pnlHttpSession.setMBeanServerConnection(server);

        // create a timer that will refresh the TAB's as required
        timer = new Timer(GlobalPreferences.sharedInstance().getMonitoredDataPoll() * 1000, new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (refreshRunning)
                {
                    return;
                }

                refreshRunning = true;
                RequestProcessor.getDefault().post(new Runnable()
                {
                    public void run()
                    {
                        try
                        {
                            if (application.getState() == Application.STATE_AVAILABLE)
                            {
                                // Schedule the SwingWorker to update the GUI
                                model.refreshJMXStatistics(server);

                                pnlClusterOverview.updateData();
                                pnlClusterOverview.updateGUI();
                                pnlMember.updateData();
                                pnlMember.updateGUI();
                                pnlService.updateData();
                                pnlService.updateGUI();
                                pnlCache.updateData();
                                pnlCache.updateGUI();

                                // only update proxy tab if there are proxies configured
                                if (model.isCoherenceExtendConfigured())
                                {
                                    pnlProxy.updateData();
                                    pnlProxy.updateGUI();
                                }

                                pnlMachine.updateData();
                                pnlMachine.updateGUI();

                                if (model.is1213AndAbove())
                                {
                                    pnlPersistence.updateData();
                                    pnlPersistence.updateGUI();
                                }

                                if (model.isCoherenceWebConfigured())
                                {
                                    pnlHttpSession.updateData();
                                    pnlHttpSession.updateGUI();
                                }
                            }
                        }
                        catch (Exception ex)
                        {
                            LOGGER.throwing(this.getClass().getName(), "refresh", ex);
                        }
                        finally
                        {
                            refreshRunning = false;
                        }
                    }

                });
            }

        });
        timer.setInitialDelay(800);
        timer.start();

        // getDataSource().notifyWhenRemoved(this);

        return dvc;

    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void removed()
    {
        timer.stop();
    }


    /**
     * Called on removal.
     *
     * @param app {@link Application} to remove
     */
    public void dataRemoved(Application app)
    {
        timer.stop();
    }
}
