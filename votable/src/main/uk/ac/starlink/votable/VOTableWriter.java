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
import java.util.Iterator;
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

    private Format dataFormat = Format.TABLEDATA;
    private boolean inline = true;

    private static final Logger logger =
        Logger.getLogger( "uk.ac.starlink.votable" );

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
         * byte equivalents.  So don't use UTF-16. 
         * Although we frequently want to write a string followed by a 
         * new line, we don't use a PrintWriter here, since that doesn't
         * throw any exceptions; we would like exceptions to be thrown
         * where they occur. */
        BufferedWriter writer = 
            new BufferedWriter( new OutputStreamWriter( out ) );

        /* Output preamble. */
        writer.write( "<?xml version='1.0'?>" );
        writer.newLine();
        writer.write( "<!DOCTYPE VOTABLE SYSTEM "
                    + "'http://us-vo.org/xml/VOTable.dtd'>" );
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
        VOSerializer serializer = dataFormat.getSerializer( startab );
        String tagname = dataFormat.getTagName();

        /* Output FIELD headers as determined by this object. */
        serializer.writeFields( writer );

        /* Start the DATA element. */
        writer.write( "<DATA>" );
        writer.newLine();
        writer.write( "<" + tagname + ">" );
        writer.newLine();

        /* Treat elements which write to a STREAM element. */
        if ( serializer instanceof VOStreamable ) {
            VOStreamable streamer = (VOStreamable) serializer;

            /* Case where the STREAM is to go to an external file. */
            if ( ! inline && file != null ) {
                String basename = file.getName();
                int dotpos = basename.lastIndexOf( '.' );
                basename = dotpos > 0 ? basename.substring( 0, dotpos )
                                      : basename;
                String dataname = basename + "-data" + 
                                  dataFormat.getExtension();
                writer.write( "<STREAM" + 
                              formatAttribute( "href", dataname ) + "/>" );
                writer.newLine();
                File datfile = new File( file.getParentFile(), dataname );
                OutputStream strm =
                    new BufferedOutputStream( new FileOutputStream( datfile ) );
                streamer.streamData( strm );
                strm.close();
            }

            /* Case where the STREAM is written inline base64-encoded. */
            else {
                writer.write( "<STREAM" + 
                              formatAttribute( "encoding", "base64" ) + ">" );
                writer.newLine();
                writer.flush();
                Base64OutputStream b64strm = new Base64OutputStream( out, 16 );
                streamer.streamData( b64strm );
                b64strm.endBase64();
                b64strm.flush();
                writer.write( "</STREAM>" );
                writer.newLine();
            }
        }

        /* If it's not going to a STREAM is has to be TABLEDATA. */
        else {
            assert serializer instanceof TabledataVOSerializer;
            assert dataFormat == Format.TABLEDATA;
            ((TabledataVOSerializer) serializer).writeData( writer );
        }

        /* Close the open elements and tidy up. */
        writer.write( "</" + tagname + ">" );
        writer.newLine();
        writer.write( "</DATA>" );
        writer.newLine();
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

    /**
     * Returns the string "votable";
     *
     * @return  name of the format written by this writer
     */
    public String getFormatName() {
        return "votable";
    }

    /**
     * Sets the format in which the table data will be output.
     *
     * @param  bulk data format
     */
    public void setDataFormat( Format format ) {
        this.dataFormat = format;
    }

    /**
     * Returns the format in which this writer will output the bulk table data.
     *
     * @return  bulk data format
     */
    public Format getDataFormat() {
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

    /**
     * Class of objects representing the different serialization formats
     * into which VOTable cell data can be written.  Each of the 
     * available formats is
     * represented by a static final member of this class.
     * Members of this class know how to supply a {@link VOSerializer}
     * object for a given StarTable; it is the VOSerializer which
     * does the hard work of writing the bulk table data.
     */
    public static abstract class Format {
        private final String name;
        private final String extension;
        private Format( String name, String extension ) {
            this.name = name;
            this.extension = extension;
        }
        public String getTagName() {
            return name;
        }
        public String getExtension() {
            return extension;
        }
        public String toString() {
            return name;
        }

        /**
         * Returns a serializer object which can do the hard work 
         * of serializing a StarTable object to a VOTable TABLE 
         * element in the format represented by this object.
         *
         * @param   table  the table which must be serialized
         * @return  a serializer object which can serialize <tt>table</tt>
         *          in the way defined for this data format
         */
        public abstract VOSerializer getSerializer( StarTable table )
               throws IOException;

        /** TABLEDATA format (pure XML). */
        public static final Format TABLEDATA = 
            new Format( "TABLEDATA", null ) {
                public VOSerializer getSerializer( StarTable table ) {
                    return new TabledataVOSerializer( table );
                }
            };

        /** FITS format. */
        public static final Format FITS = 
            new Format( "FITS", ".fits" ) {
                public VOSerializer getSerializer( StarTable table )
                        throws IOException{
                    return new FITSVOSerializer( table );
                }
            };

        /** Raw binary format. */
        public static final Format BINARY =
            new Format( "BINARY", ".bin" ) {
                public VOSerializer getSerializer( StarTable table ) {
                    return new BinaryVOSerializer( table );
                }
            };
    }
}
