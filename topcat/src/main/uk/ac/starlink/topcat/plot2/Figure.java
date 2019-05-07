package uk.ac.starlink.topcat.plot2;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Area;

/**
 * Defines an area on the graphics surface, including criteria for
 * determining whether a given point is or is not included in it.
 *
 * @author   Mark Taylor
 * @since    15 Feb 2019
 */
public interface Figure {

    /**
     * Returns a drawable shape representing the area defined by this
     * figure on its plotting surface.  The shape does not necessarily
     * extend beyond the plot surface's bounding rectangle, even though
     * the figure may encompass a larger area.
     * Null may be returned if this figure does not represent a non-blank area.
     *
     * @return   figure area, or null
     */
    Area getArea();

    /**
     * Paints the path defined by this figure's points.
     * This may or may not be the actual boundary of the enclosed area,
     * but it should convey to the user the choices they have made by
     * selecting the vertices.
     *
     * @param  g  destination graphics context
     */
    void paintPath( Graphics2D g );

    /**
     * Returns the array of vertices defining this figure.
     * This may or may not be the same as the array of points that went
     * into defining it, since in some cases some of the points are ignored.
     * These points may be marked visually in some way.
     *
     * @return   array of significant user-chosen vertices for this figure
     */
    Point[] getVertices();

    /**
     * Returns a generic algebraic (JEL-like) expression for determining
     * inclusion in this figure.  This should represent symbolic variables
     * (such as X, Y) rather than actually available ones.
     * Null may be returned if this figure does not represent a non-blank area.
     *
     * @return   boolean JEL inclusion expression, or null
     */
    String getExpression();

    /**
     * Returns an algebraic (JEL) expression that tests whether a point
     * from a given point cloud is contained within this figure.
     * Null may be returned if this figure does not represent a non-blank area.
     *
     * @param  cloud   the source of the data points
     * @return   boolean JEL inclusion expression, or null
     */
    String createExpression( TableCloud cloud );

    /**
     * Returns some ADQL text that represents this figure.
     * It should be straightforward for the user to adapt the resulting text
     * for use as part of an ADQL query.
     *
     * <p>At present no assumptions are specified about the ADQL version etc,
     * but a widely-applicable dialect (for instance ADQL v2.0 with
     * geometry support) should be used where possible.
     *
     * @return   ADQL representation of this figure
     */
    String getAdql();
}
