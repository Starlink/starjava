/*
 * Copyright (C) 2008 Science and Technology Facilities Council
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
 * Define the <tt>stacking</tt> order of the spectra displayed in a
 * {@link DivaPlot}.
 * <p>
 * The order is defined by a value derived from the properties (using a JEL
 * expression) in the SpecData instances associated with a plot. When
 * reorder is called the yoffset of each associated SpecData is updated to be
 * a multiple of the shift.
 *
 * @author Peter W. Draper
 * @version $Id:$
 */
public class PlotStacker
{
    private DivaPlot plot = null;
    private String expression = null;
    private double shift = 0.1;

    /**
     * Constructor. Define the {@link DivaPlot} we're associated with.
     *
     * @param plot the DivaPlot
     * @param expression a JEL expression that references the FITS headers in
     *                   the SpecData instances displayed in the Plot. The
     *                   relative value of this defines the stacking order.
     */
    public PlotStacker( DivaPlot plot, String expression, double shift )
    {
        setPlot( plot );
        setExpression( expression );
        setShift( shift );
    }

    /**
     * Set the DivaPlot we're associated with.
     */
    public void setPlot( DivaPlot plot )
    {
        this.plot = plot;
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
     * Do an update of the stacking order in the DivaPlot. This should be
     * called sometime after the spectra displayed the associated Plot are
     * changed, but before the plot is redrawn.
     *
     * @throws a SplatException will be thrown if an error evaluating the
     *         expression occurs.
     */
    public void reorder()
        throws SplatException
    {
        // Use the SpecDataComp to iterate over the spectra and update the
        // order number.
        SpecDataComp spectra = plot.getSpecDataComp();
        int nspectra = spectra.count();

        double[] values = new double[nspectra];
        int[] indices = new int[nspectra];
        SpecData specData;
        if ( expression != null && ! "".equals( expression ) ) {

            //  Have an expression, so evaluate and sort.
            for ( int i = 0; i < nspectra; i++ ) {
                specData = spectra.get( i );
                indices[i] = i;
                values[i] = TableCalc.calc( specData, expression );
            }
            Sort.insertionSort2( values, indices );
        }
        else {
            //  No expression, so no ordering, just however the SpecDataComp
            //  orders them.
            for ( int i = 0; i < nspectra; i++ ) {
                indices[i] = i;
            }
        }

        // Now update the offsets in the SpecData themselves.
        double fraction = 0.0;
        for ( int i = 0; i < nspectra; i++ ) {
            specData = spectra.get( indices[i] );
            specData.setYOffset( fraction );
            fraction += shift;
        }
    }
}
