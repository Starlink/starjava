package uk.ac.starlink.ttools.task;

import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.join.HEALPixMatchEngine;
import uk.ac.starlink.table.join.JoinType;
import uk.ac.starlink.table.join.MatchEngine;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.DoubleParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.func.Coords;

/**
 * TableMapper which does the work for sky-specific pair matching (tskymatch2).
 *
 * @author   Mark Taylor
 * @since    2 Nov 2007
 */
public class SkyMatch2Mapper implements TableMapper {

    private final Parameter[] raParams_;
    private final Parameter[] decParams_;
    private final DoubleParameter errorParam_;
    private final JoinTypeParameter joinParam_;
    private final FindModeParameter modeParam_;

    /**
     * Constructor.
     */
    public SkyMatch2Mapper() {
        raParams_ = new Parameter[ 2 ];
        decParams_ = new Parameter[ 2 ];
        for ( int i = 0; i < 2; i++ ) {
            int i1 = i + 1;
            Parameter raParam = new Parameter( "ra" + i1 );
            Parameter decParam = new Parameter( "dec" + i1 );
            raParam.setPrompt( "<expr/degs>" );
            decParam.setPrompt( "<expr/degs>" );
            raParam.setPrompt( "Expression for table " + i1
                             + " right ascension in degrees" );
            decParam.setPrompt( "Expression for table " + i1
                              + " declination in degrees" );
            raParam.setDescription( new String[] {
                "<p>Value in degrees for the right ascension of positions in",
                "table " + i1 + " to be matched.",
                "This may simply be a column name, or it may be an",
                "algebraic expression calculated from columns as explained",
                "in <ref id='jel'/>.", 
                "</p>",
            } );
            decParam.setDescription( new String[] {
                "<p>Value in degrees for the declination of positions in",
                "table " + i1 + " to be matched.",
                "This may simply be a column name, or it may be an",
                "algebraic expression calculated from columns as explained",
                "in <ref id='jel'/>.", 
            } );
            raParams_[ i ] = raParam;
            decParams_[ i ] = decParam;
        }

        errorParam_ = new DoubleParameter( "error" );
        errorParam_.setUsage( "<arcsec>" );
        errorParam_.setPrompt( "Maximum separation in arcsec" );

        joinParam_ = new JoinTypeParameter( "join" );
        modeParam_ = new FindModeParameter( "find" );
    }

    public Parameter[] getParameters() {
        return new Parameter[] {
            raParams_[ 0 ],
            decParams_[ 0 ],
            raParams_[ 1 ],
            decParams_[ 1 ],
            errorParam_,
            joinParam_,
            modeParam_,
        }; 
    }

    public TableMapping createMapping( Environment env, int nin )
            throws TaskException {
        String ra1 = raParams_[ 0 ].stringValue( env );
        String dec1 = decParams_[ 0 ].stringValue( env );
        String ra2 = raParams_[ 1 ].stringValue( env );
        String dec2 = decParams_[ 1 ].stringValue( env );
        double error = errorParam_.doubleValue( env ) * Coords.ARC_SECOND;
        if ( error < 0 ) {
            throw new ParameterValueException( errorParam_,
                                               "Negative value illegal" );
        }
        JoinType join = joinParam_.joinTypeValue( env );
        boolean bestOnly = modeParam_.bestOnlyValue( env );

        MatchEngine engine =
            new HumanMatchEngine( new HEALPixMatchEngine( error, false ) );
        JoinFixAction fixact1 =
            JoinFixAction.makeRenameDuplicatesAction( "_1", false, true );
        JoinFixAction fixact2 =
            JoinFixAction.makeRenameDuplicatesAction( "_2", false, true );
        return new Match2Mapping( engine,
                                  new String[] { ra1, dec1, },
                                  new String[] { ra2, dec2, },
                                  join, bestOnly, fixact1, fixact2,
                                  env.getErrorStream() );
    }
}
