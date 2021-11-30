package uk.ac.starlink.pds4;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.util.ByteList;

/**
 * Concrete Pds4StarTable sublclass for delimited tables.
 *
 * @author   Mark Taylor
 * @since    24 Nov 2021
 */
public class DelimitedPds4StarTable extends Pds4StarTable {

    private final int delim_;
    private final int ncol_;
    private final int nfield_;
    private final ColumnReader[] colRdrs_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.pds4" );
    
    /**
     * Constructor.
     *
     * @param  table  table object on which this table is based
     * @param  contextUrl   parent URL for the PDS4 label
     */
    public DelimitedPds4StarTable( DelimitedTable table, URL contextUrl )
            throws IOException {
        super( table, contextUrl );
        delim_ = (int) table.getFieldDelimiter();
        colRdrs_ = createColumnReaders( table.getContents() );
        ncol_ = colRdrs_.length;
        nfield_ = getFieldCount( table.getContents() );
    }

    public int getColumnCount() {
        return ncol_;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return colRdrs_[ icol ].getInfo();
    }

    public RowSequence getRowSequence() throws IOException {
        final InputStream in = getDataStream();
        final ByteList buf = new ByteList( 1024 );
        final int[] iStarts = new int[ nfield_ ];
        final int[] iEnds = new int[ nfield_ ];
        final long nrow = getRowCount();
        return new RowSequence() {
            long irow_;
            byte[] bdata_;
            public boolean next() throws IOException {
                if ( irow_ < nrow && readLine() ) {
                    irow_++;
                    return true;
                }
                else {
                    return false;
                }
            }
            public Object getCell( int icol ) {
                checkRow();
                return doGetCell( icol );
            }
            public Object[] getRow() {
                checkRow();
                Object[] row = new Object[ ncol_ ];
                for ( int ic = 0; ic < ncol_; ic++ ) {
                    row[ ic ] = doGetCell( ic );
                }
                return row;
            }
            public void close() throws IOException {
                in.close();
            }

            /**
             * Reads a cell in the current row without checking that there
             * is a current row.
             *
             * @param  icol  column index
             * @return   cell value
             */
            Object doGetCell( int icol ) {
                return colRdrs_[ icol ].readField( bdata_, iStarts, iEnds );
            }

            /**
             * Throws a suitable exception if there is no current row.
             */
            void checkRow() {
                if ( irow_ == 0 ) {
                    throw new IllegalStateException( "No current row" );
                }
            }

            /**
             * Reads data from the input stream into a byte buffer,
             * keeping track of where the start and end of each field is.
             */
            boolean readLine() throws IOException {
                buf.clear();
                int iField = 0;
                iStarts[ iField ] = 0;
                boolean inQuote = false;

                /* See DSV format as specified in sec 4C.1 of PDS Standards
                 * Reference 1.16.0. */
                while ( true ) {
                    int c = in.read();
                    if ( c < 0 ) {
                        return false;
                    }
                    else if ( c == '"' && buf.size() == iStarts[ iField ] ) {
                        inQuote = true;
                    }
                    else if ( c == '"' && inQuote ) {
                        inQuote = false;
                    }
                    else if ( ! inQuote && c == delim_ ) {
                        if ( iField < nfield_ ) {
                            iEnds[ iField ] = buf.size();
                        }
                        iField++;
                        if ( iField < nfield_ ) {
                            iStarts[ iField ] = buf.size();
                        }
                    }

                    /* Record ends can be CR+LF or just LF.  It is possible
                     * to find out which we're expecting from the label,
                     * but here we don't bother; just look for an LF, and
                     * squash any CR that's immediately preceding it.
                     * See Goldfarb's First Law of Text Processing. */
                    else if ( ! inQuote && c == 0x0a ) {
                        if ( iField < nfield_ ) {
                            iEnds[ iField ] = buf.size();
                            if ( buf.get( iEnds[ iField ] - 1 ) == 0x0d ) {
                                iEnds[ iField ]--;
                            }
                        }
                        bdata_ = buf.getByteBuffer();
                        return true;
                    }
                    else {
                        buf.add( (byte) c );
                    }
                }
            }
        };
    }

    /**
     * Returns an array of column readers for a given record structure.
     *
     * @param  items  fields and groups constituting this table's records
     * @return  column reader array
     */
    private static ColumnReader[] createColumnReaders( RecordItem[] items ) {
        List<ColumnReader> list = new ArrayList<>();
        int ifield = 0;
        for ( RecordItem item : items ) {
            if ( item instanceof Field ) {
                list.add( new ScalarColumnReader( (Field) item, ifield ) );
                ifield++;
            }
            else if ( item instanceof Group ) {
                Group group = (Group) item;
                if ( group.getRepetitions() > 0 ) {
                    int isub = 0;
                    for ( RecordItem subItem : group.getContents() ) {
                        if ( subItem instanceof Field ) {
                            Field field = (Field) subItem;
                            FieldReader<?,?> frdr =
                                FieldReader
                               .getInstance( field.getFieldType(),
                                             field.getBlankConstants() );
                            ColumnReader crdr =
                                createVectorColumnReader( field, group, frdr,
                                                          ifield + isub );
                            list.add( crdr );
                            isub++;
                        }
                        else if ( subItem instanceof Group ) {
                            logger_.warning( "Omit nested group" );
                            Group subGroup = (Group) subItem;
                            isub += getFieldCount( subGroup );
                        }
                    }
                }
                ifield += getFieldCount( group );
            }
        }
        return list.toArray( new ColumnReader[ 0 ] );
    }

    /**
     * Returns the number of scalar-equivalent fields represented by a Group.
     *
     * @param  group  group
     * @return   number of fields in record within the given group
     */
    private static int getFieldCount( Group group ) {
        return getFieldCount( group.getContents() ) * group.getRepetitions();
    }

    /**
     * Returns the number of scalar-equivalent fields represented by a
     * sequence of Fields and Groups.
     *
     * @param  items  fields and groups
     * @return  number of fields in record covered by given items
     */
    private static int getFieldCount( RecordItem[] items ) {
        int n = 0;
        for ( RecordItem item : items ) {
            if ( item instanceof Field ) {
                n++;
            }
            else if ( item instanceof Group ) {
                n += getFieldCount( (Group) item );
            }
        }
        return n;
    }

    /**
     * Constructs a VectorColumnReader.
     * This method required for generic gymnastics.
     *
     * @param  field  field
     * @param  group   group containing this field
     * @param  fieldReader  reader corresponding to field
     * @param  iField0  index of first field in record used by this reader
     * @return   new reader
     */
    private static <S,A> VectorColumnReader<S,A>
            createVectorColumnReader( Field field, Group group,
                                      FieldReader<S,A> frdr, int iField0 ) {
        return new VectorColumnReader<S,A>( field, group, frdr, iField0 );
    }

    /**
     * Defines how typed data is read from a record.
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
         * @param  iStarts  field index start offsets into record
         * @param  iEnds    field index end offsets into record
         * @return   typed field value
         */
        Object readField( byte[] record, int[] iStarts, int[] iEnds );
    }

    /**
     * ColumnReader implementation for scalar (non-grouped) fields.
     */
    private static class ScalarColumnReader implements ColumnReader {

        final FieldReader<?,?> fieldReader_;
        final int iField_;
        final ColumnInfo info_;
        final int startBit_;
        final int endBit_;

        /**
         * Constructor.
         *
         * @param  field  field object
         * @param  iField  index of field into record
         */
        ScalarColumnReader( Field field, int iField ) {
            fieldReader_ = FieldReader.getInstance( field.getFieldType(),
                                                    field.getBlankConstants() );
            iField_ = iField;
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

        public Object readField( byte[] record, int[] iStarts, int[] iEnds ) {
            int ioff = iStarts[ iField_ ];
            int leng = iEnds[ iField_ ] - ioff;
            return fieldReader_.readScalar( record, ioff, leng,
                                            startBit_, endBit_ );
        }
    }

    /**
     * ColumnReader implementation for vector (grouped) fields.
     */
    private static class VectorColumnReader<S,A> implements ColumnReader {

        final FieldReader<S,A> fieldReader_;
        final ColumnInfo info_;
        final int iField0_;
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
         * @param  iField0   index of first field in record used by this reader
         */
        VectorColumnReader( Field field, Group group,
                            FieldReader<S,A> fieldReader, int iField0 ) {
            fieldReader_ = fieldReader;
            iField0_ = iField0;
            nrep_ = group.getRepetitions();
            step_ = getFieldCount( group ) / nrep_; // is it?
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

        public A readField( byte[] record, int[] iStarts, int[] iEnds ) {
            A array = fieldReader_.createArray( nrep_ );
            for ( int i = 0; i < nrep_; i++ ) {
                int ifield = iField0_ + i * step_;
                int ioff = iStarts[ ifield ];
                int leng = iEnds[ ifield ] - ioff;
                fieldReader_.readElement( record, ioff, leng,
                                          startBit_, endBit_,
                                          array, i );
            }
            return array;
        }
    }
}
