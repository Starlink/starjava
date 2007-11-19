package uk.ac.starlink.ttools.join;

import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Logger;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.join.AnisotropicCartesianMatchEngine;
import uk.ac.starlink.table.join.CombinedMatchEngine;
import uk.ac.starlink.table.join.EqualsMatchEngine;
import uk.ac.starlink.table.join.HEALPixMatchEngine;
import uk.ac.starlink.table.join.HTMMatchEngine;
import uk.ac.starlink.table.join.IsotropicCartesianMatchEngine;
import uk.ac.starlink.table.join.MatchEngine;
import uk.ac.starlink.table.join.SphericalPolarMatchEngine;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.func.Coords;
import uk.ac.starlink.ttools.task.ExtraParameter;
import uk.ac.starlink.ttools.task.TableEnvironment;
import uk.ac.starlink.ttools.task.WordsParameter;

/**
 * Parameter for acquiring a {@link uk.ac.starlink.table.join.MatchEngine}.
 *
 * @author   Mark Taylor
 * @since    2 Sep 2005
 */
public class MatchEngineParameter extends Parameter implements ExtraParameter {

    private final WordsParameter paramsParam_;
    private MatchEngine matchEngine_;

    /** Base name for tuple parameter. */
    private static final String TUPLE_NAME = "values";

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.task" );

    public MatchEngineParameter( String name ) {
        super( name );

        paramsParam_ = new WordsParameter( "params" );

        setDefault( "sky" );
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
            "<code>" + paramsParam_.getName() + "</code> and",
            "<code>values*</code> parameter(s).",
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
    }

    public String getExtraUsage( TableEnvironment env ) {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "   Available matchers, with associated parameters,"
                   + " include:\n" );
        try {
            for ( Iterator it = Arrays.asList( getExampleValues() ).iterator();
                  it.hasNext(); ) {
                String name = (String) it.next();
                StringBuffer line = new StringBuffer();
                MatchEngine engine = createEngine( name );
                line.append( "      " )
                    .append( getName() )
                    .append( '=' )
                    .append( name );
                String pad = line.toString().replaceAll( ".", " " );
                String vu = getValuesUsage( engine );
                String pu = getParamsUsage( engine );
                line.append( vu );
                if ( line.length() + pu.length() > 78 ) {
                    line.append( '\n' )
                        .append( pad );
                }
                line.append( pu )
                    .append( '\n' );
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
    public Parameter getMatchParametersParameter() {
        return paramsParam_;
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
    public WordsParameter createMatchTupleParameter( String numLabel ) {
        boolean isNumbered = numLabel != null && numLabel.length() > 0;
        WordsParameter tupleParam = new WordsParameter( TUPLE_NAME + numLabel );
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
    public static void configureTupleParameter( WordsParameter tupleParam,
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
            sbuf.append( tinfos[ i ].getName() );
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
        checkGotValue( env );
        return matchEngine_;
    }

    public void setValueFromString( Environment env, String stringVal )
            throws TaskException {

        /* Get the unconfigured engine corresponding to this parameter's
         * string value. */
        String name = stringVal.toLowerCase();
        MatchEngine engine = createEngine( name );

        /* See about the match parameter constants that the resulting
         * match engine requires. */
        DescribedValue[] params = engine.getMatchParameters();
        int nParam = params.length;

        /* If there are no such parameters, we don't need to acquire them. */
        if ( nParam == 0 ) {
            paramsParam_.setNullPermitted( true );
            paramsParam_.setDefault( null );
        }

        /* If there are, enquire about the values to use via the 
         * params parameter. */
        else {
            paramsParam_.setNullPermitted( false );
            StringBuffer sbuf = new StringBuffer( "Match parameters (" );
            for ( int i = 0; i < nParam; i++ ) {
                if ( i > 0 ) {
                    sbuf.append( ' ' );
                }
                sbuf.append( getInfoUsage( params[ i ].getInfo() ) );
            }
            sbuf.append( ')' );
            paramsParam_.setPrompt( sbuf.toString() );
            paramsParam_.setRequiredWordCount( nParam );
            String[] words = paramsParam_.wordsValue( env );
            for ( int i = 0; i < nParam; i++ ) {
                String word = words[ i ];
                try {

                    /* Set the values in the match engine accordingly. */
                    params[ i ].setValueFromString( word );
                    logger_.info( params[ i ].toString() );
                }
                catch ( IllegalArgumentException e ) {
                    throw new UsageException( "Value " + words[ i ]
                                            + " not suitable "
                                            + "for matching parameter "
                                            + params[ i ].getInfo() );
                }
            }
        }
        matchEngine_ = engine;
        super.setValueFromString( env, stringVal );
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
        String[] names = name.trim().toLowerCase().split( "\\+" );
        MatchEngine[] components = new MatchEngine[ names.length ];
        for ( int i = 0; i < names.length; i++ ) {
            MatchEngine component;
            String cName = names[ i ];
            if ( "sky".equals( cName ) ||
                 "healpix".equals( cName ) ) {
                component = new HEALPixMatchEngine( Coords.ARC_SECOND, false );
            }
            else if ( "skyerr".equals( cName ) ) {
                component = new HEALPixMatchEngine( Coords.ARC_SECOND, true );
            }
            else if ( "sky3d".equals( cName ) ) {
                component = new SphericalPolarMatchEngine( 0. );
            }
            else if ( "exact".equals( cName ) ) {
                component = new EqualsMatchEngine();
            }
            else if ( cName.matches( "[0-9]d" ) ) {
                int ndim = Integer.parseInt( cName.substring( 0, 1 ) );
                component =
                    new IsotropicCartesianMatchEngine( ndim, 0.0, false );
            }
            else if ( cName.matches( "[0-9]d_anisotropic" ) ) {
                int ndim = Integer.parseInt( cName.substring( 0, 1 ) );
                component =
                    new AnisotropicCartesianMatchEngine( new double[ ndim ] );
            }
            else if ( cName.matches( "htm" ) ) {
                component = new HTMMatchEngine( Coords.ARC_SECOND, false );
            }
            else {
                throw new UsageException( "Unknown matcher element: " + cName );
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
     * @return  values usage - possibly empty, not null
     */
    public String getValuesUsage( MatchEngine engine ) {
        StringBuffer sbuf = new StringBuffer();
        ValueInfo[] tupleInfos = engine.getTupleInfos();
        if ( tupleInfos.length > 0 ) {
            sbuf.append( " values*='" );
            for ( int i = 0; i < tupleInfos.length; i++ ) {
                if ( i > 0 ) {
                    sbuf.append( ' ' );
                }
                sbuf.append( '<' )
                    .append( getInfoUsage( tupleInfos[ i ] ) )
                    .append( '>' );
            }
            sbuf.append( '\'' );
        }
        return sbuf.toString();
    }

    /**
     * Returns a string giving the usage for the match parameters part
     * of the matching command line.
     * 
     * @param  engine  match engine
     * @return  params usage - possibly empty, not null
     */
    public String getParamsUsage( MatchEngine engine ) {
        StringBuffer sbuf = new StringBuffer();
        DescribedValue[] params = engine.getMatchParameters();
        if ( params.length > 0 ) {
            sbuf.append( " params='" );
            for ( int i = 0; i < params.length; i++ ) {
                if ( i > 0 ) {
                    sbuf.append( ' ' );
                }
                sbuf.append( '<' )
                    .append( getInfoUsage( params[ i ].getInfo() ) )
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
    private String getInfoUsage( ValueInfo info ) {
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
            "sky", "skyerr", "sky3d", "exact", "1d", "2d", "3d",
            "2d_anisotropic", "3d_anisotropic",
            "sky+1d",
        };
    }
}
