package uk.ac.starlink.ttools.plot2.layer;

import java.util.Comparator;
import java.awt.geom.Point2D;

/**
 * Represents one of the available plot axes.
 *
 * @author   Mark Taylor
 * @since    18 Jun 2018
 */
public enum AxisOpt {

    X( new Comparator<Point2D>() {
        public int compare( Point2D p1, Point2D p2 ) {
            return (int) Math.signum( p1.getX() - p2.getX() );
        }
    } ) {
        double getAxisValue( XYArrayData xyData, int index ) {
            return xyData.getX( index );
        }
    },

    Y( new Comparator<Point2D>() {
        public int compare( Point2D p1, Point2D p2 ) {
            return (int) Math.signum( p1.getY() - p2.getY() );
        }
    } ) {
        double getAxisValue( XYArrayData xyData, int index ) {
            return xyData.getY( index );
        }
    },

    TIME( new Comparator<Point2D>() {
        public int compare( Point2D p1, Point2D p2 ) {
            return (int) Math.signum( p1.getX() - p2.getX() );
        }
    } ) {
        double getAxisValue( XYArrayData xyData, int index ) {
            return xyData.getX( index );
        }
    };

    private final Comparator<Point2D> pointComparator_;

    /**
     * Constructor.
     *
     * @param   pointComparator  comparator for sorting points along
     *          the axis defined by this object
     */
    private AxisOpt( Comparator<Point2D> pointComparator ) {
        pointComparator_ = pointComparator;
    }

    /**
     * Returns a comparator for sorting points along the axis defined
     * by this object.
     *
     * @return   point comparator
     */
    Comparator<Point2D> pointComparator() {
        return pointComparator_;
    }

    /**
     * Returns the value of the axis represented by this option at
     * a given index on a given XYArrayData object.
     *
     * @param  xyData  array data
     * @param  index   index
     * @return  value of this axis at index
     */
    abstract double getAxisValue( XYArrayData xyData, int index );
}
