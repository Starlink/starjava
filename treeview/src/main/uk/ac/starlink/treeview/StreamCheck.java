package uk.ac.starlink.treeview;

import java.io.*;

/**
 * Examines bytes in a file see what sort of object it represents.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class StreamCheck {
    public static final int MAX_LINE_LENGTH = 240;
    public static final int TESTED_CHARS = 512;

    private boolean isEmpty = false;
    private boolean isText = false;


    /**
     * Initializes a StreamCheck from an InputStream object.  It will 
     * take a look at some of the bytes and make a judgement.  The
     * supplied stream will be closed by this method.
     *
     * @param  stream  a stream supplying bytes from the start of the
     *                 file to be examined.  The stream will be closed
     *                 by this method.
     * @throws  IOException  if an IO error happens during reading the stream
     */
    public StreamCheck( InputStream stream ) throws IOException {

        /* Read bytes from the stream. */
        byte[] buf = new byte[ TESTED_CHARS ];
        int nbyte = stream.read( buf );

        /* Look at the bytes to see if it appears to be a text file
         * (reasonable length lines and printable characters) */
        if ( nbyte <= 0 ) {
            isEmpty = true;
        }
        else {
            int lleng = 0;
            isText = true;
            for ( int i = 0; i < nbyte; i++ ) {
                boolean isret = false;
                boolean isctl = false;
                int bval = buf[ i ];
                switch ( bval ) {
                    case '\n':
                    case '\r':
                        lleng = 0;
                        // no break here is intentional
                    case '\t':
                    case '\f':
                    // case -87:         // copyright symbol
                    case (byte) 169:  // copyright symbol
                    case (byte) 163:  // pound sign
                        isctl = true;
                }
                lleng++;
                if ( lleng > MAX_LINE_LENGTH ) {
                    isText = false;
                    break;
                }
                if ( ( bval > 126 || bval  < 32 ) && ! isctl ) {
                    isText = false;
                    break;
                }
            }
        }

        /* Tidy up. */
        stream.close();
    }

    /**
     * Initializes a StreamCheck from a File object.
     *
     * @param   the File on which to do the check
     * @throws  FileNotFoundException  if the file does not exist
     * @throws  IOException  if an IO error happens during reading the stream
     */
    public StreamCheck( File file ) throws FileNotFoundException,
                                           IOException {
        this( new FileInputStream( file ) );
    }
     

    /**
     * Enquires whether the file appears to be a printable text file.
     *
     * @return  true if the file starts with characters that are all
     *          printable, with a reasonable scattering of carriage
     *          returns.
     */
    public boolean isText() {
        return isText;
    }

    /**
     * Enquires whether the file is empty.
     *
     * @return  true if there are no bytes to read in the file.
     */
    public boolean isEmpty() {
        return isEmpty;
    }

}
