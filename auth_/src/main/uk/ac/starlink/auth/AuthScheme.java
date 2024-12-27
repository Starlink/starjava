package uk.ac.starlink.auth;

import java.net.URL;

/**
 * Represents an RFC7235 authentication scheme.
 * It knows how to turn a particular class of authentication Challenges
 * into an AuthContext.
 *
 * <p>Note that instances of this class may or may not map exactly to
 * an Authentication Scheme as defined in RFC7235; a given instance
 * may only be able to respond to a subset of challenges with a
 * given scheme name.
 *
 * @author   Mark Taylor
 * @since    15 Jun 2020
 * @see  <a href="https://datatracker.ietf.org/doc/html/rfc7235">RFC 7235</a>
 */
public interface AuthScheme {

    /**
     * Returns a human-readable name identifying the authentication
     * scheme implemented by this object.
     * The returned string is typically the <code>auth-scheme</code>
     * token from an RFC7235 challenge (for instance "Basic" for Basic 
     * authentication), but a different value may be used if required
     * to distinguish it from other instances.
     *
     * @return   name for this scheme
     */
    String getName();

    /**
     * Attempts to return an object that can take user input to generate
     * an AuthContext based on a given challenge.
     * There are three possible outcomes of this method.
     * <ul>
     * <li>If this scheme recognises the challenge type and expects to be able
     *     to use it to generate AuthContexts, it should return a suitable
     *     ContextFactory</li>
     * <li>If this scheme recognises the challenge type but something
     *     is wrong with the challenge syntax (for instance missing
     *     parameters), it should throw a BadChallengeException,
     *     preferably with an explanatory message</li>
     * <li>If this scheme doesn't recognise the challenge type
     *     (for instance the challenge scheme string is not that
     *     implemented by this object), it should return null</li>
     * </ul>
     *
     * <p>Note that this method should just examine the syntax of the
     * supplied challenge; it is not expected to make network connections etc
     * to determine if context creation will be successful.
     *
     * @param  challenge   authentication challenge object
     * @param  url   URL with which the challenge is associated
     * @return   context factory if challenge is recognised,
     *           or null if it isn't
     * @throws  BadChallengeException  if the challenge scheme etc
     *          indicates that it is destined for this AuthScheme,
     *          but the challenge is not of the correct form
     */
    ContextFactory createContextFactory( Challenge challenge, URL url )
            throws BadChallengeException;
}
