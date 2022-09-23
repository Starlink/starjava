package uk.ac.starlink.ttools.join;

import java.util.concurrent.ForkJoinPool;
import uk.ac.starlink.table.RowRunner;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.util.SplitPolicy;
import uk.ac.starlink.util.SplitProcessor;

/**
 * Parameter for acquiring a RowRunner for use in crossmatching.
 * The documentation, though not the functionality, is specific to
 * the crossmatching case.
 * The returned value may be <code>null</code>,
 * which corresponds to legacy (non-threaded) operation.
 *
 * @author   Mark Taylor
 * @since    31 Aug 2021
 */
public class MatchRunnerParameter extends ChoiceParameter<RowRunner> {

    private static final String CLASSIC = "classic";
    private static final String SEQUENTIAL = "sequential";
    private static final String PARALLEL = "parallel";
    private static final String PARALLEL_ALL = "parallel-all";
    private static final String PARTEST = "partest";

    /** Maximum value for default parallelism. */
    public static final int DFLT_PARALLELISM_LIMIT = 6;

    /** Actual value for default parallelism (also limited by machine). */
    public static final int DFLT_PARALLELISM =
        Math.min( DFLT_PARALLELISM_LIMIT,
                  Runtime.getRuntime().availableProcessors() );

    /** Default parallel value (parallel with default parallelism). */
    public static final RowRunner PARALLEL_RUNNER =
        createParallelMatchRunner( DFLT_PARALLELISM );

    /**
     * Constructor.
     *
     * @param  name  parameter name
     */
    public MatchRunnerParameter( String name ) {
        super( name, RowRunner.class );
        setNullPermitted( true );
        addOption( PARALLEL_RUNNER, PARALLEL );
        addOption( RowRunner.DEFAULT, PARALLEL_ALL );
        addOption( RowRunner.SEQUENTIAL, SEQUENTIAL );
        addOption( null, CLASSIC );
        addOption( RowRunner.PARTEST, PARTEST );
        setStringDefault( PARALLEL );
        setPrompt( "Threading implementation" );
        setUsage( String.join( "|",
            PARALLEL,
            PARALLEL + "<n>",
            PARALLEL_ALL,
            SEQUENTIAL,
            CLASSIC,
            PARTEST
        ) );
        setDescription( new String[] {
            "<p>Selects the threading implementation.",
            "The options are currently:",
            "<ul>",
            "<li><code>" + PARALLEL + "</code>:",
                "uses multithreaded implementation for large tables,",
                "with default parallelism,",
                "which is the smaller of " + DFLT_PARALLELISM_LIMIT,
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
            "is currently limited to a parallelism of " +
            DFLT_PARALLELISM_LIMIT,
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
            return createParallelMatchRunner( nThread );
        }
        else {
            return super.stringToObject( env, sval );
        }
    }

    /**
     * Creates a runner instance that runs in parallel with a given
     * number of threads.
     *
     * @param   nThread  parallelism
     * @return   new runner
     */
    private static RowRunner createParallelMatchRunner( int nThread ) {
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
