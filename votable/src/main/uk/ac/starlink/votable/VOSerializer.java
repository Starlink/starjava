package uk.ac.starlink.votable;

import java.io.BufferedWriter;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Logger;
import uk.ac.starlink.fits.FitsConstants;
import uk.ac.starlink.fits.FitsTableSerializer;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.WrapperStarTable;

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
     * Writes any PARAM and INFO elements associated with this serializer's
     * table.  These should generally go in the RESOURCE element
     * in which the table will be contained.
     * 
     * @param   writer  destination stream
     */
    public void writeParams( BufferedWriter writer ) throws IOException {
        for ( Iterator it = table.getParameters().iterator(); it.hasNext(); ) {
            DescribedValue param = (DescribedValue) it.next();
            ValueInfo pinfo = param.getInfo();

            /* Output the characteristics of this param unless it's called
             * 'description' in which case it gets treated specially. */
            if ( ! pinfo.getName().equalsIgnoreCase( "description" ) ) {

                /* Try to write it as a typed PARAM element. */
                Encoder encoder = Encoder.getEncoder( pinfo );
                if ( encoder != null ) {
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

                /* If it's a URL write it as a LINK. */
                else if ( param.getValue() instanceof URL ) {
                    writer.write( "<LINK"
                        + formatAttribute( "title", pinfo.getName() )
                        + formatAttribute( "href", param.getValue().toString() )
                        + ">" );
                    writer.newLine();
                }

                /* If it's of a funny type, just try to write it as an INFO. */
                else {
                    Object value = param.getValue();
                    writer.write( "<INFO" );
                    writer.write( formatAttribute( "name", pinfo.getName() ) );
                    if ( value != null ) {
                        writer.write( formatAttribute( "value", 
                                                       value.toString() ) );
                    }
                    writer.write( "/>" );
                    writer.newLine();
                }
            }
        }
    }

    /**
     * Writes any DESCRIPTION element associated with this serializer's table.
     * This should generally go just inside the TABLE element itself.
     * If there's no suitable description text, nothing will be written.
     *
     * @param   writer  destination stream
     */
    public void writeDescription( BufferedWriter writer ) throws IOException {
        String description = null;
        for ( Iterator it = table.getParameters().iterator(); it.hasNext(); ) {
            DescribedValue dval = (DescribedValue) it.next();
            if ( dval.getInfo().getName().equalsIgnoreCase( "description" ) ) {
                Object value = dval.getValue();
                if ( value instanceof String && 
                     ((String) value).trim().length() > 0 ) {
                    description = (String) value;
                    break;
                }
            }
        }
        if ( description != null ) {
            writer.write( "<DESCRIPTION>" );
            writer.newLine();
            writer.write( formatText( description.trim() ) );
            writer.newLine();
            writer.write( "</DESCRIPTION>" );
            writer.newLine();
        }
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
    public static String formatAttribute( String name, String value ) {
        return new StringBuffer()
            .append( ' ' )
            .append( name )
            .append( '=' )
            .append( '"' )
            .append( value.replaceAll( "&", "&amp;" )
                          .replaceAll( "<", "&lt;" )
                          .replaceAll( ">", "&gt;" )
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
    public static String formatText( String text ) {
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
     * Writes a FIELD element to a writer.
     *
     * @param  content  text content of the element, if any
     * @param  attributes   a name-value map of attributes
     * @param  writer    destination stream
     */
    private static void writeFieldElement( BufferedWriter writer,
                                           String content, Map attributes )
            throws IOException {
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
     * Factory method which returns a serializer capable of serializing
     * a given table to a given data format.
     *
     * @param  dataFormat  one of the supported VOTable serialization formats
     * @param  table  the table to be serialized
     */
    public static VOSerializer makeSerializer( DataFormat dataFormat,
                                               StarTable table )
            throws IOException {

        /* Ensure that columns have NULL_VALUE_INFO keys in their auxiliary
         * metadata if they need them (that is, if they are nullable
         * integer typed columns).  This may be required to ensure that 
         * null values get serialized properly. */
        ValueInfo badKey = Tables.NULL_VALUE_INFO;
        int ncol = table.getColumnCount();
        final ColumnInfo[] colInfos = new ColumnInfo[ ncol ];
        int modified = 0;
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo cinfo = new ColumnInfo( table.getColumnInfo( icol ) );
            Class clazz = cinfo.getContentClass();
            if ( cinfo.isNullable() && 
                 Number.class.isAssignableFrom( clazz ) &&
                 cinfo.getAuxDatum( badKey ) == null ) {
                Number badValue;
                if ( clazz == Byte.class || clazz == Short.class ) {
                    badValue = new Short( Short.MIN_VALUE );
                }
                else if ( clazz == Integer.class ) {
                    badValue = new Integer( Integer.MIN_VALUE );
                }
                else if ( clazz == Long.class ) {
                    badValue = new Long( Long.MIN_VALUE );
                }
                else {
                    badValue = null;
                }
                if ( badValue != null ) {
                    modified++;
                    cinfo.getAuxData()
                         .add( new DescribedValue( badKey, badValue ) );
                }
            }
            colInfos[ icol ] = cinfo;
        }
        if ( modified > 0 ) {
            table = new WrapperStarTable( table ) {
                public ColumnInfo getColumnInfo( int icol ) {
                    return colInfos[ icol ];
                }
            };
        }

        /* Return a serializer. */
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
                writeFieldElement( writer, content, atts );
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
            RowSequence rseq = table.getRowSequence();
            try {
                while ( rseq.next() ) {
                    writer.write( "  <TR>" );
                    writer.newLine();
                    Object[] rowdata = rseq.getRow();
                    for ( int icol = 0; icol < ncol; icol++ ) {
                        Encoder encoder = encoders[ icol ];
                        if ( encoder != null ) {
                            String text =
                                encoder.encodeAsText( rowdata[ icol ] );
                            writer.write( "    <TD>" );
                            writer.write( formatText( text ) );
                            writer.write( "</TD>" );
                            writer.newLine();
                        }
                    }
                    writer.write( "  </TR>" );
                    writer.newLine();
                }
            }
            finally {
                rseq.close();
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
            xmlwriter.write( "<STREAM" + formatAttribute( "href", href ) +
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
            RowSequence rseq = table.getRowSequence();
            try {
                while ( rseq.next() ) {
                    Object[] row = rseq.getRow();
                    for ( int icol = 0; icol < ncol; icol++ ) {
                        Encoder encoder = encoders[ icol ];
                        if ( encoder != null ) {
                            encoder.encodeToStream( row[ icol ], out );
                        }
                    }
                }
            }
            finally {
                rseq.close();
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
                        if ( ! "1".equals( atts.get( "arraysize" ) ) ) {
                            atts.remove( "arraysize" );
                        }
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
                    writeFieldElement( writer, content, atts );
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
