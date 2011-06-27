package uk.ac.starlink.ttools.taplint;

import java.net.URL;

/**
 * Represents a stage of validator processing.
 *
 * @author   Mark Taylor
 * @since    3 Jun 2011
 */
public interface Stage {

    /**
     * Returns a short one-line description of this stage.
     *
     * @return   description in imperative mood
     */
    String getDescription();

    /**
     * Performs the validation checks for this stage.
     *
     * @param  reporter   destination for validation messages
     * @param  serviceUrl  TAP service URL
     */
    void run( Reporter reporter, URL serviceUrl );
}
