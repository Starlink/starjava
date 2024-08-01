package uk.ac.starlink.ttools.task;

import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;

/**
 * Parameter for acquiring a fixed-length array of floating point values.
 *
 * @author   Mark Taylor
 * @since    19 Sep 2014
 */
public class DoubleArrayParameter extends Parameter<double[]> {

    private final int count_;

    /**
     * Constructor.
     *
     * @param  name  parameter name
     * @param  count   numeric array length
     */
    @SuppressWarnings("this-escape")
    public DoubleArrayParameter( String name, int count ) {
        super( name, double[].class, false );
        count_ = count;
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < count; i++ ) {
            if ( i > 0 ) {
                sbuf.append( "," );
            }
            sbuf.append( "<num>" );
        }
        setUsage( sbuf.toString() );
    }

    /**
     * Returns the parameter value as an array of doubles.
     *
     * @param  env  execution environment
     * @return   array value
     */
    public double[] doublesValue( Environment env ) throws TaskException {
        return objectValue( env );
    }

    /**
     * Returns the parameter value as an array of floats.
     *
     * @param  env  execution environment
     * @return   array value
     */
    public float[] floatsValue( Environment env ) throws TaskException {
        double[] dvals = objectValue( env );
        if ( dvals == null ) {
            return null;
        }
        else {
            float[] fvals = new float[ dvals.length ];
            for ( int i = 0; i < dvals.length; i++ ) {
                fvals[ i ] = (float) dvals[ i ];
            }
            return fvals;
        }
    }

    public double[] stringToObject( Environment env, String stringval )
            throws TaskException {
        String[] svals = stringval.split( "," );
        if ( svals.length != count_ ) {
            String msg = "Not " + count_ + " comma-separated values";
            throw new ParameterValueException( this, msg );
        }
        double[] dvals = new double[ count_ ];
        for ( int i = 0; i < count_; i++ ) {
            String sv = svals[ i ].trim();
            try {
                dvals[ i ] = Double.parseDouble( sv );
            }
            catch ( NumberFormatException e ) {
                throw new ParameterValueException( this, "Not numeric: " + sv );
            }
        }
        return dvals;
    }

    public String objectToString( Environment env, double[] dvals )
            throws TaskException {
        if ( dvals == null ) {
            return null;
        }
        if ( dvals.length != count_ ) {
            throw new TaskException( "Wrong length" );
        }
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < count_; i++ ) {
            if ( i > 0 ) {
                sbuf.append( "," );
            }
            sbuf.append( dvals[ i ] );
        }
        return sbuf.toString();
    }
}
