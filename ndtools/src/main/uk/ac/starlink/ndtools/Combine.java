package uk.ac.starlink.ndtools;

import java.io.IOException;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.ArrayImpl;
import uk.ac.starlink.array.BadHandler;
import uk.ac.starlink.array.BridgeNDArray;
import uk.ac.starlink.array.CombineArrayImpl;
import uk.ac.starlink.array.Combiner;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDArrays;
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
 * Task for generic combination of two NDXs to produce a third.
 */
class Combine implements Task {

    private String description;
    private Combiner icombi;
    private Combiner vcombi;

    private ExistingNdxParameter ndx1par;
    private ExistingNdxParameter ndx2par;
    private NewNdxParameter ndx3par;

    public Parameter[] getParameters() {
        return new Parameter[] { ndx1par, ndx2par, ndx3par };
    }

    /**
     * Create a new combine task.  Its behaviour is defined by two
     * combiners which define how the image arrays are combined and
     * how the variance arrays are combined.  This does not allow
     * for combinations in which the image pixels affect the variance
     * pixels or vice versa.
     *
     * @param  description  short description of the operation represented
     *         by this Combine object
     * @param  icombi  a Combiner which handles image component combination
     * @param  vcombi  a Combiner which handles variance component combination
     */
    public Combine( String description, Combiner icombi, Combiner vcombi ) {
        this.description = description;
        this.icombi = icombi;
        this.vcombi = vcombi;

        ndx1par = new ExistingNdxParameter( "ndx1" );
        ndx1par.setPrompt( "First input NDX" );
        ndx1par.setPosition( 1 );

        ndx2par = new ExistingNdxParameter( "ndx2" );
        ndx2par.setPrompt( "Second input NDX" );
        ndx2par.setPosition( 2 );
 
        ndx3par = new NewNdxParameter( "ndxout" );
        ndx3par.setPrompt( "Output NDX" );
        ndx3par.setPosition( 3 );
    }

    public String getPurpose() {
        return description + " combination of two NDXs.";
    }

    public Executable createExecutable( Environment env ) throws TaskException {
        return new CombineExecutable( ndx1par.ndxValue( env ),
                                      ndx2par.ndxValue( env ),
                                      ndx3par.ndxConsumerValue( env ) );
    }

    /**
     * Helper class which does the work of a combination.
     */
    private class CombineExecutable implements Executable {

        final Ndx ndx1;
        final Ndx ndx2;
        final NdxConsumer ndxOut;

        public CombineExecutable( Ndx ndx1, Ndx ndx2, NdxConsumer ndxOut ) {
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

            im1 = NDArrays.toRequiredArray( im1, req );
            im2 = NDArrays.toRequiredArray( im2, req );
            NDArray im3 = new BridgeNDArray(
                new CombineArrayImpl( im1, im2, icombi, shape, type, bh ) );
            NDArray var3;
            if ( ndx1.hasVariance() && ndx2.hasVariance() ) {
                NDArray var1 = NDArrays.toRequiredArray( ndx1.getVariance(),
                                                         req );
                NDArray var2 = NDArrays.toRequiredArray( ndx2.getVariance(),
                                                         req );
                var3 = new BridgeNDArray(
                    new CombineArrayImpl( var1, var2, vcombi,
                                          shape, type, bh ) );
            }
            else {
                var3 = null;
            }

            MutableNdx ndx3 = new DefaultMutableNdx( im3 );
            ndx3.setVariance( var3 );
            ndx3.setTitle( description + " of " 
                         + ( ndx1.hasTitle() ? ndx1.getTitle()
                                             : "<unnamed>" )
                         + " and " 
                         + ( ndx2.hasTitle() ? ndx2.getTitle()
                                             : "<unnamed>" ) );

            ndxOut.consume( ndx3 );
        }
    }
}
