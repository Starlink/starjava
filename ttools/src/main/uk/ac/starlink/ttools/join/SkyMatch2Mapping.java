package uk.ac.starlink.ttools.join;

import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.join.JoinType;
import uk.ac.starlink.table.join.ProgressIndicator;
import uk.ac.starlink.table.join.PairMode;
import uk.ac.starlink.table.join.FixedSkyMatchEngine;
import uk.ac.starlink.task.ExecutionException;

/**
 * Match2Mapping subclass specific to sky matching.
 * In particular this can attempt to guess RA/DEC columns in the input tables
 * using UCDs etc.
 *
 * @author   Mark Taylor
 * @since    6 Nov 2007
 */
public class SkyMatch2Mapping extends Match2Mapping {

    /** Matches Right Ascension column name. */
    private static final Pattern RA_REGEX;

    /** Matches Declination column name. */
    private static final Pattern DEC_REGEX;
    static {
        try {
            RA_REGEX = Pattern.compile( "RA_?J?(2000)?",
                                        Pattern.CASE_INSENSITIVE );
            DEC_REGEX = Pattern.compile( "DEC?L?_?J?(2000)?",
                                         Pattern.CASE_INSENSITIVE );
        }
        catch ( PatternSyntaxException e ) {
            throw (Error) new AssertionError( "Bad pattern?" )
                         .initCause( e );
        }
    }
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.task" );

    /**
     * Constructor.
     *
     * @param   matcher  sky match engine
     * @param   raExpr1  JEL expression for right ascension/degrees in table 1
     *                   - if null, a guess is made
     * @param   decExpr1 JEL expression for declination/degrees in table 1
     *                   - if null, a guess is made
     * @param   raExpr2  JEL expression for right ascension/degrees in table 2
     *                   - if null, a guess is made
     * @param   decExpr2 JEL expression for declination/degrees in table 2
     *                   - if null, a guess is made
     * @param   join  output row selection type
     * @param   pairMode   pair matching mode
     * @param   fixact1    deduplication fix action for first input table
     * @param   fixact2    deduplication fix action for second input table
     * @param   progger    progress indicator for match process
     */
    public SkyMatch2Mapping( FixedSkyMatchEngine matcher, 
                             String raExpr1, String decExpr1,
                             String raExpr2, String decExpr2,
                             JoinType join, PairMode pairMode,
                             JoinFixAction fixact1, JoinFixAction fixact2,
                             ProgressIndicator progger ) {
        super( new HumanMatchEngine( matcher ),
               new String[] { raExpr1, decExpr1, }, 
               new String[] { raExpr2, decExpr2, }, join, pairMode,
               fixact1, fixact2, 
               new HumanMatchEngine( matcher ).getMatchScoreInfo(), progger );
    }

    protected StarTable makeSubTable( StarTable inTable, String[] exprTuple )
            throws ExecutionException {
        String raEx = exprTuple[ 0 ];
        String decEx = exprTuple[ 1 ];
        String raExpr = ( raEx == null || raEx.trim().length() == 0 ) 
                      ? guessDegreesExpression( inTable, "ra", RA_REGEX )
                      : raEx;
        String decExpr = ( decEx == null || decEx.trim().length() == 0 )
                       ? guessDegreesExpression( inTable, "dec", DEC_REGEX )
                       : decEx;
        if ( raExpr == null || decExpr == null ) {
            throw new ExecutionException( "Failed to identify likely "
                                        + "RA/DEC columns" );
        }
        return super.makeSubTable( inTable, new String[] { raExpr, decExpr, } );
    }

    /**
     * Looks at table columns and makes a guess at a JEL expression which 
     * will give RA/Dec values in degrees.
     *
     * @param  inTable  input table
     * @param  ucdAtom  UCD atom describing value "ra" or "dec"
     * @param  nameRegex  regular expression for matching a column name
     *                    giving the quantity (not necessarily in degrees)
     * @return  JEL expression giving angle in degrees, or null if none
     *          can be found
     */
    static String guessDegreesExpression( StarTable inTable, String ucdAtom,
                                          Pattern nameRegex ) {

        /* Prepare possible matching UCD1 and UCD1+ strings. */
        String atom = ucdAtom.toLowerCase();
        final String ucd1Part = "pos_eq_" + atom;
        final String ucd1Full = ucd1Part + "_main";
        final String ucd1pPart = "pos.eq." + atom;
        final String ucd1pFull = ucd1pPart + ";meta.main";

        /* Examine each column, assigning a score to columns that look like
         * they might be what we're after.  The best score is retained. */
        int bestIndex = -1;
        int score = 0;
        int ncol = inTable.getColumnCount();
        for ( int i = 0; i < ncol; i++ ) {
            ColumnInfo info = inTable.getColumnInfo( i );
            if ( Number.class.isAssignableFrom( info.getContentClass() ) ) {
                String ucd = info.getUCD();
                String name = info.getName();
                if ( ucd != null && ucd.length() > 0 ) {
                    ucd = ucd.trim().toLowerCase();

                    /* Full UCD match. */
                    if ( score < 20 &&
                         ( ucd.equals( ucd1Full ) ||
                           ucd.equals( ucd1pFull ) ) ) {
                        bestIndex = i;
                        score = 20;
                    }

                    /* Partial UCD match. */
                    if ( score < 10 &&
                         ( ucd.equals( ucd1Part ) ||
                           ucd.equals( ucd1pPart ) ) ) {
                        bestIndex = i;
                        score = 10;
                    }
                }
                if ( name != null && name.length() > 0 ) {

                    /* Name match. */
                    if ( score < 5 &&
                         nameRegex.matcher( name.trim() ).matches() ) {
                        bestIndex = i;
                        score = 5;
                    }
                }
            }
        }

        /* No leads?  Bail out. */
        if ( bestIndex < 0 ) {
            assert score == 0;
            return null;
        }

        ColumnInfo info = inTable.getColumnInfo( bestIndex );
        logger_.info( "Identified column " + info + " as "
                    + ucdAtom.toUpperCase() );
 
        /* Try to work out the units of the best guess column we have. */
        String units = info.getUnitString();
        double factor;
        if ( units == null || units.trim().length() == 0 ) {
            logger_.info( "No units listed for column " + info.getName()
                        + " - assuming degrees" );
            factor = 1.0;
        }
        else if ( units.toLowerCase().startsWith( "deg" ) ) {
            factor = 1.0;
        }
        else if ( units.toLowerCase().startsWith( "rad" ) ) {
            factor = 180 / Math.PI;
        }
        else {
            logger_.info( "Units for column " + info.getName() + " listed as "
                        + units + " - assuming degrees" );
            factor = 1.0;
        }

        /* Construct and return the final expression.  It's a column reference
         * possibly multiplied by a unit conversion factor.  Use the $n form
         * of column reference since it won't be scuppered by spaces etc
         * in the column name. */
        String expr = "$" + ( bestIndex + 1 );
        if ( factor != 1.0 ) {
            expr = factor + "*" + expr;
        }
        logger_.config( ucdAtom.toUpperCase() + ": " + expr );
        return expr;
    }
}
