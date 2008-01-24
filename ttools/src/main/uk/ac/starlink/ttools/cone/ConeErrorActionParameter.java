package uk.ac.starlink.ttools.cone;

import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;

/**
 * Parameter for selecting {@link ConeErrorAction} objects.
 *
 * @author   Mark Taylor
 * @since    24 Jan 2008
 */
public class ConeErrorActionParameter extends Parameter {

    private ConeErrorAction action_;

    private static final ConeErrorAction[] FIXED_ACTIONS =
        new ConeErrorAction[] {
            ConeErrorAction.ABORT,
            ConeErrorAction.IGNORE,
            ConeErrorAction.RETRY,
        };
    private static final String RETRY_PREFIX = ConeErrorAction.RETRY.toString();

    /**
     * Constructor.
     *
     * @param   name  parameter name
     */
    public ConeErrorActionParameter( String name ) {
        super( name );
        StringBuffer ubuf = new StringBuffer();
        for ( int i = 0; i < FIXED_ACTIONS.length; i++ ) {
            if ( i > 0 ) {
                ubuf.append( '|' );
            }
            ubuf.append( FIXED_ACTIONS[ i ].toString() );
        }
        ubuf.append( '|' )
            .append( RETRY_PREFIX )
            .append( "<n>" );
        setUsage( ubuf.toString() );
        setPrompt( "Action on cone search failure" );
        setDefault( ConeErrorAction.ABORT.toString() );
        setDescription( new String[] {
            "<p>Determines what will happen if any of the individual cone",
            "search requests fails.  By default the task aborts.",
            "That may be the best thing to do, but for unreliable or",
            "poorly implemented services you may find that some searches",
            "fail and others succeed so it can be best to continue",
            "operation in the face of a few failures.",
            "The options are:",
            "<ul>",
            "<li><code>" + ConeErrorAction.ABORT.toString() + "</code>:",
            "failure of any query terminates the task",
            "</li>",
            "<li><code>" + ConeErrorAction.IGNORE.toString() + "</code>:",
            "failure of a query is treated the same as a query which",
            "returns no rows",
            "</li>",
            "<li><code>" + ConeErrorAction.RETRY.toString() + "</code>:",
            "failed queries are retried until they succeed;",
            "use with care - if the failure is for some good, or at least",
            "reproducible reason this could prevent the task from ever",
            "completing",
            "</li>",
            "<li><code>" + RETRY_PREFIX + "&lt;n&gt;" + "</code>:",
            "failed queries are retried at most a fixed number",
            "<code>&lt;n&gt;</code> of times",
            "If they still fail the task terminates.",
            "</li>",
            "</ul>",
            "</p>",
        } );
    }

    public void setValueFromString( Environment env, String stringVal )
            throws TaskException {
        action_ = stringToAction( stringVal );
        super.setValueFromString( env, stringVal );
    }

    /**
     * Returns the value of this parameter as a ConeErrorAction.
     *
     * @param  env  execution environment
     * @return  action value
     */
    public ConeErrorAction actionValue( Environment env ) throws TaskException {
        checkGotValue( env );
        return action_;
    }

    /**
     * Decodes a string representation into a ConeErrorAction object.
     *
     * @param  stringVal   string value
     * @return   action object
     */
    private ConeErrorAction stringToAction( String stringVal )
            throws TaskException {
        for ( int i = 0; i < FIXED_ACTIONS.length; i++ ) {
            ConeErrorAction action = FIXED_ACTIONS[ i ];
            if ( action.toString().equalsIgnoreCase( stringVal ) ) {
                return action;
            }
        } 
        if ( stringVal.toLowerCase()
                      .startsWith( RETRY_PREFIX.toLowerCase() ) ) {
            String numString = stringVal.substring( RETRY_PREFIX.length() );
            int nTry;
            try {
                nTry = Integer.parseInt( numString );
                if ( nTry <= 0 ) {
                    throw new ParameterValueException( this,
                                                       nTry + " out of range" );
                }
            }
            catch ( NumberFormatException e ) {
                throw new ParameterValueException( this, "\"" + numString
                                                 + "\" not numeric" );
            }
            return ConeErrorAction.createRetryAction( stringVal, nTry );
        }

        /* No known action. */
        throw new ParameterValueException( this, "Unknown error action "
                                               + stringVal );
    }
}
