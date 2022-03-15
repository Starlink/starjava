package uk.ac.starlink.task;

import java.io.IOException;

/**
 * Defines an object which does the work of a task without any further
 * user or environment interaction.
 *
 * @author   Mark Taylor
 * @since    19 Aug 2005
 */
@FunctionalInterface
public interface Executable {

    /**
     * Performs the work defined by this object.
     */
    void execute() throws TaskException, IOException;
}
