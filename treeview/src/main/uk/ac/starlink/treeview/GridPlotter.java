package uk.ac.starlink.treeview;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
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
        final ScalingPlot plotPan = new ScalingPlot( wcs, lower, upper );
        add( plotPan, BorderLayout.CENTER );

        /* Construct a current-frame selector. */
        ButtonGroup bgrp = new ButtonGroup();
        ActionListener buttwatch = new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
               int ifrm = Integer.parseInt( evt.getActionCommand() );
               plotPan.setCurrent( ifrm );
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

    private static class ScalingPlot extends JPanel {

        private static int lgap = 70;
        private static int rgap = 30;
        private static int tgap = 40;
        private static int bgap = 60;

        private final FrameSet wcs;
        private final double[] lower;
        private final double[] upper;
        private final double[] bbox;
        private final double gridAspect;
        private Plot plot;
        private Dimension lastSize;
        private int currentFrame;

        public ScalingPlot( FrameSet wcs, double[] lower, double[] upper ) {
            if ( lower.length != 2 || upper.length != 2 ||
                 wcs.getFrame( FrameSet.AST__BASE ).getNaxes() != 2 ) {
                throw new IllegalArgumentException( "Not a 2d WCS" );
            }
            this.wcs = wcs;
            this.lower = lower;
            this.upper = upper;
            bbox = new double[] { lower[ 0 ], lower[ 1 ], 
                                  upper[ 0 ], upper[ 1 ] };
            gridAspect = ( upper[ 1 ] - lower[ 1 ] )
                       / ( upper[ 0 ] - lower[ 0 ] );
            currentFrame = wcs.getCurrent();
            setBorder( BorderFactory.createBevelBorder( BevelBorder.RAISED ) );
        }

        public void setCurrent( int ifrm ) {
            currentFrame = ifrm;
            if ( plot != null ) {
                plot.setCurrent( ifrm );
                plot.clear();
                plot.grid();
                repaint();
            }
        }

        protected void paintComponent( Graphics g ) {
            super.paintComponent( g );
            Dimension size = getSize();
            if ( size.width < lgap + rgap + 150 ) {
                size.width = lgap + rgap + 150;
            }
            if ( size.height < tgap + bgap + 150 ) {
                size.height = tgap + bgap + 150;
            }
            if ( ! size.equals( lastSize ) ) {
                lastSize = size;
                reconfigurePlot( size );
            }
            plot.paint( g );
        }

        private void reconfigurePlot( Dimension size ) {
            Dimension plotWindow = new Dimension( size );
            plotWindow.width -= ( lgap + rgap );
            plotWindow.height -= ( tgap + bgap );
            double windowAspect = 
                (double) plotWindow.height / (double) plotWindow.width;
            Dimension plotSize = new Dimension();
            if ( gridAspect < windowAspect ) {
                plotSize.width = plotWindow.width;
                plotSize.height = 
                    (int) Math.round( plotSize.width * gridAspect );
            }
            else {
                plotSize.height = plotWindow.height;
                plotSize.width =
                    (int) Math.round( plotSize.height / gridAspect );
            }
            Rectangle position = new Rectangle( plotSize );
            position.x = lgap + 
                ( size.width - ( lgap + plotSize.width + rgap ) ) / 2;
            position.y = tgap +
                ( size.height - ( tgap + plotSize.height + bgap ) ) / 2;
            plot = new Plot( wcs, position, bbox );
            plot.setGrid( true );
            plot.setCurrent( currentFrame );
            plot.grid();
            setPreferredSize( plotSize );
        }
    }
}
