package uk.ac.starlink.ttools.build;

import java.util.HashSet;
import java.util.Set;

/**
 * Set of identifiers used for class members during documentation.
 *
 * @author   Mark Taylor
 * @since    23 Jan 2023
 */
public class MemberIdSet {

    private final Set<String> ids_;

    /**
     * Constructor.
     */
    public MemberIdSet() {
        ids_ = new HashSet<String>();
    }

    /**
     * Returns an identifier not yet used for a given text string
     * if possible.
     * The return value is syntactically an XML identifier based on
     * the input, and is different from all previous returns from
     * this method of this class.  If the presented text has been
     * presented before, null is returned.
     *
     * @param  text  input string
     * @return  XML-friendy identifier, or null
     */
    public String getUniqueId( String text ) {
        String memberId = text.replaceFirst( "[^a-zA-Z_].*", "" );
        return ids_.add( memberId ) ? memberId : null;
    }
}
