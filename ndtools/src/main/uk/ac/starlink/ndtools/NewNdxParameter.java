package uk.ac.starlink.ndtools;

import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.ndx.NdxIO;
import uk.ac.starlink.task.AbortException;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;

/**
 * Parameter representing a new Ndx object ready for writing.
 */
class NewNdxParameter extends Parameter {

    private NdxIO ndxio = new NdxIO();

    public NewNdxParameter( String name ) {
        super( name );
    }

    /**
     * Gets an Ndx from this parameter.  It will have writable array
     * components and will be based on a given template Ndx (same
     * shape, type etc).
     *
     * @param  template   template Ndx
     * @return  an Ndx with writable array components
     */
    public Ndx getOutputNdx( Ndx template )
             throws ParameterValueException, AbortException {
        checkGotValue();
        String loc = stringValue();
        try {
            if ( ndxio.makeBlankNdx( loc, template ) ) {
                Ndx outndx = ndxio.makeNdx( loc, AccessMode.WRITE );
                return outndx;
            }
            else {
                throw new ParameterValueException( this,
                                                   "Unknown NDX type " + loc );
            }
        }
        catch ( Exception e ) {
            throw new ParameterValueException(
                this, "Failed to write NDX at " + loc, e );
        }
    }

    /**
     * Writes out an existing (readable) Ndx object to the writable NDX
     * represented by this parameter.
     *
     * @param   ndx  an Ndx whose content is to be copied into a new NDX
     *          represented by this parameter
     */
    public void outputNdx( Ndx ndx )
           throws ParameterValueException, AbortException {
        checkGotValue();
        String loc = stringValue();
        try {
            ndxio.outputNdx( loc, ndx );
        }
        catch ( Exception e ) {
            throw new ParameterValueException(
                this, "Failed to write NDX to " + loc, e );
        }
    }
 
}
