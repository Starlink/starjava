package uk.ac.starlink.ttools;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.starlink.table.Domain;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.tfcat.Geometry;
import uk.ac.starlink.tfcat.LinearRing;
import uk.ac.starlink.tfcat.Position;
import uk.ac.starlink.tfcat.TfcatObject;
import uk.ac.starlink.tfcat.TfcatUtil;
import uk.ac.starlink.util.DoubleList;
import uk.ac.starlink.util.LongList;

/**
 * Domain representing two-dimensional regions on a common surface.
 * The surface may be a plane or the surface of a sphere.
 *
 * <p>The sole instance of this singleton class is available as the
 * {@link #INSTANCE} static member.
 *
 * @author   Mark Taylor
 * @since    14 Apr 2020
 */
public class AreaDomain implements Domain<AreaMapper> {

    /** Singleton instance. */
    public static final AreaDomain INSTANCE = new AreaDomain();

    /** STC-S - see TAP 1.0 section 6. */
    public static final AreaMapper STCS_MAPPER = createStcsMapper();

    /** Mapper for (x,y,r) circles - see DALI 1.1 section 3.3.6. */
    public static final AreaMapper CIRCLE_MAPPER =
        createSimpleNumericDaliMapper( Area.Type.CIRCLE,
                                       "3-element array (<code>x</code>, "
                                     + "<code>y</code>, <code>r</code>)",
                                       "3-element array (<code>ra</code>, "
                                     + "<code>dec</code>, <code>r</code>)" );

    /** Mapper for (xi,yi,...) polygons - see DALI 1.1 section 3.3.7. */
    public static final AreaMapper POLYGON_MAPPER = createPolygonMapper();

    /** Mapper for (x,y) points - see DALI 1.1 section 3.3.5. */
    public static final AreaMapper POINT_MAPPER =
        createSimpleNumericDaliMapper( Area.Type.POINT,
                                       "2-element array "
                                     + "(<code>x</code>,<code>y</code>)",
                                       "2-element array "
                                     + "(<code>ra</code>,<code>dec</code>)" );

    /** Mapper for ASCII format MOCs. */
    public static final AreaMapper ASCIIMOC_MAPPER = createAsciiMocMapper();

    /** Mapper for single HEALPix UNIQ values. */
    public static final AreaMapper UNIQ_MAPPER = createUniqMapper();

    /** Mapper for TFCat strings. */
    public static final AreaMapper TFCAT_MAPPER = createTfcatMapper();

    private static final String WORDS_REGEX =
        "\\s*([A-Za-z]+)\\s+([A-Za-z][A-Za-z0-9]*\\s+)*";
    private static final String NUMBER_REGEX =
        "\\s*([-+]?[0-9]*\\.?[0-9]+(?:[eE][-+]?[0-9]+)?)";
    private static final Pattern WORDS_PATTERN =
        Pattern.compile( WORDS_REGEX );
    private static final Pattern NUMBER_PATTERN =
        Pattern.compile( NUMBER_REGEX );
    private static final Pattern PARENTHESIS_PATTERN =
        Pattern.compile( "\\s*\\((.*)\\)\\s*" );
    private static final Pattern SMOC_TOKENS_PATTERN =
        Pattern.compile( "(?:\\s*s)?\\s*([^\\s]+)" );
    private static final Pattern MOC_PATTERN =
        Pattern.compile( "(?:([0-9]+)/)?(?:([0-9]+)(?:-([0-9]+))?)?" );

    // Note the use of a possessive quantifier (++) for the number matching
    // here - this reduces the recursion depth, and hence the chance of
    // a StackOverflowError which can otherwise happen when looking
    // at a very long list of numbers.
    private static final Pattern TERM_PATTERN =
        Pattern.compile( "(" + WORDS_REGEX + ")+"
                       + "(" + NUMBER_REGEX + ")++" );

    /**
     * Private sole constructor prevents external instantiation.
     */
    private AreaDomain() {
    }

    public String getDomainName() {
        return "Area";
    }

    public AreaMapper[] getMappers() {
        return new AreaMapper[] {
            POINT_MAPPER,
            CIRCLE_MAPPER,
            POLYGON_MAPPER,
            ASCIIMOC_MAPPER,
            UNIQ_MAPPER,
            STCS_MAPPER,
            TFCAT_MAPPER,
        };
    }

    public AreaMapper getProbableMapper( ValueInfo info ) {
        if ( info == null ) {
            return null;
        }
        for ( DomainMapper mapper : info.getDomainMappers() ) {
            if ( mapper instanceof AreaMapper ) {
                return (AreaMapper) mapper;
            }
        }
        Class<?> clazz = info.getContentClass();
        String name = info.getName();
        String xtype = info.getXtype();
        String ucd = info.getUCD();
        if ( clazz.equals( String.class ) ) {
            if ( "moc".equalsIgnoreCase( xtype ) ||
                 "smoc".equalsIgnoreCase( xtype ) ) {
                return ASCIIMOC_MAPPER;
            }
            else if ( "stc-s".equalsIgnoreCase( xtype ) ||
                      "stc".equalsIgnoreCase( xtype ) ) {
                return STCS_MAPPER;
            }
            else if ( "s_region".equals( name ) ||
                      "pos.outline;obs.field".equals( ucd ) ||
                      ( ucd != null && ucd.startsWith( "pos.outline" ) ) ) {
                return STCS_MAPPER;
            }
            else if ( "tfcat".equals( name ) ||
                      "tfcat".equalsIgnoreCase( xtype ) ) { // non-standard
                return TFCAT_MAPPER;
            }
            else {
                return null;
            }
        }
        else if ( isNumArray( clazz ) ) {
            if ( "circle".equalsIgnoreCase( xtype ) ) {
                return CIRCLE_MAPPER;
            }
            else if ( "polygon".equalsIgnoreCase( xtype ) ) {
                return POLYGON_MAPPER;
            }
            else if ( "point".equalsIgnoreCase( xtype ) ) {
                return POINT_MAPPER;
            }
            else {
                return null;
            }
        }
        else if ( Integer.class.equals( clazz ) || Long.class.equals( clazz ) ){
            if ( "UNIQ".equalsIgnoreCase( name ) ) {
                return UNIQ_MAPPER;
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }

    public AreaMapper getPossibleMapper( ValueInfo info ) {
        if ( info == null ) {
            return null;
        }
        for ( DomainMapper mapper : info.getDomainMappers() ) {
            if ( mapper instanceof AreaMapper ) {
                return (AreaMapper) mapper;
            }
        }
        Class<?> clazz = info.getContentClass();
        if ( String.class.equals( clazz ) ) {
            return STCS_MAPPER;
        }
        else if ( isNumArray( clazz ) ) {
            int[] shape = info.getShape();
            if ( shape != null && shape.length == 1 ) {
                int nel = shape[ 0 ];
                if ( nel == 3 ) {
                    return CIRCLE_MAPPER;
                }
                else if ( nel == 2 ) {
                    return POINT_MAPPER;
                }
                else if ( nel % 2 == 0 && nel >= 6 || nel < 0 ) {
                    return POLYGON_MAPPER;
                }
                else {
                    return null;
                }
            }
            else if ( shape == null ) {
                return POLYGON_MAPPER;
            }
            else {
                return null;
            }
        }
        else if ( Integer.class.equals( clazz ) || Long.class.equals( clazz ) ){
            return UNIQ_MAPPER;
        }
        else {
            return null;
        }
    }

    /**
     * Indicates whether a given class is a numerical array of the sort
     * that the mappers implemented within this class can use.
     *
     * @param   clazz  source value class
     * @param   true iff clazz is a double[] or float[] array
     */
    private static boolean isNumArray( Class<?> clazz ) {
        return double[].class.equals( clazz )
            || float[].class.equals( clazz );
    }

    /**
     * Returns a mapper that can turn STC-S Strings into areas.
     *
     * @return  area mapper instance
     */
    private static AreaMapper createStcsMapper() {
        String descrip = new StringBuffer()
            .append( "Region description using STC-S syntax;\n" )
            .append( "see <webref " )
            .append( "url='http://www.ivoa.net/documents/TAP/20100327/'>" )
            .append( "TAP 1.0</webref>, section 6.\n" )
            .append( "Note there are some restrictions:\n" )
            .append( "<code>&lt;frame&gt;</code>, " )
            .append( "<code>&lt;refpos&gt;</code> and " )
            .append( "<code>&lt;flavor&gt;</code> metadata are ignored,\n" )
            .append( "polygon winding direction is ignored " )
            .append( "(small polygons are assumed)\n" )
            .append( "and the <code>INTERSECTION</code> and <code>NOT</code> " )
            .append( "constructions are not supported.\n" )
            .append( "The non-standard <code>MOC</code> construction " )
            .append( "is supported." )
            .toString();
        return new AreaMapper( "STC-S", descrip, String.class ) {
            public Function<Object,Area> areaFunction( Class<?> clazz ) {
                if ( String.class.equals( clazz ) ) {
                    return obj -> obj instanceof String
                                ? stcsArea( (String) obj, true )
                                : null;
                }
                else {
                    return null;
                }
            }
        };
    }

    /**
     * Returns a mapper that can turn ASCII MOC serializations
     * into areas.
     *
     * @return   MOC mapper instance
     */
    private static AreaMapper createAsciiMocMapper() {
        String descrip = new StringBuffer()
            .append( "Region description using ASCII MOC syntax;\n" )
            .append( "see <webref " )
            .append( "url='http://www.ivoa.net/documents/MOC/'>" )
            .append( "MOC 2.0</webref> sec 4.3.2.\n" )
            .append( "Note there are currently a few issues\n" )
            .append( "with MOC plotting, especially for large pixels." )
            .toString();
        return new AreaMapper( "MOC-ASCII", descrip, String.class ) {
            public Function<Object,Area> areaFunction( Class<?> clazz ) {
                if ( String.class.equals( clazz ) ) {
                    return obj -> obj instanceof String
                                ? mocArea( (String) obj )
                                : null;
                }
                else {
                    return null;
                }
            }
        };
    }

    /**
     * Returns a mapper that can turn single HEALPix/MOC UNIQ values
     * into areas.
     *
     * @return  UNIQ mapper instance
     */
    private static AreaMapper createUniqMapper() {
        String descrip = String.join( "\n",
            "Region description representing a single HEALPix cell",
            "as defined by an UNIQ value, see",
            "<webref url='http://www.ivoa.net/documents/MOC/'>MOC 2.0</webref>",
            "sec 4.3.1."
        );
        return new AreaMapper( "UNIQ", descrip, Number.class ) {
            public Function<Object,Area> areaFunction( Class<?> clazz ) {
                if ( Integer.class.equals( clazz ) ||
                     Long.class.equals( clazz ) ) {
                    return obj -> {
                        if ( obj instanceof Integer || obj instanceof Long ) {
                            long uniq = ((Number) obj).longValue();
                            double duniq = Double.longBitsToDouble( uniq );
                            return new Area( Area.Type.MOC,
                                             new double[] { duniq } );
                        }
                        else {
                            return null;
                        }
                    };
                }
                else {
                    return null;
                }
            }
        };
    }

    /**
     * Returns a mapper that makes an attempt at turning TFCat texts
     * into areas.  Support is partial.
     *
     * @return  TFCat mapper
     */
    private static AreaMapper createTfcatMapper() {
        String stdUrl = "https://doi.org/10.25935/6068-8528";
        String descrip = String.join( "\n",
            "Time-Frequency region defined by the",
            "<webref url='https://doi.org/10.25935/6068-8528'" +
                     ">TFCat standard</webref>.",
            "Support is currently incomplete;",
            "holes in Polygons and MultiPolygons are not displayed correctly,",
            "single Points may not be displayed,",
            "and Coordinate Reference System information is ignored.",
        "" );
        return new AreaMapper( "TFCAT", descrip, String.class ) {
            public Function<Object,Area> areaFunction( Class<?> clazz ) {
                if ( String.class.equals( clazz ) ) {
                    return obj -> obj instanceof String
                                ? tfcatArea( (String) obj )
                                : null;
                }
                else {
                    return null;
                }
            }
            @Override
            public String getSkySourceDescription() {
                return null;
            }
        };
    }

    /**
     * Returns a mapper that can turn a particular type of area represented
     * as a numerical array as described in DALI 1.1. section 3.3
     * into an area object.  All numeric values are required to be definite
     * (not NaN).
     * This only works if the numeric array matches the requirements of
     * the AreaType constructor.
     *
     * @param   areaType   area type
     * @param   descrip    description of mapping type
     * @param   skyDescrip  description of mapping type suitable for sky
     */
    private static AreaMapper
            createSimpleNumericDaliMapper( final Area.Type areaType,
                                           String descrip, String skyDescrip ) {
        return new AreaMapper( areaType.toString(), descrip, Object.class ) {
            public Function<Object,Area> areaFunction( Class<?> clazz ) {
                if ( double[].class.equals( clazz ) ) {
                    return obj -> {
                        if ( obj instanceof double[] ) {
                            double[] data = (double[]) obj;
                            if ( areaType.isLegalArrayLength( data.length ) &&
                                 allDefinite( data ) ) {
                                return new Area( areaType, data );
                            }
                        }
                        return null;
                    };
                }
                else if ( float[].class.equals( clazz ) ) {
                    return obj -> {
                        if ( obj instanceof float[] ) {
                            float[] fdata = (float[]) obj;
                            int nd = fdata.length;
                            if ( areaType.isLegalArrayLength( nd ) &&
                                 allDefinite( fdata ) ) {
                                double[] ddata = new double[ nd ];
                                for ( int i = 0; i < nd; i++ ) {
                                    ddata[ i ] = fdata[ i ];
                                }
                                return new Area( areaType, ddata );
                            }
                        }
                        return null;
                    };
                }
                else {
                    return null;
                }
            }
            @Override
            public String getSkySourceDescription() {
                return skyDescrip;
            }
        };
    }

    /**
     * Returns a mapper that turns numeric arrays into polygons
     * or multipolygons.  Arrays consist of pairs of coordinates (x, y)
     * and polygons may be delimited by (NaN, NaN).
     * This works for DALI 1.1 polygon (sec 3.3.7).
     *
     * @return  polygon mapper
     */
    private static AreaMapper createPolygonMapper() {
        final Area.Type polygonType = Area.Type.POLYGON;
        String descrip = "2n-element array "
                       + "(<code>x1</code>,<code>y1</code>,"
                       + " <code>x2</code>,<code>y2</code>,...);\n"
                       + "a <code>NaN</code>,<code>NaN</code> pair "
                       + "can be used to delimit distinct polygons.";
        String skyDescrip = "2n-element array "
                          + "(<code>ra1</code>,<code>dec1</code>,"
                          + " <code>ra2</code>,<code>dec2</code>,...);\n"
                          + "a <code>NaN</code>,<code>NaN</code> pair "
                          + "can be used to delimit distinct polygons.";

        /* In many cases the array length may be fixed but not filled up
         * with useful polygon data, so take steps to compact the arrays
         * and only pass on the useful data to the Area constructor. */
        return new AreaMapper( "POLYGON", descrip, Object.class ) {
            public Function<Object,Area> areaFunction( Class<?> clazz ) {
                if ( double[].class.equals( clazz ) ) {
                    return obj -> {
                        if ( obj instanceof double[] ) {
                            double[] data = (double[]) obj;
                            int nd = data.length;
                            if ( polygonType.isLegalArrayLength( nd ) ) {
                                int ndef = 0;
                                for ( int i = nd - 1; i >= ndef; i-- ) {
                                    if ( ! Double.isNaN( data[ i ] ) ) {
                                        ndef = i + 1;
                                    }
                                }
                                if ( polygonType.isLegalArrayLength( ndef ) ) {
                                    final double[] pdata;
                                    if ( ndef == nd ) {
                                        pdata = data;
                                    }
                                    else {
                                        pdata = new double[ ndef ];
                                        System.arraycopy( data, 0, pdata, 0,
                                                          ndef );
                                    }
                                    return new Area( polygonType, pdata );
                                }
                                else {
                                    return null;
                                }
                            }
                            else {
                                return null;
                            }
                        }
                        else {
                            return null;
                        }
                    };
                }
                else if ( float[].class.equals( clazz ) ) {
                    return obj -> {
                        if ( obj instanceof float[] ) {
                            float[] data = (float[]) obj;
                            int nd = data.length;
                            if ( polygonType.isLegalArrayLength( nd ) ) {
                                int ndef = 0;
                                for ( int i = nd - 1; i >= ndef; i-- ) {
                                    if ( ! Float.isNaN( data[ i ] ) ) {
                                        ndef = i + 1;
                                    }
                                }
                                if ( polygonType.isLegalArrayLength( ndef ) ) {
                                    double[] pdata = new double[ ndef ];
                                    for ( int i = 0; i < ndef; i++ ) {
                                        pdata[ i ] = data[ i ];
                                    }
                                    return new Area( polygonType, pdata );
                                }
                                else {
                                    return null;
                                }
                            }
                            else {
                                return null;
                            }
                        }
                        else {
                            return null;
                        }
                    };
                }
                else {
                    return null;
                }
            }
            @Override
            public String getSkySourceDescription() {
                return skyDescrip;
            }
        };
    }

    /**
     * Decodes an STC-S string as an Area object.
     * Support is not complete: no INTERSECTION, no NOT;
     * frame, refpos and flavor are ignored;
     * polygon winding direction is ignored (small polygon is assumed).
     *
     * @param   stcs   STC-S string
     * @param   allowPoint  true if POINT type is acceptable
     * @return   area specified, or null
     */
    private static Area stcsArea( CharSequence stcs, boolean allowPoint ) {
        Matcher w0matcher = WORDS_PATTERN.matcher( stcs );
        if ( w0matcher.lookingAt() ) {
            String word0 = w0matcher.group( 1 ).toUpperCase();
            CharSequence remainder =
                stcs.subSequence( w0matcher.end(), stcs.length() );
            if ( "CIRCLE".equals( word0 ) ) {
                Area.Type circleType = Area.Type.CIRCLE;
                double[] numbers = getNumbers( remainder );
                return circleType.isLegalArrayLength( numbers.length )
                     ? new Area( circleType, numbers )
                     : null;
            }
            else if ( "POLYGON".equals( word0 ) ) {
                Area.Type polygonType = Area.Type.POLYGON;
                double[] numbers = getNumbers( remainder );
                return polygonType.isLegalArrayLength( numbers.length )
                     ? new Area( polygonType, numbers )
                     : null;
            }
            else if ( "BOX".equals( word0 ) ) {
                Area.Type polygonType = Area.Type.POLYGON;
                double[] numbers = getNumbers( remainder );
                if ( numbers.length == 4 ) {
                    double x1 = numbers[ 0 ];
                    double y1 = numbers[ 1 ];
                    double x2 = x1 + numbers[ 2 ];
                    double y2 = y1 + numbers[ 3 ];
                    double[] vertices = { x1, y1, x2, y1, x2, y2, x1, y2 };
                    return new Area( Area.Type.POLYGON, vertices );
                }
                else {
                    return null;
                }
            }
            else if ( allowPoint && "POSITION".equals( word0 ) ) {
                Area.Type pointType = Area.Type.POINT;
                double[] numbers = getNumbers( remainder );
                return pointType.isLegalArrayLength( numbers.length )
                     ? new Area( pointType, numbers )
                     : null;
            }
            else if ( "UNION".equals( word0 ) ) {
                Matcher parenMatcher = PARENTHESIS_PATTERN.matcher( remainder );
                if ( ! parenMatcher.matches() ) {
                    return null;
                }
                Matcher termMatcher =
                    TERM_PATTERN.matcher( parenMatcher.group( 1 ) );
                List<Area> shapes = new ArrayList<>();
                while ( termMatcher.find() ) {
                    Area termArea = stcsArea( termMatcher.group(), allowPoint );
                    if ( termArea != null ) {
                        shapes.add( termArea );
                    }
                    else {
                        return null;
                    }
                }
                return Area.createMultishape( shapes.toArray( new Area[ 0 ] ) );
            }
            else if ( "MOC".equals( word0 ) ) {
                return mocArea( remainder );
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }

    /**
     * Decodes an ASCII MOC string as an Area object.
     *
     * @param   txt  MOC ASCII serialization string
     * @return  area specified, or null
     */
    private static Area mocArea( CharSequence txt ) {
        LongList list = new LongList();
        Area.Type mocType = Area.Type.MOC;
        Matcher tokenMatcher = SMOC_TOKENS_PATTERN.matcher( txt );
        long order = -1;
        long kOrder = Integer.MIN_VALUE;
        while ( tokenMatcher.find() ) {
            String token = tokenMatcher.group( 1 );
            assert token.length() > 0;
            Matcher matcher = MOC_PATTERN.matcher( token );
            if ( matcher.matches() ) {
                String orderTxt = matcher.group( 1 );
                String ipix0Txt = matcher.group( 2 );
                String ipixnTxt = matcher.group( 3 );
                if ( orderTxt != null ) {
                    order = Long.parseLong( orderTxt );
                    kOrder = 4L << ( 2 * order );
                }
                if ( order >= 0 && ipix0Txt != null ) {
                    long ipix0 = Long.parseLong( ipix0Txt );
                    long ipixn = ipixnTxt == null ? ipix0
                                                  : Long.parseLong( ipixnTxt );
                    for ( long ipix = ipix0; ipix <= ipixn; ipix++ ) {
                        long nuniq = kOrder + ipix;
                        list.add( nuniq );
                    }
                }
            }
        }
        int n = list.size();
        if ( n > 0 ) {
            double[] data = new double[ n ];
            for ( int i = 0; i < n; i++ ) {
                data[ i ] = Double.longBitsToDouble( list.get( i ) );
            }
            return new Area( Area.Type.MOC, data );
        }
        else {
            return null;
        }
    }

    /**
     * Performs best-efforts decoding of a TFCat text as an Area object.
     *
     * @param  txt  TFCat text
     * @return   area specified, or null
     */
    private static Area tfcatArea( CharSequence txt ) {
        TfcatObject tfcat = TfcatUtil.parseTfcat( txt.toString(), null );
        if ( tfcat == null ) {
            return null;
        }
        List<Geometry<?>> geoms = TfcatUtil.getAllGeometries( tfcat );
        TfcatPointList plist = new TfcatPointList();
        for ( Geometry<?> geom : geoms ) {
            Object shape = geom.getShape();

            /* TFCat Point. */
            if ( shape instanceof Position ) {
                plist.addPosition( (Position) shape );
                plist.addBreak();
            }

            /* TFCat MultiPoint or LineString. */
            else if ( shape instanceof Position[] ) {
                plist.addLine( (Position[]) shape );
                plist.addBreak();
            }

            /* TFCat MultiLineString. */
            else if ( shape instanceof Position[][] ) {
                for ( Position[] line : (Position[][]) shape ) {
                    plist.addLine( line );
                    plist.addBreak();
                }
            }

            /* TFCat Polygon - holes not handled correctly. */
            else if ( shape instanceof LinearRing[] ) {
                plist.addPolygon( (LinearRing[]) shape );
                plist.addBreak();
            }

            /* TFCat MultiPolygon - holes not handled correctly. */
            else if ( shape instanceof LinearRing[][] ) {
                for ( LinearRing[] lrings : (LinearRing[][]) shape ) {
                    plist.addPolygon( lrings );
                    plist.addBreak();
                }
            }
            else {
                assert false;
            }
        }
        int n2 = plist.dlist_.size();
        if ( n2 > 2 ) {
            int n = n2 - 2;
            double[] data = new double[ n ];
            System.arraycopy( plist.dlist_.getDoubleBuffer(), 0, data, 0, n );
            return new Area( Area.Type.POLYGON, data );
        }
        else {
            return null;
        }
    }

    /**
     * Parses a whitespace-separated list of floating-point values.
     *
     * @param   cseq   input text 
     * @return   array of numeric values parsed from input
     */
    private static double[] getNumbers( CharSequence cseq ) {
        Matcher matcher = NUMBER_PATTERN.matcher( cseq );
        DoubleList dlist = new DoubleList(); 
        while ( matcher.find() ) {
            dlist.add( Double.parseDouble( matcher.group( 1 ) ) );
        }
        return dlist.toDoubleArray(); 
    }

    /**
     * Indicates whether all elements of a given double array are non-NaN.
     *
     * @param  data  floating point array
     * @return  false if any element is NaN
     */
    private static boolean allDefinite( double[] data ) {
        if ( data != null ) {
            int n = data.length;
            for ( int i = 0; i < n; i++ ) {
                if ( Double.isNaN( data[ i ] ) ) {
                    return false;
                }
            }
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Indicates whether all elements of a given float array are non-NaN.
     *
     * @param  data  floating point array
     * @return  false if any element is NaN
     */
    private static boolean allDefinite( float[] data ) {
        if ( data != null ) {
            int n = data.length;
            for ( int i = 0; i < n; i++ ) {
                if ( Float.isNaN( data[ i ] ) ) {
                    return false;
                }
            }
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Helper class for decoding TFCat geometries.
     * You can feed it TFCat shapes, and it accumulates a list of
     * coordinate values suitable for feeding to an Area.Type.POLYGON-type
     * Area constructor.
     *
     * <p>Note however that it currently does not correctly handle TFCat
     * (Multi)Polygon geometries that contain holes.
     */
    private static class TfcatPointList {
        final DoubleList dlist_;

        TfcatPointList() {
            dlist_ = new DoubleList();
        }

        /**
         * Adds a single position geometry to the coordinate list.
         *
         * @param  position  position to accumuate
         */
        void addPosition( Position position ) {
            dlist_.add( position.getTime() );
            dlist_.add( position.getSpectral() );
        }

        /**
         * Adds an array of positions to the coordinate list as a line.
         * They are made to look like a line by turning them into
         * a zero-width polygon.
         *
         * @param  positions   position array
         */
        void addLine( Position[] positions ) {
            int np = positions.length;
            for ( int ip = 0; ip < np; ip++ ) {
                addPosition( positions[ ip ] );
            }
            for ( int ip = np - 1; ip >= 0; ip-- ) {
                addPosition( positions[ ip ] );
            }
        }

        /**
         * Adds an array of linear rings representing a polygon
         * to the coordinate list.  According to TFCat, the first one
         * in the list is an actual polygon, and subsequent ones represent
         * holes in the outer one.
         * Since the AreaType coordinate array doesn't currently have the
         * capability to represent holes, at present this just adds
         * the holes alongside the outer one, which is clearly wrong.
         *
         * @param  lrings  list of one or more rings which together represent
         *                 a single, possibly holey, polygon
         */
        void addPolygon( LinearRing[] lrings ) {
            if ( lrings.length > 0 ) {
                for ( Position pos : lrings[ 0 ].getDistinctPositions() ) {
                    addPosition( pos );
                }
                addBreak();
            }
        }
 
        /**
         * Adds an inter-polygon break in the coordinate list.
         */
        void addBreak() {
            dlist_.add( Double.NaN );
            dlist_.add( Double.NaN );
        }
    }
}
