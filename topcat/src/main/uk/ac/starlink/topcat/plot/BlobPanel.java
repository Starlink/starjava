package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import javax.swing.JComponent;
import javax.swing.event.MouseInputListener;

/**
 * Component which allows the user to draw a blob using the mouse.
 * You drag the mouse around to cover patches of the component;
 * you can create several separate or overlapping blobs by doing 
 * several click-drag sequences.  If you drag off the window the
 * currently-dragged region will be ditched.  Try it, it's easy.
 *
 * @author   Mark Taylor (Starlink)
 * @since    8 Jul 2004
 */
public class BlobPanel extends JComponent
                       implements MouseListener, MouseMotionListener {

    private Area blob_ = new Area();
    private GeneralPath dragPath_;
    private Color color_ = new Color( 0, 0, 0, 64 );

    /**
     * Creates a new BlobPanel.
     */
    public BlobPanel() {
        addMouseListener( this );
        addMouseMotionListener( this );
    }

    /**
     * Returns the currently-defined blob.
     *
     * @return   shape drawn
     */
    public Shape getBlob() {
        return simplify( blob_ );
    }

    /**
     * Sets the currently-defined blob.
     *
     * @param  blob  shape to be displayed and played around with by the user
     */
    public void setBlob( Shape blob ) {
        blob_ = new Area( blob );
        repaint();
    }

    /**
     * Returns the current blob colour.  By default it's a semi-transparent 
     * black.
     *
     * @return  blob drawing colour
     */
    public Color getColor() {
        return color_;
    }

    /**
     * Sets the current blob colour.
     *
     * @param  color  blob drawing colour
     */
    public void setColor( Color color ) {
        color_ = color;
        repaint();
    }

    protected void paintComponent( Graphics g ) {
        super.paintComponent( g );
        Color oldColor = g.getColor();
        g.setColor( color_ );
        Graphics2D g2 = (Graphics2D) g;

        Area area = new Area( blob_ );
        if ( dragPath_ != null ) {
            area.add( new Area( dragPath_ ) );
        }
        g2.fill( area );

        g.setColor( oldColor );
    }

    /*
     * MouseListener implementation.
     */

    public void mouseEntered( MouseEvent evt ) {}

    public void mouseExited( MouseEvent evt ) {
        if ( dragPath_ != null ) {
            dragPath_ = null;
            repaint();
        }
    }

    public void mouseClicked( MouseEvent evt ) {}

    public void mousePressed( MouseEvent evt ) {
        Point p = evt.getPoint();
        dragPath_ = new GeneralPath();
        dragPath_.moveTo( p.x, p.y );
    }

    public void mouseReleased( MouseEvent evt ) {
        if ( dragPath_ != null ) {
            blob_.add( new Area( simplify( dragPath_ ) ) );
            dragPath_ = null;
            repaint();
        }
    }

    /*
     * MouseMotionListener implementation.
     */

    public void mouseDragged( MouseEvent evt ) {
        if ( dragPath_ != null ) {
            Point p = evt.getPoint();
            dragPath_.lineTo( p.x, p.y );
            repaint();
        }
    }

    public void mouseMoved( MouseEvent evt ) {}


    /**
     * Returns a simplified version of a given shape.
     * This may be important since the one drawn by the user could end up
     * having a lot of path components.
     */
    private static Shape simplify( Shape shape ) {
        // Current implementation is a no-op.  If we need to be cleverer,
        // probably want to look at java.awt.geom.FlatteningPathIterator?
        return shape;
    }

    /**
     * Main method.
     */
    public static void main( String[] args ) {
        BlobPanel blobber = new BlobPanel();
        blobber.setPreferredSize( new java.awt.Dimension( 500, 400 ) );
        javax.swing.JFrame top = new javax.swing.JFrame();
        top.getContentPane().setLayout( new java.awt.BorderLayout() );
        top.getContentPane().add( blobber );
        top.pack();
        top.setVisible( true );
    }
}
