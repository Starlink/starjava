package uk.ac.starlink.votable;

import uk.ac.starlink.table.StarTable;
import org.xml.sax.SAXException;

/**
 * SAX-like handler which defines callbacks that can be made when a 
 * VOTable data is encountered during a SAX stream.
 *
 * @author   Mark Taylor (Starlink)
 * @since    15 Apr 2005
 * @see   TableContentHandler
 */
public interface TableHandler {

    /**
     * Called when a table is about to be transmitted.
     * This call will occur somewhere between matched DATA element 
     * <tt>startElement</tt> and <tt>endElement</tt> calls.
     * The <tt>metadata</tt> argument signals column and table metadata
     * argument about the table whose rows are about to be transmitted.
     *
     * If the number of rows that will be transmitted via subsequent
     * calls to <tt>rowData</tt> is known, this value should be made
     * available as the row count of <tt>metadata</tt>
     * ({@link StarTable#getRowCount}); if it is not known, the row count
     * should be -1.  However, this object should not attempt to read
     * any of <tt>meta</tt>'s cell data.
     * <p>
     * The data to be transmitted in subsequent calls of <tt>acceptRow</tt>
     * must match the metadata transmitted in this call in the same way
     * that rows of a StarTable must match its own metadata (number and
     * content clases of columns etc).
     *
     * @param   metadata  metadata object
     */
    void startTable( StarTable metadata ) throws SAXException;

    /**
     * Called when a row has been read.  This method will be called
     * between matched <tt>startTable</tt> and <tt>endTable</tt> calls.
     *
     * @param   row   array of data objects representing a row in the 
     *          current table
     */
    void rowData( Object[] row ) throws SAXException;

    /**
     * Called when there are no more rows to be transmitted.
     */
    void endTable() throws SAXException;
}
