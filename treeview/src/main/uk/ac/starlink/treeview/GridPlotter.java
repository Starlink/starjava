package uk.ac.starlink.treeview;

import java.awt.BorderLayout;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.Plot;
import uk.ac.starlink.datanode.viewers.TreeviewLAF;

/**
 * A Component which displays a coordinate grid and frame selection panel
 * for a given FrameSet and base frame bounds. 
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class GridPlotter extends JPanel {

    private int currentFrame;

    /**
     * Constructs a GridPlotter.
     *
     * @param  size  maximum linear dimension (larger of width and height) 
     *               of the plotting surface.  The Component itself will
     *               be larger because it will contain axis annotations
     * @param  shape  the shape of the base frame of the supplied FrameSet
     * @param  wcs    a FrameSet from which the Plot will be constructed
     */
    GridPlotter( NDShape baseShape, final FrameSet wcs ) {
        super( new BorderLayout() );
        currentFrame = wcs.getCurrent();

        /* Get upper and lower limits on the actual plotting surface. */
        long[] dims = baseShape.getDims();
        int ndim = baseShape.getNumDims();
        double[] lower = new double[ ndim ];
        double[] upper = new double[ ndim ];
        for ( int i = 0; i < ndim; i++ ) {
            lower[ i ] = 0.5;
            upper[ i ] = 0.5 + dims[ i ];
        }

        /* Construct a panel to hold the plot display itself. */
        final ScalingPlot plotPan = new ScalingPlot( wcs, lower, upper ) {
            protected void configurePlot( Plot plot ) {
                plot.setCurrent( currentFrame + 1 );
                super.configurePlot( plot );
            }
        };
        TreeviewLAF.configureMainPanel( plotPan );
        add( plotPan, BorderLayout.CENTER );

        /* Construct a current-frame selector. */
        int nfrm = wcs.getNframe();
        String[] frameNames = new String[ nfrm ];
        for ( int i = 0; i < nfrm; i++ ) {
            frameNames[ i ] = ( i + 1 ) + ": " 
                            + wcs.getFrame( i + 1 ).getDomain();
        }
        final JComboBox selecter = new JComboBox( frameNames );
        selecter.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                if ( evt.getStateChange() == ItemEvent.SELECTED ) {
                    currentFrame = selecter.getSelectedIndex() + 1;
                    plotPan.refreshPlot();
                }
            }
        } );
        selecter.setSelectedIndex( currentFrame - 1 );
        selecter.setMaximumSize( selecter.getPreferredSize() );
        
        /* Put the selector in a box. */
        Box controlBox = new Box( BoxLayout.X_AXIS );
        controlBox.add( new JLabel( "Plotted co-ordinate frame: " ) );
        controlBox.add( selecter );
        controlBox.add( Box.createGlue() );
        TreeviewLAF.configureControlPanel( controlBox );

        /* Display the box in this panel. */
        add( controlBox, BorderLayout.NORTH );
    }

}
