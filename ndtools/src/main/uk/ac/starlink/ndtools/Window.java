package uk.ac.starlink.ndtools;

import java.io.IOException;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDArrays;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Requirements;
import uk.ac.starlink.ndx.DefaultMutableNdx;
import uk.ac.starlink.ndx.MutableNdx;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;

/**
 * Task which provides a window on an NDX.  A new NDX is created which
 * is based on an existing one but has a different shape.  Any part
 * which falls outside the underlying shape will be filled with bad pixels.
 * The new shape must have the same number of dimensions as the old one.
 */
class Window implements Task {

    ExistingNdxParameter inpar;
    NewNdxParameter outpar;
    ShapeParameter shapepar;

    public Window() {
        inpar = new ExistingNdxParameter( "in" );
        inpar.setPrompt( "Input NDX" );
        inpar.setPosition( 1 );

        outpar = new NewNdxParameter( "out" );
        outpar.setPrompt( "Output NDX" );
        outpar.setPosition( 2 );

        shapepar = new ShapeParameter( "shape" );
        shapepar.setPrompt( "New shape specification" );
        shapepar.setPosition( 3 );
    }

    public String getPurpose() {
        return "Creates a rectangular window on an existing NDX";
    }

    public Parameter[] getParameters() {
        return new Parameter[] { inpar, outpar, shapepar };
    }

    public Executable createExecutable( Environment env ) throws TaskException {
        return new Windower( inpar.ndxValue( env ),
                             outpar.ndxConsumerValue( env ),
                             shapepar.shapeValue( env ) );
    
    }

    private class Windower implements Executable {

        final Ndx ndx1;
        final NdxConsumer ndxOut;
        final NDShape shape;

        Windower( Ndx ndx1, NdxConsumer ndxOut, NDShape shape ) {
            this.ndx1 = ndx1;
            this.ndxOut = ndxOut;
            this.shape = shape;
        }

        public void execute() throws IOException {

            Requirements req = new Requirements( AccessMode.READ )
                              .setWindow( shape );

            NDArray im = NDArrays.toRequiredArray( ndx1.getImage(), req );
            NDArray var = null;
            if ( ndx1.hasVariance() ) {
                var = NDArrays.toRequiredArray( ndx1.getVariance(), req );
            }
            NDArray qual = null;
            if ( ndx1.hasQuality() ) {
                qual = NDArrays.toRequiredArray( ndx1.getQuality(), req );
            }

            MutableNdx ndx2 = new DefaultMutableNdx( im );
            ndx2.setVariance( var );
            ndx2.setQuality( qual );
            ndxOut.consume( ndx2 );
        }
    }
}
