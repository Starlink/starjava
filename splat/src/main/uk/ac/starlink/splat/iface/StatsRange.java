/*
 * Copyright (C) 2005 Central Laboratory of the Research Councils
 *
 *  History:
 *     20-JUN-2005 (Peter W. Draper):
 *        Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.Color;
import uk.ac.starlink.diva.FigureChangedEvent;
import uk.ac.starlink.diva.XRangeFigure;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.plot.DivaPlot;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.Statistics;

/**
 * StatsRange extends the {@link XGraphicsRange} class to add four new rows
 * that contain statistics values for the spectrum being drawn over (the
 * current spectrum of a {@link PlotControl}).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class StatsRange
    extends XGraphicsRange
{
    /** The PlotControl instance. */
    protected PlotControl control = null;

    /** The Statistics instance. */
    protected Statistics stats = new Statistics( new double[] {0.0} );

    /**
     * Create a range interactively.
     *
     * @param control PlotControl that is to display the range.
     * @param model StatsRangesModel model that arranges to have the
     *              properties of the range displayed (may be null).
     * @param colour the colour of any figures.
     * @param constrain whether figures are fixed to move only in X and have
     *                  initial size of the full Y dimension of plot.
     */
    public StatsRange( PlotControl control, StatsRangesModel model,
                       Color colour, boolean constrain )
    {
        super( control.getPlot(), model, colour, constrain );
        setControl( control );
    }

    /**
     * Create a range non-interactively.
     *
     * @param control PlotControl that is to display the range.
     * @param model StatsRangesModel model that arranges to have the
     *              properties of the range displayed (may be null).
     * @param colour the colour of any figures.
     * @param constrain whether figures are contrained to move only in X and
     *                  have initial size of the full Y dimension.
     * @param range a pair of doubles containing the range (on physical
     *      coordinates) to be used.
     */
    public StatsRange( PlotControl control, StatsRangesModel model,
                       Color colour, boolean constrain, double[] range )
    {
        super( control.getPlot(), model, colour, constrain, range );
        setControl( control );
    }

    /**
     * Set the PlotControl instance to use.
     *
     * @param control The PlotControl instance.
     */
    protected void setControl( PlotControl control )
    {
        this.control = control;
        DivaPlot newPlot = control.getPlot();
        if ( newPlot != plot ) {
            setPlot( newPlot );
        }
    }

    /**
     * Make the current statistics match the current range for the current
     * spectrum shown in the assocated plot.
     */
    public void updateStats()
    {
        SpecData currentSpectrum = control.getCurrentSpectrum();
        if ( currentSpectrum != null ) {

            //  Extract the data from the spectrum.
            double[] range = getRange();
            int[] lower = currentSpectrum.bound( range[0] );
            int[] higher = currentSpectrum.bound( range[1] );
            if ( lower[1] > higher[0] ) {
                //  Swap when coordinates run right to left.
                int[] temp = lower;
                lower = higher;
                higher = temp;
            }
            int nvals = higher[0] - lower[1] + 1;

            if ( nvals > 0 ) {
                double[] data = currentSpectrum.getYData();
                int low = lower[1];
                int high = higher[0];

                //  Test for presence of BAD values in the data. These are not
                //  allowed in the final data.
                int n = 0;
                int t = 0;
                for ( int i = low; i <= high; i++ ) {
                    if ( data[i] != SpecData.BAD ) n++;
                }

                //  Now allocate the necessary memory and copy in the data.
                double[] rangeData = new double[n];
                n = 0;
                for ( int i = low; i <= high; i++ ) {
                    if ( data[i] != SpecData.BAD ) {
                        rangeData[n] = data[i];
                        n++;
                    }
                }

                //  Perform stats...
                stats.setData( rangeData );
            }
        }
    }

    /**
     * Get mean value of the spectrum in this range.
     */
    public double getMean()
    {
        return stats.getMean();
    }

    /**
     * Get standard deviation of the spectrum in this range.
     */
    public double getStandardDeviation()
    {
        return stats.getStandardDeviation();
    }

    /**
     * Get minimum value of spectrum in this range.
     */
    public double getMin()
    {
        return stats.getMinimum();
    }

    /**
     * Get maximum value of spectrum in this range.
     */
    public double getMax()
    {
        return stats.getMaximum();
    }

    //
    //  FigureListener interface.
    //

    /**
     * Sent when the figure is created.
     */
    public void figureCreated( FigureChangedEvent e )
    {
        if ( e.getSource() instanceof XRangeFigure ) {
            super.figureCreated( e );
            updateStats();
        }
    }

    /**
     * Sent when the figure is changed (i.e.&nbsp;moved or transformed).
     */
    public void figureChanged( FigureChangedEvent e )
    {
        updateStats();
        super.figureChanged( e );
    }
}
