/*
 * File: ExportableJTable.java
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

package com.sun.tools.visualvm.modules.coherence.panel.util;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import com.sun.tools.visualvm.modules.coherence.Localization;
import com.sun.tools.visualvm.modules.coherence.tablemodel.AbstractCoherenceTableModel;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Data;

/**
 * An implementation of a {@link JTable} that allows exporting table data as CSV.
 *
 * @author Tim Middleton
 */
public class ExportableJTable extends JTable implements ActionListener
{
    private static final long serialVersionUID = 5999795232769091368L;

    /**
     * The line separator for the platform this process is running on.
     */
    private static String LF = System.getProperty("line.separator");

    /**
     * File chooser to select a file.
     */
    private static JFileChooser fileChooser = null;

    /**
     * The logger object to use.
     */
    private static final Logger LOGGER = Logger.getLogger(ExportableJTable.class.getName());

    /**
     * Menu item for "Save As".
     */
    private JMenuItem menuItem;


    /**
     * Create the table.
     *
     * @param model the {@link TableModel} to base this {@link JTable} on
     */
    public ExportableJTable(TableModel model)
    {
        super(model);
        
        // ensure users can only ever select one row at a time
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        setShowGrid(true);
        String sOS = System.getProperty("os.name").toLowerCase();
        if (sOS.indexOf("windows") != -1)
        {
            setGridColor(UIManager.getColor("controlHighlight"));
        }
        else if (sOS.indexOf("mac") != -1)
        {
            setGridColor(Color.LIGHT_GRAY);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public JPopupMenu getComponentPopupMenu()
    {
        JPopupMenu menu = new JPopupMenu("Table Options:");

        menuItem = new JMenuItem(Localization.getLocalText("LBL_save_data_as"));

        menuItem.addActionListener(this);
        menu.add(menuItem);

        return menu;
    }


    /**
     * Respond to a right-click event for saving data to disk.
     *
     * @param event  the event
     */
    public void actionPerformed(ActionEvent event)
    {
        JComponent src = (JComponent) event.getSource();

        if (src.equals(menuItem))
        {
            int result = fileChooser.showSaveDialog(this);

            if (result == JFileChooser.APPROVE_OPTION)
            {
                saveTableDataToFile(fileChooser.getSelectedFile());
            }
        }
    }


    /**
     * Save the data for the table to a CSV file.
     *
     * @param file  the {@link File} to save to
     */
    private void saveTableDataToFile(File file)
    {
        PrintStream fileWriter = null;

        try
        {
            fileWriter = new PrintStream(new FileOutputStream(file));

            AbstractCoherenceTableModel<Object, Data> tableModel = (AbstractCoherenceTableModel) this.getModel();

            // Get the column headers

            TableColumnModel columnModel = this.getTableHeader().getColumnModel();
            int              columnCount = columnModel.getColumnCount();

            for (int i = 0; i < columnCount; i++)
            {
                fileWriter.print("\"" + columnModel.getColumn(i).getHeaderValue()
                                 + (i < columnCount - 1 ? "\"," : "\""));
            }

            fileWriter.print(LF);

            // output the data line by line
            for (int r = 0; r < tableModel.getRowCount(); r++)
            {
                for (int c = 0; c < columnCount; c++)
                {
                    Object oValue = tableModel.getValueAt(r, c);

                    fileWriter.print((oValue == null ? "" : oValue.toString()) + (c < columnCount - 1 ? "," : ""));
                }

                fileWriter.print(LF);
            }

        }
        catch (IOException ioe)
        {
            LOGGER.log(Level.WARNING,
                       Localization.getLocalText("LBL_unable_to_save",
                                                 new String[] {file.toString(), ioe.getMessage()}));
        }
        finally
        {
            if (fileWriter != null)
            {
                fileWriter.close();
            }
        }
    }


    /**
     * An implementation of a {@link JFileChooser} that will confirm overwrite of
     * an existing file.
     *
     */
    public static class CheckExistsFileChooser extends JFileChooser
    {
        /**
         * Default constructor.
         */
        public CheckExistsFileChooser()
        {
            super();
        }


        /**
         * Constructor taking a directory name to check for existence.
         *
         * @param sDirectory the directory to check for existence
         */
        public CheckExistsFileChooser(String sDirectory)
        {
            super(sDirectory);
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void approveSelection()
        {
            if (!validateFileSelection(this.getSelectedFile()))
            {
            }
            else
            {
                super.approveSelection();
            }
        }


        /**
         * If a file exists, then ensure you ask the user if they want to
         * overwrite it.
         *
         * @param file  the {@link File} that was selected
         *
         * @return true if the file does not exist or the user wants to overwrite it
         */
        private boolean validateFileSelection(File file)
        {
            if (file.exists())
            {
                String sQuestion = Localization.getLocalText("LBL_file_already_exists",
                                                             new String[] {file.getAbsolutePath()});

                if (JOptionPane.showConfirmDialog(null, sQuestion, Localization.getLocalText("LBL_confirm"),
                                                  JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                {
                    return true;
                }

                return false;
            }

            return true;
        }
    }


    /**
     * Initialize so that we only get one instance.
     */
    static
    {
        fileChooser = new CheckExistsFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter(Localization.getLocalText("LBL_csv_file"), "csv"));
    }
}
