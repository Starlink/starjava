package uk.ac.starlink.vo;

import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents an IVO Identifier.
 *
 * <p>For most purposes these are strings, but case handling during
 * comparison needs some care.
 * The relevant text in Section 2.1 of the
 * <a href="https://www.ivoa.net/documents/IVOAIdentifiers/"
 *    >IVOA Identifiers standard</a> reads:
 * <blockquote>
 * "The Registry references are, as a whole, compared case-insensitively,
 * and must be treated case-insensitively throughout to maintain backwards
 * compatibility with version 1 of this specification. When comparing full
 * IVOIDs, the local part must be split off and compared preserving case,
 * while the registry part must be compared case-insensitively."
 * </blockquote>
 *
 * <p>The form of an IVOID is:
 * <pre>
 *    ivo://&lt;authority&gt;&lt;path&gt;[?&lt;query&gt;][#&lt;fragment&gt;]
 * </pre>
 * where <code>ivo://&lt;authority&gt;&lt;path&gt;</code> is known as
 * the "registry part" and
 * <code>[?&lt;query&gt;][#&lt;fragment&gt;]</code> is the "local part".
 *
 * <p>Invalid Ivoids can still be used and compared; if the text supplied
 * at construction time does not conform to the IVO ID syntax,
 * the registry and local parts will be null, and the
 * equality semantics will simply be that of (case-sensitive) strings.
 *
 * @author   Mark Taylor
 * @since    4 Jul 2023
 * @see   <a href="https://www.ivoa.net/documents/IVOAIdentifiers/20160523/"
 *           >IVOA Identifiers v2.0</a>
 */
public class Ivoid {

    private final String txt_;
    private final String registryPart_;
    private final String registrypart_;
    private final String localPart_;
    private static final Pattern IVOID_REGEX =
        Pattern.compile( "(ivo://[^?#]+)(.*)" );
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );
    private static boolean hasWarned_;

    /**
     * Constructor.  Construction will always succeed, but a valid IVO ID
     * will have a registry part beginning "ivo://...".
     *
     * @param  txt   textual representation of IVOID
     */
    public Ivoid( String txt ) {
        txt_ = txt;
        Matcher matcher = IVOID_REGEX.matcher( txt == null ? "" : txt );
        if ( matcher.matches() ) {
            registryPart_ = matcher.group( 1 );
            registrypart_ = registryPart_.toLowerCase();
            localPart_ = matcher.group( 2 );
        }
        else {
            registryPart_ = null;
            registrypart_ = null;
            localPart_ = null;
        }
    }

    /**
     * Returns the registry part of this IVOID.
     * This will begin "ivo://..." for a valid IVOID, and be null
     * otherwise.
     *
     * @return  registry part, may be null for invalid IVOID text
     */
    public String getRegistryPart() {
        return registryPart_;
    }

    /**
     * Returns the local part of this IVOID.
     * This may be empty if no local part is present,
     * or null in the case of an invalid IVOID.
     * If present and non-empty the first character will be
     * "#" or "?".
     *
     * @return  local part, may be null for invalid IVOID text,
     *          or may be the empty string for no local part
     */
    public String getLocalPart() {
        return localPart_;
    }

    /**
     * Indicates whether the registry part of this Ivoid is equivalent
     * to a supplied textual representation of another registry part.
     *
     * @param  regPart  textual representation of registry part of an IVO ID
     * @return   true iff this ivoid's registry part is equivalent
     */
    public boolean matchesRegistryPart( String regPart ) {
        return regPart != null
            && regPart.equalsIgnoreCase( registryPart_ );
    }

    /**
     * Indicates whether the string represented by this Ivoid object is
     * a valid IVO ID.  The test is more or less that it starts "ivo://...",
     * and is equivalent to testing whether
     * {@link #getRegistryPart} returns a non-null value.
     *
     * @return  true iff this object represents a valid IVO ID
     */
    public boolean isValid() {
        return registryPart_ != null;
    }

    /**
     * Returns the normalised form of this ivoid suitable for use
     * (for instance ADQL equality matching) in the context of the
     * Relational Registry.
     * See RegTAP 1.1, section 4.3.
     *
     * @return  lower-cased verson of valid IVO ID, or exact invalid text
     * @see <a href="https://www.ivoa.net/documents/RegTAP/">RegTAP</a>
     */
    public String toRegtapString() {
        return registryPart_ == null
             ? txt_
             : registryPart_.toLowerCase() + localPart_;
    }

    /**
     * Typed version of {@link #equals} method.
     *
     * @return  true iff other is equivalent to this
     */
    public boolean equalsIvoid( Ivoid other ) {
        return equals( other );
    }

    @Override
    public int hashCode() {
        return registrypart_ == null
             ? Objects.hash( txt_ )
             : Objects.hash( registrypart_, localPart_ );
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof Ivoid ) {
            Ivoid other = (Ivoid) o;
            return this.registrypart_ == null
                 ? ( other.registrypart_ == null &&
                     Objects.equals( this.txt_, other.txt_ ) )
                 : ( this.registrypart_.equals( other.registrypart_ ) &&
                     Objects.equals( this.localPart_, other.localPart_ ) );
        }
        else {

            /* Issue a warning if an equality comparison between an Ivoid
             * and a non-Ivoid object is attempted.  This is not illegal,
             * but it's most likely to happen if there is confusion in the
             * code between Ivoids and Strings, for instance calling
             * List.contains(String) on a List<Ivoid>.
             * There are a number of static type bugs along these lines
             * that will not be picked up by the compiler.
             * Unfortunately this won't pick them all up either,
             * e.g. probably not the wrong type of HashMap/Set keys. */
            if ( o != null && ! hasWarned_ ) {
                hasWarned_ = true;
                String msg = new StringBuffer()
                   .append( "At least one Ivoid/" )
                   .append( o.getClass().getName() )
                   .append( " equality comparison" )
                   .append( " - possible programming error" )
                   .toString();
                logger_.warning( msg );
            }
            return false;
        }
    }

    @Override
    public String toString() {
        return txt_;
    }
}
