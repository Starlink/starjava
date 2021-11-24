package uk.ac.starlink.pds4;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
    private final ColumnInfo[] colInfos_;
    private final FieldReader<?>[] colRdrs_;
    
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
        Field[] fields = table.getFields();
        ncol_ = fields.length;
        colInfos_ = new ColumnInfo[ ncol_ ];
        colRdrs_ = new FieldReader<?>[ ncol_ ];
        for ( int ic = 0; ic < ncol_; ic++ ) {
            Field field = fields[ ic ];
            FieldReader<?> rdr =
                FieldReader.getInstance( field.getFieldType() );
            ColumnInfo info =
                new ColumnInfo( field.getName(), rdr.getContentClass(),
                                field.getDescription() );
            info.setUnitString( field.getUnit() );
            colRdrs_[ ic ] = rdr;
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
        final InputStream in = getDataStream();
        final ByteList buf = new ByteList( 1024 );
        final int[] iStarts = new int[ ncol_ ];
        final int[] iEnds = new int[ ncol_ ];
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
                int off = iStarts[ icol ];
                int leng = iEnds[ icol ] - off;
                return leng == 0
                     ? null
                     : colRdrs_[ icol ].readField( bdata_, off, leng, 0, 0 );
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
                        iEnds[ iField ] = buf.size();
                        iField++;
                        if ( iField < ncol_ ) {
                            iStarts[ iField ] = buf.size();
                        }
                    }

                    /* Record ends can be CR+LF or just LF.  It is possible
                     * to find out which we're expecting from the label,
                     * but here we don't bother; just look for an LF, and
                     * squash any CR that's immediately preceding it.
                     * See Goldfarb's First Law of Text Processing. */
                    else if ( ! inQuote && c == 0x0a ) {
                        if ( iField < ncol_ ) {
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
}
