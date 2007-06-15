package uk.ac.starlink.topcat.plot;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.event.MouseInputAdapter;

/**
 * Mouse listener which can manufacture zoom requests on the basis of
 * mouse gestures.  The zones over which mouse gestures are gathered 
 * from and over which graphical feedback is given is defined by a list
 * of {@link ZoomRegion} objects held by instances of this class.
 *
 * <p>To use an instance of this class, you must install it as both a 
 * <code>MouseListener</code> <em>and</em> a 
 * <code>MouseMotionListener</code> on the relevant component.
 *
 * @author   Mark Taylor
 * @since    28 Mar 2006
 */
public class Zoomer extends MouseInputAdapter {

    private List regionList_;
    private ZoomDrag drag_;
    private ZoomRegion dragRegion_;
    private Component cursorComponent_;

    /**
     * Constructor.
     */
    public Zoomer() {
        regionList_ = new ArrayList();
    }

    /**
     * Returns the list of {@link ZoomRegion} objects which defines the
     * behaviour of this object.
     *
     * @return   zoom region list
     */
    public List getRegions() {
        return regionList_;
    }

    /**
     * Sets the list of {@link ZoomRegion} objects which defines the
     * behaviour of this object.
     *
     * @param   regionList  list of zoom regions
     */
    public void setRegions( List regionList ) {
        regionList_ = regionList;
    }

    /**
     * Sest the component, if any, over which this object should modify
     * the cursor over.  The cursor will be altered to indicate when
     * the mouse is in an active zoom target region.
     *
     * @param   comp   component over which the cursor can be changed
     */
    public void setCursorComponent( Component comp ) {
        cursorComponent_ = comp;
    }

    public void mousePressed( MouseEvent evt ) {
        if ( evt.getButton() == MouseEvent.BUTTON1 && drag_ == null ) {
            for ( Iterator it = regionList_.iterator(); it.hasNext(); ) {
                ZoomRegion region = (ZoomRegion) it.next();
                Point point = getPoint( evt );
                if ( region.getTarget() != null &&
                     region.getTarget().contains( point ) ) {
                    drag_ = region.createDrag( (Component) evt.getSource(),
                                               point );
                    dragRegion_ = region;
                    return;
                }
            }
        }
    }

    public void mouseMoved( MouseEvent evt ) {
        configureCursor( evt );
    }

    public void mouseDragged( MouseEvent evt ) {
        if ( drag_ != null ) {
            drag_.dragTo( getPoint( evt ) );
        }
    }

    public void mouseReleased( MouseEvent evt ) {
        if ( drag_ != null ) {
            double[][] zoomBounds = drag_.boundsAt( getPoint( evt ) );
            if ( zoomBounds != null ) {
                dragRegion_.zoomed( zoomBounds );
            }
            drag_ = null;
            dragRegion_ = null;
        }
        configureCursor( evt );
    }

    /**
     * Returns the point at which a mouse event originated relative to
     * the component in which it was seen.
     *
     * @param   evt   event
     * @return   relative position
     */
    private Point getPoint( MouseEvent evt ) {
        Point point = new Point( evt.getPoint() );
        Point offset = ((Component) evt.getSource()).getLocation();
        point.translate( offset.x, offset.y );
        return point;
    }

    /**
     * Configures the cursor property for the position defined by a given
     * mouse event.
     *
     * @param  evt  event
     */
    private void configureCursor( MouseEvent evt ) {
        Component cursorComponent = cursorComponent_ != null 
                                  ? cursorComponent_
                                  : (Component) evt.getSource();
        if ( cursorComponent != null ) {
            Point p = getPoint( evt );
            for ( Iterator it = regionList_.iterator(); it.hasNext(); ) {
                ZoomRegion region = (ZoomRegion) it.next();
                if ( region.getTarget().contains( p ) ) {
                    cursorComponent.setCursor( region.getCursor() );
                    return;
                }
            }
            cursorComponent.setCursor( null );
        }
    }
}
