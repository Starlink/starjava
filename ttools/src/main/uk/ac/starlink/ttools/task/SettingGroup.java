package uk.ac.starlink.ttools.task;

/**
 * Represents a set of parameter-value pairs to be grouped together
 * for display purposes.  This aggregates a list of setting objects
 * and a 'grouping level'.  The level corresponds roughly to the
 * amount of indentation to be used when exported to text.
 * The level starts at 1 for task-level parameter settings
 * and should generally be incremented in 1s.
 *
 * @author   Mark Taylor
 * @since    26 Sep 2017
 */
public class SettingGroup {

    private final int level_;
    private final Setting[] settings_;

    /**
     * Constructor.
     *
     * @param  level  grouping level
     * @param  settings  settings in the group
     */
    public SettingGroup( int level, Setting[] settings ) {
        level_ = level;
        settings_ = settings;
    }

    /**
     * Returns the grouping level.
     *
     * @return  level
     */
    public int getLevel() {
        return level_;
    }

    /**
     * Returns the grouped settings.
     *
     * @return settings
     */
    public Setting[] getSettings() {
        return settings_;
    }
}
