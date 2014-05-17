package uk.ac.starlink.ttools.task;

import uk.ac.starlink.ttools.cone.ServiceFindMode;

/**
 * Mode for upload crossmatches corresponding to the user options.
 * This is related to the ServiceFindMode, but not in a 1:1 fashion.
 *
 * @author   Mark Taylor
 * @since    5 Jun 2014
 */
public class UserFindMode {

    private final String name_;
    private final String summary_;
    private final ServiceFindMode serviceMode_;
    private final boolean oneToOne_;

    /** All matches. */
    public static final UserFindMode ALL;

    /** Best match only. */
    public static final UserFindMode BEST;

    /** Best match in local table for each remote table row. */
    public static final UserFindMode BEST_REMOTE;

    /** One output row per local table row, best match or blank. */
    public static final UserFindMode EACH;

    /** One output row per local table row, best score only or blank. */
    public static final UserFindMode EACH_SCORE;

    /** All useful instances. */
    private static final UserFindMode[] INSTANCES = {
        ALL =
            new UserFindMode( "all", "All matches",
                              ServiceFindMode.ALL, false ),
        BEST =
            new UserFindMode( "best",
                              "Matched rows, best remote row for "
                            + "each input row",
                              ServiceFindMode.BEST, false ),
        BEST_REMOTE =
            new UserFindMode( "best-remote",
                              "Matched rows, best input row for "
                            + "each remote row",
                              ServiceFindMode.BEST_REMOTE, false ),
        EACH =
            new UserFindMode( "each",
                              "One row per input row, "
                            + "contains best remote match or blank",
                              ServiceFindMode.BEST, true ),
        EACH_SCORE =
            new UserFindMode( "each-dist",
                              "One row per input row, "
                            + "column giving distance only "
                            + "for best match",
                              ServiceFindMode.BEST_SCORE, true ),
    };

    /**
     * Constructor.
     *
     * @param  name  mode name
     * @param  summary  mode summary
     * @param  serviceMode  ServiceFindMode instance underlying this function
     * @param  oneToOne   true iff output rows match 1:1 with input rows
     */
    private UserFindMode( String name, String summary,
                          ServiceFindMode serviceMode, boolean oneToOne ) {
        name_ = name;
        summary_ = summary;
        serviceMode_ = serviceMode;
        oneToOne_ = oneToOne;
        if ( oneToOne && ! serviceMode.supportsOneToOne() ) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Returns the one-word name for this mode.
     *
     * @return  mode name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns a short text summary of the meaning of this mode.
     *
     * @return  mode summary
     */
    public String getSummary() {
        return summary_;
    }

    /**
     * Returns the service mode associated with this user mode.
     *
     * @return   service mode
     */
    public ServiceFindMode getServiceMode() {
        return serviceMode_;
    }

    /**
     * Indicates whether this mode describes a match for which the count
     * and sequence of the output table rows are in one to one correspondence
     * with the input table rows.
     *
     * @return  true iff output rows match 1:1 with input rows
     */
    public boolean isOneToOne() {
        return oneToOne_;
    }

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Returns a list of all the useful instances of this class.
     */
    public static UserFindMode[] getInstances() {
        return INSTANCES.clone();
    };
}
