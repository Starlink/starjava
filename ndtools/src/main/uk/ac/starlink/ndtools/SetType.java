package uk.ac.starlink.ndtools;

import java.io.IOException;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDArrays;
import uk.ac.starlink.array.Requirements;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.ndx.DefaultMutableNdx;
import uk.ac.starlink.ndx.MutableNdx;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.task.AbortException;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.Task;

/**
 * Sets the data type of an NDX to one of the known numerical types.
 */
class SetType implements Task {

    private ExistingNdxParameter inpar;
    private NewNdxParameter outpar;
    private TypeParameter typepar;

    public SetType() {
        inpar = new ExistingNdxParameter( "in" );
        inpar.setPrompt( "Input NDX" );
        inpar.setPosition( 1 );

        outpar = new NewNdxParameter( "out" );
        outpar.setPrompt( "Output NDX" );
        outpar.setPosition( 2 );

        typepar = new TypeParameter( "type" );
        typepar.setPrompt( "New type" );
        typepar.setPosition( 3 );
    }

    public String getUsage() {
        return "in out newtype";
    }

    public Parameter[] getParameters() {
        return new Parameter[] { inpar, outpar, typepar };
    }

    public void invoke( Environment env ) 
            throws ParameterValueException, AbortException, IOException {

        Ndx ndx1 = inpar.ndxValue();
        Type type = typepar.typeValue();
        Requirements req = new Requirements( AccessMode.READ )
                          .setType( type );

        NDArray image = NDArrays.toRequiredArray( ndx1.getImage(), req );
        NDArray variance = null;
        if ( ndx1.hasVariance() ) {
            variance = NDArrays.toRequiredArray( ndx1.getVariance(), req );
        }
        NDArray quality = null;
        if ( ndx1.hasQuality() ) {
            quality = ndx1.getQuality();
        }
        MutableNdx ndx2 = new DefaultMutableNdx( ndx1 );
        ndx2.setImage( image );
        ndx2.setVariance( variance );
        ndx2.setQuality( quality );
        outpar.outputNdx( ndx2 );
    }
}
