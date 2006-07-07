package uk.ac.starlink.votable.dom;

import org.w3c.dom.CharacterData;

public class DelegatingCharacterData extends DelegatingNode
        implements CharacterData {

    private final CharacterData base_;
    private final DelegatingDocument doc_;

    protected DelegatingCharacterData( CharacterData base,
                                       DelegatingDocument doc ) {
        super( base, doc );
        base_ = base;
        doc_ = doc;
    }

    public String getData() {
        return base_.getData();
    }

    public void setData( String data ) {
        base_.setData( data );
    }

    public int getLength() {
        return base_.getLength();
    }

    public String substringData( int offset, int count ) {
        return base_.substringData( offset, count );
    }

    public void appendData( String arg ) {
        base_.appendData( arg );
    }

    public void insertData( int offset, String arg ) {
        base_.insertData( offset, arg );
    }

    public void deleteData( int offset, int count ) {
        base_.deleteData( offset, count );
    }

    public void replaceData( int offset, int count, String arg ) {
        base_.replaceData( offset, count, arg );
    }
}
