package uk.ac.starlink.ttools.cone;

import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;

/**
 * Parameter for selecting {@link ConeErrorPolicy} objects.
 *
 * @author   Mark Taylor
 * @since    24 Jan 2008
 */
public class ConeErrorPolicyParameter extends Parameter<ConeErrorPolicy> {

    private static final ConeErrorPolicy[] FIXED_POLICIES =
        new ConeErrorPolicy[] {
            ConeErrorPolicy.ABORT,
            ConeErrorPolicy.IGNORE,
            ConeErrorPolicy.RETRY,
        };
    private static final String RETRY_PREFIX = ConeErrorPolicy.RETRY.toString();

    /**
     * Constructor.
     *
     * @param   name  parameter name
     */
    @SuppressWarnings("this-escape")
    public ConeErrorPolicyParameter( String name ) {
        super( name, ConeErrorPolicy.class, true );
        StringBuffer ubuf = new StringBuffer();
        for ( int i = 0; i < FIXED_POLICIES.length; i++ ) {
            if ( i > 0 ) {
                ubuf.append( '|' );
            }
            ubuf.append( FIXED_POLICIES[ i ].toString() );
        }
        ubuf.append( '|' )
            .append( RETRY_PREFIX )
            .append( "<n>" );
        setUsage( ubuf.toString() );
        setPrompt( "Action on cone search failure" );
        setStringDefault( ConeErrorPolicy.ABORT.toString() );
        setDescription( new String[] {
            "<p>Determines what will happen if any of the individual cone",
            "search requests fails.  By default the task aborts.",
            "That may be the best thing to do, but for unreliable or",
            "poorly implemented services you may find that some searches",
            "fail and others succeed so it can be best to continue",
            "operation in the face of a few failures.",
            "The options are:",
            "<ul>",
            "<li><code>" + ConeErrorPolicy.ABORT.toString() + "</code>:",
            "Failure of any query terminates the task.",
            "</li>",
            "<li><code>" + ConeErrorPolicy.IGNORE.toString() + "</code>:",
            "Failure of a query is treated the same as a query which",
            "returns no rows.",
            "</li>",
            "<li><code>" + ConeErrorPolicy.RETRY.toString() + "</code>:",
            "Failed queries are retried until they succeed;",
            "an increasing delay is introduced for each failure.",
            "Use with care - if the failure is for some good, or at least",
            "reproducible reason this could prevent the task from ever",
            "completing.",
            "</li>",
            "<li><code>" + RETRY_PREFIX + "&lt;n&gt;" + "</code>:",
            "Failed queries are retried at most a fixed number",
            "<code>&lt;n&gt;</code> of times;",
            "an increasing delay is introduced for each failure.",
            "If failures persist the task terminates.",
            "</li>",
            "</ul>",
            "</p>",
        } );
    }

    /**
     * Returns the value of this parameter as a ConeErrorPolicy.
     *
     * @param  env  execution environment
     * @return  error policy value
     */
    public ConeErrorPolicy policyValue( Environment env ) throws TaskException {
        return objectValue( env );
    }

    public ConeErrorPolicy stringToObject( Environment env, String stringVal )
            throws TaskException {
        for ( int i = 0; i < FIXED_POLICIES.length; i++ ) {
            ConeErrorPolicy policy = FIXED_POLICIES[ i ];
            if ( policy.toString().equalsIgnoreCase( stringVal ) ) {
                return policy;
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
            return ConeErrorPolicy.createRetryPolicy( stringVal, nTry );
        }

        /* No known policy. */
        throw new ParameterValueException( this, "Unknown error action "
                                               + stringVal );
    }
}
