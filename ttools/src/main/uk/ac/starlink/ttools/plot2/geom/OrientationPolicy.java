package uk.ac.starlink.ttools.plot2.geom;

import uk.ac.starlink.ttools.plot2.Orientation;

/**
 * Characterises choices about the orientations of axis numeric labels.
 *
 * @author   Mark Taylor
 * @since    10 Sep 2024
 */
public class OrientationPolicy {

    private final String name_;
    private final String description_;
    private final Orientation[] xorients_;
    private final Orientation[] yorients_;
    private final Orientation[] x2orients_;
    private final Orientation[] y2orients_;

    private static final double ANGLE = 45;

    /** All X labels are horizontal. */
    public static final OrientationPolicy HORIZONTAL =
        new OrientationPolicy( "horizontal",
                               "axis labels are horizontal",
                               new Orientation[] { Orientation.X },
                               new Orientation[] { Orientation.Y },
                               new Orientation[] { Orientation.ANTI_X },
                               new Orientation[] { Orientation.ANTI_Y } );

    /** All X labels are at an angle. */
    public static final OrientationPolicy ANGLED =
        new OrientationPolicy( "angled",
                               "axis labels are angled",
                               new Orientation[] {
                                   Orientation.createAngledX( ANGLE, false ),
                               },
                               new Orientation[] { Orientation.Y },
                               new Orientation[] {
                                   Orientation.createAngledX( ANGLE, true ),
                               },
                               new Orientation[] { Orientation.ANTI_Y } );

    /** X labels may be horizontal or angled depending on crowding. */
    public static final OrientationPolicy ADAPTIVE =
        new OrientationPolicy( "adaptive",
                               "axis labels are horizontal if possible, "
                             + "but angled if necessary to fit more in",
                               new Orientation[] {
                                   Orientation.X,
                                   Orientation.createAngledX( ANGLE, false ),
                               },
                               new Orientation[] { Orientation.Y },
                               new Orientation[] {
                                   Orientation.ANTI_X,
                                   Orientation.createAngledX( ANGLE, true ),
                               },
                               new Orientation[] { Orientation.ANTI_Y } );

    /** List of known/useful options. */
    private static final OrientationPolicy[] OPTIONS =
        { HORIZONTAL, ANGLED, ADAPTIVE, };

    /**
     * Constructor.
     *
     * @param  name  policy name
     * @param  description  short user-directed description of policy
     * @param  xorients   acceptable orientations for X axis labels,
     *                    in order of preference
     * @param  yorients   acceptable orientations for Y axis labels,
     *                    in order of preference
     * @param  x2orients  acceptable orientations for secondary X axis labels,
     *                    in order of preference
     * @param  y2orients  acceptable orientations for secondary Y axis labels,
     *                    in order of preference
     */
    public OrientationPolicy( String name, String description,
                              Orientation[] xorients,
                              Orientation[] yorients,
                              Orientation[] x2orients,
                              Orientation[] y2orients ) {
        name_ = name;
        description_ = description;
        xorients_ = xorients;
        yorients_ = yorients;
        x2orients_ = x2orients;
        y2orients_ = y2orients;
    }

    /**
     * Returns the name of this policy.
     *
     * @return  name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns the description of this policy.
     *
     * @return  user-directed description
     */
    public String getDescription() {
        return description_;
    }

    /**
     * Returns acceptable orientations for X axis labels,
     * in order of preference.
     *
     * @return  ordered orientation list
     */
    public Orientation[] getOrientationsX() {
        return xorients_.clone();
    }

    /**
     * Returns acceptable orientations for Y axis labels,
     * in order of preference.
     *
     * @return  ordered orientation list
     */
    public Orientation[] getOrientationsY() {
        return yorients_.clone();
    }

    /**
     * Returns acceptable orientations for secondary X axis labels,
     * in order of preference.
     *
     * @return  ordered orientation list
     */
    public Orientation[] getOrientationsX2() {
        return x2orients_.clone();
    }

    /**
     * Returns acceptable orientations for secondary Y axis labels,
     * in order of preference.
     *
     * @return  ordered orientation list
     */
    public Orientation[] getOrientationsY2() {
        return y2orients_.clone();
    }

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Returns a list of the available options.
     *
     * @return  policy options
     */
    public static OrientationPolicy[] getOptions() {
        return OPTIONS.clone();
    }
}
