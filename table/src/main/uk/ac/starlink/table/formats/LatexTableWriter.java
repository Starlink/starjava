package uk.ac.starlink.table.formats;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.Tables;

/**
 * A StarTableWriter that outputs text to a LaTeX document.
 * A standalone document is output, but this can be stripped of the
 * header and footer if just the table element is required by looking
 * for the lines 
 * <pre>
 *    \begin{tabular}
 *       ...
 *    \end{tabular}
 * </pre>
 *
 * @author   Mark Taylor (Starlnk)
 */
public class LatexTableWriter implements StarTableWriter {

    /**
     * Returns the string "LaTeX".
     */
    public String getFormatName() {
        return "LaTeX";
    }

    /**
     * Returns true for <tt>location</tt> with a ".tex" extension.
     */
    public boolean looksLikeFile( String location ) {
        return location.endsWith( ".tex" );
    }

    public void writeStarTable( StarTable startab, String location ) 
            throws IOException {

        /* Get a stream for output. */
        OutputStream ostrm = getStream( location );
 
        /* Work out the tabular format. */
        StringBuffer tfmt = new StringBuffer( "|" );
        ColumnInfo[] colinfos = Tables.getColumnInfos( startab );
        int ncol = colinfos.length;
        int[] maxWidths = new int[ ncol ];
        for ( int i = 0; i < ncol; i++ ) {
            ColumnInfo colinfo = colinfos[ i ];
            Class clazz = colinfo.getContentClass();
            if ( Number.class.isAssignableFrom( clazz ) ) {
                tfmt.append( 'r' );
                maxWidths[ i ] = 16;
            }
            else {
                tfmt.append( 'l' );
                maxWidths[ i ] = 66;
            }
            tfmt.append( '|' );
        }

        /* Write the header information. */
        printLatexHeader( ostrm, startab );
        printLine( ostrm, "\\begin{tabular}{" + tfmt + "}" );
        printLine( ostrm, "\\hline" );

        /* Write the column headings. */
        for ( int i = 0; i < ncol; i++ ) {
            boolean first = i == 0;
            boolean last = i == ncol - 1;
            ColumnInfo colinfo = colinfos[ i ];
            String line = new StringBuffer()
                         .append( "  \\multicolumn{1}{" )
                         .append( first ? "|" : "" )
                         .append( 'c' )
                         .append( "|" )
                         .append( "}{" )
                         .append( escape( colinfo.getName() ) )
                         .append( "} " )
                         .append( last ? "\\\\" : "&" )
                         .toString();
            printLine( ostrm, line );
            
        }
        printLine( ostrm, "\\hline" );

        /* Write the data. */
        RowSequence rseq = startab.getRowSequence();
        while ( rseq.hasNext() ) {
            rseq.next();
            Object[] row = rseq.getRow();
            print( ostrm, "  " );
            for ( int i = 0; i < ncol; i++ ) {
                String datum = colinfos[ i ]
                              .formatValue( row[ i ], maxWidths[ i ] );
                if ( i > 0 ) {
                    print( ostrm, " & " );
                }
                print( ostrm, escape( datum ) );
            }
            printLine( ostrm, "\\\\" );
        }

        /* Write footer information. */
        print( ostrm, "\\hline" );
        printLine( ostrm, "\\end{tabular}" );
        printLatexFooter( ostrm, startab );

        /* Close. */
        ostrm.close();
    }

    /**
     * Outputs the header information which precedes the tabular environment.
     *
     * @param   ostrm the stream to write to
     * @startab  the StarTable which the tabular will contain
     */
    private void printLatexHeader( OutputStream ostrm, StarTable startab ) 
            throws IOException {
        printLine( ostrm, "\\documentclass{article}" );
        printLine( ostrm, "\\begin{document}" );
        printLine( ostrm, "\\begin{table}" );
    }

    /**
     * Outputs the footer information which succeeds the tabular environment.
     *
     * @param   ostrm the stream to write to
     * @startab  the StarTable which the tabular will contain
     */
    private void printLatexFooter( OutputStream ostrm, StarTable startab ) 
            throws IOException {
        String tname = startab.getName();
        if ( tname != null && tname.trim().toString().length() > 0 ) {
            printLine( ostrm, "\\caption{" + escape( tname ) + "}" );
        }
        printLine( ostrm, "\\end{table}" );
        printLine( ostrm, "\\end{document}" );
    }

    /**
     * Outputs a given string to a stream, with a newline character appended.
     *
     * @param  ostrm  the output stream
     * @param  line  the string to output
     */
    private void printLine( OutputStream ostrm, String line ) 
            throws IOException {
        print( ostrm, line );
        ostrm.write( (int) '\n' );
    }

    /**
     * Outputs a given string to a stream.
     *
     * @param  ostrm  the output stream
     * @param  str  the string to output
     */
    private void print( OutputStream ostrm, String str ) throws IOException {
        ostrm.write( str.getBytes() );
    }

    /**
     * Turns a Unicode string into an ASCII string suitable for inclusion
     * in LaTeX text - any special characters are escaped in LaTeX-friendly
     * ways.
     *
     * @param  line  the input string
     * @return  a LaTeX-friendly version of <tt>line</tt>
     */
    private String escape( String line ) {
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < line.length(); i++ ) {
            char chr = line.charAt( i );
            switch ( chr ) {
                case '$':
                case '&':
                case '%':
                case '#':
                case '_':
                case '{':
                case '}':
                    sbuf.append( '\\' ).append( chr );
                    break;
                case '~':
                case '^':
                case '\\':
                    sbuf.append( "\\verb+" ).append( chr ).append( '+' );
                    break;
                default:
                    sbuf.append( ( chr > 0 && chr < 127 ) ? chr : '?' );
            }
        }
        return sbuf.toString();
    }

    /**
     * Turns a location string into a suitable output stream.
     *
     * @param  user-supplied location for the output file
     * @return  an output stream corresponding to <tt>location</tt>
     */
    private OutputStream getStream( String location ) throws IOException {
        if ( location.equals( "-" ) ) {
            return System.out;
        }
        else {
            return new FileOutputStream( location );
        }
    }
}
