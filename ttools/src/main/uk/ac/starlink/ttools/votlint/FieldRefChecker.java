package uk.ac.starlink.ttools.votlint;

/**
 * Attribute checker for ref attributes that point to FIELDs.
 *
 * @author   Mark Taylor (Starlink0
 * @since    7 Apr 2005
 */
public class FieldRefChecker extends RefChecker {

    public FieldRefChecker() {
        super( "FIELD" );
    }

    public void checkLink( VotLintContext context,
                           ElementRef from, ElementRef to ) {
        assert from.getHandler() instanceof FieldHandler;
        String toName = to.getName();

        /* Ensure that the reference is to a different FIELD in the same
         * TABLE - if not it doesn't make much sense. */
        if ( to.getHandler() instanceof FieldHandler ) {
            ElementRef fromTable = 
                ((FieldHandler) from.getHandler()).getTableRef();
            ElementRef toTable =
                ((FieldHandler) to.getHandler()).getTableRef();
            if ( fromTable == null || toTable == null || 
                 ! fromTable.equals( toTable ) ) {
                context.warning( new VotLintCode( "RFT" ),
                                 from + " has ref to " + toName + 
                                 " in a different table" );
            }
        }
        else {
            context.warning( new VotLintCode( "RFW" ),
                             from + " has ref to element type " + toName + 
                             " - not meaningful" );
        }
    }
}
