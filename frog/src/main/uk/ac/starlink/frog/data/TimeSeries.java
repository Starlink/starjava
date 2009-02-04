package uk.ac.starlink.frog.data;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;

import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.Grf;
import uk.ac.starlink.ast.Mapping;
import uk.ac.starlink.ast.Plot;
import uk.ac.starlink.frog.ast.AstUtilities;
import uk.ac.starlink.frog.data.Ephemeris;
import uk.ac.starlink.frog.util.FrogException;
import uk.ac.starlink.frog.util.FrogDebug;
import uk.ac.starlink.ast.grf.DefaultGrf;
import uk.ac.starlink.ast.grf.DefaultGrfMarker;
import uk.ac.starlink.ast.grf.DefaultGrfState;

/**
 * TimeSeries defines an interface for general access to time series datasets 
 * of differing fundamental data types and is the main data model used in FROG.
 * <p>
 *
 * It uses a derived class of TimeSeriesImpl to a supported data format (i.e.
 * FITS, NDF and text files) to give generalised access to:
 * <ul>
 *   <li> the time series data
 *   <li> the associated data errors
 *   <li> the coordinate of any data point
 *   <li> the time series properties (i.e. related values)
 * </ul>
 * <p>
 *
 * It should always be used when dealing with time series data to avoid any
 * seriesialised knowledge of the data format. <p>
 *
 * Missing data, or gaps in the series, are indicated using the seriesial
 * value TimeSeries.BAD. Generally useful code should always test for this in
 * the data values (otherwise you'll see numeric problems as BAD is the lowest
 * possible double value). <p>
 *
 *
 * @author Peter W. Draper
 * @author Alasdair Allan
 * @created May 31, 2002
 * @version $Id$
 * @see TimeSeriesImpl
 * @see TimeSeriesFactory
 * @see TimeSeriesAccess
 * @see AnalyticSeries
 * @see "The Bridge Design Pattern"
 */
public class TimeSeries
     implements TimeSeriesAccess, AnalyticSeries, Serializable
{

    /**
     *  Application wide debug manager
     */
    protected FrogDebug debugManager = FrogDebug.getReference();


    //  =============
    //  Constructors.
    //  =============

    /**
     * Create an instance using the data in a given TimeSeriesImpl object.
     *
     * @param impl a concrete implementation of a TimeSeriesImpl class that is
     *      accessing series data in of some format.
     * @exception FrogException thrown if there are problems obtaining
     *      series.
     */
    public TimeSeries( TimeSeriesImpl impl ) throws FrogException
    {
    
        debugManager.print( "                TimeSeries( impl )");
        this.impl = impl;
        shortName = impl.getShortName();
        fullName = impl.getFullName();
        readData();
    }


    /**
     * Finalise object. Free any resources associated with member variables.
     *
     * @exception Throwable Description of the Exception
     */
    protected void finalize() throws Throwable
    {
        this.impl = null;
        this.views = null;
        this.xPos = null;
        this.yPos = null;
        this.yErr = null;
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
     * Set or query alpha blending fraction.
     */
    public final static int LINE_ALPHA_BLEND = 4;

    /**
     * Set or query error bar colour.
     */
    public final static int ERROR_COLOUR = 5;

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
     * Use data point only plotting style.
     */
    public final static int POINTS = 3;

    //
    //  Types of series data. The default is UNCLASSIFIED.
    //  Nothing is done with this information here, it's just for
    //  external tagging. USERTYPE is just a spare.
    //

    /**
     * Series is unclassified.
     */
    public final static int UNCLASSIFIED = 0;

    /**
     * Series is a Time Series
     */
    public final static int TIMESERIES = 1; 
 
    /**
     * Series is a Folded Time Series
     */
    public final static int FOLDED = 2;      
 
    /**
     * Series is a Binned & Folded Time Series
     */
    public final static int BINFOLDED = 3;  
        
    /**
     * Series is Fit to a Time Series
     */
    public final static int FITTOSERIES = 4;
         
    /**
     * Series is Detrended to a Time Series
     */
    public final static int DETRENDED = 5;
         
    /**
     * Series is Detrend polynomial fit to a Time Series
     */
    public final static int POLYNOMIAL = 6;    

    /**
     * Series is fake data
     */
    public final static int FAKEDATA = 7;  
    
    /**
     * Series is a sin() + cos() fit
     */
    public final static int SINCOSFIT = 8;        
  
    
    //  ===================
    //  Protected variables
    //  ===================

    /**
     * Reference to the data access adaptor. This object is not kept during
     * serialisation as all restored objects use a memory implementation (disk
     * file associations are difficult to maintain).
     */
    protected transient TimeSeriesImpl impl = null;

   /** 
    * Ephemeris object associated with this teim series, should only be present
    * for TimeSeries objects of type FOLDED and BINFOLDED.
    */
    protected Ephemeris ephem = null;

   /** 
    * Sin Fit object associated with a time series, should only be present
    * for TimeSeries objects of type SINCOSFIT.
    */
    protected SinFit sinFit = null;

   /** 
    * The name of the TimeSeries this TimeSeries was derived from, or 
    * "File" if none. The TimeSeries can then be found from an instance 
    * of the TimeSeriesManager using this as a key
    */
    protected String origin = "a File";
    
    /**
     * References to all known views (Plots) of this series. Obviously this
     * information must be lost when restoring the object from a stream.
     */
    protected transient ArrayList views = new ArrayList();

    /**
     * The X data values for the series.
     */
    protected double[] xPos = null;

    /**
     * The Y data values for the series.
     */
    protected double[] yPos = null;

    /**
     * The Y data errors for the series.
     */
    protected double[] yErr = null;
    
   /**
    * Nyquist frequency for the series
    */ 
    protected double nyquist;

    /**
     * Whether the timeseries data is actually in magnitudes and therefore
     * should be plotted with the y-axis flipped.
     */
    protected boolean yFlipped = false;

   /**
     * At some point in the past this dataset has been detrended
     */
    protected boolean detrend = false;

    /**
     * Symbolic name of the series.
     */
    protected String shortName = null;

    /**
     * Full name of the series.
     */
    protected String fullName = null;

    /**
     * The range of coordinates spanned (i.e. min/max values in xPos and
     * yPos).
     */
    protected double[] range = new double[4];

    /**
     * The full range of coordinates spanned (i.e. min/max values in xPos and
     * yPos, plus the standard deviations in yPos).
     */
    protected double[] fullRange = new double[4];

    /**
     * Reference to AstUtilities object that contains coordinates 
     * associated with the series. Marked "transient" as much of this state
     * information is stored in AST structures, so cannot be restored from a
     * serialized stream.
     */
    protected transient AstUtilities astJ = null;

    /**
     * The "graphics" line thickness.
     */
    protected double lineThickness = 1.0;

    /**
     * The "graphics" line style.
     */
    protected double lineStyle = 1.0;

    /**
     * The "graphics" line alpha blend fraction.
     */
    protected double alphaBlend = 1.0;

    /**
     * The "graphics" line colour.
     */
    protected double lineColour = (double) Color.blue.getRGB();

    /**
     * The colour used to drawn error bars.
     */
    protected double errorColour = (double) Color.red.getRGB();

    /**
     * The series plot style.
     */
    protected int plotStyle = POINTS;

    /**
     * The series marker style.
     */ 
    protected int plotMarker = 7; // defaults to DefaultGrFMarker.FILLEDSQUSRE
     
   /**
     * The size of the marker drawn in plotStyle POINTS
     */ 
    protected double markerSize = 5.00; // defaults to 5.0 times the standard
     
    /**
     * Whether error bars should be drawn.
     */
    protected boolean drawErrorBars = true;

    /**
     * Whether this series should be used when auto-ranging (certain classes
     * of series, i.e. generated ones, could be expected to have artificial
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
     * The type of data stored in this series. This is purely symbolic and
     * is meant for use in organisational duties.
     */
    protected int type = UNCLASSIFIED;


    //  ==============
    //  Public methods
    //  ==============

    /**
     * Get the full name for series (cannot edit, this is usually the
     * filename).
     *
     * @return the full name.
     */
    public String getFullName()
    {
        return fullName;
    }


    /**
     * Get a symbolic name for series.
     *
     * @return the short name.
     */
    public String getShortName()
    {
        return shortName;
    }


    /**
     * Change the symbolic name of a series.
     *
     * @param shortName new short name for the series.
     */
    public void setShortName( String shortName )
    {
        this.shortName = shortName;
    }

    /**
     * Get the detrended data flag
     *
     * @return detrend whether the data has been detrended
     */
    public boolean getDetrend()
    {
        return detrend;
    }


    /**
     * Change the detrended data flag
     *
     * @param detrend whether the data has been detrended
     */
    public void setDetrend( boolean b )
    {
        detrend = b;
    }

    /**
     * Get references to series X data (i.e. the coordinates).
     *
     * @return reference to series X data.
     */
    public double[] getXData()
    {
        return xPos;
    }


    /**
     * Get references to series Y data (i.e. the data values).
     *
     * @return reference to series Y data.
     */
    public double[] getYData()
    {
        return yPos;
    }


    /**
     * Get references to series Y data errors (i.e. the data errors).
     *
     * @return reference to series Y data errors.
     */
    public double[] getYDataErrors()
    {
        return yErr;
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
     * Get the ephemeris object associated with this TimeSeries, if any.
     *
     * @return ephem The ephemeris object
     * @see Ephemeris
     */
    public Ephemeris getEphemeris()
    {
        return ephem;
    }

   /**
     * Get the SinFit object associated with this TimeSeries, if any.
     *
     * @return sinfit The SinFit object
     * @see SinFit
     */
    public SinFit getSinFit()
    {
        return sinFit;
    }
    
     /**
      * Set if the Y axis should be flipped.
      */
     public void setYFlipped( boolean yFlipped )
     {
         this.yFlipped = yFlipped;
     }
     
    /**
     * Check to see whether the data in is magnitudes.
     */
     public boolean isInMags()
     {
       return yFlipped;
     }

   /**
     * Set the ephemeris object associated with this TimeSeries.
     *
     * @param ephem The ephemeris object to be assocaited with the series
     * @see Ephemeris
     */
    public void setEphemeris( Ephemeris e )
    {
        ephem = e;
    }

   /**
     * Set the sin fit object associated with this TimeSeries.
     *
     * @param fit The sin fit object to be assocaited with the series
     * @see SinFit
     */
    public void setSinFit( SinFit fit )
    {
        sinFit = fit;
    }
    
    /**
     * Save the series to disk (if supported). Uses the current state of the
     * series (i.e. any file names etc.) to decide how to do this.
     *
     * @exception FrogException thown if an error occurs during save.
     */
    public void save() throws FrogException
    {
        impl.save();
    }


    /**
     * Create a new series by extracting sections of this series. The
     * section extents are defined in physical coordinates. Each section will
     * not contain any values that lie outside of its physical coordinate
     * range. <p>
     *
     * The series created here is not added to any lists or created with any
     * configuration other than the default values (i.e. you must do this part
     * yourself) and is only keep in memory.
     *
     * @param name short name for the series.
     * @param ranges an array of pairs of physical coordinates. These define
     *      the extents of the ranges to extract.
     * @return a series that contains data only from the given ranges. May
     *      be null if no values can be located.
     */
    public TimeSeries getSect( String name, double[] ranges )
    {
        //  Locate the index of the positions just below and above our
        //  physical value pairs and count the number of values that
        //  will be extracted.
        int nRanges = ranges.length / 2;
        int[] lower = new int[nRanges];
        int[] upper = new int[nRanges];
        int[] low;
        int[] high;
        int nvals = nRanges - 1;
        // for gaps.
        for ( int i = 0, j = 0; j < nRanges; i += 2, j++ ) {
            low = bound( ranges[i] );
            lower[j] = low[1];
            high = bound( ranges[i + 1] );
            upper[j] = high[0];
            nvals += high[0] - low[1] + 1;
        }
        if ( nvals == 0 ) {
            return null;
        }

        //  Copy extracted values.
        double[] newCoords = new double[nvals];
        double[] newData = new double[nvals];
        int k = 0;
        int length = 0;
        for ( int j = 0; j < nRanges; j++ ) {
            if ( j != 0 ) {

                //  Need a gap in the series.
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

        //  And create the memory series.
        MEMTimeSeriesImpl memSeriesImpl = new MEMTimeSeriesImpl( name );
        if ( newErrors == null ) {
            memSeriesImpl.setData( newData, newCoords );
        }
        else {
            memSeriesImpl.setData( newData, newCoords, newErrors );
        }
        try {
            return new TimeSeries( memSeriesImpl );
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
        return null;
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
     * Get the data ranges in the X and Y axes with standard deviation.
     *
     * @return reference to array of 4 values, xlow, xhigh, ylow, yhigh.
     */
    public double[] getFullRange()
    {
        return fullRange;
    }


    /**
     * Get reference to AstUtilities object set up to seriesify coordinate
     * transformations.
     *
     * @return AstUtilities object describing axis transformations.
     */
    public AstUtilities getAst()
    {
        return astJ;
    }


    /**
     * Get the size of the series.
     *
     * @return number of coordinate positions in series.
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
     * Return the TimeSeriesImpl object so that it can expose very data
     * specific methods (if needed, you really shouldn't use this).
     *
     * @return TimeSeriesImpl object defining the data access used by this
     *      instance.
     */
    public TimeSeriesImpl getTimeSeriesImpl()
    {
        return impl;
    }


    /**
     * Set the thickness of the line used to plot the series.
     *
     * @param lineThickness the line width to be used when plotting.
     */
    public void setLineThickness( double lineThickness )
    {
        this.lineThickness = lineThickness;
    }


    /**
     * Get the width of the line to be used when plotting the series. The
     * meaning of this value isn't defined.
     *
     * @return the line thickness.
     */
    public double getLineThickness()
    {
        return lineThickness;
    }


    /**
     * Set the style of the line used to plot the series.
     *
     * @param lineStyle the line style to be used when plotting.
     */
    public void setLineStyle( double lineStyle )
    {
        this.lineStyle = lineStyle;
    }


    /**
     * Get the line type to be used when plotting the series. The meaning of
     * this value is not defined.
     *
     * @return the line style in use.
     */
    public double getLineStyle()
    {
        return lineStyle;
    }


    /**
     * Set the alpha blending fraction of the line used plot the series.
     *
     * @param alphaBlend the alpha belanding fraction (0.0 to 1.0).
     */
    public void setAlphaBlend( double alphaBlend )
    {
        this.alphaBlend = alphaBlend;
    }


    /**
     * Get the alpha blending fraction.
     *
     * @return the current alpha blending fraction.
     */
    public double getAlphaBlend()
    {
        return alphaBlend;
    }


    /**
     * Set the colour index to be used when plotting the series.
     *
     * @param lineColour the colour as an RGB integer.
     */
    public void setLineColour( int lineColour )
    {
        this.lineColour = lineColour;
    }


    /**
     * Get the colour of the line to be used when plotting the series.
     *
     * @return the line colour (an RGB integer).
     */
    public double getLineColour()
    {
        return lineColour;
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
    public double getErrorColour()
    {
        return errorColour;
    }


    /**
     * Set the type of series lines that are drawn (these can be polylines
     * or histogram-like, simple markers are a possibility for a future
     * implementation). The value should be one of the symbolic constants
     * "POLYLINE" and "HISTOGRAM" or "POINTS".
     *
     * @param style one of the symbolic contants TimeSeries.POLYLINE or
     *      TimeSeries.HISTOGRAM.
     */
    public void setPlotStyle( int style )
    {
        this.plotStyle = style;
    }
 
   /**
     * Set the type marker drawn when ploting using plotStyle POINTS
     *
     * @param i int representing the marker type, one of the symbolic
     *        constants from DefaultGrfMarker, e.g. FOT or CROSS
     */
    public void setMarkStyle( int style )
    {
        this.plotMarker = style;
    }
 
   /**
     * Get the type current marker drawn when ploting using plotStyle POINTS
     *
     * @return i int representing the marker type, one of the symbolic
     *        constants from DefaultGrfMarker, e.g. FOT or CROSS
     */
    public int getMarkStyle(  )
    {
        return plotMarker;
    }

   /**
     * Set the size of the marker drawn when ploting using plotStyle POINTS
     *
     * @param size double representing the marker size.
     */
    public void setMarkSize( double size )
    {
       this.markerSize = size;

    }
 
   /**
     * Get the size of the marker drawn when ploting using plotStyle POINTS
     *
     * @return size double representing the marker size
     */
    public double getMarkSize( )
    {
        return markerSize;
    }

    /**
     * Return the nyquist frequency for the time series
     */
     public double getNyquist() 
     { 
         return nyquist;
     }     
    
    /**
     * Get the value of plotStyle.
     *
     * @return the current plotting type (TimeSeries.POLYLINE or
     *      TimeSeries.HISTOGRAM).
     */
    public int getPlotStyle()
    {
        return plotStyle;
    }


    /**
     * Set a known numeric series property.
     *
     * @param what either LINE_THICKNESS, LINE_STYLE, LINE_COLOUR, PLOT_STYLE,
     *      LINE_ALPHA_BLEND or ERROR_COLOUR.
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
            case LINE_ALPHA_BLEND:
            {
                setAlphaBlend( value.doubleValue() );
                break;
            }
            case ERROR_COLOUR:
            {
                setErrorColour( value.intValue() );
                break;
            }
        }
    }


    /**
     * Set the type of the series data.
     *
     * @param type either UNCLASSIFIED, TIMESERIES, FOLDED, BINFOLDED,
     *                    FITTOSERIES, etc.
     */
    public void setType( int type )
    {
        this.type = type;
    }


    /**
     * Get the origin of the series data.
     *
     * @return origin the associated original TimeSeries (or "File")
     */
    public String getOrigin()
    {
        return origin;
    }


    /**
     * Set the origin of the series data.
     *
     * @param origin the associated original TimeSeries (or "a File", 
     *               "a SOAP message" )
     */
    public void setOrigin( String s )
    {
        this.origin = s;
    }


    /**
     * Get the type of the series data.
     *
     * @return type either UNCLASSIFIED, TIMESERIES, FOLDED, BINFOLDED,
     *                     FITTOSERIES, etc
     */
    public int getType()
    {
        return type;
    }

    /**
     * Set whether the series should have its errors drawn as error bars, or
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
     * Set whether the series should be used when deriving auto-ranged
     * values.
     *
     * @param state if true then this series should be used when determining
     *      data limits (artificial series could have unreasonable ranges
     *      when extrapolated).
     */
    public void setUseInAutoRanging( boolean state )
    {
        this.useInAutoRanging = state;
    }


    /**
     * Find out if we should be used when determining an auto-range.
     *
     * @return whether this series is used when auto-ranging.
     */
    public boolean isUseInAutoRanging()
    {
        return useInAutoRanging;
    }


    /**
     * Read the data from the series into local arrays. This also
     * initialises a suitable AST frameset to describe the coordinate system
     * in use and establishes the minimum and maximum ranges for both
     * coordinates (X, i.e. the time stamp and Y, i.e. data count).
     *
     * @exception FrogException thrown if coordinate are not invertable (i.e.
     *      not monotonic).
     */
    protected void readData() throws FrogException
    {

        debugManager.print( "                  TimeSeries readData()");

        //  Get the series data counts and errors (can be null).
        try {
            yPos = impl.getData();
            yErr = impl.getDataErrors();
        }
        catch (RuntimeException e) {
            // None seriesific errors, like no data...
            throw new FrogException( "Failed to read data: " + 
                                      e.getMessage() );
        }
        
        debugManager.print( "");
        for ( int i = 0; i < yPos.length; i++ ) {
                if ( yErr != null ) {
                   debugManager.print( "                  yPos" + i +
                                       ": " +  yPos[i] + "    " + yErr[i] );
                } else {
                   debugManager.print( "                  yPos" + i +
                                       ": " +  yPos[i]  );                                         } 
        }                 

        // Check we have some data.
        if ( yPos == null ) { 
            throw new FrogException( "Series does not contain any data" );
        }

        //  Now get the data value range.
        double yMin = Double.MAX_VALUE;
        double yMax = Double.MIN_VALUE;
        double fullYMin = Double.MAX_VALUE;
        double fullYMax = Double.MIN_VALUE;
        if ( yErr != null ) {
            for ( int i = 0; i < yPos.length; i++ ) {
                if ( yPos[i] != TimeSeries.BAD ) {
                    yMin = Math.min( yMin, yPos[i] );
                    yMax = Math.max( yMax, yPos[i] );
                    fullYMin = Math.min( fullYMin, yPos[i] -
                        ( yErr[i] * 0.5 ) );
                    fullYMax = Math.max( fullYMax, yPos[i] +
                        ( yErr[i] * 0.5 ) );
                }
            }
        }
        else {
            for ( int i = 0; i < yPos.length; i++ ) {
                if ( yPos[i] != TimeSeries.BAD ) {
                    yMin = Math.min( yMin, yPos[i] );
                    yMax = Math.max( yMax, yPos[i] );
                }
            }
        }

        //  Create the init time stamp positions as simple
        //  increasing vector (this places them at centre of pixels).
        xPos = new double[yPos.length];
        for ( int i = 0; i < yPos.length; i++ ) {
            xPos[i] = (double) ( i + 1.0 );
        }
        double xMin = 1.0;
        double xMax = (double)yPos.length + 1.0;

        //  Get the dimensionality of the series. If more than 1 we
        //  need to pick out a significant axis to work with.
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

        //  Get the AST component (this should define the wavelength
        //  axis coordinates)
        FrameSet astref = impl.getAst();
        
        if ( astref == null ) {
            System.err.println( "series has no coordinate information" );
        }
        else {
  
            //  Create the required AstUtilities object to manipulate the AST
            //  frameset.
            AstUtilities ast = new AstUtilities( astref );

            //  Create a frameset that is suitable for displaying a
            //  "series". This has a coordinate X axis and a data Y
            //  axis. The coordinates are chosen to run along the
            //  sigaxis (if input data has more than one dimension)
            //  and may be a distance, rather than absolute coordinate.
            FrameSet seriesref = ast.makeSeries( sigaxis, 0, yPos.length,
                impl.getProperty( "label" ),
                impl.getProperty( "units" ),
                false );
            astJ = new AstUtilities( seriesref, true );

            //  Get the mapping for the X axis and check that it is
            //  useful, i.e. we can invert it.
            Mapping oned = astJ.get1DMapping( 1 );

            //  Get the centres of the pixel positions in current
            //  coordinates (so we can eventually go from current
            //  coordinates through to graphics coordinates when
            //  actually drawing the series).
            double [] tPos = oned.tran1( xPos.length, xPos, true);
            xPos = tPos;
            tPos = null;
            
            // verbose if in debug mode                   
            debugManager.print( "");
            for ( int i = 0; i < yPos.length; i++ ) {
                debugManager.print( "                  xPos" + i +
                                    ": " +  xPos[i]  );
            }  
                
            // set up a stupidly high nyquist value
            nyquist = Double.MAX_VALUE;
            for( int i = 1; i < xPos.length; i++ ) {
               nyquist = Math.min( nyquist, ( xPos[i] - xPos[i-1] ) ); 
            }
            nyquist = 1.0 / ( 2.0 * nyquist );
        
            if (debugManager.getDebugFlag()) {                    
               double xData[] = impl.getTime();                         
               double yData[] = impl.getData();   
               double errors[] = impl.getDataErrors();
               debugManager.print( "" );
               for ( int i = 0; i < xData.length; i++ ) {
                  if( errors != null ) {
                     debugManager.print( "                  Impl" + i + ": " + 
                         xData[i] + "    " + yData[i] + "    " + errors[i] );
                  } else {
                      debugManager.print( "                  Impl" + i + ": " + 
                         xData[i] + "    " + yData[i] );                      } 
               }                       
            } 
                          
            //  Get the axis range.
            xMin = Double.MAX_VALUE;
            xMax = Double.MIN_VALUE;
            for ( int i = 0; i < xPos.length; i++ ) {
                if ( xPos[i] != TimeSeries.BAD ) {
                    xMin = Math.min( xMin, xPos[i] );
                    xMax = Math.max( xMax, xPos[i] );
                }
            }
            if ( xMin == Double.MAX_VALUE ||
                xMax == Double.MIN_VALUE ) {
                xMin = 0.0;
                xMax = 0.0;
            }

            //  Record data ranges.
            range[0] = xMin;
            range[1] = xMax;
            range[2] = yMin;
            range[3] = yMax;

            fullRange[0] = xMin;
            fullRange[1] = xMax;
            if ( yErr != null ) {
                fullRange[2] = fullYMin;
                fullRange[3] = fullYMax;
            }
            else {
                fullRange[2] = yMin;
                fullRange[3] = yMax;
            }

            //  Add slack so that error bars do not abut the edges.
            double slack = ( fullRange[3] - fullRange[2] ) * SLACK;
            fullRange[2] = fullRange[2] - slack;
            fullRange[3] = fullRange[3] + slack;
        }
    }


    /**
     * Draw the series onto the given widget using a suitable AST GRF
     * object.
     *
     * @param grf Grf object that can be drawn into using AST primitives.
     * @param plot reference to AstPlot defining transformation from physical
     *      coordinates into graphics coordinates.
     * @param limits limits of the region to draw in physical coordinates
     *      (e.g. user defined ranges), used to clip graphics.
     */
    public void drawSeries( Grf grf, Plot plot, double[] limits )
    {
        //  Get a list of positions suitable for transforming.
        //  Note BAD value is same for graphics (AST, Grf) and data,
        //  so tests can be missed (should only be in yPos).
        double[] xypos = null;
        if ( plotStyle == POLYLINE || plotStyle == POINTS ) {
            xypos = new double[xPos.length * 2];
            for ( int i = 0, j = 0; j < xPos.length; j++, i += 2 ) {
                xypos[i] = xPos[j];
                xypos[i + 1] = yPos[j];
            }
        }
        else if (plotStyle == HISTOGRAM ) {
            //  Draw a histogram style plot. Need to generate a list
            //  of positions that can be connected like a normal
            //  series, but looks like a histogram. Create a list
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
            xypos[i + 1] = yPos[j];
            xypos[i + 3] = yPos[j];
            j++;
            i += 4;

            //  Do normal positions.
            for ( ; j < xPos.length - 1; j++, i += 4 ) {
                fwidth = ( xPos[j + 1] - xPos[j] ) * 0.5;
                bwidth = ( xPos[j] - xPos[j - 1] ) * 0.5;
                xypos[i] = xPos[j] - bwidth;
                xypos[i + 2] = xPos[j] + fwidth;
                xypos[i + 1] = yPos[j];
                xypos[i + 3] = yPos[j];
            }

            //  Until final final point which is also unpaired forward
            //  so use double backwards width.
            bwidth = ( xPos[j] - xPos[j - 1] ) * 0.5;
            xypos[i] = xPos[j] - bwidth;
            xypos[i + 2] = xPos[j] + bwidth;
            xypos[i + 1] = yPos[j];
            xypos[i + 3] = yPos[j];
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

        //  Do the same for the clip region.
        Rectangle cliprect = null;
        if ( limits != null ) {
            double[][] clippos = astJ.astTran2( plot, limits, false );
            cliprect =
                new Rectangle( (int) clippos[0][0],
                               (int) clippos[1][1],
                               (int) ( clippos[0][1] - clippos[0][0] ),
                               (int) ( clippos[1][0] - clippos[1][1] ) );
        }

        //  Plot using the GRF primitive, rather than astPolyCurve, as
        //  we are just plotting series and require only straight
        //  lines, not geodesics (this makes it much faster). Need to
        //  establish line properites first, draw polyline and then
        //  restore properties.
        DefaultGrf defaultGrf = (DefaultGrf) grf;
        DefaultGrfState oldState = setGrfAttributes( defaultGrf );

        defaultGrf.setClipRegion( cliprect );

        if( plotStyle == POINTS ) {
        
           // using markers rather than connecting together the points
           defaultGrf.attribute( defaultGrf.GRF__SIZE, 
                                 markerSize, defaultGrf.GRF__MARK );
                                 
           renderSeriesMark( defaultGrf, xpos, ypos );
           defaultGrf.attribute( Grf.GRF__COLOUR, errorColour, Grf.GRF__MARK );
           
           if( haveYDataErrors() ) {
              renderErrorBars( defaultGrf, plot );
           }   
           
        } else {
           
           // connect together the points using a POLYLINE
           renderSeriesLine( defaultGrf, xpos, ypos );
           defaultGrf.attribute( Grf.GRF__COLOUR, errorColour, Grf.GRF__LINE );
           if( haveYDataErrors() ) {
              renderErrorBars( defaultGrf, plot ); 
           }   
        }
                  
        defaultGrf.setClipRegion( null );

        resetGrfAttributes( defaultGrf, oldState );
    }


    /**
     * Draw the series using the current series plotting style
     * connecting together the points using a polyline
     *
     * @param grf DefaultGrf object that can be drawn into using AST
     *            primitives. 
     * @param xpos graphics X coordinates of series
     * @param ypos graphics Y coordinates of series
     */
    protected void renderSeriesLine( DefaultGrf grf, double[] xpos, 
                                     double[] ypos )
    {
        grf.polyline( xpos, ypos );
    }

    /**
     * Draw the series using the current series plotting style
     * using the current series plotting marker type.
     *
     * @param grf DefaultGrf object that can be drawn into using AST
     *            primitives. 
     * @param xpos graphics X coordinates of series
     * @param ypos graphics Y coordinates of series
     */
    protected void renderSeriesMark( DefaultGrf grf, double[] xpos,
                                     double[] ypos )
    {
        grf.marker( xpos, ypos, plotMarker );
    }

    /**
     * Draw error bars. These are quite simple lines above and below positions
     * by half a standard deviation, with a serif at each end.
     *
     * @param grf DefaultGrf object that can be drawn into using AST
     *            primitives. 
     * @param plot reference to AstPlot defining transformation from physical
     *             coordinates into graphics coordinates.
     */
    protected void renderErrorBars( DefaultGrf grf, Plot plot )
    {
        if ( drawErrorBars ) {
            double[] xypos = new double[4];
            double[][] xygpos = null;
            double[] xpos = null;
            double[] ypos = null;
            double half = 0.0;
            for ( int i = 0; i < xPos.length; i++ ) {
                if ( yErr[i] != TimeSeries.BAD ) {
                    half = yErr[i] * 0.5;
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
    protected DefaultGrfState setGrfAttributes( DefaultGrf grf )
    {
        DefaultGrfState oldState = new DefaultGrfState();
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
        grf.attribute( grf.GRF__ALPHA, alphaBlend, Grf.GRF__LINE );
        return oldState;
    }


    /**
     * Restore an existing Grf object to a given state.
     *
     * @param grf Grf object being used.
     * @param oldState the state to return Grf object to.
     */
    protected void resetGrfAttributes( DefaultGrf grf, 
                                       DefaultGrfState oldState )
    {
        grf.attribute( Grf.GRF__COLOUR, oldState.getColour(), Grf.GRF__LINE );
        grf.attribute( Grf.GRF__STYLE, oldState.getStyle(), Grf.GRF__LINE );
        grf.attribute( Grf.GRF__WIDTH, oldState.getWidth(), Grf.GRF__LINE );
        grf.attribute( grf.GRF__ALPHA, oldState.getAlpha(), Grf.GRF__LINE );
    }


    /**
     * Lookup the physical values (i.e. time stamp and data value) that
     * correspond to a graphics X coordinate.
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
        double[][] xyphys = AstUtilities.astTran2( plot, xypos, true );
        int[] bounds = bound( xyphys[0][0] );

        //  Find which position is nearest in reality.
        double[] result = new double[2];
        if ( ( xyphys[0][0] - xPos[bounds[0]] ) <
             ( xPos[bounds[1]] - xyphys[0][0] ) ) {
            result[0] = xPos[bounds[0]];
            result[1] = yPos[bounds[0]];
        }
        else {
            result[0] = xPos[bounds[1]];
            result[1] = yPos[bounds[1]];
        }
        return result;
    }


    /**
     * Locate the indices of the two coordinates that lie closest to a given
     * coordinate. In the case of an exact match then both indices are
     * returned as the same value.
     *
     * @param xcoord the coordinate value to bound.
     * @return array of two integers, the lower and upper indices.
     */
    public int[] bound( double xcoord )
    {
        //  Use a binary search as values should be sorted to increase
        //  in either direction (wavelength, pixel coordinates etc.).
        int low = 0;
        int high = xPos.length - 1;
        int mid = 0;
        if ( xPos[0] < xPos[high] ) {
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
        int bounds[] = new int[2];
        bounds[0] = low;
        bounds[1] = high;
        return bounds;
    }


    /**
     * Lookup the physical values (i.e. wavelength and data value) that
     * correspond to a graphics X coordinate. Value is returned as formatted
     * strings for the selected axis (could be sky coordinates for instance).
     *
     * @param xg X graphics coordinate
     * @param plot AST plot needed to transform graphics position into
     *      physical coordinates
     * @return array of two Strings, the formatted wavelength and data values
     */
    public String[] formatLookup( int xg, Plot plot )
    {
        double[] xypos = lookup( xg, plot );

        //  Let AST format the values as appropriate for the axes.
        String result[] = new String[2];
        result[0] = AstUtilities.astFormat( 1, plot, xypos[0] );
        result[1] = AstUtilities.astFormat( 2, plot, xypos[1] );
        return result;
    }


    /**
     * Return interpolated physical values (i.e. wavelength and data value)
     * that correspond to a graphics X coordinate.
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
        double[][] xyphys = AstUtilities.astTran2( plot, xypos, true );

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
        result[0] = AstUtilities.astFormat( 1, plot, xyphys[0][0] );
        result[1] = AstUtilities.astFormat( 2, plot, xyphys[1][0] );
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
        return AstUtilities.astUnFormat( axis, plot, value );
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
        return AstUtilities.astFormat( axis, plot, value );
    }


    /**
     * Add a Plot reference to the list of known views of this series.
     *
     * @param plot reference to a Plot
     */
    public void addPlot( Plot plot )
    {
        views.add( plot );
    }


    /**
     * Remove a Plot reference.
     *
     * @param plot reference to a Plot
     */
    public void removePlot( Plot plot )
    {
        views.remove( plot );
    }


    /**
     * Remove a Plot reference.
     *
     * @param index of the Plot
     */
    public void removePlot( int index )
    {
        views.remove( index );
    }


    /**
     * Get the number of plots currently using this series.
     *
     * @return number of plot currently using this series.
     */
    public int plotCount()
    {
        return views.size();
    }


//
//  AnalyticSeries implementation.
//
    /**
     * Return the value of the series at an arbitrary X position.
     *
     * @param x the coordiante at which to evaluate this series.
     * @return data value of this series at the given coordinate.
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
        else if ( yPos[low] == TimeSeries.BAD || yPos[high] == TimeSeries.BAD ) {
            return TimeSeries.BAD;
        }
        else {
            //  Interpolate a data value;
            double m = ( yPos[low] - yPos[high] ) / ( xPos[low] - xPos[high] );
            return x * m + ( yPos[low] - ( xPos[low] * m ) );
        }
    }


    /**
     * Return the value of the series evaluated at series of arbitrary X
     * positions.
     *
     * @param x the coordiantes at which to evaluate this series.
     * @return data values of this series at the given coordinates.
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
     * Restore a TimeSeries object from a serialized state. Note all objects
     * restored by this route are assigned a MEMTimeSeriesImpl object, as the
     * association between a disk file and deserialized object cannot be
     * guaranteed (TODO: might like to store some details about the original
     * file somewhere). This method is also necessary so that AST objects can
     * be restored.
     *
     * @param in the serialized stream containing object of this class.
     * @exception IOException Description of the Exception
     * @exception ClassNotFoundException Description of the Exception
     */
    private void readObject( ObjectInputStream in )
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        MEMTimeSeriesImpl newImpl = new MEMTimeSeriesImpl( shortName );
        fullName = null;
        if ( haveYDataErrors() ) {
            newImpl.setData( getYData(), getXData(), getYDataErrors() );
        }
        else {
            newImpl.setData( getYData(), getXData() );
        }
        this.impl = newImpl;
        try {
            readData();
        }
        catch ( FrogException e ) {
            e.printStackTrace();
        }
    }
}
