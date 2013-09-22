/*
 * File: AbstractCoherencePanel.java
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

package com.sun.tools.visualvm.modules.coherence.panel;

import com.sun.tools.visualvm.modules.coherence.Localization;
import com.sun.tools.visualvm.modules.coherence.VisualVMModel;

import java.awt.Component;
import java.awt.LayoutManager;

import javax.management.MBeanServerConnection;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * An abstract implementation of a {@link JPanel} which provides basic support
 * to be displayed as JVisualVM plug-in.
 *
 * @author Tim Middleton
 */
public abstract class AbstractCoherencePanel extends JPanel
{
    private static final long serialVersionUID = -7607701492285533521L;

    /**
     * Filler for spacing.
     */
    private static final String FILLER = "   ";

    /**
     * The connection to the JMX MBean server.
     */
    protected MBeanServerConnection server;

    /**
     * The visualVM model.
     */
    protected final VisualVMModel model;


    /**
     * Create an instance of the panel which will be used to display
     * data retrieved from JMX.
     *
     * @param  manager the {@link LayoutManager} to use
     * @param  model   the {@link VisualVMModel} to interrogate for data
     */
    public AbstractCoherencePanel(LayoutManager manager,
                                  VisualVMModel model)
    {
        super(manager);
        this.model = model;
        this.setOpaque(true);
    }


    /**
     * Called to update any GUI related artifacts. Called from done().
     */
    public abstract void updateGUI();


    /**
     * Update any data. Called from doInBackground().
     */
    public abstract void updateData();


    /**
     * Set the {@link MBeanServerConnection} to get JMX data from.
     *
     * @param mbs  the {@link MBeanServerConnection} to get JMX data from
     */
    public void setMBeanServerConnection(MBeanServerConnection mbs)
    {
        this.server = mbs;
    }


    /**
     * Create a {@link JLabel} with the specified localized text and set the
     * {@link Component} that the label is for to help with accessibility.
     *
     * @param sKey      the key to look up in Bundle.properties
     * @param component the {@link Component} that the label is for or null
     *
     * @return a {@link JLabel} with the specified text
     */
    protected JLabel getLocalizedLabel(String    sKey,
                                       Component component)
    {
        JLabel label = new JLabel();

        label.setText(Localization.getLocalText(sKey) + ":");

        if (component != null)
        {
            label.setLabelFor(component);
        }

        return label;
    }


    /**
     * Create a {@link JLabel} with the specified localized text.
     *
     * @param sKey  the key to look up in Bundle.properties
     *
     * @return a {@link JLabel} with the specified text
     */
    protected JLabel getLocalizedLabel(String sKey)
    {
        return getLocalizedLabel(sKey, (Component) null);
    }


    /**
     * Return localized text given a key.
     *
     * @param sKey  the key to look up in Bundle.properties
     *
     * @return localized text given a key
     */
    protected String getLocalizedText(String sKey)
    {
        return Localization.getLocalText(sKey);
    }


    /**
     * Return a label which is just a filler.
     *
     * @return a label which is just a filler
     *
     */
    protected JLabel getFiller()
    {
        JLabel label = new JLabel();

        label.setText(FILLER);

        return label;
    }


    /**
     * Create a {@link JTextField} with the specified width and make it
     * right aligned.
     *
     * @param width  the width for the {@link JTextField}
     *
     * @return the newly created text field
     */
    protected JTextField getTextField(int width)
    {
        return getTextField(width, JTextField.RIGHT);
    }


    /**
     * Create a {@link JTextField} with the specified width and specified
     * alignment.
     *
     * @param  width  the width for the {@link JTextField}
     * @param  align  either {@link JTextField}.RIGHT or LEFT
     *
     * @return the newly created text field
     */
    protected JTextField getTextField(int width,
                                      int align)
    {
        JTextField textField = new JTextField();

        textField.setEditable(false);
        textField.setColumns(width);
        textField.setHorizontalAlignment(align);

        textField.setOpaque(true);

        return textField;
    }
}
