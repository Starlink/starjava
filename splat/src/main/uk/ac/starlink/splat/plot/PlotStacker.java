/*
 * Copyright (C) 2008-2009 Science and Technology Facilities Council
 *
 *  History:
 *    17-DEC-2008 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.plot;

import uk.ac.starlink.splat.data.LineIDSpecData;
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
 * <p>
 * LineID spectra often do not show on a display at all, so an offset is
 * generated and not used. For this reason any LineID spectra are checked
 * to see if they are displayed and skipped if not.
 *
 * @author Peter W. Draper
 * @version $Id:$
 */
public class PlotStacker
{
    private DivaPlot plot = null;
    private String expression = null;
    private boolean ordering = true;
    private double dmax = Double.MIN_VALUE;
    private double dmin = Double.MAX_VALUE;
    private double shift = 0.1;

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
     * Check if the given spectrum is a line identifier has any positions that
     * are visible on the plot. Use to avoid giving an offset to spectra that
     * will not be displayed at all.
     */
    private boolean lineIDSpecVisible( LineIDSpecData specData )
    {
        //  LineIDSpecData can be drawn in two sidebands, so
        //  see how many positions it has actually drawn. This
        //  should be for the other sideband (horrible hack).
        if ( specData.getDrawn() > 0 ) {
            return true;
        }

        //  Check normal sideband, if applicable.
        double tmp[] = plot.getPhysicalLimits();
        double cbox[] = new double[4];
        cbox[0] = tmp[0];
        cbox[1] = tmp[2];
        cbox[2] = tmp[1];
        cbox[3] = tmp[2];

        //  These values are in the current spectrum. Transform to the
        //  spectrum we're checking.
        SpecDataComp spectra = plot.getSpecDataComp();
        double bbox[] = spectra.transformCoords( specData, cbox, false );
        if ( bbox == null ) {
            //  Get this when spectrum is current spectrum or we're
            //  not coordinate matching.
            bbox = cbox;
        }

        //  Nearest points to these limits in the spectrum.
        double p1[] = specData.nearest( bbox[0] );
        double p2[] = specData.nearest( bbox[1] );

        //  Sort limits.
        double xMin = Math.min( bbox[0], bbox[1] );
        double xMax = Math.max( bbox[0], bbox[1] );
        double pMin = Math.min( p1[0], p2[0] );
        double pMax = Math.max( p1[0], p2[0] );

        //  If points are outside limits, don't offset.
        if ( ( pMin < xMin || pMin > xMax ) &&
             ( pMax < xMin || pMax > xMax ) ) {
            return false;
        }
        return true;
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
                int nshift = 1;
                for ( int i = 0; i < nspectra; i++ ) {
                    specData = spectra.get( indices[i] );
                    if ( specData instanceof LineIDSpecData ) {

                        //  Line identifiers can be skipped and only
                        //  contribute 1 shift in total to the range.
                        if ( lineIDSpecVisible( (LineIDSpecData)specData ) ) {
                            specData.setYOffset( fraction );
                            fraction += shift;
                            nshift++;
                        }
                        else {
                            specData.setYOffset( 0.0 );
                        }
                    }
                    else {
                        specData.setYOffset( fraction );
                        fraction += shift;
                    }
                }

                //  Min and max for ranging.
                dmin = 0.0;
                if ( nshift > 1 ) nshift--;
                dmax = fraction - ((double)nshift *shift);
            }
            else {

                //  Offsets derived directly from the expression.
                double value;
                resetMinMax();
                for ( int i = 0; i < nspectra; i++ ) {
                    specData = spectra.get( i );
                    if ( specData instanceof LineIDSpecData ) {

                        //  Line identifiers can be skipped and don't
                        //  contribute to the range.
                        if ( lineIDSpecVisible( (LineIDSpecData)specData ) ) {
                            value = TableCalc.calc( specData, expression );
                            specData.setYOffset( value );
                        }
                        else {
                            specData.setYOffset( 0.0 );
                        }
                    }
                    else {
                        value = TableCalc.calc( specData, expression );
                        specData.setYOffset( value );
                        dmax = Math.max( value, dmax );
                        dmin = Math.min( value, dmin );
                    }
                }
            }
        }
        else {
            //  No expression, so no offsets, unless ordering
            //  is established in that case we use the natural order.
            if ( ordering ) {
                double fraction = 0.0;
                int nshift = 1;
                for ( int i = 0; i < nspectra; i++ ) {
                    specData = spectra.get( i );
                    if ( specData instanceof LineIDSpecData ) {
                        if ( lineIDSpecVisible( (LineIDSpecData)specData ) ) {
                            specData.setYOffset( fraction );
                            fraction += shift;
                            nshift++;
                        }
                        else {
                            specData.setYOffset( 0.0 );
                        }
                    }
                    else {
                        specData.setYOffset( fraction );
                        fraction += shift;
                    }
                }
                dmin = 0.0;
                if ( nshift > 1 ) nshift--;
                dmax = fraction - ((double) nshift* shift);
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
