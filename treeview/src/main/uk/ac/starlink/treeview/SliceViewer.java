package uk.ac.starlink.treeview;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.array.BridgeNDArray;
import uk.ac.starlink.array.MouldArrayImpl;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Order;
import uk.ac.starlink.array.OrderedNDShape;
import uk.ac.starlink.array.WindowArrayImpl;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.datanode.viewers.TreeviewLAF;

public class SliceViewer extends JPanel {

    private NDArray nda;
    private FrameSet wcs;
    private Order order;
    private OrderedNDShape depthShape;
    private JComboBox orienter = new JComboBox();
    private JSlider slider = new JSlider();
    private JLabel planeLabel = new JLabel();
    private Set planeSet = new HashSet();
    private CardLayout flipper = new CardLayout();
    private JPanel imageHolder = new JPanel( flipper );
    
    public SliceViewer( NDArray nda, FrameSet wcs ) {
        super( new BorderLayout() );
        this.nda = nda;
        this.wcs = wcs;
        this.order = nda.getShape().getOrder();

        /* Set up a box for basic viewer controls. */
        Box controlBox = new Box( BoxLayout.Y_AXIS );

        /* Set up a slice selection slider. */
        slider.addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                updateSliderPosition();
            }
        } );

        /* Set up a slice orientation selector.  This contains one entry
         * for each pair of non-degenerate dimensions over which slices
         * can be taken. */
        OrderedNDShape shape = nda.getShape();
        int ndim = shape.getNumDims();
        long[] dims = shape.getDims();
        long[] origin = shape.getOrigin();
        for ( int i = 0; i < ndim; i++ ) {
            for ( int j = i + 1; j < ndim; j++ ) {
                if ( dims[ i ] > 1 && dims[ j ] > 1 ) {
                    long[] depthOrigin = (long[]) origin.clone();
                    long[] depthDims = (long[]) dims.clone();
                    depthOrigin[ i ] = -Long.MIN_VALUE;
                    depthOrigin[ j ] = -Long.MIN_VALUE;
                    depthDims[ i ] = 1;
                    depthDims[ j ] = 1;
                    NDShape dshape = new NDShape( depthOrigin, depthDims );
                    orienter.addItem( dshape );
                }
            }
        }
        orienter.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                if ( evt.getStateChange() == ItemEvent.SELECTED ) {
                    updateOrientation();
                }
            }
        } );
        int norient = orienter.getItemCount();
        if ( norient < 1 ) {
            throw new IllegalArgumentException(
                "Too few non-dummy dimensions " + shape );
        }

        /* Place the viewer. */
        add( imageHolder, BorderLayout.CENTER );

        /* Place the orientation selector in the control box. */
        Box orientBox = new Box( BoxLayout.X_AXIS );
        orientBox.add( new JLabel( "Orientation selector: " ) );
        orientBox.add( orienter );
        orientBox.add( Box.createGlue() );
        controlBox.add( orientBox );

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

        /* Message the orientation control to generate the first image. */
        orienter.setSelectedIndex( 0 );
        updateOrientation();
    }

    /**
     * Updates the display for a new slider position.
     */
    private void updateSliderPosition() {
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
                    ImageViewer iv = new ImageViewer( slice, null, false );
                    imageHolder.add( iv, name );
                }
                catch ( IOException e ) {
                    e.printStackTrace();
                }
            }
            flipper.show( imageHolder, name );
        }
    }

    /**
     * Updates the display for a new slice orientation selection.
     */
    private void updateOrientation() {
        NDShape dshape = (NDShape) orienter.getSelectedItem();
        if ( ! dshape.equals( depthShape ) ) {
            depthShape = new OrderedNDShape( dshape, order );
            slider.setMinimum( 0 );
            slider.setMaximum( (int) (depthShape.getNumPixels() - 1 ) );
            slider.setValue( 0 );
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
