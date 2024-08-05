package uk.ac.starlink.fits;

import java.awt.datatransfer.DataFlavor;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import uk.ac.starlink.table.MultiTableBuilder;
import uk.ac.starlink.table.QueueTableSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.formats.DocumentedTableBuilder;
import uk.ac.starlink.util.Compression;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.IOUtils;
import uk.ac.starlink.util.URLUtils;

/**
 * Implementation of the <code>TableBuilder</code> interface which 
 * gets <code>StarTable</code>s from FITS files.
 *
 * <p>The table implementation used by uncompressed binary FITS tables
 * stored on disk <em>maps</em> the file into memory 
 * ({@link java.nio.MappedByteBuffer}) rather than reading the stream as such;  
 * this makes table construction very fast and cheap on memory, regardless of
 * storage policy.  This behaviour can be inhibited by referring to the
 * file location as a URL (e.g. "file:spec23.fits" rather than "spec23.fits"),
 * which fools the handler into thinking that it can't be mapped.
 *
 * <p>Limited support is provided for the
 * <a href="https://healpix.sourceforge.io/data/examples/healpix_fits_specs.pdf"
 *    >HEALPix-FITS</a> convention;
 * the relevant {@link uk.ac.starlink.table.HealpixTableInfo} table parameters
 * are added, but any BAD_DATA keyword value is ignored,
 * and the 1024-element array-valued column variant of the format is not
 * understood.
 *
 * @author   Mark Taylor (Starlink)
 */
public class FitsTableBuilder extends DocumentedTableBuilder
                              implements MultiTableBuilder {

    private static final Logger logger =
        Logger.getLogger( "uk.ac.starlink.fits" );

    private final WideFits wide_;

    /**
     * Default constructor.
     */
    public FitsTableBuilder() {
        this( WideFits.DEFAULT );
    }

    /**
     * Constructor.
     *
     * @param   wide  convention for representing extended columns;
     *                use null to avoid use of extended columns
     */
    public FitsTableBuilder( WideFits wide ) {
        super( new String[] { "fit", "fits" } );
        wide_ = wide;
    }

    /**
     * Returns "FITS".
     */
    public String getFormatName() {
        return "FITS";
    }

    /**
     * Creates a StarTable from a DataSource which refers to a FITS
     * file or stream.  If the source has a position attribute, it
     * will be interpreted in one of two ways:
     * <ul>
     * <li>If it's an integer, it will be interpreted as the index of the
     *     HDU holding the table.  The first HDU is number 0 (though
     *     being primary this one can't hold a table), the first extension
     *     is number 1, etc.</li>
     * <li>Otherwise it's interpreted as the name of the extension.
     *     Either of the forms "EXTNAME" or "EXTNAME-EXTVER" is permitted,
     *     where EXTNAME and EXTVER are the values of the corresponding
     *     FITS header cards, as per the FITS standard.</li>
     * </ul>
     * <p>If the EXTNAME happens to be in the form of a positive integer,
     * this means you can't refer to the extension by name.  Too bad.
     *
     * <p>If there is no position attribute, the first HDU which does hold
     * a table is used.
     *
     * @param  datsrc  the source of the FITS table data
     * @param  wantRandom  whether a random-access table is preferred
     * @param  policy   a StoragePolicy object which may be used to
     *         supply scratch storage if the builder needs it
     * @return  a new StarTable based on <code>datsrc</code>,
     *          or <code>null</code> if it doesn't look like a FITS table
     */
    public StarTable makeStarTable( DataSource datsrc, boolean wantRandom,
                                    StoragePolicy policy )
            throws IOException {

        /* Check if this looks like a FITS file. */
        if ( ! FitsUtil.isMagic( datsrc.getIntro() ) ) {
            throw new TableFormatException( "Doesn't look like a FITS file" );
        }

        try ( InputStream in = datsrc.getInputStream() ) {

            /* Keep track of the position in the stream. */
            long[] pos = new long[] { 0L };

            /* If an HDU was specified explicitly, try to pick up that one
             * as a table. */
            String spos = datsrc.getPosition();
            if ( spos != null && spos.trim().length() > 0 ) {

                /* If it looks like an integer, treat it as an HDU index. */
                int ihdu;
                try {
                    ihdu = Integer.parseInt( spos.trim() );
                }
                catch ( NumberFormatException e ) {
                    ihdu = -1;
                }
                final StarTable table;
                if ( ihdu >= 0 ) {
                    try {
                        pos[ 0 ] += FitsUtil.skipHDUs( in, ihdu );
                        table = attemptReadTable( in, wantRandom, datsrc,
                                                  wide_, pos, policy );
                    }
                    catch ( EOFException e ) {
                        throw new IOException( "Fell off end of file "
                                             + "looking for HDU #" + ihdu, e );
                    }
                }

                /* Otherwise treat it as an extension name or name-version
                 * string (EXTNAME and EXTVER headers, see FITS standard). */
                else {
                    try {
                        table = findNamedTable( in, datsrc, spos, wide_,
                                                pos, policy );
                    }
                    catch ( EOFException e ) {
                        throw new IOException( "No extension found with "
                                             + "EXTNAME or EXTNAME-EXTVER "
                                             + "\"" + spos + "\"", e );
                    }
                }

                /* Adjust name if required and return. */
                if ( table != null ) {
                    if ( table.getName() == null ) {
                        table.setName( datsrc.getName() );
                    }
                    table.setURL( datsrc.getURL() );
                    return table;
                }
                else {
                    throw new IOException( datsrc + " not a Table HDU" );
                }
            }

            /* Otherwise starting from where we are, find the first
             * table HDU. */
            else {
                try {
                    while ( true ) {
                        StarTable table =
                            attemptReadTable( in, wantRandom, datsrc,
                                              wide_, pos, policy );
                        if ( table != null ) {
                            if ( table.getName() == null ) {
                                table.setName( datsrc.getName() );
                            }
                            table.setURL( datsrc.getURL() );
                            return table;
                        }
                    }
                }
                catch ( EOFException e ) {
                    throw new IOException( "No table HDUs in " + datsrc, e );
                }
            }
        }
    }

    public TableSequence makeStarTables( DataSource datsrc,
                                         StoragePolicy policy )
            throws IOException {
        String frag = datsrc.getPosition();
        if ( frag != null && frag.trim().length() > 0 ) {
            return Tables
                  .singleTableSequence( makeStarTable( datsrc, false,
                                                       policy ) );
        }
        if ( ! FitsUtil.isMagic( datsrc.getIntro() ) ) {
            throw new TableFormatException( "Doesn't look like a FITS file" );
        }
        MultiLoadWorker loadWorker =
            new MultiLoadWorker( datsrc, wide_, policy );
        loadWorker.start();
        return loadWorker.getTableSequence();
    }

    /**
     * Returns <code>true</code> for a flavor with the MIME type
     * "application/fits".
     */
    public boolean canImport( DataFlavor flavor ) {
        if ( flavor.getPrimaryType().equals( "application" ) &&
             flavor.getSubType().equals( "fits" ) ) {
            return true;
        }
        return false;
    }

    /**
     * Reads a FITS table from an input stream and writes it to a sink.
     *
     * @param  in  input stream containing the FITS data
     * @param  sink  destination for table data
     * @param  extnum  may contain a string representation of the HDU
     *         number in which the required table is found (otherwise the
     *         first table HDU will be used)
     */
    public void streamStarTable( InputStream in, TableSink sink,
                                 String extnum ) throws IOException {
        if ( extnum != null && extnum.matches( "[1-9][0-9]*" ) ) {
            int ihdu = Integer.parseInt( extnum );
            FitsUtil.skipHDUs( in, ihdu );
            if ( ! attemptStreamStarTable( in, sink, false ) ) {
                throw new IOException( "No table HDU at extension " + ihdu );
            }
        }
        else {
            boolean done = false;
            while ( ! done ) {
                done = attemptStreamStarTable( in, sink, true );
            }
            if ( ! done ) {
                throw new IOException( "No table extensions found" );
            }
        }
    }

    public boolean canStream() {
        return true;
    }

    public boolean docIncludesExample() {
        return false;
    }

    public String getXmlDescription() {
        return readText( "FitsTableBuilder.xml" );
    }

    /**
     * Attempts to convert the HDU starting at the current position in
     * an input stream into a table, writing it to a given sink.
     * If the HDU does not contain a table, no table is written.
     *
     * @param  in  input stream positioned at the start of a FITS HDU
     * @param  sink  destination for the table data if one is found
     * @param  readAnyway  whether to skip forward to the end of the HDU
     *         even if it does not contain a table
     * @return  <code>true</code> if the table was successfully copied
     */
    private boolean attemptStreamStarTable( InputStream in, TableSink sink,
                                            boolean readAnyway )
            throws IOException {
        FitsHeader hdr = FitsUtil.readHeader( in );
        String xtension = hdr.getStringValue( "XTENSION" );
        if ( "BINTABLE".equals( xtension ) ) {
            BasicInput input = InputFactory.createSequentialInput( in );
            BintableStarTable.streamStarTable( hdr, input, wide_, sink );
            return true;
        }
        else if ( "TABLE".equals( xtension ) ) {
            BasicInput input = InputFactory.createSequentialInput( in );
            AsciiTableStarTable.streamStarTable( hdr, input, sink );
            return true;
        }
        else {
            if ( readAnyway ) {
                long datasize = hdr.getDataByteCount();
                IOUtils.skip( in, datasize );
            }
            return false;
        }
    }

    /**
     * Looks through the HDUs in a given FITS stream and if it finds
     * one which has a given name, attempts to make a table out of it.
     * The supplied <code>name</code> is checked against the EXTNAME
     * header value (if present), and if that fails, against EXTNAME-EXTVER
     * (if EXTVER is present too).  Matching is case-insensitive.
     *
     * @param  in  stream to read from, positioned at the start of an HDU
     *         (before the header)
     * @param  datsrc  a DataSource which can supply the data 
     *         in <code>strm</code>
     * @param  name  target extension name or name-version
     * @param   wide  convention for representing extended columns;
     *                use null to avoid use of extended columns
     * @param  pos  a 1-element array holding the position
     *         in <code>datsrc</code>
     *         at which <code>strm</code> is positioned -
     *         it's an array so it can be updated by this routine (sorry)
     * @param  policy  storage policy, or null for default (normally not used)
     * @return  a new table
     */
    public static StarTable findNamedTable( InputStream in,
                                            DataSource datsrc, String name,
                                            WideFits wide, long[] pos,
                                            StoragePolicy policy )
            throws IOException {
        while ( true ) {
            FitsHeader hdr = FitsUtil.readHeader( in );
            long headsize = hdr.getHeaderByteCount();
            long datasize = hdr.getDataByteCount();
            long datpos = pos[ 0 ] + headsize;
            pos[ 0 ] += headsize + datasize;
            if ( headerName( hdr, name ) ) {
                TableResult tres =
                    attemptReadTableData( in, datsrc, datpos, hdr, wide,
                                          policy );
                assert pos[ 0 ] == tres.afterPos_;
                return tres.table_;
            }
            else {
                IOUtils.skip( in, datasize );
            }
        }
    }

    /**
     * Reads the next header, and returns a StarTable based on it if it
     * represents a table.  If a StarTable is returned, it may not be safe
     * to use the supplied input stream subsequently for other purposes.
     * If the next HDU is some non-table type, <code>null</code> is
     * returned and the stream is advanced to the end of that HDU;
     * in this case the stream may continue to be used (e.g. for 
     * further calls to this method).
     *
     * <p>On exit, the first element of the <code>pos</code> array 
     * contains the position after the current HDU.
     * 
     * @param  in  stream to read from, positioned at the start of an HDU
     *         (before the header)
     * @param  wantRandom  whether a random-access table is preferred
     * @param  datsrc  a DataSource which can supply the data 
     *         in <code>strm</code>
     * @param   wide  convention for representing extended columns;
     *                use null to avoid use of extended columns
     * @param  pos  a 1-element array holding the position
     *         in <code>datsrc</code>
     *         at which <code>strm</code> is positioned -
     *         it's an array so it can be updated by this routine (sorry)
     * @param  policy  storage policy, or null for default (normally not used)
     * @return   a StarTable made from the HDU at the start of <code>strm</code>
     *           or null
     */
    public static StarTable attemptReadTable( InputStream in,
                                              boolean wantRandom, 
                                              DataSource datsrc,
                                              WideFits wide, long[] pos,
                                              StoragePolicy policy )
            throws IOException {
        FitsHeader hdr = FitsUtil.readHeader( in );
        long datpos = pos[ 0 ] + hdr.getHeaderByteCount();
        TableResult tres =
            attemptReadTableData( in, datsrc, datpos, hdr, wide, policy );
        pos[ 0 ] = tres.afterPos_;
        return tres.table_;
    }

    /**
     * Takes a header and a stream positioned just after it,
     * and tries to turn it into a table.
     * A TableResult is returned as long as the stream can be interpreted
     * as FITS; if the header describes a table, the table will be
     * contained.
     *
     * @param   strm  stream to read for, positioned at the start of the
     *          data part of an HDU (just after the header)
     * @param   datsrc  a DataSource which can supply the data
     *          in <code>strm</code>
     * @param   datpos  offset into the file at which the stream is
     *          currently positioned
     * @param   hdr  populated header describing the upcoming data
     * @param   wide  convention for representing extended columns;
     *                use null to avoid use of extended columns
     * @param  policy  storage policy, or null for default (normally not used)
     * @return  an object which may contain a table and other information
     */
    private static TableResult attemptReadTableData( InputStream in,
                                                     DataSource datsrc,
                                                     long datpos,
                                                     FitsHeader hdr,
                                                     WideFits wide,
                                                     StoragePolicy policy )
            throws IOException {
        long datasize = hdr.getDataByteCount();
        long afterpos = datpos + datasize;
        String xtension = hdr.getStringValue( "XTENSION" );

        /* If it's a BINTABLE HDU, make a BintableStarTable out of it. */
        if ( "BINTABLE".equals( xtension ) ) {
            Long pcount = hdr.getLongValue( "PCOUNT" );
            final InputFactory inFact;
            if ( pcount != null && pcount.longValue() > 0 ) {

                /* If there is a non-zero heap, the table presumably(?)
                 * has P or Q descriptors, meaning that sequential data access
                 * is not going to be able to access all the cell data.
                 * Therefore we have to ensure that the InputFactory provides
                 * random access to the data stream.  That may (e.g. in case
                 * of data compression) mean caching the whole data stream. */
                logger.info( "FITS file has non-zero heap" );
                inFact = InputFactory.createRandomFactory( datsrc, datpos,
                                                           datasize, policy );
            }
            else {
                inFact = InputFactory.createFactory( datsrc, datpos, datasize );
            }
            StarTable table =
                BintableStarTable.createTable( hdr, inFact, wide );
            try {
                IOUtils.skip( in, datasize );
            }
            catch ( EOFException e ) {
                throw new EOFException( "FITS file too short for HDU"
                                      + " - corrupted/truncated?" );
            }
            return new TableResult( table, afterpos );
        }

        /* If it's a TABLE HDU (ASCII table) make an AsciiTableStarTable. */
        else if ( "TABLE".equals( xtension ) ) {
            InputFactory inFact =
                InputFactory.createFactory( datsrc, datpos, datasize );
            StarTable table = AsciiTableStarTable.createTable( hdr, inFact );
            IOUtils.skip( in, datasize );
            return new TableResult( table, afterpos );
        }

        /* It's not a table HDU - skip over it and return no table. */
        else {
            IOUtils.skip( in, datasize );
            return new TableResult( null, afterpos );
        }
    }

    /**
     * Indicates whether the header has a given name.
     * EXTNAME or EXTNAME-VERSION, matched case-insensitively, count.
     *
     * @param  hdr   header
     * @param  name  required name
     * @return  true iff <code>hdr</code> appears to be named <code>name</code>
     */
    private static boolean headerName( FitsHeader hdr, String name ) {
        String extname = hdr.getStringValue( "EXTNAME" );
        if ( extname == null || extname.trim().length() == 0 ) {
            return false;
        }
        if ( extname.trim().equalsIgnoreCase( name ) ) {
            return true;
        }
        Integer extver = hdr.getIntValue( "EXTVER" );
        if ( extver != null ) {
            return (extname + "-" + extver).equalsIgnoreCase( name );
        }
        return false;
    }

    /**
     * Thread which loads tables.
     */
    private static class MultiLoadWorker extends Thread {
        private final DataSource datsrc_;
        private final WideFits wide_;
        private final StoragePolicy policy_;
        private final QueueTableSequence tqueue_;

        /**
         * Constructor.
         *
         * @param  datsrc  data source containing FITS table
         * @param   wide  convention for representing extended columns;
         *                use null to avoid use of extended columns
         * @param  policy  storage policy, or null for default
         *                 (normally not used)
         */
        MultiLoadWorker( DataSource datsrc, WideFits wide,
                         StoragePolicy policy ) {
            super( "FITS multi table loader" );
            setDaemon( true );
            datsrc_ = datsrc;
            wide_ = wide;
            policy_ = policy;
            tqueue_ = new QueueTableSequence();
        }

        /**
         * Returns the table sequence populated by this thread.
         * This thread must be started for the returned sequence to become
         * populated and eventually terminated.
         *
         * @return   output table sequence
         */
        TableSequence getTableSequence() {
            return tqueue_;
        }

        public void run() {
            try {
                multiLoad();
            }
            catch ( Throwable e ) {
                tqueue_.addError( e );
            }
            finally {
                tqueue_.endSequence();
            }
        }

        /**
         * Does the work of loading tables.  Table successes and failures
         * are added to the table sequence, ready for readout on a different
         * thread, as they are encountered.
         */
        private void multiLoad() throws IOException {
            try ( InputStream in = datsrc_.getInputStream() ) {
                long pos = 0L;
                for ( int ihdu = 0; true; ihdu++ ) {
                    FitsHeader hdr = FitsUtil.readHeaderIfPresent( in );
                    if ( hdr == null ) {
                        if ( ihdu == 0 ) {
                            throw new EOFException( "Empty stream" );
                        }
                        else {
                            return;
                        }
                    }
                    long datpos = pos + hdr.getHeaderByteCount();
                    TableResult tres =
                        attemptReadTableData( in, datsrc_, datpos, hdr,
                                              wide_, policy_ );
                    StarTable table = tres.table_;
                    pos = tres.afterPos_;
                    if ( table != null ) {
                        if ( table.getName() == null ) {
                            table.setName( datsrc_.getName() + "#" + ihdu );
                        }
                        URL baseUrl = datsrc_.getURL();
                        if ( baseUrl != null &&
                             baseUrl.toString().indexOf( '#' ) < 0 ) {
                            String hduUrl = baseUrl + "#" + ihdu;
                            try {
                                table.setURL( URLUtils.newURL( hduUrl ) );
                            }
                            catch ( MalformedURLException e ) {
                                logger.info( "Bad URL " + hduUrl + "?" );
                            }
                        }
                        tqueue_.addTable( table );
                    }
                }
            }
        }
    }

    /**
     * Encapsulates information about the attempt to read a table from
     * a FITS HDU in a stream.
     */
    private static class TableResult {
        final StarTable table_;
        final long afterPos_;

        /**
         * Constructor.
         *
         * @param  table  table, if one could be read;
         *                null if the HDU was not a table type
         * @param  afterPos  position in stream of first byte following the
         *                   HDU just read
         */
        TableResult( StarTable table, long afterPos ) {
            table_ = table;
            afterPos_ = afterPos;
        }
    }
}
