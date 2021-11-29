package uk.ac.starlink.pds4;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowAccess;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.URLUtils;

/**
 * Concrete Pds4StarTable subclass for fixed-length-record
 * (binary and character) PDS tables.
 *
 * @author   Mark Taylor
 * @since    24 Nov 2021
 */
public class BasePds4StarTable extends Pds4StarTable {

    private final int ncol_;
    private final int recordLength_;
    private final ColumnInfo[] colInfos_;
    private final ColumnReader[] colRdrs_;
    private final ByteBuffer dataBuf_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.pds4" );

    /**
     * Constructor.
     *
     * @param  table  table object on which this table is based
     * @param  contextUrl   parent URL for the PDS4 label
     */
    public BasePds4StarTable( BaseTable table, URL contextUrl )
            throws IOException {
        super( table, contextUrl );
        recordLength_ = Tables.checkedLongToInt( table.getRecordLength() );
        Field[] fields = table.getFields();
        ncol_ = fields.length;
        colInfos_ = new ColumnInfo[ ncol_ ];
        colRdrs_ = new ColumnReader[ ncol_ ];
        for ( int ic = 0; ic < ncol_; ic++ ) {
            Field field = fields[ ic ];
            ColumnReader crdr = new ColumnReader( field );
            ColumnInfo info =
                new ColumnInfo( field.getName(),
                                crdr.fieldReader_.getContentClass(),
                                field.getDescription() );
            info.setUnitString( field.getUnit() );
            colRdrs_[ ic ] = crdr;
            colInfos_[ ic ] = info;
        }
        dataBuf_ = getDataBuffer();
    }

    public int getColumnCount() {
        return ncol_;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return colInfos_[ icol ];
    }

    public RowSequence getRowSequence() throws IOException {
        InputStream in = getDataStream();
        final long nrow = getRowCount();
        return new RowSequence() {
            long irow_;
            final byte[] record_ = new byte[ recordLength_ ];
            public boolean next() throws IOException {
                if ( irow_ >= nrow ) {
                    return false;
                }
                int ioff = 0;
                while ( ioff < recordLength_ ) {
                    int nb = in.read( record_, ioff, recordLength_ - ioff );
                    if ( nb < 0 ) {
                        return false;
                    }
                    ioff += nb;
                }
                if ( ioff == recordLength_ ) {
                    irow_++;
                    return true;
                }
                else {
                    throw new IOException( "EOF midway through record" );
                }
            }
            public Object getCell( int icol ) {
                checkRow();
                return colRdrs_[ icol ].readFromRecord( record_ );
            }
            public Object[] getRow() {
                checkRow();
                Object[] row = new Object[ ncol_ ];
                for ( int icol = 0; icol < ncol_; icol++ ) {
                    row[ icol ] = colRdrs_[ icol ].readFromRecord( record_ );
                }
                return row;
            }
            public void close() throws IOException {
                in.close();
            }

            /**
             * Throws a suitable exception if there is no current row.
             */
            void checkRow() {
                if ( irow_ == 0 ) {
                    throw new IllegalStateException( "No current row" );
                }
            }
        };
    }

    @Override
    public boolean isRandom() {
        return dataBuf_ != null;
    }

    @Override
    public Object[] getRow( long lrow ) throws IOException {
        byte[] record = new byte[ recordLength_ ];
        synchronized( dataBuf_ ) {
            dataBuf_.position( recordLength_ * Tables.checkedLongToInt( lrow ));
            dataBuf_.get( record );
        }
        Object[] row = new Object[ ncol_ ];
        for ( int icol = 0; icol < ncol_; icol++ ) {
            row[ icol ] = colRdrs_[ icol ].readFromRecord( record );
        }
        return row;
    }

    @Override
    public Object getCell( long lrow, int icol ) throws IOException {
        ColumnReader crdr = colRdrs_[ icol ];
        byte[] cellBuf = new byte[ crdr.length_ ];
        synchronized ( dataBuf_ ) {
            dataBuf_.position( recordLength_ * Tables.checkedLongToInt( lrow )
                             + crdr.offset_ );
            dataBuf_.get( cellBuf );
        }
        return crdr.readFromCell( cellBuf );
    }

    @Override
    public RowAccess getRowAccess() throws IOException {
        ByteBuffer dbuf = dataBuf_.duplicate();
        byte[] record = new byte[ recordLength_ ];
        return new RowAccess() {
            public void setRowIndex( long lrow ) {
                dbuf.position( recordLength_ * Tables.checkedLongToInt( lrow ));
                dbuf.get( record );
            }
            public Object[] getRow() {
                Object[] row = new Object[ ncol_ ];
                for ( int icol = 0; icol < ncol_; icol++ ) {
                    row[ icol ] = colRdrs_[ icol ].readFromRecord( record );
                }
                return row;
            }
            public Object getCell( int icol ) {
                return colRdrs_[ icol ].readFromRecord( record );
            }
            public void close() {
            }
        };
    }

    /**
     * Attempts to return a mapped byte buffer corresponding to the region
     * of the data file containing the data for this table.
     *
     * @return  data buffer, or null if buffer is unavailable or cannot
     *          be mapped for some reason
     */
    private ByteBuffer getDataBuffer() throws IOException {
        File file = URLUtils.urlToFile( getDataUrl().toString() );
        if ( file == null || ! file.canRead() ) {
            return null;
        }
        long size = getRowCount() * recordLength_;
        if ( size > Integer.MAX_VALUE ) {
            return null;
        }
        int isize = (int) size;
        try {
            FileChannel channel = new FileInputStream( file ).getChannel();
            return channel.map( FileChannel.MapMode.READ_ONLY,
                                getDataOffset(), isize );
        }
        catch ( IOException e ) {
            logger_.log( Level.INFO, "Failed to map file: " + file, e );
            return null;
        }
    }

    /**
     * Aggregates a FieldReader with information about the position of a
     * given field within each record.
     */
    private static class ColumnReader {

        final FieldReader<?> fieldReader_;
        final int offset_;
        final int length_;
        final int startBit_;
        final int endBit_;

        /**
         * Constructor.
         *
         * @param   field  field information
         */
        ColumnReader( Field field ) {
            fieldReader_ = FieldReader.getInstance( field.getFieldType(),
                                                    field.getBlankConstants() );
            offset_ = field.getFieldLocation() - 1; // field_location is 1-based
            length_ = field.getFieldLength();
            startBit_ = 0;
            endBit_ = 0;
        }

        /**
         * Reads the typed content of this field from a record buffer.
         *
         * @param  recordBuf   byte buffer containing fixed-length record
         * @return   typed field value
         */
        Object readFromRecord( byte[] recordBuf ) {
            return fieldReader_
                  .readField( recordBuf, offset_, length_, startBit_, endBit_ );
        }

        /**
         * Reads the typed content of this field from a buffer containing
         * only the value (at offset zero).
         *
         * @param  cellBuf  buffer containing cell content only
         * @return   typed field value
         */
        Object readFromCell( byte[] cellBuf ) {
            return fieldReader_
                  .readField( cellBuf, 0, length_, startBit_, endBit_ );
        }
    }
}
