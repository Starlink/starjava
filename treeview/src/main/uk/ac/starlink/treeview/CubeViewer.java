package uk.ac.starlink.treeview;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.CardLayout;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.array.BridgeNDArray;
import uk.ac.starlink.array.MouldArrayImpl;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.OrderedNDShape;
import uk.ac.starlink.array.WindowArrayImpl;
import uk.ac.starlink.ast.FrameSet;

/** 
 * Displays the pixels of a N-d array.  One plane is shown at a time,
 * with a slider to select which plane you want to see.
 */
class CubeViewer implements ComponentMaker2 {
 
    private JPanel imageHolder;
    private CardLayout flipper;
    private Set planeSet;
    private JComponent[] components;
    private NDArray nda;
    private FrameSet wcs;
    

    /**
     * Construct a cube viewer.
     * 
     * @param  nda  the array data to view
     * @param  wcs  the associated WCS, may be null
     */
    public CubeViewer( NDArray nda, FrameSet wcs ) {
        this.nda = nda;
        this.wcs = wcs;

        /* Set up a hash of the images we have constructed. */
        planeSet = new HashSet();

        /* Get a shape of which each pixel represents one of the possible
         * 2-dimensional slices of our NDArray.  It's a copy of the original
         * shape, but the dummy dimensions are represented by bounds
         * -Long.MIN_VALUE:-Long.MIN_VALUE. */
        OrderedNDShape shape = nda.getShape();
        long[] depthOrigin = shape.getOrigin();
        long[] depthDims = shape.getDims();
        depthOrigin[ 0 ] = depthOrigin[ 1 ] = -Long.MIN_VALUE;
        depthDims[ 0 ] = depthDims[ 1 ] = 1;
        final OrderedNDShape depthShape =
            new OrderedNDShape( depthOrigin, depthDims, shape.getOrder() );
        int nplanes = (int) depthShape.getNumPixels();

        /* Set up the controls. */
        final JSlider slider = new JSlider( 0, nplanes - 1 );
        final JLabel planeLabel = new JLabel();
        slider.addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent e ) {
                int value = slider.getValue();
                long[] spec = depthShape.offsetToPosition( (long) value );
                planeLabel.setText( makeName( spec ) + "   " );
                if ( ! slider.getValueIsAdjusting() ) {
                    displayPlane( spec );
                }
            }
        } );

        /* Arrange the components in the window. */
        JPanel sliderHolder = new JPanel( new BorderLayout( 5, 5 ) );
        sliderHolder.add( slider, BorderLayout.CENTER );
        sliderHolder.add( planeLabel, BorderLayout.EAST );
        sliderHolder.add( new JLabel( "  " ), BorderLayout.WEST );
        Border baseBorder = BorderFactory.createLineBorder( Color.black );
        Border border = BorderFactory
                       .createTitledBorder( baseBorder, "Slice selector" );
        sliderHolder.setBorder( border );
        flipper = new CardLayout( 10, 10 );
        imageHolder = new JPanel( flipper );

        /* Record them for later use. */
        components = new JComponent[] { sliderHolder, imageHolder };

        /* Message the slider to generate the first image. */
        slider.setValue( 0 );
    }


    public JComponent[] getComponents() {
        return components;
    }

    /**
     * Display the slice specified by the given slice specification,
     * and cache it or whatever for future use.
     *
     * @param  spec  slice specification (see getSlice)
     */
    private void displayPlane( long[] spec ) {
        String name = makeName( spec );
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

        /* Get the N-dimensional window which reveals the requested slice. */
        long[] windowOrigin = shape.getOrigin();
        long[] windowDims = shape.getDims();
        int nok = ndim;
        for ( int i = 0; i < ndim; i++ ) {
            if ( spec[ i ] == Long.MIN_VALUE ) {
                windowOrigin[ i ] = spec[ i ];
                windowDims[ i ] = 1;
                nok--;
            }
        }

        /* Get the reduced-dimensional shape which has no dummy dimensions. */
        long[] sliceOrigin = new long[ nok ];
        long[] sliceDims = new long[ nok ];
        int iok = 0;
        for ( int i = 0; i < ndim; i++ ) {
            if ( spec[ i ] != Long.MIN_VALUE ) {
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
