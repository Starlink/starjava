/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     22-JUN-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * JTable cell renderer for integer and floating point numbers in
 * cells. Attempts to get a better result than the default number
 * renderer that has far too much precision and far too little and
 * forgets to use exponents for large numbers, it also can never match
 * the coordinates in use for any "wavelength" axis (not true now have
 * AstDouble class, but still true for plain numbers).
 * <p>
 * So this class can use a request to an appropriate axis of a Plot to
 * format the result, or the toString()" method, which is better matched
 * for many floating values (the DecimalNumber class could probably also
 * be used for more complex schemes).
 * 
 * @author Peter W. Draper
 * @version $Id$
 */
public class NumberCellRenderer 
    extends DefaultTableCellRenderer
{
    /**
     * Returns a renderer for numbers in a JTable.
     *
     * @param table the JTable
     * @param value the value to assign to the cell at [row, column].
     * @param isSelected true if cell is selected
     * @param hasFocus true if cell has focus
     * @param row  the row of the cell to render
     * @param column the column of the cell to render
     * @return the table cell renderer
     */
    public Component getTableCellRendererComponent( JTable table,
                                                    Object value,
                                                    boolean isSelected,
                                                    boolean hasFocus,
                                                    int row, 
                                                    int column) 
    { 
        super.getTableCellRendererComponent( table, value, isSelected,
                                             hasFocus, row, column ); 
        try {
            setText( value.toString() );
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }
}
