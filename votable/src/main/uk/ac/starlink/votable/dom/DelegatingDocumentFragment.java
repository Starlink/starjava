package uk.ac.starlink.votable.dom;

import org.w3c.dom.DocumentFragment;

public class DelegatingDocumentFragment extends DelegatingNode
                                        implements DocumentFragment {
    protected DelegatingDocumentFragment( DocumentFragment base,
                                       DelegatingDocument doc ) {
        super( base, doc );
    }
}
