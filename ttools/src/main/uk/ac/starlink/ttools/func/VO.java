// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.ttools.func;

import uk.ac.starlink.ttools.votlint.UcdStatus;
import uk.ac.starlink.ttools.votlint.UnitStatus;

/**
 * Virtual Observatory-specific functions.
 * Some of these are for rather technical purposes.
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
     * Possible values include "OK", "BAD_SYNTAX" etc.
     * UCD1+, UCD1 and SIAv1 ("VOX:")-style UCDs are recognised.
     *
     * @param  ucd   UCD value
     * @return   "OK" for conformant UCD1+, otherwise some other value
     */
    public static String ucdStatus( String ucd ) {
        UcdStatus status = UcdStatus.getStatus( ucd );
        return status == null ? null : status.getCode().toString();
    }

    /**
     * Returns a user-readable message associated with the parsing of a UCD.
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
     * Possible values include "OK", "BAD_SYNTAX" etc.
     *
     * @param  unit  unit value
     * @return   "OK" for conformant VOUnits, otherwise some other value
     */
    public static String vounitStatus( String unit ) {
        UnitStatus status = UnitStatus.getStatus( unit );
        return status == null ? null : status.getCode().toString();
    }

    /**
     * Returns a user-readable message associated with the parsing of a
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
