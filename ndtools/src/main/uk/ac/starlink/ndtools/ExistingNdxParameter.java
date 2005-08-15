package uk.ac.starlink.ndtools;

import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.ndx.NdxIO;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;

/**
 * Parameter representing an Ndx object which already exists.
 */
class ExistingNdxParameter extends Parameter {

    private NdxIO ndxio = new NdxIO();
    private Ndx ndxvalue;
    private AccessMode accessMode = AccessMode.READ;

    public ExistingNdxParameter( String name ) {
        super( name );
    }

    /**
     * Sets the access mode which will be required from the NDX represented
     * by this parameter.  By default only read access is available.
     *
     * @param  mode  access mode - either AccessMode.READ or AccessMode.UPDATE
     */
    public void setAccessMode( AccessMode mode ) {
        if ( mode == AccessMode.READ || mode == AccessMode.UPDATE ) {
            this.accessMode = mode;
        }
        else {
            throw new IllegalArgumentException( "Writable not allowed" );
        }
    }

    /**
     * Get the value of the parameter as an Ndx object.
     */
    public Ndx ndxValue( Environment env ) throws TaskException {
        checkGotValue( env );
        return ndxvalue;
    }

    public void setValueFromString( Environment env, String stringval )
            throws TaskException {
        String loc = stringval;
        try {
            ndxvalue = ndxio.makeNdx( loc, accessMode );
        }
        catch ( Exception e ) {
            throw new ParameterValueException(
                this, "Failed to read NDX from " + loc, e );
        }
        if ( ndxvalue == null ) {
            throw new ParameterValueException(
                this, "Unknown NDX type " + loc );
        }
        super.setValueFromString( env, stringval );
    }
    
}
