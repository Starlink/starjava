package uk.ac.starlink.hapi;

import java.io.EOFException;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.EmptyRowSequence;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.util.IOUtils;
import uk.ac.starlink.util.IOSupplier;

/**
 * Converts HAPI metadata and input streams to StarTables.
 *
 * @author   Mark Taylor
 * @since    12 Jan 2024
 */
public class HapiTableReader {

    private final ParamReader[] paramRdrs_;
    private final ColumnInfo[] colInfos_;
    private final int nparam_;
    private final int ncol_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.hapi" );

    /**
     * Constructor.
     *
     * @param  hapiInfo  HAPI table metadata
     */
    public HapiTableReader( HapiInfo hapiInfo ) {
        HapiParam[] params = hapiInfo.getParameters();
        nparam_ = params.length;
        paramRdrs_ = new ParamReader[ nparam_ ];
        List<ColumnInfo> cinfoList = new ArrayList<>();
        for ( int ip = 0; ip < nparam_; ip++ ) {
            ParamReader paramRdr = ParamReader.createReader( params[ ip ] );
            paramRdrs_[ ip ] = paramRdr;
            int nc = paramRdr.getColumnCount();
            for ( int ic = 0; ic < nc; ic++ ) {
                cinfoList.add( paramRdr.getColumnInfo( ic ) );
            }
        }
        colInfos_ = cinfoList.toArray( new ColumnInfo[ 0 ] );
        ncol_ = colInfos_.length;
    }

    /**
     * Produces a StarTable based on this metadata given a source of
     * row sequence data.  This may be obtained using other methods
     * of this class.
     *
     * <p>If a null RowSequence supplier is provided, the resulting
     * table will be data-less; its metadata methods will work,
     * but attempts to read its data will fail.
     *
     * @param  rseqSupplier  provides sequential data for the table, or null
     * @return   table
     */
    public StarTable createStarTable( IOSupplier<RowSequence> rseqSupplier ) {
        return new AbstractStarTable() {
            public int getColumnCount() {
                return colInfos_.length;
            }
            public ColumnInfo getColumnInfo( int ic ) {
                return colInfos_[ ic ];
            }
            public long getRowCount() {
                return -1;
            }
            public RowSequence getRowSequence() throws IOException {
                if ( rseqSupplier != null ) {
                    return rseqSupplier.get();
                }
                else {
                    throw new UnsupportedOperationException();
                }
            }
        };
    }

    /**
     * Returns a row sequence given a HAPI data stream including the
     * prepended commented header (info JSON object) information.
     * The header is used to determine the serialization format,
     * though not the table metadata, of the data part of the input stream.
     *
     * <p>The supplied input stream will be used as is, so any buffering
     * should be applied before calling this method.
     *
     * @param   in  input stream including prepended HAPI header
     * @return   row sequence
     */
    public RowSequence createRowSequenceUsingHeader( InputStream in )
            throws IOException {
        int[] overread1 = new int[ 1 ];
        HapiInfo hdr = HapiInfo.fromCommentedStream( in, overread1 );
        int b0 = overread1[ 0 ];
        Byte byte0 = ( b0 & 0xff ) == b0 ? Byte.valueOf( (byte) b0 ) : null;
        String fmt = hdr.getFormat();
        return createRowSequence( in, byte0, fmt );
    }

    /**
     * Returns a row sequence given a HAPI data stream with no header.
     * The first byte may optionally be supplied separately.
     *
     * <p>The supplied input stream will be used as is, so any buffering
     * should be applied before calling this method.
     *
     * @param  in   input stream ready for use
     * @param  byte0  byte to read at start of input sequence, or null
     * @param  fmt  data stream format, one of "csv" or "binary"
     * @return  row sequence
     */
    public RowSequence createRowSequence( InputStream in, Byte byte0,
                                          String fmt )
            throws IOException {

        /* Ensure that we have an initial byte, even if one didn't get
         * passed to this routine.  If there is no first byte,
         * then there's no data, so return an empty row sequence. */
        if ( byte0 == null ) {
            int b = in.read();
            if ( b >= 0 ) {
                byte0 = Byte.valueOf( (byte) b );
            }
            else {
                return EmptyRowSequence.getInstance();
            }
        }
        byte b0 = byte0.byteValue();

        /* If the first byte is an open brace ("{"), then it can't be
         * a legible CSV or binary data stream, since the first column
         * must be an ISO-8601 timestamp.  It is probably a JSON object.
         * This shouldn't really happen, but some HAPI services
         * insert a status object saying there's no data rather than
         * just leaving an empty stream.  So treat this case specially
         * rather than generating a nasty error condition when the
         * read is attempted. */
        if ( b0 == '{' &&
             ( "csv".equals( fmt ) || "binary".equals( fmt ) ) ) {
            return createUnexpectedJsonRowSequence( in, b0 );
        }

        /* Otherwise dispatch to a suitable format-specific stream reader. */
        if ( "csv".equals( fmt ) ) {
            return createCsvRowSequence( in, byte0 );
        }
        else if ( "binary".equals( fmt ) ) {
            return createBinaryRowSequence( in, byte0 );
        }
        else if ( "json".equals( fmt ) ) {
            throw new TableFormatException( "Unsupported HAPI data format "
                                          + fmt );
        }
        else {
            throw new TableFormatException( "Unknown HAPI data format " + fmt );
        }
    }

    /**
     * Returns a row sequence given a HAPI CSV format data stream
     * with no header.
     * The first byte may optionally be supplied separately.
     *
     * @param  in   input stream 
     * @param  byte0  byte to read at start of input sequence, or null
     * @return  row sequence
     */
    private RowSequence createCsvRowSequence( InputStream in, Byte byte0 ) {
        CsvReader csvRdr = new CsvReader();
        if ( byte0 != null ) {
            csvRdr.setPrefixByte( byte0.byteValue() );
        }
        Object[][] results = createResultsArray( paramRdrs_ );
        return new RowSequence() {
            Object[] row_;
            public boolean next() throws IOException {
                String[] csvRow = csvRdr.readCsvRow( in );
                if ( csvRow != null ) {
                   int foff = 0;
                   int coff = 0;
                   row_ = new Object[ ncol_ ];
                   for ( int ip = 0; ip < nparam_; ip++ ) {
                        ParamReader paramRdr = paramRdrs_[ ip ];
                        Object[] result = results[ ip ];
                        int nf = paramRdr.getFieldCount();
                        int nc = paramRdr.getColumnCount();
                        paramRdr.readStringValues( csvRow, foff, result );
                        System.arraycopy( result, 0, row_, coff, nc );
                        foff += nf;
                        coff += nc;
                    }
                    return true;
                }
                else {
                    row_ = null;
                    return false;
                }
            }
            public Object[] getRow() {
                if ( row_ != null ) {
                    return row_;
                }
                else {
                    throw new IllegalStateException();
                }
            }
            public Object getCell( int icol ) {
                return getRow()[ icol ];
            }
            public void close() throws IOException {
                in.close();
            }
        };
    }

    /**
     * Returns a row sequence given a HAPI binary format data stream
     * with no header.
     * The first byte may optionally be supplied separately.
     *
     * @param  in   input stream 
     * @param  byte0  byte to read at start of input sequence, or null
     * @return  row sequence
     */
    private RowSequence createBinaryRowSequence( InputStream in, Byte byte0 ) {
        final int bufsize = Arrays.stream( paramRdrs_ )
                           .mapToInt( prdr -> prdr.getByteCount() )
                           .sum();
        final Object[][] results = createResultsArray( paramRdrs_ );
        return new RowSequence() {
            Byte byte0_ = byte0;
            Object[] row_;
            final byte[] buf_ = new byte[ bufsize ];
            public boolean next() throws IOException {
                if ( fillBuffer() ) {
                    int boff = 0;
                    int coff = 0;
                    row_ = new Object[ ncol_ ];
                    for ( int ip = 0; ip < nparam_; ip++ ) {
                        ParamReader prdr = paramRdrs_[ ip ];
                        Object[] result = results[ ip ];
                        int nb = prdr.getByteCount();
                        int nc = prdr.getColumnCount();
                        prdr.readBinaryValues( buf_, boff, result );
                        System.arraycopy( result, 0, row_, coff, nc );
                        boff += nb;
                        coff += nc;
                    }
                    return true;
                }
                else {
                    row_ = null;
                    return false;
                }
            }
            public Object[] getRow() {
                if ( row_ != null ) {
                    return row_;
                }
                else {
                    throw new IllegalStateException();
                }
            }
            public Object getCell( int icol ) {
                return row_[ icol ];
            }
            public void close() throws IOException {
                in.close();
            }

            /**
             * Reads bytes from the input stream into the buffer.
             * If the buffer can be filled, true is returned.
             * If no bytes can be read, false is returned.
             * If only part of the buffer can be filled before encountering
             * the end of the file, an IOException is thrown.
             *
             * @return  true for complete buffer, false for EOF
             */
            private boolean fillBuffer() throws IOException {
                int len = buf_.length;
                int off = 0;
                if ( byte0_ != null ) {
                    buf_[ 0 ] = byte0_.byteValue();
                    off++;
                    len--;
                    byte0_ = null; 
                }
                while ( len > 0 ) {
                    int nb = in.read( buf_, off, len );
                    if ( nb == -1 ) {
                        if ( off == 0 ) {
                            return false;
                        }
                        else {
                            String msg = "Unexpected end of HAPI stream";
                            throw new EOFException( msg );
                        }
                    }
                    len -= nb;
                    off += nb;
                }
                return true;
            }
        };
    }

    /**
     * Try to interpret a stream as a JSON status object, and return
     * an empty RowSequence if it is.  If it's not, throw an error.
     *
     * @param  in  input stream
     * @param  byte0  single byte to prepend to the stream
     * @return  empty row sequence if it does look like a JSON status report
     * @throws  IOException  if it doesn't look like a JSON status report
     */
    private static RowSequence createUnexpectedJsonRowSequence( InputStream in,
                                                                byte byte0 )
            throws IOException {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( (char) byte0 );
        sbuf.append( new String( IOUtils.readBytes( in, 100_000 ),
                                 StandardCharsets.UTF_8 ) );
        JSONObject json;
        try {
            json = new JSONObject( sbuf.toString() );
        }
        catch ( JSONException e ) {
            json = null;
        }
        JSONObject status = json == null ? null
                                         : json.optJSONObject( "status" );
        if ( status != null ) {
            logger_.info( "JSON status instead of data: " + status.toString() );
            return EmptyRowSequence.getInstance();
        }
        else {
            throw new IOException( "Unexpected content starting '"
                                 + (char) byte0 + "' in data stream" );
        }
    }

    /**
     * Returns an array of object arrays suitable for use as workspace
     * with a set of ParamReaders.
     *
     * @param   paramRdrs   parameter readers
     * @return  array of Object arrays, one for each of this reader's
     *          ParamReaders
     */
    private static Object[][] createResultsArray( ParamReader[] paramRdrs ) {
        Object[][] results = new Object[ paramRdrs.length ][];
        for ( int ip = 0; ip < paramRdrs.length; ip++ ) {
            results[ ip ] = new Object[ paramRdrs[ ip ].getColumnCount() ];
        }
        return results;
    }
}
