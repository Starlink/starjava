/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     21-SEP-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.data;

import java.util.ArrayList;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import uk.ac.starlink.ast.AstException;
import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.Grf;
import uk.ac.starlink.ast.Plot;
import uk.ac.starlink.ast.SpecFrame;
import uk.ac.starlink.splat.ast.ASTJ;
import uk.ac.starlink.splat.util.SplatException;

/**
 * This class is designed to handle multiple instances of SpecData
 * objects. Thus creating an apparent "composite" spectrum, from possibly
 * several others.
 * <p>
 * This feature is intended, for instance, to allow the display multiple
 * spectra in a single plot and should be used when referencing spectral data.
 * <p>
 * One of the spectra (initially the first) has special status and is known as
 * the current spectrum. This defines the coordinate system that all other
 * spectra should honour. It also is used in constructs such as the compound
 * name.
 * <p>
 * Alignment of coordinates uses astConvert.
 * <p>
 * This class implements the {@link ComboBoxModel} interface so that it can be
 * used in JComboBoxes (and JLists) that want to display its state.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see SpecData
 */
public class SpecDataComp
    implements ComboBoxModel
{
    /* @hints This could be extended using the Composite design pattern to
     * allow the import of groups of SpecData objects from complete
     * composite plots, but I'm keeping this simple for now and only
     * allowing the import of single spectra at a time.
     *
     * The alignment stuff is quite expensive, slow and takes a lot of
     * memory, especially for large numbers of spectra, so it is
     * switchable at present. Could look into why and maybe form a
     * caching system of somekind (or get SpecData's to take an extra
     * mapping).
     */

    /**
     * The "current" SpecData object. This defines the base coordinate system
     * used for drawing, transformations etc.
     */
    protected SpecData currentSpec = null;

    /**
     * The last "current" SpecData object. Retained so that transformations
     * from this to the new current spectrum may be attempted, but you'd
     * better be quick.
     */
    protected SpecData lastCurrentSpec = null;

    /**
     * Whether we're matching coordinate units between spectra.
     */
    private boolean coordinateMatching = false;

    /**
     * Whether we're matching data units between spectra. This requires
     * that coordinateMatching is also true.
     */
    private boolean dataUnitsMatching = true; // false;

    /**
     * Whether we are to include spacing for error bars in the automatic
     * ranging.
     */
    private boolean errorbarAutoRanging = false;

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
     * Set whether we're being careful about matching data units between
     * spectra. If so then AST will check if any Frames can be converted to
     * preserve units.
     */
    public void setDataUnitsMatching( boolean on )
    {
        dataUnitsMatching = on;
    }

    /**
     * Get whether we're being careful about matching data units between
     * spectra.
     */
    public boolean isDataUnitsMatching()
    {
        return dataUnitsMatching;
    }

    /**
     * Set whether we need to add extra space in the autoranging for
     * the errorbars, if displayed.
     */
    public void setErrorbarAutoRanging( boolean on )
    {
        errorbarAutoRanging = on;
    }

    /**
     * Get whether we need to add extra space in the autoranging for the
     * errorbars, if displayed.
     */
    public boolean isErrorbarAutoRanging()
    {
        return errorbarAutoRanging;
    }

    /**
     * Set the current spectrum. Is added to list if not already present.
     */
    public void setCurrentSpectrum( SpecData spectrum )
    {
        if ( ! spectrum.equals( currentSpec ) ) {
            lastCurrentSpec = currentSpec;
            currentSpec = spectrum;
            if ( ! spectra.contains( spectrum ) ) {
                add( spectrum );
            }
        }
    }

    /**
     * Set the current spectrum by index.
     */
    public void setCurrentSpectrum( int index )
    {
        SpecData spectrum = (SpecData) spectra.get( index );
        setCurrentSpectrum( spectrum );
    }

    /**
     * Return the current spectrum.
     */
    public SpecData getCurrentSpectrum()
    {
        return currentSpec;
    }

    /**
     * Return the last current spectrum.
     */
    public SpecData getLastCurrentSpectrum()
    {
        return lastCurrentSpec;
    }

    /**
     *  Add a spectrum to the managed list.
     *
     *  @param inspec reference to a SpecData object that is to be
     *                added to the composite
     */
    public void add( SpecData inspec )
    {
        if ( currentSpec == null ) {
            // First spectrum is current.
            currentSpec = inspec;
        }
        spectra.add( inspec );
        fireListDataAdded( spectra.indexOf( inspec ) );
    }

    /**
     *  Remove a spectrum.
     *
     *  @param inspec reference to the spectrum to remove.
     */
    public void remove( SpecData inspec )
    {
	int index = spectra.indexOf( inspec );
        spectra.remove( inspec );
        if ( inspec.equals( currentSpec ) ) {
            // Removing the current spectrum. This becomes the first in list
            // or null.
            lastCurrentSpec = currentSpec;
            if ( spectra.size() > 0 ) {
                currentSpec = (SpecData) spectra.get( 0 );
            }
            else {
                currentSpec = null;
            }
        }
	if ( index != -1 ) {
	    fireListDataRemoved( index );
	}
    }

    /**
     *  Remove a spectrum.
     *
     *  @param index the index of the spectrum.
     */
    public void remove( int index )
    {
        SpecData spectrum = (SpecData) spectra.get( index );
        remove( spectrum );
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
     *  @param inspec the spectrum.
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
        }
        else {
            return false;
        }
    }

    /**
     *  Get reference to ASTJ object set up to specify the coordinate
     *  system. This always returns the ASTJ object of the current
     *  spectrum, so all other spectra must have a context that is
     *  valid within the coordinate system defined by it.
     */
    public ASTJ getAst()
    {
        if ( currentSpec != null ) {
            return currentSpec.getAst();
        }
        return null;
    }

    /**
     *  Get a symbolic name for all spectra.
     */
    public String getShortName()
    {
        if ( currentSpec != null ) {
            StringBuffer name = new StringBuffer( currentSpec.getShortName() );
            if ( spectra.size() > 1 ) {
                name.append( "(+" )
                    .append( spectra.size() - 1 )
                    .append( " others)" );
            }
            return name.toString();
        }
        return "";
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
        return ( (SpecData)spectra.get( index ) ).getShortName();
    }

    /**
     *  Get the full name of a spectrum.
     */
    public String getFullName( int index )
    {
        return ( (SpecData)spectra.get( index ) ).getFullName();
    }

    /**
     *  Get the data range of all the spectra
     */
    public double[] getRange()
        throws SplatException
    {
        if ( currentSpec == null ) {
            double[] range = new double[4];
            range[0] = Double.MAX_VALUE;
            range[1] = -Double.MAX_VALUE;
            range[2] = Double.MAX_VALUE;
            range[3] = -Double.MAX_VALUE;
            return range;
        }
        double[] range = (double[]) currentSpec.getRange().clone();

        int count = spectra.size();
        SpecData spectrum = null;
        int failed  = 0;
        SplatException lastException = null;
        double[] newrange;
        for ( int i = 0; i < count; i++ ) {
            spectrum = (SpecData) spectra.get( i );
            if ( ! spectrum.equals( currentSpec ) ) {
                newrange = spectrum.getRange();
                if ( coordinateMatching ) {
                    //  Need to convert between these coordinates and those of
                    //  the reference spectrum.
                    try {
                        newrange = transformRange( currentSpec, spectrum,
                                                   newrange,
                                                   dataUnitsMatching );
                    }
                    catch (SplatException e) {
                        failed++;
                        lastException = e;
                    }
                }
                checkRangeLimits( newrange, range );
            }
        }
        if ( lastException != null ) {
            throw new SplatException( "Failed to align coordinate systems",
                                      lastException );
        }
        return range;
    }

    /**
     * Given a range of limits, check if these need changing to include a
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
     * Get the range of a spectrum. Includes space for errorbars in they are
     * being drawn and if we have been asked to include it.
     */
    protected double[] getSpectrumRange( SpecData spectrum )
    {
        double[] newrange = null;
        if ( spectrum.isDrawErrorBars() && errorbarAutoRanging ) {
            newrange = spectrum.getFullRange();
        }
        else {
            newrange = spectrum.getRange();
        }
        return newrange;
    }

    /**
     *  Get the full data range of all the spectra. This includes space for
     *  error bars if selected.
     */
    public double[] getFullRange()
        throws SplatException
    {
        if ( currentSpec == null ) {
            double[] range = new double[4];
            range[0] = Double.MAX_VALUE;
            range[1] = -Double.MAX_VALUE;
            range[2] = Double.MAX_VALUE;
            range[3] = -Double.MAX_VALUE;
            return range;
        }
        double[] range = (double[]) getSpectrumRange( currentSpec ).clone();

        int count = spectra.size();
        SpecData spectrum = null;
        int failed = 0;
        SplatException lastException = null;
        double[] newrange;
        for ( int i = 0; i < count; i++ ) {
            spectrum = (SpecData) spectra.get( i );
            if ( ! spectrum.equals( currentSpec ) ) {
                newrange = getSpectrumRange( spectrum );
                if ( coordinateMatching ) {
                    //  Need to convert between these coordinates and those of
                    //  the reference spectrum.
                    try {
                        newrange = transformRange( currentSpec, spectrum,
                                                   newrange,
                                                   dataUnitsMatching );
                    }
                    catch (SplatException e) {
                        failed++;
                        lastException = e;
                    }
                }
                checkRangeLimits( newrange, range );
            }
        }
        if ( lastException != null ) {
            throw new SplatException( "Failed to align coordinate systems",
                                      lastException );
        }
        return range;
    }

    /**
     * Get the data range of the spectra, that should be used when
     * auto-ranging. Autoranging only uses spectra marked for this purpose,
     * unless there are no allowable spectra (in which case it would be bad to
     * have no autorange). If errorbars are in use then their range is also
     * accommodated, if requested.
     */
    public double[] getAutoRange()
        throws SplatException
    {
        if ( currentSpec == null ) {
            double[] range = new double[4];
            range[0] = Double.MAX_VALUE;
            range[1] = -Double.MAX_VALUE;
            range[2] = Double.MAX_VALUE;
            range[3] = -Double.MAX_VALUE;
            return range;
        }
        double[] range = (double[]) getSpectrumRange( currentSpec ).clone();

        int count = spectra.size();
        int used = 1;
        double newrange[];
        SpecData spectrum = null;
        int failed = 0;
        SplatException lastException = null;
        for ( int i = 0; i < count; i++ ) {
            spectrum = (SpecData)spectra.get( i );
            if ( ! spectrum.equals( currentSpec ) ) {
                if ( spectrum.isUseInAutoRanging() ) {
                    newrange = getSpectrumRange( spectrum );
                    if ( coordinateMatching ) {
                        //  Need to convert between these coordinates and
                        //  those of the reference spectrum.
                        try {
                            newrange = transformRange( currentSpec, spectrum,
                                                       newrange,
                                                       dataUnitsMatching );
                        }
                        catch (SplatException e) {
                            failed++;
                            lastException = e;
                        }
                    }
                    checkRangeLimits( newrange, range );
                    used++;
                }
            }
        }
        if ( lastException != null ) {
            throw new SplatException( "Failed to align coordinate systems",
                                      lastException );
        }
        return range;
    }

    /**
     * Transform range-like position-pairs between the plot coordinates
     * of two spectra. The coordinate systems are aligned using astConvert
     * if possible, if not a {@link SplatExceotion} is thrown.
     *
     * The input and output coordinates are [x1,x2,y1,y2,...].
     */
    public double[] transformRange( SpecData target, SpecData source,
                                    double[] range, boolean matchDataUnits )
        throws SplatException
    {
        if ( range == null ) return null;

        double[] result = range;
        try {
            //  Try to align the plotting FrameSets of the SpecData. Only need
            //  to do this between DATAPLOT domains. If requested set the
            //  active units for the whole frame to get the data units aligned
            //  actively too (SpecFrames are always on, so we just need to get
            //  the data axis working).
            FrameSet to = target.getAst().getRef();
            FrameSet fr = source.getAst().getRef();
            if ( matchDataUnits ) {
                to.setActiveUnit( true );
            }
            String stdofrest = null;
            FrameSet aligned = fr.convert( to, "DATAPLOT" );
            if ( matchDataUnits ) {
                to.setActiveUnit( false );
            }
            if ( aligned == null ) {
                if ( matchDataUnits ) {
                    throw new SplatException( "Failed to align spectral " +
                                              "coordinates or data units" );
                }
                throw new SplatException( "Failed to spectral coordinates" );
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
                                     double[] limits, boolean matchDataUnits )
        throws SplatException
    {
        if ( limits == null ) return null;

        double[] result = limits;
        Frame to = plot.getFrame( FrameSet.AST__CURRENT );
        Frame fr =
            target.getAst().getRef().getFrame( FrameSet.AST__CURRENT );
        if ( matchDataUnits ) {
            fr.setActiveUnit( true );
        }
        FrameSet aligned = to.convert( fr, "DATAPLOT" );
        if ( matchDataUnits ) {
            fr.setActiveUnit( false );
        }
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
        if ( spectra.size() == 0 ) {
            return;
        }
        Plot localPlot = plot;
        double[] localLimits = limits;
        SpecData spectrum = null;
        for ( int i = 0; i < spectra.size(); i++ ) {
            spectrum = (SpecData)spectra.get( i );
            if ( coordinateMatching ) {
                if ( ! spectrum.equals( currentSpec ) ) {
                    //  The coordinates systems and, optionally, data units of
                    //  the spectra need to be matched.
                    localPlot = alignPlots( plot, spectrum, 
                                            dataUnitsMatching );
                    localLimits = transformLimits( plot, spectrum, limits,
                                                   dataUnitsMatching );
                }
                else {
                    localPlot = plot;
                    localLimits = limits;
                }
            }
            spectrum.drawSpec( grf, localPlot, localLimits );
        }
    }

    /**
     * Modify a plot so that it uses a different set of current
     * coordinates. The coordinate systems are aligned using astConvert if
     * possible, otherwise the original plot is returned.
     */
    public Plot alignPlots( Plot plot, SpecData source, 
                            boolean matchDataUnits )
        throws SplatException
    {
        Plot result = (Plot) plot.copy();

        //  Try to align the plot FrameSet and the SpecData. Only
        //  need to do this between DATAPLOT domains.
        Frame to = result.getFrame( FrameSet.AST__CURRENT );
        Frame from = source.getAst().getRef().getFrame(FrameSet.AST__CURRENT);

        // If spectrum is a LineID then we should attempt to transform it into
        // the system of main spectrum (this aligns if a source velocity is
        // set).
        int iaxes[] = { 1 };
        Frame picked = to.pickAxes( 1, iaxes, null );
        FrameSet aligned = null;
        if (source instanceof LineIDSpecData && picked instanceof SpecFrame) {
            //  How to match data units?
            String stdofrest = to.getC( "StdOfRest" );
            to.set( "StdOfRest=Source" );
            aligned = from.convert( to, "DATAPLOT" );
            to.set( "StdOfRest=" + stdofrest );
        }
        else {
            if ( matchDataUnits ) {
                from.setActiveUnit( true );
            }
            aligned = to.convert( from, "DATAPLOT" );
            if ( matchDataUnits ) {
                from.setActiveUnit( false );
            }
        }
        if ( aligned == null ) {
            throw new SplatException( "Failed to align coordinates" +
                                      " while transforming between plots");
        }

        result.addFrame( FrameSet.AST__CURRENT, aligned, from );
        return result;
    }

    /**
     *  Lookup the physical values (i.e.<!-- --> wavelength and data value)
     *  that correspond to a graphics X coordinate.
     *  <p>
     *  Note that this uses the current spectrum.
     *
     *  @param xg X graphics coordinate
     *  @param plot AST plot needed to transform graphics position
     *              into physical coordinates
     *
     */
    public double[] lookup( int xg, Plot plot )
    {
        if ( currentSpec != null ) {
            return currentSpec.lookup( xg, plot );
        }
        return new double[2];
    }

    /**
     *  Lookup the physical values (i.e.&nbsp;wavelength and data value)
     *  that correspond to a graphics X coordinate, returned in
     *  formatted strings (could be hh:mm:ss.ss for instance).
     *  <p>
     *  Note that this uses the current spectrum
     *
     *  @param xg X graphics coordinate
     *  @param plot AST plot needed to transform graphics position
     *              into physical coordinates
     *
     */
    public String[] formatLookup( int xg, Plot plot )
    {
        if ( currentSpec != null ) {
            return currentSpec.formatLookup( xg, plot );
        }
        return new String[] { "", "" };
    }

    /**
     *  Lookup interpolated physical values (that is the wavelength and data
     *  value) that correspond to a graphics X coordinate, returned in
     *  formatted strings (could be hh:mm:ss.ss for instance).
     *  <p>
     *  Note that this uses the current spectrum.
     *
     *  @param xg X graphics coordinate
     *  @param plot AST plot needed to transform graphics position
     *              into physical coordinates
     *
     */
    public String[] formatInterpolatedLookup( int xg, Plot plot )
    {
        if ( currentSpec != null ) {
            return currentSpec.formatInterpolatedLookup( xg, plot );
        }
        return new String[]{ "", "" };
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
        if ( currentSpec != null ) {
            return currentSpec.unFormat( axis, plot, value );
        }
        return 0.0;
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
        if ( currentSpec != null ) {
            return currentSpec.format( axis, plot, value );
        }
        return "";
    }

    /**
     *  Get the size of the current spectrum.
     */
    public int size()
    {
        if ( currentSpec != null ) {
            return currentSpec.size();
        }
        return 0;
    }

    //
    // Implement the ComboBoxModel interface.
    //

    public Object getSelectedItem()
    {
        // The current spectrum.
        return currentSpec;
    }

    public void setSelectedItem( Object anItem )
    {
        if ( anItem instanceof SpecData ) {
            setCurrentSpectrum( (SpecData) anItem );
        }
    }

    public Object getElementAt( int index )
    {
        return get( index );
    }

    public int getSize()
    {
        return count();
    }

    protected EventListenerList listenerList = new EventListenerList();

    public void addListDataListener( ListDataListener l )
    {
        listenerList.add( ListDataListener.class, l );
    }

    public void removeListDataListener( ListDataListener l )
    {
        listenerList.remove( ListDataListener.class, l );
    }

    /**
     * Called when a spectrum is added.
     */
    protected void fireListDataAdded( int index )
    {
        Object[] listeners = listenerList.getListenerList();
        ListDataEvent e = null;
        for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
            if ( listeners[i] == ListDataListener.class ) {
                if (e == null) {
                    e = new ListDataEvent( this,
                                           ListDataEvent.INTERVAL_ADDED,
                                           index, index );
                }
                ((ListDataListener)listeners[i+1]).intervalAdded( e );
            }
        }
    }

    /**
     * Called when a spectrum is removed.
     */
    protected void fireListDataRemoved( int index )
    {
        Object[] listeners = listenerList.getListenerList();
        ListDataEvent e = null;
        for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
            if ( listeners[i] == ListDataListener.class ) {
                if (e == null) {
                    e = new ListDataEvent( this,
                                           ListDataEvent.INTERVAL_REMOVED,
                                           index, index );
                }
                ((ListDataListener)listeners[i+1]).intervalRemoved( e );
            }
        }
    }
}
