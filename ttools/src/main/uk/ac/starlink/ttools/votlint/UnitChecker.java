package uk.ac.starlink.ttools.votlint;

import uk.ac.starlink.vo.UnitStatus;

/**
 * Attribute checker that checks unit strings against the VOUnits standard.
 *
 * <p>Note that VOTable does not require the unit attribute to conform
 * to the VOUnits standard, it merely recoommends that.
 * So reports from this checker are only advisory.
 *
 * @author   Mark Taylor
 * @since    8 Jul 2021
 * @see <a href="https://www.ivoa.net/documents/VOUnits/">VOUnits</a>
 */
public class UnitChecker implements AttributeChecker {

    /** Singleton instance. */
    public static final UnitChecker INSTANCE = new UnitChecker();

    private UnitChecker() {
    }

    public void check( String unit, ElementHandler handler ) {
        VotLintContext context = handler.getContext();
        if ( context.isCheckUnit() ) {
            UnitStatus status = UnitStatus.getStatus( unit );
            if ( status != null ) {
                UnitStatus.Code code = status.getCode();
                if ( code.isError() || code.isWarning() ) {
                    context.warning( new VotLintCode( "VOU" ),
                                     "Bad VOUnit \"" + unit + "\""
                                   + " (" + code + "): "
                                   + status.getMessage() );
                }
            }
        }
    }
}
