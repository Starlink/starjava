package uk.ac.starlink.table.formats;

import java.awt.datatransfer.DataFlavor;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.util.DataSource;

/**
 * Implementation of the <code>TableBuilder</code> interface which gets
 * <code>StarTable</code>s from World Data Centre-type text files.
 * This format doesn't appear to have a proper name or definition.
 * This implementation is a result of reverse-engineering the format 
 * specification by looking at a couple of files.
 *
 * @author   Mark Taylor (Starlink)
 */
public class WDCTableBuilder extends DocumentedTableBuilder {

    public WDCTableBuilder() {
        super( new String[ 0 ] );
    }

    public String getFormatName() {
        return "WDC";
    }

    public StarTable makeStarTable( DataSource datsrc, boolean wantRandom,
                                    StoragePolicy storagePolicy )
            throws IOException {

        /* If it doesn't start how we expect WDC text to start, bail
         * out straight away. */
        String start = new String( datsrc.getIntro() );
        if ( ! start.startsWith( "Column formats and units" ) ) {
            throw new TableFormatException( "Doesn't start \"" +
                                            "Column formats and units\"" );
        }

        /* Looks OK, make a serious attempt to read it. */
        BufferedInputStream strm = 
            new BufferedInputStream( datsrc.getInputStream() );

        /* Try to parse the meaningful parts of the header info. */
        WDCReader wdcReader = new WDCReader( strm );

        /* Return a new table based on this understanding. */
        WDCStarTable st = new WDCStarTable( wdcReader, datsrc );
        st.setURL( datsrc.getURL() );
        st.setName( datsrc.getName() );
        return st;
    }

    /**
     * Returns false since there is no MIME type which targets WDC format.
     */
    public boolean canImport( DataFlavor flavor ) {
        return false;
    }

    /**
     * Throws an exception; streaming of WDC tables is not implemented.
     * It probably could be if necessary.
     */
    public void streamStarTable( InputStream in, TableSink sink, String pos )
            throws TableFormatException {
        throw new TableFormatException( "WDC streaming not implemented" );
    }

    public boolean canStream() {
        return false;
    }

    public boolean docIncludesExample() {
        return false;
    }

    public String getXmlDescription() {
        return readText( "WDCTableBuilder.xml" );
    }

    public static String readLine( BufferedInputStream strm ) 
            throws IOException {
        StringBuffer sbuf = new StringBuffer();
        while ( true ) {
            int c = strm.read();
            if ( c == -1 ) {
                return null;
            }
            else if ( c == 0x0a ) {
                return sbuf.toString();
            }
            else {
                sbuf.append( (char) c );
            }
        }
    }
}
