package uk.ac.starlink.splat.data;

import uk.ac.starlink.ast.Grf;
import uk.ac.starlink.ast.Plot;
import uk.ac.starlink.splat.ast.ASTJ;

/**
 *  General interface that objects providing public access to spectral
 *  data should implement.
 *  <p>
 *  @since $Date$
 *  @since 21-SEP-2000
 *  @version $Id$
 *  @author Peter W. Draper
 *  @see SpecData
 *
 */
public interface SpecDataAccess 
{
    /**
     *  Draw the spectral data using the graphics context provided.
     */
    public void drawSpec( Grf grf, Plot plot, double[] limits );

    /**
     *  Get reference to ASTJ object that implements the spectral
     *  coordinate system.
     */
    public ASTJ getAst();

    /**
     *  Get a symbolic name for the spectral context.
     */
    public String getShortName();

    /**
     *  Get the full name for the spectrum.
     */
    public String getFullName();

    /**
     *  Get the range (i.e. data limits) of the spectral data.
     */
    public double[] getRange();

    /**
     *  Get the full range (i.e. data limits plus standard deviations)
     *  of the spectral data. 
     */
    public double[] getFullRange();

    /**
     *  Get the dimension of the spectrum (i.e. the number of values
     *  it contains).
     */
    public int size();

    /**
     *  Lookup the physical values (i.e. wavelength and data value)
     *  that correspond to a graphics X coordinate.
     *  <p>
     *  Note that this only works for first spectrum.
     *
     *  @param xg X graphics coordinate
     *  @param plot AST plot needed to transform graphics position
     *              into physical coordinates
     *  
     */
    public double[] lookup( int xg, Plot plot );

}
