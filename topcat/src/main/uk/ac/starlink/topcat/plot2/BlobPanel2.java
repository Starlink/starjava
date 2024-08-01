package uk.ac.starlink.topcat.plot2;

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
import uk.ac.starlink.ttools.plot2.PlotUtil;

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
public abstract class BlobPanel2 extends JComponent {

    private final BlobListener blobListener_;
    private List<Shape> blobs_;
    private GeneralPath dragPath_;
    private Action blobAction_;
    private Color fillColor_ = new Color( 0, 0, 0, 64 );
    private Color pathColor_ = new Color( 0, 0, 0, 128 );
    private boolean isActive_;

    /* Name of boolean property associated with isActive method. */
    public static final String PROP_ACTIVE = "active";

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public BlobPanel2() {
        blobListener_ = new BlobListener();
        addComponentListener( new ComponentAdapter() {
            public void componentResized( ComponentEvent evt ) {
                clear();
            }
        } );
        setOpaque( false );

        /* Constructs an associated action which can be used to start
         * and stop blob drawing. */
        blobAction_ = new AbstractAction() {
            public void actionPerformed( ActionEvent evt ) {
                if ( isActive() ) {
                    if ( ! blobs_.isEmpty() ) {
                        blobCompleted( getBlob() );
                    }
                    else {
                        setActive( false );
                    }
                }
                else {
                    setActive( true );
                }
            }
        };

        /* Initialise the action and this component. */
        clear();
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
     * Resets the current blob to an empty shape.
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
        if ( active != isActive_ ) {
            clear();
        }
        isActive_ = active;
        blobAction_.putValue( Action.NAME,
                              active ? "Finish Drawing Blob"
                                     : "Draw Blob Subset" );
        blobAction_.putValue( Action.SMALL_ICON,
                              active ? ResourceIcon.BLOB_SUBSET_END
                                     : ResourceIcon.BLOB_SUBSET );
        blobAction_.putValue( Action.SHORT_DESCRIPTION,
                              active ? "Define subset from currently-drawn " +
                                       "region"
                                     : "Draw a freehand region on the plot " +
                                       "to define a new row subset" );
        blobAction_.putValue( PROP_ACTIVE, Boolean.valueOf( active ) );
        setListening( active );
        setVisible( active );
    }

    /**
     * Changes whether this component is listening to mouse gestures to
     * modify the shape.  This method is called by <code>setActive</code>,
     * but may be called independently of it as well.
     *
     * @param  isListening   whether mouse gestures can affect current shape
     */
    public void setListening( boolean isListening ) {
        if ( isListening ) {
            addMouseListener( blobListener_ );
            addMouseMotionListener( blobListener_ );
        }
        else {
            removeMouseListener( blobListener_ );
            removeMouseMotionListener( blobListener_ );
        }
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
     * blob drawing session.  Implementations of this method are expected
     * to clear up by calling <code>setActive(false)</code> when the
     * blob representation is no longer required.
     *
     * @param  blob  completed shape
     */
    protected abstract void blobCompleted( Shape blob );

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

    @Override
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
     * Mouse listener that uses mouse gestures to draw a blob.
     */
    private class BlobListener implements MouseListener, MouseMotionListener {

        public void mouseEntered( MouseEvent evt ) {
        }

        public void mouseExited( MouseEvent evt ) {
            if ( dragPath_ != null ) {
                dragPath_ = null;
                repaint();
            }
        }

        public void mouseClicked( MouseEvent evt ) {
            if ( PlotUtil.getButtonChangedIndex( evt ) == 3 ) {
                int nblob = blobs_.size();
                if ( nblob > 0 ) {
                    blobs_.remove( nblob - 1 );
                }
                repaint();
            }
        }

        public void mousePressed( MouseEvent evt ) {
            if ( PlotUtil.getButtonChangedIndex( evt ) == 1 ) {
                Point p = evt.getPoint();
                dragPath_ = new GeneralPath();
                dragPath_.moveTo( p.x, p.y );
            }
        }

        public void mouseReleased( MouseEvent evt ) {
            if ( PlotUtil.getButtonChangedIndex( evt ) == 1 ) {
                if ( dragPath_ != null ) {
                    blobs_.add( simplify( dragPath_ ) );
                    dragPath_ = null;
                    repaint();
                }
            }
        }

        public void mouseDragged( MouseEvent evt ) {
            if ( dragPath_ != null ) {
                Point p = evt.getPoint();
                dragPath_.lineTo( p.x, p.y );
                repaint();
            }
        }

        public void mouseMoved( MouseEvent evt ) {
        }
    }
}
