package uk.ac.starlink.votable;

import java.io.BufferedWriter;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Map;
import java.util.logging.Logger;
import uk.ac.starlink.fits.FitsConstants;
import uk.ac.starlink.fits.FitsTableSerializer;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;

/**
 * Class which knows how to serialize a table's fields and data to a
 * VOTable DATA element.
 * Obtain an instance of this class using the {@link #makeSerializer}
 * method.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class VOSerializer {
    final StarTable table;
    private final DataFormat format;

    final static Logger logger = Logger.getLogger( "uk.ac.starlink.votable" );

    /**
     * Constructs a new serializer which can write a given StarTable.
     *
     * @param  table  the table to write
     * @param  format  the data format being used
     */
    private VOSerializer( StarTable table, DataFormat format ) {
        this.table = table;
        this.format = format;
    }

    /**
     * Returns the data format which this object can serialize to.
     *
     * @return   output format
     */
    public DataFormat getFormat() {
        return format;
    }

    /**
     * Returns the table object which this object can serialize.
     *
     * @return  table to write
     */
    public StarTable getTable() {
        return table;
    }

    /**
     * Writes the FIELD headers corresponding to this table on a given writer.
     *
     * @param  writer  destination stream
     */
    public abstract void writeFields( BufferedWriter writer )
            throws IOException;

    /**
     * Writes this serializer's table data as a self-contained 
     * &lt;DATA&gt; element.
     * If this serializer's format is binary (non-XML) the bytes
     * will get written base64-encoded into a STREAM element.
     * 
     * @param   writer  destination stream
     */
    public abstract void writeInlineDataElement( BufferedWriter writer )
            throws IOException;

    /**
     * Writes this serializer's table data to a &lt;DATA&gt; element 
     * containing a &lt;STREAM&gt; element which references an external
     * data source (optional method).  
     * The binary data itself will be written to an
     * output stream supplied separately (it will not be inline).
     * If this serializer's format is not binary (i.e. if it's TABLEDATA)
     * an <tt>UnsupportedOperationException</tt> will be thrown.
     *
     * @param  xmlwriter  destination stream for the XML output
     * @param  href   URL for the external stream (output as the <tt>href</tt>
     *                attribute of the written &lt;STREAM&gt; element)
     * @param  streamout  destination stream for the binary table data
     */
    public abstract void writeHrefDataElement( BufferedWriter xmlwriter,
                                               String href,
                                               DataOutput streamout )
            throws IOException;

    /**
     * Factory method which returns a serializer capable of serializing
     * a given table to a given data format.
     *
     * @param  dataFormat  one of the supported VOTable serialization formats
     * @param  table  the table to be serialized
     */
    public static VOSerializer makeSerializer( DataFormat dataFormat,
                                               StarTable table )
            throws IOException {
        if ( dataFormat == DataFormat.TABLEDATA ) {
            return new TabledataVOSerializer( table );
        }
        else if ( dataFormat == DataFormat.FITS ) {
            return new FITSVOSerializer( table );
        }
        else if ( dataFormat == DataFormat.BINARY ) {
            return new BinaryVOSerializer( table );
        }
        else {
            throw new AssertionError( "No such format " 
                                    + dataFormat.toString() );
        }
    }


    //
    // A couple of non-public static methods follow which are used by
    // both the TABLEDATA and the BINARY serializers.  These are only
    // in this class because they have to be somewhere - they should
    // really be methods of an abstract superclass of the both of them,
    // but this is impossible since the BINARY one already inherits
    // from StreamableVOSerializer.  Multiple inheritance would be 
    // nice for once.
    //

    /**
     * Returns the set of encoders used to encode a given StarTable in
     * one of the native formats (BINARY or TABLEDATA).
     *
     * @param  table  the table to characterise
     * @return  an array of encoders used for encoding its data
     */
    private static Encoder[] getEncoders( StarTable table ) {
        int ncol = table.getColumnCount();
        Encoder[] encoders = new Encoder[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo info = table.getColumnInfo( icol );
            encoders[ icol ] = Encoder.getEncoder( info );
            if ( encoders[ icol ] == null ) {
                logger.warning( "Can't serialize column " + info + " of type " +
                                info.getContentClass().getName() );
            }
        }
        return encoders;
    }

    /**
     * Writes the FIELD elements corresponding to a set of Encoders.
     *
     * @param  encoders  the list of encoders (some may be null)
     * @param  table   the table being serialized
     * @param  writer  destination stream
     */
    private static void outputFields( Encoder[] encoders, StarTable table,
                                      BufferedWriter writer )
            throws IOException {
        int ncol = encoders.length;
        for ( int icol = 0; icol < ncol; icol++ ) {
            Encoder encoder = encoders[ icol ];
            if ( encoder != null ) {
                String content = encoder.getFieldContent();
                Map atts = encoder.getFieldAttributes();
                VOTableWriter.writeFieldElement( writer, content, atts );
            }
            else {
                writer.write( "<!-- Omitted column " +
                              table.getColumnInfo( icol ) + " -->" );
                writer.newLine();
            }
        }
    }


    /**
     * TABLEDATA implementation of VOSerializer.
     */
    private static class TabledataVOSerializer extends VOSerializer {
        private final Encoder[] encoders;

        TabledataVOSerializer( StarTable table ) {
            super( table, DataFormat.TABLEDATA );
            encoders = getEncoders( table );
        }

        public void writeFields( BufferedWriter writer ) throws IOException {
            outputFields( encoders, table, writer );
        }
     
        public void writeInlineDataElement( BufferedWriter writer )
                throws IOException {
            writer.write( "<DATA>" );
            writer.newLine();
            writer.write( "<TABLEDATA>" );
            writer.newLine();
            int ncol = encoders.length;
            for ( RowSequence rseq = table.getRowSequence(); rseq.hasNext(); ) {
                rseq.next();
                writer.write( "  <TR>" );
                writer.newLine();
                Object[] rowdata = rseq.getRow();
                for ( int icol = 0; icol < ncol; icol++ ) {
                    Encoder encoder = encoders[ icol ];
                    if ( encoder != null ) {
                        String text = encoder.encodeAsText( rowdata[ icol ] );
                        writer.write( "    <TD>" );
                        writer.write( VOTableWriter.formatText( text ) );
                        writer.write( "</TD>" );
                        writer.newLine();
                    }
                }
                writer.write( "  </TR>" );
                writer.newLine();
            }
            writer.write( "</TABLEDATA>" );
            writer.newLine();
            writer.write( "</DATA>" );
            writer.newLine();
            writer.flush();
        }

        public void writeHrefDataElement( BufferedWriter writer, String href,
                                          DataOutput streamout ) {
            throw new UnsupportedOperationException( 
                "TABLEDATA only supports inline output" );
        }
    }

    /**
     * Abstract subclass for VOSerializers which write their data as 
     * binary output (bytes rather than characters) to a STREAM element.
     * This class is package-private (rather than private) since it is
     * used by VOTableWriter for efficiency reasons.
     */
    static abstract class StreamableVOSerializer extends VOSerializer {
        private final String tagname;

        /**
         * Initialises this serializer.
         *
         * @param  table  the table it will serialize
         * @param  format  serialization format
         * @param  tagname  the name of the XML element that contains the data
         */
        private StreamableVOSerializer( StarTable table, DataFormat format,
                                        String tagname ) {
            super( table, format );
            this.tagname = tagname;
        }

        /**
         * Writes raw binary data representing the table data cells 
         * to an output stream.  These are the data which are contained in the
         * STREAM element of a VOTable document.  
         * No markup (e.g. the STREAM start/end tags) should be included.
         * 
         * @param  out  destination stream
         */
        public abstract void streamData( DataOutput out ) throws IOException;

        public void writeInlineDataElement( BufferedWriter writer ) 
                throws IOException {

            /* Start the DATA element. */
            writer.write( "<DATA>" );
            writer.newLine();
            writer.write( "<" + tagname + ">" );
            writer.newLine();

            /* Write the STREAM element. */
            writer.write( "<STREAM encoding='base64'>" );
            writer.newLine();
            Base64OutputStream b64out = 
                new Base64OutputStream( new WriterOutputStream( writer ), 16 );
            DataOutputStream dataout = new DataOutputStream( b64out );
            streamData( dataout );
            dataout.flush();
            b64out.endBase64();
            writer.write( "</STREAM>" );
            writer.newLine();

            /* Finish off the DATA element. */
            writer.write( "</" + tagname + ">" );
            writer.newLine();
            writer.write( "</DATA>" );
            writer.newLine();
        }

        public void writeHrefDataElement( BufferedWriter xmlwriter, String href,
                                          DataOutput streamout )
                throws IOException {

            /* Start the DATA element. */
            xmlwriter.write( "<DATA>" );
            xmlwriter.newLine();
            xmlwriter.write( '<' + tagname + '>' );
            xmlwriter.newLine();
            
            /* Write the STREAM element. */
            xmlwriter.write( "<STREAM" + 
                             VOTableWriter.formatAttribute( "href", href ) +
                             "/>" );
            xmlwriter.newLine();

            /* Finish the DATA element. */
            xmlwriter.write( "</" + tagname + ">" );
            xmlwriter.newLine();
            xmlwriter.write( "</DATA>" );
            xmlwriter.newLine();

            /* Write the bulk data to the output stream. */
            streamData( streamout );
        }
    }

    /**
     * BINARY format implementation of VOSerializer.
     */
    private static class BinaryVOSerializer extends StreamableVOSerializer {
        private final Encoder[] encoders;

        BinaryVOSerializer( StarTable table ) {
            super( table, DataFormat.BINARY, "BINARY" );
            encoders = getEncoders( table );
        }

        public void writeFields( BufferedWriter writer ) throws IOException {
            outputFields( encoders, table, writer );
        }

        public void streamData( DataOutput out ) throws IOException {
            int ncol = encoders.length;
            for ( RowSequence rseq = table.getRowSequence(); rseq.hasNext(); ) {
                rseq.next();
                Object[] row = rseq.getRow();
                for ( int icol = 0; icol < ncol; icol++ ) {
                    Encoder encoder = encoders[ icol ];
                    if ( encoder != null ) {
                        encoder.encodeToStream( row[ icol ], (DataOutput) out );
                    }
                }
            }
        }
    }

    /**
     * FITS format implementation of VOSerializer.
     */
    private static class FITSVOSerializer extends StreamableVOSerializer {

        private final FitsTableSerializer fitser;

        FITSVOSerializer( StarTable table ) throws IOException {
            super( table, DataFormat.FITS, "FITS" );
            fitser = new FitsTableSerializer( table );
        }

        public void writeFields( BufferedWriter writer ) throws IOException {
            int ncol = table.getColumnCount();
            for ( int icol = 0; icol < ncol; icol++ ) {

                /* Get information about how this column is going to be 
                 * written by the FITS serializer. */
                char tform = fitser.getFormatChar( icol );
                int[] dims = fitser.getDimensions( icol );

                /* Only write a FIELD element if the FITS serializer is going
                 * to serialize it. */
                if ( dims != null ) {

                    /* Get the basic information for this column. */
                    Encoder encoder =
                        Encoder.getEncoder( table.getColumnInfo( icol ) );
                    String content = encoder.getFieldContent();
                    Map atts = encoder.getFieldAttributes();

                    /* Modify the datatype attribute to match what the FITS
                     * serializer will write. */
                    String datatype;
                    switch ( tform ) {
                        case 'L': datatype = "boolean";       break;
                        case 'X': datatype = "bit";           break;
                        case 'B': datatype = "unsignedByte";  break;
                        case 'I': datatype = "short";         break;
                        case 'J': datatype = "int";           break;
                        case 'K': datatype = "long";          break;
                        case 'A': datatype = "char";          break;
                        case 'E': datatype = "float";         break;
                        case 'D': datatype = "double";        break;
                        case 'C': datatype = "floatComplex";  break;
                        case 'M': datatype = "doubleComplex"; break;
                        default:
                            throw new AssertionError( "Unknown format letter " 
                                                    + tform );
                    }
                    atts.put( "datatype", datatype );

                    /* Modify the arraysize attribute to match what the FITS
                     * serializer will write. */
                    if ( dims.length == 0 ) {
                        atts.remove( "arraysize" );
                    }
                    else {
                        StringBuffer arraysize = new StringBuffer();
                        for ( int i = 0; i < dims.length; i++ ) {
                            if ( i > 0 ) {
                                arraysize.append( 'x' );
                            }
                            arraysize.append( dims[ i ] );
                        }
                        atts.put( "arraysize", arraysize.toString() );
                    }

                    /* Write out the FIELD element with attributes which match
                     * the way the FITS serializer will write the table. */
                    VOTableWriter.writeFieldElement( writer, content, atts );
                }
                else {
                    writer.write( "<!-- Omitted column " + 
                                  table.getColumnInfo( icol ) + " -->" );
                    writer.newLine();
                }
            }
        }

        public void streamData( DataOutput out ) throws IOException {
            FitsConstants.writeEmptyPrimary( out );
            fitser.writeHeader( out );
            fitser.writeData( out );
        }

    }

    /**
     * Adapter class which turns a Writer into an OutputStream.
     * This is used for writing base64 down -
     * we don't worrry about encodings here since the only characters
     * going down the writer will be base64-type characters, which
     * can just be typecast to bytes.
     */
    private static class WriterOutputStream extends OutputStream {
        Writer writer;
        static final int BUFLENG = 10240;
        char[] mainBuf = new char[ BUFLENG ];
        WriterOutputStream( Writer writer ) {
            this.writer = writer;
        }
        public void close() throws IOException {
            writer.close();
        }
        public void flush() throws IOException {
            writer.flush();
        }
        public void write( byte[] b ) throws IOException {
            write( b, 0, b.length );
        }
        public void write( byte[] b, int off, int len ) throws IOException {
            char[] buf = len <= BUFLENG ? mainBuf : new char[ len ];
            for ( int i = 0; i < len; i++ ) {
                buf[ i ] = (char) b[ off++ ];
            }
            writer.write( buf, 0, len );
        }
        public void write(int b) throws IOException {
            writer.write( b );
        }
    }
}
