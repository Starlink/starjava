package uk.ac.starlink.treeview;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
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

    /**
     * Constructs a GridPlotter.
     *
     * @param  size  maximum linear dimension (larger of width and height) 
     *               of the plotting surface.  The Component itself will
     *               be larger because it will contain axis annotations
     * @param  shape  the shape of the base frame of the supplied FrameSet
     * @param  wcs    a FrameSet from which the Plot will be constructed
     */
    GridPlotter( int size, NDShape baseShape, FrameSet wcs ) {
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

        /* Check that this frameset is suitable to plot in 2 dimensions. */
        if ( lower.length == 2 && upper.length == 2 &&
            wcs.getFrame( FrameSet.AST__BASE ).getNaxes() == 2 ) {

            /* Set the shape of the plot. */
            int lgap = 70;
            int rgap = 20;
            int tgap = 40;
            int bgap = 50;
            double aspect = ( upper[ 1 ] - lower[ 1 ] ) 
                          / ( upper[ 0 ] - lower[ 0 ] );
            Dimension paneldim = ( aspect < 1.0 ) 
               ? new Dimension( lgap + rgap + size,
                                bgap + tgap + (int) ( size * aspect ) )
               : new Dimension( lgap + rgap + (int) ( size / aspect ),
                                bgap + tgap + size );
         
            /* Construct a Plot to put in the window. */
            double bbox[] = new double[] { lower[ 0 ], lower[ 1 ],
                                           upper[ 0 ], upper[ 1 ] };
            final Plot plot = new Plot( wcs, new Rectangle( paneldim ), bbox, 
                                        lgap, rgap, bgap, tgap );
            plot.setGrid( true );

            /* Construct a panel to hold the plot display itself. */
            final JPanel plotPan = new JPanel() {
                protected void paintComponent( Graphics g ) {
                    super.paintComponent( g );
                    plot.paint( g );
                }
            };
            plotPan.setPreferredSize( paneldim );
            add( plotPan, BorderLayout.CENTER );

            /* Construct a current-frame selector. */
            ButtonGroup bgrp = new ButtonGroup();
            ActionListener buttwatch = new ActionListener() {
                public void actionPerformed( ActionEvent evt ) {
                   int ifrm = Integer.parseInt( evt.getActionCommand() );
                   plot.setCurrent( ifrm );
                   plot.clear();
                   plot.grid();
                   plotPan.repaint();
                }
            };
            int nfrm = wcs.getNframe();
            int curfrm = wcs.getCurrent();
            Box buttPan = new Box( BoxLayout.Y_AXIS );
            for ( int i = 0; i < nfrm; i++ ) {
                String text = ( i + 1 ) + ": " 
                            + plot.getFrame( i + 2 ).getDomain();
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
}
