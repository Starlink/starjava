package uk.ac.starlink.votable.dom;

import org.w3c.dom.Text;

public class DelegatingText extends DelegatingCharacterData implements Text {

    private final Text base_;
    private final DelegatingDocument doc_;

    protected DelegatingText( Text base, DelegatingDocument doc ) {
        super( base, doc );
        base_ = base;
        doc_ = doc;
    }

    //
    // Level 2 implementation.
    //

    public Text splitText( int offset ) {
        return (Text) doc_.getDelegator( base_.splitText( offset ) );
    }

//DOM3     //
//DOM3     // Level 3 implementation.
//DOM3     //
//DOM3 
//DOM3     public boolean isElementContentWhitespace() {
//DOM3         return base_.isElementContentWhitespace();
//DOM3     }
//DOM3 
//DOM3     public String getWholeText() {
//DOM3         return base_.getWholeText();
//DOM3     }
//DOM3 
//DOM3     public Text replaceWholeText( String content ) {
//DOM3         return (Text) doc_.getDelegator( base_
//DOM3                                         .replaceWholeText( content ) );
//DOM3     }
}
