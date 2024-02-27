package uk.ac.starlink.vo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.function.Predicate;
import uk.ac.starlink.util.Bi;

/**
 * Describes a documented language feature that can be used
 * in some variant of the ADQL language.
 *
 * <p>A number of static methods are provided that return lists of
 * features provided by a particular ADQL version either absolutely
 * or in the context of a list of language feature declarations.
 * This class contains knowledge about what standard mandatory and
 * optional features are declared by known versions of the ADQL standard.
 *
 * <p>Note that ADQL 2.0 does not define a proper type system,
 * so in the case of ADQL 2.0 the types declared here are not strictly
 * based in the standard, but a casual reading will give the intent
 * of what was written in the ADQL 2.0 standard.
 * 
 * @author   Mark Taylor
 * @since    23 Feb 2024
 */
public class AdqlFeature {

    private final String name_;
    private final String description_;

    private static final Arg X = new Arg( "x", Type.DOUBLE );
    private static final Arg Y = new Arg( "y", Type.DOUBLE );
    private static final Arg N = new Arg( "n", Type.INTEGER );
    private static final Arg GEOM = new Arg( "geom", Type.GEOM );
    private static final Arg POINT = new Arg( "point", Type.POINT );
    private static final Arg COOSYS = new Arg( "coosys", Type.STRING );
    private static final Arg VARARGS = new Arg( "...", null );
    private static final String PI = "\u03c0";

    private static final Ivoid UDF_FTYPE =
        TapCapability.createTapRegExtIvoid( "#features-udf" );
    private static final Ivoid[] ADQLGEO_FTYPES = new Ivoid[] {
        TapCapability.createTapRegExtIvoid( "#features-adqlgeo" ),
        // error from early ADQL2.1 draft
        TapCapability.createTapRegExtIvoid( "#features-adql-geo" ),
    };
    private static final Ivoid STRING_FTYPE;
    private static final Ivoid CONDITIONAL_FTYPE;
    private static final Ivoid CTA_FTYPE;
    private static final Ivoid SETS_FTYPE;
    private static final Ivoid TYPE_FTYPE;
    private static final Ivoid UNIT_FTYPE;
    private static final Ivoid OFFSET_FTYPE;
    private static final Ivoid[] ADQL21MISC_FTYPES = new Ivoid[] {
        STRING_FTYPE =
            TapCapability.createTapRegExtIvoid( "#features-adql-string" ),
        CONDITIONAL_FTYPE =
            TapCapability.createTapRegExtIvoid( "#features-adql-conditional" ),
        CTA_FTYPE =
            TapCapability.createTapRegExtIvoid( "#features-adql-common-table" ),
        SETS_FTYPE =
            TapCapability.createTapRegExtIvoid( "#features-adql-sets" ),
        TYPE_FTYPE =
            TapCapability.createTapRegExtIvoid( "#features-adql-type" ),
        UNIT_FTYPE =
            TapCapability.createTapRegExtIvoid( "#features-adql-unit" ),
        OFFSET_FTYPE =
            TapCapability.createTapRegExtIvoid( "#features-adql-offset" ),
    };

    /** Includes feature types for Ivoids representing UDFs. */
    public static final Predicate<Ivoid> UDF_FILTER =
        ftype -> UDF_FTYPE.equals( ftype );

    /** Includes feature types for Ivoids representing Geometry functions. */
    public static final Predicate<Ivoid> ADQLGEO_FILTER =
        ftype -> Arrays.asList( ADQLGEO_FTYPES ).contains( ftype );

    /** Includes feature types for Ivoids representing optional features. */
    public static final Predicate<Ivoid> ADQL21MISC_FILTER =
        ftype -> Arrays.asList( ADQL21MISC_FTYPES ).contains( ftype );

    /** Includes feature types for Ivoids representing non-standard features. */
    public static final Predicate<Ivoid> NONSTD_FILTER =
        createExcludeFilter( ADQLGEO_FTYPES, ADQL21MISC_FTYPES,
                             new Ivoid[] { UDF_FTYPE } );

    private static final Function[] MATHS_FUNCS = createMathsFunctions();
    private static final Function[] TRIG_FUNCS = createTrigFunctions();
    private static final Function[] GEOM_FUNCS_20 =
        createGeomFunctions( AdqlVersion.V20 );
    private static final Function[] GEOM_FUNCS_21 =
        createGeomFunctions( AdqlVersion.V21 );
    private static final Map<FeatKey,Function> OPT_FUNCS_21 =
        createOptionalFunctions();
    private static final Map<FeatKey,AdqlFeature> OPT_FEATS_21 =
        createOptionalFeatures();

    /**
     * Constructor.
     *
     * @param   name  feature name
     * @param   description  plain text description directed at users
     */
    protected AdqlFeature( String name, String description ) {
        name_ = name;
        description_ = description;
    }

    /**
     * Returns the name of this feature.
     *
     * @return   function name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns a plain text description of this feature.
     *
     * @return  description
     */
    public String getDescription() {
        return description_;
    }

    /**
     * Returns an array of the maths functions defined by ADQL.
     * This is the same for ADQL 2.0 and 2.1, and consists of the
     * contents of Table 1 in 
     * <a href="https://www.ivoa.net/documents/ADQL/20231215/REC-ADQL-2.1.html#tth_sEc2.3"
     *    >Section 2.3 of ADQL 2.1</a>.
     *
     * @return  maths functions
     */
    public static Function[] getMathsFunctions() {
        return MATHS_FUNCS.clone();
    }

    /**
     * Returns an array of the trigonometric functions defined by ADQL.
     * This is the same for ADQL 2.0 and 2.1 and consists of the
     * contents of Table 2 in
     * <a href="https://www.ivoa.net/documents/ADQL/20231215/REC-ADQL-2.1.html#tth_sEc2.3"
     *    >Section 2.3 of ADQL 2.1</a>.
     *
     * @return  trig functions
     */
    public static Function[] getTrigFunctions() {
        return TRIG_FUNCS.clone();
    }

    /**
     * Returns an array of the standard ADQL 2.0 or 2.1 geometry functions
     * declared by a given TapCapability object.
     * These are defined in
     * <a href="https://www.ivoa.net/documents/ADQL/20231215/REC-ADQL-2.1.html#tth_sEc4.2"
     *    >Section 4.2 of ADQL 2.1</a>
     * and Section 2.4 of
     * <a href="https://www.ivoa.net/documents/cover/ADQL-20081030.html"
     *    >ADQL 2.0</a>.
     *
     * @param  version   ADQL version
     * @param  tcap     capabilities
     * @return  geometry functions
     */
    public static Function[] getGeomFunctions( AdqlVersion version,
                                               TapCapability tcap ) {
        Collection<Ivoid> ftypeSet =
            new HashSet<Ivoid>( Arrays.asList( ADQLGEO_FTYPES ) );
        Set<String> geomFuncNames =
            Arrays.stream( tcap == null ? new TapLanguage[ 0 ]
                                        : tcap.getLanguages() )
                  .flatMap( lang -> lang.getFeaturesMap().entrySet().stream() )
                  .filter( entry -> ftypeSet.contains( entry.getKey() ) )
                  .flatMap( entry -> Arrays.stream( entry.getValue() ) )
                  .map( feature -> feature.getForm() )
                  .collect( Collectors.toSet() );
        return Arrays.stream( is21( version ) ? GEOM_FUNCS_21 : GEOM_FUNCS_20 )
              .filter( f -> geomFuncNames.contains( f.getName().toUpperCase() ))
              .toArray( n -> new Function[ n ] );
    }

    /**
     * Returns an array of the ADQL 2.1 optional functions (but not other
     * optional features) declared by a given TapCapability object.
     * These are defined in 
     * <a href="https://www.ivoa.net/documents/ADQL/20231215/REC-ADQL-2.1.html#tth_sEc4"
     *    >Section 4 of ADQL 2.1</a>.
     * 
     * @param  tcap     capabilities
     * @return  optional functions
     */
    public static Function[] getOptionalFunctions( TapCapability tcap ) {
        Map<FeatKey,Function> funcMap = new LinkedHashMap<>( OPT_FUNCS_21 );
        funcMap.keySet().retainAll( getFeatureKeys( tcap ) );
        return funcMap.values().toArray( new Function[ 0 ] );
    }

    /**
     * Returns an array of the ADQL 2.1 optional features (excluding functions)
     * declared by a given TapCapability object.
     * These are defined in
     * <a href="https://www.ivoa.net/documents/ADQL/20231215/REC-ADQL-2.1.html#tth_sEc4"
     *    >Section 4 of ADQL 2.1</a>.
     * 
     * @param  tcap     capabilities
     * @return  optional features
     */
    public static AdqlFeature[] getOptionalFeatures( TapCapability tcap ) {
        Map<FeatKey,AdqlFeature> featMap = new LinkedHashMap<>( OPT_FEATS_21 );
        featMap.keySet().retainAll( getFeatureKeys( tcap ) );
        return featMap.values().toArray( new AdqlFeature[ 0 ] );
    }

    /**
     * Returns a collection of feature keys from a given TapCapability object.
     *
     * @param  tcap  capability, may be null
     * @return  all declared feature keys 
     */
    private static Set<FeatKey> getFeatureKeys( TapCapability tcap ) {
        return Arrays
              .stream( tcap == null ? new TapLanguage[ 0 ]
                                    : tcap.getLanguages() )
              .flatMap( lang -> lang.getFeaturesMap().entrySet().stream() )
              .flatMap( entry -> Arrays
                                .stream( entry.getValue() )
                                .map( feat -> new FeatKey( entry.getKey(),
                                                           feat.getForm() ) ) )
              .collect( Collectors.toSet() );
    }

    /**
     * Utility function to create a function with a numerical result
     * and a single argument.
     *
     * @param  name  function name
     * @param  description  function description
     * @param  arg   sole argument
     * @return  new function
     */
    private static Function numFunc1( String name, String description,
                                      Arg arg ) {
        return new Function( name, description,
                             new Arg[] { arg }, Type.DOUBLE );
    }

    /**
     * Utility function to create a function with a numerical result
     * and two arguments.
     *
     * @param  name  function name
     * @param  description  function description  
     * @param  arg1  first argument
     * @param  arg2  second argument
     * @return  new function
     */
    private static Function numFunc2( String name, String description,
                                      Arg arg1, Arg arg2 ) {
        return new Function( name, description,
                             new Arg[] { arg1, arg2 }, Type.DOUBLE );
    }

    /**
     * Utility function to create a numeric argument.
     *
     * @param   name  argument name
     * @return   new argument
     */
    private static Arg numArg( String name ) {
        return new Arg( name, Type.DOUBLE );
    }

    /**
     * Utility function to create an ADQL 2.0/2.1 function with a
     * geometry output and, in the case of ADQL 2.1, a coordsys first argument.
     *
     * @param   name  function name
     * @param   description   function description
     * @param   args   argument list (excluding possible coordsys)
     * @param   returnType   return type of function
     * @param   version   ADQL version
     * @return   new function
     */
    private static Function geomFunc( String name, String description,
                                      Arg[] args, Type returnType,
                                      AdqlVersion version ) {
        final Arg[] allArgs;
        final String fullDescription;
        if ( is21( version ) ) {
            allArgs = args;
            fullDescription = description;
        }
        else {
            List<Arg> argList = new ArrayList<>();
            argList.add( COOSYS );
            argList.addAll( Arrays.asList( args ) );
            fullDescription = new StringBuffer()
               .append( description )
               .append( " The initial " )
               .append( COOSYS )
               .append( " argument is supposed to give the name of " )
               .append( "a coordinate system, e.g. 'ICRS'. " )
               .append( "Services often ignore this parameter " )
               .append( "(which is removed in later versions of ADQL)" )
               .append( "; the empty string '' can often be used." )
               .toString();
            allArgs = argList.toArray( new Arg[ 0 ] );
        }
        return new Function( name, fullDescription, allArgs, returnType );
    }

    /**
     * Indicates whether a version is ADQL2.1-like.
     *
     * @param  version  version
     * @return  true if version should be treated more like ADQL 2.1 than
     *               ADQL 2.0
     */
    private static boolean is21( AdqlVersion version ) {
        return ! version.equals( AdqlVersion.V20 );
    }

    /**
     * Return a list of all the Maths functions defined by ADQL.
     *
     * @return  function array
     */
    private static final Function[] createMathsFunctions() {

        /* ADQL 2.1 sec 2.3, Table 1. */
        return new Function[] {
            numFunc1( "abs", "Returns the absolute value of " + X + ".", X ),
            numFunc1( "ceiling", 
                      "Returns the smallest integer that is not less than " +
                      X + ".", X ),
            numFunc1( "degrees",
                      "Converts the angle " + X + " from radians to degrees.",
                      X ),
            numFunc1( "exp", "Returns the exponential of " + X + ".", X ),
            numFunc1( "floor",
                      "Returns the largest integer that is not greater than " +
                      X + ".", X ),
            numFunc1( "log",
                      "Returns the natural logarithm (base e) of " + X + ". " +
                      "The value of " + X + " must be greater than zero",
                      X ),
            numFunc1( "log10",
                      "Returns the base 10 logarithm of " + X + ". " +
                      "The value of " + X + " must be greater than zero",
                      X ),
            numFunc2( "mod",
                      "Returns the remainder r of " + X + "/" + Y +
                      " as a floating point value, where: " +
                      "r has the same sign as " + X + "; " +
                      "-r is less than -" + Y + "; " +
                      X + "=n*" + Y + "+r for a given integer n.",
                      X, Y ),
            new Function( "pi", "The numeric constant " + PI + ".",
                          new Arg[ 0 ], Type.DOUBLE ),
            numFunc2( "power",
                      "Returns the value of " + X +
                      " raised to the power of " + Y + ".",
                      X, Y ),
            numFunc1( "radians",
                      "Converts the angle " + X + " from degrees to radians.",
                      X ),
            numFunc1( "sqrt",
                      "Returns the positive square root of " + X + ".",
                      X ),
            numFunc1( "rand",
                      "Returns a random value between 0.0 and 1.0. " +
                      "The optional argument " + X + ", originally intended " +
                      "to provide a random seed, has undefined semantics. " +
                      "Query writers are advised to omit this argument.",
                      X ),
            numFunc2( "round",
                      "Rounds " + X + " to " + N + " decimal places. " +
                      "The integer " + N + " is optional and defaults to 0 " +
                      "if not specified. " +
                      "A negative value of " + N + " will round to the left " +
                      "of the decimal point.",
                      X, N ),
            numFunc2( "truncate",
                      "Truncates " + X + " to " + N + " decimal places. " +
                      "The integer " + N + " is optional and defaults to 0 " +
                      "if not specified.",
                      X, N ),
        };
    }

    /**
     * Returns a list of all the Trig functions defined by ADQL.
     *
     * @return  function array
     */
    private static final Function[] createTrigFunctions() {

        /* ADQL 2.1 sec 2.3, Table 2. */
        return new Function[] {
            numFunc1( "acos",
                      "Returns the arc cosine of " + X + ", in the range of " +
                      "0 through " + PI + " radians. " +
                      "The absolute value of " + X + " must be less than " +
                      "or equal to 1.0.",
                      X ),
            numFunc1( "asin",
                      "Returns the arc sine of " + X + ", in the range of " +
                      "-" + PI + "/2 through " + PI + "/2 radians. " +
                      "The absolute value of " + X + " must be less than " +
                      "or equal to 1.0.",
                      X ),
            numFunc1( "atan",
                      "Returns the arc tangent of " + X + ", in the range of " +
                      "-" + PI + "/2 through " + PI + "/2 radians.",
                      X ),
            numFunc2( "atan2",
                      "Converts rectangular coordinates " + X + ", " + Y +
                      " to polar angle. " +
                      "It computes the arc tangent of " + Y + "/" + X +
                      " in the range of " +
                      "-" + PI + "/2 through " + PI + "/2 radians.",
                      Y, X ),
            numFunc1( "cos",
                      "Returns the cosine of the angle " + X + " in radians, " +
                      "in the range of -1.0 through 1.0.",
                      X ),
            numFunc1( "sin",
                      "Returns the sine of the angle " + X + " in radians, " +
                      "in the range of -1.0 through 1.0.",
                      X ),
            numFunc1( "tan",
                      "Returns the tangent of the angle " + X + " in radians.",
                      X ),
        };
    }

    /**
     * Returns a list of all the geometry functions defined by a given
     * version of ADQL.
     *
     * @param  version  ADQL version
     * @return  list of functions
     */
    private static Function[] createGeomFunctions( AdqlVersion version ) {
        boolean is21 = is21( version );
        List<Function> list = new ArrayList<>();

        /* AREA. */
        list.add( numFunc1( "AREA",
                            "Computes the area, in square degrees, " +
                            "of a given geometry.",
                            GEOM ) );

        /* BOX. */
        String boxDescrip1 = "Defines a box on the sky, centered at";
        String boxDescrip2 = String.join( "\n",
            "and with arms extending, parallel to the coordinate axes",
            "at the center position, for half the respective sizes",
            "on either side.",
            "Angles are in degrees."
        );
        if ( is21 ) {
            boxDescrip2 += "BOX is a special case of POLYGON " +
                           "defined purely for convenience. " +
                           "It is deprecated and may be removed " +
                           "in future versions of ADQL.";
        }
        list.add( geomFunc( "BOX",
                            boxDescrip1 + " (clon, clat) " + boxDescrip2,
                            new Arg[] { numArg( "clon" ), numArg( "clat" ),
                                        numArg( "dlon" ), numArg( "dlat" ), },
                            Type.POLYGON,
                            version ) );
        if ( is21 ) {
            list.add( geomFunc( "BOX",
                                boxDescrip1 + " center " + boxDescrip2,
                                new Arg[] {
                                    new Arg( "center", Type.POINT ),
                                    numArg( "dlon" ), numArg( "dlat" ),
                                },
                                Type.POLYGON,
                                version ) );
 
        }

        /* CENTROID. */
        list.add( new Function( "CENTROID",
                                "Computes the centroid of a given " +
                                "geometry and returns a POINT.",
                                new Arg[] { new Arg( "geom", Type.GEOM ) },
                                Type.POINT ) );

        /* CIRCLE. */
        String circleDescrip =
            "Defines a circular region on the sky (a cone in space). " +
            "Arguments are in degrees.";
        list.add( geomFunc( "CIRCLE", circleDescrip,
                            new Arg[] { numArg( "clon" ), numArg( "clat" ),
                                        numArg( "radius" ), },
                            Type.CIRCLE, version ) );
        if ( is21 ) {
            list.add( geomFunc( "CIRCLE", circleDescrip,
                                new Arg[] { POINT, numArg( "radius" ) },
                                Type.CIRCLE, version ) );
        }

        /* CONTAINS. */
        String containsDescrip = String.join( "\n", new String[] {
            "Determines whether a geometry is wholly contained within another.",
            "This is most commonly used to express a point-in-shape condition.",
            "Returns the integer value 1 if the first argument is in,",
            "or on the boundary of, the second argument,",
            "and the integer value 0 if it is not.",
            "When used as a predicate in the WHERE clause of a query,",
            "the returned value must be compared to the integer values 1 or 0,",
            "e.g. \"WHERE 1=CONTAINS(POINT(25,-19), CIRCLE(25.4,-20,10)\".",
        } );
        list.add( new Function( "CONTAINS", containsDescrip,
                                new Arg[] { new Arg( "inner", Type.GEOM ),
                                            new Arg( "outer", Type.GEOM ) },
                                Type.INTEGER ) );

        /* COORD1, COORD2. */
        list.add( numFunc1( "COORD1",
                            "Extracts the first coordinate value in degrees " +
                            "of a given POINT. " +
                            "For example COORD1(POINT(25.0,-19.5)) " +
                            "would return 25.",
                            POINT ) );
        list.add( numFunc1( "COORD2",
                            "Extracts the second coordinate value in degrees "+
                            "of a given POINT. " +
                            "For example COORD2(POINT(25.0,-19.5)) " +
                            "would return -19.5.",
                            POINT ) );

        /* COORDSYS. */
        String coordsysDescrip =
            "Extracts the coordinate system name from a given geometry.";
        if ( is21 ) {
            coordsysDescrip +=
                " This function doesn't make much sense at ADQL 2.1 " +
                "and is deprecated; it may be removed in future ADQL versions.";
        }
        list.add( new Function( "COORDSYS", coordsysDescrip,
                                new Arg[] { GEOM }, Type.STRING ) );

        /* DISTANCE. */
        String distanceDescrip =
            "Computes the arc length along a great circle between two points " +
            "and returns a numeric value in degrees.";
        list.add( new Function( "DISTANCE", distanceDescrip,
                                new Arg[] {
                                    new Arg( "point1", Type.POINT ),
                                    new Arg( "point2", Type.POINT ),
                                }, Type.DOUBLE ) );
        if ( is21 ) {
            list.add( new Function( "DISTANCE",
                                    distanceDescrip +
                                    " All arguments are in degrees.",
                                    new Arg[] {
                                        numArg( "lon1" ), numArg( "lat1" ),
                                        numArg( "lon2" ), numArg( "lat2" ),
                                    }, Type.DOUBLE ) );
        }

        /* INTERSECTS. */
        String intersectsDescrip = String.join( "\n", new String[] {
            "Determines whether two geometry values overlap.",
            "This is most commonly used to express a \"shape-vs-shape\"",
            "intersection test.",
            "Returns the integer value 1 if the shapes intersect,",
            "and the integer value 0 if it is not.",
            "When used as a predicate in the WHERE clause of a query,",
            "the returned value must be compared to the integer values 1 or 0,",
            "for example \"WHERE 1=INTERSECTS(...)\".",
            // There is some business about using a POINT as one argument
            // which I can't be bothered to document here.
        } );
        list.add( new Function( "INTERSECTS", intersectsDescrip,
                                new Arg[] { new Arg( "geom1", Type.GEOM ),
                                            new Arg( "geom2", Type.GEOM ) },
                                Type.INTEGER ) );

        /* POINT. */
        list.add( geomFunc( "POINT",
                            "Defines a single location on the sky. " +
                            "The arguments are in degrees.",
                            new Arg[] { numArg( "lon" ), numArg( "lat" ), },
                            Type.POINT, version ) );

        /* POLYGON. */
        String polygonDescrip = String.join( "\n", new String[] {
            "Defines a region on the sky with boundaries denoted by",
            "great circles passing through specified coordinates.",
            "At least three vertices must be specified,",
            "and the last vertex is implicitly connected to the first vertex.",
        } );
        list.add( geomFunc( "POLYGON",
                            polygonDescrip + " All arguments are in degrees.",
                            new Arg[] {
                                numArg( "lon1" ), numArg( "lat1" ),
                                numArg( "lon2" ), numArg( "lat2" ),
                                numArg( "lon3" ), numArg( "lat3" ),
                                VARARGS,
                            },
                            Type.POLYGON, version ) );
        if ( is21 ) {
            list.add( geomFunc( "POLYGON", polygonDescrip,
                                new Arg[] {
                                    new Arg( "point1", Type.POINT ),
                                    new Arg( "point2", Type.POINT ),
                                    new Arg( "point3", Type.POINT ),
                                    VARARGS,
                                },
                                Type.POLYGON, version ) );
        }

        /* REGION. */
        String regionDescrip = String.join( "\n", new String[] {
            "Provides a way of expressing a complex region represented by",
            "a single string literal.",
            "The argument must be a string literal not a string expression",
            "or column reference.",
            "The syntax is service specific;",
            "it may correspond to the semi-standard STC/S notation.",
        } );
        list.add( new Function( "REGION", regionDescrip,
                                new Arg[] { new Arg( "text", Type.STRING ) },
                                Type.REGION ) );

        /* Return list. */
        return list.toArray( new Function[ 0 ] );
    }

    /**
     * Creates a map of the optional functions defined by the ADQL 2.1 standard,
     * keyed by a type combining feature IVOID and feature FORM.
     *
     * @return  map containing optional functions
     */
    private static Map<FeatKey,Function> createOptionalFunctions(){
        Map<FeatKey,Function> map = new LinkedHashMap<>();

        /* ADQL 2.1 Section 4.4. */
        for ( String transform : new String[] { "LOWER", "UPPER" } ) {
            map.put( new FeatKey( STRING_FTYPE, transform ),
                     new Function( transform,
                                   "Maps the input string to " +
                                   transform.toLowerCase() + " case " +
                                   "in accordance with the rules of " +
                                   "the database's locale.",
                                   new Arg[] { new Arg( "text", Type.STRING ) },
                                   Type.STRING ) );
        }

        /* ADQL 2.1 Section 4.8. */
        map.put( new FeatKey( CONDITIONAL_FTYPE, "COALESCE" ),
                 new Function( "COALESCE",
                               "Returns the first of its arguments that " +
                               "is not NULL. NULL is returned only if " +
                               "all arguments are NULL. " +
                               "All arguments must be of the same type.",
                               new Arg[] { new Arg( "arg", Type.ANY ), VARARGS},
                               Type.ANY ) );

        /* ADQL 2.1 Section 4.9. */
        String inUnitDescription = String.join( "\n", new String[] {
            "Returns the value of the first argument transformed into the unit",
            "defined by the second argument.",
            "The first argument must be a numeric expression;",
            "if it is a column name, the VOUnits for this column ought to",
            "be found in the metadata attached to this column.",
            "The second argument must be a string literal giving a unit",
            "definition in valid VOUnit syntax.",
            "The system MUST report an error if the second argument",
            "is not a valid unit description, or if the system is not able",
            "to convert the value into the requested unit.",
        } );
        map.put( new FeatKey( UNIT_FTYPE, "IN_UNIT" ),
                 numFunc2( "IN_UNIT", inUnitDescription,
                           numArg( "value" ),
                           new Arg( "unit", Type.STRING ) ) );
        return Collections.unmodifiableMap( map );
    }

    /**
     * Creates a map of the optional features defined by the ADQL 2.1 standard,
     * keyed by a type combining feature IVOID and feature FORM.
     *
     * @return  map containing optional features
     */
    private static Map<FeatKey,AdqlFeature> createOptionalFeatures(){
        Map<FeatKey,AdqlFeature> map = new LinkedHashMap<>();

        /* ADQL 2.1 Section 4.4. */
        map.put( new FeatKey( STRING_FTYPE, "ILIKE" ),
                 new AdqlFeature( "ILIKE",
                                  "String comparison operator that operates " +
                                  "as the standard LIKE operator, " +
                                  "but guaranteed case-insensitive." ) );

        /* ADQL 2.1 Section 4.5. */
        String withDescrip = String.join( "\n", new String[] {
            "Common Table Expressions are supported.",
            "You can write expressions like",
            "\"WITH subtable AS (SELECT ...) SELECT ... FROM subtable ...\".",
            "Recursive CTEs are not supported.",
            "They can be defined only in the main query,",
            "they are not allowed in sub-queries.",
        } );
        map.put( new FeatKey( CTA_FTYPE, "WITH" ),
                 new AdqlFeature( "WITH", withDescrip ) );

        /* ADQL 2.1 Section 4.6. */
        String unionDescrip = String.join( "\n", new String[] {
            "Operator that combines two SELECT clauses",
            "giving the union of the two results.",
            "The joined queries must have the same number of columns",
            "with the same data types.",
            "Duplicated rows are removed unless the form UNION ALL is used.",
        } );
        map.put( new FeatKey( SETS_FTYPE, "UNION" ),
                 new AdqlFeature( "UNION", unionDescrip ) );
        String intersectDescrip = String.join( "\n", new String[] {
            "Operator that combines two SELECT clauses",
            "giving the intersection of the two results.",
            "The joined queries must have the same number of columns",
            "with the same data types.",
            "Duplicated rows are removed unless the form INTERSECT ALL is used."
        } );
        map.put( new FeatKey( SETS_FTYPE, "INTERSECT" ),
                 new AdqlFeature( "INTERSECT", intersectDescrip ) );
        String exceptDescrip = String.join( "\n", new String[] {
            "Operator that combines two SELECT clauses",
            "giving those that appear in the first operand but not the second.",
            "The joined queries must have the same number of columns",
            "with the same data types.",
        } );
        map.put( new FeatKey( SETS_FTYPE, "EXCEPT" ),
                 new AdqlFeature( "EXCEPT", exceptDescrip ) );

        /* ADQL 2.1 Section 4.6. */
        String castDescrip = String.join( "\n", new String[] {
            "Returns the value of the first argument converted into",
            "the datatype specified by the second argument.",
            "The syntax is CAST(value AS target-type).",
            "At least the following types are supported:",
            "INTEGER, SMALLINT, BIGINT, REAL, DOUBLE PRECISION,",
            "CHAR or CHAR(n), VARCHAR or VARCHAR(n), TIMESTAMP.",
            "Examples are",
            "\"CAST(value AS INTEGER)\",",
            "\"CAST('2021-01-14T11:25:00' AS TIMESTAMP)\".",
        } );
        map.put( new FeatKey( TYPE_FTYPE, "CAST" ),
                 new AdqlFeature( "CAST", castDescrip ) );

        /* ADQL 2.1 Section 4.10. */
        String offsetDescrip = String.join( "\n", new String[] {
            "Clause that may be used to remove a specified number of rows",
            "from the beginning of the result.",
            "The syntax is \"SELECT ... OFFSET n\",",
            "where n is the number of rows to omit.",
            "The OFFSET clause comes right at the end of the SELECT statement,",
            "after any ORDER BY clause.",
            "If both OFFSET and TOP clauses are included,",
            "OFFSET is applied first.",
        } );
        map.put( new FeatKey( OFFSET_FTYPE, "OFFSET" ),
                 new AdqlFeature( "OFFSET", offsetDescrip ) );
        return Collections.unmodifiableMap( map );
    }

    /**
     * Utility method to create a filter that excludes strings in a
     * number of given lists.
     *
     * @param  excludes   one or more arrays of strings none of which
     *                    a target ivoid should equal to pass the test
     * @return   predicate indicating non-inclusion in supplied arrays
     */
    private static Predicate<Ivoid> createExcludeFilter( Ivoid[]... excludes ) {
        Collection<Ivoid> excludeSet = new HashSet<>();
        for ( Ivoid[] items : excludes ) {
            excludeSet.addAll( Arrays.asList( items ) );
        }
        return f -> ! excludeSet.contains( f );
    }

    /**
     * AdqlFeature subclass which represents a function.
     * This includes declared arguments and a return type.
     */
    public static class Function extends AdqlFeature {

        private final Arg[] args_;
        private final Type returnType_;

        /**
         * Constructor.
         *
         * @param  name  function name
         * @param  args  function declared arguments
         * @param  returnType  function declared return type
         * @param  description   plain text description directed at users
         */
        protected Function( String name, String description,
                            Arg[] args, Type returnType ) {
            super( name, description );
            args_ = args;
            returnType_ = returnType;
        }

        /**
         * Returns the declared arguments of this function.
         *
         * @return  arguments
         */
        public Arg[] getArgs() {
            return args_;
        }

        /**
         * Returns the declared return type of this function.
         *
         * @return  return type
         */
        public Type getReturnType() {
            return returnType_;
        }
    }

    /**
     * Represents a declared argument of a function.
     * The name can be abused to contain some other string that
     * represents how something in the argument list should be represented.
     */
    public static class Arg {

        private final String name_;
        private final Type type_;

        /**
         * Constructor.
         *
         * @param  name  argument name
         * @param  type  argument type
         */
        Arg( String name, Type type ) {
            name_ = name;
            type_ = type;
        }

        /**
         * Returns the name of this argument.
         *
         * @return  argument name or representation
         */
        public String getName() {
            return name_;
        }

        /**
         * Returns the type of this argument.
         *
         */
        public Type getType() {
            return type_;
        }

        @Override
        public String toString() {
            return name_;
        }
    }

    /**
     * Datatype of a function argument or return value.
     */
    public enum Type {
        DOUBLE, INTEGER, STRING, POINT, CIRCLE, POLYGON, REGION, GEOM, ANY;
    }

    /**
     * Combines the type and name for a TapCapability declared language feature.
     * Suitable for use as a Map key.
     */
    private static class FeatKey extends Bi<Ivoid,String> {

        /**
         * Constructor.
         *
         * @param  type  feature type, as declared in type attribute
         * @param  name  feature name, as declared in FORM element
         */
        FeatKey( Ivoid type, String name ) {
            super( type, name.toUpperCase() );
        }
    }
}
