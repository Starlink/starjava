package uk.ac.starlink.ttools.join;

import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.RowRunner;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.join.JoinType;
import uk.ac.starlink.table.join.ProgressIndicator;
import uk.ac.starlink.table.join.PairMode;
import uk.ac.starlink.table.join.FixedSkyMatchEngine;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.ttools.task.SkyCoordParameter;

/**
 * Match2Mapping subclass specific to sky matching.
 *
 * @author   Mark Taylor
 * @since    6 Nov 2007
 */
public class SkyMatch2Mapping extends Match2Mapping {

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
     * @param   runner    controls parallel implementation,
     *                    or null for sequential
     */
    public SkyMatch2Mapping( FixedSkyMatchEngine.InDegrees matcher, 
                             String raExpr1, String decExpr1,
                             String raExpr2, String decExpr2,
                             JoinType join, PairMode pairMode,
                             JoinFixAction fixact1, JoinFixAction fixact2,
                             ProgressIndicator progger, RowRunner runner ) {
        super( matcher,
               new String[] { raExpr1, decExpr1, }, 
               new String[] { raExpr2, decExpr2, }, join, pairMode,
               fixact1, fixact2, 
               matcher.getMatchScoreInfo(), progger, runner );
    }

    protected StarTable makeSubTable( StarTable inTable, String[] exprTuple )
            throws ExecutionException {
        String raEx = exprTuple[ 0 ];
        String decEx = exprTuple[ 1 ];
        String raExpr = ( raEx == null || raEx.trim().length() == 0 ) 
                      ? SkyCoordParameter.guessRaDegreesExpression( inTable )
                      : raEx;
        String decExpr = ( decEx == null || decEx.trim().length() == 0 )
                       ? SkyCoordParameter.guessDecDegreesExpression( inTable )
                       : decEx;
        if ( raExpr == null || decExpr == null ) {
            throw new ExecutionException( "Failed to identify likely "
                                        + "RA/DEC columns" );
        }
        return super.makeSubTable( inTable, new String[] { raExpr, decExpr, } );
    }
}
