/*
 * $Id: ArcConnector.java,v 1.9 2000/05/02 00:43:18 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.canvas.connector;

import diva.canvas.CanvasUtilities;
import diva.canvas.Figure;
import diva.canvas.Site;
import diva.canvas.TransformContext;
import diva.canvas.connector.AbstractConnector;
import diva.canvas.toolbox.LabelFigure;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** A Connector that draws itself in an arc. The connector
 * draws itself approximately through the center of the figures
 * that own the sites to which it is connected. The curvature of the
 * arc can be specified in one of two ways, depending on which
 * variable of the arc's shape remain constant as the distance
 * between the two figures at the ends of the arc is varied:
 *
 * <ol>
 * <li> Constant incident angle: The angle at which the arc connects
 * to the figure remains constant. This is the default behaviour,
 * and the default angle is 45 degrees.
 *
 * <li> Constant displacement at the maximum arc point, from
 * the straight line drawn between the two end points of the
 * connector. The default displacement is 20 (not for any good reason,
 * but is has to be something...).
 * </ul>
 * 
 * <P>Currently, only the first is supported.
 * 
 * <p> The connector uses an instance of PaintedPath to draw itself,
 * so see that class for a more detailed description of the paint- and
 * stroke-related methods.
 *
 * @version $Revision: 1.9 $
 * @author  Edward Lee (eal@eecs.berkeley.edu)
 * @author  John Reekie (johnr@eecs.berkeley.edu)
 * @rating  Red
 */
public class ArcConnector extends AbstractConnector {

    /** The constant that specifies that the connector is drawn with
     * a constant incident angle. Actually, the angle is the angle
     * out of the tail figure.
     */
    //// public static int CONSTANT_INCIDENT_ANGLE = 84;

    /** The constant that specifies that the connector is drawn with
     * a constant deviation from a straight line.
     */
    //// public static int CONSTANT_DISPLACEMENT = 84;

    /** The drawing mode, which must be one of the constants
     * defined here.
     */
    //// private int _mode = CONSTANT_INCIDENT_ANGLE;

    /** The arc shape that defines the connector shape
     */
    private Arc2D _arc;

    /** The line shape that is used when the arc is "flat"
     */
    ////private Line2D _flatLine;

    /** The incident angle
     */
    private double _angle = Math.PI / 5;

    /** The calculated parameters of the arc
     */
    private double _centerX;
    private double _centerY;
    private double _radius;
    private double _startAngle;
    private double _extentAngle;
    
    /** The arc displacement
     */
    //// private double _displacement = 20.0;

    /** Create a new arc connector between the given
     * sites. The connector is drawn with a width of one
     * and in black, and at the default incident angle of
     * 45 degrees (PI/4 radians).
     */
    public ArcConnector (Site tail, Site head) {
        super(tail, head);
        _arc =  new Arc2D.Double();
        getPaintedPath().shape = _arc;
        route();
    }

    /** Get the angle at whih the arc leaves the tail figure.
     */
    public double getAngle () {
        return _angle;
    }

    /** Tell the connector to reposition its label if it has one.
     * The label is currently only positioned at the center of the arc.
     */
    public void repositionLabel () {
        if (getLabelFigure() != null) {
            // Hm... I don't know why I need the PI/2 here -- johnr
            Point2D pt = new Point2D.Double(
                    _centerX + _radius * Math.sin(
                            _startAngle + _extentAngle/2 + Math.PI/2),
                    _centerY + _radius * Math.cos(
                            _startAngle + _extentAngle/2 + Math.PI/2));
            getLabelFigure().translateTo(pt);
            getLabelFigure().autoAnchor(_arc);
        }
    }

    /** Tell the connector to route itself between the
     * current positions of the head and tail sites.
     */
    public void route () {
        repaint();

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

        // Figure out the centers of the attached figures
        Point2D tailCenter, headCenter;
        if (tailSite.getFigure() != null) {
            tailCenter = CanvasUtilities.getCenterPoint(tailSite.getFigure(),
							currentContext);
        } else {
            tailCenter = tailPt;
        }
        if (headSite.getFigure() != null) {
            headCenter = CanvasUtilities.getCenterPoint(headSite.getFigure(), 
						        currentContext);
        } else {
            headCenter = headPt;
        }

        // Figure out the angle between the centers
        double x = headCenter.getX() - tailCenter.getX();
        double y = headCenter.getY() - tailCenter.getY();
        double gamma = Math.atan2(y, x);

        // Tell the sites to adjust their positions
        double alpha = _angle;
        double beta = Math.PI / 2.0 - alpha;
        double headNormal = gamma - alpha - Math.PI; 
        double tailNormal = gamma + alpha;
        tailSite.setNormal(tailNormal);
        headSite.setNormal(headNormal);
  
        // Recompute the head and tail points
        if (currentContext != null) {
            tailPt = tailSite.getPoint(currentContext);
            headPt = headSite.getPoint(currentContext);
        } else {
            tailPt = tailSite.getPoint();
            headPt = headSite.getPoint();
        }

        double tailx0 = tailPt.getX();
        double taily0 = tailPt.getY();
        double tailx1 = tailx0 + Math.cos(tailNormal);
        double taily1 = taily0 - Math.sin(tailNormal);
        double headx0 = headPt.getX();
        double heady0 = headPt.getY();
        double headx1 = headx0 + Math.cos(headNormal);
        double heady1 = heady0 - Math.sin(headNormal);

        // Adjust for decorations on the ends
        if (getHeadEnd() != null) {
            getHeadEnd().setNormal(headNormal);
            getHeadEnd().setOrigin(headPt.getX(), headPt.getY());
            getHeadEnd().getConnection(headPt);
        }
        if (getTailEnd() != null) {
            getTailEnd().setNormal(tailNormal);
            getTailEnd().setOrigin(tailPt.getX(), tailPt.getY());
            getTailEnd().getConnection(tailPt);
        }
 
        // Figure out the angle yet again (!)
        x = headPt.getX() - tailPt.getX();
        y = headPt.getY() - tailPt.getY();
        gamma = Math.atan2(y, x);        

        // Finally! Now that we have angle between the head and
        // tail of the connector, figure out what the center
        // of the arc is. First, compute assuming that gamma is
        // zero.
        double dx = Math.sqrt(x*x + y*y) / 2.0;
        double dy = -dx * Math.tan(beta);

        // That's the offset from the tail point to the center
        // of the arc's circle. Rotate it through gamma.
        double dxdash = dx * Math.cos(gamma) - dy * Math.sin(gamma);
        double dydash = dx * Math.sin(gamma) + dy * Math.cos(gamma);

        // Get the center
        double centerX = tailPt.getX() + dxdash;
        double centerY = tailPt.getY() + dydash;
        double radius = Math.sqrt(dx*dx + dy*dy);

        // Remember some parameters for later use
        this._centerX = centerX;
        this._centerY = centerY;
        this._radius = radius;
        this._startAngle = 3*Math.PI/2 - alpha - gamma;

        // FIXME: I don't know why I need to do this, I screwed
        // up the math somewhere... -- hjr
        if (_angle < 0) {
            _startAngle += Math.PI;
        }

        // Intersect the two normals, if they point towards eachother, then 
        // use the short side of the circle.  If they point away, 
        // then use the large side.
        // FIXME: This is ugly, but it works
        // right on the screen.  -- sn.
        double A = (tailx0-headx0);
        double B = (headx1-headx0)-(tailx1-tailx0);
        double t = A/B;
        if(B == 0 || t < 0) {
            A = (taily0-heady0);
            B = (heady1-heady0)-(taily1-taily0);
            // since the geometry is upside down.
            if(B != 0) {
                t = -A/B;
            }
       }
  
        if(t > 0) {
            _extentAngle = (2.0 * alpha);
        } else {
            _extentAngle = (2.0 * alpha) - 2 * Math.PI;
        }

        // Set the arc
        _arc.setArcByCenter(
                centerX, centerY, radius,
                _startAngle / Math.PI * 180,
                _extentAngle / Math.PI * 180,
                Arc2D.OPEN);

        // Move the label
        repositionLabel();

        // Woo-hoo!
        repaint();
    }

    /** Set the angle at which the arc leaves the tail figure,
     * in radians. Because of the sign of the geometry, an
     * arc with positive angle and with an arrowhead on
     * its head will appear to be drawn counter-clockwise, 
     * and an arc with a negative angle will apear to be
     * drawn clockwise. As a general rule, angles should be
     * somewhat less than PI/2, and PI/4 a good general maximum
     * figure.
     */
    public void setAngle (double angle) {
        _angle = angle;
    }

    /** Translate the connector. This method is implemented, since
     * controllers may wish to translate connectors when the
     * sites at both ends are moved the same distance.
     */
    public void translate (double x, double y) {
        Rectangle2D bounds = _arc.getBounds();
        repaint();
        _arc.setFrame(bounds.getX() + x, bounds.getY() + y,
                bounds.getWidth(), bounds.getHeight());
        if (getLabelFigure() != null) {
            getLabelFigure().translate(x, y);
        }
        repaint();
    }
}


