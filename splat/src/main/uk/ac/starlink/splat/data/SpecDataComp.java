/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     21-SEP-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.data;

import java.util.ArrayList;

import uk.ac.starlink.splat.ast.ASTJ;
import uk.ac.starlink.ast.AstException;
import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.Grf;
import uk.ac.starlink.ast.Plot;
import uk.ac.starlink.splat.util.SplatException;

/**
 * This class is designed to handle multiple instances of SpecData
 * objects. Thus creating an apparent "composite" spectrum, from
 * possibly several others.
 * <p>
 * This feature is intended, for instance, to allow the display
 * multiple spectra in a single plot and should be used when
 * referencing spectral data.
 * <p>
 * Note that the first spectrum holds special status. This defines the
 * coordinate system that all other spectra should honour. It also is
 * reported first in constructs such as the name.
 * <p>
 * Alignment of coordinates uses astConvert.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see SpecData
 */
public class SpecDataComp
{
    /* @hints This could be extended using the Composite design pattern to
     * allow the import of groups of SpecData objects from complete
     * composite plots, but I'm keeping this simple for now and only
     * allowing the import of single spectra at a time.
     *
     * The alignment stuff is quite expensive, slow and takes a lot of
     * memery, especially for large numbers of spectra, so it is
     * switchable at present. Could look into why and maybe form a
     * caching system of somekind (or get SpecData's to take an extra
     * mapping).
     */

    /** Whether we're being careful to match coordinates */
    private boolean coordinateMatching = false;

    /**
     *  List of references to the spectra. Note that indexing this
     *  list isn't fixed, i.e. removing SpecData objects reshuffles
     *  the indices.
     */
    protected ArrayList spectra = new ArrayList();

    /**
     *  Create a SpecDataComp instance.
     */
    public SpecDataComp()
    {
        //  Do nothing.
    }

    /**
     *  Create a SpecDataComp adding in the first spectrum.
     */
    public SpecDataComp( SpecData inspec )
    {
        add( inspec );
    }

    /**
     *  Create a SpecDataComp adding in the first from a concrete
     *  implementation.
     */
    public SpecDataComp( SpecDataImpl inspec ) throws SplatException
    {
        add( new SpecData( inspec ) );
    }

    /**
     * Set whether we're being careful about matching coordinates
     * between spectra. If so then AST will check if any SpecFrames
     * can be converted to preserve units, systems, etc. Switching
     * this on can be slow.
     */
    public void setCoordinateMatching( boolean on )
    {
        coordinateMatching = on;
    }

    /**
     * Get whether we're being careful about matching coordinates.
     */
    public boolean isCoordinateMatching()
    {
        return coordinateMatching;
    }

    /**
     *  Add a spectrum to the managed list.
     *
     *  @param inspec reference to a SpecData object that is to be
     *                added to the composite
     */
    public void add( SpecData inspec )
    {
        spectra.add( inspec );
    }

    /**
     *  Remove a spectrum.
     *
     *  @param inspec reference to the spectrum to remove.
     */
    public void remove( SpecData inspec )
    {
        spectra.remove( inspec );
    }

    /**
     *  Remove a spectrum.
     *
     *  @param index the index of the spectrum.
     */
    public void remove( int index )
    {
        spectra.remove( index );
    }

    /**
     *  Get a reference to a spectrum.
     *
     *  @param index the index of the spectrum.
     */
    public SpecData get( int index )
    {
        return (SpecData) spectra.get( index );
    }

    /**
     *  Get the index of a spectrum.
     *
     *  @param index the index of the spectrum.
     */
    public int indexOf( SpecData inspec )
    {
        return spectra.indexOf( inspec );
    }

    /**
     *  Get the number of spectra currently being handled.
     */
    public int count()
    {
        return spectra.size();
    }

    /**
     *  Return if we already have a reference to a spectrum.
     */
    public boolean have( SpecData spec )
    {
        if ( spectra.indexOf( spec ) > -1 ) {
            return true;
        } else {
            return false;
        }
    }

    /**
     *  Get reference to ASTJ object set up to specify the coordinate
     *  system. This always returns the ASTJ object of the first
     *  spectrum, so all other spectra must have a context that is
     *  valid within the coordinate system defined by it.
     */
    public ASTJ getAst()
    {
        return ((SpecData)spectra.get(0)).getAst();
    }

    /**
     *  Get a symbolic name for all spectra.
     */
    public String getShortName()
    {
        StringBuffer name = new StringBuffer( ((SpecData)spectra.get(0)).getShortName() );
        if ( spectra.size() > 1 ) {
            name.append("(+").append(spectra.size()-1).append(" others)");
        }
        return name.toString();
    }

    /**
     *  Get a full name for all spectra. Blank.
     */
    public String getFullName()
    {
        return "";
    }

    /**
     *  Get the symbolic name of a spectrum.
     */
    public String getShortName( int index )
    {
        return ((SpecData)spectra.get(index)).getShortName();
    }

    /**
     *  Get the full name of a spectrum.
     */
    public String getFullName( int index )
    {
        return ((SpecData)spectra.get(index)).getFullName();
    }

    /**
     *  Get the data range of all the spectra
     */
    public double[] getRange()
        throws SplatException
    {
        double[] range = new double[4];
        range[0] = Double.MAX_VALUE;
        range[1] = -Double.MAX_VALUE;
        range[2] = Double.MAX_VALUE;
        range[3] = -Double.MAX_VALUE;
        SpecData baseSpectrum = null;
        SpecData spectrum = null;
        int failed  = 0;
        SplatException lastException = null;
        for ( int i = 0; i < spectra.size(); i++ ) {
            spectrum = (SpecData) spectra.get(i);
            double[] newrange = spectrum.getRange();
            if ( coordinateMatching ) {
                if ( i > 0 ) {
                    //  Need to convert between these coordinates and
                    //  those of the reference spectrum.
                    try {
                        newrange = transformRange( baseSpectrum,
                                                   spectrum,
                                                   newrange);
                    }
                    catch (SplatException e) {
                        failed++;
                        lastException = e;
                    }
                }
                else {
                    baseSpectrum = spectrum;
                }
            }
            checkRangeLimits( newrange, range );
        }
        if ( lastException != null ) {
            throw new SplatException( "Failed to align coordinate systems",
                                      lastException);
        }
        return range;
    }

    /**
     * Given a limit range, check if these need changing to include a
     * new set of limits.
     */
    private void checkRangeLimits( double[] newrange, double[] range )
    {
        //  First two values are X limits, second two Y. Ordering is
        //  not guaranteed so check both.
        range[0] = Math.min( range[0], newrange[0] );
        range[0] = Math.min( range[0], newrange[1] );

        range[1] = Math.max( range[1], newrange[0] );
        range[1] = Math.max( range[1], newrange[1] );

        range[2] = Math.min( range[2], newrange[2] );
        range[2] = Math.min( range[2], newrange[3] );

        range[3] = Math.max( range[3], newrange[2] );
        range[3] = Math.max( range[3], newrange[3] );
    }

    /**
     *  Get the full data range of all the spectra.
     */
    public double[] getFullRange()
        throws SplatException
    {
        double[] range = new double[4];
        range[0] = Double.MAX_VALUE;
        range[1] = -Double.MAX_VALUE;
        range[2] = Double.MAX_VALUE;
        range[3] = -Double.MAX_VALUE;
        SpecData baseSpectrum = null;
        SpecData spectrum = null;
        int failed = 0;
        SplatException lastException = null;
        for ( int i = 0; i < spectra.size(); i++ ) {
            spectrum = (SpecData) spectra.get(i);
            double[] newrange = spectrum.getFullRange();
            if ( coordinateMatching ) {
                if ( i > 0 ) {
                    //  Need to convert between these coordinates and
                    //  those of the reference spectrum.
                    try {
                        newrange = transformRange( baseSpectrum,
                                                   spectrum,
                                                   newrange);
                    }
                    catch (SplatException e) {
                        failed++;
                        lastException = e;
                    }
                }
                else {
                    baseSpectrum = spectrum;
                }
            }
            checkRangeLimits( newrange, range );
        }
        if ( lastException != null ) {
            throw new SplatException( "Failed to align coordinate systems",
                                      lastException );
        }
        return range;
    }

    /**
     * Get the data range of the spectra, that should be used when
     * auto-ranging. Autoranging only uses spectra marked for this
     * purpose, unless there are no allowable spectra (in which case
     * it would be bad to have no autorange). If errorbars are in use
     * then their range is also accomodated.
     */
    public double[] getAutoRange()
        throws SplatException
    {
        double[] range = new double[4];
        range[0] = Double.MAX_VALUE;
        range[1] = -Double.MAX_VALUE;
        range[2] = Double.MAX_VALUE;
        range[3] = -Double.MAX_VALUE;
        int count = spectra.size();
        int used = 0;
        double newrange[];
        SpecData baseSpectrum = null;
        SpecData spectrum = null;
        int failed = 0;
        SplatException lastException = null;
        for ( int i = 0; i < count; i++ ) {
            spectrum = (SpecData)spectra.get(i);
            if ( spectrum.isUseInAutoRanging() || count == 1 ) {
                if ( spectrum.isDrawErrorBars() ) {
                    newrange = spectrum.getFullRange();
                }
                else {
                    newrange = spectrum.getRange();
                }
                if ( coordinateMatching ) {
                    if ( i > 0 ) {
                        //  Need to convert between these coordinates and
                        //  those of the reference spectrum.
                        try {
                            newrange = transformRange( baseSpectrum, spectrum,
                                                       newrange);
                        }
                        catch (SplatException e) {
                            failed++;
                            lastException = e;
                        }
                    }
                    else {
                        baseSpectrum = spectrum;
                    }
                }
                checkRangeLimits( newrange, range );
                used++;
            }
        }
        if ( used == 0 ) {
            range = getFullRange();
        }
        if ( lastException != null ) {
            throw new SplatException( "Failed to align coordinate systems",
                                      lastException );
        }
        return range;
    }

    /**
     * Transform range-like position-pairs between the plot
     * coordinates of two spectra. The coordinate systems are aligned
     * using astConvert if possible, otherwise the input coordinates
     * are returned.
     *
     * The input and output coordinates are [x1,x2,y1,y2,...].
     */
    public double[] transformRange( SpecData target, SpecData source,
                                    double[] range )
        throws SplatException
    {
        double[] result = range;
        try {
            //  Try to align the plotting FrameSets of the SpecData. Only
            //  need to do this between DATAPLOT domains. ?? Will this
            //  work with CmpFrame containing one SpecFrame?
            FrameSet to = target.getAst().getRef();
            FrameSet fr = source.getAst().getRef();
            FrameSet aligned = fr.convert( to, "DATAPLOT" );
            if ( aligned == null ) {
                throw new SplatException( "Failed to aligned coordinates" +
                                          " while transforming ranges");
            }

            //  2D coords, so need separate X,Y coords.
            double xin[] = new double[range.length/2];
            double yin[] = new double[xin.length];
            for ( int i = 0; i < xin.length; i++ ) {
                xin[i] = range[i];
                yin[i] = range[i+xin.length];
            }
            double[][] tmp = aligned.tran2( xin.length, xin, yin, true );

            // Put back to vectorized array.
            result = new double[range.length];
            for ( int i = 0; i < xin.length; i++ ) {
                result[i] = tmp[0][i];
                result[i+xin.length] = tmp[1][i];
            }
        }
        catch (Exception e) {
            // All Exceptions are recast to SplatExceptions.
            throw new SplatException( e );
        }
            return result;
    }


    /**
     * Transform position-pairs (usually limits) between the
     * coordinates of a Plot to those of a spectrum. The coordinate
     * systems are aligned using astConvert if possible, otherwise the
     * input coordinates are returned.
     *
     * The input and output coordinates are [x1,y1,x2,y2,...].
     */
    public double[] transformLimits( Plot plot, SpecData target,
                                     double[] limits )
        throws SplatException
    {
        double[] result = limits;
        Frame to = plot.getFrame( FrameSet.AST__CURRENT );
        Frame fr =
            target.getAst().getRef().getFrame( FrameSet.AST__CURRENT );
        FrameSet aligned = to.convert( fr, "DATAPLOT" );
        if ( aligned == null ) {
            throw new SplatException( "Failed to aligned coordinates" +
                                      " while transforming limits");
        }

        //  2D coords, so need separate X,Y coords.
        double xin[] = new double[limits.length/2];
        double yin[] = new double[xin.length];
        for ( int i = 0, j = 0; i < xin.length; i++, j+=2 ) {
            xin[i] = limits[j];
            yin[i] = limits[j+1];
        }
        double[][] tmp = aligned.tran2( xin.length, xin, yin, true );

        // Put back to vectorized array.
        result = new double[limits.length];
        for ( int i = 0, j = 0; i < xin.length; i++, j+=2 ) {
            result[j] = tmp[0][i];
            result[j+1] = tmp[1][i];
        }
        return result;
    }

    /**
     *  Draw all spectra using the graphics context provided.
     */
    public void drawSpec( Grf grf, Plot plot, double[] limits )
        throws SplatException
    {
        Plot localPlot = plot;
        double[] localLimits = limits;
        SpecData spectrum = null;
        for ( int i = 0; i < spectra.size(); i++ ) {
            spectrum = (SpecData)spectra.get( i );
            if ( coordinateMatching ) {
                if ( i > 0 ) {
                    // Need to align these plot coordinates with ones
                    // we're drawing.
                    localPlot = alignPlots( plot, spectrum );
                    localLimits = transformLimits( plot, spectrum, limits );
                }
            }
            spectrum.drawSpec( grf, localPlot, localLimits );
        }
    }

    /**
     * Modify a plot so that it uses a different set of current
     * coordinates. The coordinate systems are aligned using
     * astConvert if possible, otherwise the original plot is
     * returned.
     */
    public Plot alignPlots( Plot plot, SpecData source )
        throws SplatException
    {
        Plot result = (Plot)plot.copy();

        //  Try to align the plot FrameSet and the SpecData. Only
        //  need to do this between DATAPLOT domains.
        Frame to = result.getFrame( FrameSet.AST__CURRENT );
        Frame from = source.getAst().getRef().getFrame(FrameSet.AST__CURRENT);
        FrameSet aligned = to.convert( from, "DATAPLOT" );
        if ( aligned == null ) {
            throw new SplatException( "Failed to align coordinates" +
                                      " while transforming between plots");
        }

        result.addFrame( FrameSet.AST__CURRENT, aligned, from );
        return result;
    }

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
    public double[] lookup( int xg, Plot plot )
    {
        return ((SpecData)spectra.get(0)).lookup( xg, plot );
    }

    /**
     *  Lookup the physical values (i.e. wavelength and data value)
     *  that correspond to a graphics X coordinate, returned in
     *  formatted strings (could be hh:mm:ss.ss for instance).
     *  <p>
     *  Note that this only works for first spectrum.
     *
     *  @param xg X graphics coordinate
     *  @param plot AST plot needed to transform graphics position
     *              into physical coordinates
     *
     */
    public String[] formatLookup( int xg, Plot plot )
    {
        return ((SpecData)spectra.get(0)).formatLookup( xg, plot );
    }

    /**
     *  Lookup interpolated physical values (i.e. wavelength and data value)
     *  that correspond to a graphics X coordinate, returned in
     *  formatted strings (could be hh:mm:ss.ss for instance).
     *  <p>
     *  Note that this only works for first spectrum.
     *
     *  @param xg X graphics coordinate
     *  @param plot AST plot needed to transform graphics position
     *              into physical coordinates
     *
     */
    public String[] formatInterpolatedLookup( int xg, Plot plot )
    {
        return ((SpecData)spectra.get(0)).formatInterpolatedLookup( xg, plot );
    }

    /**
     * Convert a formatted value into a floating value coordinates
     * (the input could be hh:mm:ss.s, in which case we get back
     * suitable radians).
     *
     *  @param axis the axis to use for formatting rules.
     *  @param plot AST plot that defines the coordinate formats.
     *  @param value the formatted value.
     *  @return the unformatted value.
     */
    public double unFormat( int axis, Plot plot, String value )
    {
        return ((SpecData)spectra.get(0)).unFormat( axis, plot, value );
    }

    /**
     *  Convert a floating point coordinate into a value formatted for
     *  a given axis.
     *
     *  @param axis the axis to use for formatting rules.
     *  @param plot AST plot that defines the coordinate formats.
     *  @param value the value.
     *  @return the formatted value.
     */
    public String format( int axis, Plot plot, double value )
    {
        return ((SpecData)spectra.get(0)).format( axis, plot, value );
    }

    /**
     *  Get the size of the spectrum (first only).
     */
    public int size()
    {
        return ((SpecData)spectra.get(0)).size();
    }
}
