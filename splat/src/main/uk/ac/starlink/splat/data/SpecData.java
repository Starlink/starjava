/*
 * Copyright (C) 2002-2004 Central Laboratory of the Research Councils
 * Copyright (C) 2007 Particle Physics and Astronomy Research Council
 * Copyright (C) 2007-2009 Science and Technology Facilities Council
 *
 *  History:
 *     01-SEP-2002 (Peter W. Draper):
 *        Original version.
 *     26-FEB-2004 (Peter W. Draper):
 *        Added column name methods.
 */
package uk.ac.starlink.splat.data;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.logging.Logger;

import nom.tam.fits.Header;

import uk.ac.starlink.ast.AstException;
import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.Grf;
import uk.ac.starlink.ast.Mapping;
import uk.ac.starlink.ast.Plot;
import uk.ac.starlink.ast.grf.DefaultGrf;
import uk.ac.starlink.ast.grf.DefaultGrfMarker;
import uk.ac.starlink.ast.grf.DefaultGrfState;
import uk.ac.starlink.diva.interp.LinearInterp;
import uk.ac.starlink.splat.ast.ASTChannel;
import uk.ac.starlink.splat.ast.ASTJ;
import uk.ac.starlink.splat.util.Sort;
import uk.ac.starlink.splat.util.SplatException;

//  IMPORT NOTE: modifying the member variables could change the
//  serialization signature of this class. If really need to then
//  think about providing a backwards compatibility mechanism with
//  the Serializable API (this should be possible since all
//  serializable classes have a serialVersionUID).

/**
 * SpecData defines an interface for general access to spectral datasets of
 * differing fundamental data types and represents the main data model used
 * in SPLAT.
 * <p>
 *
 * It uses a derived class of SpecDataImpl to a supported data format (i.e.
 * FITS, NDF and text files etc.) to give generalised access to:
 * <ul>
 *   <li> the spectrum data
 *   <li> the associated data errors
 *   <li> the coordinate of any data point
 *   <li> the spectrum properties (i.e. related values)
 * </ul>
 * <p>
 *
 * It should always be used when dealing with spectral data to avoid any
 * specialised knowledge of the data format. <p>
 *
 * Missing data, or gaps in the spectrum, are indicated using the special
 * value SpecData.BAD. Generally useful code should always test for this in
 * the data values (otherwise you'll see numeric problems as BAD is the lowest
 * possible double value). <p>
 *
 * Matching of data values between this spectrum and anothers coordinates can
 * currently be done using the evalYData and evalYDataArray method of the
 * AnalyticSpectrum interface (but note that at present this uses a simple
 * interpolation scheme, so shouldn't be used for analysis, except when the
 * interpolated spectrum is an analytic one, such as a polynomial). <p>
 *
 * Each object records a series of properties that define how the spectrum
 * should be rendered (i.e. line colour, thickness, style, plotting style, or
 * marker type and size, plus whether to show any errors as bars etc.). These
 * are stored in any serialized versions of this class. Rendering using the
 * Grf object primitives is performed by this class for spectra and error
 * bars. <p>
 *
 * General utilities for converting coordinates and looking up values are
 * provided, as are methods for specialised functions like formatting and
 * unformatting values. This allows you to avoid understanding what is
 * returned as a value from a user interaction as formatting and unformatting
 * match the units of the spectral axes (which can therefore be in esoteric
 * units, like RA or Dec).
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see SpecDataImpl
 * @see SpecDataFactory
 * @see AnalyticSpectrum
 * @see "The Bridge Design Pattern"
 */
public class SpecData
     implements AnalyticSpectrum, Serializable
{
    // Logger.
    private static Logger logger =
        Logger.getLogger( "uk.ac.starlink.splat.data.SpecData" );

    //  =============
    //  Constructors.
    //  =============

    /**
     * Create an instance using the data in a given SpecDataImpl object.
     *
     * @param impl a concrete implementation of a SpecDataImpl class that is
     *      accessing spectral data in of some format.
     * @exception SplatException thrown if there are problems obtaining
     *      spectrum.
     */
    public SpecData( SpecDataImpl impl )
        throws SplatException
    {
        this( impl, false );
    }

    /**
     * Create an instance using the data in a given SpecDataImpl
     * object.  Do not attempt to read the data if suggested. This variant is
     * provided for sub-classes that will deal with the data at some later
     * time.
     *
     * @param impl a concrete implementation of a SpecDataImpl class that
     *             will be used for spectral data in of some format.
     * @param check if true then a check for the presence of data will be
     *              made, before attempting a read. Otherwise no check will be
     *              made and problems will be indicated by throwing an error
     *              at a later time.
     * @exception SplatException thrown if there are problems obtaining
     *            spectrum information.
     */
    protected SpecData( SpecDataImpl impl, boolean check )
        throws SplatException
    {
        setSpecDataImpl( impl, check );
    }

    /**
     * Set the {@link SpecDataImpl} instance.
     *
     * @param impl a concrete implementation of a SpecDataImpl class that
     *             will be used for spectral data in of some format.
     * @exception SplatException thrown if there are problems obtaining
     *            spectrum information.
     */
    public void setSpecDataImpl( SpecDataImpl impl )
        throws SplatException
    {
        setSpecDataImpl( impl, false );
    }

    /**
     * Set the {@link SpecDataImpl} instance. This is the constructor.
     *
     * @param impl a concrete implementation of a SpecDataImpl class that
     *             will be used for spectral data in of some format.
     * @param check if true then a check for the presence of data will be
     *              made, before attempting a read. Otherwise no check will be
     *              made and problems will be indicated by throwing an error
     *              at a later time.
     * @exception SplatException thrown if there are problems obtaining
     *            spectrum information.
     */
    protected void setSpecDataImpl( SpecDataImpl impl, boolean check )
        throws SplatException
    {
        this.impl = impl;
        fullName = impl.getFullName();
        setShortName( impl.getShortName() );
        if ( ! check || ( check && impl.getData() != null  ) ) {
            readDataPrivate();
        }
    }

    /**
     * Return the SpecDataImpl object so that it can expose very data specific
     * methods (if needed, you really shouldn't use this).
     *
     * @return SpecDataImpl object defining the data access used by this
     *      instance.
     */
    public SpecDataImpl getSpecDataImpl()
    {
        return impl;
    }

    /**
     * Finalise object. Free any resources associated with member variables.
     *
     * @exception Throwable Description of the Exception
     */
    protected void finalize()
        throws Throwable
    {
        this.impl = null;
        this.xPos = null;
        this.yPos = null;
        this.yPosOri = null;
        this.yErr = null;
        this.yErrOri = null;
        this.xPosG = null;
        this.yPosG = null;
        this.astJ = null;
        super.finalize();
    }


    //  ================
    //  Public constants
    //  ================

    /**
     * Value of BAD (missing) data.
     */
    public final static double BAD = -Double.MAX_VALUE;

    //
    //  Plotting property symbolic constants.
    //

    /**
     * Set or query line thickness.
     */
    public final static int LINE_THICKNESS = 0;

    /**
     * Set or query line drawing style.
     */
    public final static int LINE_STYLE = 1;

    /**
     * Set or query line colour.
     */
    public final static int LINE_COLOUR = 2;

    /**
     * Set or query line drawing style.
     */
    public final static int PLOT_STYLE = 3;

    /**
     * Set or query the marker drawing type.
     */
    public final static int POINT_TYPE = 4;

    /**
     * Set or query the marker size.
     */
    public final static int POINT_SIZE = 5;

    /**
     * Set or query alpha composite value.
     */
    public final static int LINE_ALPHA_COMPOSITE = 6;

    /**
     * Set or query error bar colour.
     */
    public final static int ERROR_COLOUR = 7;

    /**
     * Set or query the number of sigma error bars are drawn at.
     */
    public final static int ERROR_NSIGMA = 8;

    /**
     * Set or query the frequency error bars are drawn at.
     */
    public final static int ERROR_FREQUENCY = 9;

    //
    //  Symbolic contants defining the possible plotting styles.
    //

    /**
     * Use polyline plotting style.
     */
    public final static int POLYLINE = 1;

    /**
     * Use histogram plotting style.
     */
    public final static int HISTOGRAM = 2;

    /**
     * Use a point plotting style.
     */
    public final static int POINT = 3;

    //
    //  Types of spectral data. The default is UNCLASSIFIED.
    //  Nothing is done with this information here, it's just for external
    //  tagging. USERTYPE is just a spare.
    //

    /**
     * Spectrum is unclassified.
     */
    public final static int UNCLASSIFIED = 0;

    /**
     * Spectrum is a target (observation).
     */
    public final static int TARGET = 1;

    /**
     * Spectrum is an arc.
     */
    public final static int ARC = 2;

    /**
     * Spectrum is a twilight sky exposure.
     */
    public final static int SKY = 3;

    /**
     * Spectrum is a polynomial.
     */
    public final static int POLYNOMIAL = 4;

    /**
     * Spectrum is a line fit.
     */
    public final static int LINEFIT = 5;

    /**
     * Spectrum is a user defined type.
     */
    public final static int USERTYPE = 6;

    //
    // Limits for the number of digits used in a formatted value.
    //
    public final static int MAX_DIGITS = 17; // Perfect IEEE double precision
    public final static int MIN_DIGITS = 7;  // AST uses 7

    /**
     * Serialization version ID string (generated by serialver on original
     * star.jspec.data.SpecData class).
     */
    final static long serialVersionUID = 1719961247707612529L;

    //  ===================
    //  Protected variables
    //  ===================

    /**
     * Reference to the data access adaptor. This object is not kept during
     * serialisation as all restored objects use a memory implementation (disk
     * file associations are difficult to maintain).
     */
    protected transient SpecDataImpl impl = null;

    /**
     * The X data values for the spectrum.
     */
    protected double[] xPos = null;

    /**
     * The Y data values for the spectrum.
     */
    protected double[] yPos = null;
    protected double[] yPosOri = null;

    /**
     * The Y data errors for the spectrum.
     */
    protected double[] yErr = null;
    protected double[] yErrOri = null;

    /**
     * The X positions transformed into graphics coordinates.
     * 
     * Available after first drawSpec() call.
     */
    protected double[] xPosG = null;
    
    /**
     * The Y positions transformed into graphics coordinates.
     * 
     * Available after first drawSpec() call.
     */
    protected double[] yPosG = null;
    
    /**
     * Symbolic name of the spectrum.
     */
    protected String shortName = null;

    /**
     * Full name of the spectrum.
     */
    protected String fullName = null;

    /**
     * Whether symbolic names should be truncated when equal to the full name.
     * Shared by all instances.
     */
    protected static boolean simplifyShortNames = false;

    /**
     * The range of coordinates spanned (min/max values in xPos and yPos).
     */
    protected double[] range = new double[4];

    /**
     * The full range of coordinates spanned (i.e.&nbsp;min/max values
     * in xPos and yPos, plus the standard deviations * nsigma in yPos).
     */
    protected double[] fullRange = new double[4];

    /**
     * The coordinates and data values of the points used to define the
     * range. These may be needed when a transformation of the region that the
     * spectrum is drawn into is performed (the actual points are required for
     * potentially non-linear transformations, like flux when it depends on
     * wavelength).
     */
    protected double[] xEndPoints = new double[4];
    protected double[] yEndPoints = new double[4];

    /**
     * The coordinates and data values of the points used to define the
     * full range. These may be needed when a transformation of the region
     * that the spectrum is drawn into is performed (the actual points are
     * required for potentially non-linear transformations, like flux when it
     * depends on wavelength). Note that the X versions are same as xEndPoints.
     */
    protected double[] yFullEndPoints = new double[4];

    /**
     * Reference to ASTJ object that contains an AST FrameSet that wraps
     * the spectrum FrameSet so that it can be plotted (i.e.&nbsp;makes it
     * suitably two dimensional). Marked "transient" it should always be
     * re-generated when a spectrum is loaded.
     */
    protected transient ASTJ astJ = null;

    /**
     * The "graphics" line thickness.
     */
    protected double lineThickness = 1.0;

    /**
     * The "graphics" line style.
     */
    protected double lineStyle = 1.0;

    /**
     * The "graphics" line alpha composite value (SRC_OVER).
     */
    protected double alphaComposite = 1.0;

    /**
     * The "graphics" line colour.
     */
    protected double lineColour = (double) Color.blue.getRGB();

    /**
     * The colour used to drawn error bars.
     */
    protected double errorColour = (double) Color.red.getRGB();

    /**
     * The number of sigma any error bars are drawn at.
     */
    protected int errorNSigma = 1;

    /**
     * The frequency (that is how often) error bars are drawn.
     */
    protected int errorFrequency = 1;

    /**
     * The spectrum plot style.
     */
    protected int plotStyle = POLYLINE; //and146: set this to POINT

    /**
     * Whether error bars should be drawn.
     */
    protected boolean drawErrorBars = false;

    /**
     * Whether this spectrum should be used when auto-ranging (certain classes
     * of spectra, i.e.<!-- --> generated ones, could be expected to have artificial
     * ranges).
     */
    protected boolean useInAutoRanging = true;

    /**
     * Slack added to full data range to stop error bar serifs from just
     * running along axes.
     */
    private final static double SLACK = 0.02;

    /**
     * Half length of error bar serifs.
     */
    private final static double SERIF_LENGTH = 2.5;

    /**
     * The type of data stored in this spectrum. This is purely symbolic and
     * is meant for use in organisational duties.
     */
    protected int type = UNCLASSIFIED;

    /**
     * Whether the spectrum coordinates are monotonic.
     */
    protected boolean monotonic = true;

    /**
     * The type of point that is drawn.
     */
    protected int pointType = DefaultGrfMarker.DOT;

    /**
     * The size of any points.
     */
    protected double pointSize = 5.0;

    /**
     * Serialized form of the backing implementation AST FrameSet. This is
     * only used when the object is serialized itself and cannot be relied on
     * at any time.
     */
    protected String[] serializedFrameSet = null;

    /**
     * Data units and label for storage in serialized form. These are only
     * used when the object is serialized and cannot be used for any other
     * purpose.
     */
    protected String serializedDataUnits = null;
    protected String serializedDataLabel = null;

    /**
     * Most significant axis being used when the spectrum was serialised.
     * This will continue to be used when deserialised when the original
     * data axes has been lost.
     */
    protected int serializedSigAxis = 0;

    /**
     * The apparent data units. If possible conversion from the actual data
     * units to these values will occur (this is achieved by transforming the
     * data values using the FluxFrame part of the spectral AST FrameSet). If
     * not possible then these will be ignored.
     */
    protected String apparentDataUnits = null;

    /**
     * Whether to search for spectral coordinate frames when creating the plot
     * FrameSets. If true then a SpecFrame anywhere in the original WCS of an
     * instance will be used, or if possible a spectral coordinate system will
     * be deduced from any labelling and units. If false SpecFrames will only
     * be used it they are in the current coordinate system. Applies to the
     * whole application.
     */
    protected static boolean searchForSpecFrames = true;

    /**
     * An offset to apply to the Y physical coordinates so that spectra
     * can be displayed offset from one another.
     */
    private double yoffset = 0.0;

    /**
     * Whether the Y offsets to coordinates should be applied.
     */
    private boolean applyYOffset = false;
    
    //  ==============
    //  Public methods
    //  ==============

    /**
     * Get the full name for spectrum (cannot edit, this is usually the
     * filename).
     *
     * @return the full name.
     */
    public String getFullName()
    {
        return fullName;
    }


    /**
     * Get a symbolic name for spectrum. This will be "simplified" if
     * {@link simplyShortNames} is currently true and the symbolic name is the
     * same as the full name (usually the disk file name).
     * <p>
     * Simplication means removing all but the last part of the path (parent
     * directory) and the filename.
     *
     * @return the short name.
     */
    public String getShortName()
    {
        String sName = shortName;
        if ( simplifyShortNames && sName.equals( fullName ) ) {
            File file = new File( sName );
            File par = file.getParentFile();
            if ( par != null ) {
                String parPar = par.getParent();
                if ( parPar != null ) {
                    //  Short name has a fuller path than we want remove this.
                    sName = sName.substring( parPar.length() + 1 );
                }
            }
        }
        return sName;
    }

    /**
     * Change the symbolic name of a spectrum.
     *
     * @param shortName new short name for the spectrum.
     */
    public void setShortName( String shortName )
    {
        this.shortName = shortName;
    }

    /**
     * Set whether to simplify all short names when they are requested.
     * Applies to all instances of SpecData.
     *
     * @param simplify whether to simplify short names.
     */
    public static void setSimplifiedShortNames( boolean simplify )
    {
        simplifyShortNames = simplify;
    }

    /**
     * Get whether we're simplifying all short names when they are requested.
     * Applies to all instances of SpecData.
     *
     * @return the current state
     */
    public static boolean isSimplifiedShortNames()
    {
        return simplifyShortNames;
    }

    /**
     * Get references to spectrum X data (the coordinates as a single array).
     *
     * @return reference to spectrum X data.
     */
    public double[] getXData()
    {
        return xPos;
    }


    /**
     * Get references to spectrum Y data (the data values).
     *
     * @return reference to spectrum Y data.
     */
    public double[] getYData()
    {
        return yPos;
    }


    /**
     * Get references to spectrum Y data errors (the data errors).
     *
     * @return reference to spectrum Y data errors.
     */
    public double[] getYDataErrors()
    {
        return yErr;
    }
    
    /**
     * Get references to spectrum X graphics coordinates
     * 
     * @return reference to spectrum X graphics coordinates
     */
    public double[] getXGraphicsCoordinates() {
    	return xPosG;
    }
    
    /**
     * Get references to spectrum Y graphics coordinates
     * 
     * @return reference to spectrum Y graphics coordinates
     */
    public double[] getYGraphicsCoordinates() {
    	return yPosG;
    }


    /**
     * Return if data errors are available.
     *
     * @return true if Y data has errors.
     */
    public boolean haveYDataErrors()
    {
        return ( yErr != null );
    }

    /**
     * Swap the data and the data errors, so that the spectral data are the
     * errors. This can be used to plot the errors as a line rather than as
     * errorbars. Does nothing there are no errors. Call again to undo the
     * effect. Note does not affect the underlying implementation, so
     * refreshing from that will also reset this.
     */
    public void swapDataAndErrors()
    {
        if ( yErr != null ) {
            double tmp[] = yPos;
            yPos = yErr;
            yErr = tmp;

            tmp = yPosOri;
            yPosOri = yErrOri;
            yErrOri = tmp;

            //  Reset ranges.
            setRangePrivate();
        }
    }


    /**
     * Set the data units of the underlying representation. This will not
     * cause any modification of the data value themselves.
     *
     * @param dataUnits data units string in AST format.
     */
    public void setDataUnits( String dataUnits )
    {
        impl.setDataUnits( dataUnits );
    }

    /**
     * Get the underlying data units. This should be set to "unknown" when
     * unknown.
     */
    public String getDataUnits()
    {
        return impl.getDataUnits();
    }

    /**
     * Get the current data units. These will be the apparent data units when
     * they are in force, otherwise they will be the underlying data units.
     */
    public String getCurrentDataUnits()
    {
        if ( apparentDataUnits == null ) {
            return impl.getDataUnits();
        }
        return apparentDataUnits;
    }

    /**
     * Set the data label.
     *
     * @param dataLabel label describing the data values.
     */
    public void setDataLabel( String dataLabel )
    {
        impl.setDataLabel( dataLabel );
    }

    /**
     * Get the data label. This should be set to "data values" when unknown.
     */
    public String getDataLabel()
    {
        return impl.getDataLabel();
    }

    /**
     * Set the apparent data units. If possible the spectral FrameSet will
     * present the data values in these units. This usually requires a
     * FluxFrame, i.e. the underlying data must have recognised data units.
     * <p>
     * These units will not be used until the next regeneration of the
     * spectral FrameSet. A null value will return to the original units.
     *
     * @param dataUnits apparent data units string in AST format.
     */
    public void setApparentDataUnits( String dataUnits )
    {
        apparentDataUnits = dataUnits;
    }

    /**
     * Get the apparent data units currently in use. These are null then they
     * have not been set, or have been found invalid.
     */
    public String getApparentDataUnits()
    {
        return apparentDataUnits;
    }

    /**
     * Set the offset used to stack this spectra. A number in the physical
     * coordinates.
     */
    public void setYOffset( double yoffset )
    {
        this.yoffset = yoffset;
    }

    /**
     * Get stacking offset.
     */
    public double getYOffset()
    {
        return yoffset;
    }

    /**
     * Set whether to use the stacking offset.
     */
    public void setApplyYOffset( boolean applyYOffset  )
    {
        this.applyYOffset = applyYOffset;
    }

    /**
     * Get whether we're using the stacking offset.
     */
    public boolean isApplyYOffset()
    {
        return applyYOffset;
    }

    /**
     * Return a keyed value from the FITS headers. Returns "" if not
     * found. Standard properties are made available with specific methods
     * ({@link #getDataLabel} etc.), which should be used to access those.
     */
    public String getProperty( String property )
    {
        return impl.getProperty( property );
    }

    /**
     * Return the FITS headers as a whole, if any are available.
     */
    public Header getHeaders()
    {
        if ( impl instanceof FITSHeaderSource ) {
            return ((FITSHeaderSource)impl).getFitsHeaders();
        }
        return null;
    }

    /**
     * Save the spectrum to disk (if supported). Uses the current state of the
     * spectrum (i.e. any file names etc.) to decide how to do this.
     *
     * @exception SplatException thown if an error occurs during save.
     */
    public void save()
        throws SplatException
    {
        try {
            impl.save();
        }
        catch (Exception e) {
            throw new SplatException( "Failed to save spectrum", e );
        }
    }

    /**
     * Create a new spectrum by extracting sections of this spectrum. The
     * section extents are defined in physical coordinates. Each section will
     * not contain any values that lie outside of its physical coordinate
     * range. <p>
     *
     * The spectrum created here is not added to any lists or created with any
     * configuration other than the default values (i.e. you must do this part
     * yourself) and is only kept in memory.
     *
     * @param name short name for the spectrum.
     * @param ranges an array of pairs of physical coordinates. These define
     *      the extents of the ranges to extract.
     * @return a spectrum that contains data only from the given ranges. May
     *      be null if no values can be located.
     */
    public SpecData getSect( String name, double[] ranges )
    {
        //  Locate the index of the positions just below and above our
        //  physical value pairs and count the number of values that
        //  will be extracted.
        int nRanges = ranges.length / 2;
        int[] lower = new int[nRanges];
        int[] upper = new int[nRanges];
        int[] low;
        int[] high;
        int nvals = 0;
        for ( int i = 0, j = 0; j < nRanges; i += 2, j++ ) {
            low = bound( ranges[i] );
            lower[j] = low[1];
            high = bound( ranges[i + 1] );
            upper[j] = high[0];
            nvals += high[0] - low[1] + 1;
        }

        if ( nvals < 0 ) {
            //  Coordinates run in a reversed direction.
            nvals = Math.abs( nvals ) + 2 * nRanges;
            high = lower;
            lower = upper;
            upper = high;
        }

        //  Equal sets of bounds give no extraction.
        if ( nvals == 0 ) {
            return null;
        }

        //  Add room for any gaps.
        nvals += nRanges - 1;

        //  Copy extracted values.
        double[] newCoords = new double[nvals];
        double[] newData = new double[nvals];
        int k = 0;
        int length = 0;
        for ( int j = 0; j < nRanges; j++ ) {
            if ( j != 0 ) {

                //  Need a gap in the spectrum.
                newCoords[k] = xPos[upper[j - 1]] +
                    ( xPos[lower[j]] - xPos[upper[j - 1]] ) * 0.5;
                newData[k] = BAD;
                k++;
            }
            length = upper[j] - lower[j] + 1;
            System.arraycopy( xPos, lower[j], newCoords, k, length );
            System.arraycopy( yPos, lower[j], newData, k, length );
            k += length;
        }

        //  Same for errors, if have any.
        double[] newErrors = null;
        if ( haveYDataErrors() ) {
            newErrors = new double[nvals];
            k = 0;
            length = 0;
            for ( int j = 0; j < nRanges; j++ ) {
                if ( j != 0 ) {
                    newErrors[k] = BAD;
                    k++;
                }
                length = upper[j] - lower[j] + 1;
                System.arraycopy( yErr, lower[j], newErrors, k, length );
                k += length;
            }
        }

        //  And create the memory spectrum.
        return createNewSpectrum( name, newCoords, newData, newErrors );
    }

    /**
     * Create a new spectrum by deleting sections of this spectrum. The
     * section extents are defined in physical coordinates. Each section will
     * not contain any values that lie outside of its physical coordinate
     * range. <p>
     *
     * The spectrum created here is not added to any lists or created with any
     * configuration other than the default values (i.e. you must do this part
     * yourself) and is only kept in memory.
     *
     * @param name short name for the spectrum.
     * @param ranges an array of pairs of physical coordinates. These define
     *      the extents of the ranges to delete.
     * @return a spectrum that contains data only from outside the
     *      given ranges. May be null if all values are to be deleted.
     */
    public SpecData getSubSet( String name, double[] ranges )
    {

        //  Locate the index of the positions just below and above our
        //  physical value pairs and count the number of values that will be
        //  deleted.
        int nRanges = ranges.length / 2;
        int[] lower = new int[nRanges];
        int[] upper = new int[nRanges];
        int[] low;
        int[] high;
        int ndelete = 0;

        for ( int i = 0, j = 0; j < nRanges; i += 2, j++ ) {
            low = bound( ranges[i] );
            lower[j] = low[1];
            high = bound( ranges[i + 1] );
            upper[j] = high[0];
            ndelete += high[0] - low[1] + 1;
        }

        if ( ndelete < 0 ) {
            //  Coordinates run in a reversed direction.
            ndelete = Math.abs( ndelete ) + 2 * nRanges;
            high = lower;
            lower = upper;
            upper = high;

            //  The ranges must increase.
            Sort.insertionSort2( lower, upper );
        }

        if ( ndelete >= xPos.length || ndelete == 0 ) {
            return null;
        }

        //  Copy remaining data. The size of result spectrum is the current
        //  size, minus the number of positions that will be erased, plus one
        //  per range to hold the BAD value to form the break.
        int nkeep = xPos.length - ndelete + nRanges;
        double[] newCoords = new double[nkeep];
        double[] newData = new double[nkeep];
        int length = 0;
        for ( int i = 0, j = 0, k = 0; i < xPos.length; i++ ) {
            if ( i >= lower[j] && i <= upper[j] ) {
                //  Arrived within a range, so mark one mid-range
                //  position BAD (so show the break) and skip to the
                //  end. Set for next valid range.
                newCoords[k] = xPos[i];
                newData[k] = BAD;
                k++;
                if ( j + 1 == nRanges ) {
                    //  No more ranges, so just complete the copy.
                    for ( int l = upper[j] + 1; l < xPos.length; l++ ) {
                        if ( k < nkeep ) {
                            newCoords[k] = xPos[l];
                            newData[k] = yPos[l];
                        }
                        k++;
                    }
                    i = xPos.length;
                    break;
                }
                i = upper[j];
                j++;
            }
            else {
                newCoords[k] = xPos[i];
                newData[k] = yPos[i];
                k++;
            }
        }

        //  Same for errors, if have any.
        double[] newErrors = null;
        if ( haveYDataErrors() ) {
            newErrors = new double[nkeep];
            for ( int i = 0, j = 0, k = 0; i < xPos.length; i++ ) {
                if ( i >= lower[j] && i <= upper[j] ) {
                    newErrors[k] =  BAD;
                    k++;
                    if ( j + 1 == nRanges ) {
                        for ( int l = upper[j] + 1; l < xPos.length; l++ ) {
                            if ( k < nkeep ) {
                                newErrors[k] = yErr[l];
                            }
                            k++;
                        }
                        i = xPos.length;
                        break;
                    }
                    i = upper[j];
                    j++;
                }
                else {
                    newErrors[k] = yErr[i];
                    k++;
                }
            }
        }

        //  Final task, trim off any BAD values at ends if ranges
        //  included those.
        if ( lower[0] <= 0 || upper[upper.length-1] >= ( xPos.length - 1 ) ) {
            int i1 = 0;
            int i2 = newData.length;
            if ( lower[0] <= 0 ) {
                i1++;
            }
            if ( upper[upper.length-1] >= ( xPos.length - 1 ) ) {
                i2--;
            }
            int trimlength = i2 - i1;
            double trimCoords[] = new double[trimlength];
            System.arraycopy( newCoords, i1, trimCoords, 0, trimlength );
            newCoords = trimCoords;

            double trimData[] = new double[trimlength];
            System.arraycopy( newData, i1, trimData, 0, trimlength );
            newData = trimData;

            double trimErrors[] = null;
            if ( newErrors != null ) {
                trimErrors = new double[trimlength];
                System.arraycopy( newErrors, i1, trimErrors, 0, trimlength );
                newErrors = trimErrors;
            }
        }

        //  And create the memory spectrum.
        return createNewSpectrum( name, newCoords, newData, newErrors );
    }

    /**
     * Create a new spectrum by deleting sections of this spectrum and linear
     * interpolating across the sections. The section extents are defined in
     * physical coordinates. <p>
     *
     * The spectrum created here is not added to any lists or created with any
     * configuration other than the default values (i.e. you must do this part
     * yourself) and is only kept in memory.
     *
     * @param name short name for the spectrum.
     * @param ranges an array of pairs of physical coordinates. These define
     *               the extents of the ranges to remove and interpolate.
     * @return the new spectrum.
     */
    public SpecData getInterpolatedSubSet( String name, double[] ranges )
    {
        //  Locate the index of the positions just below and above our
        //  physical value pairs.
        int nRanges = ranges.length / 2;
        int[] lower = new int[nRanges];
        int[] upper = new int[nRanges];
        int[] low;
        int[] high;

        int ninterp = 0;
        for ( int i = 0, j = 0; j < nRanges; i += 2, j++ ) {
            low = bound( ranges[i] );
            lower[j] = low[1];
            high = bound( ranges[i + 1] );
            upper[j] = high[0];
            ninterp += high[0] - low[1] + 1;
        }

        if ( ninterp < 0 ) {
            //  Coordinates run in a reversed direction.
            ninterp = Math.abs( ninterp ) + 2 * nRanges;
            high = lower;
            lower = upper;
            upper = high;

            //  The ranges must increase.
            Sort.insertionSort2( lower, upper );
        }

        if ( ninterp >= xPos.length || ninterp == 0 ) {
            return null;
        }

        //  Copy data.
        int nkeep = xPos.length;
        double[] newCoords = new double[xPos.length];
        double[] newData = new double[xPos.length];
        System.arraycopy( xPos, 0, newCoords, 0, xPos.length );
        System.arraycopy( yPos, 0, newData, 0, xPos.length );

        LinearInterp interp;
        double x[] = new double[2];
        double y[] = new double[2];
        for ( int i = 0, j = 0; i < xPos.length; i++ ) {
            if ( i >= lower[j] && i <= upper[j] ) {

                //  Arrived within a range. Need to interpolate.
                x[0] = xPos[lower[j]];
                x[1] = xPos[upper[j]];
                y[0] = yPos[lower[j]];
                y[1] = yPos[upper[j]];
                interp = new LinearInterp( x, y );

                for ( ; i <= upper[j]; i++ ) {
                    newData[i] = interp.interpolate( xPos[i] );
                }

                //  Next range or done.
                if ( j + 1 == nRanges ) break;
                j++;
            }
        }

        //  Dump errors.

        //  And create the memory spectrum.
        return createNewSpectrum( name, newCoords, newData, null );
    }

    /**
     * Create a new (memory-resident) spectrum using the given data and
     * coordinates, but based on this instance.
     * <p>
     * This looses the full WCS as the new coordinates are assumed no longer
     * directly related, i.e.<!-- --> indices of data do not map to proper
     * coordinates using the WCS, but the coordinate system of the spectrum
     * should be preserved (using the SpecFrame to reconstruct a new WCS).
     */
    protected  SpecData createNewSpectrum( String name, double[] coords,
                                           double[] data, double[] errors )
    {
        EditableSpecData newSpec = null;
        try {
            newSpec = SpecDataFactory.getInstance().createEditable( name,
                                                                    this );
            FrameSet frameSet = ASTJ.get1DFrameSet( astJ.getRef(), 1 );
            newSpec.setSimpleUnitDataQuick( frameSet, coords,
                                            getCurrentDataUnits(), data,
                                            errors );
            applyRenderingProperties( newSpec );
        }
        catch ( Exception e ) {
            e.printStackTrace();
            newSpec = null;
        }
        return newSpec;
    }

    /**
     * Copy some rendering properties so that another spectrum seems to
     * inherit this one.
     */
    public void applyRenderingProperties( SpecData newSpec )
    {
        newSpec.setLineThickness( lineThickness );
        newSpec.setLineStyle( lineStyle );
        newSpec.setAlphaComposite( alphaComposite );
        newSpec.setLineColour( (int) lineColour );
        newSpec.setErrorColour( (int) errorColour );
        newSpec.setPlotStyle( plotStyle );
        newSpec.setPointType( pointType );
        newSpec.setPointSize( pointSize );
    }

    /**
     * Get the data ranges in the X and Y axes. Does not include data errors.
     *
     * @return reference to array of 4 values, xlow, xhigh, ylow, yhigh.
     */
    public double[] getRange()
    {
        return range;
    }


    /**
     * Get the data ranges in the X and Y axes, including the standard
     * deviations.
     *
     * @return reference to array of 4 values, xlow, xhigh, ylow, yhigh.
     */
    public double[] getFullRange()
    {
        return fullRange;
    }

    /**
     * Get the coordinates of the points used to define the range in the X axis.
     *
     * @return reference to array of 4 values, x1, y1, y2, y2.
     */
    public double[] getXEndPoints()
    {
        return xEndPoints;
    }

    /**
     * Get the coordinates of the points used to define the range in the Y axis.
     *
     * @return reference to array of 4 values, x1, y1, y2, y2.
     */
    public double[] getYEndPoints()
    {
        return yEndPoints;
    }

    /**
     * Get the coordinates of the points used to define the full range in the
     * X axis.
     *
     * @return reference to array of 4 values, x1, y1, y2, y2.
     */
    public double[] getXFullEndPoints()
    {
        return xEndPoints;
    }

    /**
     * Get the coordinates of the points used to define the full range in the
     * Y axis.
     *
     * @return reference to array of 4 values, x1, y1, y2, y2.
     */
    public double[] getYFullEndPoints()
    {
        return yFullEndPoints;
    }


    /**
     * Get reference to ASTJ object set up to specify the
     * transformations for plotting coordinates against data values.
     *
     * @return ASTJ object describing plottable axis transformations.
     */
    public ASTJ getAst()
    {
        return astJ;
    }

    /**
     * Get reference to the FrameSet that is used by the SpecDataImpl. Note
     * this may not be 1D. It is made available mainly for creating copies of
     * SpecData (the ASTJ FrameSet is not suitable for writing directly to
     * disk file). Do not confuse this with the {@link #getAst()} FrameSet, it
     * is the original dataset FrameSet.
     */
    public FrameSet getFrameSet()
    {
        return impl.getAst();
    }


    /**
     * Return the "channel spacing". Determined by getting increment of moving
     * one pixel along the first axis. Can be in units other than default by
     * supplying an attribute string (System=FREQ,Unit=MHz).
     *
     * @param atts an AST attribute string, use to set the coordinates that
     *             the channel spacing is required in.
     *
     * @return the channel spacing
     */
    public double channelSpacing( String atts )
    {
        //  Get the axis from the underlying dataset AST FrameSet. Cannot
        //  use the plot FrameSet as it may be currently modified by an
        //  in progress redraw of the main plot (base frame will be
        //  incorrect).
        FrameSet frameSet = impl.getAst();
        int axis = getMostSignificantAxis();
        FrameSet mapping = ASTJ.extract1DFrameSet( frameSet, axis );

        //  Apply the attributes.
        if ( ! "".equals( atts ) ) {
            mapping.set( atts );
        }

        //  Delta of one pixel in base frame, around the centre.
        int first = xPos.length / 2;
        double xin[] = new double[]{ first, first + 1 };
        double xout[] = mapping.tran1( 2, xin, true );
        double dnu = Math.abs( xout[1] - xout[0] );
        return dnu;
    }

    /**
     * Get the size of the spectrum.
     *
     * @return number of coordinate positions in spectrum.
     */
    public int size()
    {
        return xPos.length;
    }


    /**
     * Return the data format as a suitable name.
     *
     * @return the data format as a simple string (FITS, NDF, etc.).
     */
    public String getDataFormat()
    {
        return impl.getDataFormat();
    }


    /**
     * Set the thickness of the line used to plot the spectrum.
     *
     * @param lineThickness the line width to be used when plotting.
     */
    public void setLineThickness( double lineThickness )
    {
        this.lineThickness = lineThickness;
    }


    /**
     * Get the width of the line to be used when plotting the spectrum. The
     * meaning of this value isn't defined.
     *
     * @return the line thickness.
     */
    public double getLineThickness()
    {
        return lineThickness;
    }


    /**
     * Set the style of the line used to plot the spectrum.
     *
     * @param lineStyle the line style to be used when plotting.
     */
    public void setLineStyle( double lineStyle )
    {
        this.lineStyle = lineStyle;
    }


    /**
     * Get the line type to be used when plotting the spectrum. The meaning of
     * this value is not defined.
     *
     * @return the line style in use.
     */
    public double getLineStyle()
    {
        return lineStyle;
    }


    /**
     * Set the alpha composite value of the line used plot the spectrum.
     *
     * @param alphaComposite the alpha composite value (0.0 to 1.0).
     */
    public void setAlphaComposite( double alphaComposite )
    {
        this.alphaComposite = alphaComposite;
    }


    /**
     * Get the alpha composite value.
     *
     * @return the current alpha composite value.
     */
    public double getAlphaComposite()
    {
        return alphaComposite;
    }


    /**
     * Set the colour index to be used when plotting the spectrum.
     *
     * @param lineColour the colour as an RGB integer.
     */
    public void setLineColour( int lineColour )
    {
        this.lineColour = (double) lineColour;
    }


    /**
     * Get the colour of the line to be used when plotting the spectrum.
     *
     * @return the line colour (an RGB integer).
     */
    public int getLineColour()
    {
        return (int) lineColour;
    }


    /**
     * Set the colour index to be used when drawing error bars.
     *
     * @param errorColour the colour as an RGB integer.
     */
    public void setErrorColour( int errorColour )
    {
        this.errorColour = errorColour;
    }


    /**
     * Get the colour used when drawing error bars.
     *
     * @return the error bar colour (an RGB integer).
     */
    public int getErrorColour()
    {
        return (int) errorColour;
    }

    /**
     * Set the number of sigma error bars are drawn at.
     *
     * @param nsigma the number of sigma
     */
    public void setErrorNSigma( int nsigma )
    {
        this.errorNSigma = nsigma;
    }


    /**
     * Get the number of sigma error bars are drawn at.
     *
     * @return the number of sigma.
     */
    public double getErrorNSigma()
    {
        return errorNSigma;
    }

    /**
     * Set the frequency that error bars are drawn.
     *
     * @param freq the frequency (runs from 1 upwards).
     */
    public void setErrorFrequency( int freq )
    {
        this.errorFrequency = Math.max( 1, Math.abs( freq ) );
    }

    /**
     * Get the frequency that error bars are drawn.
     *
     * @return the frequency
     */
    public double getErrorFrequency()
    {
        return errorFrequency;
    }

    /**
     * Set the type of spectral lines that are drawn (these can be polylines
     * or histogram-like, simple markers are a possibility for a future
     * implementation). The value should be one of the symbolic constants
     * "POLYLINE", "HISTOGRAM" or "POINT".
     *
     * @param style one of the symbolic contants SpecData.POLYLINE,
     *      SpecData.HISTOGRAM or SpecData.POINT.
     */
    public void setPlotStyle( int style )
    {
        this.plotStyle = style;
    }

    /**
     * Get the value of plotStyle.
     *
     * @return the current plotting type (SpecData.POLYLINE,
     *         SpecData.HISTOGRAM or SpecData.POINT).
     */
    public int getPlotStyle()
    {
        return plotStyle;
    }


    /**
     * Set the type of marker used when drawing spectra with a "POINT"
     * style. The marker types are defined by the underlying AST graphics
     * implementation - {@link DefaultGrfMarker}.
     *
     * @param type one of the symbolic values used in {@link DefaultGrfMarker}.
     */
    public void setPointType( int type )
    {
        this.pointType = type;
    }

    /**
     * Get the type of marker that will be used if the spectrum is drawing
     * using a "POINT" style.
     *
     * @return the current marker type.
     */
    public int getPointType()
    {
        return pointType;
    }

    /**
     * Set the size used when drawing using a point type.
     *
     * @param pointSize the size of a point.g
     */
    public void setPointSize( double pointSize )
    {
        this.pointSize = pointSize;
    }


    /**
     * Get the size used when drawing using a point type.
     *
     * @return the size of points
     */
    public double getPointSize()
    {
        return pointSize;
    }


    /**
     * Set a known numeric spectral property.
     *
     * @param what either LINE_THICKNESS, LINE_STYLE, LINE_COLOUR, PLOT_STYLE,
     *      POINT_TYPE, POINT_SIZE, LINE_ALPHA_COMPOSITE or ERROR_COLOUR.
     * @param value container for numeric value. These depend on property
     *      being set.
     */
    public void setKnownNumberProperty( int what, Number value )
    {
        switch ( what ) {
            case LINE_THICKNESS:
            {
                setLineThickness( value.doubleValue() );
                break;
            }
            case LINE_STYLE:
            {
                setLineStyle( value.doubleValue() );
                break;
            }
            case LINE_COLOUR:
            {
                setLineColour( value.intValue() );
                break;
            }
            case PLOT_STYLE:
            {
                setPlotStyle( value.intValue() );
                break;
            }
            case POINT_TYPE:
            {
                setPointType( value.intValue() );
                break;
            }
            case POINT_SIZE:
            {
                setPointSize( value.doubleValue() );
                break;
            }
            case LINE_ALPHA_COMPOSITE:
            {
                setAlphaComposite( value.doubleValue() );
                break;
            }
            case ERROR_COLOUR:
            {
                setErrorColour( value.intValue() );
                break;
            }
            case ERROR_NSIGMA:
            {
                setErrorNSigma( value.intValue() );
                break;
            }
            case ERROR_FREQUENCY:
            {
                setErrorFrequency( value.intValue() );
                break;
            }
        }
    }

    /**
     * Set the type of the spectral data.
     *
     * @param type either UNCLASSIFIED, TARGET, ARC, SKY, POLYNOMIAL or
     *      LINEFIT or USERTYPE.
     */
    public void setType( int type )
    {
        this.type = type;
    }


    /**
     * Get the type of the spectral data.
     *
     * @return type either UNCLASSIFIED, TARGET, ARC, SKY, POLYNOMIAL or
     *      LINEFIT or USERTYPE.
     */
    public int getType()
    {
        return type;
    }


    /**
     * Get whether the spectrum coordinates are monotonic or not.
     */
    public boolean isMonotonic()
    {
        return monotonic;
    }

    /**
     * Set whether the spectrum should have its errors drawn as error bars, or
     * not. If no errors are available then this is always false.
     *
     * @param state true to draw error bars, if possible.
     */
    public void setDrawErrorBars( boolean state )
    {
        if ( yErr != null && state ) {
            drawErrorBars = true;
        }
        else {
            drawErrorBars = false;
        }
    }


    /**
     * Find out if we're drawing error bars.
     *
     * @return true if we're drawing error bars.
     */
    public boolean isDrawErrorBars()
    {
        return drawErrorBars;
    }


    /**
     * Set whether the spectrum should be used when deriving auto-ranged
     * values.
     *
     * @param state if true then this spectrum should be used when determining
     *      data limits (artificial spectra could have unreasonable ranges
     *      when extrapolated).
     */
    public void setUseInAutoRanging( boolean state )
    {
        this.useInAutoRanging = state;
    }


    /**
     * Find out if we should be used when determining an auto-range.
     *
     * @return whether this spectrum is used when auto-ranging.
     */
    public boolean isUseInAutoRanging()
    {
        return useInAutoRanging;
    }

    /**
     * Set if searching should be done for suitable spectral coordinate
     * systems when creating the plot framesets. If changed after the
     * application starts it requires {@link initialiseAst()} to be invoked on
     * all spectra before the change will be seen.
     */
    public static void setSearchForSpecFrames( boolean searchForSpecFrames )
    {
        SpecData.searchForSpecFrames = searchForSpecFrames;
    }

    /**
     * Find out if searching for a suitable spectral coordinate
     * systems will be made when creating plot framesets.
     */
    public static boolean isSearchForSpecFrames()
    {
        return SpecData.searchForSpecFrames;
    }

    /**
     * Getter for object type that identifies type of object (spectrum or timeseries)
     * FIXME: This is a hacky way for quick and partial timeseries implementation
     * @return
     */
    public ObjectTypeEnum getObjectType() {
		return impl.getObjectType();
	}
    
    /**
     * /**
     * Setter for object type that identifies type of object (spectrum or timeseries)
     * FIXME: This is a hacky way for quick and partial timeseries implementation
     
     * @param objectType
     */
    public void setObjectType(ObjectTypeEnum objectType) {
		impl.setObjectType(objectType);
	}
    
    /**
     * Read the data from the spectrum into local arrays.  This also
     * initialises a suitable AST frameset to describe the coordinate system
     * in use and establishes the minimum and maximum ranges for both
     * coordinates (X, i.e. the wavelength and Y, i.e. data count).
     *
     * @exception SplatException thrown if an error condition is encountered.
     */
    protected void readData()
        throws SplatException
    {
        readDataPrivate();
    }

    /**
     * Private version of readData as called from constructor.
     */
    private void readDataPrivate()
        throws SplatException
    {
        //  Get the spectrum data counts and errors (can be null).
        try {
            apparentDataUnits = null;
            yPos = impl.getData();
            yPosOri = yPos;
            yErr = impl.getDataErrors();
            yErrOri = yPos;
        }
        catch (RuntimeException e) {
            // None specific errors, like no data...
            throw new SplatException( "Failed to read data: " +
                                      e.getMessage() );
        }

        // Check we have some data.
        if ( yPos == null ) {
            throw new SplatException( "Spectrum does not contain any data" );
        }
        initialiseAst();
    }

    /**
     * Initialise the spectral coordinates. Uses the AST information of the
     * underlying implementation to generate a FrameSet suitable for plotting
     * coordinates against data values and generates the "getXData()" array.
     */
    public void initialiseAst()
        throws SplatException
    {
        //  Create the init "wavelength" positions as simple
        //  increasing vector (this places them at centre of pixels).
        xPos = new double[yPos.length];
        for ( int i = 0; i < yPos.length; i++ ) {
            xPos[i] = (double) ( i + 1 );
        }

        //  Get the dimensionality of the spectrum. If more than 1 we
        //  need to pick out a significant axis to work with.
        int sigaxis = getMostSignificantAxis();

        //  Get the AST component (this should define the wavelength
        //  axis coordinates, somehow).
        FrameSet astref = impl.getAst();
        if ( astref == null ) {
            throw new SplatException("spectrum has no coordinate information");
        }
        else {
            //  Create the required ASTJ object to manipulate the AST
            //  frameset.
            ASTJ ast = new ASTJ( astref );

            //  If the sigaxis isn't 1 then we may have a 3D spectrum
            //  where the other axes define an extraction position.
            //  Check for this and record that position.
            checkForExtractionPosition( astref, sigaxis );

            //  Create a frameset that is suitable for displaying a
            //  "spectrum". This has a coordinate X axis and a data Y
            //  axis. The coordinates are chosen to run along the sigaxis (if
            //  input data has more than one dimension) and may be a distance,
            //  rather than absolute coordinate.
            FrameSet specref = null;
            try {
                specref = ast.makeSpectral( sigaxis, 0, yPos.length,
                                            getDataLabel(), getDataUnits(),
                                            false, searchForSpecFrames );
            }
            catch (AstException e) {
                throw new SplatException( "Failed to find a valid spectral " +
                                          "coordinate system", e );
            }
            try {
                astJ = new ASTJ( specref );

                //  Get the mapping for the X axis and check that it is
                //  useful, i.e. we can invert it. If this isn't the case then
                //  many operations will fail, so record this so that we can
                //  check.
                Mapping oned = astJ.get1DMapping( 1 );
                monotonic = ( oned.getI( "TranInverse" ) == 1 );
                if ( ! monotonic ) {
                    logger.info( impl.getFullName() + ": " +
                                 " coordinates are not" +
                                 " monotonic this means some" +
                                 " operations will fail" );
                }

                //  Get the centres of the pixel positions in current
                //  coordinates (so we can eventually go from current
                //  coordinates through to graphics coordinates when
                //  actually drawing the spectrum).
                double[] tPos = ASTJ.astTran1( oned, xPos, true );
                xPos = tPos;
                tPos = null;

                //  Set the apparent data units, if possible.
                convertToApparentDataUnits();

                //  Set the axis range.
                setRangePrivate();

                //  Establish the digits value used to format the wavelengths,
                //  if not already set in the original data (the plot may
                //  override this).
                setFormatDigits();
            }
            catch (Exception e) {
                throw new SplatException( e );
            }
        }
    }

    /**
     * If not set in the original spectral axis WCS set a value for the digits
     * attribute that makes sure that each wavelength value (assuming a linear
     * scale) can be distinguished.
     */
    protected void setFormatDigits()
    {
        FrameSet frameSet = astJ.getRef();
        if ( ! frameSet.test( "digits(1)" ) && xPos.length > 1 ) {

            //  Number of decimals needed for channel spacing. Only used
            //  if the necessary precision is fractional (less than 0).
            //  We try to keep two figures, not just one.
            int first = xPos.length / 2;
            double dvalue = xPos[first] - xPos[first-1];
            double value = Math.abs( dvalue );
            double deltapower = Math.log( value ) / Math.log( 10.0 );
            int ideltapower = (int) deltapower;

            //  2nd figure in fractional precision.
            if ( ideltapower <= 0 ) {
                ideltapower -= 2;
            }

            //  Number of figures needed for maximum X coordinate, without
            //  a fractional part.
            dvalue = range[1];
            value = Math.abs( dvalue );
            double power = Math.log( value ) / Math.log( 10.0 );
            int ipower = 1 + (int) power;

            //  If we need fractional precision, add those figures to those
            //  for the maximum X coordinate.
            int digits;
            if ( ideltapower <= 0 ) {
                digits = Math.abs( ideltapower ) + ipower;
            }
            else {
                //  Otherwise just enough figures for the X coordinate.
                digits = ipower;
            }

            //  Keep the number of digits in a sensible range.
            digits = Math.min( MAX_DIGITS, Math.max( MIN_DIGITS, digits ) );

            //  Set the value.
            frameSet.setI( "Digits(1)", digits );
        }
    }

    /**
     * Convert the data units to some new value, if possible.
     */
    protected void convertToApparentDataUnits()
    {
        if ( apparentDataUnits == null ||
             apparentDataUnits.equals( "Unknown" ) ) {
            return;
        }

        //  To do this we need to modify the data values and then set the
        //  data units of the Frame representing the data value to
        //  match. This necessarily means creating a memory copy of the
        //  data values. We work with the full Frame, rather than extracting
        //  the second axis, as FluxFrames will only transform properly when
        //  part of a SpecFluxFrame (to get from pre frequency to per
        //  wavelength requires the spectral coordinate).

        //  Get a reference to the current Frame. This should be a
        //  SpecFluxFrame or a plain CmpFrame.
        FrameSet frameSet = astJ.getRef();
        Frame currentFrame = frameSet.getFrame( FrameSet.AST__CURRENT );

        //  Make a copy of this and set the new units.
        Frame targetFrame = (Frame) currentFrame.copy();
        targetFrame.setActiveUnit( true );
        targetFrame.set( "unit(2)=" + apparentDataUnits );

        //  Now try to convert between them
        currentFrame.setActiveUnit( true );
        Mapping mapping = currentFrame.convert( targetFrame, "" );
        if ( mapping == null ) {

            //  If fails then just use the values we have.
            logger.info( shortName + ": cannot convert data units from " +
                         currentFrame.getC( "unit" ) + " to " +
                         apparentDataUnits );
            apparentDataUnits = null;
            return;
        }

        //  Now transform the original data values, not ones that may have
        //  already been transformed. The FrameSet will match these.
        try {
            double[][] tmp1 = mapping.tran2( yPos.length, xPos, yPosOri,
                                             true );
            if ( yErr != null ) {
                double[][] tmp2 = mapping.tran2( yPos.length, xPos, yErrOri,
                                                 true );
                yErr = new double[yPos.length];
                for ( int i = 0; i < yPos.length; i++ ) {
                    yErr[i] = tmp2[1][i];
                }
            }
            yPos = new double[yPos.length];
            for ( int i = 0; i < yPos.length; i++ ) {
                yPos[i] = tmp1[1][i];
            }
        }
        catch (AstException e) {
            logger.info( shortName + ": failed to convert to new data units ( "
                         + e.getMessage() + " )" );
            apparentDataUnits = null;
            return;
        }

        //  And set the data units.
        currentFrame.setActiveUnit( false );
        currentFrame.set( "unit(2)=" + apparentDataUnits );
    }

    /**
     * Return the most significant axis of the data held by the
     * implemenation. This is the default axis that is used when the
     * underlying data has more than one dimension. The value returned is an
     * AST index (starts at 1).
     */
    public int getMostSignificantAxis()
    {
        //  If this is a de-serialized spectrum the connection to the WCS
        //  axes, if any, has been lost - handle that.
        if ( serializedSigAxis != 0 ) {
            return serializedSigAxis;
        }

        int sigaxis = 1;
        int dims[] = impl.getDims();
        if ( dims.length > 1 ) {
            for ( int i = 0; i < dims.length; i++ ) {
                if ( dims[i] > 1 ) {
                    sigaxis = i + 1;
                    break;
                }
            }
        }
        return sigaxis;
    }

    /**
     * If this is a nD dataset with WCS record the position of the
     * spectrum in the standard properties, if possible.
     */
    protected void checkForExtractionPosition( FrameSet astref, int sigaxis )
    {
        if ( impl instanceof FITSHeaderSource ) {
            int dims[] = impl.getDims();
            if ( dims.length > 1 ) {
                //  Get the coordinates for the grid position 1,1,1.
                double basepos[] = new double[dims.length];
                for ( int i = 0; i < dims.length; i++ ) {
                    basepos[i] = 1.0;
                }
                int nout = astref.getI( "Nout" );
                try {
                    double out[] = astref.tranN( 1, dims.length, basepos,
                                                 true, nout );

                    //  Cannot use "lonaxis" and "lataxis" within a compound
                    //  frame (they only apply to the embedded skyframe), so
                    //  search the hard way.
                    int lonaxis = 0;
                    int lataxis = 0;
                    for ( int i = 1; i <= nout; i++ ) {
                        try {
                            if ( lonaxis == 0 ) {
                                if ( astref.getI( "IsLonAxis(" + i + ")" ) > 0 ) {
                                    lonaxis = i;
                                }
                            }
                        }
                        catch (AstException e) {
                            //  Do nothing.
                            e.printStackTrace();
                        }
                        try {
                            if ( lataxis == 0 ) {
                                if ( astref.getI( "IsLatAxis(" + i + ")" ) > 0 ) {
                                    lataxis = i;
                                }
                            }
                        }
                        catch (AstException e) {
                            //  Do nothing.
                            e.printStackTrace();
                        }
                    }
                    Header header = getHeaders();
                    String ra = astref.format( lonaxis, out[lonaxis-1] );
                    String dec = astref.format( lataxis, out[lataxis-1] );
                    header.addValue( "EXRAX", ra, "Spectral position" );
                    header.addValue( "EXDECX", dec, "Spectral position" );
                }
                catch (Exception e) {
                    logger.info( "Failed to get spectral position ( " +
                                 e.getMessage() + " )" );
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * Set/reset the current range of the data.
     */
    public void setRange()
    {
        setRangePrivate();
    }

    /**
     * Private version of setRange. Called from constructor so should not be
     * directly overridden.
     */
    private void setRangePrivate()
    {
        double xMin = Double.MAX_VALUE;
        double xMinY = 0.0;

        double xMax = -Double.MAX_VALUE;
        double xMaxY = 0.0;

        double yMin = xMin;
        double yMinX = 0.0;

        double yMax = xMax;
        double yMaxX = 0.0;

        double fullYMin = xMin;
        double fullYMinX = 0.0;
        double fullYMax = xMax;
        double fullYMaxX = 0.0;
        double tmp;

        if ( yErr != null ) {

            for ( int i = yPos.length - 1; i >= 0; i-- ) {
                if ( yPos[i] != SpecData.BAD ) {
                    if ( xPos[i] < xMin ) {
                        xMin = xPos[i];
                        xMinY = yPos[i];
                    }
                    if ( xPos[i] > xMax ) {
                        xMax = xPos[i];
                        xMaxY = yPos[i];
                    }
                    if ( yPos[i] < yMin ) {
                        yMin = yPos[i];
                        yMinX = xPos[i];
                    }
                    if ( yPos[i] > yMax ) {
                        yMax = yPos[i];
                        yMaxX = xPos[i];
                    }

                    tmp = yPos[i] - ( yErr[i] * errorNSigma );
                    if ( tmp < fullYMin ) {
                        fullYMin = tmp;
                        fullYMinX = xPos[i];
                    }

                    tmp = yPos[i] + ( yErr[i] * errorNSigma );
                    if ( tmp > fullYMax ) {
                        fullYMax = tmp;
                        fullYMaxX = xPos[i];
                    }
                }
            }
        }
        else {
            for ( int i = yPos.length - 1; i >= 0 ; i-- ) {
                if ( yPos[i] != SpecData.BAD ) {
                    if ( xPos[i] < xMin ) {
                        xMin = xPos[i];
                        xMinY = yPos[i];
                    }
                    if ( xPos[i] > xMax ) {
                        xMax = xPos[i];
                        xMaxY = yPos[i];
                    }
                    if ( yPos[i] < yMin ) {
                        yMin = yPos[i];
                        yMinX = xPos[i];
                    }
                    if ( yPos[i] > yMax ) {
                        yMax = yPos[i];
                        yMaxX = xPos[i];
                    }
                }
            }
            fullYMin = yMin;
            fullYMax = yMax;
            fullYMinX = yMinX;
            fullYMaxX = yMaxX;
        }
        if ( xMin == Double.MAX_VALUE ) {
            xMin = 0.0;
            xMinY = 0.0;
        }
        if ( xMax == -Double.MAX_VALUE ) {
            xMax = 0.0;
            xMaxY = 0.0;
        }

        //  Record plain range.
        range[0] = xMin;
        range[1] = xMax;
        range[2] = yMin;
        range[3] = yMax;

        //  And the "full" version.
        fullRange[0] = xMin;
        fullRange[1] = xMax;
        fullRange[2] = fullYMin;
        fullRange[3] = fullYMax;

        //  Add slack so that error bars do not abutt the edges.
        double slack = ( fullRange[3] - fullRange[2] ) * SLACK;
        fullRange[2] = fullRange[2] - slack;
        fullRange[3] = fullRange[3] + slack;

        //  Coordinates of positions used to determine plain range.
        xEndPoints[0] = xMin;
        xEndPoints[1] = xMinY;
        xEndPoints[2] = xMax;
        xEndPoints[3] = xMaxY;

        yEndPoints[0] = yMinX;
        yEndPoints[1] = yMin;
        yEndPoints[2] = yMaxX;
        yEndPoints[3] = yMax;

        //  Coordinates of positions used to determine full range.
        yFullEndPoints[0] = fullYMinX;
        yFullEndPoints[1] = fullRange[2];
        yFullEndPoints[2] = fullYMaxX;
        yFullEndPoints[3] = fullRange[3];
    }

    /**
     * Draw the spectrum onto the given widget using a suitable AST GRF
     * object.
     *
     * @param grf Grf object that can be drawn into using AST primitives.
     * @param plot reference to Plot defining transformation from physical
     *             coordinates into graphics coordinates.
     * @param clipLimits limits of the region to draw used to clip graphics.
     *                   These can be in physical or graphics coordinates.
     * @param physical whether limits are physical or graphical.
     * @param fullLimits full limits of drawing area in graphics coordinates.
     *                   May be used for positioning when clipping limits are
     *                   not used.
     */
    public void drawSpec( Grf grf, Plot plot, double[] clipLimits,
                          boolean physical, double[] fullLimits )
    {
        //  Get a list of positions suitable for transforming.
        //  Note BAD value is same for graphics (AST, Grf) and data,
        //  so tests can be missed (should only be in yPos).
        double[] xypos = null;
        double yoffset = 0.0;
        if ( applyYOffset ) {
            yoffset = this.yoffset;
        }
        if ( plotStyle == POLYLINE || plotStyle == POINT ) {
            xypos = new double[xPos.length * 2];
            for ( int i = 0, j = 0; j < xPos.length; j++, i += 2 ) {
                xypos[i] = xPos[j];
                xypos[i + 1] = yPos[j] + yoffset;
            }
        }
        else {
            //  Draw a histogram style plot. Need to generate a list
            //  of positions that can be connected like a normal
            //  spectrum, but looks like a histogram. Create a list
            //  with positions at each "corner":
            //
            //       x  *  x
            //                   x  *  x
            //             x  *  x
            //
            //  i.e. generate "x" positions for each "*" position. Use
            //  different backwards and forwards width of bin as step
            //  could be non-linear.
            double bwidth = 0.0; //  backwards width
            double fwidth = 0.0; //  forwards width
            int i = 0; // position in xypos array (pairs of X,Y coordinates)
            int j = 0; // position xPos & yPos arrays
            xypos = new double[xPos.length * 4];

            //  First point has no backwards width, so use double
            //  forward width.
            fwidth = ( xPos[j + 1] - xPos[j] ) * 0.5;
            xypos[i] = xPos[j] - fwidth;
            xypos[i + 2] = xPos[j] + fwidth;
            xypos[i + 1] = yPos[j] + yoffset;
            xypos[i + 3] = yPos[j] + yoffset;
            j++;
            i += 4;

            //  Do normal positions.
            for ( ; j < xPos.length - 1; j++, i += 4 ) {
                fwidth = ( xPos[j + 1] - xPos[j] ) * 0.5;
                bwidth = ( xPos[j] - xPos[j - 1] ) * 0.5;
                xypos[i] = xPos[j] - bwidth;
                xypos[i + 2] = xPos[j] + fwidth;
                xypos[i + 1] = yPos[j] + yoffset;
                xypos[i + 3] = yPos[j] + yoffset;
            }

            //  Until final final point which is also unpaired forward
            //  so use double backwards width.
            bwidth = ( xPos[j] - xPos[j - 1] ) * 0.5;
            xypos[i] = xPos[j] - bwidth;
            xypos[i + 2] = xPos[j] + bwidth;
            xypos[i + 1] = yPos[j] + yoffset;
            xypos[i + 3] = yPos[j] + yoffset;
        }

        //  Transform positions into graphics coordinates.
        double[][] xygpos = astJ.astTran2( (Mapping) plot, xypos, false );
        int np = xygpos[0].length;
        double[] xpos = new double[np];
        double[] ypos = new double[np];
        for ( int j = 0; j < np; j++ ) {
            xpos[j] = xygpos[0][j];
            ypos[j] = xygpos[1][j];
        }

        xPosG = xpos;
        yPosG = ypos;

        //  Do the same for the clip region.
        Rectangle cliprect = null;
        if ( clipLimits != null ) {
            if ( physical ) {
                double[][] clippos = astJ.astTran2( plot, clipLimits, false );
                cliprect =
                    new Rectangle( (int) clippos[0][0],
                                   (int) clippos[1][1],
                                   (int) ( clippos[0][1] - clippos[0][0] ),
                                   (int) ( clippos[1][0] - clippos[1][1] ) );
            }
            else {
                cliprect = new Rectangle((int) clipLimits[0],
                                         (int) clipLimits[3],
                                         (int) (clipLimits[2]-clipLimits[0]),
                                         (int) (clipLimits[1]-clipLimits[3]));
            }
        }

        //  Plot using the GRF primitive, rather than astPolyCurve, as
        //  we are just plotting spectra and require only straight
        //  lines, not geodesics (this makes it much faster). Need to
        //  establish line properites first, draw polyline and then
        //  restore properties.
        boolean line = ( plotStyle != POINT );
        DefaultGrf defaultGrf = (DefaultGrf) grf;
        DefaultGrfState oldState = setGrfAttributes( defaultGrf, line );

        defaultGrf.setClipRegion( cliprect );

        if ( line ) {
            renderSpectrum( defaultGrf, xpos, ypos );
        }
        else {
            renderPointSpectrum( defaultGrf, xpos, ypos, pointType );
        }

        //  Set line characteristics for error bars. Need to do this as they
        //  will not be set when drawing markers.
        defaultGrf.attribute( Grf.GRF__COLOUR, errorColour, Grf.GRF__LINE );
        defaultGrf.attribute( Grf.GRF__WIDTH, lineThickness, Grf.GRF__LINE );
        defaultGrf.attribute( Grf.GRF__STYLE, lineStyle, Grf.GRF__LINE );
        defaultGrf.attribute( defaultGrf.GRF__ALPHA, alphaComposite,
                              Grf.GRF__LINE );
        renderErrorBars( defaultGrf, plot );

        defaultGrf.setClipRegion( null );

        resetGrfAttributes( defaultGrf, oldState, line );
    }


    /**
     * Draw the spectrum using the current spectrum plotting style.
     *
     * @param grf DefaultGrf object that can be drawn into using AST
     *            primitives.
     * @param xpos graphics X coordinates of spectrum
     * @param ypos graphics Y coordinates of spectrum
     */
    protected void renderSpectrum( DefaultGrf grf, double[] xpos,
                                   double[] ypos )
    {
        grf.polyline( xpos, ypos );
    }

    /**
     * Draw the spectrum using markers.
     *
     * @param grf DefaultGrf object that can be drawn into using AST
     *            primitives.
     * @param xpos graphics X coordinates of spectrum
     * @param ypos graphics Y coordinates of spectrum
     * @param type the type of marker to use
     */
    protected void renderPointSpectrum( DefaultGrf grf, double[] xpos,
                                        double[] ypos, int type )
    {
        grf.marker( xpos, ypos, type );
    }

    /**
     * Draw error bars. These are quite simple lines above and below positions
     * by number of standard deviations, with a serif at each end.
     *
     * @param grf DefaultGrf object that can be drawn into using AST
     *            primitives.
     * @param plot reference to Plot defining transformation from physical
     *             coordinates into graphics coordinates.
     */
    protected void renderErrorBars( DefaultGrf grf, Plot plot )
    {
        if ( drawErrorBars && yErr != null ) {
            double[] xypos = new double[4];
            double[][] xygpos = null;
            double[] xpos = null;
            double[] ypos = null;
            double half = 0.0;
            for ( int i = 0; i < xPos.length; i += errorFrequency ) {
                if ( yErr[i] != SpecData.BAD ) {
                    half = yErr[i] * errorNSigma;
                    xypos[0] = xPos[i];
                    xypos[1] = yPos[i] - half;
                    xypos[2] = xPos[i];
                    xypos[3] = yPos[i] + half;
                    xygpos = astJ.astTran2( plot, xypos, false );

                    xpos = new double[2];
                    ypos = new double[2];
                    xpos[0] = xygpos[0][0];
                    xpos[1] = xygpos[0][1];
                    ypos[0] = xygpos[1][0];
                    ypos[1] = xygpos[1][1];
                    grf.polyline( xpos, ypos );

                    // Add the serifs.
                    xpos = new double[2];
                    ypos = new double[2];
                    xpos[0] = xygpos[0][0] - SERIF_LENGTH;
                    xpos[1] = xygpos[0][0] + SERIF_LENGTH;
                    ypos[0] = xygpos[1][0];
                    ypos[1] = xygpos[1][0];
                    grf.polyline( xpos, ypos );

                    xpos = new double[2];
                    ypos = new double[2];
                    xpos[0] = xygpos[0][1] - SERIF_LENGTH;
                    xpos[1] = xygpos[0][1] + SERIF_LENGTH;
                    ypos[0] = xygpos[1][1];
                    ypos[1] = xygpos[1][1];
                    grf.polyline( xpos, ypos );
                }
            }
        }
    }

    /**
     * Apply the current state to a Grf object, returning its existing state,
     * so that it can be restored.
     *
     * @param grf Grf object that can be drawn into using AST primitives.
     * @return the graphics state of the Grf object before being modified by
     *      this method.
     */
    protected DefaultGrfState setGrfAttributes( DefaultGrf grf, boolean line )
    {
        DefaultGrfState oldState = new DefaultGrfState();
        if ( line ) {
            oldState.setColour( grf.attribute( Grf.GRF__COLOUR, BAD,
                                               Grf.GRF__LINE ) );
            oldState.setStyle( grf.attribute( Grf.GRF__STYLE, BAD,
                                              Grf.GRF__LINE ) );
            oldState.setWidth( grf.attribute( Grf.GRF__WIDTH, BAD,
                                              Grf.GRF__LINE ) );
            oldState.setAlpha( grf.attribute( grf.GRF__ALPHA, BAD,
                                              Grf.GRF__LINE ) );

            //  Set new one from object members.
            grf.attribute( Grf.GRF__WIDTH, lineThickness, Grf.GRF__LINE );
            grf.attribute( Grf.GRF__STYLE, lineStyle, Grf.GRF__LINE );
            grf.attribute( Grf.GRF__COLOUR, lineColour, Grf.GRF__LINE );
            grf.attribute( grf.GRF__ALPHA, alphaComposite, Grf.GRF__LINE );

        }
        else {
            oldState.setColour( grf.attribute( Grf.GRF__COLOUR, BAD,
                                               Grf.GRF__MARK ) );
            oldState.setSize( grf.attribute( Grf.GRF__SIZE, BAD,
                                             Grf.GRF__MARK ) );
            oldState.setAlpha( grf.attribute( grf.GRF__ALPHA, BAD,
                                              Grf.GRF__MARK ) );

            //  Set new one from object members.
            grf.attribute( Grf.GRF__SIZE, pointSize, Grf.GRF__MARK );
            grf.attribute( Grf.GRF__COLOUR, lineColour, Grf.GRF__MARK );
            grf.attribute( grf.GRF__ALPHA, alphaComposite, Grf.GRF__MARK );

        }
        return oldState;
    }

    /**
     * Restore an existing Grf object to a given state.
     *
     * @param grf Grf object being used.
     * @param oldState the state to return Grf object to.
     */
    protected void resetGrfAttributes( DefaultGrf grf,
                                       DefaultGrfState oldState,
                                       boolean line )
    {
        if ( line ) {
            grf.attribute( Grf.GRF__COLOUR, oldState.getColour(),
                           Grf.GRF__LINE );
            grf.attribute( Grf.GRF__STYLE, oldState.getStyle(),
                           Grf.GRF__LINE );
            grf.attribute( Grf.GRF__WIDTH, oldState.getWidth(),
                           Grf.GRF__LINE );
            grf.attribute( grf.GRF__ALPHA, oldState.getAlpha(),
                           Grf.GRF__LINE );
        }
        else {
            grf.attribute( Grf.GRF__COLOUR, oldState.getColour(),
                           Grf.GRF__MARK );
            grf.attribute( Grf.GRF__SIZE, oldState.getSize(),
                           Grf.GRF__MARK );
            grf.attribute( grf.GRF__ALPHA, oldState.getAlpha(),
                           Grf.GRF__MARK );
        }
    }

    /**
     * Draw a fake spectrum using some given graphics coordinates
     * as the X and Y positions. This is intended for use when
     * a small representation of the spectrum is required, for
     * instance as part of a plot legend. See {@link drawSpec}.
     *
     * Note if a histogram style is being used that will be ignored.
     *
     * @param grf Grf object that can be drawn into using AST primitives.
     * @param xpos the graphics X positions.
     * @param ypos the graphics Y positions.
     */
    public void drawLegendSpec( Grf grf, double xpos[], double ypos[] )
    {
        boolean line = ( plotStyle != POINT );
        DefaultGrf defaultGrf = (DefaultGrf) grf;
        DefaultGrfState oldState = setGrfAttributes( defaultGrf, line );
        if ( line ) {
            renderSpectrum( defaultGrf, xpos, ypos );
        }
        else {
            renderPointSpectrum( defaultGrf, xpos, ypos, pointType );
        }
        resetGrfAttributes( defaultGrf, oldState, line );
    }

    /**
     * Lookup the index nearest to a given physical coordinate.
     *
     * @param x X coordinate
     * @return the index.
     */
    public int nearestIndex( double x )
    {
        //  Bound our value.
        int[] bounds = bound( x );

        //  Find which position is nearest.
        double[] result = new double[2];
        if ( Math.abs(x - xPos[bounds[0]]) < Math.abs(xPos[bounds[1]] - x) ) {
            return bounds[0];
        }
        return bounds[1];
    }


    /**
     * Lookup the nearest physical values (wavelength and data value)
     * to a given physical coordinate.
     *
     * @param x X coordinate
     * @return array of two doubles. The wavelength and data values.
     */
    public double[] nearest( double x )
    {
        int index = nearestIndex( x );
        double[] result = new double[2];
        result[0] = xPos[index];
        result[1] = yPos[index];
        return result;
    }

    /**
     * Lookup the physical values (wavelength and data value) that correspond
     * to a graphics X coordinate.
     *
     * @param xg X graphics coordinate
     * @param plot AST plot needed to transform graphics position into
     *             physical coordinates
     * @return array of two doubles. The wavelength and data values.
     */
    public double[] lookup( int xg, Plot plot )
    {
        //  Get the physical coordinate that corresponds to our
        //  graphics position.
        double[] xypos = new double[2];
        xypos[0] = (double) xg;
        xypos[1] = 0.0;
        double[][] xyphys = ASTJ.astTran2( plot, xypos, true );

        return nearest( xyphys[0][0] );
    }

    /**
     * Locate the indices of the two coordinates that lie closest to a given
     * coordinate. In the case of an exact match or value that lies
     * beyond the edges, then both indices are returned as the same value.
     *
     * @param xcoord the coordinate value to bound.
     * @return array of two integers, the lower and upper indices.
     */
    public int[] bound( double xcoord )
    {
        int bounds[] = new int[2];
        int low = 0;
        int high = xPos.length - 1;
        boolean increases = ( xPos[low] < xPos[high] );

        // Check off scale.
        if ( ( increases && xcoord < xPos[low] ) ||
             ( ! increases && xcoord > xPos[low] ) ) {
            high = low;
        }
        else if ( ( increases && xcoord > xPos[high] ) ||
                  ( ! increases && xcoord < xPos[high] ) ) {
            low = high;
        }
        else {
            //  Use a binary search as values should be sorted to increase
            //  in either direction (wavelength, pixel coordinates etc.).
            int mid = 0;
            if ( increases ) {
                while ( low < high - 1 ) {
                    mid = ( low + high ) / 2;
                    if ( xcoord < xPos[mid] ) {
                        high = mid;
                    }
                    else if ( xcoord > xPos[mid] ) {
                        low = mid;
                    }
                    else {
                        // Exact match.
                        low = high = mid;
                        break;
                    }
                }
            }
            else {
                while ( low < high - 1 ) {
                    mid = ( low + high ) / 2;
                    if ( xcoord > xPos[mid] ) {
                        high = mid;
                    }
                    else if ( xcoord < xPos[mid] ) {
                        low = mid;
                    }
                    else {
                        // Exact match.
                        low = high = mid;
                        break;
                    }
                }
            }
        }
        bounds[0] = low;
        bounds[1] = high;
        return bounds;
    }

    public int getPrefferedPlotType() {
    	int plotType = SpecData.POLYLINE;
    	
    	if (ObjectTypeEnum.TIMESERIES.equals(getObjectType())) {
    		plotType = SpecData.POINT;
    	}
    	
    	return plotType;
    }
    
    public int getPrefferedPointType() {
    	int pointType = 0; // dot
    	
    	if (ObjectTypeEnum.TIMESERIES.equals(getObjectType())) {
    		pointType = 1; // cross
    	}
    	
    	return pointType;
    }
    
    /**
     * Lookup the physical values (i.e.&nbsp;wavelength and data value) that
     * correspond to a graphics X coordinate. Value is returned as formatted
     * strings for the selected axis (could be sky coordinates for instance).
     *
     * @param xg X graphics coordinate
     * @param plot AST plot needed to transform graphics position into
     *             physical coordinates
     * @return array of two Strings, the formatted wavelength and data values
     */
    public String[] formatLookup( int xg, Plot plot )
    {
        double[] xypos = lookup( xg, plot );

        //  Let AST format the values as appropriate for the axes.
        String result[] = new String[2];
        result[0] = ASTJ.astFormat( 1, plot, xypos[0] );
        result[1] = ASTJ.astFormat( 2, plot, xypos[1] );
        return result;
    }


    /**
     * Return interpolated physical values (i.e.&nbsp;wavelength and
     * data value) that correspond to a graphics X coordinate.
     *
     * @param xg X graphics coordinate
     * @param plot AST plot needed to transform graphics position into
     *      physical coordinates
     * @return the physical coordinate and value. These are linearly
     *      interpolated.
     */
    public String[] formatInterpolatedLookup( int xg, Plot plot )
    {
        //  Convert the graphics coordinate to a physical coordinate.
        double[] xypos = new double[2];
        xypos[0] = (double) xg;
        xypos[1] = 0.0;
        double[][] xyphys = ASTJ.astTran2( plot, xypos, true );

        //  Get the indices of surrounding positions.
        int[] bounds = bound( xyphys[0][0] );
        if ( bounds[0] == bounds[1] ) {
            xyphys[1][0] = yPos[bounds[0]];
        }
        else {
            //  interpolate for the data value.
            xyphys[1][0] = yPos[bounds[0]] +
                ( yPos[bounds[0]] - yPos[bounds[1]] ) /
                ( xPos[bounds[0]] - xPos[bounds[1]] ) *
                ( xyphys[0][0] - xPos[bounds[0]] );
        }

        //  Let AST format the values as appropriate for the axes.
        String result[] = new String[2];
        result[0] = ASTJ.astFormat( 1, plot, xyphys[0][0] );
        result[1] = ASTJ.astFormat( 2, plot, xyphys[1][0] );
        return result;
    }


    /**
     * Convert a formatted coordinate string into a double precision value
     * (could be celestial coordinates for example).
     *
     * @param axis the axis to use for formatting rules.
     * @param plot AST plot that defines the coordinate formats.
     * @param value the formatted value.
     * @return the unformatted value.
     */
    public double unFormat( int axis, Plot plot, String value )
    {
        return ASTJ.astUnFormat( axis, plot, value );
    }


    /**
     * Convert a coordinate value into a formatted String suitable for a given
     * axis (could be celestial coordinates for example).
     *
     * @param axis the axis to use for formatting rules.
     * @param value the value.
     * @param plot AST plot that defines the coordinate formats.
     * @return the formatted value.
     */
    public String format( int axis, Plot plot, double value )
    {
        return ASTJ.astFormat( axis, plot, value );
    }

//
// Column names and control (optional within the implementation).
//
    /**
     * Return if the implementation supports the modification of
     * columns.
     */
    public boolean isColumnMutable()
    {
        return ( getColumnNames() != null );
    }

    /**
     * Return the names of all the available columns, if they can be
     * modified. Returns null otherwise.
     */
    public String[] getColumnNames()
    {
        return impl.getColumnNames();
    }

    /**
     * Return the name of the column associated with the coordinates.
     * If modification is not supported then this name is only symbolic.
     */
    public String getXDataColumnName()
    {
        return impl.getCoordinateColumnName();
    }

    /**
     * Set the name of the column associated with the coordinates. If
     * the underlying implementation supports it this will result in a
     * modification of the coordinate system and coordinate values.
     */
    public void setXDataColumnName( String name )
        throws SplatException
    {
        setXDataColumnName( name, false );
    }

    /**
     * Set the name of the column associated with the coordinates. If
     * the underlying implementation supports it this will result in a
     * modification of the coordinate system and coordinate values.
     * The update to the AST coordinate systems can be suppressed using this
     * method, only do this if you know an update will happen anyway.
     */
    public void setXDataColumnName( String name, boolean updateAST )
        throws SplatException
    {
        if ( isColumnMutable() ) {
            String currentName = getXDataColumnName();
            if ( ! currentName.equals( name ) ) {
                impl.setCoordinateColumnName( name );
                if ( updateAST ) {
                    initialiseAst();
                }
            }
        }
    }

    /**
     * Return the name of the column associated with the data values.
     * If modification is not supported then this name is only symbolic.
     */
    public String getYDataColumnName()
    {
        return impl.getDataColumnName();
    }

    /**
     * Set the name of the column associated with the data values. If
     * the underlying implementation supports it this will result in a
     * modification of values. Returns true if the name didn't match the
     * previous value, and names are mutable (in this instance a re-read of
     * the data has occurred and the coordinate systems have been updated).
     */
    public boolean setYDataColumnName( String name )
        throws SplatException
    {
        if ( isColumnMutable() ) {
            String currentName = getYDataColumnName();
            if ( ! currentName.equals( name ) ) {
            	// and146: tady se nastavuje vse kolem osy y
                impl.setDataColumnName( name );
                readData();
                return true;
            }
        }
        return false;
    }

    /**
     * Return the name of the column associated with the data errors.
     * If modification is not supported then this name is only symbolic.
     */
    public String getYDataErrorColumnName()
    {
        return impl.getDataErrorColumnName();
    }

    /**
     * Set the name of the column associated with the data errors. If
     * the underlying implementation supports it this will result in a
     * modification of values.
     */
    public void setYDataErrorColumnName( String name )
        throws SplatException
    {
        if ( isColumnMutable() ) {
            String currentName = getYDataErrorColumnName();
            if ( ! currentName.equals( name ) ) {
                impl.setDataErrorColumnName( name );
                yErr = impl.getDataErrors();
            }
        }
    }

//
//  AnalyticSpectrum implementation.
//     TODO: flux conversation and error estimation, proper model
//     types (i.e. use another object to compute values).
//
    /**
     * Return the value of the spectrum at an arbitrary X position.
     *
     * @param x the coordinate at which to evaluate this spectrum.
     * @return data value of this spectrum at the given coordinate.
     */
    public double evalYData( double x )
    {
        //  Locate the bounds of the point.
        int[] bounds = bound( x );
        int low = bounds[0];
        int high = bounds[1];
        if ( low == high ) {
            return yPos[low];
        }
        else if ( yPos[low] == SpecData.BAD || yPos[high] == SpecData.BAD ) {
            return SpecData.BAD;
        }
        else {
            if ( low > high ) {
                int tmp = low;
                low = high;
                high = tmp;
            }

            //  Interpolate a data value;
            double m = ( yPos[low] - yPos[high] ) / ( xPos[low] - xPos[high] );
            return x * m + ( yPos[low] - ( xPos[low] * m ) );
        }
    }


    /**
     * Return the value of the spectrum evaluated at series of arbitrary X
     * positions.
     *
     * @param x the coordiantes at which to evaluate this spectrum.
     * @return data values of this spectrum at the given coordinates.
     */
    public double[] evalYDataArray( double[] x )
    {
        double[] y = new double[x.length];
        for ( int i = 0; i < x.length; i++ ) {
            y[i] = evalYData( x[i] );
        }
        return y;
    }

//
//  Serializable interface.
//
    /**
     * Store this object in a serialized state. The main purpose of providing
     * this method is to also accommodate the serialization of the current
     * implementation FrameSet (by AST).
     */
    private void writeObject( ObjectOutputStream out )
        throws IOException
    {
        //  Serialize the AST FrameSet.
        serializedFrameSet = new String[1];
        ASTChannel chan = new ASTChannel( serializedFrameSet );
        chan.write( impl.getAst() );
        serializedFrameSet = new String[chan.getIndex()];
        chan.setArray( serializedFrameSet );
        chan.write( impl.getAst() );

        //  Store data units and label.
        serializedDataUnits = getCurrentDataUnits();
        serializedDataLabel = getDataLabel();

        //  Store signficant axis, so correct axis of the WCS is
        //  used on restoration.
        serializedSigAxis = getMostSignificantAxis();

        //  And store all member variables.
        out.defaultWriteObject();

        //  Finished.
        serializedFrameSet = null;
        serializedDataUnits = null;
        serializedDataLabel = null;
    }

    /**
     * Restore an object from a serialized state. Note all objects restored by
     * this route are assigned a MEMSpecDataImpl object, as the association
     * between a disk file and deserialized object cannot be guaranteed
     * (TODO: might like to store some details about the original file
     * somewhere). This method is also necessary so that AST objects can be
     * restored.
     *
     * @param in the serialized stream containing object of this class.
     * @exception IOException Description of the Exception
     * @exception ClassNotFoundException Description of the Exception
     */
    private void readObject( ObjectInputStream in )
        throws IOException, ClassNotFoundException
    {
        try {
            // Restore state of member variables.
            in.defaultReadObject();

            //  Create the backing impl.
            MEMSpecDataImpl newImpl = new MEMSpecDataImpl( shortName );
            fullName = null;

            //  Restore the AST FrameSet, if available.
            if ( serializedFrameSet != null ) {
                ASTChannel chan = new ASTChannel( serializedFrameSet );
                FrameSet frameSet = (FrameSet) chan.read();
                serializedFrameSet = null;

                if ( serializedDataUnits != null ) {
                    newImpl.setDataUnits( serializedDataUnits );
                    serializedDataUnits = null;
                }
                if ( serializedDataLabel != null ) {
                    newImpl.setDataLabel( serializedDataLabel );
                    serializedDataLabel = null;
                }

                if ( haveYDataErrors() ) {
                    newImpl.setFullData( frameSet, newImpl.getDataUnits(),
                                         getYData(), getYDataErrors() );
                }
                else {
                    newImpl.setFullData( frameSet, newImpl.getDataUnits(),
                                         getYData() );
                }
            }
            else {
                if ( haveYDataErrors() ) {
                    newImpl.setSimpleData( getXData(), newImpl.getDataUnits(),
                                           getYData(), getYDataErrors() );
                }
                else {
                    newImpl.setSimpleData( getXData(), newImpl.getDataUnits(),
                                           getYData() );
                }
            }
            this.impl = newImpl;

            //  Full reset of state.
            readDataPrivate();
        }
        catch ( SplatException e ) {
            e.printStackTrace();
        }
    }
}
