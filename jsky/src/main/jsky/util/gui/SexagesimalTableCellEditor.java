/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: SexagesimalTableCellEditor.java,v 1.4 2002/07/09 13:30:38 brighton Exp $
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
import jsky.coords.DMS;


/**
 * Used to reformat RA,DEC coordinates in a JTable in sexagesimal notation
 * for display.
 */
public class SexagesimalTableCellEditor extends DefaultCellEditor {

    private boolean hoursFlag;

    /** Constructor.
     *
     * @param hoursFlag if true, divide the cell value by 15 and display hours : min : sec,
     *                  otherwise display deg : min : sec.
     */
    public SexagesimalTableCellEditor(boolean hoursFlag) {
        super(new JTextField());
        this.hoursFlag = hoursFlag;
    }


    /**
     * This method is sent to the editor by the drawing table to
     * configure the editor appropriately before drawing.  Return
     * the Component used for drawing.
     *
     * @param	table		the JTable that is asking the editor to draw.
     *				This parameter can be null.
     * @param	value		the value of the cell to be rendered.  It is
     *				up to the specific editor to interpret
     *				and draw the value.  eg. if value is the
     *				String "true", it could be rendered as a
     *				string or it could be rendered as a check
     *				box that is checked.  null is a valid value.
     * @param	isSelected	true is the cell is to be editor with
     *				selection highlighting
     * @param	row	        the row index of the cell being drawn.  When
     *				drawing the header the rowIndex is -1.
     * @param	column	        the column index of the cell being drawn
     */
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected,
                                                 int row, int column) {
        Component component = super.getTableCellEditorComponent(table, value, isSelected, row, column);
        double val = Double.NaN;
        if (value != null) {
            if (value instanceof Float)
                val = ((Float) value).doubleValue();
            else if (value instanceof Double)
                val = ((Double) value).doubleValue();
        }
        if (!Double.isNaN(val)) {
            if (hoursFlag) {
                ((JTextField) component).setText(new HMS(val / 15.).toString());
            }
            else {
                ((JTextField) component).setText(new DMS(val).toString());
            }
        }

        return component;
    }


    /** Returns the value contained in the editor */
    public Object getCellEditorValue() {
        Object o = super.getCellEditorValue();
        if (o instanceof String) {
            if (hoursFlag) {
                try {
                    HMS hms = new HMS((String) o);
                    return new Double(hms.getVal() * 15.);
                }
                catch (Exception e) {
                    DialogUtil.error("Invalid value: '" + o + "', expected decimal degrees or h:m:s");
                }
            }
            else {
                try {
                    DMS dms = new DMS((String) o);
                    return new Double(dms.getVal());
                }
                catch (Exception e) {
                    DialogUtil.error("Invalid value: '" + o + "', expected decimal degrees or d:m:s");
                }
            }
        }
        return o;
    }
}
