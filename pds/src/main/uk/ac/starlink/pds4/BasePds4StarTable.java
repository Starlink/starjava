package uk.ac.starlink.pds4;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.Tables;

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
                return colRdrs_[ icol ].readCell( record_ );
            }
            public Object[] getRow() {
                checkRow();
                Object[] row = new Object[ ncol_ ];
                for ( int icol = 0; icol < ncol_; icol++ ) {
                    row[ icol ] = colRdrs_[ icol ].readCell( record_ );
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
            fieldReader_ = FieldReader.getInstance( field.getFieldType() );
            offset_ = field.getFieldLocation() - 1; // field_location is 1-based
            length_ = field.getFieldLength();
            startBit_ = 0;
            endBit_ = 0;
        }

        /**
         * Reads the typed content of this field from a record buffer.
         *
         * @param  buf   byte buffer containing fixed-length record
         * @return   typed field value
         */
        Object readCell( byte[] buf ) {
            return fieldReader_
                  .readField( buf, offset_, length_, startBit_, endBit_ );
        }
    }
}
