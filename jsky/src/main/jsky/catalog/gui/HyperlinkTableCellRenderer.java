/*
 * ESO Archive
 *
 * $Id: HyperlinkTableCellRenderer.java,v 1.5 2002/07/09 13:30:36 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/07/23  Created
 */

package jsky.catalog.gui;

import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import jsky.catalog.*;
import jsky.util.*;


/**
 * Used to display a button in a table cell.
 *
 * @see HyperlinkTableCellEditor
 */
public class HyperlinkTableCellRenderer extends JButton implements TableCellRenderer {

    /** The table field corresponding to this cell */
    protected FieldDesc field;

    /** The value in the cell */
    protected Object value;

    /** Object representing the table data */
    protected TableQueryResult tableQueryResult;

    /**
     * Create a JTable cell renderer based on a JButton.
     *
     * @param field object representing a field (a table column description) in the table query result.
     * @param tableQueryResult contains the table data
     * @param sp optional panel used to display status information
     */
    public HyperlinkTableCellRenderer(FieldDesc field, TableQueryResult tableQueryResult) {
        this.field = field;
        this.tableQueryResult = tableQueryResult;
        setHorizontalAlignment(LEFT);
    }


    /**
     * This method is sent to the renderer by the drawing table to
     * configure the renderer appropriately before drawing.  Return
     * the Component used for drawing.
     * <p>
     * In this case, the returned component is a JButton, which is
     * set to get the contents of the URL and display it, based on the content type.
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
        this.value = value;
        setBackground(table.getBackground());

        try {
            field.getLinkValue(tableQueryResult, value, row); // just a test
            setText(field.getLinkText(tableQueryResult, value, row, column));
        }
        catch (Exception e) {
            setText("");
        }

        return this;
    }

    // The default table renderers define validate, revalidate, repaint,
    // and  firePropertyChange to be no-ops
    public void validate() {
    }

    public void revalidate() {
    }

    public void repaint(Rectangle r) {
    }

    public void repaint(long tm, int x, int y, int width, int height) {
    }

    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
    }

    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
    }
}
