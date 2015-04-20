package uk.ac.starlink.vo;

import java.io.IOException;

/**
 * Interface defining callbacks for receiving asynchronously an generated value.
 * All methods of this interface are to be called on the Event Dispatch Thread.
 *
 * @author   Mark Taylor
 * @since    20 Apr 2015
 */
public interface ResultHandler<T> {

    /**
     * Indicates whether this handler is still permitted to affect the GUI.
     * If it returns false, the other methods will have no effect.
     * Called on EDT.
     *
     * @return   true iff handler is permitted to affect GUI
     */
    boolean isActive();

    /**
     * Updates GUI to indicate the result is in the process of being acquired.
     * Called on EDT. Ignored if not isActive.
     */
    void showWaiting();

    /**
     * Updates GUI with the acquired result.
     * Called on EDT. Ignored if not isActive.
     */
    void showResult( T result );

    /**
     * Updates GUI with an error obtained while trying to acquire result.
     * Called on EDT. Ignored if not isActive.
     */
    void showError( IOException error );
}
