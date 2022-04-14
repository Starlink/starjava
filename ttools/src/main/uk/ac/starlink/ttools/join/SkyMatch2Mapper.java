package uk.ac.starlink.ttools.join;

import java.io.PrintStream;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.RowRunner;
import uk.ac.starlink.table.join.CdsHealpixSkyPixellator;
import uk.ac.starlink.table.join.FixedSkyMatchEngine;
import uk.ac.starlink.table.join.HealpixSkyPixellator;
import uk.ac.starlink.table.join.JoinType;
import uk.ac.starlink.table.join.NullProgressIndicator;
import uk.ac.starlink.table.join.PairMode;
import uk.ac.starlink.table.join.ProgressIndicator;
import uk.ac.starlink.table.join.TextProgressIndicator;
import uk.ac.starlink.task.DoubleParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.func.CoordsRadians;
import uk.ac.starlink.ttools.task.SkyCoordParameter;
import uk.ac.starlink.ttools.task.TableMapper;
import uk.ac.starlink.ttools.task.TableMapping;

/**
 * TableMapper which does the work for sky-specific pair matching (tskymatch2).
 *
 * @author   Mark Taylor
 * @since    2 Nov 2007
 */
public class SkyMatch2Mapper implements TableMapper {

    private final Parameter<?>[] raParams_;
    private final Parameter<?>[] decParams_;
    private final DoubleParameter errorParam_;
    private final JoinTypeParameter joinParam_;
    private final FindModeParameter modeParam_;
    private final IntegerParameter healpixkParam_;
    private final Parameter<RowRunner> runnerParam_;

    /**
     * Constructor.
     */
    public SkyMatch2Mapper() {
        raParams_ = new Parameter<?>[ 2 ];
        decParams_ = new Parameter<?>[ 2 ];
        for ( int i = 0; i < 2; i++ ) {
            int i1 = i + 1;
            raParams_[ i ] =
                SkyCoordParameter
               .createRaParameter( "ra" + i1, null, "table " + i1 );
            decParams_[ i ] =
                SkyCoordParameter
               .createDecParameter( "dec" + i1, null, "table " + i1 );
        }

        errorParam_ = new DoubleParameter( "error" );
        errorParam_.setUsage( "<value/arcsec>" );
        errorParam_.setPrompt( "Maximum separation in arcsec" );
        errorParam_.setDescription( new String[] {
            "<p>The maximum separation permitted between two objects",
            "for them to count as a match.  Units are arc seconds.",
            "</p>",
        } );

        healpixkParam_ = new IntegerParameter( "tuning" );
        healpixkParam_.setUsage( "<healpix-k>" );
        healpixkParam_.setPrompt( "HEALPix pixel size parameter" );
        healpixkParam_.setDescription( new String[] {
            "<p>Tuning parameter that controls the pixel size used when",
            "binning the rows.",
            "The legal range is from",
            "0 (corresponding to pixel size of about 60 degrees) to",
            "20 (about 0.2 arcsec).",
            "The value of this parameter will not affect the result",
            "but may affect the performance in terms of CPU and memory",
            "resources required.",
            "A default value will be chosen based on the size of the",
            "<code>" + errorParam_.getName() + "</code>",
            "parameter, but it may be possible to improve performance by",
            "adjusting the default value.",
            "The value used can be seen by examining the progress output.",
            "If your match is taking a long time or is failing from lack",
            "of memory it may be worth trying different values",
            "for this parameter.",
            "</p>",
        } );
        healpixkParam_.setNullPermitted( true );
        healpixkParam_.setMinimum( 0 );
        healpixkParam_.setMaximum( 20 );

        joinParam_ = new JoinTypeParameter( "join" );
        modeParam_ = new FindModeParameter( "find" );
        runnerParam_ = new MatchRunnerParameter( "runner" );
    }

    public Parameter<?>[] getParameters() {
        return new Parameter<?>[] {
            raParams_[ 0 ],
            decParams_[ 0 ],
            raParams_[ 1 ],
            decParams_[ 1 ],
            errorParam_,
            healpixkParam_,
            joinParam_,
            modeParam_,
            runnerParam_,
        }; 
    }

    public TableMapping createMapping( Environment env, int nin )
            throws TaskException {
        String ra1 = raParams_[ 0 ].stringValue( env );
        String dec1 = decParams_[ 0 ].stringValue( env );
        String ra2 = raParams_[ 1 ].stringValue( env );
        String dec2 = decParams_[ 1 ].stringValue( env );
        double error = errorParam_.doubleValue( env )
                     * CoordsRadians.ARC_SECOND_RADIANS;
        if ( error < 0 ) {
            throw new ParameterValueException( errorParam_,
                                               "Negative value illegal" );
        }
        HealpixSkyPixellator pixer = new CdsHealpixSkyPixellator();
        FixedSkyMatchEngine.InDegrees matcher =
            new FixedSkyMatchEngine.InDegrees( pixer, error );
        int defk = pixer.getHealpixK();
        if ( defk >= 0 ) {
            healpixkParam_.setIntDefault( defk );
        }
        int k = healpixkParam_.intValue( env );
        pixer.setHealpixK( k );
        JoinType join = joinParam_.joinTypeValue( env );
        PairMode pairMode = modeParam_.objectValue( env );

        JoinFixAction fixact1 =
            JoinFixAction.makeRenameDuplicatesAction( "_1", false, true );
        JoinFixAction fixact2 =
            JoinFixAction.makeRenameDuplicatesAction( "_2", false, true );
        PrintStream err = env.getErrorStream();
        ProgressIndicator progger = err == null
                                  ? new NullProgressIndicator()
                                  : new TextProgressIndicator( err, false );
        RowRunner runner = runnerParam_.objectValue( env );
        return new SkyMatch2Mapping( matcher, ra1, dec1, ra2, dec2, join,
                                     pairMode, fixact1, fixact2, progger,
                                     runner );
    }
}
