/*
 * Copyright (C) 2000-2005 Central Laboratory of the Research Councils
 *
 *  History:
 *     21-SEP-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.data;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import uk.ac.starlink.ast.AstException;
import uk.ac.starlink.ast.CmpFrame;
import uk.ac.starlink.ast.FluxFrame;
import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.Grf;
import uk.ac.starlink.ast.Plot;
import uk.ac.starlink.ast.SpecFluxFrame;
import uk.ac.starlink.ast.SpecFrame;
import uk.ac.starlink.splat.ast.ASTJ;
import uk.ac.starlink.splat.util.SplatException;

/**
 * This class is designed to handle multiple instances of SpecData
 * objects. Thus creating an apparent "composite" spectrum, from possibly
 * several others.
 * <p>
 * This feature is intended, for instance, to allow the display multiple
 * spectra in a single DivaPlot and should be used when referencing spectral
 * data.
 * <p>
 * One of the spectra (initially the first) has special status and is known as
 * the current spectrum. This defines the coordinate system that all other
 * spectra should honour. It also is used in constructs such as the compound
 * name.
 * <p>
 * There are sophisticated mechanisms in place for aligning the spectral
 * coordinates and data units of the spectra. By default it is assumed that
 * all spectra have the same units, but otherwise they can be transformed
 * into the coordinates and data units of the current spectrum before being
 * plotted (this uses the AST spectral coordinates frame SpecFrame and the
 * flux matching FluxFrame, if no FluxFrame is available matching may still be
 * possible using AST active units for dimensionally similar data units).
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
     * that coordinateMatching is also true. Note that this value is only used
     * when the spectra do not have FluxFrames describing their data
     * coordinates.
     */
    private boolean dataUnitsMatching = false;

    /**
     * Whether we are to include spacing for error bars in the automatic
     * ranging.
     */
    private boolean errorbarAutoRanging = false;

    /**
     * List of references to the spectra. Note that indexing this
     * list isn't fixed, i.e. removing SpecData objects reshuffles
     * the indices.
     */
    protected ArrayList spectra = new ArrayList();

    /**
     * List of references to the FrameSets that define the mappings between
     * the various spectral and data coordinate systems of the spectra. The
     * indices of these are the SpecData objects.
     */
    protected Map mappings = new HashMap();

    /**
     * Whether to re-generate all the mappings for converting to the
     * coordinates of the current spectrum.
     */
    private boolean regenerateMappings = true;

    /**
     * Whether line identifier spectra should track the position of the
     * current spectrum.
     */
    private boolean trackerLineIDs = false;

    /**
     * Whether line identifier spectra should prefix the short name to their
     * labels.
     */
    private boolean prefixLineIDs = false;

    /**
     * Whether line identifier spectra should show vertical marks.
     */
    private boolean showVerticalMarks = false;

    /**
     * Whether line identifiers should be draw horizontally.
     */
    private boolean drawHorizontalLineIDs = false;

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
        try {
            add( inspec );
        }
        catch (SplatException e) {
            //  Will never occur. First spectrum is not mapped.
            e.printStackTrace();
        }
    }

    /**
     *  Create a SpecDataComp adding in the first from a concrete
     *  implementation.
     */
    public SpecDataComp( SpecDataImpl inspec )
        throws SplatException
    {
        add( new SpecData( inspec ) );
    }

    /**
     * Set whether we're being careful about matching coordinates
     * between spectra. If so then AST will check if any SpecFrames
     * can be converted to preserve units, systems, etc. Switching
     * this on can be slow.
     */
    public void setCoordinateMatching( boolean coordinateMatching )
    {
        //  If coordinate matching state is changed we may need to generate
        //  the mappings between the current spectrum and all the others.
        if ( coordinateMatching != this.coordinateMatching ) {
            if ( coordinateMatching ) {
                regenerateMappings = true;
            }
        }
        this.coordinateMatching = coordinateMatching;
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
     * preserve units. If the frame is a FluxFrame, which may be part of a
     * SpecFluxFrame then units matching will be on by default and this value
     * will have no effect (matching will occur whenever coordinate matching
     * is switched on and works).
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
     */
    public void setTrackerLineIDs( boolean trackerLineIDs )
    {
        this.trackerLineIDs = trackerLineIDs;
    }

    /**
     */
    public boolean isTrackerLineIDs()
    {
        return trackerLineIDs;
    }

    /**
     */
    public void setShowVerticalMarks( boolean showVerticalMarks )
    {
        this.showVerticalMarks = showVerticalMarks;
    }

    /**
     */
    public boolean isShowVerticalMarks()
    {
        return showVerticalMarks;
    }

    /**
     */
    public void setPrefixLineIDs( boolean prefixLineIDs )
    {
        this.prefixLineIDs = prefixLineIDs;
    }

    /**
     */
    public boolean isPrefixLineIDs()
    {
        return prefixLineIDs;
    }

    /**
     */
    public void setDrawHorizontalLineIDs( boolean drawHorizontalLineIDs )
    {
        this.drawHorizontalLineIDs = drawHorizontalLineIDs;
    }

    /**
     */
    public boolean isDrawHorizontalLineIDs()
    {
        return drawHorizontalLineIDs;
    }

    /**
     * Override default behaviour for regenerating all the inter-spectrum
     * transformations. You will need to do this when a spectrum has been
     * modified.
     */
    public void regenerate()
    {
        regenerateMappings = true;
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
                try {
                    add( spectrum );
                }
                catch (SplatException e) {
                    //  Should never fail as current spectrum is not mapped.
                    e.printStackTrace();
                }
            }
            regenerateMappings  = true;
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
     * When a spectrum is removed, call this to make sure that it isn't the
     * current spectrum. If it is the current spectrum resets to the top of
     * the list or null if the list is empty.
     */
    protected void checkCurrentSpectrumRemoved( SpecData inspec )
    {
        if ( inspec.equals( currentSpec ) ) {
            lastCurrentSpec = currentSpec;
            if ( spectra.size() > 0 ) {
                currentSpec = (SpecData) spectra.get( 0 );
            }
            else {
                currentSpec = null;
            }
            regenerateMappings = true;
        }
    }

    /**
     *  Add a spectrum to the managed list.
     *
     *  @param inspec reference to a SpecData object that is to be
     *                added to the composite
     */
    public void add( SpecData inspec )
        throws SplatException
    {
        //  NOTE: This method may be used by TOPCAT classes.
        if ( currentSpec == null ) {
            // First spectrum is current.
            currentSpec = inspec;
            regenerateMappings = true;
        }
        spectra.add( inspec );
        if ( ! regenerateMappings ) {
            generateMapping( inspec );
        }
        fireListDataAdded( spectra.indexOf( inspec ) );
    }

    /**
     *  Add a list of spectra to the managed list.
     *
     *  @param inspec reference to the SpecData objects that are to be
     *                added to the composite
     */
    public void add( SpecData inspec[] )
        throws SplatException
    {
        if ( currentSpec == null ) {
            // First spectrum is current.
            currentSpec = inspec[0];
            regenerateMappings = true;
        }
        int lower = 0;
        int higher = 0;
        int index = 0;
        int failed = 0;
        for ( int i = 0; i < inspec.length; i++ ) {
            spectra.add( inspec[i] );
            index = spectra.indexOf( inspec[i] );
            lower = ( lower < index ) ? lower : index;
            higher = ( higher > index ) ? higher : index;
            if ( ! regenerateMappings ) {
                try {
                    generateMapping( inspec[i] );
                }
                catch (SplatException e) {
                    failed++;
                }
            }
        }
        fireListDataAdded( lower, higher );
        if ( failed != 0 ) {
            throw new SplatException( "Failed to align the coordinate " +
                                      "systems of " + failed + " spectra" );
        }
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
        mappings.remove( inspec );
        checkCurrentSpectrumRemoved( inspec );
	if ( index != -1 ) {
	    fireListDataRemoved( index );
	}
    }

    /**
     *  Remove a list of spectra.
     *
     *  @param inspec references to the spectra to remove.
     */
    public void remove( SpecData inspec[] )
    {
        int indices[] = new int[inspec.length];

        for ( int i = 0; i < inspec.length; i++ ) {
            indices[i] = spectra.indexOf( inspec[i] );
            spectra.remove( inspec[i] );
            mappings.remove( inspec[i] );
            checkCurrentSpectrumRemoved( inspec[i] );
        }
        int lower = 0;
        int upper = 0;
        for ( int i = 0; i < inspec.length; i++ ) {
            if ( indices[i] != -1 ) {
                lower = ( lower < indices[i] ) ? lower : indices[i];
                upper = ( upper > indices[i] ) ? upper : indices[i];
            }
        }
        if ( lower != -1 && upper != -1 ) {
            fireListDataChanged( lower, upper );
        }
        else if ( lower != -1 && upper == -1 ) {
	    fireListDataRemoved( lower );
        }
        else if ( lower == -1 && upper != -1 ) {
	    fireListDataRemoved( upper );
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
     *  system. This always returns the ASTJ object of the current spectrum,
     *  so all other spectra must have a context that is valid within the
     *  coordinate system defined by it.
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
     *  Get the data ranges of all the spectra
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

        regenerateMappings();

        int count = spectra.size();
        SpecData spectrum = null;
        double[] newrange = null;
        double[] xEndPoints;
        double[] yEndPoints;
        FrameSet mapping = null;

        for ( int i = 0; i < count; i++ ) {
            spectrum = (SpecData) spectra.get( i );
            if ( ! spectrum.equals( currentSpec ) ) {
                if ( coordinateMatching ) {
                    xEndPoints = spectrum.getXEndPoints();
                    yEndPoints = spectrum.getYEndPoints();
                    mapping = (FrameSet) mappings.get( spectrum );
                    newrange = transformEndPoints( mapping, xEndPoints,
                                                   yEndPoints );
                }
                else {
                    newrange = spectrum.getRange();
                }
                checkRangeLimits( newrange, range );
            }
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
     * Get the range of a spectrum. Includes space for errorbars if they are
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

        regenerateMappings();

        int count = spectra.size();
        SpecData spectrum = null;
        double[] newrange = null;
        double[] xEndPoints;
        double[] yEndPoints;
        FrameSet mapping = null;

        for ( int i = 0; i < count; i++ ) {
            spectrum = (SpecData) spectra.get( i );
            if ( ! spectrum.equals( currentSpec ) ) {
                if ( coordinateMatching ) {
                    if ( spectrum.isDrawErrorBars() && errorbarAutoRanging ) {
                        xEndPoints = spectrum.getXFullEndPoints();
                        yEndPoints = spectrum.getYFullEndPoints();
                    }
                    else {
                        xEndPoints = spectrum.getXEndPoints();
                        yEndPoints = spectrum.getYEndPoints();
                    }
                    mapping = (FrameSet) mappings.get( spectrum );
                    newrange = transformEndPoints( mapping, xEndPoints,
                                                   yEndPoints );
                }
                else {
                    newrange = getSpectrumRange( spectrum );
                }
                checkRangeLimits( newrange, range );
            }
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

        regenerateMappings();

        int count = spectra.size();
        double newrange[] = null;
        double[] xEndPoints;
        double[] yEndPoints;
        SpecData spectrum = null;
        FrameSet mapping;

        for ( int i = 0; i < count; i++ ) {
            spectrum = (SpecData)spectra.get( i );
            if ( ! spectrum.equals( currentSpec ) ) {
                if ( spectrum.isUseInAutoRanging() ) {
                    if ( coordinateMatching ) {
                        if ( spectrum.isDrawErrorBars() &&
                             errorbarAutoRanging ) {
                            xEndPoints = spectrum.getXFullEndPoints();
                            yEndPoints = spectrum.getYFullEndPoints();
                        }
                        else {
                            xEndPoints = spectrum.getXEndPoints();
                            yEndPoints = spectrum.getYEndPoints();
                        }
                        mapping = (FrameSet) mappings.get( spectrum );
                        newrange = transformEndPoints( mapping, xEndPoints,
                                                       yEndPoints );
                    }
                    else {
                        newrange = getSpectrumRange( spectrum );
                    }
                    checkRangeLimits( newrange, range );
                }
            }
        }
        return range;
    }

    /**
     * Transform a range of position from the coordinates of a given spectrum
     * to those of the current spectrum. The spectrum must be a member of this
     * object.
     */
    public double[] transformRange( SpecData referenceSpec, double[] range )
    {
        return transformRange( (FrameSet) mappings.get( referenceSpec ),
                               range );
    }

    /**
     * Transform range-like position-pairs using a given mapping.
     *
     * The input and output coordinates are [x1,x2,y1,y2,...].
     */
    protected double[] transformRange( FrameSet mapping, double[] range )
    {
        if ( range == null ) return null;

        double[] result = range;

        //  2D coords, so need separate X,Y coords.
        double xin[] = new double[range.length/2];
        double yin[] = new double[xin.length];
        for ( int i = 0; i < xin.length; i++ ) {
            xin[i] = range[i];
            yin[i] = range[i+xin.length];
        }
        double[][] tmp = mapping.tran2( xin.length, xin, yin, false );

        // Put back to vectorized array.
        result = new double[range.length];
        for ( int i = 0; i < xin.length; i++ ) {
            result[i] = tmp[0][i];
            result[i+xin.length] = tmp[1][i];
        }
        return result;
    }

    /**
     * Transform the end-points of a spectrum to determine a new range. This
     * makes sure that the mapping is applied to real points on a spectrum not
     * a bounding range. When dealing with SpecFluxFrames this matters.
     *
     * The input coordinates are two sets of [x1,y1,x2,y2,x3,y3,x4,y4] arrays.
     */
    protected double[] transformEndPoints( FrameSet mapping,
                                           double[] xEndPoints,
                                           double[] yEndPoints )
    {
        if ( xEndPoints == null || yEndPoints == null ) return null;

        //  2D coords, so need separate X,Y coords.
        double xin[] = new double[xEndPoints.length];
        double yin[] = new double[xin.length];
        for ( int i = 0, j = 0; i < xin.length; i++, j++ ) {
            xin[i] = xEndPoints[j];
            yin[i] = xEndPoints[j+1];
            i++;
            xin[i] = yEndPoints[j];
            yin[i] = yEndPoints[j+1];
            j++;
        }
        double[][] tmp = mapping.tran2( xin.length, xin, yin, false );

        // Pick out the min/max pairs.
        double[] result = new double[4];
        result[0] = Double.MAX_VALUE;
        result[1] = -Double.MAX_VALUE;
        result[2] = Double.MAX_VALUE;
        result[3] = -Double.MAX_VALUE;

        for ( int i = 0; i < xin.length; i++ ) {
            result[0] = Math.min( result[0], tmp[0][i] );
            result[1] = Math.max( result[1], tmp[0][i] );
            result[2] = Math.min( result[2], tmp[1][i] );
            result[3] = Math.max( result[3], tmp[1][i] );
        }
        return result;
    }

    /**
     * Transform position-pairs (usually limits) using a given mapping.
     *
     * The input and output coordinates are [x1,y1,x2,y2,...].
     */
    public double[] transformLimits( FrameSet mapping, double[] limits,
                                     boolean forward )
        throws SplatException
    {
        if ( limits == null ) return null;

        double[] result = limits;

        //  2D coords, so need separate X,Y coords.
        double xin[] = new double[limits.length/2];
        double yin[] = new double[xin.length];

        for ( int i = 0, j = 0; i < xin.length; i++, j+=2 ) {
            xin[i] = limits[j];
            yin[i] = limits[j+1];
        }

        double[][] tmp = mapping.tran2( xin.length, xin, yin, forward );

        // Put back to vectorized array.
        boolean bad = false;
        result = new double[limits.length];
        for ( int i = 0, j = 0; i < xin.length; i++, j+=2 ) {
            result[j] = tmp[0][i];
            result[j+1] = tmp[1][i];
        }
        return result;
    }

    /**
     *  Draw all spectra using the graphics context provided.
     *
     *  @param grf AST graphics context
     *  @param plot AST plot
     *  @param clipLimits limits of area being drawn in world coordinates,
     *                    when clipping is required. Set to null when no
     *                    clipping applies.
     *  @param fullLimits limits of the whole graphics component in world
     *                    coordinates.
     */
    public void drawSpec( Grf grf, Plot plot, double[] clipLimits,
                          double[] fullLimits )
        throws SplatException
    {
        if ( spectra.size() == 0 ) {
            return;
        }
        Plot localPlot = plot;

        //  Transform limits into graphics coordinates, if possible. These
        //  apply to all spectra.
        boolean physical = false;
        double[] localClipLimits = transformLimits( plot, clipLimits, false );
        if ( localClipLimits == null ) {
            //  No graphics limits, assume given limits are valid and physical.
            localClipLimits = clipLimits;
            physical = true;
        }

        //  Also transform fullLimits into graphics coordinates.
        double[] localFullLimits = transformLimits( plot, fullLimits, false );

        SpecData spectrum = null;
        FrameSet mapping = null;

        regenerateMappings();

        for ( int i = 0; i < spectra.size(); i++ ) {
            spectrum = (SpecData)spectra.get( i );
            if ( coordinateMatching ) {
                if ( ! spectrum.equals( currentSpec ) ) {
                    //  The coordinates systems and, optionally, data units of
                    //  the spectra need to be matched so that they are drawn
                    //  to the correct scale on the Plot of the current
                    //  spectrum.
                    mapping = (FrameSet) mappings.get( spectrum );
                    localPlot = alignPlots( plot, mapping );
                }
                else {
                    localPlot = plot;
                }
            }
            if ( spectrum instanceof LineIDSpecData ) {
                LineIDSpecData lineSpec = (LineIDSpecData) spectrum;
                if ( trackerLineIDs ) {
                    lineSpec.setSpecData( currentSpec, mapping );
                }
                else {
                    lineSpec.setSpecData( null, null );
                }
                lineSpec.setPrefixShortName( prefixLineIDs );
                lineSpec.setShowVerticalMarks( showVerticalMarks );
                lineSpec.setDrawHorizontal( drawHorizontalLineIDs );
            }
            spectrum.drawSpec( grf, localPlot, localClipLimits, physical,
                               localFullLimits );
        }
    }

    /**
     * When necessary re-generate the Mappings that match from the current
     * spectrum to all the other spectra.
     */
    protected void regenerateMappings()
        throws SplatException
    {
        if ( coordinateMatching && regenerateMappings && ( spectra.size() > 1 ) ) {
            SpecData spectrum = null;
            for ( int i = 0; i < spectra.size(); i++ ) {
                spectrum = (SpecData)spectra.get( i );
                generateMapping( spectrum );
            }
            regenerateMappings = false;
        }
    }

    protected void generateMapping( SpecData spectrum )
        throws SplatException
    {
        if ( coordinateMatching && ( ! spectrum.equals( currentSpec ) ) ) {

            //  Get the current frames of the current spectrum and the target
            //  one.
            Frame to =
                currentSpec.getAst().getRef().getFrame(FrameSet.AST__CURRENT);
            Frame from =
                spectrum.getAst().getRef().getFrame(FrameSet.AST__CURRENT);

            //  Determine if the current spectrum has a SpecFrame. This will
            //  be the first axis for any DATAPLOT.
            int iaxes[] = new int[1];
            iaxes[0] = 1;
            Frame picked = to.pickAxes( 1, iaxes, null );
            boolean haveSpecFrame = ( picked instanceof SpecFrame );

            // If spectrum is a line id then we should attempt to transform it
            // into the system of main spectrum (this aligns if a source
            // velocity is set).
            FrameSet mapping = null;
            boolean sourceTransform = false;
            if ( spectrum instanceof LineIDSpecData && haveSpecFrame ) {
                sourceTransform = true;

                // Cannot be sure to match data units as line identifiers do
                // not generally have these. When they are unset the data
                // units will have value "unknown" or have no data positions,
                // in those cases set the data units to those of the current
                // spectrum.
                boolean haveDataPositions =
                    ((LineIDSpecData) spectrum).haveDataPositions();

                String dataUnits = from.getC( "unit(2)" );
                if ( dataUnits == null || dataUnits.equals( "unknown" ) ) {
                    haveDataPositions = false;
                }

                if ( ! haveDataPositions ) {
                    //  Use units Frame of the current spectrum along with the
                    //  SpecFrame of the line identifiers.
                    Frame fromSpecFrame  = from.pickAxes( 1, iaxes, null );
                    iaxes[0] = 2;
                    Frame toUnitFrame = to.pickAxes( 1, iaxes, null );

                    //  Make the right kind of Frame. SpecFluxFrames will not
                    //  match any other type of Frame.
                    if ( to instanceof SpecFluxFrame ) {
                        from = new SpecFluxFrame( (SpecFrame) fromSpecFrame,
                                                  (FluxFrame) toUnitFrame );
                    }
                    else {
                        from = new CmpFrame( fromSpecFrame, toUnitFrame );
                    }
                }
            }

            //  If not using a FluxFrame in a SpecFluxFrame we can still match
            //  units that are dimensionally the same.
            if ( dataUnitsMatching ) {
                from.setActiveUnit( true );
            }

            //  Transform to source rest frame for matching against line ids.
            if ( sourceTransform ) {
                String stdofrest = to.getC( "StdOfRest(1)" );
                to.set( "StdOfRest(1)=Source" );
                mapping = to.convert( from, "DATAPLOT" );
                to.set( "StdOfRest(1)=" + stdofrest );
            }
            else {
                mapping = to.convert( from, "DATAPLOT" );
            }

            if ( dataUnitsMatching ) {
                from.setActiveUnit( false );
            }

            if ( mapping == null ) {
                throw new SplatException( "Failed to align coordinates of " +
                                          currentSpec.getShortName() +
                                          " and " +
                                          spectrum.getShortName() );
            }

            //  Associate mapping with the spectrum.
            mappings.put( spectrum, mapping );
        }
    }


    /**
     * Modify a plot so that it uses a different set of current coordinates as
     * current. The coordinate systems are aligned using the given mapping
     * which should map from the coordinates of one spectrum to those of
     * another.
     */
    public Plot alignPlots( Plot plot, FrameSet mapping )
    {
        Plot result = (Plot) plot.copy();
        result.addFrame( FrameSet.AST__CURRENT, mapping, mapping );
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

    // Implement the ListModel part of ComboBoxModel.

    protected EventListenerList listenerList = new EventListenerList();

    public void addListDataListener( ListDataListener l )
    {
        listenerList.add( ListDataListener.class, l );
    }

    public Object getElementAt( int index )
    {
        return get( index );
    }

    public int getSize()
    {
        return count();
    }


    // Implement handling of ListDataListeners.

    public void removeListDataListener( ListDataListener l )
    {
        listenerList.remove( ListDataListener.class, l );
    }

    /**
     * Called when a spectrum is added.
     */
    protected void fireListDataAdded( int index )
    {
        fireListDataAdded( index, index );
    }

    /**
     * Called when a list of spectra are added.
     */
    protected void fireListDataAdded( int index1, int index2 )
    {
        Object[] listeners = listenerList.getListenerList();
        ListDataEvent e = null;
        for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
            if ( listeners[i] == ListDataListener.class ) {
                if (e == null) {
                    e = new ListDataEvent( this,
                                           ListDataEvent.INTERVAL_ADDED,
                                           index1, index2 );
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

    /**
     * Called when more than one spectrum is changed. This this case the
     * indices bracket the change.
     */
    protected void fireListDataChanged( int index1, int index2 )
    {
        Object[] listeners = listenerList.getListenerList();
        ListDataEvent e = null;
        for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
            if ( listeners[i] == ListDataListener.class ) {
                if (e == null) {
                    e = new ListDataEvent( this,
                                           ListDataEvent.CONTENTS_CHANGED,
                                           index1, index2 );
                }
                ((ListDataListener)listeners[i+1]).contentsChanged( e );
            }
        }
    }
}
