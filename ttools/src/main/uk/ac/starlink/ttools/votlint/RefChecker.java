package uk.ac.starlink.ttools.votlint;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Attribute checker for checking XML IDREF type attributes.
 * Facilities are provided for checking that the reference is to elements
 * of a certain sort (for instance, there's no sense in getting a 
 * GROUP to reference an INFO).
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Apr 2005
 */
public class RefChecker implements AttributeChecker {

    private final Set<String> legalReferents_;

    /**
     * Constructs a RefChecker which can only sensibly reference a given
     * list of element types.
     *
     * @param   legalReferents  list of element local names which this
     *          ref can sensibly point to
     */
    public RefChecker( String[] legalReferents ) {
        legalReferents_ =
            new HashSet<String>( Arrays.asList( legalReferents ) );
    }

    /**
     * Constructs a RefChecker which can only sensibly reference a single
     * given element type.
     *
     * @param   legalReferent   sole element local name which this ref can
     *          sensibly point to
     */
    public RefChecker( String legalReferent ) {
        this( new String[] { legalReferent } );
    }

    public void check( String id, ElementHandler handler ) {
        ElementRef from = handler.getRef();
        handler.getContext().registerRef( id, from, this );
    }

    /**
     * Checks that an IDREF-&gt;ID arc is sensible.
     *
     * @param   context  lint context
     * @param   id     ID value
     * @param   from   element with IDREF value of id
     * @param   to     element with ID value of id
     */
    public void checkLink( VotLintContext context, String id,
                           ElementRef from, ElementRef to ) {
        String toName = to.getName();
        String fromName = from.getName();
        if ( ! legalReferents_.contains( toName ) ) {
            context.warning( new VotLintCode( "WRA" ),
                             fromName + " has ref '" + id + 
                             "' to element type " + toName +
                             " - is this meaningful?" );
        }
    }
}
