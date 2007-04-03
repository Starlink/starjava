/*
 * Copyright (C) 2005 Central Laboratory of the Research Councils
 * Copyright (C) 2007 Particle Physics and Astronomy Research Council
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
import uk.ac.starlink.splat.util.JACUtilities;
import uk.ac.starlink.splat.util.NumericIntegrator;
import uk.ac.starlink.splat.util.Sort;
import uk.ac.starlink.splat.util.Statistics;

/**
 * StatsRange extends the {@link XGraphicsRange} class to add four or five new
 * rows that contain statistics values for the spectrum being drawn over (the
 * current spectrum of a {@link PlotControl}), under the supervision of 
 * a {@link StatsFrame} instance.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class StatsRange
    extends XGraphicsRange
{
    /** The StatsFrame */
    private StatsFrame statsFrame = null;

    /** The PlotControl instance. */
    private PlotControl control = null;

    /** The Statistics instance. */
    private Statistics stats = new Statistics( new double[] {0.0} );

    /** The NumericIntegrator instance */
    private NumericIntegrator integ = new NumericIntegrator();

    /** The TSYS value */
    private double tsys = -1.0;

    /**
     * Create a range interactively or non-interactively.
     *
     * @param statsRange StatsFrame with a PlotControl that is to display the
     *                   range. 
     * @param model StatsRangesModel model that arranges to have the
     *              properties of the range displayed (may be null).
     * @param colour the colour of any figures.
     * @param constrain whether figures are fixed to move only in X and have
     *                  initial size of the full Y dimension of plot.
     * @param range a pair of doubles containing the range (in physical
     *              coordinates) to be used. Set null if the figure is to be
     *              created interactively.
     */
    public StatsRange( StatsFrame statsFrame, StatsRangesModel model,
                       Color colour, boolean constrain, double[] range )
    {
        super( statsFrame.getPlotControl().getPlot(), model, colour, 
               constrain );
        this.statsFrame = statsFrame;
        setControl( statsFrame.getPlotControl() );

        //  Do this after construction to avoid problems with initialization
        //  order.
        if ( range == null ) {
            startInteraction();
        }
        else {
            createFromRange( range );
        }
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
            double[] coords = currentSpectrum.getXData();
            int low = Sort.lookup( coords, range[0] );
            int high = Sort.lookup( coords, range[1] );
            if ( low > high ) {
                //  Swap when coordinates run right to left.
                int temp = low;
                low = high;
                high = temp;
            }
            int nvals = high - low + 1;

            if ( nvals > 0 ) {
                double[] data = currentSpectrum.getYData();

                //  Test for presence of BAD values in the data. These are not
                //  allowed in the final data.
                int n = 0;
                int t = 0;
                for ( int i = low; i <= high; i++ ) {
                    if ( data[i] != SpecData.BAD ) n++;
                }

                //  What do we need to determine?
                boolean showFlux = ((StatsRangesModel)model).getShowFlux();
                boolean showTSYS = ((StatsRangesModel)model).getShowTSYS();

                //  Flux needs a monotonic spectrum.
                boolean monotonic = currentSpectrum.isMonotonic();
                showFlux = ( showFlux && monotonic );

                //  Do the necessary extractions.
                double[] rangeData = new double[n];
                if ( showFlux ) {
                    double[] rangeCoords = new double[n];
                    if ( n > 1 ) {
                        n = 0;
                        for ( int i = low; i <= high; i++ ) {
                            if ( data[i] != SpecData.BAD ) {
                                rangeData[n] = data[i];
                                rangeCoords[n] = coords[i];
                                n++;
                            }
                        }

                        //  Set up for flux estimates.
                        integ.setData( rangeCoords, rangeData );
                    }
                    else {
                        // No flux for one point.
                        rangeData = new double[2];
                        rangeCoords = new double[2];
                        rangeData[0] = 0.0;
                        rangeData[1] = 0.0;
                        rangeCoords[0] = 0.0;
                        rangeCoords[1] = 1.0;
                        integ.setData( rangeCoords, rangeData );
                    }
                }
                else {
                    n = 0;
                    for ( int i = low; i <= high; i++ ) {
                        if ( data[i] != SpecData.BAD ) {
                            rangeData[n] = data[i];
                            n++;
                        }
                    }
                }

                //  Perform stats...
                stats.setData( rangeData );

                //  TSYS requires the standard deviation and factors held
                //  by the StatsFrame.
                tsys = -1.0;
                if ( showTSYS ) {
                    double std = getStandardDeviation();
                    double[] factors = statsFrame.getTSYSFactors();
                    if ( factors != null ) {
                        tsys = JACUtilities.calculateTSYS( factors[0],
                                                           factors[1],
                                                           factors[2],
                                                           std );
                    }
                }

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

    /**
     * Get the flux estimate of the spectrum in this range.
     */
    public double getFlux()
    {
        return integ.getIntegral();
    }

    /**
     * Get the TSYS value.
     */
    public double getTSYS()
    {
        return tsys;
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
