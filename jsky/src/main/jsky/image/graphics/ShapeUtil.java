/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: ShapeUtil.java,v 1.7 2002/08/20 09:57:58 brighton Exp $
 */

package jsky.image.graphics;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.swing.JFrame;
import javax.swing.SwingConstants;

import diva.canvas.interactor.DragInteractor;
import diva.canvas.interactor.Interactor;
import diva.canvas.interactor.SelectionInteractor;
import diva.util.java2d.Polygon2D;
import jsky.coords.CoordinateConverter;
import jsky.image.gui.DivaGraphicsImageDisplay;
import jsky.util.gui.BasicWindowMonitor;


/**
 * Utility methods for generating various common Shapes for drawing figures.
 *
 * @version $Revision: 1.7 $
 * @author Allan Brighton
 */
public class ShapeUtil {

    /** Default length of an arrow */
    public static final int ARROW_SIZE = 8;

    /**
     * Return a Shape object for a "plus" (+) symbol.
     *
     * @param center the center point in screen coords
     * @param north the north point in screen coords
     * @param east the east point in screen coords
     */
    public static Shape makePlus(Point2D.Double center,
                                 Point2D.Double north,
                                 Point2D.Double east) {
        Point2D.Double south = new Point2D.Double(center.x - (north.x - center.x), center.y - (north.y - center.y));
        Point2D.Double west = new Point2D.Double(center.x + (center.x - east.x), center.y + (center.y - east.y));

        GeneralPath path = new GeneralPath();
        path.append(new Line2D.Double(east, west), false);
        path.append(new Line2D.Double(north, south), false);
        return path;
    }

    /**
     * Return a Shape object for a "cross" (x) symbol.
     *
     * @param x the center X coord in screen coords
     * @param y the center Y coord in screen coords
     * @param size the radius of the symbol
     */
    public static Shape makeCross(double x, double y, double size) {
        Point2D.Double sw = new Point2D.Double(x - size, y + size);
        Point2D.Double se = new Point2D.Double(x + size, y + size);
        Point2D.Double nw = new Point2D.Double(x - size, y - size);
        Point2D.Double ne = new Point2D.Double(x + size, y - size);

        GeneralPath path = new GeneralPath();
        path.append(new Line2D.Double(se, nw), false);
        path.append(new Line2D.Double(sw, ne), false);
        return path;
    }

    /**
     * Return a Shape object for a "triangle" symbol.
     *
     * @param x the center X coord in screen coords
     * @param y the center Y coord in screen coords
     * @param size the radius of the symbol
     */
    public static Shape makeTriangle(double x, double y, double size) {
        Point2D.Double north = new Point2D.Double(x, y - size);
        Point2D.Double sw = new Point2D.Double(x - size, y + size);
        Point2D.Double se = new Point2D.Double(x + size, y + size);

        Polygon2D.Double p = new Polygon2D.Double();
        p.moveTo(se.x, se.y);
        p.lineTo(sw.x, sw.y);
        p.lineTo(north.x, north.y);
        p.closePath();
        return p;
    }


    /**
     * Return a Shape object for a "diamond" symbol.
     *
     * @param x the center X coord in screen coords
     * @param y the center Y coord in screen coords
     * @param size the radius of the symbol
     */
    public static Shape makeDiamond(double x, double y, double size) {
        Point2D.Double north = new Point2D.Double(x, y - size);
        Point2D.Double east = new Point2D.Double(x - size, y);
        Point2D.Double south = new Point2D.Double(x, y + size);
        Point2D.Double west = new Point2D.Double(x + size, y);

        Polygon2D.Double p = new Polygon2D.Double();
        p.moveTo(east.x, east.y);
        p.lineTo(north.x, north.y);
        p.lineTo(west.x, west.y);
        p.lineTo(south.x, south.y);
        p.closePath();
        return p;
    }

    /**
     * Return a Shape object for a "square" symbol.
     *
     * @param x the center X coord in screen coords
     * @param y the center Y coord in screen coords
     * @param size the radius of the symbol
     */
    public static Shape makeSquare(double x, double y, double size) {
        return new Rectangle2D.Double(x - size, y - size, size * 2, size * 2);
    }

    /**
     * Return a Shape object for an "ellipse" symbol.
     *
     * @param x the center X coord in screen coords
     * @param y the center Y coord in screen coords
     * @param size the radius of the symbol
     */
    public static Shape makeEllipse(double x, double y, double size) {
        return new Ellipse2D.Double(x - size, y - size, size * 2, size * 2);
    }


    /**
     * Return a Shape object for an "ellipse" symbol.
     *
     * @param center the center point in screen coords
     * @param north the north point in screen coords
     * @param east the east point in screen coords
     */
    public static Shape makeEllipse(Point2D.Double center,
                                    Point2D.Double north,
                                    Point2D.Double east) {

        Point2D.Double south = new Point2D.Double(center.x - (north.x - center.x), center.y - (north.y - center.y));
        Point2D.Double west = new Point2D.Double(center.x + (center.x - east.x), center.y + (center.y - east.y));

        // XXX Note: This is not an ellipse. What we really want is a "smooth polygon"...
        GeneralPath p = new GeneralPath();
        p.moveTo((float) north.x, (float) north.y);
        p.quadTo((float) west.x, (float) west.y, (float) south.x, (float) south.y);
        p.quadTo((float) east.x, (float) east.y, (float) north.x, (float) north.y);
        p.closePath();
        return p;
    }


    /**
     * Return a Shape object for a "compass" symbol (has two lines,
     * from the center point, pointing north and east).
     *
     * @param center the center point in screen coords
     * @param north the north point in screen coords
     * @param east the east point in screen coords
     */
    public static Shape makeCompass(Point2D.Double center,
                                    Point2D.Double north,
                                    Point2D.Double east) {

        GeneralPath path = new GeneralPath();
        path.append(new Line2D.Double(center, east), false);
        addArrowLine(path, center, north);
        return path;
    }


    /**
     * Return a Shape object for the "line" symbol.
     *
     * @param center the center point in screen coords
     * @param north the north point in screen coords
     * @param east the east point in screen coords
     */
    public static Shape makeLine(Point2D.Double center,
                                 Point2D.Double north,
                                 Point2D.Double east) {

        Point2D.Double south = new Point2D.Double(center.x - (north.x - center.x), center.y - (north.y - center.y));
        Line2D.Double p = new Line2D.Double(north, south);
        return p;
    }

    /**
     * Return a Shape object for the "arrow" symbol (a line
     * from center to north with an arrow at north).
     *
     * @param center the center point in screen coords
     * @param north the north point in screen coords
     */
    public static Shape makeArrow(Point2D.Double center,
                                  Point2D.Double north) {
        GeneralPath path = new GeneralPath();
        addArrowLine(path, center, north);
        return path;
    }

    /**
     * Add a line with an arrow at the end to the given GeneralPath.
     *
     * @param path the line and arrow are added to the GeneralPath
     * @param startPos the start of the line
     * @param endPos the end of the line (where the arrow should be)
     */
    public static void addArrowLine(GeneralPath path, Point2D.Double startPos, Point2D.Double endPos) {
        path.append(new Line2D.Double(startPos, endPos), false);

        // add arrow at endPos
        double x = startPos.x - endPos.x;
        double y = startPos.y - endPos.y;
        boolean flip = (x < 0.0);
        double angle = Math.atan(y / x);
        path.append(makeArrowHead(endPos, ARROW_SIZE, angle, flip), false);
    }

    /**
     * Make and return an arrow head shape at the given position.
     *
     * @param pos the position of the point of the arrow in screen coords
     * @param length the length of the arrow
     * @param angle the rotation angle in radians
     * @param flip if true flip the arrow direction
     */
    public static Polygon2D makeArrowHead(Point2D.Double pos, double length, double angle, boolean flip) {
        AffineTransform at = new AffineTransform();
        at.setToRotation(angle, pos.x, pos.y);

        double l1 = length * 1.0;
        double l2 = length * 1.3;
        double w = length * 0.4;

        if (flip) {
            // flip arrow
            l1 = -l1;
            l2 = -l2;
            //at.translate(length, 0.0);
        }

        Polygon2D polygon = new Polygon2D.Double();
        polygon.moveTo(pos.x, pos.y);
        polygon.lineTo(
                pos.x + l2,
                pos.y + w);
        polygon.lineTo(
                pos.x + l1,
                pos.y);
        polygon.lineTo(
                pos.x + l2,
                pos.y - w);
        polygon.closePath();
        polygon.transform(at);
        return polygon;
    }

    /**
     * test main: usage: java GraphicsImageDisplay <filename>.
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("GraphicsImageDisplay");
        DivaGraphicsImageDisplay imageDisplay = new DivaGraphicsImageDisplay();
        imageDisplay.clear();

        // Add some test objects
        DivaImageGraphics g = new DivaImageGraphics(imageDisplay);
        CoordinateConverter coordinateConverter = imageDisplay.getCoordinateConverter();
        SelectionInteractor si = g.getSelectionInteractor();
        SelectionInteractor fsi = g.getFixedSelectionInteractor();
        DragInteractor di = g.getDragInteractor();
        int anchor = SwingConstants.CENTER;
        Font font = new Font("Dialog", Font.PLAIN, 10);

        Point2D.Double center = new Point2D.Double(50, 50);
        Point2D.Double north = new Point2D.Double(65, 20);
        Point2D.Double east = new Point2D.Double(35, 40);
        Shape shape = ShapeUtil.makeCompass(center, north, east);
        Color fill = Color.blue;
        Color outline = Color.yellow;
        float lineWidth = 1;
        Interactor interactor = si;

        g.add(g.makeFigure(shape, fill, outline, lineWidth, interactor));

        frame.getContentPane().add(imageDisplay, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new BasicWindowMonitor());
    }
}

