package uk.ac.starlink.votable;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.NoSuchElementException;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Element;
import uk.ac.starlink.util.DOMUtils;

/**
 * Implements the VOTable TABLE element whose child is a BINARY element.
 * <p>
 * Note this is currently completely untested code, since I don't have
 * any test data.
 *
 * @author   Mark Taylor (Starlink)
 */
class BinaryTable extends Table {

    private int ncols;
    private DataInput istrm;
    private Object[] nextRow;
    private boolean[] colVar;
    private int[] colBytes;

    BinaryTable( Element tableEl, Element binaryEl, String systemId )
            throws VOTableFormatException {
        super( new DOMSource( tableEl, systemId ) );
        ncols = getColumnCount();

        /* Get the location of the object data. */
        Element streamEl = DOMUtils.getChildElementByName( binaryEl, "STREAM" );
        Stream stream = new Stream( new DOMSource( streamEl, systemId ) );
        try {
            istrm = new DataInputStream( stream.getInputStream() );
            nextRow = obtainNextRow();
        }
        catch ( IOException e ) {
            throw new VOTableFormatException( e );
        }
    }

    public int getRowCount() {
        return -1;
    }

    public Object[] nextRow() {
        if ( nextRow == null ) {
            throw new NoSuchElementException();
        }
        Object[] result = nextRow;
        try {
            nextRow = obtainNextRow();
        }
        catch ( IOException e ) {
            throw new VOTableFormatException( e );
        }
        return result;
    }

    public boolean hasNextRow() {
        return nextRow != null;
    }

    private Object[] obtainNextRow() throws IOException {
        Object[] rowContents = new Object[ ncols ];
        int icol = 0;
        try {
            for ( ; icol < ncols; icol++ ) {
                rowContents[ icol ] = getField( icol ).getDecoder()
                                     .decodeStream( istrm );
            }
        }

        /* An EOFException in the first column is taken to mean that the
         * stream ended at the end of the last row.  In fact this isn't
         * the only possible explanation (EOF could have been reached 
         * midway through reading the first column rather than at its
         * start) but given we've only got a DataInput it's hard to 
         * detect EOF in any more respectable way. */
        catch ( EOFException e ) {
            if ( icol == 0 ) {
                return null;
            }
            else {
                throw e;
            }
        }
        return rowContents;
    }

}
