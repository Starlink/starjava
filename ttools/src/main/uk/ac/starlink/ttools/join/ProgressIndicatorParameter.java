package uk.ac.starlink.ttools.join;

import java.io.PrintStream;
import uk.ac.starlink.table.join.NullProgressIndicator;
import uk.ac.starlink.table.join.ProgressIndicator;
import uk.ac.starlink.table.join.TextProgressIndicator;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.TaskException;

/**
 * Parameter for getting a ProgressIndicator.
 *
 * @author   Mark Taylor
 * @since    20 Nov 2007
 */
public class ProgressIndicatorParameter extends BooleanParameter {

    private ProgressIndicator indicator_;

    /**
     * Constructor.
     *
     * @param   name  parameter name
     */
    public ProgressIndicatorParameter( String name ) {
        super( name );
        setPrompt( "Report progress to screen" );
        setDefault( "true" );
        setDescription( new String[] {
            "<p>Determines whether information on progress of the match",
            "should be output to the standard error stream as it progresses.",
            "For lengthy matches this is a useful reassurance and can give",
            "guidance about how much longer it will take.",
            "It can also be useful as a performance diagnostic.",
            "</p>",
        } );
    }

    public void setValueFromString( Environment env, String sval )
             throws TaskException {
        super.setValueFromString( env, sval );
        ProgressIndicator indicator;
        if ( booleanValue( env ) ) {
            PrintStream strm = env.getErrorStream();
            if ( strm != null ) {
                indicator = new TextProgressIndicator( strm );
            }
            else {
                indicator = new NullProgressIndicator();
            }
        }
        else {
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
