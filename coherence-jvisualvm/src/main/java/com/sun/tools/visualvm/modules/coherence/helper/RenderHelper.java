/*
 * File: RenderHelper.java
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

package com.sun.tools.visualvm.modules.coherence.helper;

import com.sun.tools.visualvm.modules.coherence.Localization;

import java.awt.Color;
import java.awt.Component;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.JLabel;
import javax.swing.JTable;

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * Various methods to help in rendering table rows and columns.
 *
 * @author Tim Middleton
 *
 */
public class RenderHelper
{
    /**
     * Format for millis renderer.
     */
    public static NumberFormat MILLIS_FORMAT = new DecimalFormat("#,##0.0000");

    /**
     * Format for load average renderer.
     */
    public static NumberFormat LOAD_AVERAGE_FORMAT = new DecimalFormat("#,##0.00");

    /**
     * Format for % renderer.
     */
    public static NumberFormat PERCENT_FORMAT = new DecimalFormat("##0%");

    /**
     * Format for integer renderer.
     */
    public static NumberFormat INTEGER_FORMAT = new DecimalFormat("##,###,###,###,###");

    /**
     * Node safe tool tip.
     */
    public static final String NODE_SAFE_TOOLTIP = Localization.getLocalText("TTIP_node_safe");

    /**
     * Machine safe tool tip.
     */
    public static final String MACHINE_SAFE_TOOLTIP = Localization.getLocalText("TTIP_machine_safe");

    /**
     * Rack safe tool tip.
     */
    public static final String RACK_SAFE_TOOLTIP = Localization.getLocalText("TTIP_rack_safe");

    /**
     * Site safe tool tip.
     */
    public static final String SITE_SAFE_TOOLTIP = Localization.getLocalText("TTIP_site_safe");

    /**
     * Endangered tool tip.
     */
    public static final String ENDANGERED_TOOLTIP = Localization.getLocalText("TTIP_endangered");


    /**
     * Set the renderer for a particular table and column.
     *
     * @param table    the {@link JTable} to set the renderer for
     * @param col      the column, index of 0, to set the renderer for
     * @param renderer the {@link TableCellRenderer} to apply to the table and column
     */
    public static void setColumnRenderer(JTable            table,
                                         int               col,
                                         TableCellRenderer renderer)
    {
        TableColumn column = table.getColumnModel().getColumn(col);

        if (column == null)
        {
            throw new IllegalArgumentException("No column number " + col + " for table");
        }

        column.setCellRenderer(renderer);
    }


    /**
     * Set the renderer to a MillisRenderer() for a particular table and column
     *
     * @param table the {@link JTable} to set the renderer for
     * @param col   the column, index of 0, to set the renderer for
     */
    public static void setMillisRenderer(JTable table,
                                         int    col)
    {
        TableColumn column = table.getColumnModel().getColumn(col);

        if (column == null)
        {
            throw new IllegalArgumentException("No column number " + col + " for table");
        }

        column.setCellRenderer(new DecimalRenderer());
    }


    /**
     * Set the renderer to a IntegerRenderer() for a particular table and column
     *
     * @param table the {@link JTable} to set the renderer for
     * @param col   the column, index of 0, to set the renderer for
     */
    public static void setIntegerRenderer(JTable table,
                                          int    col)
    {
        TableColumn column = table.getColumnModel().getColumn(col);

        if (column == null)
        {
            throw new IllegalArgumentException("No column number " + col + " for table");
        }

        column.setCellRenderer(new IntegerRenderer());
    }


    /**
     * Sets the default table header alignment for all columns for a table.
     *
     * @param table the {@link JTable} to set the alignment for
     * @param align the alignment, either {@link JLabel}.RIGHT or {@link JLabel}.LEFT.
     */
    public static void setHeaderAlignment(JTable table,
                                          int    align)
    {
        DefaultTableCellRenderer renderer = ((DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer());

        renderer.setHorizontalAlignment(align);
        table.getTableHeader().setDefaultRenderer(renderer);
    }


    /**
     * Sets the default table header alignment for a particular column for a table.
     *
     * @param table the {@link JTable} to set the alignment for
     * @param col   the column to set alignment for
     * @param align the alignment, either {@link JLabel}.RIGHT or {@link JLabel}.LEFT.
     */
    public static void setHeaderAlignment(JTable table,
                                          int    col,
                                          int    align)
    {
        DefaultTableCellRenderer renderer = ((DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer());

        renderer.setHorizontalAlignment(align);
        table.getColumn(table.getColumnName(col)).setHeaderRenderer(renderer);
    }


    /**
     * Renderer for cache hit rate.
     */
    @SuppressWarnings("serial")
    public static class CacheHitProbabilityRateRenderer extends DefaultTableCellRenderer
    {
        /**
        * {@inheritDoc}
        */
        @Override
        public Component getTableCellRendererComponent(JTable  table,
                                                       Object  value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int     row,
                                                       int     column)
        {
            Component c      = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            float     fValue = new Float(getText());

            if (fValue <= 0.500)
            {
                setBackground(Color.red);
                setForeground(Color.white);
            }
            else if (fValue <= 0.75)
            {
                setBackground(Color.orange);
                setForeground(Color.white);
            }
            else
            {
                setBackground(Color.white);
                setForeground(Color.black);
            }

            if (c instanceof JLabel && value instanceof Number)
            {
                JLabel renderedLabel = (JLabel) super.getTableCellRendererComponent(table,
                                                                                    value,
                                                                                    isSelected,
                                                                                    hasFocus,
                                                                                    row,
                                                                                    column);

                renderedLabel.setHorizontalAlignment(JLabel.RIGHT);

                String text = MILLIS_FORMAT.format((Number) value);

                renderedLabel.setText(text);
            }

            return c;
        }
    }


    /**
     * Render for a milliseconds column of type Float.
     *
     */
    @SuppressWarnings("serial")
    public static class DecimalRenderer extends DefaultTableCellRenderer
    {
        /**
         * Default the format to millis.
         */
        private NumberFormat numberFormat = MILLIS_FORMAT;


        /**
         * Construct an MillisRenderer with a default alignment of RIGHT and
         * default millis renderer.
         */
        public DecimalRenderer()
        {
            super();
            setHorizontalAlignment(JLabel.RIGHT);
        }


        /**
         * Construct an MillisRenderer with a default alignment of RIGHT and
         * supplied {@link NumberFormat}.
         *
         * @param sFormat  the {@link NumberFormat} to use to render
         */
        public DecimalRenderer(NumberFormat sFormat)
        {
            super();
            this.numberFormat = sFormat;
            setHorizontalAlignment(JLabel.RIGHT);
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public Component getTableCellRendererComponent(JTable  table,
                                                       Object  value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int     row,
                                                       int     column)
        {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (c instanceof JLabel && value instanceof Number)
            {
                JLabel renderedLabel = (JLabel) super.getTableCellRendererComponent(table,
                                                                                    value,
                                                                                    isSelected,
                                                                                    hasFocus,
                                                                                    row,
                                                                                    column);

                renderedLabel.setHorizontalAlignment(JLabel.RIGHT);

                String text = numberFormat.format((Number) value);

                renderedLabel.setText(text);
            }

            return c;
        }
    }


    /**
     * Renderer for the free memory percent.
     */
    public static class FreeMemoryRenderer extends DefaultTableCellRenderer
    {
        /**
        * {@inheritDoc}
        */
        @Override
        public Component getTableCellRendererComponent(JTable  table,
                                                       Object  value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int     row,
                                                       int     column)
        {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!"".equals(getText()) && getText() != null)
            {
                float fValue = new Float(getText());

                if (fValue < .15)
                {
                    setBackground(Color.red);
                    setForeground(Color.white);
                    setToolTipText("Warning: This machine has less than 15% free physical memory.");
                }
                else if (fValue < 0.25)
                {
                    setBackground(Color.orange);
                    setForeground(Color.black);
                    setToolTipText("Warning: This machine has less than 25% free physical memory.");
                }
                else
                {
                    setBackground(Color.white);
                    setForeground(Color.black);
                }

                if (c instanceof JLabel && value instanceof Number)
                {
                    JLabel renderedLabel = (JLabel) super.getTableCellRendererComponent(table,
                                                                                        value,
                                                                                        isSelected,
                                                                                        hasFocus,
                                                                                        row,
                                                                                        column);

                    renderedLabel.setHorizontalAlignment(JLabel.RIGHT);

                    String text = PERCENT_FORMAT.format((Number) value);

                    renderedLabel.setText(text);
                }
            }
            else
            {
                setBackground(Color.white);
                setForeground(Color.black);
            }

            return c;
        }
    }


    /**
     * Render for a standard Integer.
     *
     */
    @SuppressWarnings("serial")
    public static class IntegerRenderer extends DefaultTableCellRenderer
    {
        /**
         * Construct an IntegerRenderer with a default alignment of RIGHT.
         */
        public IntegerRenderer()
        {
            super();
            setHorizontalAlignment(JLabel.RIGHT);
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public Component getTableCellRendererComponent(JTable  table,
                                                       Object  value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int     row,
                                                       int     column)
        {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (c instanceof JLabel && (value instanceof Integer || value instanceof Long))
            {
                JLabel label = (JLabel) c;

                label.setHorizontalAlignment(JLabel.RIGHT);

                String text = INTEGER_FORMAT.format((Number) value);

                label.setText(text);
            }

            return c;
        }
    }


    /**
     * Renderer for a statusHA column.
     */
    @SuppressWarnings("serial")
    public static class StatusHARenderer extends DefaultTableCellRenderer
    {
        /**
        * {@inheritDoc}
        */
        @Override
        public Component getTableCellRendererComponent(JTable  table,
                                                       Object  value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int     row,
                                                       int     column)
        {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if ("ENDANGERED".equals(getText()))
            {
                setBackground(Color.red);
                setForeground(Color.white);
                setToolTipText(ENDANGERED_TOOLTIP);
            }
            else if ("NODE-SAFE".equals(getText()))
            {
                setBackground(Color.orange);
                setForeground(Color.black);
                setToolTipText(NODE_SAFE_TOOLTIP);
            }
            else if ("n/a".equals(getText()))
            {
                setBackground(Color.white);
                setForeground(Color.black);
            }
            else
            {
                setBackground(Color.green);
                setForeground(Color.black);

                setToolTipText((getText().equals("MACHINE-SAFE")
                                ? MACHINE_SAFE_TOOLTIP
                                : (getText().equals("RACK-SAFE") ? RACK_SAFE_TOOLTIP : SITE_SAFE_TOOLTIP)));
            }

            return c;
        }
    }


    /**
     * Renderer for a publisher or receiver success rate.
     */
    @SuppressWarnings("serial")
    public static class SuccessRateRenderer extends DefaultTableCellRenderer
    {
        /**
        * {@inheritDoc}
        */
        @Override
        public Component getTableCellRendererComponent(JTable  table,
                                                       Object  value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int     row,
                                                       int     column)
        {
            Component c      = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            float     fValue = new Float(getText());

            if (fValue <= 0.900)
            {
                setBackground(Color.red);
                setForeground(Color.white);
                setToolTipText("Warning: The publisher success rate is less than 90%. "
                    + "this indicates consierable problems with this node communicating"
                    + " with other nodes. Further analysis of the network health detail should be carried out.");
            }
            else if (fValue <= 0.950)
            {
                setBackground(Color.orange);
                setForeground(Color.white);
                setToolTipText("Warning: The publisher success rate is less than 95%. "
                               + "Further analysis of the network health detail should be carried out.");
            }
            else
            {
                setBackground(Color.white);
                setForeground(Color.black);
            }

            if (c instanceof JLabel && value instanceof Number)
            {
                JLabel renderedLabel = (JLabel) super.getTableCellRendererComponent(table,
                                                                                    value,
                                                                                    isSelected,
                                                                                    hasFocus,
                                                                                    row,
                                                                                    column);

                renderedLabel.setHorizontalAlignment(JLabel.RIGHT);

                String text = MILLIS_FORMAT.format((Number) value);

                renderedLabel.setText(text);
            }

            return c;
        }
    }


    /**
     * Renderer for the thread utilization.
     */
    public static class ThreadUtilRenderer extends DefaultTableCellRenderer
    {
        /**
        * {@inheritDoc}
        */
        @Override
        public Component getTableCellRendererComponent(JTable  table,
                                                       Object  value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int     row,
                                                       int     column)
        {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!"".equals(getText()) && getText() != null)
            {
                float fValue = new Float(getText());

                if (fValue >= 0.90)
                {
                    setBackground(Color.red);
                    setForeground(Color.white);
                }
                else if (fValue >= 0.60)
                {
                    setBackground(Color.orange);
                    setForeground(Color.black);
                }
                else
                {
                    setBackground(Color.white);
                    setForeground(Color.black);
                }

                if (c instanceof JLabel && value instanceof Number)
                {
                    JLabel renderedLabel = (JLabel) super.getTableCellRendererComponent(table,
                                                                                        value,
                                                                                        isSelected,
                                                                                        hasFocus,
                                                                                        row,
                                                                                        column);

                    renderedLabel.setHorizontalAlignment(JLabel.RIGHT);

                    String text = PERCENT_FORMAT.format((Number) value);

                    renderedLabel.setText(text);
                }
            }
            else
            {
                setBackground(Color.white);
                setForeground(Color.black);
            }

            return c;
        }
    }
}
