package uk.ac.starlink.ttools.plot2;

import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.PdfGraphicExporter;
import uk.ac.starlink.ttools.plot.Picture;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.layer.BinList;
import uk.ac.starlink.ttools.plot2.paper.PaperType;

/**
 * Miscellaneous utilities for use with the plotting classes.
 *
 * @author   Mark Taylor
 * @since    13 Feb 2013
 */
public class PlotUtil {

    private static Boolean dfltAntialias_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2" );

    /**
     * TupleSequence instance that contains no tuples.
     */
    public static final TupleSequence EMPTY_TUPLE_SEQUENCE =
            new TupleSequence() {
        public boolean next() {
            return false;
        }
        public long getRowIndex() {
            return -1L;
        }
        public Object getObjectValue( int icol ) {
            throw new IllegalStateException();
        }
        public double getDoubleValue( int icol ) {
            throw new IllegalStateException();
        }
        public int getIntValue( int icol ) {
            throw new IllegalStateException();
        }
        public boolean getBooleanValue( int icol ) {
            throw new IllegalStateException();
        }
    };

    /** Relative location of latex font location list. */
    private static final String LATEX_FONT_PATHS = "latex_fonts.txt";

    /** PDF GraphicExporter suitable for use with JLaTeXMath. */
    public static final PdfGraphicExporter LATEX_PDF_EXPORTER =
        PdfGraphicExporter
       .createExternalFontExporter( PlotUtil.class
                                   .getResource( LATEX_FONT_PATHS ) );

    /** Maximum distance from a click to a clicked-on position. */
    public static final double NEAR_PIXELS = 4.0;

    /** Maximum size for autoscaled variable-size markers. */
    public static final int DEFAULT_MAX_PIXELS = 20;

    /** Absolute maximum number of pixels per marker. */
    public static final short MAX_MARKSIZE = 100;

    /** Minimum number of input differences that fill up a colour ramp. */
    public static final int MIN_RAMP_UNIT = 12;

    /** Amount of padding added to data ranges for axis scaling. */
    private static final double PAD_FRACTION = 0.02;

    /** Level at which plot reports are logged. */
    private static final Level REPORT_LEVEL = Level.INFO;

    /**
     * Private constructor prevents instantiation.
     */
    private PlotUtil() {
    }

    /**
     * Compares two possibly null objects for equality.
     *
     * @param  o1  one object or null
     * @param  o2  other object or null
     * @return   true iff objects are equal or are both null
     */
    public static boolean equals( Object o1, Object o2 ) {
        return o1 == null ? o2 == null : o1.equals( o2 );
    }

    /**
     * Returns a hash code for a possibly null object.
     *
     * @param   obj  object or null
     * @return   hash value
     */
    public static int hashCode( Object obj ) {
        return obj == null ? 0 : obj.hashCode();
    }

    /**
     * Policy for whether to cache full precision coordinates.
     *
     * @return   if false, it's OK to truncate doubles to floats
     *           when it seems reasonable
     */
    public static boolean storeFullPrecision() {
        return true;
    }

    /**
     * Indicates whether antialiasing of text is turned on or off by
     * default.  There are a few considerations here.  Text generally
     * looks nicer antialiased, but it is noticeably slower to paint. 
     * Furthermore, there is a serious bug in OSX Java text rendering
     * (java 1.7+?) that means un-antialiased characters in non-horizontal
     * strings are drawn in the wrong order or wrong places or something
     * (text drawn vertically has the letters sdrawkcab, anyway);
     * painting it with antialiasing turned on works round this for
     * some reason.
     *
     * <p>So the current policy is to set the default true for OSX,
     * and false for other platforms.
     *
     * @return   default antialiasing for potentially non-horizontal text
     */
    public synchronized static boolean getDefaultTextAntialiasing() {
        if ( dfltAntialias_ == null ) {
            String os;
            try {
                os = System.getProperty( "os.name" );
            }
            catch ( SecurityException e ) {
                os = "?";
            }
            boolean isMac = os != null
                         && ( os.toLowerCase().indexOf( "macos" ) >= 0 ||
                              os.toLowerCase().indexOf( "mac os" ) >= 0 );
            dfltAntialias_ = Boolean.valueOf( isMac );
            logger_.info( "Use default text antialias setting "
                        + dfltAntialias_ + " (os.name=" + os + ")" );
        }
        return dfltAntialias_.booleanValue();
    }

    /**
     * Returns a suffix to append to one of a set of similar coordinate
     * names for disambiguation.
     * Where there are several sets of positional coordinates that would
     * otherwise have the same names, use this method to come up with
     * a consistent suffix.  It is only usual to invoke this method
     * if there are in fact multiple positions, if there's only one just
     * don't give it a suffix.
     * 
     * @param  ipos  zero-based position number
     * @return  suffix; currently <code>1+ipos</code>
     */
    public static String getIndexSuffix( int ipos ) {
        return Integer.toString( 1 + ipos );
    }

    /**
     * Writes message through the logging system
     * about the time a named step has taken.
     * The elapsed time is presumed to be the time between the supplied
     * time and the time when this method is called.
     * If the elapsed time is zero, nothing is logged.
     *
     * @param  logger   log message destination
     * @param  phase  name of step to log time of
     * @param  start   start {@link java.lang.System#currentTimeMillis
     *                              currentTimeMillis}
     */
    public static void logTime( Logger logger, String phase, long start ) {
        long time = System.currentTimeMillis() - start;
        if ( time > 0 ) {
            logger.info( phase + " time: " + time );
        }
    }

    /**
     * Concatenates two arrays to form a single one.
     *
     * @param  a1  first array
     * @param  a2  second array
     * @return  concatenated array
     */
    public static <T> T[] arrayConcat( T[] a1, T[] a2 ) {
        int count = a1.length + a2.length;
        List<T> list = new ArrayList<T>( count );
        list.addAll( Arrays.asList( a1 ) );
        list.addAll( Arrays.asList( a2 ) );
        Class eClazz = a1.getClass().getComponentType();
        @SuppressWarnings("unchecked")
        T[] result =
            (T[]) list.toArray( (Object[]) Array.newInstance( eClazz, count ) );
        return result;
    }

    /**
     * Concatentates lines, adding a newline character at the end of each.
     *
     * @param   lines  lines of text
     * @return  concatenation
     */
    public static String concatLines( String[] lines ) {
        int leng = 0;
        for ( String line : lines ) {
            leng += line.length() + 1;
        }
        StringBuffer sbuf = new StringBuffer( leng );
        for ( String line : lines ) {
            sbuf.append( line ).append( '\n' );
        }
        return sbuf.toString();
    }

    /**
     * Turns a Number object into a double primitive.
     * If the supplied value is null, Double.NaN is returned.
     *
     * @param  value  number object
     * @return   primitive value
     */
    public static double toDouble( Number value ) {
        return value == null ? Double.NaN : value.doubleValue();
    }

    /**
     * Indicates whether a value is a definite number.
     *
     * @param  value  value to test
     * @return  true iff <code>value</code> is non-NaN and non-infinite
     */
    public static boolean isFinite( double value ) {
        return ! Double.isNaN( value ) && ! Double.isInfinite( value );
    }

    /**
     * Determines whether both coordinates of a graphics position are
     * definite numbers.
     *
     * @param  gp  position to test
     * @return  true  iff X and Y coordinates are both non-NaN and non-infinite
     */
    public static boolean isPointFinite( Point2D.Double gp ) {
        return isFinite( gp.x ) && isFinite( gp.y );
    }

    /**
     * Determines whether both coordinates of a graphics position are
     * not NaNs.
     *
     * @param  gp  position to test
     * @return  true iff X and Y coordinates are both non-NaN
     */
    public static boolean isPointReal( Point2D.Double gp ) {
        return ! Double.isNaN( gp.x ) && ! Double.isNaN( gp.y );
    }

    /**
     * Maps a floating point graphics position to an integer graphics
     * position, that is a 2-dimensional grid cell index.
     * It does this by calling {@link #ifloor} on both coordinates.
     * The input coordinates must not be NaN.
     *
     * @param   dpos   input definite floating point graphics position
     * @param   gpos   output graphics position object
     */
    public static void quantisePoint( Point2D.Double dpos, Point gpos ) {
        gpos.x = ifloor( dpos.x );
        gpos.y = ifloor( dpos.y );
    }

    /**
     * Determines the integer not larger than a given non-NaN
     * floating point value.
     * 
     * @param  x  definite floating point value
     * @return  floor of input
     * @see   java.lang.Math#floor
     */
    public static int ifloor( double x ) {
        int y = (int) x;
        return x >= 0 || x == y || y == Integer.MIN_VALUE ? y : y - 1;
    }

    /**
     * Returns a 2-element array consisting of the two input values
     * in ascending order.  If either is NaN, behaviour is undefined.
     *
     * @param  p1  one value
     * @param  p2  other value
     * @return   2-element array [plo,phi]
     */
    public static double[] orderPair( double p1, double p2 ) {
        return p1 <= p2 ? new double[] { p1, p2 }
                        : new double[] { p2, p1 };
    }

    /**
     * Determines the union of the data bounds of zones in a gang.
     *
     * @param  gang  gang
     * @return   rectangle containing all data bounds rectangles in gang,
     *           or null if no zones
     */
    public static Rectangle getGangBounds( Gang gang ) {
        int nz = gang.getZoneCount();
        int xmin = Integer.MAX_VALUE;
        int ymin = Integer.MAX_VALUE;
        int xmax = Integer.MIN_VALUE;
        int ymax = Integer.MIN_VALUE;
        for ( int iz = 0; iz < nz; iz++ ) {
            Rectangle rect = gang.getZonePlotBounds( iz );
            xmin = Math.min( xmin, rect.x );
            ymin = Math.min( ymin, rect.y );
            xmax = Math.max( xmax, rect.x + rect.width );
            ymax = Math.max( ymax, rect.y + rect.height );
        }
        return xmax >= xmin && ymax >= ymin
             ? new Rectangle( xmin, ymin, xmax - xmin, ymax - ymin )
             : null;
    }

    /**
     * Returns a single-element array from an object with a parameterised type.
     * The array element type is taken from the runtime type of the single
     * element.
     *
     * @param   object  array element
     * @return   array containing element
     */
    public static <T> T[] singletonArray( T object ) {
        T[] array = (T[]) Array.newInstance( object.getClass(), 1 );
        array[ 0 ] = object;
        return array;
    }

    /**
     * Returns an empty array suitable (it has the right parameterised type)
     * for containing elements that are profiles for a given surface factory.
     *
     * @param   surfFact  surface factory
     * @param   length   array size
     * @return   new empty array
     */
    public static <P> P[] createProfileArray( SurfaceFactory<P,?> surfFact,
                                              int length ) {
        P profile = surfFact.createProfile( new ConfigMap() );
        P[] array = (P[]) Array.newInstance( profile.getClass(), length );
        return array;
    }

    /**
     * Returns an empty array suitable (it has the right parameterised type)
     * for containing elements that are aspects for a given surface factory.
     *
     * @param   surfFact  surface factory
     * @param   length   array size
     * @return   new empty array
     */
    public static <P,A> A[] createAspectArray( SurfaceFactory<P,A> surfFact,
                                               int length ) {
        ConfigMap config = new ConfigMap();
        P profile = surfFact.createProfile( config );
        A aspect = surfFact.createAspect( profile, config, null );
        A[] array = (A[]) Array.newInstance( aspect.getClass(), length );
        return array;
    }

    /**
     * Turns an Icon into a Picture.
     *
     * @param   icon   icon
     * @return  picture  picture
     */
    public static Picture toPicture( final Icon icon ) {
        return new Picture() {
            public int getPictureWidth() {
                return icon.getIconWidth();
            }
            public int getPictureHeight() {
                return icon.getIconHeight();
            }
            public void paintPicture( Graphics2D g2 ) {
                icon.paintIcon( null, g2, 0, 0 );
            }
        };
    }

    /**
     * Determines range information for a set of layers which have
     * Cartesian (or similar) coordinates.
     *
     * <p>The <code>logFlags</code> array can flag whether
     * some of the data dimensions have logarithmic scaling.
     * This may not make sense in all cases, if not, supply null.
     *
     * @param   layers   plot layers
     * @param   nDataDim  dimensionality of data points
     * @param   logFlags  <code>nDataDim</code>-element array indicating
     *                    whether data dimensions are
     *                    linear (false) or logarithmic (true)
     * @param   dataStore  data storage
     * @return   <code>nDataDim</code>-element array of ranges, each containing
     *           the range of data position coordinate values for
     *           the corresponding dimension
     */
    @Slow
    public static Range[] readCoordinateRanges( PlotLayer[] layers,
                                                int nDataDim,
                                                boolean[] logFlags,
                                                DataStore dataStore ) {

        /* Set up an array of range objects, one for each data dimension. */
        Range[] ranges = new Range[ nDataDim ];
        for ( int idim = 0; idim < nDataDim; idim++ ) {
            ranges[ idim ] = new Range();
        }

        /* Create a point cloud containing all the data coordinates
         * represented by the supplied layers.  If there are several
         * layers using the same basic positions, this will combine
         * them efficiently.  This includes two sets of subclouds:
         * firstly tuples giving actual point positions,
         * and secondly tuples that may only represent partial positions
         * (some elements of the data space coordinate vector are NaN). */
        SubCloud[] subClouds =
            arrayConcat( SubCloud.createSubClouds( layers, true ),
                         SubCloud.createPartialSubClouds( layers, true ) );
        PointCloud cloud = new PointCloud( subClouds );

        /* Iterate over the represented points to mark out the basic
         * range of data positions covered by the layers. */
        for ( double[] dpos : cloud.createDataPosIterable( dataStore ) ) {
            for ( int idim = 0; idim < nDataDim; idim++ ) {
                ranges[ idim ].submit( dpos[ idim ] );
            }
        }

        /* If any of the layers wants to supply non-data-position points
         * to mark out additional space, take account of those too. */
        boolean[] lflags = logFlags == null ? new boolean[ nDataDim ]
                                            : logFlags;
        for ( int il = 0; il < layers.length; il++ ) {
            layers[ il ].extendCoordinateRanges( ranges, lflags, dataStore );
        }

        /* Pad the ranges with a bit of space. */
        for ( int idim = 0; idim < nDataDim; idim++ ) {
            padRange( ranges[ idim ], logFlags[ idim ] );
        }

        /* Return the ranges. */
        return ranges;
    }

    /**
     * Pads a data range to provide a bit of extra space at each end.
     * If one of the limits is near to zero, it is padded to zero
     * instead of adding a fixed amount.
     * A standard padding fraction is used.
     *
     * @param  range  range to pad
     * @param  logFlag  true for logarithmic scaling, false for linear
     */
    public static void padRange( Range range, boolean logFlag ) {
        double[] bounds = range.getBounds();
        double lo = bounds[ 0 ];
        double hi = bounds[ 1 ];
        if ( lo < hi ) {
            double padFrac = PAD_FRACTION;
            final boolean loNearZero;
            final boolean hiNearZero;
            if ( logFlag ) {
                loNearZero = false;
                hiNearZero = false;
            }
            else {
                double ztol = 2 * padFrac;
                double zfrac = unscaleValue( lo, hi, 0., logFlag );
                loNearZero = 0 - zfrac >= 0 && 0 - zfrac <= ztol;
                hiNearZero = zfrac - 1 >= 0 && zfrac - 1 <= ztol;
            }
            range.submit( loNearZero
                          ? 0
                          : scaleValue( lo, hi, 0 - padFrac, logFlag ) );
            range.submit( hiNearZero
                          ? 0
                          : scaleValue( lo, hi, 1 + padFrac, logFlag ) );
        }
    }

    /**
     * Modifies a supplied range object by submitting the values in the
     * bins of a given BinList.Result.
     * 
     * @param  range  range to extend
     * @param  binResult  bin data
     */
    public static void extendRange( Range range, BinList.Result binResult ) {
        for ( Iterator<Long> it = binResult.indexIterator(); it.hasNext(); ) {
            range.submit( binResult.getBinValue( it.next().longValue() ) );
        }
    }

    /**
     * Scans a tuple sequence to identify the data point which is
     * plotted closest to a given graphics position.
     * Note the result might still be a long way off - standard practice
     * is to threshold the result against the value of {@link #NEAR_PIXELS}.
     *
     * @param  surface  plot surface
     * @param  geom     maps data positions to graphics positions
     * @param  iPosCoord   coordinate index of positional coords in tseq
     * @param  tseq     tuple sequence positioned at start
     * @param  point    reference graphics position
     * @return   object giving row index and distance;
     *           null is returned if no points are present
     */
    @Slow
    public static IndicatedRow getClosestRow( Surface surface, DataGeom geom,
                                              int iPosCoord, TupleSequence tseq,
                                              Point2D point ) {
        double[] dpos = new double[ surface.getDataDimCount() ];
        Point2D.Double gp = new Point2D.Double();
        long bestIndex = -1;
        double bestDist2 = Double.POSITIVE_INFINITY;
        while ( tseq.next() ) {
            if ( geom.readDataPos( tseq, iPosCoord, dpos ) &&
                 surface.dataToGraphics( dpos, true, gp ) ) {
                double dist2 = gp.distanceSq( point );
                if ( dist2 < bestDist2 ) {
                    bestDist2 = dist2;
                    bestIndex = tseq.getRowIndex();
                }
            }
        }
        return Thread.currentThread().isInterrupted() || bestIndex < 0
             ? null
             : new IndicatedRow( bestIndex, Math.sqrt( bestDist2 ) );
    }

    /**     
     * Creates an icon which will paint a surface and the layers on it.
     * If the <code>storedPlans</code> object is supplied, it may contain
     * plans from previous plots.  On exit, it will contain the plans
     * used for this plot.
     *
     * @param  placer  plot placement
     * @param  layers   layers constituting plot content
     * @param  auxRanges  requested range information calculated from data
     * @param  dataStore  data storage object
     * @param  paperType  rendering type
     * @param  cached  whether to cache pixels for future use
     * @param  storedPlans  writable collection of plan objects, or null
     * @return   icon containing complete plot
     */ 
    @Slow
    public static Icon createPlotIcon( PlotPlacement placer, PlotLayer[] layers,
                                       Map<AuxScale,Range> auxRanges,
                                       DataStore dataStore, PaperType paperType,
                                       boolean cached,
                                       Collection<Object> storedPlans ) {
        Surface surface = placer.getSurface();
        int nl = layers.length;
        logger_.info( "Layers: " + nl + ", Paper: " + paperType );
        Drawing[] drawings = new Drawing[ nl ];
        Object[] plans = new Object[ nl ];
        Set<Object> knownPlans = new HashSet<Object>();
        if ( storedPlans != null ) {
            knownPlans.addAll( storedPlans );
        }
        long t1 = System.currentTimeMillis();
        for ( int il = 0; il < nl; il++ ) {
            drawings[ il ] = layers[ il ]
                            .createDrawing( surface, auxRanges, paperType );
            plans[ il ] = drawings[ il ].calculatePlan( knownPlans.toArray(),
                                                        dataStore );
            knownPlans.add( plans[ il ] );
        }
        PlotUtil.logTime( logger_, "Plans", t1 );
        if ( storedPlans != null ) {
            storedPlans.clear();
            storedPlans.addAll( new HashSet<Object>( Arrays.asList( plans ) ) );
        }
        Icon dataIcon =
            paperType.createDataIcon( surface, drawings, plans, dataStore,
                                      cached );
        if ( logger_.isLoggable( REPORT_LEVEL ) ) {
            for ( int il = 0; il < nl; il++ ) {
                ReportMap report = drawings[ il ].getReport( plans[ il ] );
                if ( report != null ) {
                    String rtxt = report.toString( false );
                    if ( rtxt.length() > 0 ) {
                        String msg = new StringBuffer()
                            .append( "Layer " )
                            .append( il ) 
                            .append( ": " )
                            .append( rtxt ) 
                            .toString();
                        logger_.log( REPORT_LEVEL, msg );
                    }
                }
            }
        }
        return placer.createPlotIcon( dataIcon ); 
    }

    /**
     * Determines which mouse button was changed at a given mouse event.
     * It's not really clear across the landscape of different mouse types
     * and different OSes what user gestures different mouse/keyboard gestures
     * will generate.  We collect all the logic in this method, so if
     * it turns out it's not working properly it can be adjusted easily.
     *
     * <p>This method will return an integer in the range 0-3 with the
     * following meaning:
     * <ul>
     * <li>0: no button pressed</li>
     * <li>1: left button pressed (normal primary button)</li>
     * <li>2: center button pressed (least likely to be present)</li>
     * <li>3: right button pressed (normal secondary button)</li>
     * </ul>
     * <p>The output of this method is the 'logical' value, so 2 may be
     * returned to indicate simultaneous press of both buttons on
     * a 2-button mouse if it's set up that way.
     * If users have set up their mice strangely then a physical left
     * click might not yield a value of 1 - that's their lookout.
     *
     * <p>This method is only intended for use when a single button is
     * expected; multi-button gestures are not supported.
     *
     * <p>We follow the (conventional) usage where ctrl-click means
     * right-click on a single button mouse, and we also currently use
     * shift-click to mean center button.  These conventions may be
     * noted in user documentation.
     *
     * @param   evt   mouse event
     * @return   button indicator, 0-3
     * @see   #getButtonDownIndex
     */
    public static int getButtonChangedIndex( MouseEvent evt ) {

        /* There are SwingUtilities methods isLeftMouseButton etc that test
         * against InputEvent.BUTTON1_MASK etc.  But those constants are
         * deprecated (in favour of BUTTON1_DOWN_MASK etc) in the InputEvent
         * class.  So avoid using those for now, and do it by hand.
         * I don't know if evt.getButton already takes account of
         * ctrl/shift conventions, but even if it does, the following code
         * should be OK (as long as there is no attempt to make sense of
         * gestures which use multiple logical buttons simultaneously). */

        int iButt = evt.getButton();
        int exmods = evt.getModifiersEx();
        if ( iButt == MouseEvent.BUTTON3 ) {
            return 3;
        }
        else if ( iButt == MouseEvent.BUTTON2 ) {
            return 2;
        }
        else if ( iButt == MouseEvent.NOBUTTON ) {
            return 0;
        }
        else {
            assert iButt == MouseEvent.BUTTON1;
            if ( ( exmods & InputEvent.CTRL_DOWN_MASK ) != 0 ) {
                return 3;
            }
            else if ( ( exmods & InputEvent.SHIFT_DOWN_MASK ) != 0 ) {
                return 2;
            }
            else {
                return 1;
            }
        }
    }

    /**
     * Determines which single button is depressed at a given mouse event.
     * The output value and considerations are the same as for
     * {@link #getButtonChangedIndex}.
     *
     * @param   evt   mouse event
     * @return   button indicator, 0-3
     * @see   #getButtonChangedIndex
     */
    public static int getButtonDownIndex( MouseEvent evt ) {
        int exmods = evt.getModifiersEx();
        if ( ( exmods & InputEvent.BUTTON3_DOWN_MASK ) != 0 ) {
            return 3;
        }
        else if ( ( exmods & InputEvent.BUTTON2_DOWN_MASK ) != 0 ) {
            return 2;
        }
        else if ( ( exmods & InputEvent.BUTTON1_DOWN_MASK ) != 0 ) {
            if ( ( exmods & InputEvent.CTRL_DOWN_MASK ) != 0 ) {
                return 3;
            }
            else if ( ( exmods & InputEvent.SHIFT_DOWN_MASK ) != 0 ) {
                return 2;
            }
            else {
                return 1;
            }
        }
        else {
            return 0;
        }
    }

    /**
     * Determines a zoom factor from a mouse wheel event and a given
     * unit zoom factor.
     * It just raises the given unit factor to the power of 
     * the number of wheel rotations (and applies a sense adjustment).
     *
     * @param   unitFactor   positive zoom factor corresponding to a
     *                       single click
     * @param   wheelrot   mouse wheel rotation
     * @return   zoom factor
     */
    public static double toZoom( double unitFactor, int wheelrot ) {
        return Math.pow( unitFactor, - wheelrot );
    }

    /**
     * Determines an X, Y or isotropic zoom factor from a pair of
     * screen positions and a given unit zoom factor.
     * The absolute positions of the supplied points are not important,
     * only their separation is used.
     *
     * @param   unitFactor   positive zoom factor corresponding to a
     *                       single click
     * @param  p0    origin point
     * @param  p1    destination point
     * @param  isY   direction flag; TRUE for Y zoom, FALSE for X zoom and
     *               null for isotropic zoom
     * @return   zoom factor
     */
    public static double toZoom( double unitFactor, Point p0, Point p1,
                                 Boolean isY ) {

        /* Zoom in is right and up, i.e. in the conventional direction of
         * increasing data (though not graphics) coordinates.
         * This differs from the sense of the Y direction zoom used by
         * the old-style plots (inherited from PtPlot). */
        int dx = p1.x - p0.x;
        int dy = - p1.y + p0.y;
        final int npix;
        if ( isY == null ) {
            npix = dx + dy;
        }
        else {
            npix = isY.booleanValue() ? dy : dx;
        }
        return Math.pow( unitFactor, npix / 24.0 );
    }

    /**
     * Returns a value determined by a fixed range and a fractional scale point
     * within it.  If the point is zero the minimum value is returned,
     * and if it is one the maximum value is returned.
     *
     * @param  min  minimum of range
     * @param  max  maximum of range
     * @param  frac  fractional scale point
     * @param  isLog  true iff the range is logarithmic
     * @return   data value corresponding to fractional scale point
     */
    public static double scaleValue( double min, double max, double frac,
                                     boolean isLog ) {
        return isLog
             ? min * Math.pow( ( max / min ), frac )
             : min + ( max - min ) * frac;
    }

    /**
     * Returns the proportional position of a point within a fixed range.
     * If the point is equal to the minimum value zero is returned,
     * and if it is equal to the maximum value one is returned.
     * This is the inverse function of {@link #scaleValue}.
     *
     * @param  min  minimum of range
     * @param  max  maximum of range
     * @param  point  data value
     * @param  isLog  true iff the range is logarithmic
     * @return  fractional value corresponding to data point
     */
    public static double unscaleValue( double min, double max, double point,
                                       boolean isLog ) {
        return isLog
             ? ( Math.log( point ) - Math.log( min ) )
               / ( Math.log( max ) - Math.log( min ) )
             : ( point - min ) / ( max - min );
    }

    /**
     * Returns a range determined by a fixed range and a subrange within it.
     * If the subrange is 0-1 the output range is the input range.
     *
     * @param  min  minimum of range
     * @param  max  maximum of range
     * @param  subrange  sub-range, both ends between 0 and 1
     * @param  isLog  true iff the range is logarithmic
     * @return   2-element array giving low, high values of scaled range
     */
    public static double[] scaleRange( double min, double max,
                                       Subrange subrange, boolean isLog ) {
        return new double[] {
            scaleValue( min, max, subrange.getLow(), isLog ),
            scaleValue( min, max, subrange.getHigh(), isLog ),
        };
    }

    /**
     * Indicates whether two floating point numbers are approximately equal
     * to each other.
     * Exact semantics are intentionally not well-defined by this contract.
     *
     * @param   v0  one value
     * @param   v1  other value
     * @return  true if they are about the same
     */
    public static boolean approxEquals( double v0, double v1 ) {
        if ( v0 == 0 ) {
            return v1 == 0;
        }
        else {
            double r = v1 / v0;
            return r >= .9999 && r <= 1.0001;
        }
    }

    /**
     * Numeric formatting utility function.
     *
     * @param  value  numeric value to format
     * @param  baseFmt  format string as for {@link java.text.DecimalFormat}
     * @param  nFracDigits  fixed number of digits after the decimal point
     * @return  formatted string
     */
    public static String formatNumber( double value, String baseFmt,
                                       int nFracDigits ) {
        DecimalFormat fmt = new DecimalFormat( baseFmt );
        fmt.setMaximumFractionDigits( nFracDigits );
        fmt.setMinimumFractionDigits( nFracDigits );
        return fmt.format( value );
    }

    /**
     * Formats a number so that it presents a number of significant figures
     * corresponding to a supplied small difference.
     * The idea is that the output should be compact, but that applying
     * this function to value and value+epsilon should give visibly
     * different results.  The number of significant figures is determined
     * by epsilon, not further rounded (trailing zeroes are not truncated).
     *
     * @param   value   value to format
     * @param   epsilon   small value
     * @return  formatted value
     */
    public static String formatNumber( double value, double epsilon ) {
        epsilon = Math.abs( epsilon );

        /* Work out the number of significant figures. */
        double aval = Math.abs( value );
        int nsf =
            Math.max( 0, (int) Math.round( -Math.log10( epsilon / aval ) ) );

        /* Return a formatted string on this basis. */
        if ( aval >= 1e6 || aval <= 1e-4 ) {
            return formatNumber( value, "0.#E0", nsf );
        }
        else if ( epsilon >= 0.9 ) {
            return Long.toString( (long) Math.round( value ) );
        }
        else {
            int ndp = (int) Math.round( Math.max( 0, -Math.log10( epsilon ) ) );
            return ndp == 0
                 ? Long.toString( (long) Math.round( value ) )
                 : formatNumber( value, "0.0", ndp );
        }
    }

    /**
     * Returns the rectangle that results from removing the insets from
     * a given rectangle.
     *
     * @param   base  input rectangle
     * @param   insets  amount that should be excluded from the edges of
     *                  the base rectangle
     * @return  new, smaller rectangle
     */
    public static Rectangle subtractInsets( Rectangle base, Insets insets ) {
        return new Rectangle( base.x + insets.left,
                              base.y + insets.top,
                              base.width - insets.left - insets.right,
                              base.height - insets.top - insets.bottom );
    }
}
