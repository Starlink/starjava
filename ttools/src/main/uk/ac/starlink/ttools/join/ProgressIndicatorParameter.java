package uk.ac.starlink.ttools.join;

import java.io.PrintStream;
import uk.ac.starlink.table.join.NullProgressIndicator;
import uk.ac.starlink.table.join.ProgressIndicator;
import uk.ac.starlink.table.join.TextProgressIndicator;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;

/**
 * Parameter for getting a ProgressIndicator.
 *
 * @author   Mark Taylor
 * @since    20 Nov 2007
 */
public class ProgressIndicatorParameter extends ChoiceParameter<String> {

    private static final String NONE;
    private static final String LOG;
    private static final String TIME;
    private static final String PROFILE;
    private static final String[] OPTIONS = {
        NONE = "none",
        LOG = "log",
        TIME = "time",
        PROFILE = "profile",
    };

    /**
     * Constructor.
     *
     * @param   name  parameter name
     */
    public ProgressIndicatorParameter( String name ) {
        super( name, OPTIONS );
        setPrompt( "How to report progress to screen" );
        setStringDefault( LOG );
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
            "<li><code>" + TIME + "</code>:",
                 "progress information and some time profiling",
                 "information is shown",
                 "</li>",
            "<li><code>" + PROFILE + "</code>:",
                 "progress information and limited time/memory profiling",
                 "information are shown",
                 "</li>",
            "</ul>",
            "</p>",
        } );
    }

    /**
     * Returns the progress indicator indicated by the value of this parameter.
     *
     * @param  env  execution environment
     * @return  progress indicator, not null
     */
    public ProgressIndicator progressIndicatorValue( Environment env )
             throws TaskException {
        return stringToProgressIndicator( env, objectValue( env ) );
    }

    public String stringToObject( Environment env, String sval )
            throws TaskException {
        stringToProgressIndicator( env, sval );  // validation
        return sval;
    }

    /**
     * Turns a string value of this parameter into a progress indicator.
     *
     * @param  env  execution environment
     * @param  sval  string value of this parameter
     * @return   non-null progress indicator
     */
    private ProgressIndicator stringToProgressIndicator( Environment env,
                                                         String sval )
            throws TaskException {
        PrintStream strm = env.getErrorStream();
        if ( strm != null ) {
            if ( LOG.equals( sval ) ) {
                return TextProgressIndicator
                      .createInstance( strm, false, false );
            }
            else if ( TIME.equals( sval ) ) {
                return TextProgressIndicator
                      .createInstance( strm, true, false );
            }
            else if ( PROFILE.equals( sval ) ) {
                return TextProgressIndicator
                      .createInstance( strm, true, true );
            }
            else if ( NONE.equals( sval ) ) {
                return new NullProgressIndicator();
            }
            else {
                throw new ParameterValueException( this, "Bad value " + sval );
            }
        }
        else {
            return new NullProgressIndicator();
        }
    }
}
