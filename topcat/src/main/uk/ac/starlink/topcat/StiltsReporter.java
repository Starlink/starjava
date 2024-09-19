package uk.ac.starlink.topcat;

import java.awt.event.ActionListener;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.ttools.task.Setting;
import uk.ac.starlink.ttools.task.StiltsCommand;

/**
 * Defines a component that can generate a STILTS command,
 * assumed equivalent in some way to its current state.
 *
 * @author   Mark Taylor
 * @since    19 Sep 2024
 */
public interface StiltsReporter {

    /**
     * Returns a stilts command corresponding to current state.
     *
     * @param   tableNamer   table namer
     * @return   command, or null if current state does not correspond to one
     */
    StiltsCommand createStiltsCommand( TopcatTableNamer tableNamer );

    /**
     * Adds a listener that will be messaged if the current stilts
     * command might have changed.
     *
     * @param  listener  listener to add
     */
    void addStiltsListener( ActionListener listener );

    /**
     * Removes a listener that may have been previously added.
     *
     * @param listener  listener to remove
     */
    void removeStiltsListener( ActionListener listener );

    /**
     * Creates a setting for a given parameter and value.
     * This is simply a shorthand for {@link StiltsCommand#createParamSetting},
     * of which repeated invocations are required by StiltsReporter
     * implementations, and which is otherwise verbose.
     *
     * @param   param  task parameter
     * @param   tval   typed value for parameter
     * @return   setting object
     */
    default <T> Setting pset( Parameter<T> param, T tval ) {
        return StiltsCommand.createParamSetting( param, tval );
    }
}
