package uk.ac.starlink.votable;

import java.io.DataInput;
import java.io.DataInputStream;
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
        for ( int icol = 0; icol < ncols; icol++ ) {
            rowContents[ icol ] = getField( icol ).getDecoder()
                                 .decodeStream( istrm );
        }
        return rowContents;
    }

}
