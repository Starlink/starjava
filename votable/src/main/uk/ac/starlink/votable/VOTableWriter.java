package uk.ac.starlink.votable;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Logger;
import uk.ac.starlink.fits.FitsTableWriter;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.ValueInfo;

/**
 * Implementation of the <tt>StarTableWriter</tt> interface for
 * VOTables.  The <tt>dataFormat</tt> and <tt>inline</tt> attributes
 * can be modified to affect how the bulk cell data are output -
 * this may be in TABLEDATA, FITS or BINARY format, and in the 
 * latter two cases may be either inline as base64 encoded CDATA or
 * to a separate stream.
 *
 * @author   Mark Taylor (Starlink)
 */
public class VOTableWriter implements StarTableWriter {

    private DataFormat dataFormat = DataFormat.TABLEDATA;
    private boolean inline = true;
    private String xmlDeclaration = DEFAULT_XML_DECLARATION;
    private String doctypeDeclaration = DEFAULT_DOCTYPE_DECLARATION;

    /** Default XML declaration in written documents. */
    public static final String DEFAULT_XML_DECLARATION =
        "<?xml version='1.0'?>";

    /** Default document type declaration in written documents. */
    public static final String DEFAULT_DOCTYPE_DECLARATION =
        "<!DOCTYPE VOTABLE SYSTEM 'http://us-vo.org/xml/VOTable.dtd'>";

    private static final Logger logger =
        Logger.getLogger( "uk.ac.starlink.votable" );

    /**
     * Constructs a default VOTableWriter.
     * Output is in TABLEDATA format.
     */
    public VOTableWriter() {
        this( DataFormat.TABLEDATA, true );
    }

    /**
     * Constructs a VOTableWriter with specified output characteristics.
     *
     * @param   dataFormat   the format in which tables will be written
     * @param   inline       whether output of streamed formats should be
     *                       inline and base64-encoded or not
     */
    public VOTableWriter( DataFormat dataFormat, boolean inline ) {
        this.dataFormat = dataFormat;
        this.inline = inline;
    }

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
        OutputStream out;
        File file;
        if ( location.equals( "-" ) ) {
            file = null;
            out = System.out;
        }
        else {
            file = new File( location );
            out = new BufferedOutputStream( new FileOutputStream( file ) );
        }
        writeStarTable( startab, out, file );
    }

    /**
     * Writes a StarTable to a given stream.
     * <p>
     * Currently, an entire XML VOTable document is written,
     * and the TABLEDATA format (all table cells written inline
     * as separate XML elements) is used.
     *
     * @param   startab  the table to write
     * @param   out  the stream down which to write the table
     * @param   file  the filename to which <tt>out</tt> refers; this is used
     *          if necessary to come up with a suitable filename for
     *          related files which need to be written.  May be <tt>null</tt>.
     */
    public void writeStarTable( StarTable startab, OutputStream out,
                                File file ) 
            throws IOException {

        /* For most of the output we write to a Writer; it is obtained
         * here and uses the default encoding.  If we write bulk data
         * into the XML (using Base64 encoding) we write that direct to
         * the underlying output stream, taking care to flush before and
         * after.  This relies on the fact that the characters output
         * from the base64 encoding have 1-byte representations in the
         * XML encoding we are using which are identical to their base64
         * byte equivalents.  So in the line below which constructs a
         * Writer from an OutputStream, don't do it using e.g. UTF-16 
         * encoding for which that wouldn't hold.
         *
         * Although we frequently want to write a string followed by a 
         * new line, we don't use a PrintWriter here, since that doesn't
         * throw any exceptions; we would like exceptions to be thrown
         * where they occur. */
        BufferedWriter writer = 
            new BufferedWriter( new OutputStreamWriter( out ) );

        /* Output preamble. */
        writer.write( xmlDeclaration );
        writer.newLine();
        writer.write( doctypeDeclaration );
        writer.newLine();
        writer.write( "<VOTABLE version='1.0'>" );
        writer.newLine();
        writer.write( "<!--" );
        writer.newLine();
        writer.write( " !  VOTable written by " + 
                      formatText( this.getClass().getName() ) );
        writer.newLine();
        writer.write( " !-->" );
        writer.newLine();
        writer.write( "<RESOURCE>" );
        writer.newLine();

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
                if ( encoder == null ) {
                    logger.warning( "Can't output parameter " + pinfo.getName()
                                  + " of type " 
                                  + pinfo.getContentClass().getName() );
                }
                else {
                    String valtext = encoder.encodeAsText( param.getValue() );
                    String content = encoder.getFieldContent();
                
                    writer.write( "<PARAM" );
                    writer.write( formatAttributes( encoder
                                                   .getFieldAttributes() ) );
                    writer.write( formatAttribute( "value", valtext ) );
                    if ( content.length() > 0 ) {
                        writer.write( ">" );
                        writer.newLine();
                        writer.write( content );
                        writer.newLine();
                        writer.write( "</PARAM>" );
                    }
                    else {
                        writer.write( "/>" );
                    }
                    writer.newLine();
                }
            }
        }

        /* Start the TABLE element itself. */
        writer.write( "<TABLE" );
        String tname = startab.getName();
        if ( tname != null && tname.trim().length() > 0 ) {
            writer.write( formatAttribute( "name", tname.trim() ) );
        }
        writer.write( ">" );
        writer.newLine();

        /* Output a DESCRIPTION element if we have something suitable. */
        if ( description != null && description.trim().length() > 0 ) {
            writer.write( "<DESCRIPTION>" );
            writer.newLine();
            writer.write( formatText( description.trim() ) );
            writer.newLine();
            writer.write( "</DESCRIPTION>" );
            writer.newLine();
        }

        /* Get the format to provide a configuration object which describes
         * exactly how the data from each cell is going to get written. */
        VOSerializer serializer = 
            VOSerializer.makeSerializer( dataFormat, startab );

        /* Output FIELD headers as determined by this object. */
        serializer.writeFields( writer );

        /* Now write the DATA element. */
        /* First Treat the case where we write data inline. */
        if ( inline || file == null ) {

            /* For elements which stream data to a Base64 encoding we
             * write the element by hand using some package-private methods.
             * This is just an efficiency measure - it means the 
             * writing is done directly to the base OutputStream rather 
             * than wrapping a new OutputStream round the Writer which 
             * is wrapped round the base OutputStream.
             * But we could omit this stanza altogether and let the 
             * work get done by serializer.writeInlineDataElement.
             * I don't know whether the efficiency hit is significant or not. */
            if ( serializer instanceof VOSerializer.StreamableVOSerializer ) {
                VOSerializer.StreamableVOSerializer streamer =
                    (VOSerializer.StreamableVOSerializer) serializer;
                String tagname;
                if ( dataFormat == DataFormat.FITS ) {
                    tagname = "FITS";
                }
                else if ( dataFormat == DataFormat.BINARY ) {
                    tagname = "BINARY";
                }
                else {
                    throw new AssertionError( "Unknown format " 
                                            + dataFormat.toString() );
                }
                writer.write( "<DATA>" );
                writer.newLine();
                writer.write( '<' + tagname + '>' );
                writer.newLine();
                writer.write( "<STREAM encoding='base64'>" );
                writer.newLine();
                writer.flush();
                Base64OutputStream b64strm = 
                    new Base64OutputStream( new BufferedOutputStream( out ),
                                            16 );
                DataOutputStream dataout = new DataOutputStream( b64strm );
                streamer.streamData( dataout );
                dataout.flush();
                b64strm.endBase64();
                b64strm.flush();
                writer.write( "</STREAM>" );
                writer.newLine();
                writer.write( "</" + tagname + ">" );
                writer.newLine();
                writer.write( "</DATA>" );
                writer.newLine();
            }

            /* Non-optimized/non-STREAM case. */
            else {
                serializer.writeInlineDataElement( writer );
            }
        }

        /* Treat the case where the data is streamed to an external file. */
        else {
            assert file != null;
            String basename = file.getName();
            int dotpos = basename.lastIndexOf( '.' );
            basename = dotpos > 0 ? basename.substring( 0, dotpos )
                                  : basename;
            String extension = dataFormat == DataFormat.FITS ? ".fits" : ".bin";
            String dataname = basename + "-data" + extension;
            File datafile = new File( file.getParentFile(), dataname );
            DataOutputStream dataout =
                new DataOutputStream( 
                    new BufferedOutputStream( 
                        new FileOutputStream( datafile ) ) );
            serializer.writeHrefDataElement( writer, dataname, dataout );
            dataout.close();
        }

        /* Close the open elements and tidy up. */
        writer.write( "</TABLE>" );
        writer.newLine();
        writer.write( "</RESOURCE>" );
        writer.newLine();
        writer.write( "</VOTABLE>" );
        writer.newLine();
        writer.flush();
    }

    /**
     * Returns true for filenames with the extension ".xml", ".vot" or
     * ".votable";
     *
     * @param  name of the file 
     * @return  true if <tt>filename</tt> looks like the home of a VOTable
     */
    public boolean looksLikeFile( String filename ) {
        return filename.endsWith( ".xml" )
            || filename.endsWith( ".vot" )
            || filename.endsWith( ".votable" );
    }

    public String getFormatName() {
        StringBuffer fname = new StringBuffer( "votable" );
        if ( dataFormat == DataFormat.TABLEDATA ) {
            fname.append( "-tabledata" );
            return fname.toString();
        }

        if ( dataFormat == DataFormat.FITS ) {
            fname.append( "-fits" );
        }
        else if ( dataFormat == DataFormat.BINARY ) {
            fname.append( "-binary" );
        }
        else {
            assert false;
        }
        fname.append( inline ? "-inline" : "-href" );
        return fname.toString();
    }

    /**
     * Sets the format in which the table data will be output.
     *
     * @param  bulk data format
     */
    public void setDataFormat( DataFormat format ) {
        this.dataFormat = format;
    }

    /**
     * Returns the format in which this writer will output the bulk table data.
     *
     * @return  bulk data format
     */
    public DataFormat getDataFormat() {
        return dataFormat;
    }

    /**
     * Sets whether STREAM elements should be written inline or to an
     * external file in the case of FITS and BINARY encoding.
     *
     * @param  inline  <tt>true</tt> iff streamed data will be encoded 
     *         inline in the STREAM element
     */
    public void setInline( boolean inline ) {
        this.inline = inline;
    }

    /**
     * Indicates whether STREAM elements will be written inline or to 
     * an external file in the case of FITS and BINARY encoding.
     *
     * @return  <tt>true</tt> iff streamed data will be encoded inline in
     *          the STREAM element
     */
    public boolean getInline() {
        return inline;
    }

    /**
     * Sets the XML declaration which will be used by this writer
     * at the head of any document written.
     * By default this is the value of {@link #DEFAULT_XML_DECLARATION}.
     *
     * @param  new XML declaration
     */
    public void setXMLDeclaration( String xmlDecl ) {
        this.xmlDeclaration = xmlDecl;
    }

    /**
     * Returns the XML declaration which is used by this writer
     * at the head of any document written.
     */
    public String getXMLDeclaration() {
        return xmlDeclaration;
    }

    /**
     * Sets the document type declaration which will be used by this writer
     * at the head of any document written.  By default this is
     * the value of {@link #DEFAULT_DOCTYPE_DECLARATION}.
     *
     * @param  doctypeDecl  new declaration
     */
    public void setDoctypeDeclaration( String doctypeDecl ) {
        this.doctypeDeclaration = doctypeDecl;
    }

    /**
     * Returns the document type declaration which is used by this writer
     * at the head of any document written.
     *
     * @return  doctypeDecl
     */
    public String getDoctypeDeclaration() {
        return doctypeDeclaration;
    }

    /**
     * Returns a list of votables with variant values of attributes.
     * Currently it returns a list of all the ones you get by using 
     * non-default values for the constructor parameters, that is
     * BINARY and FITS each in inline and href variants (a list of 4).
     *
     * @return   non-standard VOTableWriters.
     */
    public static List getVariantHandlers() {
        List variants = new ArrayList();
        variants.add( new VOTableWriter( DataFormat.BINARY, true ) );
        variants.add( new VOTableWriter( DataFormat.BINARY, false ) );
        variants.add( new VOTableWriter( DataFormat.FITS, true ) );
        variants.add( new VOTableWriter( DataFormat.FITS, false ) );
        return variants;
    }

    /**
     * Writes a FIELD element to a writer.
     *
     * @param  content  text content of the element, if any
     * @param  attributes   a name-value map of attributes
     * @param  writer    destination stream
     */ 
    static void writeFieldElement( BufferedWriter writer, String content,
                                   Map attributes ) throws IOException {
        writer.write( "<FIELD" + formatAttributes( attributes ) );
        if ( content != null && content.length() > 0 ) {
            writer.write( '>' );
            writer.newLine();
            writer.write( content );
            writer.newLine();
            writer.write( "</FIELD>" );
        }
        else {
            writer.write( "/>" );
        }
        writer.newLine();
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
    static String formatAttributes( Map atts ) {
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
    static String formatAttribute( String name, String value ) {
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

    /**
     * Performs necessary special character escaping for text which 
     * will be written as XML CDATA.
     * 
     * @param   text  the input text
     * @return  <tt>text</tt> but with XML special characters escaped
     */
    static String formatText( String text ) {
        int leng = text.length();
        StringBuffer sbuf = new StringBuffer( leng );
        for ( int i = 0; i < leng; i++ ) {
            char c = text.charAt( i );
            switch ( c ) {
                case '<':
                    sbuf.append( "&lt;" );
                    break;
                case '>':
                    sbuf.append( "&gt;" );
                    break;
                case '&':
                    sbuf.append( "&amp;" );
                    break;
                default:
                    sbuf.append( c );
            }
        }
        return sbuf.toString();
    }
}
