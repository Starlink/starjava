package uk.ac.starlink.table.formats;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.MultiStarTableWriter;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.ConfigMethod;
import uk.ac.starlink.util.URLUtils;

/**
 * A StarTableWriter that outputs text to HTML.
 * Depending on the value of the <code>standalone</code> attribute, 
 * the output may either be a complete HTML document or just a 
 * &lt;TABLE&gt; element suitable for inserting into an existing document.
 * The output HTML is intended to conform to HTML 3.2 or 4.01,
 * depending on options.
 *
 * @author   Mark Taylor (Starlink)
 * @see      <a href="http://www.w3.org/TR/REC-html32#table">HTML 3.2</a>
 * @see      <a href="http://www.w3.org/TR/html401/struct/tables.html"
 *              >HTML 4.01</a>
 */
public class HTMLTableWriter extends DocumentedStreamStarTableWriter
                             implements MultiStarTableWriter {

    private boolean standalone_;
    private boolean useRowGroups_;
    private int maxWidth_;
    private static final int DFLT_MAX_WIDTH = 200;

    /**
     * Constructs a new writer with default characteristics.
     */
    public HTMLTableWriter() {
        this( false, true );
    }

    /**
     * Constructs a new writer indicating whether it will produce complete
     * or partial HTML documents.
     */
    @SuppressWarnings("this-escape")
    public HTMLTableWriter( boolean standalone, boolean useRowGroups ) {
        super( new String[] { "html", "htm" } );
        setStandalone( standalone );
        useRowGroups_ = useRowGroups;
        maxWidth_ = DFLT_MAX_WIDTH;
    }

    /**
     * Sets whether output tables should be complete HTML documents.
     *
     * @param   standalone  true if the output document should be a
     *          complete HTML document
     */
    @ConfigMethod(
        property = "standalone",
        doc = "If true, the output is a freestanding HTML document "
            + "complete with HTML, HEAD and BODY tags. "
            + "If false, the output is just a TABLE element."
    )
    public void setStandalone( boolean standalone ) {
        standalone_ = standalone;
    }

    /**
     * Indicates whether output tables will be complete HTML documents.
     *
     * @return  true if the output documents will be complete HTML docs
     */
    public boolean isStandalone() {
        return standalone_;
    }

    /**
     * Sets the maximum output width in characters for a single cell.
     *
     * @param   maxWidth  new maximum cell width
     */
    @ConfigMethod(
        property = "maxCell",
        doc = "Maximum width in characters of an output table cell. "
            + "Cells longer than this will be truncated."
    )
    public void setMaxWidth( int maxWidth ) {
        maxWidth_ = maxWidth;
    }

    /**
     * Returns the maximum output width in characters for a single cell.
     *
     * @return  maximum cell width
     */
    public int getMaxWidth() {
        return maxWidth_;
    }

    public String getFormatName() {
        return "HTML";
    }

    public String getMimeType() {
        return "text/html";
    }

    public boolean docIncludesExample() {
        return true;
    }

    public String getXmlDescription() {
        return String.join( "\n",
            "<p>Writes a basic HTML <code>TABLE</code> element",
            "suitable for use as a web page or for insertion into one.",
            "</p>",
        "" );
    }

    public void writeStarTable( StarTable table, OutputStream out )
            throws IOException {
        if ( standalone_ ) {
            printHeader( out, table );
        }
        writeTableElement( table, out );
        if ( standalone_ ) {
            printFooter( out );
        }
    }

    public void writeStarTables( TableSequence tableSeq, OutputStream out )
            throws IOException {
        if ( standalone_ ) {
            printHeader( out, null );
        }
        for ( StarTable table; ( table = tableSeq.nextTable() ) != null; ) {
            printLine( out, "<P>" );
            writeTableElement( table, out );
            printLine( out, "</P>" );
        }
        if ( standalone_ ) {
            printFooter( out );
        }
    }

    public void writeStarTables( TableSequence tableSeq, String location,
                                 StarTableOutput sto ) throws IOException {
        OutputStream out = sto.getOutputStream( location );
        try {
            out = new BufferedOutputStream( out );
            writeStarTables( tableSeq, out );
            out.flush();
        }
        finally {
            out.close();
        }
    }

    private void writeTableElement( StarTable table, OutputStream ostrm )
            throws IOException {

        /* Get an iterator over the table data. */
        RowSequence rseq = table.getRowSequence();

        /* Output table header. */
        try {
            printLine( ostrm, "<TABLE BORDER='1'>" );
            String tname = table.getName();
            if ( tname != null ) {
                printLine( ostrm,
                           "<CAPTION><STRONG>" + tname +
                           "</STRONG></CAPTION>" );
            }

            /* Output column headings. */
            int ncol = table.getColumnCount();
            ColumnInfo[] colinfos = Tables.getColumnInfos( table );
            String[] names = new String[ ncol ];
            String[] units = new String[ ncol ];
            boolean hasUnits = false;
            for ( int icol = 0; icol < ncol; icol++ ) {
                ColumnInfo colinfo = colinfos[ icol ];
                String name = colinfo.getName();
                String unit = colinfo.getUnitString();
                if ( unit != null ) {
                    hasUnits = true;
                    unit = "(" + unit + ")";
                }
                names[ icol ] = name;
                units[ icol ] = unit;
            }
            String[] headings = new String[ ncol ];
            for ( int icol = 0; icol < ncol; icol++ ) {
                String heading = names[ icol ];
                String unit = units[ icol ];
                if ( hasUnits ) {
                    heading += "<BR>";
                    if ( unit != null ){
                        heading += "(" + unit + ")";
                    }
                }
                headings[ icol ] = heading;
            }
            if ( useRowGroups_ ) {
                printLine( ostrm, "<THEAD>" );
            }
            outputRow( ostrm, "TH", null, names );
            if ( hasUnits ) {
                outputRow( ostrm, "TH", null, units );
            }

            /* Separator. */
            printLine( ostrm, "<TR><TD colspan='" + ncol + "'></TD></TR>" );
            if ( useRowGroups_ ) {
                printLine( ostrm, "</THEAD>" );
            }

            /* Output the table data. */
            if ( useRowGroups_ ) {
                printLine( ostrm, "<TBODY>" );
            }
            while ( rseq.next() ) {
                Object[] row = rseq.getRow();
                String[] cells = new String[ ncol ];
                for ( int icol = 0; icol < ncol; icol++ ) {
                    cells[ icol ] = colinfos[ icol ]
                                   .formatValue( row[ icol ], getMaxWidth() );
                }
                outputRow( ostrm, "TD", null, cells );
            }
            if ( useRowGroups_ ) {
                printLine( ostrm, "</TBODY>" );
            }

            /* Finish up. */
            printLine( ostrm, "</TABLE>" );
        }
        finally {
            rseq.close();
        }
    }

    /**
     * Outputs a row of header or data cells.
     * 
     * @param  ostrm  the stream for output
     * @param  tagname   the name of the element in which to wrap each
     *         cell ("TH" or "TD")
     * @param  attlist  any attributes to put on the elements
     * @param  values  the array of values providing cell contents
     */
    private void outputRow( OutputStream ostrm, String tagname, 
                            String attlist, String[] values ) 
            throws IOException {
        int ncol = values.length;
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "<TR>" );
        for ( int icol = 0; icol < ncol; icol++ ) {
            sbuf.append( ' ' )
                .append( '<' )
                .append( tagname );
            if ( attlist != null ) {
                sbuf.append( " " + attlist );
            }
            sbuf.append( '>' );
            String value = values[ icol ] == null ? null
                                                  : escape( values[ icol ] );
            if ( value == null || value.length() == 0 ) {
                sbuf.append( "&nbsp;" );
            }
            else if ( isUrl( value ) ) {
                sbuf.append( "<A href='" )
                    .append( value )
                    .append( "'>" )
                    .append( value )
                    .append( "</A>" );
            }
            else {
                sbuf.append( value );
            }
            sbuf.append( "</" )
                .append( tagname )
                .append( ">" );
        }
        sbuf.append( "</TR>" );
        printLine( ostrm, sbuf.toString() );
    }

    /**
     * Outputs a line of text, terminated by a newline, to a stream.
     *
     * @param   ostrm  output stream
     * @param   str   string to write
     */
    private void printLine( OutputStream ostrm, String str )
            throws IOException {
        ostrm.write( str.getBytes() );
        ostrm.write( (int) '\n' );
    }

    /**
     * For standalone output, this method is invoked to output any text
     * preceding the &lt;TABLE&gt; start tag.  May be overridden to 
     * modify the form of output documents.
     *
     * @param  ostrm  output stream
     * @param  table  table for which header is required; may be null
     *         for multi-table output
     */
    protected void printHeader( OutputStream ostrm, StarTable table ) 
            throws IOException {
        String publicId = useRowGroups_
                        ? "-//W3C//DTD HTML 4.01 Transitional//EN"
                        : "-//W3C//DTD HTML 3.2 Final//EN";
        String declaration = "<!DOCTYPE HTML PUBLIC \"" + publicId + "\">";
        printLine( ostrm, declaration );
        printLine( ostrm, "<HTML>" );
        String tname = table == null ? null
                                     : table.getName();
        if ( tname != null && tname.trim().length() > 0 ) {
            printLine( ostrm, "<HEAD><TITLE>Table " +
                              escape( tname ) +
                              "</TITLE></HEAD>" );
        }
        printLine( ostrm, "<BODY>" );
    }

    /**
     * For standalone output, this method is invoked to output any text
     * following the &lt;/TABLE&gt; end tag.  May be overridden to
     * modify the form of output documents.
     *
     * @param  ostrm  output stream
     */
    protected void printFooter( OutputStream ostrm ) throws IOException {
        printLine( ostrm, "</BODY>" );
        printLine( ostrm, "</HTML>" );
    }

    /**
     * Turns a string into a one suitable for inclusion in HTML text -
     * any special characters are escaped in an HTML-friendly fashion.
     *
     * @param   line  string to escape
     * @return   an HTML-friendly version of <code>line</code>
     */
    private String escape( String line ) {
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < line.length(); i++ ) {
            char chr = line.charAt( i );
            switch ( chr ) {
                case '&':
                    sbuf.append( "&amp;" );
                    break;
                case '<':
                    sbuf.append( "&lt;" );
                    break;
                case '>':
                    sbuf.append( "&gt;" );
                    break;
                case '"':
                    sbuf.append( "&quot;" );
                    break;
                case '\'':
                    sbuf.append( "&apos;" );
                    break;
                default:
                    sbuf.append( ( chr > 0 && chr < 254 ) ? chr : '?' );
            }
        }
        return sbuf.toString();
    }

    /**
     * Determines whether a string is apparently a URL.
     * If this returns true, it is appropriate to format it within an HTML
     * "a" element.
     *
     * @param  txt   string to test
     * @return  true  iff txt looks like a URL
     */
    protected boolean isUrl( String txt ) {
        if ( txt.startsWith( "http:" ) ||
             txt.startsWith( "ftp:" ) ||
             txt.startsWith( "mailto:" ) ) {
            try {
                URLUtils.newURL( txt );
                return true;
            }
            catch ( MalformedURLException e ) {
                return false;
            }
        }
        else {
            return false;
        }
    }
}
