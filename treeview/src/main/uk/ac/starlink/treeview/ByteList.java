package uk.ac.starlink.treeview;

/**
 * Extensible buffer of bytes.  This does for <tt>byte</tt>s what a 
 * <tt>StringBuffer</tt> does for <tt>char</tt>s; it gives you a 
 * buffer of byte primitives which can be extended automatically
 * when appends are made.  This implementation is based on a StringBuffer.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ByteList {

    private final StringBuffer bytebag;
    private int length = 0;
  
    public ByteList() {
        bytebag = new StringBuffer();
    }

    public ByteList( int length ) {
        bytebag = new StringBuffer( ( length + 1 ) / 2 );
    }

    public void append( byte b ) {
        if ( length % 2 == 0 ) {
            bytebag.append( (char) ( b & 0xff ) );
            length++;
        }
        else {
            int cpos = length / 2;
            char c1 = bytebag.charAt( cpos );
            char c2 = (char) ( c1 | ( ((char) b) << 8 ) );
            bytebag.setCharAt( cpos, c2 );
            length++;
        }
    }

    public int length() {
        return length;
    }

    public byte get( int index ) {
        char ch = bytebag.charAt( index / 2 );
        if ( index % 2 == 0 ) {
            return (byte) ( ch & 0xff );
        }
        else {
            return (byte) ( ch >>> 8 );
        }
    }

}
