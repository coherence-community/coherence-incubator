/*
 * File: VisualVMViewProvider.java
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

import com.sun.tools.visualvm.application.Application;

import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.DataSourceViewProvider;
import com.sun.tools.visualvm.core.ui.DataSourceViewsManager;

import com.sun.tools.visualvm.tools.jmx.JmxModel;
import com.sun.tools.visualvm.tools.jmx.JmxModelFactory;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

/**
 * Class to provide the view for Coherence JVisualVM plugin.
 * 
 * @author Tim Middleton
 */
public class VisualVMViewProvider extends DataSourceViewProvider<Application>
{

    private static DataSourceViewProvider<Application> instance = new VisualVMViewProvider();


    /**
     * Returns true or false indicating if the JMX connection is actually for
     * a Coherence cluster or not.
     *
     * @return true if a Coherence cluster otherwise false
     */
    @Override
    public boolean supportsViewFor(Application application)
    {
        JmxModel jmx = JmxModelFactory.getJmxModelFor(application);

        // system property for disabling the MBean check as with connecting to WLS, sometimes
        // the Coherence MBean does not show up immediately and therefore the tab never gets
        // displayed
        String sDisableCheck = System.getProperty("com.oracle.coherence.jvisualvm.disable.mbean.check");

        if (jmx != null && jmx.getConnectionState() == JmxModel.ConnectionState.CONNECTED)
        {
            if (sDisableCheck != null && "true".equals(sDisableCheck))
            {
                return true;
            }

            MBeanServerConnection connection = jmx.getMBeanServerConnection();

            try
            {
                if (connection.isRegistered(new ObjectName("Coherence:type=Cluster")))
                {
                    return true;
                }
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }

        return false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected DataSourceView createView(Application application)
    {
        return new VisualVMView(application);
    }


    /**
     * Supports Save views.
     *
     * @param app {@link Application}
     *
     * @return true if support for save views
     */
    public boolean supportsSaveViewsFor(Application app)
    {
        return false;
    }


    /**
     * Save views.
     *
     * @param appSource source {@link Application}
     * @param appDest   destination {@link Application}
     */
    public void saveViews(Application appSource,
                          Application appDest)
    {
    }


    /**
     * Initialize a new the view provider.
     */
    static void initialize()
    {
        DataSourceViewsManager.sharedInstance().addViewProvider(instance, Application.class);
    }


    /**
     * Unregister the view provider.
     */
    static void unregister()
    {
        DataSourceViewsManager.sharedInstance().removeViewProvider(instance);
    }
}
