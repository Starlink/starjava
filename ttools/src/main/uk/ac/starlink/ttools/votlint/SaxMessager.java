package uk.ac.starlink.ttools.votlint;

import org.xml.sax.Locator;

/**
 * Defines how parsing messages are reported.
 *
 * @author   Mark Taylor
 * @since    29 Nov 2017
 */
public interface SaxMessager {

    /**
     * Reports a message.
     *
     * @param  level  severity level of the message, not null
     * @param  code   message identifier
     * @param  msg    message text
     * @param  locator  location in the XML document that provoked the message,
     *                  or null if unknown/inapplicable
     */
    void reportMessage( Level level, VotLintCode code, String msg,
                        Locator locator );

    /**
     * Defines the levels of severity at which messages can be reported.
     */
    public enum Level {
        INFO,
        WARNING,
        ERROR;
    }
}
