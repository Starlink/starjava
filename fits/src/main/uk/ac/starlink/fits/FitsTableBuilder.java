package uk.ac.starlink.fits;

import java.awt.datatransfer.DataFlavor;
import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import nom.tam.fits.AsciiTable;
import nom.tam.fits.AsciiTableHDU;
import nom.tam.fits.BinaryTable;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Data;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.TableHDU;
import nom.tam.util.ArrayDataInput;
import nom.tam.util.BufferedDataInputStream;
import nom.tam.util.RandomAccess;
import uk.ac.starlink.table.MultiTableBuilder;
import uk.ac.starlink.table.QueueTableSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.Compression;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.IOUtils;

/**
 * Implementation of the <tt>TableBuilder</tt> interface which 
 * gets <tt>StarTable</tt>s from FITS files.
 *
 * <p>The table implementation used by uncompressed binary FITS tables
 * stored on disk <em>maps</em> the file into memory 
 * ({@link java.nio.MappedByteBuffer}) rather than reading the stream as such;  
 * this makes table construction very fast and cheap on memory, regardless of
 * storage policy.  This behaviour can be inhibited by referring to the
 * file location as a URL (e.g. "file:spec23.fits" rather than "spec23.fits"),
 * which fools the handler into thinking that it can't be mapped.
 *
 * @author   Mark Taylor (Starlink)
 */
public class FitsTableBuilder implements TableBuilder, MultiTableBuilder {

    private static final Logger logger =
        Logger.getLogger( "uk.ac.starlink.fits" );

    /**
     * Returns "FITS".
     */
    public String getFormatName() {
        return "FITS";
    }

    /**
     * Creates a StarTable from a DataSource which refers to a FITS
     * file or stream.  If the source has a position attribute, it
     * will be interpreted as an integer indicating which HDU the
     * table is in.  The first HDU is number 0 (though being a primary
     * HDU this one can't hold a table).  If there is no position,
     * the first HDU which does hold a table is used.
     *
     * @param  datsrc  the source of the FITS table data
     * @param  wantRandom  whether a random-access table is preferred
     * @param  policy   a StoragePolicy object which may be used to
     *         supply scratch storage if the builder needs it
     * @return  a new StarTable based on <tt>datsrc</tt>, or <tt>null</tt>
     *          if it doesn't look like a FITS table
     */
    public StarTable makeStarTable( DataSource datsrc, boolean wantRandom,
                                    StoragePolicy policy )
            throws IOException {

        /* Check if this looks like a FITS file. */
        if ( ! FitsConstants.isMagic( datsrc.getIntro() ) ) {
            throw new TableFormatException( "Doesn't look like a FITS file" );
        }

        ArrayDataInput strm = null;
        StarTable table = null;
        try {

            /* Get a FITS data stream. */
            strm = FitsConstants.getInputStreamStart( datsrc );

            /* Keep track of the position in the stream. */
            long[] pos = new long[] { 0L };

            /* If an HDU was specified explicitly, try to pick up that one
             * as a table. */
            if ( datsrc.getPosition() != null ) {
                pos[ 0 ] += FitsConstants
                           .positionStream( strm, datsrc.getPosition() );
                try {
                    table = attemptReadTable( strm, wantRandom, datsrc, pos );
                }
                catch ( EOFException e ) {
                    throw new IOException( "Fell off end of file looking for "
                                         + "HDU " + datsrc.getPosition() );
                }
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
                        table = attemptReadTable( strm, wantRandom,
                                                  datsrc, pos );
                        if ( table != null ) {
                            if ( table.getName() == null ) {
                                table.setName( datsrc.getName() );
                            }
                            table.setURL( datsrc.getURL() );
                            return table;
                        }
                    }
                    // can't get here
                }
                catch ( EOFException e ) {
                    throw new IOException( "No table HDUs in " + datsrc );
                }
            }
        }
        catch ( FitsException e ) {
            throw (TableFormatException)
                  new TableFormatException( e.getMessage() ).initCause( e );
        }
        finally {
            if ( strm != null && table == null ) {
                strm.close();
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
        if ( ! FitsConstants.isMagic( datsrc.getIntro() ) ) {
            throw new TableFormatException( "Doesn't look like a FITS file" );
        }
        MultiLoadWorker loadWorker = new MultiLoadWorker( datsrc );
        loadWorker.start();
        return loadWorker.getTableSequence();
    }

    /**
     * Returns <tt>true</tt> for a flavor with the MIME type "application/fits".
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
     * @param  istrm  input stream containing the FITS data
     * @param  sink  destination for table data
     * @param  extnum  may contain a string representation of the HDU
     *         number in which the required table is found (otherwise the
     *         first table HDU will be used)
     */
    public void streamStarTable( InputStream istrm, TableSink sink,
                                 String extnum ) throws IOException {
        ArrayDataInput in = new BufferedDataInputStream( istrm );
        
        try {
            if ( extnum != null && extnum.matches( "[1-9][0-9]*" ) ) {
                int ihdu = Integer.parseInt( extnum );
                FitsConstants.skipHDUs( in, ihdu );
                if ( ! attemptStreamStarTable( in, sink, false ) ) {
                    throw new IOException( "No table HDU at extension " 
                                         + ihdu );
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
        catch ( FitsException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
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
     * @return  <tt>true</tt> if the table was successfully copied
     */
    private boolean attemptStreamStarTable( ArrayDataInput in, TableSink sink,
                                            boolean readAnyway )
            throws IOException, FitsException {
        Header hdr = new Header();
        try {
            FitsConstants.readHeader( hdr, in );
        }
        catch ( IOException e ) {
            throw (TableFormatException)
                  new TableFormatException( "Can't read FITS header", e );
        }
        String xtension = hdr.getStringValue( "XTENSION" );
        if ( "BINTABLE".equals( xtension ) ) {
            BintableStarTable.streamStarTable( hdr, in, sink );
            return true;
        }
        else if ( "TABLE".equals( xtension ) ) {
            AsciiTable tdata = new AsciiTable( hdr );
            tdata.read( in );
            tdata.getData();
            TableHDU thdu = new AsciiTableHDU( hdr, (Data) tdata );
            Tables.streamStarTable( new FitsStarTable( thdu ), sink );
            return true;
        }
        else {
            if ( readAnyway ) {
                long datasize = FitsConstants.getDataSize( hdr );
                IOUtils.skipBytes( in, datasize );
            }
            return false;
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
     * @param  strm  stream to read from, positioned at the start of an HDU
     *         (before the header)
     * @param  wantRandom  whether a random-access table is preferred
     * @param  datsrc  a DataSource which can supply the data 
     *         in <tt>strm</tt>
     * @param  pos  a 1-element array holding the position in <tt>datsrc</tt>
     *         at which <tt>strm</tt> is positioned -
     *         it's an array so it can be updated by this routine (sorry)
     * @return   a StarTable made from the HDU at the start of <tt>strm</tt>
     *           or null
     */
    public static StarTable attemptReadTable( ArrayDataInput strm,
                                              boolean wantRandom, 
                                              DataSource datsrc, long[] pos )
            throws FitsException, IOException {
        TableResult tres = attemptReadTable( strm, datsrc, pos[ 0 ] );
        pos[ 0 ] = tres.afterPos_;
        return tres.table_;
    }

    /**
     * Reads the next header, tries to turn it into a table, and returns
     * information about the result.
     * If the HDU represents a table, the returned value contains a
     * StarTable based on it; in any case it contains information about
     * the state of the stream following the attempt.
     *
     * @param   strm  stream to read for, positioned at the start of an HDU
     *          (before the header)
     * @param   datsrc  a DataSource which can supply the data
     *          in <code>strm</code>
     * @param   pos  the position in <code>datsrc</code> at which 
     *          <code>strm</code> is positioned
     * @return  an object which may contain a table and other information
     */
    private static TableResult attemptReadTable( ArrayDataInput strm,
                                                 DataSource datsrc, long pos )
           throws FitsException, IOException {

        /* Read the header. */
        Header hdr = new Header();
        int headsize = FitsConstants.readHeader( hdr, strm );
        long datasize = FitsConstants.getDataSize( hdr );
        long datpos = pos + headsize;
        long afterpos = pos + headsize + datasize;
        String xtension = hdr.getStringValue( "XTENSION" );

        /* If it's a BINTABLE HDU, make a BintableStarTable out of it. */
        if ( "BINTABLE".equals( xtension ) ) {
            if ( strm instanceof RandomAccess ) {
                StarTable table =
                    BintableStarTable
                   .makeRandomStarTable( hdr, (RandomAccess) strm );
                ((RandomAccess) strm).seek( afterpos );
                return new TableResult( table, afterpos, true, isEof( strm ) );
            }
            else {
                StarTable table =
                    BintableStarTable
                   .makeSequentialStarTable( hdr, datsrc, datpos );
                for ( long iskip = datasize; iskip > 0; ) {
                    iskip -= strm.skip( iskip );
                }
                return new TableResult( table, afterpos, false, isEof( strm ) );
            }
        }

        /* If it's a TABLE HDU (ASCII table) make a FitsStarTable. */
        else if ( "TABLE".equals( xtension ) ) {
            AsciiTable tdata = new AsciiTable( hdr );
            tdata.read( strm );
            tdata.getData();
            TableHDU thdu = new AsciiTableHDU( hdr, (Data) tdata );
            return new TableResult( new FitsStarTable( thdu ),
                                    afterpos, false, isEof( strm ) );
        }

        /* It's not a table HDU - skip over it and return no table. */
        else {
            IOUtils.skipBytes( strm, datasize );
            return new TableResult( null, afterpos, false, isEof( strm ) );
        }
    }

    /**
     * Works out whether a given ArrayDataInput is positioned at the end
     * of the stream or not.  A best effort is made.  The position of
     * the stream is not affected; though note the possibility of 
     * (common) InputStream mark/reset bugs causing trouble here.
     *
     * @param   in   input stream
     * @return  true if <code>in</code> is known to contain no more bytes;
     *          false if it may contain more
     */
    private static boolean isEof( ArrayDataInput in ) throws IOException {

        /* The following test is commented out because there is a bug
         * in java.util.zip.InflaterInputStream that makes it dangerously
         * unusable.  Compressed input streams can report available bytes
         * even when the end of stream has been reached.
         * Sun's bug ID is 4795134 - they fixed it, then the fix caused
         * problems so they re-assessed the behaviour as not a bug, so 
         * it's unlikely to be fixed again.  A belt'n'braces fix has
         * also been applied to the compression stream used by
         * uk.ac.starlink.util.DataSource, but the assessment appears 
         * to be that available() is not reliable. */
        //  if ( in instanceof InputStream &&
        //       ((InputStream) in).available() > 0 ) {
        //      return false;
        //  }

        if ( in instanceof RandomAccess ) {
            RandomAccess rin = (RandomAccess) in;
            long pos = rin.getFilePointer();
            boolean eof;
            try {
                rin.readByte();
                eof = false;
            }
            catch ( EOFException e ) {
                eof = true;
            }
            catch ( IOException e ) {
                // ?? call it an EOF
                eof = true;
            }
            if ( ! eof ) {
                rin.seek( pos );
            }
            return eof;
        }
        else if ( in instanceof InputStream &&
                  ((InputStream) in).markSupported() ) {
            InputStream is = (InputStream) in;
            is.mark( 1 );
            boolean eof = is.read() < 0;
            try {
                is.reset();
            }
            catch ( IOException e ) {
                if ( ! eof ) {
                    throw e;
                }
            }
            return eof;
        }
        else {
            return false;
        }
    }

    /**
     * Thread which loads tables.
     */
    private static class MultiLoadWorker extends Thread {
        private final DataSource datsrc_;
        private final QueueTableSequence tqueue_;

        /**
         * Constructor.
         *
         * @param  datsrc  data source containing FITS table
         */
        MultiLoadWorker( DataSource datsrc ) {
            super( "FITS multi table loader" );
            setDaemon( true );
            datsrc_ = datsrc;
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
        private void multiLoad() throws IOException, FitsException {
            ArrayDataInput in = null;
            try {
                long pos = 0L;
                boolean done = false;
                for ( int ihdu = 0; ! done ; ihdu++ ) {
                    if ( in == null ) {
                        in = FitsConstants.getInputStreamStart( datsrc_ );
                        if ( pos > 0 ) {
                            if ( in instanceof RandomAccess ) {
                                ((RandomAccess) in).seek( pos );
                            }
                            else {
                                IOUtils.skipBytes( in, pos );
                            }
                        }
                    }
                    TableResult tres = attemptReadTable( in, datsrc_, pos );
                    StarTable table = tres.table_;
                    pos = tres.afterPos_;
                    if ( tres.streamUsed_ ) {
                        in = null;
                    }
                    if ( table != null ) {
                        if ( table.getName() == null ) {
                            table.setName( datsrc_.getName() + "#" + ihdu );
                        }
                        URL baseUrl = datsrc_.getURL();
                        if ( baseUrl != null &&
                             baseUrl.toString().indexOf( '#' ) < 0 ) {
                            String hduUrl = baseUrl + "#" + ihdu;
                            try {
                                table.setURL( new URL( hduUrl ) );
                            }
                            catch ( MalformedURLException e ) {
                                logger.info( "Bad URL " + hduUrl + "?" );
                            }
                        }
                        tqueue_.addTable( table );
                    }
                    done = tres.streamEnded_;
                }
            }
            finally {
                if ( in != null ) {
                    in.close();
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
        final boolean streamUsed_;
        final boolean streamEnded_;

        /**
         * Constructor.
         *
         * @param  table  table, if one could be read;
         *                null if the HDU was not a table type
         * @param  afterPos  position in stream of first byte following the
         *                   HDU just read
         * @param  streamUsed  it is only safe to use the stream for other
         *               purposes following the read attempt if this is
         *               false
         * @param  streamEnded  true only if there are known to be no
         *               more bytes in the stream
         */
        TableResult( StarTable table, long afterPos, boolean streamUsed,
                     boolean streamEnded ) {
            table_ = table;
            afterPos_ = afterPos;
            streamUsed_ = streamUsed;
            streamEnded_ = streamEnded;
        }
    }
}
