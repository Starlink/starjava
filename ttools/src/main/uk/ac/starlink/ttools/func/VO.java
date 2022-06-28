// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.ttools.func;

import uk.ac.starlink.vo.UcdStatus;
import uk.ac.starlink.vo.UnitStatus;

/**
 * Virtual Observatory-specific functions.
 * Some of these are for rather technical purposes.
 *
 * <p>The UCD parsing functions are based on Gr\u00e9gory Mantelet's library
 * <a href="https://github.com/gmantele/ucidy">Ucidy</a>
 * corresponding to
 * <a href="https://www.ivoa.net/documents/UCD1+/20210616/">UCD1+ 1.4</a>,
 * and the VOUnit parsing functions are based on Norman Gray's library
 * <a href="http://www.astro.gla.ac.uk/users/norman/ivoa/unity/">Unity</a>
 * corresponding to
 * <a href="https://www.ivoa.net/documents/VOUnits/20140523/">VOUnits 1.0</a>.
 *
 * @author   Mark Taylor
 * @since    8 Jul 2021
 */
public class VO {

    /**
     * Private constructor prevents instantiation.
     */
    private VO() {
    }

    /**
     * Returns a token string giving the category of UCD compliance.
     * UCD1+, UCD1 and SIAv1 ("VOX:")-style UCDs are recognised.
     *
     * <p>Possible return values are currently:
     * <ul>
     * <li>"OK":
     *        conforms to UCD1+ standard</li>
     * <li>"UCD1":
     *        looks like a UCD1</li>
     * <li>"VOX":
     *        is in VOX: namespace introduced by SIAv1</li>
     * <li>"BAD_SYNTAX":
     *         not a UCD1 and cannot be parsed according to UCD1+</li>
     * <li>"BAD_SEQUENCE":
     *         UCD words violate UCD1+ sequence rules</li>
     * <li>"UNKNOWN_WORD":
     *         follows UCD1+ syntax rules but contains non-UCD1+ atom</li>
     * <li>"NAMESPACE":
     *         follows UCD1+ syntax but contains atoms in
     *         non-standard namespace</li>
     * <li>"DEPRECATED":
     *         contains deprecated UCD1+ words</li>
     * </ul>
     *
     * <p>In the case of non-compliant values, more information can be found
     * using the <code>ucdMessage</code> function.
     *
     * @param  ucd   UCD string
     * @return   "OK" for conformant UCD1+, otherwise some other value
     */
    public static String ucdStatus( String ucd ) {
        UcdStatus status = UcdStatus.getStatus( ucd );
        return status == null ? null : status.getCode().toString();
    }

    /**
     * Returns a human-readable message associated with the parsing of a UCD.
     * This is expected to be empty for a fully compliant UCD,
     * and may contain error messages or advice otherwise.
     *
     * @param  ucd   UCD value
     * @return   message text
     */
    public static String ucdMessage( String ucd ) {
        UcdStatus status = UcdStatus.getStatus( ucd );
        return status == null ? null : status.getMessage();
    }

    /**
     * Returns a token string giving the category of VOUnits compliance.
     *
     *
     * <p>Possible return values are currently:
     * <ul>
     * <li>"OK":
     *        conforms to VOUnits syntax</li>
     * <li>"UNKNOWN_UNIT":
     *        parsed as VOUnit but contains unknown base unit(s)</li>
     * <li>"GUESSED_UNIT":
     *        parsed as VOUnit but contains unknown, though guessable,
     *        base unit(s)</li>
     * <li>"BAD_SYNTAX":
     *        cannot be parsed as VOUnit syntax</li>
     * <li>"PARSE_ERROR":
     *        unexpected error during parsing</li>
     * <li>"USAGE":
     *        violates VOUnit usage constraints</li>
     * <li>"WHITESPACE":
     *        legal VOUnit except that it contains whitespace,
     *        which is not allowed by VOUnits</li>
     * </ul>
     *
     * <p>In the case of non-compliant values, more information can be found
     * using the <code>vounitMessage</code> function.
     *
     * @param  unit  unit string
     * @return   "OK" for conformant VOUnits, otherwise some other value
     */
    public static String vounitStatus( String unit ) {
        UnitStatus status = UnitStatus.getStatus( unit );
        return status == null ? null : status.getCode().toString();
    }

    /**
     * Returns a human-readable message associated with the parsing of a
     * unit string.
     * This is expected to be empty for a string fully compliant with VOUnits,
     * and may contain error messages or additional information otherwise.
     *
     * @param  unit  unit string
     * @return   message text
     */
    public static String vounitMessage( String unit ) {
        UnitStatus status = UnitStatus.getStatus( unit );
        return status == null ? null : status.getMessage();
    }
}
