/*
 * Copyright (C) 2001-2004 Central Laboratory of the Research Councils
 *
 *  History:
 *    07-JAN-2001 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.iface;

import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.table.AbstractTableModel;

import uk.ac.starlink.ast.gui.AstDouble;
import uk.ac.starlink.diva.XRangeFigure;
import uk.ac.starlink.splat.plot.DivaPlot;

/**
 * XGraphicsRangesModel extends AbstractTableModel to provide a description of
 * the data shown in a JTable that relates to a series of ranges of X position
 * (say a series of spectral ranges that describe a regions to be used for
 * fitting/determining the local background). The model has three columns that
 * contain the range identifier, followed by a lower and upper coordinate
 * limit. The numbers shown for the X coordinates are formatted to match the X
 * axis of the spectrum plot using the AstDouble class.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see XGraphicsRangesView
 * @see XGraphicsRange
 * @see DivaPlot
 * @see AstDouble
 */
public class XGraphicsRangesModel 
    extends AbstractTableModel
{
    /**
     * The plot that displays the figures we're describing. Used to give
     * better control over the formatting.
     */
    protected DivaPlot plot = null;

    /**
     * Create an instance of this class.
     */
    public XGraphicsRangesModel( DivaPlot plot )
    {
        this.plot = plot;
    }

    /**
     * List of the range objects that we're looking at.
     */
    protected ArrayList rangeObjects = new ArrayList();

    //
    //  Implement rest of ListModel interface (listeners are free from
    //  AbstractListModel)
    //
    /**
     * Return the row count (i.e.<!-- --> number of ranges).
     */
    public int getRowCount()
    {
        return rangeObjects.size();
    }

    /**
     * Return the number of columns.
     */
    public int getColumnCount()
    {
        return 3;
    }

    /**
     * Return the value of an element.
     */
    public Object getValueAt( int row, int column )
    {
        if ( column == 0 ) {
            return new Integer( row );
        }
        XGraphicsRange xRange = (XGraphicsRange) rangeObjects.get( row );
        double[] range = xRange.getRange();
        return new AstDouble( range[column - 1], plot.getMapping(), 1 );
    }

    /**
     * Set the value of an element (when edited by hand).
     *
     * @param oValue the new value.
     * @param row the new value row.
     * @param column the new value column.
     */
    public void setValueAt( Object oValue, int row, int column )
    {
        if ( column == 0 ) {
            return;
        }
        try {
            double value = 0.0;

            //  oValue can be String or a Number.
            if ( oValue instanceof String ) {
                value = AstDouble.parseDouble( (String) oValue,
                                               plot.getMapping(), 1 );
            }
            else if ( oValue instanceof Number ) {
                value = ( (Number) oValue ).doubleValue();
            }

            XGraphicsRange xRange = (XGraphicsRange) rangeObjects.get( row );
            double[] range = xRange.getRange();
            if ( column == 1 ) {
                range[0] = value;
            }
            else {
                range[1] = value;
            }
            xRange.setRange( range );

        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    /**
     * Return if a cell is editable.
     */
    public boolean isCellEditable( int row, int column )
    {
        if ( column == 0 ) {
            return false;
        }
        return true;
    }

    /**
     * Return the column names.
     */
    public String getColumnName( int column )
    {
        if ( column == 0 ) {
            return "ID";
        }
        else if ( column == 1 ) {
            return "Lower bound";
        }
        return "Upper bound";
    }

    /**
     * Return the column classes.
     */
    public Class getColumnClass( int index )
    {
        if ( index == 0 ) {
            return Integer.class;
        }
        return AstDouble.class;
    }

    //
    //  Bespoke interface. Allow the addition, removal and query of a
    //  new XGraphicsRange objects.
    //
    /**
     * React to a new range being added.
     */
    public void addRange( XGraphicsRange xRange )
    {
        rangeObjects.add( xRange );
        fireTableRowsInserted( getRowCount() - 1, getRowCount() - 1 );
    }

    /**
     * Get an Iterator over all the XGraphicsRange instances known to this
     * model instance.
     */
    public Iterator rangeIterator()
    {
        return rangeObjects.iterator();
    }

    /**
     * Lookup which XGraphicsRange contains an XRangeFigure...
     */
    public int findFigure( XRangeFigure figure )
    {
        XGraphicsRange XRange;
        for ( int i = 0, j = 0; i < getRowCount(); i++, j += 2 ) {
            XRange = (XGraphicsRange) rangeObjects.get( i );
            if ( XRange.isFigure( figure ) ) {
                return i;
            }
        }
        return -1;
    }

    /**
     * React to a range being removed
     *
     * @param index list index of the range to remove.
     */
    public void removeRange( int index )
    {
        try {
            XGraphicsRange xRange = (XGraphicsRange) rangeObjects.get( index );
            xRange.delete();
            rangeObjects.remove( index );
            fireTableRowsDeleted( index, index );
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    /**
     * React to a range being changed, just need to update its description.
     *
     * @param index list index of the range to change.
     */
    public void changeRange( int index )
    {
        fireTableDataChanged();
    }

    /**
     * Return a list of the ranges. Each range is a pair of consecutive values
     * in the returned array.
     */
    public double[] getRanges()
    {
        double[] allRanges = new double[getRowCount() * 2];
        double[] thisRange;
        XGraphicsRange XRange;
        for ( int i = 0, j = 0; i < getRowCount(); i++, j += 2 ) {
            XRange = (XGraphicsRange) rangeObjects.get( i );
            thisRange = XRange.getRange();
            allRanges[j] = thisRange[0];
            allRanges[j + 1] = thisRange[1];
        }
        return allRanges;
    }
}
