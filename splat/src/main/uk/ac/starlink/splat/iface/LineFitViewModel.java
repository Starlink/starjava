/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     22-JUN-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

/**
 * LineFitViewModel extends AbstractTableModel to provide a
 * description of the data shown in a JTable that relates to a series
 * spectral line fits.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see LineFitView
 * @see MultiTableView
 */
public class LineFitViewModel 
    extends AbstractTableModel
{
    /**
     * Array of objects that define the line properties.
     */
    private ArrayList lineList = new ArrayList();

    /**
     * What kind of data we're modelling.  Can be LineProperties QUICK,
     * GAUSS, LORENTZ or VOIGT,
     */
    private int type = LineProperties.QUICK;

    /**
     *  Create an instance of this class.
     * 
     *  @param type the type of line data that we're modelling.
     */
    public LineFitViewModel( int type )
    {
        this.type = type;
    }

//
//  Implement rest of TableModel interface (listeners are free from
//  AbstractTableModel)
//
    /**
     *  Returns the number of records managed by the data source
     *  object (i.e.<!-- --> the number of lines).
     */
    public int getRowCount() 
    {
        return lineList.size();
    }

    /**
     *  Returns the number of columns.
     */
    public int getColumnCount() 
    {
        return LineProperties.count( type );
    }

    /**
     *  Return the value of a given cell.
     */
    public Object getValueAt( int row, int column ) 
    {
        return ((LineProperties)lineList.get(row)).getNumberField(column);
    }

    /**
     *  Return the column name.
     */
    public String getColumnName( int index ) 
    {
        return LineProperties.getName( type, index );
    }

    /**
     *  Return the column classes.
     */
    public Class getColumnClass( int index ) 
    {
        return LineProperties.getNumberClass( type, index );
    }

    /**
     *  Displayed field is editable. None are.
     */
    public boolean isCellEditable( int row, int column ) 
    {
        return false;
    }

    /**
     *  Must be able to change displayed status of spectrum.
     */
    public void setValueAt( Object value, int row, int column ) 
    {
        //  If any were editable then we could do this.
        LineProperties props = (LineProperties) lineList.get( row );
        props.setField( column, ((Number)value).doubleValue() );
    }

//
//  Bespoke interface. Allow the addition, removal and query of a
//  new lines.
//
    /**
     * Add a new line.
     *
     * @param props the new LineProperties object. This should have
     *              the appropriate type and is not checked.
     */
    public void addLine( LineProperties props )
    {
        lineList.add( props );
        fireTableRowsInserted( getRowCount() - 1, getRowCount() - 1 );
    }

    /**
     * Get a reference to a LineProperties object.
     *
     * @param index list index of the line to remove.
     */
    public LineProperties getLine( int index )
    {
        try {
            return (LineProperties) lineList.get( index );
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Remove a line.
     *
     * @param index list index of the line to remove.
     */
    public void removeLine( int index )
    {
        try {
            lineList.remove( index );
            fireTableRowsDeleted( index, index );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * See if we have a line with an identifier field that matches.
     *
     * @param values the list of values associated with the line. The
     *               first is the index field.
     * @return the match LineProperties object, null if not matched
     */
    public LineProperties matchID( double[] values )
    {
        int inID = (int) values[0];
        int checkID;
        LineProperties props = null;
        for ( int i = 0; i < lineList.size(); i++ ) {
            props = (LineProperties)lineList.get(i);
            checkID = (int) props.getField( 0 );
            if ( checkID == inID ) {
                return props;
            }
        }
        return null;
    }

    /**
     *  React to a change in a lines measurements, just need to update its
     *  description.
     *
     *  @param index list index of the range to remove.
     */
    public void changeRange( int index )
    {
        fireTableRowsUpdated( index, index );
    }

    /**
     * Set the results for a line. The type of results required is
     * determined by the type of this model (and cannot be changed).
     */
    public void setResults( int index, double[] values )
    {
        LineProperties props = (LineProperties) lineList.get( index );
        int limit = Math.min( props.count( type ), values.length );
        for ( int i = 0; i < limit; i++ ) {
            props.setField( i, values[i] );
        }
        changeRange( index );
    }
}
