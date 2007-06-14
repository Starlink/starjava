package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.Icon;
import javax.swing.JComponent;

/**
 * Displays a legend for an auxiliary (colour) axis.
 * 
 * @author   Mark Taylor
 * @since    14 Jun 2007
 */
public class AuxLegend extends JComponent {

    private final int iconHeight_;
    private final int widthPad_;
    private Shader shader_;
    private int prefHeight_;
    private AxisLabeller labeller_;
    private int lastWidth_;

    /**
     * Constructor.
     *
     * @param   iconHeight  preferred transverse size of the legend colour band
     * @param   widthPad   number of pixels at each end of the legend
     *          colour band to serve as padding
     */
    public AuxLegend( int iconHeight, int widthPad ) {
        iconHeight_ = iconHeight;
        widthPad_ = widthPad;
    }

    /**
     * Configures this legend according to a given plot state.
     *
     * @param   state  plot state
     * @param   iaux   index of auxiliary axis to use
     */
    public void configure( PlotState state, int iaux ) {
        shader_ = state.getShaders().length > iaux
               ? state.getShaders()[ iaux ]
               : null;
        if ( shader_ != null ) {
            int idim = state.getMainNdim() + iaux;
            boolean logFlag = state.getLogFlags()[ idim ];
            boolean flipFlag = state.getFlipFlags()[ idim ];
            double lo = state.getRanges()[ idim ][ 0 ];
            double hi = state.getRanges()[ idim ][ 1 ];
            String label = state.getAxisLabels()[ idim ];
            labeller_ =
                new AxisLabeller( label, lo, hi, 200, logFlag, flipFlag, 
                                  getGraphics().getFontMetrics(),
                                  AxisLabeller.X, 6, widthPad_, widthPad_ );
        }
        else {
            labeller_ = null;
        }
        fitToSize();
        revalidate();
        repaint();
    }

    /**
     * Configures the internal arrangement of this legend according to
     * the current size of this component.  Should be called prior to
     * drawing and possibly before validation.
     */
    private void fitToSize() {
        if ( labeller_ != null ) {
            Insets insets = getInsets();
            labeller_.setNpix( getWidth() - insets.left - insets.right 
                                          - widthPad_ * 2 );
            prefHeight_ = iconHeight_ + labeller_.getAnnotationHeight();
        }
        else {
            prefHeight_ = 0;
        }
    }

    protected void paintComponent( Graphics g ) {

        /* JComponent boilerplate. */
        if ( isOpaque() ) {
            Color col = g.getColor();
            g.setColor( getBackground() );
            g.fillRect( 0, 0, getWidth(), getHeight() );
            g.setColor( col );
        }

        /* Draw the bar and annotations if not blank. */
        if ( labeller_ != null ) {
            fitToSize();

            /* Work out geometry. */
            Insets insets = getInsets();
            int txtHeight = labeller_.getAnnotationHeight();
            int xoff = insets.left + widthPad_;
            int yoff = insets.top;
            int xdim = labeller_.getNpix();
            int ydim = getHeight() - insets.top - insets.bottom - txtHeight;

            /* Draw the colour bar itself. */
            Icon icon = Shaders.create1dIcon( shader_, true, Color.RED,
                                              xdim, ydim, 0, 0 );
            icon.paintIcon( this, g, xoff, yoff );

            /* Draw the numerical annotations. */
            yoff += ydim;
            g.translate( xoff, yoff );
            labeller_.annotateAxis( g );
            g.translate( - xoff, - yoff );
        }
    }

    public Dimension getPreferredSize() {
        return new Dimension( 200, prefHeight_ );
    }

    public Dimension getMaximumSize() {
        return new Dimension( Integer.MAX_VALUE, prefHeight_ );
    }

    public Dimension getMinimumSize() {
        return new Dimension( 20 + getInsets().left + getInsets().right,
                              prefHeight_ );
    }
}
