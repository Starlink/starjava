package uk.ac.starlink.mirage;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;

/**
 * Turns StarTables into Mirage format tables.
 */
public class MirageFormatter {

    protected PrintStream strm;

    /**
     * Constructs a new formatter which will write to a given stream.
     *
     * @param  strm  the stream into which mirage format tables will be 
     *               written
     */
    public MirageFormatter( PrintStream strm ) {
        this.strm = strm;
    }

    /**
     * Writes a StarTable to a stream in Mirage format.
     *
     * @param  table  a StarTable object to write
     * @throws IOException  if there is a write error
     */
    public void writeMirageFormat( StarTable table ) throws IOException {

        /* Header line. */
        strm.println( "#\n# Written by " + this );

        /* Characterise the table columns by looking at the headers. */
        int ncol = table.getColumnCount();
        String[] colNames = new String[ ncol ];
        boolean[] isUsed = new boolean[ ncol ];
        boolean[] isText = new boolean[ ncol ];
        int nText = 0;
        for ( int i = 0; i < ncol; i++ ) {
            ColumnInfo cinfo = table.getColumnInfo( i );
            String colName = cinfo.getName();
            if ( colName != null ) {
                colName = colName.replaceAll( "\\s+", "_" );
                colNames[ i ] = colName;
                Class clazz = cinfo.getContentClass();
                if ( Number.class.isAssignableFrom( clazz ) ) {
                    isUsed[ i ] = true;
                }
                else if ( clazz.equals( String.class ) ) {
                    isUsed[ i ] = true;
                    isText[ i ] = true;
                    nText++;
                }
                else {
                    strm.println( "# Omitted column " + i + ": " + cinfo );
                }
            }
        }

        /* Print out the names of the columns we will use. */
        strm.println( "#\n# Column names" );
        strm.print( "format var" );
        for ( int i = 0; i < ncol; i++ ) {
            if ( isUsed[ i ] ) {
                strm.print( " " + colNames[ i ] );
            }
        }
        strm.println();

        /* Print out the name of text columns. */
        if ( nText > 0 ) {
            strm.println( "#\n# Text columns" );
            for ( int i = 0; i < ncol; i++ ) {
                if ( isText[ i ] ) {
                    strm.println( "format text " + colNames[ i ] );
                }
            }
        }

        /* Print out the table contents. */
        strm.println( "#\n# Table data" );
        RowSequence rseq = table.getRowSequence();
        try {
            while ( rseq.next() ) {
                Object[] row = rseq.getRow();
                for ( int i = 0; i < ncol; i++ ) {
                    if ( isUsed[ i ] ) {
                        Object datum = row[ i ];
                        String cell;
                        if ( datum == null ) {
                            cell = "<blank>";
                        }
                        else {
                            cell = row[ i ].toString().trim();

                            /* For text data, replace any whitespace sequence 
                             * with a single non-breaking space so as not to
                             * confuse Mirage into thinking there is more than
                             * one field. */
                            if ( isText[ i ] ) {
                                if ( cell.length() == 0 ) {
                                    cell = "<blank>";
                                }
                                else {
                                    cell = cell.replaceAll( "\\s+", "_" );
                                }
                            }
                        }
                        strm.print( cell );
                        strm.print( ' ' );
                    }
                }
                strm.println();
            }
        }
        finally {
            rseq.close();
        }
    }

    public String toString() {
        return getClass().getName();
    }

    /**
     * Writes a table to standard output in Mirage format.
     *
     * @param  args  a 1-element array giving the location of the 
     *         table to print
     */
    public static void main( String[] args ) throws IOException {
        String usage = "Usage: MirageFormatter table\n";
        if ( args.length != 1 ) {
            System.err.println( usage );
            System.exit( 1 );
        }
        StarTable tab = new StarTableFactory( false )
                       .makeStarTable( args[ 0 ] );
        if ( tab == null ) {
            System.err.println( "No known table " + args[ 0 ] );
            System.exit( 1 );
        }
        new MirageFormatter( System.out ).writeMirageFormat( tab );
    }
}
