/*
 * ESO Archive
 *
 * $Id: SexagesimalTableCellRenderer.java,v 1.3 2002/07/09 13:30:38 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/07/23  Created
 */

package jsky.util.gui;

import java.util.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.table.*;
import javax.swing.*;
import javax.swing.event.*;

import jsky.coords.HMS;


/**
 * Used to reformat RA,DEC coordinates in a JTable in sexagesimal notation
 * for display.
 */
public class SexagesimalTableCellRenderer extends DefaultTableCellRenderer {

    /** Divide by this value to convert to deg to hours, if requested */
    protected float f = 1.0F;


    /** Constructor.
     *
     * @param hoursFlag if true, divide the cell value by 15 and display hours : min : sec,
     *                  otherwise display deg : min : sec.
     */
    public SexagesimalTableCellRenderer(boolean hoursFlag) {
        if (hoursFlag)
            f = 15.0F;
    }


    /**
     * This method is sent to the renderer by the drawing table to
     * configure the renderer appropriately before drawing.  Return
     * the Component used for drawing.
     *
     * @param	table		the JTable that is asking the renderer to draw.
     *				This parameter can be null.
     * @param	value		the value of the cell to be rendered.  It is
     *				up to the specific renderer to interpret
     *				and draw the value.  eg. if value is the
     *				String "true", it could be rendered as a
     *				string or it could be rendered as a check
     *				box that is checked.  null is a valid value.
     * @param	isSelected	true is the cell is to be renderer with
     *				selection highlighting
     * @param	hasFocus	true if the cell has the focus
     * @param	row	        the row index of the cell being drawn.  When
     *				drawing the header the rowIndex is -1.
     * @param	column	        the column index of the cell being drawn
     */
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        double val = Double.NaN;
        if (value != null) {
            if (value instanceof Float)
                val = ((Float) value).doubleValue();
            else if (value instanceof Double)
                val = ((Double) value).doubleValue();
        }
        if (!Double.isNaN(val)) {
            HMS hms = new HMS(val / f);
            ((JLabel) component).setText(hms.toString());
        }

        return component;
    }
}

