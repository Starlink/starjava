package uk.ac.starlink.splat.data;

import java.util.ArrayList;

import uk.ac.starlink.splat.ast.ASTJ;
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
 * No alignment of coordinates using astConvert is attempted at present.
 *
 * @since $Date$
 * @since 21-SEP-2000
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2000 Central Laboratory of the Research Councils
 * @see SpecData
 * @hints This could be extended using the Composite design pattern to
 * allow the import of groups of SpecData objects from complete
 * composite plots, but I'm keeping this simple for now and only
 * allowing the import of single spectra at a time.
 */
public class SpecDataComp 
{
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
    {
        double[] range = new double[4];
        range[0] = Double.MAX_VALUE;
        range[1] = Double.MIN_VALUE;
        range[2] = Double.MAX_VALUE;
        range[3] = Double.MIN_VALUE;
        for ( int i = 0; i < spectra.size(); i++ ) {
            double[] newrange = ((SpecData)spectra.get(i)).getRange();
            range[0] = Math.min( range[0], newrange[0] );
            range[1] = Math.max( range[1], newrange[1] );
            range[2] = Math.min( range[2], newrange[2] );
            range[3] = Math.max( range[3], newrange[3] );
        }
        return range;
    }

    /**
     *  Get the full data range of all the spectra.
     */
    public double[] getFullRange() 
    {
        double[] range = new double[4];
        range[0] = Double.MAX_VALUE;
        range[1] = Double.MIN_VALUE;
        range[2] = Double.MAX_VALUE;
        range[3] = Double.MIN_VALUE;
        for ( int i = 0; i < spectra.size(); i++ ) {
            double[] newrange = ((SpecData)spectra.get(i)).getFullRange();
            range[0] = Math.min( range[0], newrange[0] );
            range[1] = Math.max( range[1], newrange[1] );
            range[2] = Math.min( range[2], newrange[2] );
            range[3] = Math.max( range[3], newrange[3] );
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
    {
        double[] range = new double[4];
        range[0] = Double.MAX_VALUE;
        range[1] = Double.MIN_VALUE;
        range[2] = Double.MAX_VALUE;
        range[3] = Double.MIN_VALUE;
        SpecData spec = null;
        int count = spectra.size();
        int used = 0;
        double newrange[];
        for ( int i = 0; i < count; i++ ) {
            spec = (SpecData)spectra.get(i);
            if ( spec.isUseInAutoRanging() || count == 1 ) {
                if ( spec.isDrawErrorBars() ) {
                    newrange = spec.getFullRange();
                } 
                else {
                    newrange = spec.getRange();
                }
                range[0] = Math.min( range[0], newrange[0] );
                range[1] = Math.max( range[1], newrange[1] );
                range[2] = Math.min( range[2], newrange[2] );
                range[3] = Math.max( range[3], newrange[3] );
                used++;
            }
        }
        if ( used == 0 ) {
            range = getFullRange();
        }
        return range;
    }

    /**
     *  Draw all spectra using the graphics context provided.
     */
    public void drawSpec( Grf grf, Plot plot, double[] limits ) 
    {
        for ( int i = 0; i < spectra.size(); i++ ) {
            ((SpecData)spectra.get(i)).drawSpec( grf, plot, limits );
        }
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
