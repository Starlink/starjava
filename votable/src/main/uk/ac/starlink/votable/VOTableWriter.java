package uk.ac.starlink.votable;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.fits.AbstractFitsTableWriter;
import uk.ac.starlink.table.MultiStarTableWriter;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.formats.DocumentedIOHandler;
import uk.ac.starlink.util.Base64OutputStream;
import uk.ac.starlink.util.ConfigMethod;
import uk.ac.starlink.util.DataBufferedOutputStream;
import uk.ac.starlink.util.IOUtils;

/**
 * Implementation of the <tt>StarTableWriter</tt> interface for
 * VOTables.  The <tt>dataFormat</tt> and <tt>inline</tt> attributes
 * can be modified to affect how the bulk cell data are output -
 * this may be in TABLEDATA, FITS, BINARY or BINARY2 format, and in the 
 * latter three cases may be either inline as base64 encoded CDATA or
 * to a separate stream.
 *
 * <p>Some of the Auxiliary metadata items of the ColumnInfo metadata
 * from written tables are respected:
 * <ul>
 * <li>{@link uk.ac.starlink.table.Tables#NULL_VALUE_INFO}:
 *     sets the value of "magic" blank value for
 *     integer columns</li>
 * <li>{@link uk.ac.starlink.table.Tables#UBYTE_FLAG_INFO}:
 *     if set to <code>Boolean.TRUE</code> and if the column has content class
 *     <code>Short</code> or <code>short[]</code>, the data will be written 
 *     with <code>datatype="unsignedByte"</code> instead of
 *     (signed 16-bit) <code>"short"</code>.</li>
 * <li>The <code>COOSYS_*_INFO</code> and <code>TIMESYS_*_INFO</code>
 *     items defined in the {@link uk.ac.starlink.votable.VOStarTable} class;
 *     suitable COOSYS/TIMESYS elements will be written and referenced
 *     as required to honour these items.</li>
 * <li>Various other of the <code>*_INFO</code> items defined in the
 *     {@link uk.ac.starlink.votable.VOStarTable} class;
 *     this has the effect that VOTable column attributes read in from
 *     a VOTable will be passed through if the same table is written
 *     out to a VOTable (or VOTable-based format like FITS-plus).</li>
 * </ul>
 *
 * @author   Mark Taylor (Starlink)
 */
public class VOTableWriter
        implements StarTableWriter, MultiStarTableWriter, DocumentedIOHandler {

    private DataFormat dataFormat;
    private boolean inline;
    private VOTableVersion version;
    private boolean writeSchemaLocation;
    private String xmlDeclaration = DEFAULT_XML_DECLARATION;

    /** Default XML declaration in written documents. */
    public static final String DEFAULT_XML_DECLARATION =
        "<?xml version='1.0'?>";

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
     * Constructs a VOTableWriter with specified output type and default
     * VOTable version.
     *
     * @param   dataFormat   the format in which tables will be written
     * @param   inline       whether output of streamed formats should be
     *                       inline and base64-encoded or not
     */
    public VOTableWriter( DataFormat dataFormat, boolean inline ) {
        this( dataFormat, inline, VOTableVersion.getDefaultVersion() );
    }

    /**
     * Constructs a VOTableWriter with specified output characterstics
     * and a given version of the VOTable standard.
     *
     * @param   dataFormat   the format in which tables will be written
     * @param   inline       whether output of streamed formats should be
     *                       inline and base64-encoded or not
     * @param   version    version of the VOTable standard
     */
    public VOTableWriter( DataFormat dataFormat, boolean inline,
                          VOTableVersion version ) {
        this.dataFormat = dataFormat;
        this.inline = inline;
        this.version = version;
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
        BufferedWriter writer =
            new BufferedWriter(
                new OutputStreamWriter( out, StandardCharsets.UTF_8 ) );

        /* Output preamble. */
        writePreTableXML( writer );

        /* Loop over all tables for output. */
        int itable = 0;
        for ( StarTable startab; ( startab = tableSeq.nextTable() ) != null;
              itable++ ) {
            if ( itable > 0 ) {
                writeBetweenTableXML( writer );
            }

            /* Get the format to provide a configuration object which describes
             * exactly how the data from each cell is going to get written. */
            VOSerializer serializer = 
                VOSerializer.makeSerializer( dataFormat, version, startab );

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
                    else if ( dataFormat == DataFormat.BINARY2 ) {
                        tagname = "BINARY2";
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
                    DataBufferedOutputStream dataout =
                        new DataBufferedOutputStream( b64strm );
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
                DataBufferedOutputStream dataout =
                    new DataBufferedOutputStream(
                        new FileOutputStream( datafile ) );
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
            if ( i > 0 ) {
                writeBetweenTableXML( writer );
            }
            VOSerializer.makeSerializer( dataFormat, version, startabs[ i ] )
                        .writeInlineTableElement( writer );
        }
        writePostTableXML( writer );
        writer.flush();
    }

    /**
     * Outputs all the text required before any tables are written.
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
        String doctypeDeclaration = version.getDoctypeDeclaration();
        if ( doctypeDeclaration != null && doctypeDeclaration.length() > 0 ) {
            writer.write( doctypeDeclaration );
            writer.newLine();
        }

        /* Output the VOTABLE start tag. */
        writer.write( "<VOTABLE" );
        String versionNumber = version.getVersionNumber();
        if ( versionNumber != null ) {
            writer.write( VOSerializer.formatAttribute( "version",
                                                        versionNumber ) );
        }
        String xmlNamespace = version.getXmlNamespace();
        String schemaLocation = version.getSchemaLocation();
        if ( xmlNamespace != null ) {
            if ( writeSchemaLocation && schemaLocation != null ) {
                writer.newLine();
                writer.write( VOSerializer.formatAttribute(
                                  "xmlns:xsi",
                                  "http://www.w3.org/2001/"
                                  + "XMLSchema-instance" ) );
                writer.newLine();
                writer.write( VOSerializer.formatAttribute(
                                  "xsi:schemaLocation",
                                  xmlNamespace + " " + schemaLocation ) );
            }
            writer.newLine();
            writer.write( VOSerializer.formatAttribute( "xmlns",
                                                        xmlNamespace ) );
        }
        writer.write( ">" );
        writer.newLine();

        /* Output a comment claiming authorship. */
        writer.write( "<!--" );
        writer.newLine();
        writer.write( " !  VOTable written by STIL version "
                    + IOUtils.getResourceContents( StarTable.class,
                                                   "stil.version", null )
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
     * Outputs text between one table (TABLE and possibly other associated
     * elements) and the next.  It's only called as a separator between
     * adjacent tables, not at the start or end of a sequence of them;
     * hence it's not called if only a single table is being output.
     * This method can be overridden to alter the behaviour of the
     * writer if required.
     *
     * <p>This method closes one RESOURCE element and opens another one.
     *
     * @param  writer       destination stream
     */
    protected void writeBetweenTableXML( BufferedWriter writer )
            throws IOException {
        writer.write( "</RESOURCE>" );
        writer.newLine();
        writer.write( "<RESOURCE>" );
        writer.newLine();
    }

    /**
     * Outputs all the text required after any tables in the
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

    public String[] getExtensions() {
        return new String[] { "vot", "votable", "xml", };
    }

    public boolean looksLikeFile( String filename ) {
        return DocumentedIOHandler.matchesExtension( this, filename );
    }

    public boolean docIncludesExample() {
        return false;
    }

    public String getXmlDescription() {
        return readText( "VOTableWriter.xml" );
    }

    public String getFormatName() {
        StringBuffer fname = new StringBuffer( "votable" );
        if ( dataFormat == DataFormat.TABLEDATA ) {
            return fname.toString();
        }

        if ( dataFormat == DataFormat.FITS ) {
            fname.append( "-fits" );
        }
        else if ( dataFormat == DataFormat.BINARY ) {
            fname.append( "-binary" );
        }
        else if ( dataFormat == DataFormat.BINARY2 ) {
            fname.append( "-binary2" );
        }
        else {
            assert false;
        }
        fname.append( inline ? "-inline" : "-href" );
        return fname.toString();
    }

    public String getMimeType() {
        String type = "application/x-votable+xml";
        String serialization;
        if ( dataFormat == DataFormat.TABLEDATA ) {
            serialization = "TABLEDATA";
        }
        else if ( dataFormat == DataFormat.BINARY ) {
            serialization = "BINARY";
        }
        else if ( dataFormat == DataFormat.BINARY2 ) {
            serialization = "BINARY2";
        }
        else if ( dataFormat == DataFormat.FITS ) {
            serialization = "FITS";
        }
        else {
            serialization = null;
        }
        StringBuffer sbuf = new StringBuffer( type );
        if ( serialization != null ) {
            sbuf.append( "; serialization=" )
                .append( serialization );
        }
        return sbuf.toString();
    }

    /**
     * Sets the format in which the table data will be output.
     *
     * @param  format  bulk data format
     */
    @ConfigMethod(
        property = "format",
        usage = "TABLEDATA|BINARY|BINARY2|FITS",
        doc = "<p>Gives the serialization type (DATA element content) "
            + "of output VOTables.</p>"
    )
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
    @ConfigMethod(
        property = "inline",
        doc = "If true, STREAM elements are written base64-encoded "
            + "within the body of the document, "
            + "and if false they are written to a new external binary file "
            + "whose name is derived from that of the output VOTable document. "
            + "This is only applicable to BINARY, BINARY2 and FITS formats "
            + "where output is not to a stream."
    )
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
     *
     * @return  XML declaration
     */
    public String getXMLDeclaration() {
        return xmlDeclaration;
    }

    /**
     * Sets the version of the VOTable standard to which the output
     * of this writer will conform.
     *
     * @param  version   new version
     */
    @ConfigMethod(
        property = "version",
        usage = "V10|V11|V12|V13|V14",
        example = "V14",
        doc = "<p>Gives the version of the VOTable format which will be used "
            + "when writing the VOTable.\n"
            + "\"<code>V10</code>\" is version 1.0 etc.</p>"
    )
    public void setVotableVersion( VOTableVersion version ) {
        this.version = version;
    }

    /**
     * Returns the version of the VOTable standard to which the output
     * of this writer conforms.
     *
     * @return  version
     */
    public VOTableVersion getVotableVersion() {
        return version;
    }

    /**
     * Determines whether the schema location attribute will be written
     * on opening VOTABLE tags.
     *
     * @param  writeSchemaLocation  whether to write xsi:schemaLocation atts
     */
    public void setWriteSchemaLocation( boolean writeSchemaLocation ) {
        this.writeSchemaLocation = writeSchemaLocation;
    }

    /**
     * Indicates whether the schema location attribute will be written
     * on opening VOTABLE tags.
     *
     * @return  whether xsi:schemaLocation attributes will be written
     */
    public boolean getWriteSchemaLocation() {
        return writeSchemaLocation;
    }

    @Override
    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( dataFormat.toString() );
        if ( dataFormat != DataFormat.TABLEDATA ) {
            sbuf.append( "," )
                .append( inline ? "inline" : "href" );
        }
        sbuf.append( ",v" )
            .append( version.getVersionNumber() );
        return sbuf.toString();
    }

    /**
     * Returns a list of votable writers with variant values of attributes.
     *
     * @return   non-standard VOTableWriters.
     */
    public static StarTableWriter[] getStarTableWriters() {
        VOTableVersion version = VOTableVersion.getDefaultVersion();
        List<StarTableWriter> wlist = new ArrayList<StarTableWriter>();
        wlist.add( new VOTableWriter( DataFormat.TABLEDATA, true, version ) );
        wlist.add( new VOTableWriter( DataFormat.BINARY, true, version ) );
        if ( version.allowBinary2() ) {
           wlist.add( new VOTableWriter( DataFormat.BINARY2, true, version ) );
        }
        wlist.add( new VOTableWriter( DataFormat.FITS, false, version ) );
        wlist.add( new VOTableWriter( DataFormat.BINARY, false, version ) );
        if ( version.allowBinary2() ) {
            wlist.add( new VOTableWriter( DataFormat.BINARY2, false,
                                          version ) );
        }
        wlist.add( new VOTableWriter( DataFormat.FITS, true ) );
        return wlist.toArray( new StarTableWriter[ 0 ] );
    }
}
