package uk.ac.starlink.table.formats;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.util.DataSource;

/**
 * Implementation of the <tt>TableBuilder</tt> interface which gets
 * <tt>StarTable</tt>s from World Data Centre-type text files.
 * This format doesn't appear to have a proper name or definition.
 * This implementation is a result of reverse-engineering the format 
 * specification by looking at a couple of files.
 *
 * @author   Mark Taylor (Starlink)
 */
public class WDCTableBuilder implements TableBuilder {

    public StarTable makeStarTable( DataSource datsrc ) throws IOException {

        /* If it doesn't start how we expect WDC text to start, bail
         * out straight away. */
        byte[] buffer = new byte[ 80 ];
        datsrc.getMagic( buffer );
        String start = new String( buffer );
        if ( ! start.startsWith( "Column formats and units" ) ) {
            return null;
        }

        /* Looks OK, make a serious attempt to read it. */
        BufferedInputStream strm = 
            new BufferedInputStream( datsrc.getInputStream() );

        /* Try to parse the meaningful parts of the header info. */
        WDCReader wdcReader = new WDCReader( strm );

        /* Return a new table based on this understanding. */
        WDCStarTable st = new WDCStarTable( wdcReader, datsrc );
        // st.setURL( datsrc.getURL() );
        // st.setName( datsrc.getName() );
        return st;
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
