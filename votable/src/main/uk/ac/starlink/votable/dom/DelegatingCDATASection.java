package uk.ac.starlink.votable.dom;

import org.w3c.dom.CDATASection;

public class DelegatingCDATASection extends DelegatingText 
                                    implements CDATASection {
    protected DelegatingCDATASection( CDATASection base,
                                      DelegatingDocument doc ) {
        super( base, doc );
    }
}
