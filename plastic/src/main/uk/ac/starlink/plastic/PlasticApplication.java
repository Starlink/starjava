package uk.ac.starlink.plastic;

import java.net.URI;
import org.votech.plastic.PlasticListener;

/**
 * Defines the behaviour of a PLASTIC application.
 * This interface is for use with the one-line registration methods
 * found in {@link PlasticUtils}.
 *
 * @author   Mark Taylor
 * @since    17 Feb 2006
 */
public interface PlasticApplication extends PlasticListener {

    /**
     * Returns the application generic name.
     *
     * @return  name
     */
    String getName();

    /**
     * Returns the messages which the application will support.
     * An empty array means all messages.
     *
     * @return   supported message IDs
     */
    URI[] getSupportedMessages();
}
