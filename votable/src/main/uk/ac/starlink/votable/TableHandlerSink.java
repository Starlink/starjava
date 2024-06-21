package uk.ac.starlink.votable;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.TableFormatException;
import org.xml.sax.SAXException;

/**
 * Adapts a TableHandler to become a TableSink.
 * Optionally you can force use of a given metadata table.
 *
 * @author   Mark Taylor (Starlink)
 * @since    14 Apr 2005
 */
class TableHandlerSink implements TableSink {

    private final TableHandler handler_;
    private boolean metadataSent_;

    /**
     * Constructs a new TableSink based on a handler, optionally forcing
     * use of a given metadata table.
     * If the <code>meta</code> argument is supplied then it, and not the
     * one passed in the subsequent {@link #acceptMetadata} call, is
     * the one which will be passed to the handler.
     *
     * @param   handler  destination for table-related events
     * @param   meta   the metadata object, or null
     */
    public TableHandlerSink( TableHandler handler, StarTable meta )
            throws SAXException {
        handler_ = handler;
        if ( meta != null ) {
            handler_.startTable( meta );
            metadataSent_ = true;
        }
    }

    public void acceptMetadata( StarTable meta ) throws TableFormatException {
        if ( ! metadataSent_ ) {
            metadataSent_ = true;
            try {
                handler_.startTable( meta );
            }
            catch ( SAXException e ) {
                throw new TableFormatException( e.getMessage(), e );
            }
        }
        else {
            /* Ignore this call, since we've already passed on the metadata
             * which we picked up from the TABLE element.  If the two sets of
             * metadata differ dramatically there will probably be trouble
             * down the line - too bad.  Could check it and log differences 
             * or something here I suppose.  */
        }
    }

    public void acceptRow( Object[] row ) throws IOException {
        try {
            handler_.rowData( row );
        }
        catch ( SAXException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
    }

    public void endRows() throws IOException {
        try {
            handler_.endTable();
        }
        catch ( SAXException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
    }
}
