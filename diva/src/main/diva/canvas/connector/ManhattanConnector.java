/*
 * $Id: ManhattanConnector.java,v 1.19 2002/09/26 12:19:43 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.canvas.connector;

import diva.canvas.CanvasUtilities;
import diva.canvas.Figure;
import diva.canvas.Site;
import diva.canvas.TransformContext;

import diva.util.java2d.Polyline2D;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.SwingConstants;


/** A Connector that draws itself with perpendicular lines.
 * To help it route itself, the connector contains an
 * instance of ManhattanRouter, which can be changed to create
 * other kinds of (or smarter) connectors.
 * By default the connector routes itself with rounded corners, which 
 * tend to look a little nicer in complex views.  To get standard right
 * angles at the corners, set the bend radius to zero.
 *
 * @version $Revision: 1.19 $
 * @author  John Reekie
 * @author  Michael Shilman
 */
public class ManhattanConnector extends AbstractConnector {

    /** The radius for filleting the corners of the connector.
     */
    private double _bendRadius = 50;

    /** The location to attach the label to.
     */
    private Point2D _labelLocation;

    /** The router used to route this connector
     */
    private ManhattanRouter _router = new ManhattanRouter ();

    /** Create a new manhattan connector between the given
     * sites. The connector is drawn with a width of one
     * and in black. The router is an instance of BasicManhattanRouter.
     * The connector is not routed until route() is called.
     * The corners of the connector will be rounded with a bend radius of 50. 
     */
    public ManhattanConnector (Site tail, Site head) {
        super(tail, head);
    }

    /** Apply a bend radius to the manhattan route.
     */
    public Shape getCurvedRoute (Polyline2D route) {
	GeneralPath path = new GeneralPath();
	path.moveTo((float)route.getX(0), (float)route.getY(0));
	double prevX = route.getX(0);
	double prevY = route.getY(0);
	for(int i = 2; i < route.getVertexCount(); i++) {
	    //consider triplets of coordinates
	    double x0 = prevX;//route.getX(i-2);
	    double y0 = prevY;//route.getY(i-2);
	    double x1 = route.getX(i-1);
	    double y1 = route.getY(i-1);
	    double x2 = route.getX(i);
	    double y2 = route.getY(i);
                			
	    //midpoints
	    x2 = (x1+x2)/2;
	    y2 = (y1+y2)/2;
		
	    //first make sure that the radius is not
	    //bigger than half one of the arms of the triplets
	    double d0 = Math.sqrt((x1-x0)*(x1-x0) +
				  (y1-y0)*(y1-y0));
	    double d1 = Math.sqrt((x2-x1)*(x2-x1) +
				  (y2-y1)*(y2-y1));
	    double r = Math.min(_bendRadius, d0);
	    r = Math.min(r, d1);
		
	    // The degenerate case of a direct line.
	    if((d0 == 0.0) || (d1 == 0.0)) {
		path.lineTo((float)x1, (float)y1); 
	    } else {
		//next calculate the intermediate points
		//that define the bend.
		double intX0 = x1 + ((r/d0)*(x0-x1));
		double intY0 = y1 + ((r/d0)*(y0-y1));
		double intX1 = x1 + ((r/d1)*(x2-x1));
		double intY1 = y1 + ((r/d1)*(y2-y1));
                    
		//next draw the line from the previous
		//coord to the intermediate coord, and
		//curve around the corner
		path.lineTo((float)intX0, (float)intY0);
		path.curveTo((float)x1, (float)y1, 
			     (float)x1, (float)y1, 
			     (float)intX1, (float)intY1);
		prevX = x2;
		prevY = y2;
	    }
	}
	//finally close the last segment with a line.
	path.lineTo((float)route.getX(route.getVertexCount()-1),
		    (float)route.getY(route.getVertexCount()-1));

	return path;
    }


    /** Return the direction between two points who differ by the 
     *  given amounts.  The direction returned is restricted to the 
     *  closest orthogonal direction.  The integer returned is from
     *  SwingConstants.
     */  
    private int getManhattanDirection (double xDiff, double yDiff) {
	int dir;
	if(xDiff > 0 && yDiff > 0) {
	    if(xDiff > yDiff) {
		dir = SwingConstants.EAST;
	    } else {
		dir = SwingConstants.SOUTH;
	    }
	} else if(xDiff < 0 && yDiff < 0) {
	    if(xDiff > yDiff) {
		dir = SwingConstants.NORTH;
	    } else {
		dir = SwingConstants.WEST;
	    }
	} else if(xDiff > 0) {
	    if(xDiff > -yDiff) {
		dir = SwingConstants.EAST;
	    } else {
		dir = SwingConstants.NORTH;
	    }
	} else {
	    if(-xDiff > yDiff) {
		dir = SwingConstants.WEST;
	    } else {
		dir = SwingConstants.SOUTH;
	    }
	}
	return dir;
    }

    /**
     * Return the maximum bend radius of the manhattan-routed
     * edge.  A value of zero means that the corners of the route
     * are square; the larger the value, the more curvy the corners
     * will be.
     */
    public double getBendRadius() {
	return _bendRadius;
    }
    
    /** Inform the connector that the head site has moved. This method
     * reroutes the unpinned segments at the head end of the connector.
     */
    public void headMoved (double dx, double dy) {
	if (getHeadEnd() != null) {
	    getHeadEnd().translate(dx, dy);
	}
	_router.translateEnd(dx, dy);
	updateFromRouter();
    }

    /** Tell the connector to reposition the text label.
     */
    public void repositionLabel () {
	if(_labelLocation == null) {
	    route();
	    // route will call this method recursively.
	    return;
	}
	if (getLabelFigure() != null) {
	    getLabelFigure().translateTo(_labelLocation);
            getLabelFigure().autoAnchor(getShape());
        }
    }
    
    /** 
     * Tell the connector to route itself between the
     * current positions of the head and tail sites.
     */
    public void route () {
        TransformContext currentContext = getTransformContext();
        
        Site headSite = getHeadSite();
        Site tailSite = getTailSite();
        Point2D headPt, tailPt;

        // Get the transformed head and tail points. Sometimes
        // people will call this before the connector is added
        // to a container, so deal with it
        if (currentContext != null) {
            tailPt = tailSite.getPoint(currentContext);
            headPt = headSite.getPoint(currentContext);
        } else {
            tailPt = tailSite.getPoint();
            headPt = headSite.getPoint();
        }
       
 	double xDiff = headPt.getX() - tailPt.getX();
	double yDiff = headPt.getY() - tailPt.getY();

	// Infer normals if there are none.  This needs to be 
	// smarter, and should depend on the normal at the other
	// end if there is one.
	int headDir = CanvasUtilities.reverseDirection(
	    getManhattanDirection(xDiff, yDiff));
	headSite.setNormal(CanvasUtilities.getNormal(headDir));
	if (currentContext != null) {
	    headPt = headSite.getPoint(currentContext);
	} else {
	    headPt = headSite.getPoint();
	}
	
	int tailDir = getManhattanDirection(xDiff, yDiff);
	tailSite.setNormal(CanvasUtilities.getNormal(tailDir));
	if (currentContext != null) {
	    tailPt = tailSite.getPoint(currentContext);
	} else {
	    tailPt = tailSite.getPoint();
	}
	
	// The site may not allow it's normal to be changed.  In 
	// which case, we have to ask it for the normal again
	headDir = CanvasUtilities.getDirection(headSite.getNormal());
	tailDir = CanvasUtilities.getDirection(tailSite.getNormal());

        // Adjust for decorations on the ends
	double headAngle = CanvasUtilities.getNormal(headDir);
	double tailAngle = CanvasUtilities.getNormal(tailDir);
        if (getHeadEnd() != null) {
            getHeadEnd().setNormal(headAngle);
            getHeadEnd().setOrigin(headPt.getX(), headPt.getY());
            getHeadEnd().getConnection(headPt);
        }
        if (getTailEnd() != null) {
            getTailEnd().setNormal(tailAngle);
            getTailEnd().setOrigin(tailPt.getX(), tailPt.getY());
            getTailEnd().getConnection(tailPt);
        }

	// The normals are *out* of the figure -- adjust
	headDir = CanvasUtilities.reverseDirection(headDir);

	// Do a new route
	_router.route(tailPt, tailDir, headPt, headDir);
	updateFromRouter();
    }

    /**
     * Set the maximum bend radius of the manhattan-routed
     * edge.  A value of zero means that the corners of the route
     * are square; the larger the value, the more curvy the corners
     * will be.
     * 
     * @see #getBendRadius()
     */
    public void setBendRadius(double r) {
        if(r < 0) {
	    throw new IllegalArgumentException("Illegal radius: " + r);
        }
        _bendRadius = r;
    }

    /** Inform the connector that the tail site has moved. This method
     * reroutes the unpinned segments at the tail end of the connector.
     */
    public void tailMoved (double dx, double dy) {
	if (getTailEnd() != null) {
	    getTailEnd().translate(dx, dy);
	}
	_router.translateStart(dx, dy);
	updateFromRouter();
    }

    /** Translate the connector. This method is implemented, since
     * controllers may wish to translate connectors when the
     * sites at both ends are moved the same distance.
     */
    public void translate (double x, double y) {
        repaint();
        Polyline2D line = (Polyline2D) getShape();
        line.translate(x,y);
        repaint();
    }

    /** Update the connector from the router. This method uses whatever
     * route is in the router, and reconfigures and redraws the connector
     * accordingly.
     */
    void updateFromRouter () {
	Polyline2D poly = _router.createShape();

        // Move the label
	int count = poly.getVertexCount();
	if(count > 1) {
	    // pick a location for the label in the middle of the connector.
	    _labelLocation = new Point2D.Double(
		       (poly.getX(count/2) + poly.getX(count/2-1))/2,
		       (poly.getY(count/2) + poly.getY(count/2-1))/2);
	} else {
	    // attach the label to the only point of the connector.
	    _labelLocation = 
		new Point2D.Double(poly.getX(0), poly.getY(0));
	}
	repositionLabel();

	// Set the shape, curving it if necessary
	repaint(); // Should be optimized for modified segments only
 	if(_bendRadius > 0) {
	    setShape(getCurvedRoute(poly));
	} else {
	    setShape(poly);
	}
	repaint(); // Should be optimized for modified segments only
    }
}
