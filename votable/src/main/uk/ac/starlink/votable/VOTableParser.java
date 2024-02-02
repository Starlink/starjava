package uk.ac.starlink.votable;

import java.net.URL;
import java.io.IOException;
import java.io.InputStream;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import uk.ac.starlink.auth.AuthManager;
import uk.ac.starlink.fits.FitsTableBuilder;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.util.ContentCoding;

/**
 * Extends SkeletonDOMBuilder so it can optionally message the TableHandler
 * for href-referenced TABLE data as well as the inline ones.
 *
 * @author   Mark Taylor (Starlink)
 * @since    15 Apr 2005
 */
class VOTableParser extends SkeletonDOMBuilder {

    private final ContentCoding coding_;
    private boolean readHrefs_;

    /**
     * Constructor.
     *
     * @param  strict whether to effect a strict reading of the VOTable standard
     */
    public VOTableParser( boolean strict ) {
        super( strict );
        coding_ = ContentCoding.GZIP;
    }

    /**
     * Sets whether href-referenced STREAMs should be communicated
     * to the table handler.
     *
     * @param   readHrefs  true to stream href-referenced streams to 
     *          the table handler, false to ignore them
     */
    public void setReadHrefTables( boolean readHrefs ) {
        readHrefs_ = readHrefs;
    }

    /**
     * Indicates whether href-referenced STREAMs should be communicated
     * to the table handler.
     *
     * @return   true  if href-referenced streams will be messaged to the
     *           table handler, false if they will be ignored
     */
    public boolean getReadHrefTables() {
        return readHrefs_;
    }

    protected void processBinaryHref( URL url, Attributes atts,
                                      boolean isBinary2 )
            throws SAXException {
        TableHandler tableHandler = getTableHandler();
        TableElement tableEl = getTableElement();
        if ( getReadHrefTables() && tableHandler != null && tableEl != null ) {
            InputStream in = null;
            try {
                in = coding_.openStreamAuth( url, AuthManager.getInstance() );
                tableHandler.startTable( tableEl.getMetadataTable() );
                Decoder[] decoders = getDecoders( tableEl.getFields() );
                String encoding = getAttribute( atts, "encoding" );
                RowSequence rseq =
                    new BinaryRowSequence( decoders, in, encoding, isBinary2 );
                try {
                    while ( rseq.next() ) {
                        tableHandler.rowData( rseq.getRow() );
                    }
                }
                finally {
                    rseq.close();
                }
                tableHandler.endTable();
            }
            catch ( IOException e ) {
                if ( e.getCause() instanceof SAXException ) {
                    throw (SAXException) e.getCause();
                }
                else {
                    throw (SAXException) 
                          new SAXParseException( e.getMessage(), getLocator(),
                                                 e )
                         .initCause( e );
                }
            }
            finally {
                if ( in != null ) {
                    try {
                        in.close();
                    }
                    catch ( IOException e ) {
                        // never mind
                    }
                }
            }
        }
    }

    protected void processFitsHref( URL url, String extnum, Attributes atts )
            throws SAXException {
        TableHandler tableHandler = getTableHandler();
        TableElement tableEl = getTableElement();
        if ( getReadHrefTables() && tableHandler != null && tableEl != null ) {
            InputStream in = null;
            try {
                TableSink sink = 
                    new TableHandlerSink( tableHandler,
                                          tableEl.getMetadataTable() );
                in = coding_.openStreamAuth( url, AuthManager.getInstance() );
                new FitsTableBuilder().streamStarTable( in, sink, extnum );
            }
            catch ( IOException e ) {
                if ( e.getCause() instanceof SAXException ) {
                    throw (SAXException) e.getCause();
                }
                else {
                    throw (SAXException)
                          new SAXParseException( e.getMessage(), getLocator(),
                                                 e )
                         .initCause( e );
                }
            }
            finally {
                if ( in != null ) {
                    try {
                        in.close();
                    }
                    catch ( IOException e ) {
                        // never mind
                    }
                }
            }
        }
    }
}
