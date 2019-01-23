package uk.ac.starlink.ttools.plot2;

import java.awt.geom.Point2D;

/**
 * Aggregates a line in graphics coordinates and its annotation.
 * The annotation is intended for human consumption.
 *
 * @author   Mark Taylor
 * @since    23 Jan 2019
 */
public class LabelledLine {

    private final Point2D gp0_;
    private final Point2D gp1_;
    private final String label_;

    /**
     * Constructor.
     *
     * @param  gp0  start point in graphics space
     * @param  gp1  end point in graphics space
     * @param  label  human-readable annotation for line
     */
    public LabelledLine( Point2D gp0, Point2D gp1, String label ) {
        gp0_ = gp0;
        gp1_ = gp1;
        label_ = label;
    }

    /**
     * Returns start point.
     *
     * @return  line start point in graphics space
     */
    public Point2D getPoint0() {
        return gp0_;
    }

    /**
     * Returns end point.
     *
     * @return  line end point in graphics space
     */
    public Point2D getPoint1() {
        return gp1_;
    }

    /**
     * Returns annotation.
     *
     * @return  human-readable label for line
     */
    public String getLabel() {
        return label_;
    }

    @Override
    public String toString() {
        return label_ + ": " + gp0_ + "->" + gp1_;
    }
}
