package uk.ac.starlink.treeview;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.CardLayout;
import java.nio.Buffer;
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
import uk.ac.starlink.ast.FrameSet;

/** 
 * Displays the pixels of a N-d array.  One plane is shown at a time,
 * with a slider to select which plane you want to see.
 */
class CubeViewer implements ComponentMaker2 {
 
    private long[] planeOrigin;
    private int planeSize;
    private Cartesian planeShape;
    private Cartesian depthShape;
    private JLabel planeLabel;
    private JPanel imageHolder;
    private CardLayout flipper;
    private Set planeSet;
    private Buffer niobuf;
    private FrameSet wcs;
    private JComponent[] components;
    

    /**
     * Construct a cube viewer.
     *
     * @param  niobuf  an NIO buffer containing numeric data in column-major
     *                 order, starting with the first slice
     * @param  shape   the N-dimensional shape of the cube (N&gt;2)
     * @param  origin  the N-dimensional origin of the cube
     * @param  wcs     the WCS FrameSet for each slice, if known.  Its base
     *                 frame is a 2-d grid-like frame, and its current frame
     *                 is a 2-d frame for axis display.  May be null
     */
    public CubeViewer( Buffer niobuf, Cartesian shape, long[] origin,
                       FrameSet wcs ) {
        planeSet = new HashSet();
        this.niobuf = niobuf;
        this.wcs = wcs;

        int ndim = shape.getNdim();
        planeOrigin = new long[] { origin[ 0 ], origin[ 1 ] };
        planeShape = new Cartesian( new long[] { shape.getCoord( 0 ),
                                                 shape.getCoord( 1 ) } );
        planeSize = (int) planeShape.numCells();
        depthShape = new Cartesian( ndim - 2 );
        for ( int i = 2; i < ndim; i++ ) {
            depthShape.setCoord( i - 2, shape.getCoord( i ) );
        }

        int nplanes = (int) depthShape.numCells();

        final JSlider slider = new JSlider( 0, nplanes - 1 );
        slider.addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent e ) {
                int value = slider.getValue();
                echoValue( value );
                if ( ! slider.getValueIsAdjusting() ) {
                    displayPlane( value );
                }
            }
        } );

        planeLabel = new JLabel();
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

        components = new JComponent[] { sliderHolder, imageHolder };

        slider.setValue( 0 );
    }

    public JComponent[] getComponents() {
        return components;
    }

    private void echoValue( int value ) {
        planeLabel.setText( makeName( value ) + "  " );
    }

    private void displayPlane( int value ) {
        String name = makeName( value );
        if ( ! planeSet.contains( name ) ) {
            planeSet.add( name );
            int start = value * planeSize;
            ImageViewer iv = new ImageViewer( niobuf, planeShape, planeOrigin,
                                              wcs, start, planeSize );
            imageHolder.add( iv, name );
        }
        flipper.show( imageHolder, name );
    }

    private String makeName( int value ) {
        long[] dp = depthShape.offsetToPos( (long) value );
        String ds = Cartesian.toString( dp );
        return "( *, *," + ds.substring( 1 );
    }
}
