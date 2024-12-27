
/**
 * Manages authentication for HTTP(S) resources in accordance
 * with VO standards.
 * 
 * <p>The main user-visible class of this package is
 * {@link uk.ac.starlink.auth.AuthManager}.
 * In general there is one user-visible instance of this class,
 * obtained from {@link uk.ac.starlink.auth.AuthManager#getInstance},
 * and its {@link uk.ac.starlink.auth.AuthManager#openStream openStream}
 * method can be used as a drop-in replacement for
 * {@link java.net.URL#openStream};
 * other methods are available for more nuanced HTTP interactions.
 * If this is done, when a resource is encountered which issues a
 * 401 or 403 challenge of a recognised type, the user will be queried
 * for credentials, which are used to acquire the resource in question,
 * and also for subsequent resources from the same domain, so that
 * multiple logins to the same domain are not required.
 *
 * <p>Some details about the above outline:
 * </p>
 * <ul>
 * <li>The meaning of the term "<i>domain</i>"
 *     is dependent on the authentication scheme in use.
 *     </li>
 * <li>The authentication schemes are recognised according to the
 *     <code>WWW-Authenticate</code> challenge headers accompanying
 *     an HTTP response (typcally, though not necessarily, 401/403).
 *     These recognised schemes are implementations of the
 *     {@link uk.ac.starlink.auth.AuthScheme} interface,
 *     of which several are provided, and of which a list may be
 *     configured on the <code>AuthManager</code>.
 *     </li>
 * <li>The nature of the query to the user is determined by the
 *     {@link uk.ac.starlink.auth.UserInterface} implementation
 *     installed on the AuthManager
 *     (see {@link uk.ac.starlink.auth.AuthManager#setUserInterface
 *                                     AuthManager.setUserInterface}).
 *     </li>
 * </ul>
 *
 * <p>This package relies on VO standards that are still under discussion.
 * The behaviour and user interface may change in future releases,
 * and at time of writing not all data services that require or offer
 * authentication advertise it in a way that AUTH can work with.
 * It is hoped that authentication interoperability will improve
 * in future versions of this library and of server-side software.
 *
 * <p>This package has no external dependencies, and may, depending on
 * user demand, be released in future as a standalone package.
 *
 * @author   Mark Taylor
 * @since    31 Oct 2023
 */
package uk.ac.starlink.auth;
