package uk.ac.starlink.votable.dom;

import org.w3c.dom.Comment;

public class DelegatingComment extends DelegatingCharacterData
                               implements Comment {
    protected DelegatingComment( Comment base, DelegatingDocument doc ) {
        super( base, doc );
    }
}
