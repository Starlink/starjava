package uk.ac.starlink.pds4;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
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
    private final ColumnReader[] colRdrs_;
    private final ByteBuffer dataBuf_;
    private final byte[] randomRecord_;
    private int randomIndex_;
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
        colRdrs_ = createColumnReaders( table.getContents() );
        ncol_ = colRdrs_.length;
        dataBuf_ = getDataBuffer();
        randomIndex_ = -1;
        randomRecord_ = new byte[ recordLength_ ];
    }

    public int getColumnCount() {
        return ncol_;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return colRdrs_[ icol ].getInfo();
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
                return colRdrs_[ icol ].readField( record_ );
            }
            public Object[] getRow() {
                checkRow();
                Object[] row = new Object[ ncol_ ];
                for ( int icol = 0; icol < ncol_; icol++ ) {
                    row[ icol ] = colRdrs_[ icol ].readField( record_ );
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
    public synchronized Object[] getRow( long lrow ) throws IOException {
        readRecord( lrow );
        Object[] row = new Object[ ncol_ ];
        for ( int icol = 0; icol < ncol_; icol++ ) {
            row[ icol ] = colRdrs_[ icol ].readField( randomRecord_ );
        }
        return row;
    }

    @Override
    public synchronized Object getCell( long lrow, int icol )
            throws IOException {
        readRecord( lrow );
        return colRdrs_[ icol ].readField( randomRecord_ );
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
                    row[ icol ] = colRdrs_[ icol ].readField( record );
                }
                return row;
            }
            public Object getCell( int icol ) {
                return colRdrs_[ icol ].readField( record );
            }
            public void close() {
            }
        };
    }

    /**
     * Ensures that the randomRecord_ member contains the byte data
     * for a given row index.  It will only be read if the row index
     * is different from last time.
     *
     * @param  lrow  row index
     */
    private synchronized void readRecord( long lrow ) {
        int irow = Tables.checkedLongToInt( lrow );
        if ( irow != randomIndex_ ) {
            dataBuf_.position( recordLength_ * irow );
            dataBuf_.get( randomRecord_ );
            randomIndex_ = irow;
        }
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
     * Returns an array of column readers for a given record structure.
     *
     * @param  items  fields and groups constituting this table's records
     * @return  column reader array
     */
    private static ColumnReader[] createColumnReaders( RecordItem[] items ) {
        List<ColumnReader> list = new ArrayList<>();
        for ( RecordItem item : items ) {
            if ( item instanceof Field ) {
                list.add( new ScalarColumnReader( (Field) item ) );
            }
            else if ( item instanceof Group ) {
                Group group = (Group) item;
                if ( group.getRepetitions() > 0 ) {
                    for ( RecordItem subItem : group.getContents() ) {
                        if ( subItem instanceof Field ) {
                            Field field = (Field) subItem;
                            FieldReader<?,?> frdr =
                                FieldReader
                               .getInstance( field.getFieldType(),
                                             field.getBlankConstants() );
                            list.add( createVectorColumnReader( field, group,
                                                                frdr ) );
                        }
                        else if ( subItem instanceof Group ) {
                            logger_.warning( "Omit nested group" );
                        }
                    }
                }
            }
        }
        return list.toArray( new ColumnReader[ 0 ] );
    }

    /**
     * Constructs a VectorColumnReader.
     * This method required for generic gymnastics.
     *
     * @param  field  field
     * @param  group   group containing this field
     * @param  fieldReader  reader corresponding to field
     * @return   new reader
     */
    private static <S,A> VectorColumnReader<S,A>
            createVectorColumnReader( Field field, Group group,
                                      FieldReader<S,A> fieldReader ) {
        return new VectorColumnReader<S,A>( field, group, fieldReader );
    }

    /**
     * Defines how typed data is read from a record buffer.
     */
    private static interface ColumnReader {

        /**
         * Returns the column metadata for this reader.
         *
         * @return  content class
         */
        ColumnInfo getInfo();

        /**
         * Reads the typed content of this field from a record buffer.
         *
         * @param  record   byte array giving a whole record
         * @return   typed field value
         */
        Object readField( byte[] record );
    }

    /**
     * ColumnReader implementation for scalar (non-grouped) fields.
     */
    private static class ScalarColumnReader implements ColumnReader {

        final FieldReader<?,?> fieldReader_;
        final int offset_;
        final int length_;
        final ColumnInfo info_;
        final int startBit_;
        final int endBit_;

        /**
         * Constructor.
         *
         * @param   field  field information
         */
        ScalarColumnReader( Field field ) {
            fieldReader_ = FieldReader.getInstance( field.getFieldType(),
                                                    field.getBlankConstants() );
            offset_ = field.getFieldLocation() - 1; // field_location is 1-based
            length_ = field.getFieldLength();
            info_ = new ColumnInfo( field.getName(),
                                    fieldReader_.getScalarClass(),
                                    field.getDescription() );
            info_.setUnitString( field.getUnit() );
            startBit_ = 0;
            endBit_ = 0;
        }

        public ColumnInfo getInfo() {
            return info_;
        }

        public Object readField( byte[] record ) {
            return fieldReader_
                  .readScalar( record, offset_, length_, startBit_, endBit_ );
        }
    }

    /**
     * ColumnReader implementation for vector (grouped) fields.
     */
    private static class VectorColumnReader<S,A> implements ColumnReader {

        final FieldReader<S,A> fieldReader_;
        final ColumnInfo info_;
        final int offset0_;
        final int length_;
        final int step_;
        final int nrep_;
        final int startBit_;
        final int endBit_;

        /**
         * Constructor.
         *
         * @param  field  field
         * @param  group   group containing this field
         * @param  fieldReader  reader corresponding to field
         */
        VectorColumnReader( Field field, Group group,
                            FieldReader<S,A> fieldReader ) {
            fieldReader_ = fieldReader;
            nrep_ = group.getRepetitions();
            offset0_ = group.getGroupLocation() - 1   // both are 1-based
                     + field.getFieldLocation() - 1;
            length_ = field.getFieldLength();
            step_ = group.getGroupLength() / nrep_;
            info_ = new ColumnInfo( field.getName(),
                                    fieldReader_.getArrayClass(),
                                    field.getDescription() );
            info_.setUnitString( field.getUnit() );
            info_.setShape( new int[] { nrep_ } );
            startBit_ = 0;
            endBit_ = 0;
        }

        public ColumnInfo getInfo() {
            return info_;
        }

        public A readField( byte[] record ) {
            A array = fieldReader_.createArray( nrep_ );
            for ( int i = 0; i < nrep_; i++ ) {
                fieldReader_.readElement( record,
                                          offset0_ + i * step_, length_,
                                          startBit_, endBit_, array, i );
            }
            return array;
        }
    }
}
