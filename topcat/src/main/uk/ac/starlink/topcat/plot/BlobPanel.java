package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.event.MouseInputListener;
import uk.ac.starlink.topcat.ResourceIcon;

/**
 * Component which allows the user to draw a blob using the mouse.
 * You drag the mouse around to cover patches of the component;
 * you can create several separate or overlapping blobs by doing 
 * several click-drag sequences.  If you drag off the window the
 * currently-dragged region will be ditched.  Clicking with the right
 * button removes the most recently-added blob.  Resizing the window
 * clears any existing blobs (since it's not obvious how or if to 
 * resize the blobs).  Try it, it's easy.
 *
 * @author   Mark Taylor (Starlink)
 * @since    8 Jul 2004
 */
public class BlobPanel extends JComponent
                       implements MouseListener, MouseMotionListener {

    private List<Shape> blobs_;
    private GeneralPath dragPath_;
    private Action blobAction_;
    private Color fillColor_ = new Color( 0, 0, 0, 64 );
    private Color pathColor_ = new Color( 0, 0, 0, 128 );
    private boolean isActive_;

    /**
     * Creates a new BlobPanel.
     */
    @SuppressWarnings("this-escape")
    public BlobPanel() {
        addMouseListener( this );
        addMouseMotionListener( this );
        addComponentListener( new ComponentAdapter() {
            public void componentResized( ComponentEvent evt ) {
                clear();
            }
        } );
        setOpaque( false );
        clear();

        /* Constructs an associated action which can be used to start
         * and stop blob drawing. */
        blobAction_ = new AbstractAction() {
            public void actionPerformed( ActionEvent evt ) {
                if ( isActive() ) {
                    if ( ! blobs_.isEmpty() ) {
                        blobCompleted( getBlob() );
                    }
                    clear();
                    setActive( false );
                }
                else {
                    setActive( true );
                }
            }
        };

        /* Initialises the action and this component by calling setActive
         * with an initial value. */
        setActive( false );
    }

    /**
     * Returns the currently-defined blob.
     *
     * @return   shape drawn
     */
    public Shape getBlob() {
        Area area = new Area();
        for ( Shape blob : blobs_ ) {
            area.add( new Area( blob ) );
        }
        return simplify( area );
    }

    /**
     * Sets the currently-defined blob.
     *
     * @param  blob  shape to be displayed and played around with by the user
     */
    public void setBlob( Shape blob ) {
        blobs_ = new ArrayList<Shape>();
        blobs_.add( blob );
        repaint();
    }

    /**
     * Resets the current blob to a null shape.
     */
    public void clear() {
        blobs_ = new ArrayList<Shape>();
        repaint();
    }

    /**
     * Returns the action which is used to start and stop blob drawing.
     * Invoking the action toggles the activity status of this panel,
     * and when invoked for deactivation (that is after a blob has been
     * drawn) then {@link #blobCompleted} is called.
     *
     * @return   activation toggle action
     */
    public Action getBlobAction() {
        return blobAction_;
    }

    /**
     * Sets whether this panel is active (visible, accepting mouse gestures,
     * drawing shapes) or inactive (invisible).
     *
     * @param   active  true to select activeness
     */
    public void setActive( boolean active ) {
        isActive_ = active;
        clear();
        setVisible( active );
        blobAction_.putValue( Action.NAME,
                              active ? "Finish Drawing Region"
                                     : "Draw Subset Region" );
        blobAction_.putValue( Action.SMALL_ICON,
                              active ? ResourceIcon.BLOB_SUBSET_END
                                     : ResourceIcon.BLOB_SUBSET );
        blobAction_.putValue( Action.SHORT_DESCRIPTION,
                              active ? "Define susbset from currently-drawn " +
                                       "region"
                                     : "Draw a region on the plot to define " +
                                       "a new row subset" );
    }

    /**
     * Indicates whether this blob is currently active.
     *
     * @return   true iff this blob is active (visible and drawing)
     */
    public boolean isActive() {
        return isActive_;
    }

    /**
     * Invoked when this component's action is invoked to terminate a
     * blob drawing session.  Subclasses may provide an implementation
     * which does something with the shape.  The default implemenatation
     * does nothing.
     *
     * @param  blob  completed shape
     */
    protected void blobCompleted( Shape blob ) {
    }

    /**
     * Sets the colours which will be used for drawing the blob.
     *
     * @param  fillColor  colour which fills the blob area
     * @param  pathColor  colour which delineates the blob region
     */
    public void setColors( Color fillColor, Color pathColor ) {
        fillColor_ = fillColor;
        pathColor_ = pathColor;
    }

    protected void paintComponent( Graphics g ) {
        Color oldColor = g.getColor();
        Graphics2D g2 = (Graphics2D) g;

        Area area = new Area();
        for ( Shape blob : blobs_ ) {
            area.add( new Area( blob ) );
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
