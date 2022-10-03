package uk.ac.starlink.ttools.task;

import java.util.concurrent.ForkJoinPool;
import uk.ac.starlink.table.RowRunner;
import uk.ac.starlink.table.join.RowMatcher;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.util.SplitPolicy;
import uk.ac.starlink.util.SplitProcessor;

/**
 * Parameter for acquiring a RowRunner.
 *
 * <p>The details of documentation differ according to what the runner is
 * to be used for, so factory methods are provided instead of a public
 * constructor.
 *
 * @author   Mark Taylor
 * @since    3 Oct 2022
 */
public class RowRunnerParameter extends ChoiceParameter<RowRunner> {

    private static final String CLASSIC = "classic";
    private static final String SEQUENTIAL = "sequential";
    private static final String PARALLEL = "parallel";
    private static final String PARALLEL_ALL = "parallel-all";
    private static final String PARTEST = "partest";

    /** Default runner instance for cross-matching purposes. */
    public static final RowRunner DFLT_MATCH_RUNNER =
        createParallelRowRunner( RowMatcher.DFLT_PARALLELISM );

    /**
     * Constructor.
     *
     * @param  name  parameter name
     */
    private RowRunnerParameter( String name ) {
        super( name, RowRunner.class );
    }

    @Override
    public RowRunner stringToObject( Environment env, String sval )
            throws TaskException {
        if ( sval.matches( PARALLEL + "[0-9]+" ) ) {
            String nTxt = sval.substring( PARALLEL.length() );
            int nThread;
            try {
                nThread = Integer.parseInt( nTxt );
            }
            catch ( NumberFormatException e ) {
                String msg = "Bad thread count specifier \"" + nTxt + "\"";
                throw new ParameterValueException( this, msg );
            }
            return createParallelRowRunner( nThread );
        }
        else {
            return super.stringToObject( env, sval );
        }
    }

    /**
     * Creates a runner parameter suitable for use with crossmatching tasks.
     * The parameter value may be null, which corresponds to legacy
     * (non-threaded) operation.
     *
     * @param  name  parameter name
     * @return   new parameter
     */
    public static RowRunnerParameter createMatchRunnerParameter( String name ) {
        final int dfltParallelismLimit = RowMatcher.DFLT_PARALLELISM_LIMIT;
        final RowRunner dfltParallelRunner = DFLT_MATCH_RUNNER;
        RowRunnerParameter param = new RowRunnerParameter( name );
        param.setNullPermitted( true );
        param.addOption( dfltParallelRunner, PARALLEL );
        param.addOption( RowRunner.DEFAULT, PARALLEL_ALL );
        param.addOption( RowRunner.SEQUENTIAL, SEQUENTIAL );
        param.addOption( null, CLASSIC );
        param.addOption( RowRunner.PARTEST, PARTEST );
        param.setStringDefault( PARALLEL );
        param.setPrompt( "Threading implementation" );
        param.setUsage( String.join( "|",
            PARALLEL,
            PARALLEL + "<n>",
            PARALLEL_ALL,
            SEQUENTIAL,
            CLASSIC,
            PARTEST
        ) );
        param.setDescription( new String[] {
            "<p>Selects the threading implementation.",
            "The options are currently:",
            "<ul>",
            "<li><code>" + PARALLEL + "</code>:",
                "uses multithreaded implementation for large tables,",
                "with default parallelism,",
                "which is the smaller of " + dfltParallelismLimit,
                "and the number of available processors",
                "</li>",
            "<li><code>" + PARALLEL + "&lt;n&gt;</code>:",
                "uses multithreaded implementation for large tables,",
                "with parallelism given by the supplied value",
                "<code>&lt;n&gt;</code>",
                "</li>",
            "<li><code>" + PARALLEL_ALL + "</code>:",
                "uses multithreaded implementation for large tables,",
                "with a parallelism given by the number of",
                "available processors",
                "</li>",
            "<li><code>" + SEQUENTIAL + "</code>:",
                "uses multithreaded implementation",
                "but with only a single thread",
                "</li>",
            "<li><code>" + CLASSIC + "</code>:",
                "uses legacy sequential implementation",
                "</li>",
            "<li><code>" + PARTEST + "</code>:",
                "uses multithreaded implementation even when tables are small",
                "</li>",
            "</ul>",
            "The <code>" + PARALLEL + "*</code> options",
            "should normally run faster than",
            "<code>" + SEQUENTIAL + "</code> or <code>" + CLASSIC + "</code>",
            "(which are provided mainly for testing purposes),",
            "at least for large matches",
            "and where multiple processing cores are available.",
            "</p>",
            "<p>The default value \"<code>" + PARALLEL + "</code>\"",
            "is currently limited to a parallelism of " + dfltParallelismLimit,
            "since larger values yield diminishing returns given that",
            "some parts of the matching algorithms run sequentially",
            "(Amdahl's Law), and using too many threads",
            "can sometimes end up doing more work",
            "or impacting on other operations on the same machine.",
            "But you can experiment with other concurrencies,",
            "e.g. \"<code>" + PARALLEL + "16</code>\" to run on 16 cores",
            "(if available) or \"<code>" + PARALLEL_ALL + "</code>\"",
            "to run on all available cores.",
            "</p>",
            "<p>The value of this parameter should make no difference",
            "to the matching results.",
            "If you notice any discrepancies",
            "<strong>please report them</strong>.",
            "</p>",
        } );
        return param;
    }

    /**
     * Creates a runner instance that runs in parallel with a given
     * number of threads.
     *
     * @param   nThread  parallelism
     * @return   new runner
     */
    private static RowRunner createParallelRowRunner( int nThread ) {
        SplitPolicy policy =
            new SplitPolicy( () -> new ForkJoinPool( nThread ),
                             SplitPolicy.DFLT_MIN_TASK_SIZE,
                             SplitPolicy.DFLT_MAX_TASKS_PER_CORE );
        boolean isPool = false;
        SplitProcessor<?> splitProcessor =
            SplitProcessor.createStandardProcessor( policy, isPool );
        return new RowRunner( splitProcessor ) {
            @Override
            public String toString() {
                return PARALLEL + Integer.toString( nThread );
            }
        };
    }
}
