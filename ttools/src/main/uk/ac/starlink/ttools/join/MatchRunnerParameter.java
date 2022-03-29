package uk.ac.starlink.ttools.join;

import uk.ac.starlink.table.RowRunner;
import uk.ac.starlink.task.ChoiceParameter;

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
    private static final String PARTEST = "partest";

    /**
     * Constructor.
     *
     * @param  name  parameter name
     */
    public MatchRunnerParameter( String name ) {
        super( name, RowRunner.class );
        setNullPermitted( true );
        addOption( null, CLASSIC );
        addOption( RowRunner.DEFAULT, PARALLEL );
        addOption( RowRunner.SEQUENTIAL, SEQUENTIAL );
        addOption( RowRunner.PARTEST, PARTEST );
        setStringDefault( CLASSIC );
        setPrompt( "Iteration implementation" );
        setDescription( new String[] {
            "<p>Selects the threading implementation.",
            "The options are currently:",
            "<ul>",
            "<li><code>" + CLASSIC + "</code>:",
                "uses legacy sequential implementation",
                "</li>",
            "<li><code>" + PARALLEL + "</code>:",
                "uses multithreaded implementation for large tables",
                "</li>",
            "<li><code>" + SEQUENTIAL + "</code>:",
                "uses multithreaded implementation",
                "but with only a single thread",
                "</li>",
            "<li><code>" + PARTEST + "</code>:",
                "uses multithreaded implementation even when tables are small",
                "</li>",
            "</ul>",
            "The <code>" + PARALLEL + "</code> option",
            "should run faster than the others, at least for large matches",
            "and where multiple processing cores are available.",
            "At present however, this code is less well tested",
            "than the legacy implementation, so",
            "<code>" + CLASSIC + "</code> is the default.",
            "<code>" + SEQUENTIAL + "</code> and",
            "<code>" + PARTEST + "</code> are mostly provided",
            "for testing purposes.",
            "</p>",
            "<p><code>" + PARALLEL + "</code> ought to run correctly,",
            "but if you want to be sure of the results,",
            "you can start by comparing the results from",
            "<code>" + SEQUENTIAL + "</code> and <code>" + PARALLEL + "</code>",
            "and then use <code>" + PARALLEL + "</code> for larger matches.",
            "</p>",
            "<p>Changing this parameter should make no difference",
            "to the matching results.",
            "If you notice any discrepancies",
            "<strong>please report them</strong>.",
            "</p>",
        } );
    }
}
