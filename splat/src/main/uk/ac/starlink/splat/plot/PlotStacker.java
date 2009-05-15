/*
 * Copyright (C) 2008-2009 Science and Technology Facilities Council
 *
 *  History:
 *    17-DEC-2008 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.plot;

import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataComp;
import uk.ac.starlink.splat.util.Sort;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.TableCalc;


/**
 * Sets the offsets used when displaying spectra in a {@link DivaPlot}.
 * <p>
 * There are two supported methods for deriving an offset, using a constant
 * offset applied to each spectrum using a ordering expression and just
 * deriving an offset for each spectrum from an expression. The offset
 * in each case must be in physical coordinates.
 * <p>
 * The order is defined by a value derived from the properties (using a JEL
 * expression) in the SpecData instances associated with a plot. When
 * reorder is called the yoffset of each associated SpecData is updated to be
 * a multiple of the shift.
 * <p>
 * The offset from an expression technique works similarily, but each spectrum
 * is assigned an offset from the evaluation of the expression.
 *
 * @author Peter W. Draper
 * @version $Id:$
 */
public class PlotStacker
{
    private DivaPlot plot = null;
    private String expression = null;
    private double shift = 0.1;
    private boolean ordering = true;
    private double dmin = Double.MAX_VALUE;
    private double dmax = Double.MIN_VALUE;

    /**
     * Constructor. Define the {@link DivaPlot} we're associated with.
     *
     * @param plot the DivaPlot
     * @param expression a JEL expression that references the FITS headers in
     *                   the SpecData instances displayed in the Plot. The
     *                   relative value of this defines the stacking order,
     *                   or the value the offset, depending on the value
     *                   of order.
     * @param ordering   whether JEL expression defines the ordering or the
     *                   actual offset. If ordering then an offset value is
     *                   required.
     * @param shift      the offset used when ordering
     */
    public PlotStacker( DivaPlot plot, String expression, boolean ordering,
                        double shift )
    {
        setPlot( plot );
        setExpression( expression );
        setShift( shift );
        setOrdering( ordering );
    }

    /**
     * Set the DivaPlot we're associated with.
     */
    public void setPlot( DivaPlot plot )
    {
        this.plot = plot;
        resetMinMax();
    }

    /**
     * Get the DivaPlot we're associated with.
     */
    public DivaPlot getPlot()
    {
        return plot;
    }

    /**
     * Set the JEL expression.
     */
    public void setExpression( String expression )
    {
        this.expression = expression;
        resetMinMax();
    }

    /**
     * Get the JEL expression.
     */
    public String getExpression()
    {
        return expression;
    }

    /**
     * Set the shift each spectrum is offset up the Y axis of the plot
     * is offset with respect to the previous spectrum. This is a fraction
     * of the display surface.
     */
    public void setShift( double shift )
    {
        this.shift = shift;
        resetMinMax();
    }

    /**
     * Get the shift each spectrum is offset up the Y axis of the plot
     * is offset with respect to the previous spectrum. This is a fraction
     * of the display surface.
     */
    public double getShift()
    {
       return shift;
    }

    /**
     * Set if ordering the spectra and then offsetting by a fixed amount.
     * Otherwise the expression defines the offset.
     */
    public void setOrdering( boolean ordering )
    {
        this.ordering = ordering;
        resetMinMax();
    }

    /**
     * Get if ordering.
     */
    public boolean getOrdering()
    {
       return ordering;
    }

    /**
     * Get the low limit that applying these offsets will produce.
     */
    public double getMinLimit()
        throws SplatException
    {
        if ( dmin == Double.MAX_VALUE ) {
            updateOffsets();
        }
        return dmin;
    }

    /**
     * Get the upper limit that applying these offsets will produce.
     */
    public double getMaxLimit()
        throws SplatException
    {
        if ( dmax == Double.MIN_VALUE ) {
            updateOffsets();
        }
        return dmax;
    }

    /**
     *  Reset min and max limits to not set.
     */
    private void resetMinMax()
    {
        dmin = Double.MAX_VALUE;
        dmax = Double.MIN_VALUE;
    }

    /**
     * Do an update of the offsets used by the spectra in a DivaPlot.
     *
     * This should be called sometime after the spectra displayed the
     * associated Plot are changed, but before the plot is redrawn.
     *
     * @throws a SplatException will be thrown if an error evaluating the
     *         expression occurs.
     */
    public void updateOffsets()
        throws SplatException
    {
        // Use the SpecDataComp to iterate over the spectra.
        SpecDataComp spectra = plot.getSpecDataComp();
        int nspectra = spectra.count();

        SpecData specData;
        if ( expression != null && ! "".equals( expression ) ) {
            if ( ordering ) {
                double[] values = new double[nspectra];
                int[] indices = new int[nspectra];

                //  Have an expression and ordering, so evaluate and sort.
                for ( int i = 0; i < nspectra; i++ ) {
                    specData = spectra.get( i );
                    indices[i] = i;
                    values[i] = TableCalc.calc( specData, expression );
                }
                Sort.insertionSort2( values, indices );

                //  Now update the offsets in the SpecData themselves.
                double fraction = 0.0;
                for ( int i = 0; i < nspectra; i++ ) {
                    specData = spectra.get( indices[i] );
                    specData.setYOffset( fraction );
                    fraction += shift;
                }

                //  Min and max for ranging.
                dmin = 0.0;
                dmax = fraction;
            }
            else {

                //  Offsets derived directly from the expression.
                double value;
                resetMinMax();
                for ( int i = 0; i < nspectra; i++ ) {
                    specData = spectra.get( i );
                    value  = TableCalc.calc( specData, expression );
                    specData.setYOffset( value );
                    dmax = Math.max( value, dmax );
                    dmin = Math.min( value, dmin );
                }
            }
        }
        else {
            //  No expression, so no offsets, unless ordering
            //  is established in that case we use the natural order.
            if ( ordering ) {
                double fraction = 0.0;
                for ( int i = 0; i < nspectra; i++ ) {
                    specData = spectra.get( i );
                    specData.setYOffset( fraction );
                    fraction += shift;
                }
                dmin = 0.0;
                dmax = fraction;
            }
            else {
                for ( int i = 0; i < nspectra; i++ ) {
                    specData = spectra.get( i );
                    specData.setYOffset( 0.0 );
                }
                dmin = 0.0;
                dmax = 0.0;
            }
        }
    }
}
