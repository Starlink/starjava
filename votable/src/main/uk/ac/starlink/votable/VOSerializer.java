package uk.ac.starlink.votable;

import java.io.BufferedWriter;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import uk.ac.starlink.fits.FitsConstants;
import uk.ac.starlink.fits.FitsTableSerializer;
import uk.ac.starlink.fits.FitsTableSerializerConfig;
import uk.ac.starlink.fits.FitsTableWriter;
import uk.ac.starlink.fits.StandardFitsTableSerializer;
import uk.ac.starlink.fits.WideFits;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.util.Base64OutputStream;
import uk.ac.starlink.util.IntList;
import uk.ac.starlink.votable.datalink.ServiceDescriptor;
import uk.ac.starlink.votable.datalink.ServiceParam;

/**
 * Class which knows how to serialize a table's fields and data to 
 * VOTable elements.  For writing a full VOTable document
 * which contains a single table the {@link VOTableWriter} 
 * class may be more convenient, but 
 * this class can be used in a more flexible way, by writing only
 * the elements which are required.
 *
 * <p>Obtain an instance of this class using the {@link #makeSerializer}
 * method.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class VOSerializer {

    private final StarTable table_;
    private final DataFormat format_;
    private final List<DescribedValue> paramList_;
    private final String ucd_;
    private final String utype_;
    private final String description_;
    private final ServiceDescriptor[] servDescrips_;
    final Map<MetaEl,String> coosysMap_;
    final Map<MetaEl,String> timesysMap_;

    final static Logger logger = Logger.getLogger( "uk.ac.starlink.votable" );
    private static final AtomicLong idSeq_ = new AtomicLong();

    /**
     * Constructs a new serializer which can write a given StarTable.
     *
     * @param  table  the table to write
     * @param  format  the data format being used
     * @param  version  output VOTable version
     */
    private VOSerializer( StarTable table, DataFormat format,
                          VOTableVersion version ) {
        table_ = table;
        format_ = format;

        /* Doctor the table's parameter list.  Take out items which are
         * output specially so that only the others get output as PARAM
         * elements. */
        paramList_ = new ArrayList<DescribedValue>();
        String description = null;
        String ucd = null;
        String utype = null;
        List<ServiceDescriptor> sdList = new ArrayList<ServiceDescriptor>();
        for ( DescribedValue dval : table.getParameters() ) {
            ValueInfo pinfo = dval.getInfo();
            String pname = pinfo.getName();
            Class<?> pclazz = pinfo.getContentClass();
            Object value = dval.getValue();
            if ( pname != null && pclazz != null ) {
                if ( pname.equalsIgnoreCase( "description" ) &&
                     pclazz == String.class ) {
                    description = (String) value;
                }
                else if ( pname.equals( VOStarTable.UCD_INFO.getName() )
                       && pclazz == String.class ) {
                    ucd = (String) value;
                }
                else if ( pname.equals( VOStarTable.UTYPE_INFO.getName() ) 
                       && pclazz == String.class ) {
                    utype = (String) value;
                }
                else if ( ServiceDescriptor.class.isAssignableFrom( pclazz ) ) {
                    if ( value instanceof ServiceDescriptor ) {
                        sdList.add( (ServiceDescriptor) value );
                    }
                }
                else {
                    paramList_.add( dval );
                }
            }
        }
        description_ = description;
        ucd_ = ucd;
        utype_ = utype;
        servDescrips_ = sdList.toArray( new ServiceDescriptor[ 0 ] );

        /* Get a base identifier that can be used to prepend to XML ID values.
         * As long as this is unique per output XML document,
         * ID namespace clashes can be avoided.
         * We do it here by incrementing a static variable.
         * That is not absolutely 100% bulletproof, but it will give
         * a value that's unique per JVM, so as long as you're not using
         * multiple JVMs to put together a single VOTable document,
         * (or, more likely, combining generated XML with unedited XML
         * pulled in from a previously generated VOTable document)
         * you should be OK.
         * It would be possible to make this approach more robust
         * by initialising the idSeq_ variable with some kind of
         * pseudo-random value. */
        String baseId = "t" + Long.toString( idSeq_.incrementAndGet() );

        /* Prepare COOSYS and TIMESYS elements.  Identify the materially
         * different *SYS elements that will be required, and store them
         * as keys in a map, with values that are newly constructed but
         * unique-per-JVM identifiers, for later use. */
        coosysMap_ = new LinkedHashMap<MetaEl,String>();
        timesysMap_ = new LinkedHashMap<MetaEl,String>();
        int ncol = table.getColumnCount();
        int ics = 0;
        int its = 0;
        List<ValueInfo> infos = new ArrayList<ValueInfo>();
        for ( int ic = 0; ic < ncol; ic++ ) {
            infos.add( table.getColumnInfo( ic ) );
        }
        for ( DescribedValue dval : table.getParameters() ) {
            infos.add( dval.getInfo() );
        }
        for ( ValueInfo info : infos ) {
            MetaEl coosys = getCoosys( info );
            if ( coosys != null && ! coosysMap_.containsKey( coosys ) ) {
                String id = baseId + "-coosys-" + ++ics;
                coosysMap_.put( coosys, id );
            }
            if ( version.allowTimesys() ) {
                MetaEl timesys = getTimesys( info );
                if ( timesys != null && ! timesysMap_.containsKey( timesys ) ) {
                    String id = baseId + "-timesys-" + ++its;
                    timesysMap_.put( timesys, id );
                }
            }
        }
    }

    /**
     * Returns the data format which this object can serialize to.
     *
     * @return   output format
     */
    public DataFormat getFormat() {
        return format_;
    }

    /**
     * Returns the table object which this object can serialize.
     *
     * @return  table to write
     */
    public StarTable getTable() {
        return table_;
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
     * Writes this serializer's table as a complete TABLE element.
     * If this serializer's format is binary (non-XML) the bytes
     * will get written base64-encoded into a STREAM element.
     * 
     * @param   writer  destination stream
     */
    public void writeInlineTableElement( BufferedWriter writer )
            throws IOException {
         writePreDataXML( writer );
         writeInlineDataElement( writer );
         writePostDataXML( writer );
    }

    public void writeHrefTableElement( BufferedWriter xmlwriter, String href,
                                       DataOutput streamout )
            throws IOException {
        writePreDataXML( xmlwriter );
        writeHrefDataElement( xmlwriter, href, streamout );
        writePostDataXML( xmlwriter );
    }

    /**
     * Writes any PARAM and INFO elements associated with this serializer's
     * table.  These should generally go in the TABLE element.
     * 
     * @param   writer  destination stream
     */
    public void writeParams( BufferedWriter writer ) throws IOException {
        for  ( DescribedValue param : paramList_ ) {
            ValueInfo pinfo0 = param.getInfo();
            DefaultValueInfo pinfo = new DefaultValueInfo( pinfo0 );
            Object pvalue = param.getValue();

            /* Adjust the info so that its dimension sizes are fixed,
             * and matched to the sizes of the actual value.
             * This might make it easier to write or read. */
            if ( pinfo.isArray() ) {
                int[] shape = pinfo.getShape();
                if ( shape != null && shape.length > 0 &&
                     shape[ shape.length - 1 ] < 0 && pvalue != null &&
                     pvalue.getClass().isArray() ) {
                    long block = 1;
                    for ( int idim = 0; idim < shape.length - 1 && block >= 1;
                          idim++ ) {
                        block *= shape[ idim ];
                    }
                    int leng = Array.getLength( pvalue );
                    if ( block <= Integer.MAX_VALUE && leng % block == 0 ) {
                        shape[ shape.length - 1 ] = leng / (int) block;
                        pinfo.setShape( shape );
                    }
                }
            }
            if ( String.class.equals( pinfo.getContentClass() ) &&
                 pinfo.getElementSize() < 0 && pvalue instanceof String ) {
                pinfo.setElementSize( ((String) pvalue).length() );
            }
            if ( String[].class.equals( pinfo.getContentClass() ) &&
                 pinfo.getElementSize() < 0 && pvalue instanceof String[] ) {
                int leng = 0;
                String[] strs = (String[]) pvalue;
                for ( int is = 0; is < strs.length; is++ ) {
                    if ( strs[ is ] != null ) {
                        leng = Math.max( leng, strs[ is ].length() );
                    }
                }
                pinfo.setElementSize( leng );
            }

            /* Adjust the info so that its nullability is set from the data. */
            pinfo.setNullable( Tables.isBlank( pvalue ) );

            /* Try to write it as a typed PARAM element. */
            Encoder encoder = Encoder.getEncoder( pinfo, false, false );
            if ( encoder != null ) {
                String valtext = encoder.encodeAsText( pvalue );
                String content = encoder.getFieldContent();
                Map<String,String> attMap = new LinkedHashMap<String,String>();
                attMap.putAll( getFieldAttributes( encoder, coosysMap_,
                                                   timesysMap_ ) );
                attMap.put( "value", valtext );
                writer.write( "<PARAM" );
                writer.write( formatAttributes( attMap ) );
                if ( content.length() > 0 ) {
                    writer.write( ">" );
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
            else if ( pvalue instanceof URL ) {
                writer.write( "<LINK"
                    + formatAttribute( "title", pinfo.getName() )
                    + formatAttribute( "href", pvalue.toString() )
                    + "/>" );
                writer.newLine();
            }

            /* If it's of a funny type, just try to write it as an INFO. */
            else {
                writer.write( "<INFO" );
                writer.write( formatAttribute( "name", pinfo.getName() ) );
                if ( pvalue != null ) {
                    writer.write( formatAttribute( "value", 
                                                   pvalue.toString() ) );
                }
                writer.write( "/>" );
                writer.newLine();
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
        if ( description_ != null && description_.trim().length() > 0 ) {
            writer.write( "<DESCRIPTION>" );
            writer.newLine();
            writer.write( formatText( description_.trim() ) );
            writer.newLine();
            writer.write( "</DESCRIPTION>" );
            writer.newLine();
        }
    }

    /**
     * Writes the service descriptor parameters of this serializer's table
     * as a sequence of zero or more RESOURCE elements.
     * Each has attributes type="meta" and utype="adhoc:service".
     *
     * @param  writer  destination stream
     */
    public void writeServiceDescriptors( BufferedWriter writer )
            throws IOException {
        for ( ServiceDescriptor sd : servDescrips_ ) {
            writeServiceDescriptor( writer, sd );
        }
    }

    /**
     * Writes a service descriptor object as a RESOURCE element with
     * utype="adhoc:service".
     *
     * @param   writer  destination stream
     * @param   sdesc   service descriptor object
     * @see   <a href="http://www.ivoa.net/documents/DataLink/"
     *           >DataLink-1.0, sec 4</a>
     */
    private void writeServiceDescriptor( BufferedWriter writer,
                                         ServiceDescriptor sdesc )
            throws IOException {
        String sdId = sdesc.getDescriptorId();
        String sdName = sdesc.getName();
        String sdDescription = sdesc.getDescription();
        StringBuffer rtag = new StringBuffer()
            .append( "<RESOURCE" )
            .append( formatAttribute( "type", "meta" ) )
            .append( formatAttribute( "utype", "adhoc:service" ) );
        if ( sdName != null ) {
            rtag.append( formatAttribute( "name", sdName ) );
        }
        if ( sdId != null && sdId.length() > 0 ) {
            rtag.append( formatAttribute( "ID", sdId ) );
        }
        rtag.append( ">" );
        writer.write( rtag.toString() );
        writer.newLine();
        if ( sdDescription != null ) {
            writer.write( "  <DESCRIPTION>"
                        + formatText( sdDescription.trim() )
                        + "</DESCRIPTION>" );
            writer.newLine();
        }
        writeStringParam( writer, "accessURL", sdesc.getAccessUrl() );
        writeStringParam( writer, "standardID", sdesc.getStandardId() );
        writeStringParam( writer, "resourceIdentifier",
                                  sdesc.getResourceIdentifier() );
        ServiceParam[] sdParams = sdesc.getInputParams();
        if ( sdParams.length > 0 ) {
            writer.write( "  <GROUP"
                        + formatAttribute( "name", "inputParams" )
                        + ">" );
            writer.newLine();
            for ( ServiceParam sdParam : sdParams ) {
                writeServiceParam( writer, sdParam );
            }
            writer.write( "  </GROUP>" );
            writer.newLine();
        }
        writer.write( "</RESOURCE>" );
        writer.newLine();
    }

    /**
     * Writes a PARAM element with a given value, if the value is not blank.
     * If the value is null or the empty string, no output is written.
     *
     * @param  writer  destination stream
     * @param  pname   parameter name
     * @parma  pvalue  parameter value
     */
    private void writeStringParam( BufferedWriter writer,
                                   String pname, String pvalue )
            throws IOException {
        if ( pvalue != null && pvalue.length() > 0 ) {
            StringBuffer sbuf = new StringBuffer()
                .append( "  <PARAM" )
                .append( formatAttribute( "name", pname ) )
                .append( formatAttribute( "datatype", "char" ) )
                .append( formatAttribute( "arraysize", "*" ) )
                .append( formatAttribute( "value", pvalue ) )
                .append( "/>" );
            writer.write( sbuf.toString() );
            writer.newLine();
        }
    }

    /**
     * Serialises a ServiceParam object as a PARAM element,
     * assumed to be within an inputParams GROUP.
     *
     * @param  writer  destination stream
     * @param  param   service parameter object
     */
    private void writeServiceParam( BufferedWriter writer, ServiceParam param )
            throws IOException {
        int[] arraysize = param.getArraysize();
        String name = param.getName();
        String datatype = param.getDatatype();
        String value = param.getValue();
        Map<String,String> atts = new LinkedHashMap<String,String>();
        atts.put( "name", name == null ? "??" : name );
        atts.put( "datatype", datatype == null ? "char" : datatype );
        if ( arraysize != null && arraysize.length > 0 ) {
            atts.put( "arraysize", DefaultValueInfo.formatShape( arraysize ) );
        }
        atts.put( "value", value == null ? "" : value );
        atts.put( "unit", param.getUnit() );
        atts.put( "ucd", param.getUcd() );
        atts.put( "utype", param.getUtype() );
        atts.put( "xtype", param.getXtype() );
        atts.put( "ref", param.getRef() );
        for ( String aname :
              new String[] { "unit", "ucd", "utype", "xtype", "ref", } ) {
            String aval = atts.get( aname );
            if ( aval == null || aval.length() == 0 ) {
                atts.remove( aname );
            }
        }
        writer.write( "    <PARAM" + formatAttributes( atts ) + ">" );
        writer.newLine();
        String descrip = param.getDescription();
        if ( descrip != null && descrip.trim().length() > 0 ) {
            writer.write( "      <DESCRIPTION>"
                        + formatText( descrip )
                        + "</DESCRIPTION>" );
            writer.newLine();
        }
        String[] options = param.getOptions();
        String[] minmax = param.getMinMax();
        String min = minmax == null ? null : minmax[ 0 ];
        String max = minmax == null ? null : minmax[ 1 ];
        if ( min != null || max != null || options != null ) {
            writer.write( "      <VALUES>" );
            writer.newLine();
            if ( min != null ) {
                writer.write( "        <MIN"
                            + formatAttribute( "value", min )
                            + "/>" );
                writer.newLine();
            }
            if ( max != null ) {
                writer.write( "        <MAX"
                            + formatAttribute( "value", max )
                            + "/>" );
                writer.newLine();
            }
            if ( options != null ) {
                for ( String opt : options ) {
                    writer.write( "        <OPTION"
                                + formatAttribute( "value", opt )
                                + "/>" );
                    writer.newLine();
                }
            }
            writer.write( "      </VALUES>" );
            writer.newLine();
        }
        writer.write( "    </PARAM>" );
        writer.newLine();
    }

    /**
     * Outputs the TABLE element start tag and all of its content before
     * the DATA element.
     * Other items legal where a TABLE can appear may be prepended
     * if required.
     *
     * @param   writer  output stream
     */
    public void writePreDataXML( BufferedWriter writer ) throws IOException {

        /* If we have COOSYS or TIMESYS elements, write them.
         * The schema constrains where these are allowed to go.
         * Although in some cases they
         * can go on their own before a TABLE element, depending on what
         * comes before that might not be allowed.  It's always safe
         * (at least in VOTable 1.2+, though not 1.1) to wrap them
         * in their own RESOURCE at the same level as a TABLE. */
        if ( coosysMap_.size() + timesysMap_.size() > 0 ) {
            writer.write( "<RESOURCE>" );
            writer.newLine();
            Map<MetaEl,String> metamap = new LinkedHashMap<MetaEl,String>();
            metamap.putAll( coosysMap_ );
            metamap.putAll( timesysMap_ );
            for ( Map.Entry<MetaEl,String> entry : metamap.entrySet() ) {
                MetaEl meta = entry.getKey();
                String id = entry.getValue();
                writer.write( "  " + meta.toXml( id ) );
                writer.newLine();
            }
            writer.write( "</RESOURCE>" );
            writer.newLine();
        }

        /* Output TABLE element start tag. */
        writer.write( "<TABLE" );

        /* Write table name if we have one. */
        String tname = getTable().getName();
        if ( tname != null && tname.trim().length() > 0 ) {
            writer.write( formatAttribute( "name", tname.trim() ) );
        }

        /* Write the number of rows if we know it. */
        long nrow = getTable().getRowCount();
        if ( nrow > 0 ) {
            writer.write( formatAttribute( "nrows", Long.toString( nrow ) ) );
        }

        /* Write UCD and utype information if we have it. */
        if ( ucd_ != null ) {
            writer.write( formatAttribute( "ucd", ucd_ ) );
        }
        if ( utype_ != null ) {
            writer.write( formatAttribute( "utype", utype_ ) );
        }

        /* Close TABLE element start tag. */
        writer.write( ">" );
        writer.newLine();

        /* Output a DESCRIPTION element if we have something suitable. */
        writeDescription( writer );

        /* Output table parameters as PARAM elements. */
        writeParams( writer );

        /* Output FIELD headers. */
        writeFields( writer );
    }

    /**
     * Outputs any content of the TABLE element following the DATA element
     * and the TABLE end tag.
     *
     * @param  writer  output stream
     */
    public void writePostDataXML( BufferedWriter writer ) throws IOException {
        writer.write( "</TABLE>" );
        writer.newLine();
        writeServiceDescriptors( writer );
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
        int vleng = value.length();
        StringBuffer buf = new StringBuffer( name.length() + vleng + 4 );
        buf.append( ' ' )
           .append( name )
           .append( '=' )
           .append( '"' );

        /* Assemble the string, counting the number of single and double
         * quote substitutions required. */
        int nquot = 0;
        int napos = 0;
        for ( int i = 0; i < vleng; i++ ) {
            char c = value.charAt( i );
            switch ( c ) {
                case '<':
                    buf.append( "&lt;" );
                    break;
                case '>':
                    buf.append( "&gt;" );
                    break;
                case '&':
                    buf.append( "&amp;" );
                    break;
                case '"':
                    buf.append( "&quot;" );
                    nquot++;
                    break;
                case '\'':
                    napos++;
                    buf.append( ensureLegalXml( c ) );
                    break;
                default:
                    buf.append( ensureLegalXml( c ) );
            }
        }
        buf.append( '"' );

        /* We're probably done; most likely there were no single or double
         * quotes.  But if it turns out that the output had lots of
         * double quotes and not so many single quotes, redo it with
         * single quotes on the outside to get a tidier result. */
        if ( nquot <= napos ) {
            return buf.toString();
        }
        else {
            buf.setLength( 0 );
            buf.append( ' ' )
               .append( name )
               .append( '=' )
               .append( '\'' );
            for ( int i = 0; i < vleng; i++ ) {
                char c = value.charAt( i );
                switch ( c ) {
                    case '<':
                        buf.append( "&lt;" );
                        break;
                    case '>':
                        buf.append( "&gt;" );
                        break;
                    case '&':
                        buf.append( "&amp;" );
                        break;
                    case '\'':
                        buf.append( "&apos;" );
                        break;
                    default:
                        buf.append( ensureLegalXml( c ) );
                }
            }
            buf.append( '\'' );
            return buf.toString();
        }
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
                    sbuf.append( ensureLegalXml( c ) );
            }
        }
        return sbuf.toString();
    }

    /**
     * Returns a legal XML character corresponding to an input character.
     * Certain characters are simply illegal in XML (regardless of encoding).
     * If the input character is legal in XML, it is returned;
     * otherwise some other weird but legal character 
     * (currently the inverted question mark, "\u00BF") is returned instead.
     *
     * @param   c  input character
     * @return  legal XML character, <code>c</code> if possible
     */
    public static char ensureLegalXml( char c ) {
        return ( ( c >= '\u0020' && c <= '\uD7FF' ) ||
                 ( c >= '\uE000' && c <= '\uFFFD' ) ||
                 ( ((int) c) == 0x09 ||
                   ((int) c) == 0x0A ||
                   ((int) c) == 0x0D ) )
             ? c
             : '\u00BF';
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
    private static String formatAttributes( Map<String,String> atts ) {
        StringBuffer sbuf = new StringBuffer();
        for ( String attname : new TreeSet<String>( atts.keySet() ) ) {
            String attval = atts.get( attname );
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
                                           String content,
                                           Map<String,String> attributes )
            throws IOException {
        writer.write( "<FIELD" + formatAttributes( attributes ) );
        if ( content != null && content.length() > 0 ) {
            writer.write( '>' );
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
     * Applies miscellaneous preparation steps to a table that will
     * have a VOSerializer built from it.
     *
     * @param table  table for preparation
     * @param magicNulls  whether magic null values may be required;
     *        if true, then NULL_VALUE_INFO entries are added to their
     *        auxiliary metadata where required (nullable scalar integer
     *        columns),
     *        if false any such entries are removed
     * @param allowXtype  whether xtype attributes are permitted in the output;
     *        if not, any keys which might give rise to them in the
     *        serialization are removed
     * @return   prepared table (possibly the same as input).
     */
    private static StarTable prepareForSerializer( StarTable table,
                                                   boolean magicNulls,
                                                   boolean allowXtype ) {
        ValueInfo badKey = Tables.NULL_VALUE_INFO;
        ValueInfo ubyteKey = Tables.UBYTE_FLAG_INFO;
        int ncol = table.getColumnCount();
        final ColumnInfo[] colInfos = new ColumnInfo[ ncol ];
        int modified = 0;
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo cinfo = new ColumnInfo( table.getColumnInfo( icol ) );
            boolean isUbyte =
                Boolean.TRUE
               .equals( cinfo.getAuxDatumValue( ubyteKey, Boolean.class ) );
            Class<?> clazz = cinfo.getContentClass();
            if ( magicNulls && cinfo.isNullable() && 
                 Number.class.isAssignableFrom( clazz ) &&
                 cinfo.getAuxDatum( badKey ) == null ) {
                Number badValue;
                if ( isUbyte ) {
                    badValue = new Short( (short) 0xff );
                }
                else if ( clazz == Byte.class || clazz == Short.class ) {
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
            if ( ! magicNulls && ! cinfo.isArray() ) {
                DescribedValue nv = cinfo.getAuxDatum( badKey );
                if ( nv != null ) {
                    cinfo.getAuxData().remove( nv );
                    modified++;
                }
            }
            if ( ! allowXtype ) {
                String xt = cinfo.getXtype();
                if ( xt != null ) {
                    cinfo.setXtype( null );
                    modified++;
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
        return table;
    }

    /**
     * Returns a serializer capable of serializing a given table to
     * given data format, using the default VOTable output version.
     *
     * @param  dataFormat  one of the supported VOTable serialization formats
     * @param  table  the table to be serialized
     * @return  serializer
     */
    public static VOSerializer makeSerializer( DataFormat dataFormat,
                                               StarTable table )
            throws IOException {
        return makeSerializer( dataFormat, VOTableVersion.getDefaultVersion(),
                               table );
    }

    /**
     * Returns a serializer capable of serializing
     * a given table to a given data format using a given VOTable version.
     *
     * @param  dataFormat  one of the supported VOTable serialization formats
     * @param  version  specifies the version of the VOTable standard
     *                  to which the output will conform
     * @param  table  the table to be serialized
     * @return  serializer
     */
    public static VOSerializer makeSerializer( DataFormat dataFormat,
                                               VOTableVersion version,
                                               StarTable table )
            throws IOException {

        /* Prepare. */
        boolean magicNulls =
            ( dataFormat == DataFormat.BINARY ) ||
            ( dataFormat == DataFormat.FITS ) ||
            ( dataFormat == DataFormat.TABLEDATA && ! version.allowEmptyTd() );
        table = prepareForSerializer( table, magicNulls, version.allowXtype() );

        /* Return a serializer. */
        if ( dataFormat == DataFormat.TABLEDATA ) {
            return new TabledataVOSerializer( table, version, magicNulls );
        }
        else if ( dataFormat == DataFormat.FITS ) {

            /* Use some fairly innocuous configuration here.
             * It would be possible to provide user-level configuration
             * options, but the FITS serialization format is very little used,
             * so don't bother unless some compelling use case arises. */
            FitsTableSerializerConfig config = new FitsTableSerializerConfig() {
                public boolean allowSignedByte() {
                    return false;
                }
                public WideFits getWide() {
                    return null;
                }
            };
            return new FITSVOSerializer( table, version,
                new StandardFitsTableSerializer( config, table ) );
        }
        else if ( dataFormat == DataFormat.BINARY ) {
            return new BinaryVOSerializer( table, version, magicNulls );
        }
        else if ( dataFormat == DataFormat.BINARY2 ) {
            if ( version.allowBinary2() ) {
                return new Binary2VOSerializer( table, version, magicNulls );
            }
            else {
                throw new IllegalArgumentException( "BINARY2 format not legal "
                                                  + "for VOTable " + version );
            }
        }
        else {
            throw new AssertionError( "No such format " 
                                    + dataFormat.toString() );
        }
    }

    /**
     * Constructs a FITS-type VOSerializer.  Since a FitsTableSerializer is
     * required for this, if one is already available then supplying it 
     * directly here will be more efficient than calling
     * <code>makeSerializer</code> which will have to construct another,
     * possibly an expensive step.
     *
     * @param  table  table for serialization
     * @param  fitser  fits serializer
     * @param  version  output VOTable version
     * @return  serializer
     */
    public static VOSerializer makeFitsSerializer( StarTable table,
                                                   FitsTableSerializer fitser,
                                                   VOTableVersion version )
            throws IOException {
        table = prepareForSerializer( table, false, true );
        return new FITSVOSerializer( table, version, fitser );
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
    private static Encoder[] getEncoders( StarTable table,
                                          boolean magicNulls ) {
        int ncol = table.getColumnCount();
        Encoder[] encoders = new Encoder[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo info = table.getColumnInfo( icol );
            boolean isUnicode =
                "unicodeChar"
               .equals( info.getAuxDatumValue( VOStarTable.DATATYPE_INFO,
                                               String.class ) );
            encoders[ icol ] =
                Encoder.getEncoder( info, magicNulls, isUnicode );
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
     * @param  coosysMap   MetaEl-&gt;ID map for COOSYS elements
     *                     that will be available
     * @param  timesysMap  MetaEl-&gt;ID map for TIMESYS elements
     *                     that will be available
     * @param  writer  destination stream
     */
    private static void outputFields( Encoder[] encoders, StarTable table,
                                      Map<MetaEl,String> coosysMap,
                                      Map<MetaEl,String> timesysMap,
                                      BufferedWriter writer )
            throws IOException {
        int ncol = encoders.length;
        for ( int icol = 0; icol < ncol; icol++ ) {
            Encoder encoder = encoders[ icol ];
            if ( encoder != null ) {
                String content = encoder.getFieldContent();
                Map<String,String> atts =
                    getFieldAttributes( encoder, coosysMap, timesysMap );
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

        TabledataVOSerializer( StarTable table, VOTableVersion version,
                               boolean magicNulls ) {
            super( table, DataFormat.TABLEDATA, version );
            encoders = getEncoders( table, magicNulls );
        }

        public void writeFields( BufferedWriter writer ) throws IOException {
            outputFields( encoders, getTable(), coosysMap_, timesysMap_,
                          writer );
        }
     
        public void writeInlineDataElement( BufferedWriter writer )
                throws IOException {
            writer.write( "<DATA>" );
            writer.newLine();
            writer.write( "<TABLEDATA>" );
            writer.newLine();
            int ncol = encoders.length;
            RowSequence rseq = getTable().getRowSequence();
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
                                        VOTableVersion version,
                                        String tagname ) {
            super( table, format, version );
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

        BinaryVOSerializer( StarTable table, VOTableVersion version,
                            boolean magicNulls ) {
            super( table, DataFormat.BINARY, version, "BINARY" );
            encoders = getEncoders( table, magicNulls );
        }

        public void writeFields( BufferedWriter writer ) throws IOException {
            outputFields( encoders, getTable(), coosysMap_, timesysMap_,
                          writer );
        }

        public void streamData( DataOutput out ) throws IOException {
            int ncol = encoders.length;
            RowSequence rseq = getTable().getRowSequence();
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
     * BINARY2 format implementation of VOSerializer.
     */
    private static class Binary2VOSerializer extends StreamableVOSerializer {
        private final Encoder[] encoders;

        Binary2VOSerializer( StarTable table, VOTableVersion version,
                             boolean magicNulls ) {
            super( table, DataFormat.BINARY2, version, "BINARY2" );
            encoders = getEncoders( table, magicNulls );
        }

        public void writeFields( BufferedWriter writer ) throws IOException {
            outputFields( encoders, getTable(), coosysMap_, timesysMap_,
                          writer );
        }

        public void streamData( DataOutput out ) throws IOException {

            /* Restrict attention to columns with non-null encoders,
             * that is those which we will actually be writing out. */
            IntList icolList = new IntList( encoders.length );
            for ( int icol = 0; icol < encoders.length; icol++ ) {
                if ( encoders[ icol ] != null ) {
                    icolList.add( icol );
                }
            }
            int[] icols = icolList.toIntArray();
            int ncol = icols.length;
            boolean[] nullFlags = new boolean[ ncol ];

            /* Read data from table. */
            RowSequence rseq = getTable().getRowSequence();
            try {
                while ( rseq.next() ) {
 
                    /* Prepare and write the null-flag array. */
                    Object[] row = rseq.getRow();
                    for ( int jcol = 0; jcol < ncol; jcol++ ) {
                        int icol = icols[ jcol ];
                        Object cell = row[ icol ];
                        nullFlags[ jcol ] = cell == null;
                    }
                    FlagIO.writeFlags( out, nullFlags );

                    /* Write the data cells. */
                    for ( int jcol = 0; jcol < ncol; jcol++ ) {
                        int icol = icols[ jcol ];
                        Object cell = row[ icol ];
                        encoders[ icol ].encodeToStream( cell, out );
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

        FITSVOSerializer( StarTable table, VOTableVersion version,
                          FitsTableSerializer fitser )
                throws IOException {
            super( table, DataFormat.FITS, version, "FITS" );
            this.fitser = fitser;
        }

        public void writeFields( BufferedWriter writer ) throws IOException {
            int ncol = getTable().getColumnCount();
            for ( int icol = 0; icol < ncol; icol++ ) {

                /* Get information about how this column is going to be 
                 * written by the FITS serializer. */
                char tform = fitser.getFormatChar( icol );
                int[] dims = fitser.getDimensions( icol );
                String badval = fitser.getBadValue( icol );

                /* Only write a FIELD element if the FITS serializer is going
                 * to serialize it. */
                if ( dims != null ) {

                    /* Get the basic information for this column. */
                    Encoder encoder =
                        Encoder.getEncoder( getTable().getColumnInfo( icol ),
                                            true, false );
                    String content = encoder.getFieldContent();
                    Map<String,String> atts =
                        getFieldAttributes( encoder, coosysMap_, timesysMap_ );

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

                    /* Modify the VALUES text to match what the FITS serializer
                     * will write. */
                    encoder.setNullString( badval );

                    /* Write out the FIELD element with attributes which match
                     * the way the FITS serializer will write the table. */
                    writeFieldElement( writer, content, atts );
                }
                else {
                    writer.write( "<!-- Omitted column " +
                                  getTable().getColumnInfo( icol ) + " -->" );
                    writer.newLine();
                }
            }
        }

        public void streamData( DataOutput out ) throws IOException {
            FitsConstants.writeEmptyPrimary( out );
            new FitsTableWriter().writeTableHDU( getTable(), fitser, out );
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

    /**
     * Returns the attributes required for a FIELD/PARAM element given the
     * Encoder being used to write the column or parameter in question.
     *
     * @param  encoder   encoder to write FIELD/PARAM data
     * @param  coosysMap   MetaEl-&gt;ID map for COOSYS elements that will be
     *                     available in the output document
     * @param  timesysMap  MetaEl-&gt;ID map for TIMESYS elements that will be
     *                     available in the output document
     * @return   map of FIELD/PARAM attribute name-&gt;value pairs
     */
    private static Map<String,String>
            getFieldAttributes( Encoder encoder, Map<MetaEl,String> coosysMap,
                                Map<MetaEl,String> timesysMap ) {

        /* Query encoder for basic items. */
        Map<String,String> map = encoder.getFieldAttributes();

        /* Add a ref attribute pointing to a COOSYS or TIMESYS element if one
         * that matches the requirements of the element in question
         * has been provided.  Note this relies on the fact that
         * the MetaEl class has suitable equality semantics. */
        ValueInfo info = encoder.getInfo();
        MetaEl coosys = getCoosys( info );
        MetaEl timesys = getTimesys( info );
        String csId = coosysMap != null ? coosysMap.get( coosys ) : null;
        String tsId = timesysMap != null ? timesysMap.get( timesys ) : null;
        if ( csId != null ) {
            map.put( "ref", csId );
        }
        else if ( tsId != null ) {
            map.put( "ref", tsId );
        }
        return map;
    }

    /**
     * Returns the MetaEl object corresponding to the COOSYS metadata
     * for a given ValueInfo, if such metadata is present.
     *
     * @param  info  item metadata
     * @retun   MetaEl object representing COOSYS, or null if none required
     */
    private static MetaEl getCoosys( ValueInfo info ) {
        Map<String,String> map = new LinkedHashMap<String,String>();
        addAtt( map, info, VOStarTable.COOSYS_SYSTEM_INFO, "system" );
        addAtt( map, info, VOStarTable.COOSYS_EPOCH_INFO, "epoch" );
        addAtt( map, info, VOStarTable.COOSYS_EQUINOX_INFO, "equinox" );
        return map.size() > 0 ? new MetaEl( "COOSYS", map ) : null;
    }

    /**
     * Returns the MetaEl object corresponding to the TIMESYS metadata
     * for a given ValueInfo, if such metadata is present.
     *
     * @param  info  item metadata
     * @retun   MetaEl object representing TIMESYS, or null if none required
     */
    private static MetaEl getTimesys( ValueInfo info ) {
        Map<String,String> map = new LinkedHashMap<String,String>();
        addAtt( map, info, VOStarTable.TIMESYS_TIMEORIGIN_INFO, "timeorigin" );
        addAtt( map, info, VOStarTable.TIMESYS_TIMESCALE_INFO, "timescale" );
        addAtt( map, info, VOStarTable.TIMESYS_REFPOSITION_INFO, "refposition");
        return map.size() > 0 ? new MetaEl( "TIMESYS", map ) : null;
    }

    /**
     * Utility method to add an item to the MetaEl attribute map
     * given value metadata.
     *
     * @param  map    attribute map to augment
     * @param  info   info item containing aux metadata
     * @param  auxKey aux metadata key for a String item
     * @param  attname  name of entry in attribute map
     */
    private static void addAtt( Map<String,String> map, ValueInfo info,
                                ValueInfo auxKey, String attname ) {
        DescribedValue dval = info.getAuxDatumByName( auxKey.getName() );
        if ( dval != null ) {
            String value = dval.getTypedValue( String.class );
            if ( value != null && value.trim().length() > 0 ) {
                map.put( attname, value );
            }
        }
    }

    /**
     * Represents an element with a given name and set of relevant attributes.
     * The implementation is just a very thin wrapper round a
     * name-&gt;value attribute map.
     * The equals and hashMap methods are implemented such that
     * instances with the same element name and attribute values
     * will evaluate as equal.
     */
    private static class MetaEl {

        private final String elName_;
        private final Map<String,String> attMap_;

        /**
         * Constructor.
         *
         * @param   elName  element name
         * @param   attMap  attribute name-&gt;value map,
         *                  <em>excluding</em> the ID attribute
         */
        MetaEl( String elName, Map<String,String> attMap ) {
            elName_ = elName;
            attMap_ = Collections.unmodifiableMap( attMap );
        }

        /**
         * Returns the XML serialization of this object as an element.
         *
         * @param   id  value of ID attribute
         * @return  element XML serialization
         */
        public String toXml( String id ) {
            return new StringBuffer()
                  .append( "<" )
                  .append( elName_ )
                  .append( formatAttribute( "ID", id ) )
                  .append( formatAttributes( attMap_ ) )
                  .append( "/>" )
                  .toString();
        }

        @Override
        public int hashCode() {
            int code = 442041;
            code = 23 * code + elName_.hashCode();
            code = 23 * code + attMap_.hashCode();
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof MetaEl ) {
                MetaEl other = (MetaEl) o;
                return this.elName_.equals( other.elName_ )
                    && this.attMap_.equals( other.attMap_ );
            }
            else {
                return false;
            }
        }
    }
}
