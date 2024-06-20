package uk.ac.starlink.topcat.join;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.geom.Ellipse2D;
import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import uk.ac.starlink.ttools.cone.Coverage;

/**
 * Small component which gives a visual representation of a coverage area.
 * This is currently a pixel-resolution Mollweide projection of of the
 * coverage.  Component foreground and background colours are used.
 *
 * @author   Mark Taylor
 * @since    5 Jan 2012
 */
public class CoverageView extends JComponent {

    private volatile Coverage coverage_;
    private Icon covIcon_;
    private Object iconKey_;
    private String subject_;
    private volatile Thread coverageThread_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat.join" );

    /**
     * Constructs a coverage with subject description.
     *
     * @param   subject   short description of what the coverage is of,
     *                    used in tooltip
     */
    public CoverageView( String subject ) {
        super();
        subject_ = subject;
    }

    /**
     * Constructor.
     */
    public CoverageView() {
        this( null );
    }

    /**
     * Sets the coverage for this component to display.
     * The coverage is initialised asynchronously; the initialising
     * thread will be <code>interrrupt</code>ed if a new coverage needs
     * initialising before it's done.
     *
     * @param  coverage  coverage for display
     */
    public void setCoverage( final Coverage coverage ) {
        coverage_ = coverage;
        if ( coverage != null && coverage.getAmount() == null ) {
            final Thread thread = new Thread( "Coverager" ) {
                public void run() {
                    final Thread cthread = this;
                    if ( coverage == coverage_ ) {
                        try {
                            coverage.initCoverage();
                            SwingUtilities.invokeLater( new Runnable() {
                                public void run() {
                                    CoverageView.this.repaint();
                                }
                            } );
                        }
                        catch ( IOException e ) {
                            logger_.info( "Coverage initialization did not "
                                        + "complete: " + e );
                        }
                        finally {
                            SwingUtilities.invokeLater( new Runnable() {
                                public void run() {
                                    if ( coverageThread_ == cthread ) {
                                        coverageThread_ = null;
                                    }
                                }
                            } );
                        }
                    }
                }
            };
            if ( coverageThread_ != null ) {
                coverageThread_.interrupt();
            }
            coverageThread_ = thread;
            thread.start();
        }
        repaint();
    }

    /**
     * Returns the coverage currently displayed by this component.
     *
     * @return   coverage
     */
    public Coverage getCoverage() {
        return coverage_;
    }

    @Override
    protected void paintComponent( Graphics g ) {
        super.paintComponent( g );
        Dimension size = getSize();
        Insets insets = getInsets();
        int w = size.width - insets.left - insets.right - 1;
        int h = size.height - insets.top - insets.bottom - 1;
        int x = insets.left;
        int y = insets.top;
        int wover = w - h * 2;
        if ( wover > 0 ) {
            w = h * 2;
            x += wover / 2;
        }
        else {
            h = w / 2;
            y -= wover / 4;
        }
        Ellipse2D orb = new Ellipse2D.Float( x, y, w, h );
        Coverage.Amount amount =
            coverage_ == null ? null : coverage_.getAmount();
        Graphics2D g2 = (Graphics2D) g;
        if ( amount == Coverage.Amount.ALL_SKY ) {
            g2.fill( orb );
        }
        else if ( amount == Coverage.Amount.NO_SKY ) {
            Color color = g2.getColor();
            g2.setColor( getBackground() );
            g2.fill( orb );
            g2.setColor( color );
        }
        else if ( amount == Coverage.Amount.SOME_SKY ) {
            getCoverageIcon( coverage_, h ).paintIcon( this, g2, x, y );
        }
        else {
            assert amount == Coverage.Amount.NO_DATA || amount == null;
            Color color = g2.getColor();
            g2.setColor( getBackground() );
            g2.draw( orb );
            g2.setColor( color );
        }
        StringBuffer sbuf = new StringBuffer( "Sky coverage" );
        if ( subject_ != null ) {
            sbuf.append( " for " )
                .append( subject_ );
        }
        sbuf.append( ": " )
            .append( amount );
        setToolTipText( sbuf.toString() );
    }

    @Override
    public Dimension getPreferredSize() {
        int h = super.getPreferredSize().height;
        if ( h < 4 ) {
            h = 16;
        }
        Insets insets = getInsets();
        return new Dimension( 2 * h + insets.left + insets.right,
                              h + insets.top + insets.bottom );
    }

    /**
     * Returns an icon at a given size for a given coverage object.
     * Some caching (the most recently-used icon) is performed.
     *
     * @param  coverage  coverage to represent
     * @param  height   height in pixels of icon
     */
    private synchronized Icon getCoverageIcon( Coverage coverage, int height ) {
        Object iconKey =
            Arrays.asList( new Object[] { coverage,
                                          Integer.valueOf( height ) } );
        if ( ! iconKey.equals( iconKey_ ) ) {
            iconKey_ = iconKey;
            covIcon_ = new MollweideIcon( coverage, height );
        }
        return covIcon_;
    }

    /**
     * Icon displaying coverage by colouring in covered pixels
     * on a Mollweide projection of the sky.
     * Component foreground and background colours are used.
     */
    private static class MollweideIcon implements Icon {
        
        private final Coverage coverage_;
        private final int nx_;
        private final int ny_;
        private final BitSet mask_;
        private static final double SPHERE_AREA_DEG = 360 * 360 / Math.PI;

        /** 
         * Constructor.
         *
         * @param  coverage  coverage
         * @param  height  icon height in pixels (width will be 2*height)
         */
        MollweideIcon( Coverage coverage, int height ) {
            coverage_ = coverage;

            nx_ = height * 2;
            ny_ = height;
            mask_ = new BitSet( nx_ * ny_ );

            /* Work out the equivalent radius in degrees of one screen pixel
             * (i.e. the radius of a disc which has the same area as a
             * screen pixel).  This makes sense because Mollweide is an
             * equal-area projection. */
            int npix = (int) Math.round( nx_ * ny_ * Math.PI * 0.25 );
            double pixArea = SPHERE_AREA_DEG / npix;
            double radiusDeg = Math.sqrt( pixArea / Math.PI );

            /* Set up a pixel mask that can be drawn (fast) later.
             * Ellipse coordinates on screen look like:
             *                       (0,+90)
             *   (180,0)    (90,0)   (0,0)     (-90,0)  (-180,0)
             *                       (0,-90)  
             * i.e. both directions are negative of screen directions,
             * Y because screen directions increase towards the bottom, and
             * X because astronomers like it that way (as per CDS portal
             * coverage display). */
            for ( int iy = 0; iy < ny_; iy++ ) {
                double y = ( ( 2.0 * iy / ny_ ) - 1 ) * - Math.sqrt( 2 );
                double theta = Math.asin( y * Math.sqrt( 0.5 ) );
                double phi = Math.asin( ( 2 * theta + Math.sin( 2 * theta ) )
                                        / Math.PI );
                for ( int ix = 0; ix < nx_; ix++ ) {
                    double x = ( ( 2.0 * ix / nx_ ) - 1 )
                             * - Math.sqrt( 2 ) * 2;
                    double lambda = Math.PI * x
                                  / ( 2 * Math.sqrt( 2 ) * Math.cos( theta ) );
                    if ( lambda >= -Math.PI && lambda <= +Math.PI ) {
                        double alphaDeg = lambda / Math.PI * 180;
                        double deltaDeg = phi / Math.PI * 180;
                        if ( coverage.discOverlaps( alphaDeg, deltaDeg,
                                                    radiusDeg ) ) {
                             mask_.set( pixIndex( ix, iy ) );
                        }
                    }
                }
            }
        }

        public int getIconHeight() {
            return ny_;
        }

        public int getIconWidth() {
            return nx_;
        }

        public void paintIcon( Component c, Graphics g, int ox, int oy ) {
            Color color = g.getColor();
            g.setColor( c.getBackground() );
            g.fillOval( ox, oy, nx_, ny_ );
            g.setColor( color );
            for ( int ix = 0; ix < nx_; ix++ ) {
                for ( int iy = 0; iy < ny_; iy++ ) {
                    if ( mask_.get( pixIndex( ix, iy ) ) ) {
                        g.fillRect( ox + ix, oy + iy, 1, 1 );
                    }
                }
            }
        }

        /**
         * Maps (x,y)->index for a vector representation of the icon rectangle.
         *
         * @param  ix  X position
         * @param  iy  Y position
         * @return   offset into pixel array
         */
        private final int pixIndex( int ix, int iy ) {
            return ix * ny_ + iy;
        }
    }

}
