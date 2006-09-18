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
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;

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

    public String getPurpose() {
        return "Sets the numeric type of an NDX";
    }

    public Parameter[] getParameters() {
        return new Parameter[] { inpar, outpar, typepar };
    }

    public Executable createExecutable( Environment env ) throws TaskException {
        return new Typer( inpar.ndxValue( env ),
                          outpar.ndxConsumerValue( env ),
                          typepar.typeValue( env ) );
    }

    private class Typer implements Executable {

        final Ndx ndx1;
        final NdxConsumer ndxOut;
        final Type type;

        Typer( Ndx ndx1, NdxConsumer ndxOut, Type type ) {
            this.ndx1 = ndx1;
            this.ndxOut = ndxOut;
            this.type = type;
        }

        public void execute() throws IOException {
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
            ndxOut.consume( ndx2 );
        }
    }
}
