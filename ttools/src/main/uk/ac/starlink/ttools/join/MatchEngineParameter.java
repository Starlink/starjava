package uk.ac.starlink.ttools.join;

import java.util.logging.Logger;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.join.AnisotropicCartesianMatchEngine;
import uk.ac.starlink.table.join.CdsHealpixSkyPixellator;
import uk.ac.starlink.table.join.CombinedMatchEngine;
import uk.ac.starlink.table.join.CuboidCartesianMatchEngine;
import uk.ac.starlink.table.join.ErrorCartesianMatchEngine;
import uk.ac.starlink.table.join.ErrorSkyMatchEngine;
import uk.ac.starlink.table.join.EllipseCartesianMatchEngine;
import uk.ac.starlink.table.join.EllipseSkyMatchEngine;
import uk.ac.starlink.table.join.EqualsMatchEngine;
import uk.ac.starlink.table.join.FixedSkyMatchEngine;
import uk.ac.starlink.table.join.HtmSkyPixellator;
import uk.ac.starlink.table.join.IsotropicCartesianMatchEngine;
import uk.ac.starlink.table.join.MatchEngine;
import uk.ac.starlink.table.join.SphericalPolarMatchEngine;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.StringParameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.func.CoordsRadians;
import uk.ac.starlink.ttools.task.ExtraParameter;
import uk.ac.starlink.ttools.task.TableEnvironment;
import uk.ac.starlink.ttools.task.WordsParameter;
import uk.ac.starlink.util.Loader;

/**
 * Parameter for acquiring a {@link uk.ac.starlink.table.join.MatchEngine}.
 *
 * @author   Mark Taylor
 * @since    2 Sep 2005
 */
public class MatchEngineParameter extends Parameter<MatchEngine>
                                  implements ExtraParameter {

    private final WordsParameter<String> paramsParam_;
    private final WordsParameter<String> tuningParam_;
    private final StringParameter scoreParam_;

    private static final int MAX_CHARS = 78;
    private static final ValueInfo SCORE_INFO = 
        new DefaultValueInfo( "Score", Number.class,
                              "Closeness of match (0 is exact)" );
    private static final double ARC_SECOND = CoordsRadians.ARC_SECOND_RADIANS;

    /** Base name for tuple parameter. */
    private static final String TUPLE_NAME = "values";

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.join" );

    public MatchEngineParameter( String name ) {
        super( name, MatchEngine.class, true );

        paramsParam_ = WordsParameter.createStringWordsParameter( "params" );
        tuningParam_ = WordsParameter.createStringWordsParameter( "tuning" );

        setStringDefault( "sky" );
        setPreferExplicit( true );

        setUsage( "<matcher-name>" );
        setPrompt( "Name of matching algorithm" );
        setDescription( new String[] {
            "<p>Defines the nature of the matching that will be performed.",
            "Depending on the name supplied, this may be positional",
            "matching using celestial or Cartesian coordinates,",
            "exact matching on the value of a string column,",
            "or other things.",
            "A list and explanation of the available matching algorithms",
            "is given in <ref id='MatchEngine'/>.",
            "The value supplied for this parameter determines the meanings",
            "of the values required by the ",
            "<code>" + paramsParam_.getName() + "</code>,",
            "<code>values*</code> and",
            "<code>" + tuningParam_.getName() + "</code>",
            "parameter(s).",
            "</p>",
        } );

        paramsParam_.setPrompt( "Match parameters" );
        paramsParam_.setDescription( new String[] {
            "<p>Determines the parameters of this match.",
            "This is typically one or more tolerances such as error radii.",
            "It may contain zero or more values; the values that are",
            "required depend on the match type selected by the",
            "<code>" + getName() + "</code> parameter.",
            "If it contains multiple values, they must be separated by spaces;",
            "values which contain a space can be 'quoted' or \"quoted\".",
            "</p>",
        } );
        paramsParam_.setNullPermitted( true );
        paramsParam_.setUsage( "<match-params>" );

        tuningParam_.setPrompt( "Tuning parameters" );
        tuningParam_.setDescription( new String[] {
            "<p>Tuning values for the matching process, if appropriate.",
            "It may contain zero or more values; the values that are",
            "permitted depend on the match type selected by the",
            "<code>" + getName() + "</code> parameter.",
            "If it contains multiple values, they must be separated by spaces;",
            "values which contain a space can be 'quoted' or \"quoted\".",
            "If this optional parameter is not supplied, sensible defaults",
            "will be chosen.",
            "</p>",
        } );
        tuningParam_.setNullPermitted( true );
        tuningParam_.setUsage( "<tuning-params>" );

        scoreParam_ = new StringParameter( "scorecol" );
        scoreParam_.setUsage( "<col-name>" );
        scoreParam_.setPrompt( "Match score output column name" );
        scoreParam_.setDescription( new String[] {
            "<p>Gives the name of a column in the output table to contain",
            "the \"match score\" for each pairwise match.",
            "The meaning of this column is dependent on the chosen",
            "<code>" + getName() + "</code>,",
            "but it typically represents a distance of some kind between",
            "the two matching points.",
            "If a null value is chosen, no score column will be inserted",
            "in the output table.",
            "The default value of this parameter depends on",
            "<code>" + getName() + "</code>.",
            "</p>",
        } );
        scoreParam_.setNullPermitted( true );
        scoreParam_.setStringDefault( SCORE_INFO.getName() );
    }

    public String getExtraUsage( TableEnvironment env ) {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "   Available matchers, with associated parameters,"
                   + " include:\n" );
        try {
            for ( String name : getExampleValues() ) {
                StringBuffer line = new StringBuffer();
                MatchEngine engine = createEngine( name );
                line.append( "      " )
                    .append( getName() )
                    .append( '=' )
                    .append( name );
                String pad = line.toString().replaceAll( ".", " " );
                String vu = getValuesUsage( engine, line.length() );
                String pu = getConfigUsage( engine, paramsParam_,
                                            engine.getMatchParameters() );
                String tu = getConfigUsage( engine, tuningParam_,
                                            engine.getTuningParameters() );
                int leng = line.length();
                line.append( vu );
                leng += vu.length();
                if ( leng + pu.length() > MAX_CHARS ) {
                    line.append( '\n' )
                        .append( pad );
                    leng = pad.length();
                }
                line.append( pu );
                leng += pu.length();
                if ( leng + tu.length() > MAX_CHARS ) {
                    line.append( '\n' )
                        .append( pad );
                    leng = pad.length();
                }
                line.append( tu );
                leng += tu.length();
                line.append( '\n' );
                sbuf.append( line );
            }
        }
        catch ( UsageException e ) {
            sbuf.append( "\n      ???\n" );
        }
        return sbuf.toString();
    }

    /**
     * Returns the associated parameter which is used for specifying the
     * fixed value parameters for the engine supplied by this parameter.
     *
     * @return  params parameter
     */
    public Parameter<String[]> getMatchParametersParameter() {
        return paramsParam_;
    }

    /**
     * Returns the associated parameter which is used for specifying
     * optional tuning parameters for the engine supplied by this parameter.
     *
     * @return  tuning parameter
     */
    public Parameter<String[]> getTuningParametersParameter() {
        return tuningParam_;
    }

    /**
     * Returns the associated parameter which is used for specifying the
     * match score column metadata.  Do not interrogate this parameter
     * directly - use {@link #getScoreInfo}.
     *
     * @return  match score parameter
     */
    public Parameter<String> getScoreParameter() {
        return scoreParam_;
    }

    /**
     * Returns the match score metadata associated with this parameter.
     *
     * @param   env  execution environment
     * @return  match score metadata; may be null
     */
    public ValueInfo getScoreInfo( Environment env ) throws TaskException {
        String scoreVal = scoreParam_.stringValue( env );
        if ( scoreVal == null || scoreVal.trim().length() == 0 ) {
            return null;
        }
        else {
            ValueInfo baseInfo = objectValue( env ).getMatchScoreInfo();
            DefaultValueInfo info = baseInfo == null
                                  ? new DefaultValueInfo( SCORE_INFO )
                                  : new DefaultValueInfo( baseInfo );
            info.setName( scoreVal );
            return info;
        }
    }

    /**
     * Creates a new parameter for specifying value tuples for a table,
     * suitable for use with this one.
     *
     * <p>The supplied <code>numLabel</code> parameter distinguishes the
     * parameter name if there are several; it is usually "1", "2", ...
     * or "N" for a generic number or "" if there is only one.
     * The autogenerated documentation will be adjusted accordingly.
     *
     * @param   numLabel  identifier for the new parameter
     */
    public WordsParameter<String> createMatchTupleParameter( String numLabel ) {
        boolean isNumbered = numLabel != null && numLabel.length() > 0;
        WordsParameter<String> tupleParam =
            WordsParameter.createStringWordsParameter( TUPLE_NAME + numLabel );
        tupleParam.setUsage( "<expr-list>" );
        tupleParam.setPrompt( "Expressions for match values"
                            + ( isNumbered ? ( " from table " + numLabel )
                                           : "" ) );
        tupleParam.setDescription( new String[] {
            "<p>Defines the values from",
            ( isNumbered ? ( "table " + numLabel ) : "the input table" ),
            "which are used to determine whether a match has occurred.",
            "These will typically be coordinate values such as RA and Dec",
            "and perhaps some per-row error values as well, though exactly",
            "what values are required is determined by the kind of match",
            "as determined by <code>" + getName() + "</code>.",
            "Depending on the kind of match, the number and type of",
            "the values required will be different.",
            "Multiple values should be separated by whitespace;",
            "if whitespace occurs within a single value it must be",
            "'quoted' or \"quoted\".",
            "Elements of the expression list are commonly just column",
            "names, but may be algebraic expressions calculated from",
            "zero or more columns as explained in <ref id='jel'/>.",
            "</p>",
        } );
        return tupleParam;
    }

    /**
     * Configures a tuple parameter for use with a given MatchEngine.
     * Amongst other things, the required word count will be set, so that
     * its {@link WordsParameter#wordsValue} will
     * return an array of the correct size for the match engine.
     *
     * @param   tupleParam  tuple parameter to interrogate, probably generated
     *          earlier by {@link #createMatchTupleParameter}
     * @param   matcher   match engine which will be used 
     */
    public static void
            configureTupleParameter( WordsParameter<String> tupleParam,
                                     MatchEngine matcher ) {

        /* Work out the number label of the tuple parameter. */
        String tname = tupleParam.getName();
        int inum = tname.indexOf( TUPLE_NAME );
        String numLabel = inum >= 0
                        ? tname.substring( TUPLE_NAME.length() + inum )
                        : "";

        /* Find the number of elements required in the tuple. */
        ValueInfo[] tinfos = matcher.getTupleInfos();
        int nexpr = tinfos.length;

        /* Assemble a custom prompt string. */
        StringBuffer sbuf = new StringBuffer();
        if ( numLabel == null || numLabel.length() == 0 ) {
            sbuf.append( "Match value expressions" );
        }
        else {
            sbuf.append( "Table " )
                .append( numLabel )
                .append( " match value expressions" );
        }
        sbuf.append( " (" );
        for ( int i = 0; i < nexpr; i++ ) {
            if ( i > 0 ) {
                sbuf.append( ' ' );
            }
            sbuf.append( tinfos[ i ].getName().replaceAll( " ", "_" ) );
            String units = tinfos[ i ].getUnitString();
            if ( units != null && units.trim().length() > 0 ) {
                if ( units.equalsIgnoreCase( "degree" ) ||
                     units.equalsIgnoreCase( "degrees" ) ) {
                    units = "deg";
                }
                sbuf.append( '/' )
                    .append( units );
            }
        }
        sbuf.append( ")" );
        String prompt = sbuf.toString();

        /* Construct and configure a parameter for acquiring the tuple
         * expression values. */
        tupleParam.setRequiredWordCount( nexpr );
        tupleParam.setPrompt( prompt );
    }

    /**
     * Returns the value of this parameter as a MatchEngine.
     *
     * @param  env  execution environment
     * @return  match engine
     */
    public MatchEngine matchEngineValue( Environment env )
            throws TaskException {
        return objectValue( env );
    }

    public MatchEngine stringToObject( Environment env, String stringVal )
            throws TaskException {

        /* Get the unconfigured engine corresponding to this parameter's
         * string value. */
        MatchEngine engine = createEngine( stringVal );

        /* Configure score column parameter accordingly. */
        ValueInfo scoreInfo = engine.getMatchScoreInfo();
        scoreParam_.setStringDefault( scoreInfo == null ? null
                                                        : scoreInfo.getName() );

        /* Configure the engine's subparameters. */
        setConfigValues( env, engine.getMatchParameters(), paramsParam_,
                         true );
        setConfigValues( env, engine.getTuningParameters(), tuningParam_,
                         false );

        /* Warn if there is not a sensible metric. */
        if ( ( engine instanceof CombinedMatchEngine ) &&
             ! ( engine.getScoreScale() > 0 ) ) {
            logger_.warning( "Matcher " + stringVal + " can't be scaled"
                           + "; Best match won't make much sense" );
        }

        /* Return the configured object. */
        return engine;
    }

    /**
     * Configures a MatchEngine bye setting an array of DescribedValues
     * according to the value(s) of a suitable task parameter.
     *
     * @param  env  execution environment
     * @param  config   array of DescribedValues that provide a means to
     *                  configure the MatchEngine
     * @param  wordsParam  task parameter whose value is a list of words,
     *                     one for each of the <code>configs</code> elements
     * @param  required  true iff supplying values for <code>wordsParam</code>
     *                   is mandatory
     */
    private void setConfigValues( Environment env, DescribedValue[] configs,
                                  WordsParameter<String> wordsParam,
                                  boolean required )
            throws TaskException {

        /* If there are no parameters, we don't need to acquire them. */
        int nConfig = configs.length;
        if ( nConfig == 0 ) {
            wordsParam.setNullPermitted( true );
            wordsParam.setStringDefault( null );
        }

        /* Otherwise, enquire about the values to use via the relevant
         * parameter. */
        else {
            wordsParam.setNullPermitted( ! required );
            StringBuffer sbuf = new StringBuffer( wordsParam.getPrompt() )
                .append( " (" );
            for ( int i = 0; i < nConfig; i++ ) {
                if ( i > 0 ) {
                    sbuf.append( ' ' );
                }
                sbuf.append( getInfoUsage( configs[ i ].getInfo() ) );
            }
            sbuf.append( ')' );
            wordsParam.setPrompt( sbuf.toString() );
            wordsParam.setRequiredWordCount( nConfig );
            String[] words = wordsParam.wordsValue( env );
            if ( words != null ) {
                for ( int i = 0; i < nConfig; i++ ) {
                    String word = words[ i ];
                    try {
                        configs[ i ].setValueFromString( word );
                    }
                    catch ( RuntimeException e ) {
                        throw new UsageException( "Value " + words[ i ]
                                                + " not suitable for "
                                                + configs[ i ].getInfo(), e );
                    }
                }
            }
            for ( int i = 0; i < nConfig; i++ ) {
                logger_.info( configs[ i ].toString() );
            }
        }
    }

    /**
     * Returns a new, unconfigured match engine given a short naming string.
     * Names may be separated by a '+', in which case a combined engine
     * is returned.
     *
     * @param  name   label to select match engine type
     * @return  new match engine
     */
    public MatchEngine createEngine( String name ) throws UsageException {
        String[] names = name.trim().split( "\\+" );
        MatchEngine[] components = new MatchEngine[ names.length ];
        for ( int i = 0; i < names.length; i++ ) {
            MatchEngine component;
            String cName = names[ i ];
            if ( "sky".equalsIgnoreCase( cName ) ||
                 "healpix".equalsIgnoreCase( cName ) ) {
                component =
                    new FixedSkyMatchEngine( new CdsHealpixSkyPixellator(),
                                             ARC_SECOND );
            }
            else if ( "skyerr".equalsIgnoreCase( cName ) ) {
                component =
                    new ErrorSkyMatchEngine( new CdsHealpixSkyPixellator(),
                                             ARC_SECOND );
            }
            else if ( "skyellipse".equalsIgnoreCase( cName ) ) {
                component =
                    new EllipseSkyMatchEngine(
                        new CdsHealpixSkyPixellator(), ARC_SECOND );
            }
            else if ( "skyellipse-nocirc".equalsIgnoreCase( cName ) ) {
                EllipseSkyMatchEngine matcher =
                    new EllipseSkyMatchEngine(
                        new CdsHealpixSkyPixellator(), ARC_SECOND );
                matcher.setRecogniseCircles( false );
                component = matcher;
            }
            else if ( "sky3d".equalsIgnoreCase( cName ) ) {
                component = new SphericalPolarMatchEngine( 0. );
            }
            else if ( "exact".equalsIgnoreCase( cName ) ) {
                component = new EqualsMatchEngine();
            }
            else if ( "2d_ellipse".equalsIgnoreCase( cName ) ) {
                component = new EllipseCartesianMatchEngine( 1 );
            }
            else if ( "2d_ellipse-nocirc".equalsIgnoreCase( cName ) ) {
                EllipseCartesianMatchEngine matcher =
                    new EllipseCartesianMatchEngine( 1 );
                matcher.setRecogniseCircles( false );
                component = matcher;
            }
            else if ( cName.matches( "[0-9][dD]" ) ) {
                int ndim = Integer.parseInt( cName.substring( 0, 1 ) );
                component =
                    new IsotropicCartesianMatchEngine( ndim, 0.0, false );
            }
            else if ( cName.toLowerCase().matches( "[0-9]d_err" ) ) {
                int ndim = Integer.parseInt( cName.substring( 0, 1 ) );
                component = new ErrorCartesianMatchEngine( ndim, 1.0 );
            }
            else if ( cName.toLowerCase().matches( "[0-9]d_anisotropic" ) ) {
                int ndim = Integer.parseInt( cName.substring( 0, 1 ) );
                component =
                    new AnisotropicCartesianMatchEngine( new double[ ndim ] );
            }
            else if ( cName.toLowerCase().matches( "[0-9]d_cuboid" ) ) {
                int ndim = Integer.parseInt( cName.substring( 0, 1 ) );
                component =
                   new CuboidCartesianMatchEngine( new double[ ndim ] );
            }
            else if ( cName.equalsIgnoreCase( "htm" ) ) {
                component = new FixedSkyMatchEngine( new HtmSkyPixellator(),
                                                     ARC_SECOND );
            }
            else {
                component = Loader.getClassInstance( cName, MatchEngine.class );
                if ( component == null ) {
                    throw new UsageException( "Unknown matcher element: "
                                            + cName );
                }
            }
            components[ i ] = new HumanMatchEngine( component );
        }
        return components.length == 1
             ? components[ 0 ]
             : new CombinedMatchEngine( components );
    }

    /**
     * Returns a string giving the usage for the values parameter part
     * of the matching command line.
     *
     * @param  engine  match engine
     * @param  startLeng  running line length at which the values part
     *                    begins on the output
     * @return  values usage - possibly empty, not null
     */
    public String getValuesUsage( MatchEngine engine, int startLeng ) {
        StringBuffer sbuf = new StringBuffer();
        ValueInfo[] tupleInfos = engine.getTupleInfos();
        if ( tupleInfos.length > 0 ) {
            sbuf.append( " values*='" );
            int baseLeng = startLeng + sbuf.length();
            int leng = baseLeng;
            for ( int iv = 0; iv < tupleInfos.length; iv++ ) {
                StringBuffer vbuf = new StringBuffer();
                if ( iv > 0 ) {
                    vbuf.append( ' ' );
                }
                vbuf.append( '<' )
                    .append( getInfoUsage( tupleInfos[ iv ] ) )
                    .append( '>' );
                if ( leng + vbuf.length() > MAX_CHARS ) {
                    sbuf.append( '\n' );
                    for ( int is = 0; is < baseLeng - 1; is++ ) {
                        sbuf.append( ' ' );
                    }
                    leng = baseLeng;
                }
                sbuf.append( vbuf );
                leng += vbuf.length();
            }
            sbuf.append( '\'' );
        }
        return sbuf.toString();
    }

    /**
     * Returns a string giving the usage for a parameter with configuration
     * subparameters.
     * 
     * @param  engine  match engine
     * @param  wordsParam   parameter providing values for the subparameters
     * @param  configs   modifiable subparameters
     * @return  params usage - possibly empty, not null
     */
    public String getConfigUsage( MatchEngine engine,
                                  Parameter<String[]> wordsParam,
                                  DescribedValue[] configs ) {
        StringBuffer sbuf = new StringBuffer();
        if ( configs.length > 0 ) {
            sbuf.append( ' ' )
                .append( wordsParam.getName() )
                .append( '=' )
                .append( '\'' );
            for ( int i = 0; i < configs.length; i++ ) {
                if ( i > 0 ) {
                    sbuf.append( ' ' );
                }
                sbuf.append( '<' )
                    .append( getInfoUsage( configs[ i ].getInfo() ) )
                    .append( '>' );
            }
            sbuf.append( '\'' );
        }
        return sbuf.toString();
    }

    /**
     * Returns a usage fragment appropriate to specifying a value on the
     * command line in accordance with the metadata given in a
     * ValueInfo object.
     *
     * @param  info  value metadata specification
     * @return  usage fragment
     */
    public static String getInfoUsage( ValueInfo info ) {
        StringBuffer sbuf = new StringBuffer()
            .append( info.getName().toLowerCase().replaceAll( " ", "-" ) );
        String units = info.getUnitString();
        if ( units != null ) {
            sbuf.append( '/' )
                .append( units );
        }
        return sbuf.toString();
    }

    /**
     * Returns strings naming a set of example match engine parameter values.
     * These are used in the documentation.
     */
    public static String[] getExampleValues() {
        return new String[] {
            "sky", "sky3d", "skyerr", "skyellipse",
            "exact", "1d", "2d", "3d",
            "2d_anisotropic", "3d_anisotropic",
            "2d_cuboid", "3d_cuboid",
            "1d_err", "2d_err",
            "2d_ellipse",
            "sky+1d",
        };
    }
}
