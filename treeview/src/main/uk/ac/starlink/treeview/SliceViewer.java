package uk.ac.starlink.treeview;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.array.BridgeNDArray;
import uk.ac.starlink.array.MouldArrayImpl;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.OrderedNDShape;
import uk.ac.starlink.array.WindowArrayImpl;
import uk.ac.starlink.ast.FrameSet;

public class SliceViewer extends JPanel {
    
    public SliceViewer( final NDArray nda, final FrameSet wcs ) {
        super( new BorderLayout() );

        /* Set up a box for basic viewer controls. */
        Box controlBox = new Box( BoxLayout.Y_AXIS );

        /* Get a shape of which each pixel represents one of the possible
         * 2-dimensional slices of our NDArray.  It's a copy of the original
         * shape, except the dummy dimensions are represented by bounds
         * -Long.MIN_VALUE:-Long.MIN_VALUE. */
        OrderedNDShape shape = nda.getShape();
        long[] depthOrigin = shape.getOrigin();
        long[] depthDims = shape.getDims();
        int ndim = shape.getNumDims();
        int ndummy = 2;
        for ( int i = 0; i < ndim; i++ ) {
            if ( depthDims[ i ] > 1 && ndummy > 0 ) {
                depthOrigin[ i ] = -Long.MIN_VALUE;
                depthDims[ i ] = 1;
                ndummy--;
            }
        }
        if ( ndummy != 0 ) {
            throw new IllegalArgumentException(
                "Too few non-dummy dimensions " + shape );
        }
        final OrderedNDShape depthShape =
            new OrderedNDShape( depthOrigin, depthDims, shape.getOrder() );
        int nplanes = (int) depthShape.getNumPixels();

        /* Create and place the main viewer. */
        final CardLayout flipper = new CardLayout();
        final JPanel imageHolder = new JPanel( flipper );

        /* Set up a slice selection slider. */
        final JSlider slider = new JSlider( 0, nplanes - 1 );
        final JLabel planeLabel = new JLabel();
        final Set planeSet = new HashSet();
        slider.addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                int value = slider.getValue();
                long[] spec = depthShape.offsetToPosition( (long) value );
                String name = makeName( spec );
                planeLabel.setText( name + "   " );
                if ( ! slider.getValueIsAdjusting() ) {

                    /* Display the plane. */
                    if ( ! planeSet.contains( name ) ) {
                        try {
                            planeSet.add( name );
                            NDArray slice = getSlice( nda, spec );
                            assert slice.getShape().getNumDims() == 2;
                            ImageViewer iv = new ImageViewer( slice, wcs );
                            imageHolder.add( iv, name );
                        }
                        catch ( IOException e ) {
                            e.printStackTrace();
                        }
                    }
                    flipper.show( imageHolder, name );
                }
            }
        } );

        /* Message the slider to generate the first image. */
        slider.setValue( 0 );

        /* Place the viewer. */
        add( imageHolder, BorderLayout.CENTER );

        /* Place the slider in the control box. */
        Box sliderBox = new Box( BoxLayout.X_AXIS );
        sliderBox.add( new JLabel( "Slice selector: " ) );
        sliderBox.add( slider );
        sliderBox.add( new JLabel( "    " ) );
        sliderBox.add( planeLabel );
        controlBox.add( sliderBox );

        /* Place the control panel. */
        if ( controlBox.getComponentCount() > 0 ) {
            TreeviewLAF.configureControlPanel( controlBox );
            add( controlBox, BorderLayout.NORTH );
        }
    }


    /**
     * Return a given slice of an NDArray.
     * The slice is specified by a long array of the same dimensionality
     * as the original array.  A dimension corresponding to any element
     * with the value Long.MIN_VALUE will exist in the output slice NDArray,
     * but dimensions corresponding to other elements will be sampled
     * at the position given by the value of that element.
     *
     * @param   spec   the slice specification
     * @param   nda    the NDArray to carve up
     * @return  a slice NDArray representing a subset of the original
     */
    private static NDArray getSlice( NDArray nda, long[] spec )
            throws IOException {
        int ndim = spec.length;
        NDShape shape = nda.getShape();
        long[] origin = shape.getOrigin();
        long[] dims = shape.getDims();

        /* Get the N-dimensional window which reveals the requested slice. */
        long[] windowOrigin = shape.getOrigin();
        long[] windowDims = shape.getDims();
        int nok = 0;
        for ( int i = 0; i < ndim; i++ ) {
            if ( spec[ i ] == Long.MIN_VALUE ) {
                windowOrigin[ i ] = origin[ i ];
                windowDims[ i ] = dims[ i ];
                nok++;
            }
            else {
                windowOrigin[ i ] = spec[ i ];
                windowDims[ i ] = 1;
            }
        }

        /* Get the reduced-dimensional shape which has no dummy dimensions. */
        long[] sliceOrigin = new long[ nok ];
        long[] sliceDims = new long[ nok ];
        int iok = 0;
        for ( int i = 0; i < ndim; i++ ) {
            if ( spec[ i ] == Long.MIN_VALUE ) {
                sliceOrigin[ iok ] = windowOrigin[ i ];
                sliceDims[ iok ] = windowDims[ i ];
                iok++;
            }
        }
        assert iok == nok;
        NDShape sliceShape = new NDShape( sliceOrigin, sliceDims );

        /* View the original array through the window. */
        NDShape window = new NDShape( windowOrigin, windowDims );
        NDArray winarray =
            new BridgeNDArray( new WindowArrayImpl( nda, window ) );

        /* And view that with duff dimensions removed. */
        NDArray slicearray =
            new BridgeNDArray( new MouldArrayImpl( winarray, sliceShape ) );
        return slicearray;
    }


    private String makeName( long[] spec ) {
        return NDShape.toString( spec );
    }

}
