package uk.ac.starlink.ndtools;

import java.io.IOException;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.ArrayImpl;
import uk.ac.starlink.array.BadHandler;
import uk.ac.starlink.array.BridgeNDArray;
import uk.ac.starlink.array.CombineArrayImpl;
import uk.ac.starlink.array.Combiner;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Order;
import uk.ac.starlink.array.OrderedNDShape;
import uk.ac.starlink.array.Requirements;
import uk.ac.starlink.array.Type;
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
 * Task for adding two NDXs.
 */
class Add implements Task {

    private ExistingNdxParameter ndx1par; 
    private ExistingNdxParameter ndx2par;
    private NewNdxParameter ndx3par;

    public Add() {
        ndx1par = new ExistingNdxParameter( "ndx1" );
        ndx1par.setPosition( 1 );
        ndx1par.setPrompt( "First input NDX" );

        ndx2par = new ExistingNdxParameter( "ndx2" );
        ndx2par.setPosition( 2 );
        ndx2par.setPrompt( "Second input NDX" );

        ndx3par = new NewNdxParameter( "ndxsum" );
        ndx3par.setPosition( 3 );
        ndx3par.setPrompt( "Output NDX" );
    }

    public String getPurpose() {
        return "Adds two NDXs pixel-by-pixel";
    }

    public Parameter[] getParameters() {
        return new Parameter[] { ndx1par, ndx2par, ndx3par };
    }

    public Executable createExecutable( Environment env ) throws TaskException {
        return new Adder( ndx1par.ndxValue( env ), ndx2par.ndxValue( env ),
                          ndx3par.ndxConsumerValue( env ) );
    }

    /**
     * Helper class which actually does the addition.
     */
    private static class Adder implements Executable {

        final Ndx ndx1;
        final Ndx ndx2;
        final NdxConsumer ndxOut;

        Adder( Ndx ndx1, Ndx ndx2, NdxConsumer ndxOut ) {
            this.ndx1 = ndx1;
            this.ndx2 = ndx2;
            this.ndxOut = ndxOut;
        }

        public void execute() throws ExecutionException, IOException {

            NDArray im1 = ndx1.getImage();
            NDArray im2 = ndx2.getImage();
            OrderedNDShape shape1 = im1.getShape();
            OrderedNDShape shape2 = im2.getShape();
            NDShape shape = shape1.intersection( shape2 );
            if ( shape == null ) { 
                throw new ExecutionException( "No overlap between shapes " + 
                                              shape1 + " and " + shape2 );
            }
            Order order = shape1.getOrder();
            Type type = im1.getType();
            BadHandler bh =
                BadHandler.getHandler( type, type.defaultBadValue() );
            Requirements req = new Requirements( AccessMode.READ )
                              .setType( type )
                              .setWindow( shape )
                              .setOrder( order );
            boolean hasVar = ndx1.hasVariance() && ndx2.hasVariance();
            Combiner adder = new Combiner() {
                public double combination( double x, double y ) {
                    return x + y;
                }
            };
            NDArray im3 = new BridgeNDArray( 
                new CombineArrayImpl( im1, im2, adder, shape, type, bh ) );
            NDArray var3;
            if ( ndx1.hasVariance() && ndx2.hasVariance() ) {
                NDArray var1 = ndx1.getVariance();
                NDArray var2 = ndx2.getVariance();
                var3 = new BridgeNDArray( 
                    new CombineArrayImpl( var1, var2, adder,
                                          shape, type, bh ) );
            }
            else {
                var3 = null;
            }

            MutableNdx ndx3 = new DefaultMutableNdx( im3 );
            ndx3.setVariance( var3 );
            ndx3.setTitle( "Sum of " 
                         + ( ndx1.hasTitle() ? ndx1.getTitle()
                                             : "<unnamed>" )
                         + " and " 
                         + ( ndx2.hasTitle() ? ndx2.getTitle()
                                             : "<unnamed>" ) );

            ndxOut.consume( ndx3 );
        }
    }
}
