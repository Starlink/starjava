package uk.ac.starlink.table.formats;

import java.awt.datatransfer.DataFlavor;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.util.DataSource;

/**
 * Can be used to make sense of the COINS data from the UK Government.
 * It is CSV-like, but has '@' characters as the separators.
 * Tested on file 
 * <a href="http://source.data.gov.uk/data/finance/coins/2010-06-04/fact_table_extract_2009_10.zip"
 * >/fact_table_extract_2009_10.zip</a> from http://data.gov.uk/.
 * Clearly, this is of limited use for astronomical analysis, but it may
 * provide a template for implementation of other custom CSV-like
 * data sets.
 *
 * @author   Mark Taylor
 * @since    30 Jun 2010
 */
public class CoinsTableBuilder implements TableBuilder {

    /**
     * Returns "COINS".
     */
    public String getFormatName() {
        return "COINS";
    }

    public boolean looksLikeFile( String location ) {
        return false;
    }

    public boolean canImport( DataFlavor flavor ) {
        return false;
    }

    public StarTable makeStarTable( DataSource datsrc, boolean wantRandom,
                                    StoragePolicy policy )
            throws IOException {
        return new CoinsStarTable( datsrc );
    }

    public void streamStarTable( InputStream in, TableSink sink, String pos )
            throws IOException {
        throw new TableFormatException( "Can't stream ASCII format tables" );
    }


    /**
     * StarTable implmentation for COINS data.
     */
    private static class CoinsStarTable extends StreamStarTable {
    
        private static final char DELIMITER = '@';
    
        public CoinsStarTable( DataSource datsrc )
               throws TableFormatException, IOException {
            super( StandardCharsets.UTF_8 );
            init( datsrc );
        }
    
        @Override
        protected BufferedReader getReader() throws IOException {
            BufferedReader in = super.getReader();
    
            /* The first row is known to be a non-data row. */
            readRow( in );
            return in;
        }
    
        protected RowEvaluator.Metadata obtainMetadata()
                throws TableFormatException, IOException {
            BufferedReader in = super.getReader();
            if ( in.read() != 0xff || in.read() != 0xfe ) {
                throw new TableFormatException( "Unexpected/bad BOM" );
            }
            String[] colNames = readRow( in ).toArray( new String[ 0 ] );
    
            RowEvaluator evaluator = new RowEvaluator();
            for ( List<String> row; ( row = readRow( in ) ) != null; ) {
                evaluator.submitRow( row );
            }
            in.close();
            RowEvaluator.Metadata meta = evaluator.getMetadata();
            if ( meta.nrow_ == 0 ) {
                throw new TableFormatException( "No rows" );
            }
            ColumnInfo[] colinfos = meta.colInfos_;
            RowEvaluator.Decoder<?>[] decoders = meta.decoders_;
            int ncol = meta.ncol_;
            long nrow = meta.nrow_;
            for ( int icol = 0; icol < ncol; icol++ ) {
                colinfos[ icol ].setName( colNames[ icol ] );
            }
            return new RowEvaluator.Metadata( colinfos, decoders, nrow );
        }
    
        @SuppressWarnings("fallthrough")
        protected List<String> readRow( BufferedReader in )
                throws IOException {
            List<String> cellList = new ArrayList<>();
            StringBuffer buffer = new StringBuffer();
            for ( boolean endLine = false; ! endLine; ) {
                int c1 = in.read();
                int c2 = c1 >= 0 ? in.read() : -1;
                if ( c1 < 0 || c2 < 0 ) {
                    return null;
                }
                char c = (char) ( c1 | ( c2 << 8 ) );
                switch ( c ) {
                    case '\r':
                        break;
                    case '\n':
                        endLine = true;
                        if ( cellList.size() <= 1 ) {
                            cellList.clear();
                        }
                        // no break here
                    case DELIMITER:
                        String str = buffer.toString();
                        buffer.setLength( 0 );
                        cellList.add( "NULL".equals( str ) ? null : str );
                        break;
                    default:
                        buffer.append( c );
                }
            }
            return cellList.size() <= 1 ? null : cellList;
        }
    }
}
