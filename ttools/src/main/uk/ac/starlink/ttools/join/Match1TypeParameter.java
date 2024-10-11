package uk.ac.starlink.ttools.join;

import uk.ac.starlink.table.join.Match1Type;
import uk.ac.starlink.table.join.MatchStarTables;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;

/**
 * Parameter subclass whose value is a 
 * {@link uk.ac.starlink.table.join.Match1Type} object.
 *
 * @author   Mark Taylor
 * @since    16 Nov 2007
 */
public class Match1TypeParameter extends Parameter<Match1Type> {

    /** Parameter value for identify action. */
    public static final String IDENTIFY = "identify";

    /** Parameter value for keep0 action. */
    public static final String ELIMINATE_0 = "keep0";

    /** Parameter value for keep1 action. */
    public static final String ELIMINATE_1 = "keep1";

    /** Parameter value prefix for n-fold table output. */
    public static final String WIDE_PREFIX = "wide";

    /**
     * Constructor.
     *
     * @param   name  parameter name
     */
    @SuppressWarnings("this-escape")
    public Match1TypeParameter( String name ) {
        super( name, Match1Type.class, true );
        setStringDefault( IDENTIFY );
        setNullPermitted( false );
        String usage = new StringBuffer()
            .append( IDENTIFY )
            .append( '|' )
            .append( ELIMINATE_0 )
            .append( '|' )
            .append( ELIMINATE_1 )
            .append( '|' )
            .append( WIDE_PREFIX )
            .append( 2 )
            .append( '|' )
            .append( WIDE_PREFIX )
            .append( 'N' )
            .toString();
        setUsage( usage );
        setPrompt( "Type of output table to generate from matching" );
        String grpIdName = MatchStarTables.GRP_ID_INFO.getName();
        String grpSizeName = MatchStarTables.GRP_SIZE_INFO.getName();
        setDescription( new String[] {
            "<p>Determines the form of the table which will be output",
            "as a result of the internal match.",
            "<ul>",
            "<li><code>" + IDENTIFY + "</code>:",
            "The output table is the same as the input table except that",
            "it contains two additional columns,",
            "<code>" + grpIdName + "</code> and ",
            "<code>" + grpSizeName + "</code>,",
            "following the input columns.",
            "Each group of rows which matched is assigned a unique integer,",
            "recorded in the " + grpIdName + " column,",
            "and the size of each group is recorded in the " + grpSizeName,
            "column.",
            "Rows which don't match any others (singles) have null values in",
            "both these columns.",
            "</li>",
            "<li><code>" + ELIMINATE_0 + "</code>:",
            "The result is a new table containing only \"single\" rows,",
            "that is ones which don't match any other rows in the table.",
            "Any other rows are thrown out.",
            "</li>",
            "<li><code>" + ELIMINATE_1 + "</code>:",
            "The result is a new table in which only one row",
            "(the first in the input table order)",
            "from each group of matching ones is retained.",
            "A subsequent intra-table match with the same criteria",
            "would therefore show no matches.",
            "</li>",
            "<li><code>" + WIDE_PREFIX + "N</code>:",
            "The result is a new \"wide\" table consisting of matched rows in",
            "the input table stacked next to each other.",
            "Only groups of exactly N rows in the input table are used to",
            "form the output table; each row of the output table consists of",
            "the columns of the first group member, followed by the columns of",
            "the second group member and so on.",
            "The output table therefore has",
            "N times as many columns as the input table.",
            "The column names in the new table have",
            "<code>_1</code>, <code>_2</code>, ...",
            "appended to them to avoid duplication.",
            "</li>",
            "</ul>",
            "</p>",
        } );
    }

    /**
     * Returns the value of this parameter as a Match1Type object.
     *
     * @param   env  execution environment
     */
    public Match1Type typeValue( Environment env ) throws TaskException {
        return objectValue( env );
    }

    public Match1Type stringToObject( Environment env, String sval )
            throws ParameterValueException {
        if ( sval.equalsIgnoreCase( IDENTIFY ) ) {
            return Match1Type.createIdentifyType();
        }
        else if ( sval.equalsIgnoreCase( ELIMINATE_0 ) ) {
            return Match1Type.createEliminateMatchesType( 0 );
        }
        else if ( sval.equalsIgnoreCase( ELIMINATE_1 ) ) {
            return Match1Type.createEliminateMatchesType( 1 );
        }
        else if ( sval.toLowerCase().startsWith( WIDE_PREFIX.toLowerCase() ) ) {
            String postFix = sval.substring( WIDE_PREFIX.length() );
            int wideness;
            try {
                wideness = Integer.parseInt( postFix );
            }
            catch ( NumberFormatException e ) {
                throw new ParameterValueException( this,
                                                   postFix + " not a number",
                                                   e );
            }
            return Match1Type.createWideType( wideness );
        }
        else {
            throw new ParameterValueException( this,
                                               "Unknown internal match type" );
        }
    }
}
