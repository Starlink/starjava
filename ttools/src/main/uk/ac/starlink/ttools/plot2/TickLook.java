package uk.ac.starlink.ttools.plot2;

import java.awt.Graphics2D;
import uk.ac.starlink.ttools.plot2.Equality;

/**
 * Defines the appearance of tick marks on a plot axis.
 *
 * @author   Mark Taylor
 * @since    21 Dec 2022
 */
public abstract class TickLook {

    private final String name_;

    /** Old-style look - major tickmarks extend above and below axis. */
    public static final TickLook CLASSIC = createClassicLook( "classic", 2 );

    /** Standard look - major tickmarks are twice as high as minor. */
    public static final TickLook STANDARD = createStandardLook( "standard", 2 );

    /** No ticks are drawn. */
    public static final TickLook NONE = new TickLook( "none" ) {
        public void drawMinor( Graphics2D g2 ) {
        }
        public void drawMajor( Graphics2D g2 ) {
        }
    };

    /**
     * Constructor.
     *
     * @param  name  style name
     */
    protected TickLook( String name ) {
        name_ = name;
    }

    /**
     * Draws a minor tickmark at the origin, with the axis considered
     * horizontal and the plot in the direction of the positive Y axis.
     *
     * @param  g2  graphics context
     */
    public abstract void drawMinor( Graphics2D g2 );

    /**
     * Draws a major tickmark at the origin, with the axis considered
     * horizontal and the plot in the direction of the positive Y axis.
     *
     * @param  g2  graphics context
     */
    public abstract void drawMajor( Graphics2D g2 );

    /**
     * Returns the name of this style.
     *
     * @return  look name
     */
    public String getName() {
        return name_;
    }

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Returns a look with major ticks extending both below and above
     * the axis, minor ticks only above.
     *
     * @param  name  style name
     * @param  unit  length in pixels of minor ticks
     * @return  new instance
     */
    public static TickLook createClassicLook( String name, int unit ) {
        return new LineTickLook( name, 0, unit, -unit, unit );
    }

    /**
     * Returns a look with major ticks twice as long as minor ones,
     * all ticks only extending above the axis.
     *
     * @param  name  style name
     * @param  unit  length in pixels of minor ticks
     * @return  new instance
     */
    public static TickLook createStandardLook( String name, int unit ) {
        return new LineTickLook( name, 0, unit, 0, 2 * unit );
    }

    /**
     * Returns a look based just one drawing vertical lines.
     */
    @Equality
    private static class LineTickLook extends TickLook {

        private final int minLo_;
        private final int minHi_;
        private final int majLo_;
        private final int majHi_;

        /**
         * Constructor.
         *
         * @param  name  look name
         * @param  minLo  lower Y value for minor tick
         * @param  minHi  upper Y value for minor tick
         * @param  majLo  lower Y value for major tick
         * @param  majHi  upper Y value for major tick
         */
        public LineTickLook( String name,
                             int minLo, int minHi, int majLo, int majHi ) {
            super( name );
            minLo_ = minLo;
            minHi_ = minHi;
            majLo_ = majLo;
            majHi_ = majHi;
        }

        public void drawMinor( Graphics2D g2 ) {
            g2.drawLine( 0, minLo_, 0, minHi_ );
        }

        public void drawMajor( Graphics2D g2 ) {
            g2.drawLine( 0, majLo_, 0, majHi_ );
        }

        @Override
        public int hashCode() {
            int code = 99801;
            code = 23 * code + minLo_;
            code = 23 * code + minHi_;
            code = 23 * code + majLo_;
            code = 23 * code + majHi_;
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof LineTickLook ) {
                LineTickLook other = (LineTickLook) o;
                return this.minLo_ == other.minLo_
                    && this.minHi_ == other.minHi_
                    && this.majLo_ == other.majLo_
                    && this.majHi_ == other.majHi_;
            }
            else {
                return false;
            }
        }
    }
}
