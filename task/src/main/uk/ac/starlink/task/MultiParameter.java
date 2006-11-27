package uk.ac.starlink.task;

/**
 * Marker interface which marks a parameter as one which can have multiple
 * appearances on the command line.
 *
 * @author   Mark Taylor
 */
public interface MultiParameter {

    /**
     * Returns a character which is to be used as the separator between
     * values found in adjacent occurrences of the parameter in the
     * execution environment.
     */
    char getValueSeparator();
}
