package uk.ac.starlink.votable;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.ValueInfo;

/**
 * Implementation of the <tt>StarTableWriter</tt> interface for
 * VOTables.
 *
 * @author   Mark Taylor (Starlink)
 */
public class VOTableWriter implements StarTableWriter {

    /**
     * Writes a StarTable to a given location.
     * The <tt>location</tt> is interpreted as a filename, unless it
     * is the string "<tt>-</tt>", in which case it is taken to 
     * mean <tt>System.out</tt>.
     * <p>
     * Currently, an entire XML VOTable document is written,
     * and the TABLEDATA format (all table cells written inline
     * as separate XML elements) is used.
     *
     * @param   startab  the table to write
     * @param   location  the filename to which to write the table
     */
    public void writeStarTable( StarTable startab, String location )
            throws IOException { 

        /* Get the stream to write to. */
        PrintStream out;
        if ( location.equals( "-" ) ) {
            out = System.out;
        }
        else {
            out = new PrintStream( 
                      new BufferedOutputStream( 
                          new FileOutputStream( new File( location ) ) ) );
        }

        /* Output preamble. */
        out.println( "<?xml version='1.0'?>" );
        out.println( "<!DOCTYPE VOTABLE SYSTEM "
                  +  "'http://us-vo.org/xml/VOTable.dtd'>" );
        out.println( "<VOTABLE version='1.0'>" );
        out.println( "<!--" );
        out.println( " !  VOTable written by " + this );
        out.println( " !  outputting table " + startab );
        out.println( " !-->" );
        out.println( "<RESOURCE>" );

        /* Output table parameters as PARAM elements. */
        String description = null;
        for ( Iterator it = startab.getParameters().iterator();
              it.hasNext(); ) {
            DescribedValue param = (DescribedValue) it.next();
            ValueInfo pinfo = param.getInfo();

            /* If it's called 'description', treat it specially. */
            if ( pinfo.getName().equalsIgnoreCase( "description" ) ) {
                description = pinfo.formatValue( param.getValue(), 20480 );
            }

            /* Otherwise, output the characteristics. */
            else {
                Encoder encoder = Encoder.getEncoder( pinfo );
                String valtext = encoder.encodeAsText( param.getValue() );
                String content = encoder.getFieldContent();
                
                out.print( "<PARAM" );
                out.print( formatAttributes( encoder.getFieldAttributes() ) );
                out.print( formatAttribute( "value", valtext ) );
                if ( content.length() > 0 ) {
                    out.println( ">" );
                    out.println( content );
                    out.println( "</PARAM>" );
                }
                else {
                    out.println( "/>" );
                }
            }
        }

        /* Start the TABLE element itself. */
        out.println( "<TABLE>" );

        /* Output a DESCRIPTION element if we have something suitable. */
        if ( description != null && description.trim().length() > 0 ) {
            out.println( "<DESCRIPTION>" );
            out.println( description.trim() );
            out.println( "</DESCRIPTION>" );
        }

        /* Output table columns as FIELD elements. */
        int ncol = startab.getColumnCount();
        Encoder[] encoders = new Encoder[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            ValueInfo cinfo = startab.getColumnInfo( icol );
            Encoder encoder = Encoder.getEncoder( cinfo );
            encoders[ icol ] = encoder;
            String content = encoder.getFieldContent();
            out.print( "<FIELD" );
            out.print( formatAttributes( encoder.getFieldAttributes() ) );
            if ( content.length() > 0 ) {
                out.println( ">" );
                out.println( content );
                out.println( "</FIELD>" );
            }
            else {
                out.println( "/>" );
            }
        }

        /* Start the DATA element. */
        out.println( "<DATA>" );

        /* Output the actual cell values within a TABLEDATA element. */
        out.println( "<TABLEDATA>" );
        for ( RowSequence rseq = startab.getRowSequence(); rseq.hasNext(); ) {
            rseq.next();
            out.println( "  <TR>" );
            Object[] rowdata = rseq.getRow();
            for ( int icol = 0; icol < ncol; icol++ ) {
                out.print( "    <TD>" );
                out.print( encoders[ icol ].encodeAsText( rowdata[ icol ] ) );
                out.println( "</TD>" );
            }
            out.println( "  </TR>" );
        }
        out.println( "</TABLEDATA>" );

        /* Close open elements. */
        out.println( "</DATA>" );
        out.println( "</TABLE>" );
        out.println( "</RESOURCE>" );
        out.println( "</VOTABLE>" );
        out.flush();
    }

    /**
     * Returns true for filenames with the extension ".xml" or ".votable";
     *
     * @param  name of the file 
     * @return  true if <tt>filename</tt> looks like the home of a VOTable
     */
    public boolean looksLikeFile( String filename ) {
        return filename.endsWith( ".xml" )
            || filename.endsWith( ".votable" );
    }

    /**
     * Returns the string "votable";
     *
     * @return  name of the format written by this writer
     */
    public String getFormatName() {
        return "votable";
    }

    /**
     * Turns a Map of name,value pairs into a string of attribute 
     * assignments suitable for putting in an XML start tag.
     * The resulting string starts with, but does not end with, whitespace.
     * Any necessary escaping of the strings is taken care of.
     *
     * @param  atts  Map of name,value pairs
     * @return  a string of name="value" assignments
     */
    private static String formatAttributes( Map atts ) {
        StringBuffer sbuf = new StringBuffer();
        for ( Iterator it = new TreeSet( atts.keySet() ).iterator();
              it.hasNext(); ) {
            String attname = (String) it.next();
            String attval = (String) atts.get( attname );
            sbuf.append( formatAttribute( attname, attval ) );
        }
        return sbuf.toString();
    }

    /**
     * Turns a name,value pair into an attribute assignment suitable for
     * putting in an XML start tag.
     * The resulting string starts with, but does not end with, whitespace.
     * Any necessary escaping of the strings is taken care of.
     *
     * @param  name  the attribute name
     * @param  value  the attribute value
     * @return  string of the form ' name="value"'
     */
    private static String formatAttribute( String name, String value ) {
        return new StringBuffer()
            .append( ' ' )
            .append( name )
            .append( '=' )
            .append( '"' )
            .append( value.replaceAll( "&", "&amp;" )
                          .replaceAll( "\"", "&quot;" ) )
            .append( '"' )
            .toString();
    }
}
