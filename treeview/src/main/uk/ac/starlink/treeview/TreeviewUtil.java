package uk.ac.starlink.treeview;

/**
 * Miscellaneous utilities.
 */
public class TreeviewUtil {

    /**
     * Indicates whether the bytes in a given buffer look like ASCII text
     * or not.  This is just a guess based on what characters are in there.
     *
     * @param  buf  the buffer to test
     * @return  <tt>true</tt> iff <tt>buf</tt> looks like ASCII
     */
    public static boolean isASCII( byte[] buf ) {
        int leng = buf.length;
        boolean hasUnprintables = false;
        for ( int i = 0; i < leng && ! hasUnprintables; i++ ) {
            int bval = buf[ i ];
            boolean isctl = false;
            switch( bval ) {
                case '\n':
                case '\r':
                case '\t':
                case '\f':
                case (byte) 169:  // copyright symbol
                case (byte) 163:  // pound sign
                    isctl = true;
                    break;
                default:
                    // no action
            }
            if ( bval > 126 || bval < 32 && ! isctl ) {
                hasUnprintables = true;
            }
        }
        return ! hasUnprintables;
    }
}
