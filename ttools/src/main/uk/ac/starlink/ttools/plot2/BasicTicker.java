package uk.ac.starlink.ttools.plot2;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.util.Bi;
import uk.ac.starlink.util.Pair;

/**
 * Partial Ticker implementation based on a rule defining a sequence of ticks.
 * Concrete subclasses must implement a method to create a Rule
 * suitable for a given range, and this is used to provide suitable
 * ticks for particular circumstances, including avoiding label overlap.
 *
 * @author   Mark Taylor
 * @since    17 Oct 2013
 */
public abstract class BasicTicker implements Ticker {

    private final boolean logFlag_;

    /** Ticker for linear axes. */
    public static final BasicTicker LINEAR = new BasicTicker( false ) {
        public Rule createRule( double dlo, double dhi,
                                double approxMajorCount, int adjust ) {
            return new CheckRule( createRawRule( dlo, dhi, approxMajorCount,
                                                 adjust, false ) ) {
                boolean checkLabel( Caption label, double value ) {
                    double diff = Double.parseDouble( label.toText() ) - value;
                    return diff == 0 || Math.abs( diff / value ) < 1e-10;
                }
            };
        }
    };

    /** Ticker for logarithmic axes. */
    public static final BasicTicker LOG = new BasicTicker( true ) {
        public Rule createRule( double dlo, double dhi,
                                double approxMajorCount, int adjust ) {
            if ( dlo <= 0 || dhi <= 0 ) {
                throw new IllegalArgumentException( "Negative log range?" );
            }
            return new CheckRule( createRawRule( dlo, dhi, approxMajorCount,
                                                 adjust, true ) ) {
                boolean checkLabel( Caption label, double value ) {
                   return Math.abs( Double.parseDouble( label.toText() )
                                    / value - 1 )
                        < 1e-10;
                }
            };
        }
    };

    /**
     * Constructor.
     *
     * @param  logFlag  true for logarithmic axis, false for linear
     */
    protected BasicTicker( boolean logFlag ) {
        logFlag_ = logFlag;
    }

    /**
     * Returns a new rule for labelling an axis in a given range.
     * The tick density is determined by two parameters,
     * <code>approxMajorCount</code>, which gives a baseline value for
     * the number of ticks required over the given range, and
     * <code>adjust</code>.
     * Increasing <code>adjust</code> will give more major ticks, and
     * decreasing it will give fewer ticks.
     * Each value of adjust should result in a different tick count.
     *
     * @param   dlo     minimum axis data value
     * @param   dhi     maximum axis data value
     * @param   approxMajorCount  guide value for number of major ticks
     *                            in range
     * @param   adjust  adjusts density of major ticks, zero is normal
     */
    public abstract Rule createRule( double dlo, double dhi,
                                     double approxMajorCount, int adjust );

    public TickRun getTicks( double dlo, double dhi, boolean withMinor,
                             Captioner captioner, Orientation[] orients,
                             int npix, double crowding ) {
        Bi<Rule,Orientation> orule =
            getRule( dlo, dhi, captioner, orients, npix, crowding );
        Rule rule = orule.getItem1();
        Orientation orient = orule.getItem2();
        Tick[] ticks = withMinor
                     ? PlotUtil.arrayConcat( getMajorTicks( rule, dlo, dhi ),
                                             getMinorTicks( rule, dlo, dhi ) )
                     : getMajorTicks( rule, dlo, dhi );
        return new TickRun( ticks, orient );
    }

    /**
     * Returns a Rule suitable for a given axis labelling job.
     * This starts off by generating ticks at roughly a standard separation,
     * guided by the crowding parameter.
     * If none of the orientations can generate ticks without overlap,
     * it backs off until it finds a set of ticks that can be displayed
     * in a tidy fashion.
     *
     * @param   dlo        minimum axis data value
     * @param   dhi        maximum axis data value
     * @param   captioner  caption painter
     * @param   orients    label orientation options in order of preference
     * @param   npix       number of pixels along the axis
     * @param   crowding   1 for normal tick density on the axis,
     *                     lower for fewer labels, higher for more
     * @return   basic tick generation rule with associated orientation
     */
    private Bi<Rule,Orientation> getRule( double dlo, double dhi,
                                          Captioner captioner,
                                          Orientation[] orients,
                                          int npix, double crowding ) {
        if ( dhi <= dlo  ) {
            throw new IllegalArgumentException( "Bad range: "
                                              + dlo + " .. " + dhi );
        }

        /* Work out approximately how many major ticks are requested. */
        double approxMajorCount = Math.max( 1, npix / 80 ) * crowding;

        /* Acquire a suitable rule and use it to generate the major ticks.
         * When we have the ticks, try to find an orientation for which
         * they are not so crowded as to overlap.  If that's not possible,
         * back off to lower crowding levels until we have
         * something suitable. */
        Axis axis = Axis.createAxis( 0, npix, dlo, dhi, logFlag_, false );
        int maxAdjust = -5;
        for ( int adjust = 0 ; adjust > maxAdjust; adjust-- ) {
            Rule rule = createRule( dlo, dhi, approxMajorCount, adjust );
            Tick[] majors = getMajorTicks( rule, dlo, dhi );
            for ( Orientation orient : orients ) {
                if ( ! overlaps( majors, axis, captioner, orient ) ) {
                    return new Bi<Rule,Orientation>( rule, orient );
                }
            }
        }

        /* Adjustment is getting too extreme.  Return rule with overlapping
         * labels, too bad. */
        Rule rule = createRule( dlo, dhi, approxMajorCount, maxAdjust );
        return new Bi<Rule,Orientation>( rule, orients[ 0 ] );
    }

    /**
     * Use a given rule to generate major ticks in a given range of
     * coordinates.
     *
     * @param   rule    tick generation rule
     * @param   dlo     minimum axis data value
     * @param   dhi     maximum axis data value
     * @return  array of major ticks
     */
    public static Tick[] getMajorTicks( Rule rule, double dlo, double dhi ) {
        List<Tick> list = new ArrayList<Tick>();
        for ( long index = rule.floorIndex( dlo );
              rule.indexToValue( index ) <= dhi; index++ ) {
            double major = rule.indexToValue( index );
            if ( major >= dlo && major <= dhi ) {
                Caption label = rule.indexToLabel( index );
                list.add( new Tick( major, label ) );
            }
        }
        return list.toArray( new Tick[ 0 ] );
    }

    /**
     * Use a given rule to generate minor ticks in a given range of
     * coordinates.
     *
     * @param   rule       tick generation rule
     * @param   dlo        minimum axis data value
     * @param   dhi        maximum axis data value
     * @return  array of minor ticks
     */
    public static Tick[] getMinorTicks( Rule rule, double dlo, double dhi ) {
        List<Tick> list = new ArrayList<Tick>();
        for ( long index = rule.floorIndex( dlo );
              rule.indexToValue( index ) <= dhi; index++ ) {
            double[] minors = rule.getMinors( index );
            for ( int imin = 0; imin < minors.length; imin++ ) {
                double minor = minors[ imin ];
                if ( minor >= dlo && minor <= dhi ) {
                    list.add( new Tick( minor ) );
                }
            }
        }
        return list.toArray( new Tick[ 0 ] );
    }

    /**
     * Acquire a rule for labelling a linear or logarithmic axis.
     * The tick density is determined by two parameters,
     * <code>approxMajorCount</code>, which gives a baseline value for
     * the number of ticks required over the given range, and
     * <code>adjust</code>.
     * Increasing <code>adjust</code> will give more major ticks, and
     * decreasing it will give fewer ticks.
     *
     * @param   dlo     minimum axis data value
     * @param   dhi     maximum axis data value
     * @param   approxMajorCount  guide value for number of major ticks
     *                            in range
     * @param   adjust  adjusts density of major ticks
     * @param   log     true for logarithmic, false for linear
     * @return  tick generation rule
     */
    private static Rule createRawRule( double dlo, double dhi,
                                       double approxMajorCount, int adjust,
                                       final boolean log ) {

        /* Use specifically logarithmic labelling only if the axes are
         * logarithmic and the range is greater than a factor of ten. */
        if ( log && Math.log10( dhi / dlo ) > 1 ) {
            LogSpacer[] spacers = LogSpacer.SPACERS;
            assert spacers.length == 2;

            /* Work out how many decades a single major tick interval covers. */
            double nDecade = ( Math.log10( dhi ) - Math.log10( dlo ) )
                           / approxMajorCount;
            int iSpacer;

            /* If it's more than about 1, ticks will all be 10**n. */
            if ( nDecade >= 1 ) {
                iSpacer = - (int) nDecade;
            }

            /* Otherwise use a log rule with custom spacing. */
            else if ( nDecade >= 0.5 ) {
                iSpacer = 0;
            }
            else if ( nDecade >= 0.2 ) {
                iSpacer = 1;
            }
            else {
                iSpacer = 2;
            }

            /* Apply adjustment. */
            iSpacer += adjust;

            /* If a log rule is indicated, return it here. */
            if ( iSpacer < 0 ) {
                return new DecadeLogRule( -iSpacer );
            }
            else if ( iSpacer < 2 ) {
                return new SpacerLogRule( spacers[ iSpacer ] );
            }

            /* Otherwise fall back to linear tick marks.  This is probably
             * more dense, though it might not work well for large ranges.
             * Won't happen often though. */
        }

        /* Linear tick marks. */
        double approxMajorInterval = ( dhi - dlo ) / approxMajorCount;
        int exp = (int) Math.floor( Math.log10( approxMajorInterval ) );
        double oversize = approxMajorInterval / exp10( exp );
        assert oversize >= 1 && oversize < 10;
        final int maxLevel = LinearSpacer.SPACERS.length;
        int num = exp * maxLevel
                + LinearSpacer.getSpacerIndex( oversize ) - adjust;
        int[] div = divFloor( num, maxLevel );
        return new LinearRule( div[ 0 ], LinearSpacer.SPACERS[ div[ 1 ] ] );
    }

    /**
     * Generates a major tick mark label suitable for use with linear axes.
     * Some care is required assembling the label, to make sure we
     * avoid rounding issues (like 0.999999999999).
     * Double.toString() is not good enough.
     *
     * @param  mantissa  multiplier
     * @param  exp  power of 10
     * @return  tick label text
     */
    public static Caption linearLabel( long mantissa, int exp ) {
        boolean minus = mantissa < 0;
        String sign = minus ? "-" : "";
        String digits = Long.toString( minus ? -mantissa : mantissa );
        int ndigit = digits.length();
        int sciLimit = 3;
        if ( mantissa == 0 ) {
            return Caption.createCaption( "0" );
        }
        else if ( exp >= 0 && exp <= sciLimit ) {
            return Caption.createCaption( new StringBuffer()
                                         .append( sign )
                                         .append( digits )
                                         .append( zeros( exp ) )
                                         .toString() );
        }
        else if ( exp < 0 && exp >= -sciLimit ) {
            int pointPos = ndigit + exp;
            if ( pointPos <= 0 ) {
                return Caption.createCaption( new StringBuffer()
                                             .append( sign )
                                             .append( "0." )
                                             .append( zeros( -pointPos ) )
                                             .append( digits )
                                             .toString() );
            }
            else {
                StringBuffer sbuf = new StringBuffer();
                sbuf.append( sign )
                    .append( digits.substring( 0, pointPos ) );
                if ( pointPos < ndigit ) {
                    sbuf.append( "." )
                        .append( digits.substring( pointPos ) );
                }
                return Caption.createCaption( sbuf.toString() );
            }
        }
        else if ( exp > sciLimit ) {
            StringBuffer sbuf = new StringBuffer();
            sbuf.append( sign )
                .append( digits.charAt( 0 ) );
            int postDigit = ndigit - 1;
            if ( postDigit > 0 ) {
                sbuf.append( "." )
                    .append( digits.substring( 1 ) );
            }
            int pexp = exp + postDigit;
            if ( pexp > sciLimit ) {
                return createSciCaption( sbuf.toString(), pexp );
            }
            else {
                sbuf.append( zeros( pexp ) );
                return Caption.createCaption( sbuf.toString() );
            }
        }
        else if ( exp < -sciLimit ) {
            StringBuffer sbuf = new StringBuffer();
            sbuf.append( sign );
            int pexp = exp + ndigit;
            if ( pexp > 0 ) {
                sbuf.append( digits.substring( 0, pexp ) )
                    .append( "." )
                    .append( digits.substring( pexp ) );
                return Caption.createCaption( sbuf.toString() );
            }
            else if ( pexp <= 0 && pexp >= -sciLimit ) {
                sbuf.append( "0." )
                    .append( zeros( -pexp ) )
                    .append( digits );
                return Caption.createCaption( sbuf.toString() );
            }
            else if ( pexp < -sciLimit ) {
                sbuf.append( digits.charAt( 0 ) );
                int postDigit = ndigit - 1;
                if ( postDigit > 0 ) {
                    sbuf.append( "." )
                        .append( digits.substring( 1 ) );
                }
                return createSciCaption( sbuf.toString(), pexp - 1 );
            }
            else {
                assert false;
                return Caption.createCaption( "??" );
            }
        }
        else {
            assert false;
            return Caption.createCaption( "??" );
        }
    }

    /**
     * Generates a major tick mark label suitable for use with logarithmic axes.
     *
     * @param  mantissa  multiplier in range 1-9 (inclusive)
     * @param  exponent  power of 10
     * @return  tick label text
     */
    private static Caption logLabel( long mantissa, int exponent ) {
        assert mantissa > 0 && mantissa < 10;
        double value = mantissa * exp10( exponent );

        /* Some care is required assembling the label, to make sure we
         * avoid rounding issues (like 0.999999999999).
         * Double.toString() is not good enough. */
        String smantissa = Long.toString( mantissa );
        if ( exponent == 0 ) {
            return Caption.createCaption( smantissa );
        }
        else if ( exponent > -4 && exponent < 0 ) {
            return Caption
                  .createCaption( "0." + zeros( - exponent - 1 ) + smantissa );
        }
        else if ( exponent < 4 && exponent > 0 ) {
            return Caption.createCaption( smantissa + zeros( exponent ) );
        }
        else {
            return createSciCaption( mantissa == 1 ? null : smantissa,
                                     exponent );
        }
    }

    /**
     * Returns a caption representing a number in scientific notation.
     *
     * @param  mantissa  mantissa string; if null, no mantissa will be rendered,
     *                   meaning that it is considered to be unity
     * @param  exponent  decimal exponent value as an integer
     */
    private static Caption createSciCaption( String mantissa, int exponent ) {
        String txt = new StringBuffer()
            .append( mantissa == null ? "1" : mantissa )
            .append( "e" )
            .append( Integer.toString( exponent ) )
            .toString();
        String latex = new StringBuffer()
            .append( mantissa == null ? "" : mantissa + "\\!\\times\\!" )
            .append( "10^{" )
            .append( Integer.toString( exponent ) )
            .append( "}" )
            .toString();
        return Caption.createCaption( txt, latex );
    }

    /**
     * Determines whether the labels for a set of tick marks
     * would overlap when painted on a given axis.
     *
     * @param  ticks      major tick marks
     * @param  axis       axis on which the ticks will be drawn
     * @param  captioner  caption painter
     * @param  orient     label orientation
     * @return   true  iff some of the ticks are so close to each other that
     *                 their labels will overlap
     */
    public static boolean overlaps( Tick[] ticks, Axis axis,
                                    Captioner captioner, Orientation orient ) {
        int cpad = captioner.getPad();
        Point2D[] lastBox = null;
        for ( int i = 0; i < ticks.length; i++ ) {
            Tick tick = ticks[ i ];
            Caption label = tick.getLabel();
            if ( label != null ) {
                int gx = (int) axis.dataToGraphics( tick.getValue() );
                Rectangle cbounds = captioner.getCaptionBounds( label );
                cbounds.width += cpad;
                AffineTransform oTrans =
                    orient.captionTransform( cbounds, cpad );
                Point2D.Double[] box = transformBox( cbounds, oTrans );
                for ( Point2D.Double p : box ) {
                    p.x += gx;
                }
                if ( lastBox != null &&
                     convexPolygonIntersect( box, lastBox ) ) {
                    return true;
                }
                lastBox = box;
            }
        }
        return false;
    }

    /**
     * Integer division with remainder which rounds towards minus infinity.
     * This is unlike the <code>/</code> operator which rounds towards zero.
     *
     * @param  numerator  value to divide
     * @param  divisor    value to divide by
     * @return  2-element array: (result of integer division rounded down,
     *                            positive remainder)
     */
    private static int[] divFloor( int numerator, int divisor ) {
        int whole = numerator / divisor;
        int part = numerator % divisor;
        if ( part < 0 ) {
            part += divisor;
            whole -= 1;
        }
        assert whole * divisor + part == numerator;
        return new int[] { whole, part };
    }

    /**
     * Power of ten.
     *
     * @param  exp  exponent
     * @return   <code>pow(10,exp)</code>
     */
    private static double exp10( int exp ) {
        return Math.pow( 10, exp );
    }

    /**
     * Returns a string which is a given number of zeros.
     *
     * @param  n  number of zeros
     * @return  zero-filled string of length <code>n</code>
     */
    private static String zeros( int n ) {
        StringBuffer sbuf = new StringBuffer( n );
        for ( int i = 0; i < n; i++ ) {
            sbuf.append( '0' );
        }
        return sbuf.toString();
    }

    /**
     * Transforms the vertices of a Rectangle using a given transform,
     * and returns the result as a 4-element array of Points.
     *
     * @param  box  rectangle
     * @param  trans  transform
     * @return  4-element array of transformed vertices of rectangle
     */
    private static Point2D.Double[] transformBox( Rectangle box,
                                                  AffineTransform trans ) {
        return new Point2D.Double[] {
            (Point2D.Double)
            trans.transform( new Point( box.x, box.y ),
                             new Point2D.Double() ),
            (Point2D.Double)
            trans.transform( new Point( box.x + box.width, box.y ),
                             new Point2D.Double() ),
            (Point2D.Double)
            trans.transform( new Point( box.x + box.width, box.y + box.height ),
                             new Point2D.Double() ),
            (Point2D.Double)
            trans.transform( new Point( box.x, box.y + box.height ),
                             new Point2D.Double() ),
        };
    }

    /**
     * Determines whether two convex polygons,
     * represented as lists of vertices, have any intersection.
     * If the two touch without overlapping it doesn't count.
     *
     * <p>If the polygons are not convex, behaviour is undefined
     *
     * @param  poly1  array of points representing first polygon
     * @param  poly2  array of points representing second polygon
     * @return   true iff there is a non-empty overlap between the two
     */
    private static boolean convexPolygonIntersect( Point2D[] poly1,
                                                   Point2D[] poly2 ) {

        /* For each side of each polygon, check whether all the vertices
         * of one polygon fall on one side and all the vertices of the
         * other polygon fall on the other side of it.
         * Vertices on the line are ignored.
         * If there is any side for which this is true, then you can draw a
         * line between the two polygons, and there is no overlap.
         * Otherwise you can't, and there is. */
        List<Pair<Point2D[]>> pairs = new ArrayList<>();
        pairs.add( new Pair<Point2D[]>( poly1, poly2 ) );
        pairs.add( new Pair<Point2D[]>( poly2, poly1 ) );
        for ( Pair<Point2D[]> pair : pairs ) {
            Point2D[] polyA = pair.getItem1();
            Point2D[] polyB = pair.getItem2();
            int nva = polyA.length;
            for ( int iva = 0; iva < nva; iva++ ) {
                Line line = new Line( polyA[ iva ], polyA[ ( iva+1 ) % nva ] );
                if ( line.sameSide( polyA ) * line.sameSide( polyB ) == -1 ) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Represents a line on the plane defined by two points.
     */
    private static class Line {

        double x0_;
        double y0_;
        double x1_;
        double y1_;

        /**
         * Constructs a line that runs through two points.
         *
         * @param  p0  one point
         * @param  p1  other point
         */
        Line( Point2D p0, Point2D p1 ) {
            x0_ = p0.getX();
            y0_ = p0.getY();
            x1_ = p1.getX();
            y1_ = p1.getY();
        }

        /**
         * Returns a value of -1, 0 or +1 according to which side of
         * this line a given point is on, using some unspecified convention
         * for sense.  Zero means it's on the line.
         *
         * @param  p  test point
         * @return  0 for on the line, +1 for one side, -1 for the other side
         */
        int getSide( Point2D p ) {
            return (int) Math.signum( ( p.getX() - x0_ ) * ( y1_ - y0_ )
                                    - ( p.getY() - y0_ ) * ( x1_ - x0_ ) );
        }

        /**
         * Returns a value of -1, 0, or +1 indicating which side of this line
         * all the supplied points are on, using some unspecified convention
         * for sense.  Zero means that some are definitely on one side
         * and others are definitely on the other.  Points on the line
         * are ignored.
         *
         * @param  points  test points, not collinear
         * @return  +1 if they are all either on the line or on one side,
         *          -1 if they are all either on the line or on the other side,
         *           0 if there is at least one point on each side
         */
        int sameSide( Point2D[] points ) {
            int kside = 0;
            for ( Point2D p : points ) {
                int iside = getSide( p );
                if ( iside != 0 ) {
                    if ( kside == 0 ) {
                        kside = iside;
                    }
                    else if ( kside != iside ) {
                        return 0;
                    }
                }
            }
            return kside;
        }
    }

    /**
     * Defines a specific rule for generating major and minor axis tick marks.
     * The major tick marks defined by this rule are labelled by a
     * contiguous sequence of long integer indices, which increase in the
     * direction of axis value increase.
     */
    public interface Rule {

        /**
         * Returns the largest major tick mark index value that identifies
         * an axis value less than or equal to a supplied axis value.
         *
         * @param  value  axis reference value
         * @return   major tick index for an axis point equal to
         *           or just less than <code>value</code>
         */
        long floorIndex( double value );

        /**
         * Returns the axis values for minor tickmarks that fall between the
         * a given major tick mark and the next one.
         *
         * @param   index  major tick mark index
         * @return   minor tick mark axis values between the axis values
         *           for major ticks <code>index</code> and <code>index+1</code>
         */
        double[] getMinors( long index );

        /**
         * Returns the axis value identified by a given major tick mark index.
         * Note the result may be infinite if the relevant value cannot
         * be represented by a double.
         *
         * @param  index  major tick index
         * @return  axis value for major tick
         */
        double indexToValue( long index );

        /**
         * Returns a text string to label the major tick identified by
         * a given index.
         *
         * @param  index  major tick index
         * @return  label string for major tick
         */
        Caption indexToLabel( long index );
    }

    /**
     * Defines how to split up an interval of approximately unit extent
     * into major and minor divisions of equal linear size.
     */
    private static class LinearSpacer {
        private final double thresh_;
        private final int major_;
        private final double[] minors_;

        /** Known instances, in order. */
        public static final LinearSpacer[] SPACERS = new LinearSpacer[] {
            new LinearSpacer( 2.5, 1, 0.2 ),
            new LinearSpacer( 4.5, 2, 0.5 ),
            new LinearSpacer( 7.5, 5, 1 ),
        };

        /**
         * Constructor.
         *
         * @param   oversizeThresh  selection threshold, in range 1..10
         * @param   major   interval between major ticks
         * @param   minor   interval between minor ticks
         */
        LinearSpacer( double oversizeThresh, int major, double minor ) {
            thresh_ = oversizeThresh;
            major_ = major;
            int nminor = (int) Math.round( major / minor );
            minors_ = new double[ nminor - 1 ];
            for ( int i = 1; i < nminor; i++ ) {
                minors_[ i - 1 ] = minor * i;
            }
        }

        /**
         * Returns the index into SPACERS of the spacer appropriate
         * for a given oversize value.
         *
         * @param   oversize  factor by which ten is too large for the
         *                    major tick interval; in range 1..10
         * @return  index into SPACERS, or SPACERS.length if too big
         */
        public static int getSpacerIndex( double oversize ) {
            for ( int i = 0; i < SPACERS.length; i++ ) {
                if ( oversize <= SPACERS[ i ].thresh_ ) {
                    return i;
                }
            }
            return SPACERS.length;
        }
    }

    /**
     * Defines how to split up an interval of approximately a factor of 10
     * into major and minor divisions of approximately equal logarithmic size.
     */
    private static class LogSpacer {
        private final int[] majors_;
        private final double[][] minors_;

        /** Known instances, in order. */
        public static final LogSpacer[] SPACERS = new LogSpacer[] {
            new LogSpacer( new int[] { 1, 2, 5, },
                           new double[][] { { 1.5, },
                                            { 3.0, 4.0, },
                                            { 6.0, 7.0, 8.0, 9.0, }, } ),
            new LogSpacer( new int[] { 1, 2, 3, 4, 5 },
                           new double[][] { { 1.5, },
                                            { 2.5, },
                                            { 3.5, },
                                            { 4.5, },
                                            { 6.0, 7.0, 8.0, 9.0, }, } ),
        };

        /**
         * Constructor.
         *
         * @param  major tick marks between 1 and 10
         * @param  minors  array for each major of minor tick marks
         */
        public LogSpacer( int[] majors, double[][] minors ) {
            majors_ = majors;
            minors_ = minors;
        }
    }

    /**
     * Rule instance that works with a LinearSpacer.
     */
    private static class LinearRule implements Rule {
        private final int exp_;
        private final LinearSpacer spacer_;
        private final double mult_;

        /**
         * Constructor.
         *
         * @param  log to base 10 of multiplication factor
         * @param  spacer  splits up a decade into round intervals
         */
        LinearRule( int exp, LinearSpacer spacer ) {
            exp_ = exp;
            spacer_ = spacer;
            mult_ = exp10( exp ) * spacer.major_;
        }

        public long floorIndex( double value ) {
            return (long) Math.floor( value / mult_ );
        }

        public double indexToValue( long index ) {
            double value = index * mult_;
            assert floorIndex( value + 1e-2 * mult_ ) == index 
                || Math.abs( index ) > 1e14;  // precision lost
            return value;
        }

        public double[] getMinors( long index ) {
            double major = indexToValue( index );
            double[] minors = new double[ spacer_.minors_.length ];
            for ( int i = 0; i < minors.length; i++ ) {
                minors[ i ] = major + exp10( exp_ ) * spacer_.minors_[ i ];
            }
            return minors;
        }

        public Caption indexToLabel( long index ) {
            long mantissa = index * spacer_.major_;
            return linearLabel( mantissa, exp_ );
        }
    }

    /**
     * Rule instance for logarithmic intervals in which each major tick
     * represents one or more decades.
     */
    private static class DecadeLogRule implements Rule {
        private final int nDecade_;
        private final int absFloor_;
        private final int absCeil_;
        private final double[] minors_;

        /**
         * Constructor.
         *
         * @param  nDecade  number of decades per major tick
         */
        public DecadeLogRule( int nDecade ) {
            nDecade_ = nDecade;
            absFloor_ =
                (int) Math.ceil( Math.log10( Double.MIN_VALUE ) / nDecade );
            absCeil_ =
                (int) Math.floor( Math.log10( Double.MAX_VALUE ) / nDecade );
            if ( nDecade == 1 ) {
                minors_ = new double[] { 2, 3, 4, 5, 6, 7, 8, 9 };
            }
            else if ( nDecade == 2 ) {
                minors_ = new double[] { 10, 20, 30, 40, 50, 60, 70, 80, 90, };
            }
            else if ( nDecade <= 10 ) {
                minors_ = new double[ nDecade_ - 1 ];
                for ( int i = 1; i < nDecade_; i++ ) {
                    minors_[ i - 1 ] = exp10( i );
                }
            }
            else {
                // no minor ticks; for these very large factors I should
                // really restrict nDecade to some multiple of a
                // round number N and put minor ticks every N decades.
                minors_ = new double[ 0 ];
            }
        }

        public long floorIndex( double value ) {
            return (long)
                   Math.max( Math.floor( Math.log10( value ) / nDecade_ ),
                             absFloor_ );
        }

        public double indexToValue( long index ) {
            double value = exp10( (int) index * nDecade_ );
            assert ( floorIndex( value * Math.pow( 10, nDecade_ * 1e-8 ) )
                     == index )
                || isExtremeIndex( index );
            return value;
        }

        public double[] getMinors( long index ) {
            double[] minors = new double[ minors_.length ];
            double major = indexToValue( index );
            for ( int i = 0; i < minors.length; i++ ) {
                minors[ i ] = major * minors_[ i ];
            }
            return minors;
        }

        public Caption indexToLabel( long index ) {
            return logLabel( 1, (int) index * nDecade_ );
        }

        /**
         * Tests whether the given index is so near the end of the double
         * precision range that weird things might happen.
         * This method is currently only used in assertions.
         * 
         * @param  index  index to test
         * @return  true iff index is extremely small or big
         */
        private boolean isExtremeIndex( long index ) {
            return index < absFloor_ || index > absCeil_;
        }
    }

    /**
     * Rule instance that works with a LogSpacer.
     */
    private static class SpacerLogRule implements Rule {
        private final int[] majors_;
        private final double[][] minors_;

        /**
         * Constructor.
         *
         * @param  spacer  splits up a decade into round intervals
         */
        public SpacerLogRule( LogSpacer spacer ) {
            majors_ = spacer.majors_;
            minors_ = spacer.minors_;
        }

        public long floorIndex( double value ) {
            int expFloor = (int) Math.floor( Math.log10( value ) );
            double mult = value / exp10( expFloor );
            int ik = 0;
            for ( int i = 0; i < majors_.length; i++ ) {
                if ( mult >= majors_[ i ] ) {
                    ik = i;
                }
            }
            return expFloor * majors_.length + ik;
        }

        public double indexToValue( long index ) {
            int[] div = divFloor( (int) index, majors_.length );
            int exp = div[ 0 ];
            int ik = div[ 1 ];
            double value = majors_[ ik ] * exp10( exp );
            assert floorIndex( value * 1.001 ) == index
                || Math.abs( index ) > 1e13;
            return value;
        }

        public double[] getMinors( long index ) {
            int[] div = divFloor( (int) index, majors_.length );
            int exp = div[ 0 ];
            int ik = div[ 1 ];
            double base = exp10( exp );
            double[] kminors = minors_[ ik ];
            double[] minors = new double[ kminors.length ];
            for ( int i = 0; i < kminors.length; i++ ) {
                minors[ i ] = base * kminors[ i ];
            }
            return minors;
        }

        public Caption indexToLabel( long index ) {
            int[] div = divFloor( (int) index, majors_.length );
            int exp = div[ 0 ];
            int ik = div[ 1 ];
            return logLabel( majors_[ ik ], exp );
        }
    }

    /**
     * Decorates a rule with assertions.
     * It's easy to get the tick mark labelling wrong, and easy not
     * to notice it if it happens, so this check is worth doing.
     */
    private static abstract class CheckRule implements Rule {

        private final Rule base_;

        /**
         * Constructor.
         *
         * @param  base  rule instance to be decorated
         */
        CheckRule( Rule base ) {
            base_ = base;
        }

        /**
         * Tests whether the label text matches the value.
         *
         * @param   label  major tick label text
         * @param   value  major tick value
         * @return   true iff the label correctly represents the value
         */
        abstract boolean checkLabel( Caption label, double value );

        public Caption indexToLabel( long index ) {
            Caption label = base_.indexToLabel( index );
            assert checkLabel( label, base_.indexToValue( index ) )
                 : '"' + label.toText() + '"' +
                   " != " + base_.indexToValue( index );
            return label;
        }

        public long floorIndex( double value ) {
            return base_.floorIndex( value );
        }
        public double[] getMinors( long index ) {
            return base_.getMinors( index );
        }
        public double indexToValue( long index ) {
            return base_.indexToValue( index );
        }
    }
}
