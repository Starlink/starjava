package uk.ac.starlink.table;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A StarTableWriter which outputs text to a human-readable text file.
 */
public class TextTableWriter implements StarTableWriter {

    /**
     * Maximum width for a given column.
     */
    private int maxWidth = 30;

    /**
     * Number of columns sampled to find column width.
     */
    private int sampledRows = 10;

    public String getFormatName() {
        return "text";
    }

    public boolean looksLikeFile( String location ) {
        return location.equals( "-" ) 
            || location.endsWith( ".txt" );
    }

    public void writeStarTable( StarTable startab, String location ) 
            throws IOException {

        /* Get the column headers and work out column widths for formatting. */
        int ncol = startab.getColumnCount();
        ColumnInfo[] cinfos = new ColumnInfo[ ncol ];
        int[] cwidths = new int[ ncol ];
        for ( int i = 0; i < ncol; i++ ) {
            cinfos[ i ] = startab.getColumnInfo( i );
            cwidths[ i ] = cinfos[ i ].getName().length();
        }

        for ( RowSequence rseq = startab.getRowSequence(); 
              rseq.hasNext() && rseq.getRowIndex() < sampledRows; ) {
            rseq.next();
            Object[] row = rseq.getRow();
            for ( int i = 0; i < ncol; i++ ) {
                String formatted = cinfos[ i ]
                                  .formatValue( row[ i ], maxWidth );
                if ( formatted.length() > cwidths[ i ] ) {
                    cwidths[ i ] = formatted.length();
                }
            }
        }

        for ( int i = 0; i < ncol; i++ ) {
            cwidths[ i ] = Math.min( maxWidth, cwidths[ i ] );
        }
            
        /* Get an output stream. */
        OutputStream strm = getStream( location );
 
        /* Print headings. */
        String[] heads = new String[ ncol ];
        for ( int i = 0; i < ncol; i++ ) {
            heads[ i ] = cinfos[ i ].getName();
        }
        printSeparator( strm, cwidths );
        printLine( strm, cwidths, heads );
        printSeparator( strm, cwidths );

        /* Print data. */
        for ( RowSequence rseq = startab.getRowSequence(); rseq.hasNext(); ) {
            rseq.next();
            Object[] row = rseq.getRow();
            String[] data = new String[ ncol ];
            for ( int i = 0; i < ncol; i++ ) {
                data[ i ] = cinfos[ i ].formatValue( row[ i ], cwidths[ i ] );
            }
            printLine( strm, cwidths, data );
        }
        printSeparator( strm, cwidths );

        /* Tidy up. */
        strm.flush();
    }

    private OutputStream getStream( String location ) throws IOException {
        if ( location.equals( "-" ) ) {
            return System.out;
        }
        else {
            return new FileOutputStream( location );
        }
    }

    private void printSeparator( OutputStream strm, int[] colwidths )
            throws IOException {
        for ( int i = 0; i < colwidths.length; i++ ) {
            strm.write( '+' );
            strm.write( '-' );
            for ( int j = 0; j < colwidths[ i ]; j++ ) {
                strm.write( '-' );
            }
            strm.write( '-' );
        }
        strm.write( '+' );
        strm.write( '\n' );
    }

    private void printLine( OutputStream strm, int[] colwidths, String[] data ) 
            throws IOException {
        for ( int i = 0; i < colwidths.length; i++ ) {
            strm.write( '|' );
            strm.write( ' ' );
            String datum = ( data[ i ] == null ) ? "" : data[ i ];
            int padding = colwidths[ i ] - datum.length();
            strm.write( datum.getBytes(), 0,
                        Math.min( colwidths[ i ], datum.length() ) );
            if ( padding > 0 ) {
                for ( int j = 0; j < padding; j++ ) {
                    strm.write( ' ' );
                }
            }
            strm.write( ' ' );
        }
        strm.write( '|' );
        strm.write( '\n' );
    }
    
}
