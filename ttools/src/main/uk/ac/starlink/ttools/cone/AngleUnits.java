package uk.ac.starlink.ttools.cone;

/**
 * Defines a unit of angle measurement.
 *
 * @author   Mark Taylor
 * @since    5 Nov 2007
 */
public class AngleUnits {

    /** Degrees. */
    public static final AngleUnits DEGREES =
        new AngleUnits( "deg", 360. );

    /** Radians. */
    public static final AngleUnits RADIANS =
        new AngleUnits( "rad", 2.0 * Math.PI );

    private final String name_;
    private final double circle_;

    /**
     * Constructor.
     *
     * @param  name  unit name
     * @param  circle  number of units in an entire revolution
     */
    public AngleUnits( String name, double circle ) {
        name_ = name;
        circle_ = circle;
    }

    /**
     * Returns the number of these units in an entire revolution.
     *
     * @return  quantity in a circle
     */
    public double getCircle() {
        return circle_;
    }

    /**
     * Returns the name of this unit.
     *
     * @return  name
     */
    public String getName() {
        return name_;
    }

    public boolean equals( Object other ) {
        return other instanceof AngleUnits && 
               ((AngleUnits) other).getCircle() == this.getCircle();
    }

    public int hashCode() {
        return Float.floatToIntBits( (float) circle_ );
    }
}
