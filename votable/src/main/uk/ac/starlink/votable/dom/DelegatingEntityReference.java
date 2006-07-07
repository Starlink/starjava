package uk.ac.starlink.votable.dom;

import org.w3c.dom.EntityReference;

public class DelegatingEntityReference extends DelegatingNode
                                       implements EntityReference {
    protected DelegatingEntityReference( EntityReference base,
                                         DelegatingDocument doc ) {
        super( base, doc );
    }
}
