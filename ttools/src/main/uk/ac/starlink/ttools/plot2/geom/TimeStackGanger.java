package uk.ac.starlink.ttools.plot2.geom;

/**
 * Ganger that stacks time plots vertically with a shared time axis.
 * This class is a singleton, see {@link #getInstance}.
 *
 * @author   Mark Taylor
 * @since    3 Feb 2016
 */
public class TimeStackGanger extends StackGanger<TimeAspect> {

    private static final TimeStackGanger INSTANCE = new TimeStackGanger();

    /**
     * Private constructor prevents public instantiation of singleton class.
     */
    private TimeStackGanger() {
        super( false );
    }

    public double[] getXLimits( TimeAspect aspect ) {
        return new double[] { aspect.getTMin(), aspect.getTMax() };
    }

    public TimeAspect fixXLimits( TimeAspect aspect,
                                  double xmin, double xmax ) {
        return new TimeAspect( new double[] { xmin, xmax },
                               new double[] { aspect.getYMin(),
                                              aspect.getYMax() } );
    }

    /**
     * Returns the sole instance of this class.
     *
     * @return   instance
     */
    public static TimeStackGanger getInstance() {
        return INSTANCE;
    }
}
