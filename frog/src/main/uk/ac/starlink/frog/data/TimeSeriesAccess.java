package uk.ac.starlink.frog.data;

import uk.ac.starlink.ast.Grf;
import uk.ac.starlink.ast.Plot;
import uk.ac.starlink.frog.ast.AstUtilities;

/**
 *  General interface that objects providing public access to time series
 *  should implement. Based on SPLAT's SpecDataAccess. 
 *  <p>
 *  @since $Date$
 *  @since 21-SEP-2000
 *  @version $Id$
 *  @author Peter W. Draper
 *  @author Alasdair Allan
 *  @see TimeSeries
 *
 */
public interface TimeSeriesAccess 
{
    /**
     *  Draw the time series data using the graphics context provided.
     */
    public void drawSeries( Grf grf, Plot plot, double[] limits );

    /**
     *  Get reference to AstUtilites object that implements the time
     *  series coordinate system.
     */
    public AstUtilities getAst();

    /**
     *  Get a symbolic name for the time series context.
     */
    public String getShortName();

    /**
     *  Get the full name for the time series
     */
    public String getFullName();

    /**
     *  Get the range (i.e. data limits) of the time series data.
     */
    public double[] getRange();

    /**
     *  Get the full range (i.e. data limits plus standard deviations)
     *  of the time series data. 
     */
    public double[] getFullRange();

    /**
     *  Get the dimension of the time series (i.e. the number of values
     *  it contains).
     */
    public int size();

    /**
     *  Lookup the physical values (i.e. time and data value)
     *  that correspond to a graphics X coordinate.
     *
     *  @param xg X graphics coordinate
     *  @param plot AST plot needed to transform graphics position
     *              into physical coordinates
     *  
     */
    public double[] lookup( int xg, Plot plot );

}
