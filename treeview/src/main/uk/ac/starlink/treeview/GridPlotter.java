package uk.ac.starlink.treeview;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.BevelBorder;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.Plot;

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

        /* Get upper and lower limits on the actual plotting surface. */
        int ndim = baseShape.getNumDims();
        long[] lbnds = baseShape.getOrigin();
        long[] ubnds = baseShape.getUpperBounds();
        double[] lower = new double[ ndim ];
        double[] upper = new double[ ndim ];
        for ( int i = 0; i < ndim; i++ ) {
            lower[ i ] = (double) lbnds[ i ] - 0.5;
            upper[ i ] = (double) ubnds[ i ] + 0.5;
        }

        /* Construct a panel to hold the plot display itself. */
        final ScalingPlot plotPan = new ScalingPlot( wcs, lower, upper ) {
            protected void configurePlot( Plot plot ) {
                plot.setCurrent( currentFrame );
                super.configurePlot( plot );
            }
        };
        plotPan.setBorder( BorderFactory.createLineBorder( Color.BLACK ) );
        add( plotPan, BorderLayout.CENTER );

        /* Construct a current-frame selector. */
        ButtonGroup bgrp = new ButtonGroup();
        ActionListener buttwatch = new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
               currentFrame = Integer.parseInt( evt.getActionCommand() );
               plotPan.refreshPlot();
            }
        };
        int nfrm = wcs.getNframe();
        int curfrm = wcs.getCurrent();
        Box buttPan = new Box( BoxLayout.Y_AXIS );
        for ( int i = 0; i < nfrm; i++ ) {
            String text = ( i + 1 ) + ": " + wcs.getFrame( i + 1 ).getDomain();
            JRadioButton butt = new JRadioButton( text );
            butt.setActionCommand( Integer.toString( i + 2 ) );
            butt.addActionListener( buttwatch );
            bgrp.add( butt );
            buttPan.add( butt );
            if ( i + 1 == curfrm ) {
                butt.doClick();
            }
        }
        add( buttPan, BorderLayout.NORTH );

        /* Reveal. */
        setVisible( true );
    }

}
