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
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.plot.PdfGraphicExporter;
import uk.ac.starlink.ttools.plot.Picture;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.TupleRunner;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.paper.PaperType;
import uk.ac.starlink.util.SplitCollector;

/**
 * Miscellaneous utilities for use with the plotting classes.
 *
 * @author   Mark Taylor
 * @since    13 Feb 2013
 */
public class PlotUtil {

    private static Boolean dfltAntialias_;
    private static final DecimalFormatSymbols UK_SYMBOLS =
        DecimalFormatSymbols.getInstance( Locale.UK );
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
        public TupleSequence split() {
            return null;
        }
        public long splittableSize() {
            return 0L;
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
        public long getLongValue( int icol ) {
            throw new IllegalStateException();
        }
        public boolean getBooleanValue( int icol ) {
            throw new IllegalStateException();
        }
    };

    /** Span instance not initialised with any data. */
    public static final Span EMPTY_SPAN = new BasicRanger( true ).createSpan();

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

    /** Amount of padding added to data ranges for axis scaling near zero. */
    private static final double TINY_FRACTION = 1e-7;

    /** Level at which plot reports are logged. */
    private static final Level REPORT_LEVEL = Level.INFO;

     /** Default SplitRunner for CoordSequences. */
     public static SplitRunner<CoordSequence> COORD_RUNNER =
         SplitRunner.createDefaultRunner();

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
     * Indicates whether two double values are equivalent.
     * Unlike the == operator, this function returns true if both are NaN.
     *
     * @param  d1  first value
     * @param  d2  second value
     * @return   true iff inputs are both equal or both NaN
     */
    public static boolean doubleEquals( double d1, double d2 ) {
        return Double.isNaN( d1 ) ? Double.isNaN( d2 )
                                  : d1 == d2;
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
     * Writes a message through the logging system
     * about the supplied elapsed time a named step has taken.
     * If the elapsed time is zero, nothing is logged.
     *
     * @param  logger   log message destination
     * @param  phase  name of step to log time of
     * @param  elapsed   elapsed time to report (generally milliseconds)
     */
    public static void logTimeElapsed( Logger logger, String phase,
                                       long elapsed ) {
        if ( elapsed > 0 ) {
            logger.info( phase + " time: " + elapsed );
        }
    }

    /**
     * Writes a message through the logging system
     * about the elapsed time a named step has taken given a start time.
     * The elapsed time is presumed to be the time between the supplied
     * time and the time when this method is called.
     * If the elapsed time is zero (to the nearest millisecond),
     * nothing is logged.
     *
     * @param  logger   log message destination
     * @param  phase  name of step to log time of
     * @param  start   start {@link java.lang.System#currentTimeMillis
     *                              currentTimeMillis}
     */
    public static void logTimeFromStart( Logger logger, String phase,
                                         long start ) {
        logTimeElapsed( logger, phase, System.currentTimeMillis() - start );
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
        Class<?> eClazz = a1.getClass().getComponentType();
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
        @SuppressWarnings("unchecked")
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
        @SuppressWarnings("unchecked")
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
        @SuppressWarnings("unchecked")
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
     * Convenience TupleRunner collection method using a DataStore and DataSpec.
     * Default accumulator pooling policy is used (no pool).
     *
     * @param   collector  collector
     * @param   dataSpec   data spec
     * @param   dataStore   data store, supplying both the data and the runner
     * @return   collected result
     */
    @Slow
    public static <A> A tupleCollect( SplitCollector<TupleSequence,A> collector,
                                      DataSpec dataSpec, DataStore dataStore ) {
        return dataStore.getTupleRunner()
              .collect( collector,
                        () -> dataStore.getTupleSequence( dataSpec ) );
    }


    /**
     * Extends existing range objects using range information
     * for a set of layers which have Cartesian (or similar) coordinates.
     *
     * @param   layers   plot layers
     * @param   ranges   <code>nDataDim</code>-element array of range objects
     *                   to extend
     * @param   logFlags  <code>nDataDim</code>-element array indicating
     *                    whether data dimensions are
     *                    linear (false) or logarithmic (true),
     * @param   doPad    whether to add a small standard amount of padding
     *                   to the result
     * @param   dataStore  data storage
     */
    @Slow
    public static void extendCoordinateRanges( PlotLayer[] layers,
                                               Range[] ranges,
                                               boolean[] logFlags,
                                               boolean doPad,
                                               DataStore dataStore ) {
        final int nDataDim = ranges.length;

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
        if ( subClouds.length > 0 ) {
            PointCloud cloud = new PointCloud( subClouds );

            /* Collect values for the represented points to mark out the basic
             * range of data positions covered by the layers. */
            RangeCollector<CoordSequence> rangeCollector =
                    new RangeCollector<CoordSequence>( nDataDim ) {
                public void accumulate( CoordSequence cseq, Range[] ranges ) {
                    double[] dpos = cseq.getCoords();
                    while ( cseq.next() ) {
                        for ( int idim = 0; idim < nDataDim; idim++ ) {
                            ranges[ idim ].submit( dpos[ idim ] );
                        }
                    }
                }
            };
            Range[] cloudRanges =
                dataStore.getTupleRunner().coordRunner()
                         .collect( rangeCollector, 
                                   cloud.createDataPosSupplier( dataStore ) );
            rangeCollector.mergeRanges( ranges, cloudRanges );
        }

        /* If any of the layers wants to supply non-data-position points
         * to mark out additional space, take account of those too. */
        for ( int il = 0; il < layers.length; il++ ) {
            layers[ il ].extendCoordinateRanges( ranges, logFlags, dataStore );
        }

        /* Pad the ranges with a bit of space. */
        if ( doPad ) {
            for ( int idim = 0; idim < nDataDim; idim++ ) {
                padRange( ranges[ idim ], logFlags[ idim ] );
            }
        }
    }

    /**
     * Pads a data range to provide a bit of extra space at each end
     * using a standard padding fraction.
     * If one of the limits extends nearly or exactly to zero,
     * it is padded to (very nearly) zero instead of adding a fixed amount.
     *
     * @param  range  range to pad
     * @param  logFlag  true for logarithmic scaling, false for linear
     */
    public static void padRange( Range range, boolean logFlag ) {
        double[] bounds = range.getFiniteBounds( logFlag );
        double lo = bounds[ 0 ];
        double hi = bounds[ 1 ];
        if ( lo < hi ) {
            double padFrac = PAD_FRACTION;
            double tinyFrac = TINY_FRACTION;
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

            /* Always add at least a tiny amount, even if we are trying to
             * set the bound to a round number (zero).
             * If you don't do that, then points with exactly the values
             * on the boundary range may not get plotted (since calling
             * Surface.dataToGraphics with visibleOnly=true may exclude them),
             * so you can end up with an autoranged plot that does not
             * include all the values from which the range was generated. */
            double loPadFrac = loNearZero ? tinyFrac : padFrac; 
            double hiPadFrac = hiNearZero ? tinyFrac : padFrac;
            range.submit( scaleValue( lo, hi, 0 - loPadFrac, logFlag ) );
            range.submit( scaleValue( lo, hi, 1 + hiPadFrac, logFlag ) );
        }
    }

    /**
     * Attempts to return input-level metadata about an aux value that may
     * be referenced in a given list of plot layers.
     *
     * @param   layers   list of layers
     * @param   scale    aux item of interest
     * @return   input metadata for scale, or null if nothing suitable can
     *           be found
     */
    public static ValueInfo getScaleInfo( PlotLayer[] layers, AuxScale scale ) {
        for ( PlotLayer layer : layers ) {
            AuxReader rdr = layer.getAuxRangers().get( scale );
            if ( rdr != null ) {
                ValueInfo info = rdr.getAxisInfo( layer.getDataSpec() );
                if ( info != null ) {
                    return info;
                }
            }
        }
        return null;
    }

    /**
     * Attempts to return a suitable label for an aux axis that may
     * be referenced in a given list of plot layers.
     *
     * @param   layers   list of layers
     * @param   scale    aux item of interest
     * @return   scale axis label, or null if nothing suitable could be found
     */
    public static String getScaleAxisLabel( PlotLayer[] layers,
                                            AuxScale scale ) {
        ValueInfo info = getScaleInfo( layers, scale );
        if ( info != null ) {
            String name = info.getName();
            String unit = info.getUnitString();
            if ( name != null && unit != null ) {
                return name + " / " + unit;
            }
            else if ( name != null ) {
                return name;
            }
        }
        return null;
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
     * @param  tupleSupplier    iterable over tuples
     * @param  runner    manages tuple iteration
     * @param  point    reference graphics position
     * @return   object giving row index and distance;
     *           null is returned if no points are present
     */
    @Slow
    public static IndicatedRow
            getClosestRow( Surface surface, DataGeom geom, int iPosCoord,
                           Supplier<TupleSequence> tupleSupplier,
                           TupleRunner runner, Point2D point ) {
        IndexDist ixdist = 
            runner
           .collect( new ClosestCollector( surface, geom, iPosCoord, point ),
                     tupleSupplier );
        return ixdist.bestIndex_ < 0 ||
               Thread.currentThread().isInterrupted()
             ? null
             : new IndicatedRow( ixdist.bestIndex_,
                                 Math.sqrt( ixdist.bestDist2_ ),
                                 ixdist.bestDpos_ );
    }

    /**     
     * Creates an icon which will paint a surface and the layers on it.
     * If the <code>storedPlans</code> object is supplied, it may contain
     * plans from previous plots.  On exit, it will contain the plans
     * used for this plot.
     *
     * @param  placer  plot placement
     * @param  layers   layers constituting plot content
     * @param  auxSpans   requested range information calculated from data
     * @param  dataStore  data storage object
     * @param  paperType  rendering type
     * @param  cached  whether to cache pixels for future use
     * @param  storedPlans  writable collection of plan objects, or null
     * @return   icon containing complete plot
     */ 
    @Slow
    public static Icon createPlotIcon( PlotPlacement placer, PlotLayer[] layers,
                                       Map<AuxScale,Span> auxSpans,
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
        for ( int il = 0; il < nl; il++ ) {
            drawings[ il ] = layers[ il ]
                            .createDrawing( surface, auxSpans, paperType );
            plans[ il ] = drawings[ il ].calculatePlan( knownPlans.toArray(),
                                                        dataStore );
            knownPlans.add( plans[ il ] );
        }
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
        else if ( iButt == MouseEvent.BUTTON1 ) {
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
        else if ( iButt == MouseEvent.NOBUTTON ) {
            return 0;
        }
        else {
            // non-standard button - ignore it
            return 0;
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
             ? Math.exp( Math.log( min ) * ( 1. - frac )
                       + Math.log( max ) * frac )
             : min * ( 1. - frac ) + max * frac;
    }

    /**
     * Does linear scaling between two values.
     * This convenience method just calls
     * <code>scaleValue(min, max, frac, false)</code>
     *
     * @param  min  minimum of range
     * @param  max  maximum of range
     * @param  frac  fractional scale point
     * @return   data value corresponding to fractional scale point
     */
    public static double scaleValue( double min, double max, double frac ) {
        return scaleValue( min, max, frac, false );
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
     * Returns a basic span instance with a given lower and upper bound.
     *
     * @param  lo  lower bound, may be NaN
     * @param  hi  upper bound, may be NaN
     * @return  new span
     */
    public static Span createSpan( double lo, double hi ) {
        return EMPTY_SPAN.limit( lo, hi );
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
     * The output is not Locale-sensitive, so is suitable for formatting
     * numbers that may be re-parsed as numbers (by non-Locale-sensitive code).
     *
     * @param  value  numeric value to format
     * @param  baseFmt  format string as for {@link java.text.DecimalFormat}
     * @param  nFracDigits  fixed number of digits after the decimal point
     * @return  formatted string
     */
    public static String formatNumber( double value, String baseFmt,
                                       int nFracDigits ) {
        DecimalFormat fmt = new DecimalFormat( baseFmt, UK_SYMBOLS );
        fmt.setMaximumFractionDigits( nFracDigits );
        fmt.setMinimumFractionDigits( nFracDigits );
        String out = fmt.format( value );
        return out.matches( "-0+\\.0+" )
             ? out.substring( 1 )
             : out;
    }

    /**
     * Numeric formatting utility function for writing a given number
     * of significant figures.
     * The output is not Locale-sensitive, so is suitable for formatting
     * numbers that may be re-parsed as numbers (by non-Locale-sensitive code).
     * Formatting is best-efforts and may suppress insignificant zeros.
     *
     * @param  value  numeric value to format
     * @param  nsf   (approximate) number of significant figures
     * @return  formatted string
     */
    public static String formatNumberSf( double value, int nsf ) {
        String txt = doFormatNumberSf( value, nsf );
        assert Double.isNaN( value ) || Double.isInfinite( value )
            || Math.abs( value - Double.parseDouble( txt ) ) / value
               < Math.pow( 10, -nsf );
        return txt;
    }

    /**
     * Does the work for {@link #formatNumberSf}.
     *
     * @param  value  numeric value to format
     * @param  nsf   (approximate) number of significant figures
     * @return  formatted string
     */
    private static String doFormatNumberSf( double value, int nsf ) { 
        if ( value == 0 ) {
            return "0";
        }
        else if ( Double.isNaN( value ) || Double.isInfinite( value ) ) {
            return Double.toString( value );
        }
        else {
            double absVal = Math.abs( value );
            double log10 = Math.log10( absVal );
            if ( log10 >= -1 && log10 < nsf ) {
                int ndp = nsf - (int) Math.ceil( log10 );
                StringBuffer fbuf = new StringBuffer( 2 + ndp );
                fbuf.append( "0." );
                for ( int i = 0; i < ndp; i++ ) {
                    fbuf.append( '#' );
                }
                DecimalFormat fmt =
                    new DecimalFormat( fbuf.toString(), UK_SYMBOLS );
                return fmt.format( value );
            }
            else {
                StringBuffer fbuf = new StringBuffer( 4 + nsf );
                fbuf.append( "0." );
                for ( int i = 0; i < nsf - 1; i++ ) {
                    fbuf.append( '#' );
                }
                fbuf.append( "E0" );
                DecimalFormat fmt =
                    new DecimalFormat( fbuf.toString(), UK_SYMBOLS );
                return fmt.format( value );
            }
        }
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
        if ( epsilon == 0 || Double.isNaN( epsilon ) ) {
            return Double.toString( value );
        }
        epsilon = Math.abs( epsilon );

        /* Work out the number of significant figures. */
        double aval = Math.abs( value );
        int nsf =
            Math.max( 0, (int) Math.round( -Math.log10( epsilon / aval ) ) );

        /* Return a formatted string on this basis. */
        if ( aval <= Double.MIN_NORMAL ) {
            return "0";
        }
        if ( aval >= 1e6 || aval <= 1e-4 ) {
            return formatNumber( value, "0.#E0", nsf );
        }
        else if ( epsilon >= 0.9 ) {
            return Long.toString( Math.round( value ) );
        }
        else {
            int ndp = (int) Math.round( Math.max( 0, -Math.log10( epsilon ) ) );
            return ndp == 0
                 ? Long.toString( Math.round( value ) )
                 : formatNumber( value, "0.0", ndp );
        }
    }

    /**
     * Formats a pair of values representing data bounds of a range
     * along a graphics axis. nge.  The number of pixels separating the
     * values is used to determine the formatting precision.
     *
     * @param  lo   data lower bound
     * @param  hi   data upper bound
     * @param  isLog  true for logarithmic axis, false for linear
     * @param   npix   approximate number of pixels covered by the range
     * @return   2-element array giving (lower,upper) bounds formatted
     *           and ready for presentation to the user
     */
    public static String[] formatAxisRangeLimits( double lo, double hi,
                                                  boolean isLog, int npix ) {
        if ( isLog ) {
            double dl = ( Math.log( hi ) - Math.log( lo ) ) / npix;
            return new String[] {
                formatNumber( lo, Math.min( lo * dl, lo / dl ) ),
                formatNumber( hi, Math.min( hi * dl, hi / dl ) ),
            };
        }
        else {
            double eps = ( hi - lo ) / npix;
            return new String[] {
                formatNumber( lo, eps ),
                formatNumber( hi, eps ),
            };
        }
    }

    /**
     * Rounds a number to a decimal round value.
     * The number of decimal places is determined by the size of
     * a supplied value, epsilon.
     * When turned into a string, the final digit should be about
     * the same size as epsilon.   Given decimal-&gt;binary conversions
     * and uncertain behaviour of library stringification methods like
     * Double.toDouble() this isn't bulletproof and may require some
     * adjustment, but it seems to work as desired most of the time.
     *
     * @param   x  input value
     * @param  epsilon  indicates desired rounding amount
     * @return  output value, presumably destined for stringification
     */
    public static double roundNumber( double x, double epsilon ) {
        if ( Double.isNaN( x ) ) {
            return x;
        }
        else {
            try {
                return Double.parseDouble( formatNumber( x, epsilon ) );
            }
            catch ( NumberFormatException e ) {
                assert false : formatNumber( x, epsilon ) + " -> " + e;
                return x;
            }
        }
    }

    /**
     * Utility method to set a minimum/maximum config key pair
     * to a given pair of minimum/maximum values.
     * This handles suitable rounding for presentation.
     *
     * @param   minKey  config key for minimum value
     * @param   maxKey  config key for maximum value
     * @param   min     minimum value
     * @param   max     maximum value
     * @param   npix    number of pixels (quanta) between min and max;
     *                  this is used to determine at what level of
     *                  precision to round the config values
     */
    public static ConfigMap configLimits( ConfigKey<Double> minKey,
                                          ConfigKey<Double> maxKey,
                                          double min, double max,
                                          int npix ) {
        double epsilon = ( max - min ) / npix;
        ConfigMap config = new ConfigMap();
        config.put( minKey, roundNumber( min, epsilon ) );
        config.put( maxKey, roundNumber( max, epsilon ) );
        return config;
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

    /**
     * Accumulator class for use with ClosestCollector.
     * Aggregates the index of the current best row with its distance
     * from the target position.
     */
    private static class IndexDist {
        long bestIndex_ = -1;
        double bestDist2_ = Double.POSITIVE_INFINITY;
        double[] bestDpos_ = null;
    }

    /**
     * SplitCollector implementation for determining the closest row
     * to a given graphics position on a plotting surface.
     */
    private static class ClosestCollector
            implements SplitCollector<TupleSequence,IndexDist> {

        private final Surface surface_;
        private final DataGeom geom_;
        private final int iPosCoord_;
        private final Point2D point_;

        /**
         * Constructor.
         *
         * @param  surface  plot surface
         * @param  geom     maps data positions to graphics positions
         * @param  iPosCoord   coordinate index of positional coords in tseq
         * @param  point    reference graphics position
         */
        public ClosestCollector( Surface surface, DataGeom geom, int iPosCoord,
                                 Point2D point ) {
            surface_ = surface;
            geom_ = geom;
            iPosCoord_ = iPosCoord;
            point_ = point;
        }

        public IndexDist createAccumulator() {
            return new IndexDist();
        }

        public void accumulate( TupleSequence tseq, IndexDist acc ) {
            double[] dpos = new double[ surface_.getDataDimCount() ];
            Point2D.Double gp = new Point2D.Double();
            while ( tseq.next() ) {
                if ( geom_.readDataPos( tseq, iPosCoord_, dpos ) &&
                     surface_.dataToGraphics( dpos, true, gp ) ) {
                    double dist2 = gp.distanceSq( point_ );
                    if ( dist2 < acc.bestDist2_ ) {
                        acc.bestDist2_ = dist2;
                        acc.bestIndex_ = tseq.getRowIndex();
                        acc.bestDpos_ = dpos.clone();
                    }
                }
            }
        }

        public IndexDist combine( IndexDist acc1, IndexDist acc2 ) {
            return acc1.bestDist2_ <= acc2.bestDist2_ ? acc1 : acc2;
        }
    }
}
