package uk.ac.starlink.treeview;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jsky.util.gui.VRangeSlider;
import uk.ac.starlink.array.ArrayImpl;
import uk.ac.starlink.array.BridgeNDArray;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.OrderedNDShape;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.datanode.viewers.TreeviewLAF;

/**
 * Views a 3-dimensional array by collapsing it along one axis.
 * Widgets are provided to select the axis along which it is collapsed 
 * and to determine what range of that axis it is collapsed over.
 *
 * @author   Mark Taylor (Starlink)
 */
public class CollapseViewer extends JPanel {

    private NDArray nda;
    private JComponent image;
    private OrderedNDShape shape;
    private long[] origin;
    private long[] dims;
    private int collAxis = -1;
    private long collOrigin;
    private long collDim;
    private JComboBox selecter;
    private VRangeSlider rangeSlider;
    private JLabel rangeLabel = new JLabel();

    /**
     * Constructs a new viewer from an (effectively) three-dimensional 
     * NDArray.
     *
     * @param  nda  the base NDArray
     * @param  wcs  the WCS frameset (not currently used)
     */
    public CollapseViewer( final NDArray nda, FrameSet wcs )
            throws IOException {
        super( new BorderLayout() );
        this.nda = nda;
        this.shape = nda.getShape();
        this.origin = shape.getOrigin();
        this.dims = shape.getDims();
        int ndim = shape.getNumDims();

        /* Set up a box for viewer controls. */
        Box controlBox = new Box( BoxLayout.Y_AXIS );

        /* Configure a control for selecting which axis to collapse along. */
        String[] axisNames = new String[ ndim ];
        for ( int i = 0; i < ndim; i++ ) {
            axisNames[ i ] = "Axis " + i + ": " + shape.getOrigin()[ i ] 
                           + "+" + shape.getDims()[ i ];
        }
        selecter = new JComboBox( axisNames );
        selecter.setSelectedIndex( ndim - 1 );
        selecter.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                if ( evt.getStateChange() == ItemEvent.SELECTED ) {
                    updateDisplay();
                }
            }
        } );

        /* Put the selector control in a box. */
        JComponent selecterBox = new Box( BoxLayout.X_AXIS );
        selecterBox.add( new JLabel( "Axis to collapse: " ) );
        selecterBox.add( selecter );
        selecterBox.add( Box.createGlue() );
        TreeviewLAF.configureControl( selecterBox );

        /* Configure a control for selecting collapse range. */
        rangeSlider = new VRangeSlider( null, 0.0, 1.0 );
        rangeSlider.setPreferredSize( new Dimension( 50, 38 ) );
        rangeSlider.setDrawLabels( false );
        rangeSlider.addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                updateDisplay();
            }
        } );

        /* Put the range slider in a box. */
        JComponent rangeBox = new Box( BoxLayout.X_AXIS );
        rangeBox.add( new JLabel( "Range to average over: " ) );
        rangeBox.add( rangeSlider );
        rangeBox.add( rangeLabel );
        
        /* Place the controls in the control box. */
        controlBox.add( selecterBox );
        controlBox.add( rangeBox );

        /* Place the control panel (unless it's empty). */
        if ( controlBox.getComponentCount() > 0 ) {
            TreeviewLAF.configureControlPanel( controlBox );
            add( controlBox, BorderLayout.NORTH );
        }

        /* Draw the initial state. */
        updateDisplay();
    }

    /**
     * Method called when the controls have been changed so that a new
     * drawing may be required.
     */
    private void updateDisplay() {

        /* If the collapse axis has changed, reset the range slider, 
         * which will cause another call of this update method in such
         * a way as to force a redraw. */
        int ca = selecter.getSelectedIndex();
        if ( ca != collAxis ) {
            collAxis = ca;
            collDim = -1; // force a redraw
            double lo = origin[ collAxis ] - 0.5;
            double hi = lo + dims[ collAxis ];
            rangeSlider.setBounds( lo, hi );
            rangeSlider.setValues( lo, hi );
        }

        /* Get the values of the range slider, rounding and sanity checking. */
        double[] range = rangeSlider.getMinMaxValues();
        long co = Math.round( range[ 0 ] + 0.5 );
        co = Math.max( co, origin[ collAxis ] );
        co = Math.min( co, origin[ collAxis ] + dims[ collAxis ] - 1 );
        long cd = Math.round( range[ 1 ] - range[ 0 ] );
        cd = Math.max( cd, 1L );
        cd = Math.min( cd, dims[ collAxis ] - ( co - origin[ collAxis ] ) );
        rangeSlider.setValues( -0.5 + (double) co,
                               -0.5 + (double) ( co + cd ) );

        /* If they have changed, redraw. */
        if ( co != collOrigin || cd != collDim ) {
            collOrigin = co;
            collDim = cd;
        }
        rangeLabel.setText( "  " + collOrigin + "+" + collDim + "   " );
        ArrayImpl aimpl = 
            new CollapseArrayImpl( nda, collAxis, collOrigin, collDim );
        try {
            setArray( new BridgeNDArray( aimpl ), null );
        }
        catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     * Resets the array to be drawn.
     */
    private void setArray( NDArray nda, FrameSet wcs ) throws IOException {
        if ( image != null ) {
            remove( image );
        }
        image = new ImageViewer( nda, wcs );
        add( image, BorderLayout.CENTER );
    }
}
