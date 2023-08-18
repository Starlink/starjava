package uk.ac.starlink.ttools.plot2.geom;

import uk.ac.starlink.ttools.plot2.Ganger;
import uk.ac.starlink.ttools.plot2.GangContext;
import uk.ac.starlink.ttools.plot2.GangerFactory;
import uk.ac.starlink.ttools.plot2.Padding;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;

/**
 * Ganger that stacks time plots vertically with a shared time axis.
 *
 * @author   Mark Taylor
 * @since    3 Feb 2016
 */
public class TimeStackGanger
        extends StackGanger<TimeSurfaceFactory.Profile,TimeAspect> {

    private static final boolean UP = false;

    /** GangerFactory instance that returns TimeStackGangers. */
    public static final GangerFactory<TimeSurfaceFactory.Profile,TimeAspect>
            FACTORY =
            new GangerFactory<TimeSurfaceFactory.Profile,TimeAspect>() {
        public boolean hasIndependentZones() {
            return true;
        }
        public ConfigKey<?>[] getGangerKeys() {
            return new ConfigKey<?>[ 0 ];
        }
        public Ganger<TimeSurfaceFactory.Profile,TimeAspect>
                createGanger( Padding padding, ConfigMap config,
                              GangContext context ) {
            return new TimeStackGanger( context.getRequestedZoneNames(),
                                        padding );
        }
    };

    /**
     * Constructor.
     *
     * @param  zoneNames   zone identifiers, one for each zone in the gang
     * @param  padding  defines user preferences, if any, for space
     *                  reserved outside each plot zone
     */
    public TimeStackGanger( String[] zoneNames, Padding padding ) {
        super( zoneNames, UP, padding );
    }

    public boolean isTrimmingGlobal() {
        return false;
    }

    public boolean isShadingGlobal() {
        return false;
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

        /* Remove some of the annotations; only the top plot should get
         * annotation along the top, and only the bottom plot should get
         * annotation along the bottom. */
        profiles = profiles.clone();
        for ( int i = 0; i < profiles.length; i++ ) { 
            boolean left = true;
            boolean right = true;
            boolean top = i == ( UP ? profiles.length - 1 : 0 );
            boolean bottom = i == ( UP ? 0 : profiles.length - 1 );
            SideFlags annotateFlags =
                new SideFlags( bottom, left, top, right );
            profiles[ i ] = profiles[ i ].fixAnnotation( annotateFlags );
        }   
        return profiles;
    }
}
