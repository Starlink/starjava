package uk.ac.starlink.ttools.plot2.geom;

/**
 * Ganger that stacks time plots vertically with a shared time axis.
 * This class is a singleton, see {@link #getInstance}.
 *
 * @author   Mark Taylor
 * @since    3 Feb 2016
 */
public class TimeStackGanger
        extends StackGanger<TimeSurfaceFactory.Profile,TimeAspect> {

    private static final TimeStackGanger INSTANCE = new TimeStackGanger();
    private static final boolean UP = false;

    /**
     * Private constructor prevents public instantiation of singleton class.
     */
    private TimeStackGanger() {
        super( UP );
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

    @Override
    public TimeSurfaceFactory.Profile[]
            adjustProfiles( TimeSurfaceFactory.Profile[] profiles ) {

        /* Only the bottom plot gets horizontal axis labels. */
        profiles = profiles.clone();
        for ( int i = 0; i < profiles.length; i++ ) { 
            if ( UP ? i > 0 : i < profiles.length - 1 ) {
                profiles[ i ] = profiles[ i ].fixTimeAnnotation( false );
            }
        }   
        return profiles;
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
