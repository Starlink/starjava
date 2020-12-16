package uk.ac.starlink.ecsv;

import java.io.IOException;
import java.util.logging.Logger;
import uk.ac.starlink.table.TableFormatException;

/**
 * Defines how to deliver a message to the user.
 *
 * @author   Mark Taylor
 * @since    16 Dec 2020
 */
public enum MessagePolicy {

    /** Does nothing. */
    IGNORE() {
        void deliverMessage( String msg ) {
        }
    },

    /** Logs through logging system as a WARNING. */
    WARN() {
        void deliverMessage( String msg ) {
            Logger.getLogger( "uk.ac.starlink.ecsv" ).warning( msg );
        }
    },

    /** Throws an IOException. */
    FAIL() {
        void deliverMessage( String msg ) throws IOException {
            throw new TableFormatException( msg );
        }
    };

    /**
     * Does something with a user message.
     *
     * @param  msg  message text
     */
    abstract void deliverMessage( String msg ) throws IOException;
}
