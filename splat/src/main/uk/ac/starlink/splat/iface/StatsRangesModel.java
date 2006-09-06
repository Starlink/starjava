/*
 * Copyright (C) 2005 Central Laboratory of the Research Councils
 *
 *  History:
 *    20-JUN-2005 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.iface;

import java.util.Iterator;
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

    /** Are we showing the Flux value too */
    private boolean showFlux = false;

    /** Names of the statistics columns */
    private static final String[] names = {
        "Mean", "Std dev", "Min", "Max", "Flux"
    };

    /**
     * Create an instance of this class.
     * 
     * @param control a {@link PlotControl} instance displaying the current
     *                spectrum.
     * @param showFlux whether to include a integrated flux estimate in stats.
     */
    public StatsRangesModel( PlotControl control, boolean showFlux )
    {
        super( control.getPlot() );
        setShowFlux( showFlux );
        setPlotControl( control );
    }

    /**
     * Set whether we're showing an integrated flux, or not.
     */
    protected void setShowFlux( boolean showFlux )
    {
        if ( this.showFlux != showFlux ) {
            this.showFlux = showFlux;
            
            //  Get an update of all the Fluxes, if needed.
            if ( showFlux ) {
                Iterator i = rangeIterator();
                while ( i.hasNext() ) {
                    ((StatsRange) i.next()).updateStats();
                }
            }

            fireTableStructureChanged();
        }
        this.showFlux = showFlux;
    }

    /**
     * Get whether we're showing an integrated flux, or not.
     */
    protected boolean getShowFlux()
    {
        return showFlux;
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
     * Return the number of columns. This has extra four or five columns,
     * mean, std dev, min and max, maybe flux..
     */
    public int getColumnCount()
    {
        if ( showFlux ) {
            return super.getColumnCount() + 5;
        }
        else {
            return super.getColumnCount() + 4;
        }
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
            else if ( showFlux && ( column == base + 4 ) ) {
                value = statsRange.getFlux();
            }
        }
        return new AstDouble( value, plot.getMapping(), 2 );
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
