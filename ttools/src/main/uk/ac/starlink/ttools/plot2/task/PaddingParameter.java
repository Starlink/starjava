package uk.ac.starlink.ttools.plot2.task;

import uk.ac.starlink.ttools.plot2.Padding;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;

/**
 * Parameter for receiving a Padding specification.
 * The string representation is 4 integers separated by commas;
 * any of the places may be blank.
 * The sequence of values (top,left,bottom,right) follows that of
 * {@link java.awt.Insets}.
 *
 * @author   Mark Taylor
 * @since    9 Dec 2016
 */
public class PaddingParameter extends Parameter<Padding> {

    /**
     * Constructor.
     *
     * @param  name  parameter name
     */
    public PaddingParameter( String name ) {
        super( name, Padding.class, true );
        setUsage( "<top>,<left>,<bottom>,<right>" );
        setNullPermitted( true );
    }

    public Padding stringToObject( Environment env, String stringval )
            throws TaskException {
        String[] svals = stringval.split( ",", -1 );
        if ( svals.length != 4 ) {
            throw new ParameterValueException( this,
                                               "Not 4 comma-separated values" );
        }
        Integer[] ivals = new Integer[ 4 ];
        for ( int i = 0; i < 4; i++ ) {
            String sv = svals[ i ].trim();
            if ( sv.length() > 0 ) {
                try {
                    ivals[ i ] = Integer.valueOf( sv );
                }
                catch ( NumberFormatException e ) {
                    throw new ParameterValueException( this,
                                                       "Not integer: " + sv );
                }
            }
        }
        return new Padding( ivals[ 0 ], ivals[ 1 ], ivals[ 2 ], ivals[ 3 ] );
    }
}
