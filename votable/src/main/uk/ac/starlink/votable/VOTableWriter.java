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
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.fits.AbstractFitsTableWriter;
import uk.ac.starlink.fits.FitsTableWriter;
import uk.ac.starlink.table.MultiStarTableWriter;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.Base64OutputStream;
import uk.ac.starlink.util.IOUtils;

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
public class VOTableWriter implements StarTableWriter, MultiStarTableWriter {

    private DataFormat dataFormat = DataFormat.TABLEDATA;
    private boolean inline = true;
    private String xmlDeclaration = DEFAULT_XML_DECLARATION;
    private String doctypeDeclaration = DEFAULT_DOCTYPE_DECLARATION;
    private String votableVersion = DEFAULT_VOTABLE_VERSION;

    /** Default XML declaration in written documents. */
    public static final String DEFAULT_XML_DECLARATION =
        "<?xml version='1.0'?>";

    /** Default document type declaration in written documents. */
    public static final String DEFAULT_DOCTYPE_DECLARATION = "";

    /** Default VOTABLE version number. */
    public static final String DEFAULT_VOTABLE_VERSION = "1.1";

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
     *
     * @param   startab  the table to write
     * @param   location  the filename to which to write the table
     * @param   sto   object used for location resolution
     */
    public void writeStarTable( StarTable startab, String location,
                                StarTableOutput sto )
            throws IOException { 
        writeStarTables( Tables.singleTableSequence( startab ), location, sto );
    }

    /**
     * Writes a sequence of tables to a given location.
     * They are written as separate TABLE elements in the same VOTable document.
     *
     * @param  tableSeq  table sequence
     * @param   location  the filename to which to write the table
     * @param   sto   object used for location resolution
     */
    public void writeStarTables( TableSequence tableSeq, String location,
                                 StarTableOutput sto )
            throws IOException {

        /* Get the stream to write to. */
        OutputStream out = null; 
        try {
            out = sto.getOutputStream( location );
            File file = out instanceof FileOutputStream
                      ? new File( location )
                      : null;
            if ( ! inline && file == null ) {
                throw new TableFormatException( "Can't write non-inline format"
                                              + " to a stream" );
            }
            writeStarTables( tableSeq, out, file );
        }
        finally {
            if ( out != null ) {
                out.close();
            }
        }
    }

    /**
     * Writes a StarTable to a given stream; must be inline.
     * Same as <code>writeStarTable(startab,out,(File)null)</code>.
     *
     * @param   startab  the table to write
     * @param   out  the stream down which to write the table
     */
    public void writeStarTable( StarTable startab, OutputStream out ) 
            throws IOException {
        writeStarTable( startab, out, null );
    }

    /**
     * Writes a sequence of tables to a given stream; must be inline.
     * Same as <code>writeStarTables(tableSeq,out,null)</code>.
     *
     * @param   tableSeq  tables to write
     * @param   out  destination stream
     */
    public void writeStarTables( TableSequence tableSeq, OutputStream out )
            throws IOException {
        writeStarTables( tableSeq, out, null );
    }

    /**
     * Writes a StarTable to a given stream.
     *
     * @param   startab  the table to write
     * @param   out  the stream down which to write the table
     * @param   file  the filename to which <tt>out</tt> refers; this is used
     *          if necessary to come up with a suitable filename for
     *          related files which need to be written.  May be <tt>null</tt>.
     */
    public void writeStarTable( StarTable startab, OutputStream out, File file )
            throws IOException {
        writeStarTables( Tables.singleTableSequence( startab ), out, file );
    }

    /**
     * Writes a sequence of tables to a given stream.
     *
     * @param  tableSeq  table sequence to write
     * @param  out  destination stream
     * @param   file  the filename to which <tt>out</tt> refers; this is used
     *          if necessary to come up with a suitable filename for
     *          related files which need to be written.  May be <tt>null</tt>.
     */
    public void writeStarTables( TableSequence tableSeq, OutputStream out,
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
        OutputStreamWriter osw;
        try {
            osw = new OutputStreamWriter( out, "UTF-8" );
        }
        catch ( UnsupportedEncodingException e ) {
            logger.log( Level.WARNING, "What? UTF-8 unsupported?", e );
            assert false;  // Java requires UTF-8 support
            osw = new OutputStreamWriter( out );
        }
        BufferedWriter writer = new BufferedWriter( osw );

        /* Output preamble. */
        writePreTableXML( writer );

        /* Loop over all tables for output. */
        int itable = 0;
        for ( StarTable startab; ( startab = tableSeq.nextTable() ) != null;
              itable++ ) {

            /* Get the format to provide a configuration object which describes
             * exactly how the data from each cell is going to get written. */
            VOSerializer serializer = 
                VOSerializer.makeSerializer( dataFormat, startab );

            /* Begin TABLE element including FIELDs etc. */
            serializer.writePreDataXML( writer );

            /* Now write the DATA element. */
            /* First Treat the case where we write data inline. */
            if ( inline || file == null ) {
                if ( ! inline ) {
                    assert file == null;
                    logger.warning( "Writing VOTable inline - can't do href "
                                  + "when no filename is supplied" );
                }

                /* For elements which stream data to a Base64 encoding we
                 * write the element by hand using some package-private methods.
                 * This is just an efficiency measure - it means the 
                 * writing is done directly to the base OutputStream rather 
                 * than wrapping a new OutputStream round the Writer which 
                 * is wrapped round the base OutputStream.
                 * But we could omit this stanza altogether and let the 
                 * work get done by serializer.writeInlineDataElement.
                 * I don't know whether the efficiency hit is significant
                 * or not. */
                if ( serializer instanceof
                     VOSerializer.StreamableVOSerializer ) {
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
                String extension = dataFormat == DataFormat.FITS
                                 ? ".fits"
                                 : ".bin";
                String dataname =
                    basename + "-data"
                             + ( itable > 0 ? Integer.toString( itable ) : "" )
                             + extension;
                File datafile = new File( file.getParentFile(), dataname );
                logger.info( "Writing VOTable href data at " + datafile );
                DataOutputStream dataout =
                    new DataOutputStream( 
                        new BufferedOutputStream( 
                            new FileOutputStream( datafile ) ) );
                serializer.writeHrefDataElement( writer, dataname, dataout );
                dataout.close();
            }

            /* Write postamble. */
            serializer.writePostDataXML( writer );
        }
        writePostTableXML( writer );

        /* Tidy up. */
        writer.flush();
    }

    /**
     * Writes a table directly to a stream.
     *
     * @param  startab   table to write
     * @param  writer    destination stream
     */
    public void writeInlineStarTable( StarTable startab, BufferedWriter writer )
            throws IOException {
        writeInlineStarTables( new StarTable[] { startab }, writer );
    }

    /**
     * Writes multiple tables directly to a stream.
     *
     * @param  startabs  tables
     * @param  writer    destination stream
     */
    public void writeInlineStarTables( StarTable[] startabs,
                                       BufferedWriter writer )
            throws IOException {
        writePreTableXML( writer );
        for ( int i = 0; i < startabs.length; i++ ) {
            VOSerializer.makeSerializer( dataFormat, startabs[ i ] )
                        .writeInlineTableElement( writer );
        }
        writePostTableXML( writer );
        writer.flush();
    }

    /**
     * Outputs all the text required before the TABLE element.
     * This method can be overridden to alter the behaviour of the
     * writer if required.
     *
     * @param  writer       destination stream
     * @see    #writePostTableXML
     */
    protected void writePreTableXML( BufferedWriter writer )
            throws IOException {

        /* Output XML declaration if required. */
        if ( xmlDeclaration != null && xmlDeclaration.length() > 0 ) {
            writer.write( xmlDeclaration );
            writer.newLine();
        }

        /* Output document declaration if required. */
        if ( doctypeDeclaration != null && doctypeDeclaration.length() > 0 ) {
            writer.write( doctypeDeclaration );
            writer.newLine();
        }

        /* Output the VOTABLE start tag. */
        writer.write( "<VOTABLE" );
        if ( votableVersion != null &&
             votableVersion.matches( "1.[1-9]" ) ) {
            writer.write( VOSerializer.formatAttribute( "version",
                                                        votableVersion ) );
            if ( doctypeDeclaration == null ||
                 doctypeDeclaration.length() == 0 ) {
                String votableNamespace = "http://www.ivoa.net/xml/VOTable/v"
                                        + votableVersion;
                String votableSchemaLocation = votableNamespace;
                writer.newLine();
                writer.write( VOSerializer.formatAttribute(
                                  "xmlns:xsi",
                                  "http://www.w3.org/2001/"
                                  + "XMLSchema-instance" ) );
                writer.newLine();
                writer.write( VOSerializer.formatAttribute(
                                  "xsi:schemaLocation",
                                  votableNamespace + " " +
                                  votableSchemaLocation ) );
                writer.newLine();
                writer.write( VOSerializer.formatAttribute(
                                  "xmlns",
                                  votableNamespace ) );
            }
        }
        writer.write( ">" );
        writer.newLine();

        /* Output a comment claiming authorship. */
        writer.write( "<!--" );
        writer.newLine();
        writer.write( " !  VOTable written by STIL version "
                    + IOUtils.getResourceContents( StarTable.class,
                                                   "stil.version" )
                    + " (" + VOSerializer.formatText( getClass().getName() )
                    + ")" );
        writer.newLine();
        writer.write( " !  at " + AbstractFitsTableWriter.getCurrentDate() );
        writer.newLine();
        writer.write( " !-->" );
        writer.newLine();

        /* Output RESOURCE element start tag. */
        writer.write( "<RESOURCE>" );
        writer.newLine();
    }

    /**
     * Outputs all the text required after the TABLE element in the
     * output table document.  This method can be overridden to alter
     * the behaviour of this writer if required.
     *
     * @param  writer       destination stream
     * @see    #writePreTableXML
     */
    protected void writePostTableXML( BufferedWriter writer )
            throws IOException {

        /* Close the open elements. */
        writer.write( "</RESOURCE>" );
        writer.newLine();
        writer.write( "</VOTABLE>" );
        writer.newLine();
    }

    /**
     * Returns true for filenames with the extension ".xml", ".vot" or
     * ".votable";
     *
     * @param  filename  name of the file 
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

    public String getMimeType() {
        String type = "application/x-votable+xml";
        String encoding;
        if ( dataFormat == DataFormat.TABLEDATA ) {
            encoding = "TABLEDATA";
        }
        else if ( dataFormat == DataFormat.BINARY ) {
            encoding = "BINARY";
        }
        else if ( dataFormat == DataFormat.FITS ) {
            encoding = "FITS";
        }
        else {
            encoding = null;
        }
        return "application/x-votable+xml"
             + ( encoding == null ? ""
                                  : "; encoding=\"" + encoding + "\"" ); 
    }

    /**
     * Sets the format in which the table data will be output.
     *
     * @param  format  bulk data format
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
     * @param  xmlDecl  new XML declaration
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
     * Sets the version attribtion of the VOTABLE element written by
     * this writer.
     * Note this does not necessarily change the formatting behaviour
     * to match the stated version.
     *
     * @param   votableVersion  version to declare
     */
    public void setVotableVersion( String votableVersion ) {
        this.votableVersion = votableVersion;
    }

    /**
     * Returns the value of the version attribute of the VOTABLE element
     * written by this writer.
     *
     * @return  declared votable version
     */
    public String getVotableVersion() {
        return votableVersion;
    }

    /**
     * Returns a list of votable writers with variant values of attributes.
     *
     * @return   non-standard VOTableWriters.
     */
    public static StarTableWriter[] getStarTableWriters() {
        return new StarTableWriter[] {
            new VOTableWriter( DataFormat.TABLEDATA, true ),
            new VOTableWriter( DataFormat.BINARY, true ),
            new VOTableWriter( DataFormat.FITS, false ),
            new VOTableWriter( DataFormat.BINARY, false ),
            new VOTableWriter( DataFormat.FITS, true ),
        };
    }
}
