package uk.ac.starlink.util.gui;

import java.awt.Font;

import javax.swing.table.AbstractTableModel;

/**
 * SelectCharactersModel is an implementation of the TableModel
 * interface for displaying all the characters in a given font.
 *
 * @since $Date$
 * @since 03-NOV-2000
 * @version $Id$
 * @author Peter W. Draper
 *
 */
public class SelectCharactersModel 
    extends AbstractTableModel 
{
    /**
     * The font that we're displaying.
     */
    protected Font font = null;

    /**
     * The number of characters in the font.
     */
    protected int numChars = 0;

    /**
     * The number of characters displayed in a row.
     */
    protected final int numColumns = 16;
    
    /**
     * Number of rows needed to display the whole font.
     */
    protected int numRows = 0;

    /**
     * Create an instance of this class.
     */
    @SuppressWarnings("this-escape")
    public SelectCharactersModel( Font font ) 
    {
        setFont( font );
    }

    /**
     * Set the displayed font.
     */
    public void setFont( Font font ) 
    {
        this.font = font;

        //  Count the displayable characters.
        numChars  = font.getNumGlyphs();

        //  Set the number of rows needed.
        numRows = (int) Math.ceil( (double)numChars / (double)numColumns);
    }

//
//  Implement rest of required interface (listeners are free from
//  AbstractTableModel)
//
    /**
     *  Returns the number of records managed by the data source
     *  object.
     */
    public int getRowCount() 
    {
        return numRows;
    }

    /**
     *  Returns the number of columns. Always two, the plot name and
     *  whether it is displaying the "current" spectrum.
     */
    public int getColumnCount() 
    {
        return numColumns;
    }

    /**
     *  Return the value of a given cell.
     */
    public Object getValueAt( int row, int column ) 
    {
        int index = numColumns * row + column;
        char[] c = new char[1];
        c[0] = (char) index;
        if ( ! font.canDisplay( c[0] ) ) {
            return "";
        }
        return new String( c );
    }
    
    /**
     *  Return the column names. There are none.
     */
    public String getColumnName( int index ) 
    {
        return "";
    }

    /**
     *  Return the column classes. All Strings.
     */
    public Class<?> getColumnClass( int index ) 
    {
        return "".getClass();
    }

    /**
     *  Nothing is editable.
     */
    public boolean isCellEditable( int row, int column ) 
    {
        return false;
    }

    /**
     *  Since nothing can be changed. This also does nothing.
     */
    public void setValueAt( Object value, int row, int column ) 
    {
        //  Do nothing
    }

}
