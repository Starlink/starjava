/*
 * Copyright (C) 2005 Central Laboratory of the Research Councils
 *
 *  History:
 *    20-JUN-2005 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.iface;

import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.ast.gui.AstDouble;

/**
 * StatsRangesModel extends {@link XGraphicsRangesModel} to include additional
 * columns that show basic statistics of an associated spectrum's data values
 * that lie with each of the ranges.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class StatsRangesModel 
    extends XGraphicsRangesModel
{
    /** Associated PlotControl instance. */
    private PlotControl control = null;

    /** Names of the statistics columns */
    private static final String[] names = {
        "Mean", "Std dev", "Min", "Max"
    };

    /**
     * Create an instance of this class.
     */
    public StatsRangesModel( PlotControl control )
    {
        super( control.getPlot() );
        setPlotControl( control );
    }

    /**
     * Set the PlotControl instance. This displays a current spectrum for
     * which the statistics are required.
     */
    protected void setPlotControl( PlotControl control )
    {
        this.control = control;
    }

    /**
     * Return the number of columns. This has extra four columns, mean, std
     * dev, min and max.
     */
    public int getColumnCount()
    {
        return super.getColumnCount() + 4;
    }

    /**
     * Return the value of an element.
     */
    public Object getValueAt( int row, int column )
    {
        if ( column < super.getColumnCount() ) {
            return super.getValueAt( row, column );
        }

        StatsRange statsRange = (StatsRange) rangeObjects.get( row );
        double value = 0.0;
        if ( statsRange != null ) {
            int base = super.getColumnCount();
            if ( column == base ) {
                value = statsRange.getMean();
            }
            else if ( column == base + 1 ) {
                value = statsRange.getStandardDeviation();
            }
            else if ( column == base + 2 ) {
                value = statsRange.getMin();
            }
            else if ( column == base + 3 ) {
                value = statsRange.getMax();
            }
        }
        return new AstDouble( value, plot.getMapping(), 2 );
        //return new Double( value );
    }

    /**
     * Return if a cell is editable.
     */
    public boolean isCellEditable( int row, int column )
    {
        if ( column < super.getColumnCount() ) {
            return super.isCellEditable( row, column );
        }
        return false;
    }

    /**
     * Return the column names.
     */
    public String getColumnName( int column )
    {
        if ( column < super.getColumnCount() ) {
            return super.getColumnName( column );
        }
        return names[ column - super.getColumnCount() ];
    }

    /**
     * Return the column classes.
     */
    public Class getColumnClass( int column )
    {
        if ( column < super.getColumnCount() ) {
            return super.getColumnClass( column );
        }
        return AstDouble.class;
    }
}
