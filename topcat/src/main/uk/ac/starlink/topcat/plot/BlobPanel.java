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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.event.MouseInputListener;

/**
 * Component which allows the user to draw a blob using the mouse.
 * You drag the mouse around to cover patches of the component;
 * you can create several separate or overlapping blobs by doing 
 * several click-drag sequences.  If you drag off the window the
 * currently-dragged region will be ditched.  Clicking with the right
 * button removes the most recently-added blob.  Try it, it's easy.
 *
 * @author   Mark Taylor (Starlink)
 * @since    8 Jul 2004
 */
public class BlobPanel extends JComponent
                       implements MouseListener, MouseMotionListener {

    private List blobs_;
    private GeneralPath dragPath_;
    private Color fillColor_ = new Color( 0, 0, 0, 64 );
    private Color pathColor_ = new Color( 0, 0, 0, 128 );

    /**
     * Creates a new BlobPanel.
     */
    public BlobPanel() {
        addMouseListener( this );
        addMouseMotionListener( this );
        setOpaque( false );
        clear();
    }

    /**
     * Returns the currently-defined blob.
     *
     * @return   shape drawn
     */
    public Shape getBlob() {
        Area area = new Area();
        for ( Iterator it = blobs_.iterator(); it.hasNext(); ) {
            area.add( new Area( (Shape) it.next() ) );
        }
        return simplify( area );
    }

    /**
     * Sets the currently-defined blob.
     *
     * @param  blob  shape to be displayed and played around with by the user
     */
    public void setBlob( Shape blob ) {
        blobs_ = new ArrayList();
        blobs_.add( blob );
        repaint();
    }

    /**
     * Resets the current blob to a null shape.
     */
    public void clear() {
        blobs_ = new ArrayList();
        repaint();
    }

    protected void paintComponent( Graphics g ) {
        Color oldColor = g.getColor();
        Graphics2D g2 = (Graphics2D) g;

        Area area = new Area();
        for ( Iterator it = blobs_.iterator(); it.hasNext(); ) {
            area.add( new Area( (Shape) it.next() ) );
        }
        if ( dragPath_ != null ) {
            area.add( new Area( dragPath_ ) );
            g2.setColor( pathColor_ );
            g2.draw( dragPath_ );
        }
        g2.setColor( fillColor_ );
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

    public void mouseClicked( MouseEvent evt ) {
        if ( evt.getButton() == MouseEvent.BUTTON3 ) {
            int nblob = blobs_.size();
            if ( nblob > 0 ) {
                blobs_.remove( nblob - 1 );
            }
            repaint();
        }
    }

    public void mousePressed( MouseEvent evt ) {
        if ( evt.getButton() == MouseEvent.BUTTON1 ) {
            Point p = evt.getPoint();
            dragPath_ = new GeneralPath();
            dragPath_.moveTo( p.x, p.y );
        }
    }

    public void mouseReleased( MouseEvent evt ) {
        if ( evt.getButton() == MouseEvent.BUTTON1 ) {
            if ( dragPath_ != null ) {
                blobs_.add( simplify( dragPath_ ) );
                dragPath_ = null;
                repaint();
            }
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
}
