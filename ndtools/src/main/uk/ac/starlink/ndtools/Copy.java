package uk.ac.starlink.ndtools;

import java.io.IOException;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.ndx.NdxIO;
import uk.ac.starlink.task.AbortException;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.Task;

/**
 * Copies one NDX to another.
 */
class Copy implements Task {

    private ExistingNdxParameter inpar;
    private NewNdxParameter outpar;

    public Copy() {
        inpar = new ExistingNdxParameter( "in" );
        inpar.setPrompt( "Input NDX" );
        inpar.setPosition( 1 );

        outpar = new NewNdxParameter( "out" );
        outpar.setPrompt( "Output NDX" );
        outpar.setPosition( 2 );
    }

    public Parameter[] getParameters() {
        return new Parameter[] { inpar, outpar };
    }

    public String getUsage() {
        return "in out";
    }

    public void invoke( Environment env ) 
            throws ParameterValueException, AbortException {
        outpar.outputNdx( inpar.ndxValue() );
    }

}
