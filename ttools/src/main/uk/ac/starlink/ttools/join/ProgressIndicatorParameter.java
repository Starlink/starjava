package uk.ac.starlink.ttools.join;

import java.io.PrintStream;
import uk.ac.starlink.table.join.NullProgressIndicator;
import uk.ac.starlink.table.join.ProgressIndicator;
import uk.ac.starlink.table.join.TextProgressIndicator;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.TaskException;

/**
 * Parameter for getting a ProgressIndicator.
 *
 * @author   Mark Taylor
 * @since    20 Nov 2007
 */
public class ProgressIndicatorParameter extends ChoiceParameter {

    private ProgressIndicator indicator_;
    private static final String NONE = "none";
    private static final String LOG = "log";
    private static final String PROFILE = "profile";

    /**
     * Constructor.
     *
     * @param   name  parameter name
     */
    public ProgressIndicatorParameter( String name ) {
        super( name, new String[] { NONE, LOG, PROFILE, } );
        setPrompt( "How to report progress to screen" );
        setDefault( LOG );
        setDescription( new String[] {
            "<p>Determines whether information on progress of the match",
            "should be output to the standard error stream as it progresses.",
            "For lengthy matches this is a useful reassurance and can give",
            "guidance about how much longer it will take.",
            "It can also be useful as a performance diagnostic.",
            "</p>",
            "<p>The options are:",
            "<ul>",
            "<li><code>" + NONE + "</code>:",
                 "no progress is shown",
                 "</li>",
            "<li><code>" + LOG + "</code>:",
                 "progress information is shown",
                 "</li>",
            "<li><code>" + PROFILE + "</code>:",
                 "progress information and limited time/memory profiling",
                 "information are shown",
                 "</li>",
            "</ul>",
            "</p>",
        } );
    }

    public void setValueFromString( Environment env, String sval )
             throws TaskException {
        super.setValueFromString( env, sval );
        ProgressIndicator indicator = null;
        PrintStream strm = env.getErrorStream();
        if ( strm != null ) {
            if ( LOG.equals( sval ) ) {
                indicator = new TextProgressIndicator( strm, false );
            }
            else if ( PROFILE.equals( sval ) ) {
                indicator = new TextProgressIndicator( strm, true );
            }
        }
        if ( indicator == null ) {
            indicator = new NullProgressIndicator();
        }
        indicator_ = indicator;
    }

    /**
     * Returns the value of this parameter as a ProgressIndicator object.
     *
     * @param   env  execution environment
     * @return  progress indicator
     */
    public ProgressIndicator progressIndicatorValue( Environment env )
            throws TaskException {
        checkGotValue( env );
        return indicator_;
    }
}
