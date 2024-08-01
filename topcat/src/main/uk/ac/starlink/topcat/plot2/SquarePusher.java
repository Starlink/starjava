package uk.ac.starlink.topcat.plot2;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import uk.ac.starlink.ttools.plot2.PlotUtil;

/**
 * Interactive component that allows you to drag a little rectangle round
 * inside a big rectangle.  It is used as part of the UI for legend positioning.
 *
 * @author   Mark Taylor
 * @since    13 Mar 2013
 */
public class SquarePusher extends JComponent {

    private final float xfrac_ = 0.2f;
    private final float yfrac_ = 0.2f;
    private float xpos_ = 0.9f;
    private float ypos_ = 0.1f;
    private boolean highlight_;
    private List<ActionListener> listenerList_;

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public SquarePusher() {
        listenerList_ = new ArrayList<ActionListener>();
        MouseInputListener mousey = new DragListener();
        addMouseListener( mousey );
        addMouseMotionListener( mousey );
    }

    /**
     * Adds a listener to be notified when the position changes.
     *
     * @param  listener  listener
     */
    public void addActionListener( ActionListener listener ) {
        listenerList_.add( listener );
    }

    /**
     * Removes a listener previously added.
     *
     * @param  listener  listener
     */
    public void removeActionListener( ActionListener listener ) {
        listenerList_.remove( listener );
    }

    /**
     * Returns the selected fractional horizontal position.
     *
     * @return  X position in the range 0..1
     */
    public float getXPosition() {
        return xpos_;
    }

    /**
     * Returns the selected fractional vertical position.
     *
     * @return  Y position in the range 0..1
     */
    public float getYPosition() {
        return ypos_;
    }

    /**
     * Sets the fractional position.
     *
     * @param  xpos  new X position in the range 0..1
     * @param  ypos  new Y position in the range 0..1
     */
    public void setPosition( float xpos, float ypos ) {
        if ( xpos != xpos_ || ypos != ypos_ ) {
            xpos_ = (float) PlotUtil.roundNumber( xpos, 0.2 / getWidth() );
            ypos_ = (float) PlotUtil.roundNumber( ypos, 0.2 / getHeight() );
            ActionEvent evt = new ActionEvent( this, 1, "Move" );
            for ( ActionListener listener : listenerList_ ) {
                listener.actionPerformed( evt );
            }
        }
    }

    @Override
    protected void paintComponent( Graphics g ) {
        super.paintComponent( g );
        Graphics2D g2 = (Graphics2D) g;
        Color color0 = g2.getColor();
        if ( isOpaque() ) {
            g2.setColor( getBackground() );
            g2.fillRect( 0, 0, getWidth(), getHeight() );
        }
        g2.setColor( getForeground() );
        Rectangle field = getField();
        g2.drawRect( field.x, field.y, field.width - 1, field.height - 1 );
        Rectangle target = getTarget();
        g2.drawRect( target.x, target.y, target.width - 1, target.height - 1 );
        if ( highlight_ ) {
            g2.drawRect( target.x + 1, target.y + 1,
                         target.width - 3, target.height - 3 );
        }
        g2.setColor( color0 );
    }

    @Override
    public void setEnabled( boolean isEnabled ) {
        setForeground( isEnabled
                     ? UIManager.getColor( "Label.foreground" )
                     : UIManager.getColor( "Label.disabledForeground" ) );
        super.setEnabled( isEnabled );
    }

    /**
     * Returns the rectangle usable for actual drawing, which is also
     * the rectangle against which target position is assessed.
     *
     * @return   bounds excluding insets
     */
    private Rectangle getField() {
        Dimension size = getSize();
        Insets insets = getInsets();
        return new Rectangle( insets.left, insets.top,
                              size.width - insets.left - insets.right,
                              size.height - insets.top - insets.bottom );
    }

    /**
     * Returns the shape of the smaller target rectangle.
     *
     * @return  draggable target rectangle
     */
    private Rectangle getTarget() {
        Rectangle field = getField();
        Point pt = posToPoint( new float[] { xpos_, ypos_ } );
        return new Rectangle( pt.x, pt.y,
                              Math.round( field.width * xfrac_ ),
                              Math.round( field.height * yfrac_ ) );
    }

    /**
     * Converts fractional positions to the target rectangle graphics origin.
     *
     * @param   pos  (x,y) fractional position
     * @return   graphics position of target rectangle
     */
    private Point posToPoint( float[] pos ) {
        Rectangle field = getField();
        int xoff = Math.round( field.width * ( 1f - xfrac_ ) * pos[ 0 ] );
        int yoff = Math.round( field.height * ( 1f - yfrac_ ) * pos[ 1 ] );
        return new Point( field.x + xoff, field.y + yoff );
    }

    /**
     * Converts target rectangle graphics origin to fractional positions.
     *
     * @param  point  graphics position of target rectangle
     * @return  (x,y) fractional position
     */
    private float[] pointToPos( Point point ) {
        Rectangle field = getField();
        float xpos = ( point.x - field.x ) / ( field.width * ( 1f - xfrac_ ) );
        float ypos = ( point.y - field.y ) / ( field.height * ( 1f - yfrac_ ) );
        return new float[] { xpos, ypos };
    }

    /**
     * Mouse listener that implements dragging of the target rectangle.
     */
    private class DragListener extends MouseInputAdapter {
        private Point startMousePoint_;
        private Point startTargetPoint_;

        @Override
        public void mousePressed( MouseEvent evt ) {
            Point mousePoint = evt.getPoint();
            if ( isEnabled() && getTarget().contains( mousePoint ) ) {
                startMousePoint_ = mousePoint;
                startTargetPoint_ = posToPoint( new float[] { xpos_, ypos_ } );
            }
            repaint( evt );
        }

        @Override
        public void mouseDragged( MouseEvent evt ) {
            if ( startMousePoint_ != null ) {
                Point mousePoint = evt.getPoint();
                int dx = mousePoint.x - startMousePoint_.x;
                int dy = mousePoint.y - startMousePoint_.y;
                Point targetPoint = new Point( startTargetPoint_.x + dx,
                                               startTargetPoint_.y + dy );
                float[] pos = pointToPos( targetPoint );
                setPosition( Math.max( 0f, Math.min( 1f, pos[ 0 ] ) ),
                             Math.max( 0f, Math.min( 1f, pos[ 1 ] ) ) );
                repaint( evt );
            }
        }

        @Override
        public void mouseReleased( MouseEvent evt ) {
            startMousePoint_ = null;
            startTargetPoint_ = null;
            repaint( evt );
        }

        @Override
        public void mouseMoved( MouseEvent evt ) {
            repaint( evt );
        }

        @Override
        public void mouseEntered( MouseEvent evt ) {
            repaint( evt );
        }

        @Override
        public void mouseExited( MouseEvent evt ) {
            repaint( evt );
        }

        private void repaint( MouseEvent evt ) {
            highlight_ = isEnabled()
                      && ( startMousePoint_ != null
                        || getTarget().contains( evt.getPoint() ) );
            SquarePusher.this.repaint();
        }
    }

    /**
     * Test.
     */
    public static void main( String[] args ) {
        javax.swing.JFrame frame = new javax.swing.JFrame();
        SquarePusher pusher = new SquarePusher();
        pusher.setEnabled( true );
        JComponent panel = (JComponent) frame.getContentPane();
        panel.setPreferredSize( new java.awt.Dimension( 300, 200 ) );
        panel.add( pusher );
        panel.setBorder( javax.swing.BorderFactory.createEmptyBorder(8,8,8,8) );
        frame.pack();
        frame.setVisible( true );
    }
}
