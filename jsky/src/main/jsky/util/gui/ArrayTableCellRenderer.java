/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: ArrayTableCellRenderer.java,v 1.2 2002/07/09 13:30:38 brighton Exp $
 */

package jsky.util.gui;

import java.awt.Component;
import java.lang.reflect.Array;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Used to display the contents of arrays in a table cell.
 */
public class ArrayTableCellRenderer extends DefaultTableCellRenderer {

    /**
     * Constructor: set the alignment based on the array element type.
     *
     * @param arrayClass the type of the array
     */
    public ArrayTableCellRenderer(Class arrayClass) {
        Class c = arrayClass.getComponentType();
        if (c != null && (c == int.class
                || c == double.class
                || c == float.class
                || c == Integer.class
                || c == Double.class
                || c == Float.class))
            setHorizontalAlignment(RIGHT);
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
        if (value != null) {
            Class c = value.getClass();
            if (c.isArray()) {
                String s = null;
                int n = Array.getLength(value);
                if (n >= 1) {
                    s = Array.get(value, 0).toString();
                    if (n > 1)
                        s += ", ...";
                }
                else {
                    s = "";
                }
                ((JLabel) component).setText(s);
            }
        }

        return component;
    }
}


