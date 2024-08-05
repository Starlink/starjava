package uk.ac.starlink.pds4;

import gov.nasa.pds.label.object.FieldType;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.util.DOMUtils;

/**
 * Parses a PDS4 Label (an XML file) to extract information that can be
 * used to read table data.
 *
 * <p>This code was written with reference to the PDS4 data standards v1.16.0.
 *
 * @author  Mark Taylor
 * @since   24 Nov 2021
 * @see <a href="https://pds.nasa.gov/datastandards/documents/dd/current/PDS4_PDS_DD_1G00.html"
 *         >PDS4 Common Data Dictionary</a>
 */
public class LabelParser {

    private final boolean observationalOnly_;
    private final Collection<String> blankSpecials_;

    static final String[] DEFAULT_BLANK_SPECIALS = new String[] {
        "saturated_constant",
        "missing_constant",
        "error_constant",
        "invalid_constant",
        "unknown_constant",
        "not_applicable_constant",
        "high_instrument_saturation",
        "high_representation_saturation",
        "low_instrument_saturation",
        "low_representation_saturation",
    };

    /**
     * Default parser.
     */
    public LabelParser() {
        this( false, DEFAULT_BLANK_SPECIALS );
    }

    /**
     * Custom constructor.
     *
     * @param  observationalOnly  if true, only File_Observational tables
     *                            are included; if false all are included
     * @param  blankSpecials  list of special constant elements to map
     *                        to null values in output table data
     */
    public LabelParser( boolean observationalOnly, String[] blankSpecials ) {
        observationalOnly_ = observationalOnly;
        blankSpecials_ = new HashSet<String>( Arrays.asList( blankSpecials ) );
    }

    /**
     * Parses the label file at a given URL to create a Label object.
     *
     * @param   url  location of PDS4 label XML file
     * @return   parsed label object
     */
    public Label parseLabel( URL url ) throws IOException {

        /* Determine the context (parent) URL of the supplied URL.
         * This will be necessary to locate the data file(s) described by
         * the label.  This code was copied from
         * gov/nasa/pds/objectAccess/example/ExtractTable. */
        URI parent;
        try {
            URI labelUri = url.toURI();
            parent = labelUri.getPath().endsWith( "/" )
                   ? labelUri.resolve( ".." )
                   : labelUri.resolve( "." );
        }
        catch ( URISyntaxException e ) {
            throw new TableFormatException( "Badly-formed URL", e );
        }

        /* Parse and return. */
        return parseLabel( url.openStream(), parent );
    }

    /**
     * Parses the label file in a given File to create a Label object.
     *
     * @param   file  location of PDS4 label XML file
     * @return   parsed label object
     */
    public Label parseLabel( File file ) throws IOException {
        InputStream in = new FileInputStream( file );
        File parentFile = file.getAbsoluteFile().getParentFile();
        return parseLabel( in, parentFile.toURI() );
    }

    /**
     * Parses the label file from a given InputStream to create a Label object.
     *
     * @param  in  input stream, closed unconditionally on exit
     * @param  contextUri   context URI
     * @return   parsed label object
     */
    public Label parseLabel( InputStream in, URI contextUri )
            throws IOException {
        try {
            return attemptParseLabel( in, contextUri );
        }
        catch ( SAXException e ) {
            throw new TableFormatException( "Label file not XML", e );
        }
        catch ( ParserConfigurationException | XPathExpressionException e ) {
            throw new TableFormatException( "Library error", e );
        }
    }

    /**
     * Does the work for parsing the label input stream,
     * possibly throwing various exceptions.
     *
     * @param  in  input stream, closed unconditionally on exit
     * @param  contextUri   context URI
     * @return   parsed label object
     */
    private Label attemptParseLabel( InputStream in, URI contextUri )
            throws IOException, SAXException,
                   ParserConfigurationException,
                   XPathExpressionException {
        Document doc;
        try {
            doc = DocumentBuilderFactory
                 .newInstance()
                 .newDocumentBuilder()
                 .parse( in );
        }
        finally {
            in.close();
        }
        XPath xpath = getXPath();
        NodeList tableNodes =
            (NodeList) xpath.evaluate( createTablesXpath(), doc,
                                       XPathConstants.NODESET );
        int nt = tableNodes.getLength();
        List<Table> tableList = new ArrayList<>( nt );
        for ( int it = 0; it < nt; it++ ) {
            Table table = createTable( (Element) tableNodes.item( it ) );
            if ( table != null ) {
                tableList.add( table );
            }
        }
        return new Label() {
            public URI getContextUri() {
                return contextUri;
            }
            public Table[] getTables() {
                return tableList.toArray( new Table[ 0 ] );
            }
        };
    }

    /**
     * Constructs a Table object from a label element representing a table.
     *
     * @param  tEl  element Table_Binary, Table_Character or Table_Delimited
     *              from a PDS4 label DOM
     * @return   table object, or null if the element looks wrong
     */
    private Table createTable( Element tEl )
            throws XPathExpressionException, TableFormatException {
        XPath xpath = getXPath();

        /* Find what kind of table it is. */
        final TableType ttype =
            Arrays
           .stream( TableType.values() )
           .filter( t -> t.getTableTag().equals( tEl.getTagName() ) )
           .findFirst()
           .get();
        assert ttype != null;

        /* Find the reference to the file containing the table data. */
        String fileName =
            (String) xpath.evaluate( "../" + xpathEl( "File" ) +
                                      "/" + xpathEl( "file_name" ),
                                      tEl, XPathConstants.STRING );

        /* Extract miscellaneous metadata from table element child nodes. */
        long offset = Long.parseLong( getChildContent( tEl, "offset" ) );
        long nrec = Long.parseLong( getChildContent( tEl, "records" ) );
        String name = getChildContent( tEl, "name" );
        String localIdentifier = getChildContent( tEl, "local_identifier" );
        String description = getChildContent( tEl, "description" );

        /* Find field elements (column definitions). */
        List<Field> fieldList = new ArrayList<>();
        Element recordEl =
            DOMUtils.getChildElementByName( tEl, ttype.getRecordTag() );
        RecordItem[] contents = getRecordItems( recordEl, ttype );

        /* Return a Pds4StarTable instance appropriate for the table element. */
        switch ( ttype ) {

            /* Fixed-length record table, requires record length. */
            case BINARY:
            case CHARACTER:
                String recordLengthTxt =
                    (String)
                    xpath.evaluate( ".//" + xpathEl( "record_length" ), tEl,
                                    XPathConstants.STRING );
                int recordLength = Integer.parseInt( recordLengthTxt );
                return new BaseTable() {
                    public int getRecordLength() {
                        return recordLength;
                    }
                    public String getFileName() {
                        return fileName;
                    }
                    public TableType getTableType() {
                        return ttype;
                    }
                    public long getOffset() {
                        return offset;
                    }
                    public long getRecordCount() {
                        return nrec;
                    }
                    public String getName() {
                        return name;
                    }
                    public String getLocalIdentifier() {
                        return localIdentifier;
                    }
                    public String getDescription() {
                        return description;
                    }
                    public RecordItem[] getContents() {
                        return contents;
                    }
                };

            /* Delimited table, requires field delimiter character. */
            case DELIMITED:
                String fieldDelimTxt =
                    getChildContent( tEl, "field_delimiter" );
                char fieldDelim = getFieldDelimiterChar( fieldDelimTxt );
                return new DelimitedTable() {
                    public char getFieldDelimiter() {
                        return fieldDelim;
                    }
                    public String getFileName() {
                        return fileName;
                    }
                    public TableType getTableType() {
                        return ttype;
                    }
                    public long getOffset() {
                        return offset;
                    }
                    public long getRecordCount() {
                        return nrec;
                    }
                    public String getName() {
                        return name;
                    }
                    public String getLocalIdentifier() {
                        return localIdentifier;
                    }
                    public String getDescription() {
                        return description;
                    }
                    public RecordItem[] getContents() {
                        return contents;
                    }
                };
        }
        assert false;
        return null;
    }

    /**
     * Constructs a Field object from a label element representing a field.
     *
     * @param   fEl   element Field_Binary, Field_Character or Field_Delimited
     * @return  field object
     */
    private Field createField( Element fEl ) {

        /* Extract basic metadata. */
        String name = getChildContent( fEl, "name" );
        String unit = getChildContent( fEl, "unit" );
        String description = getChildContent( fEl, "description" );
        String dataType = getChildContent( fEl, "data_type" );

        /* Get a field decoding type. */
        FieldType ftype = FieldType.getFieldType( dataType );

        /* Location and Length are only present for Field_Binary and
         * Field_Character.  Use dummy values for Field_Delimited. */
        String locationTxt = getChildContent( fEl, "field_location" );
        int location = locationTxt == null || locationTxt.trim().length() == 0
                     ? -1
                     : Integer.parseInt( locationTxt );
        String lengTxt = getChildContent( fEl, "field_length" );
        int length = lengTxt == null || lengTxt.trim().length() == 0
                   ? -1
                   : Integer.parseInt( lengTxt );

        /* Get special constant values. */
        Element specialEl =
            DOMUtils.getChildElementByName( fEl, "Special_Constants" );
        String[] blankConstants = getBlankConstants( specialEl );

        /* Return a Field object based on the information retrieved. */
        return new Field() {
            public String getName() {
                return name;
            }
            public FieldType getFieldType() {
                return ftype;
            }
            public int getFieldLocation() {
                return location;
            }
            public int getFieldLength() {
                return length;
            }
            public String getUnit() {
                return unit;
            }
            public String getDescription() {
                return description;
            }
            public String[] getBlankConstants() {
                return blankConstants;
            }
        };
    }

    /**
     * Constructs a Group object from a label element representing a group.
     *
     * @param  gEl  element Group_Field_Binary, Group_Field_Character or
     *              Group_Field_Delimited
     * @param  ttype  table type in which group is found
     */
    private Group createGroup( Element gEl, TableType ttype ) {
        assert gEl.getTagName().equals( ttype.getGroupTag() );

        /* Extract basic metadata. */
        String name = getChildContent( gEl, "name" );
        String description = getChildContent( gEl, "description" );
        String repetitionsTxt = getChildContent( gEl, "repetitions" );
        int repetitions = Integer.parseInt( repetitionsTxt );

        /* Location and Length are only present for Group_Binary and
         * Group_Character.  Use dummy values for Group_Delimited. */
        String locationTxt = getChildContent( gEl, "group_location" );
        int location = locationTxt == null || locationTxt.trim().length() == 0
                     ? -1
                     : Integer.parseInt( locationTxt );
        String lengTxt = getChildContent( gEl, "group_length" );
        int length = lengTxt == null || lengTxt.trim().length() == 0
                   ? -1
                   : Integer.parseInt( lengTxt );

        /* Read child fields and groups. */
        RecordItem[] contents = getRecordItems( gEl, ttype );
        return new Group() {
            public int getRepetitions() {
                return repetitions;
            }
            public String getName() {
                return name;
            }
            public String getDescription() {
                return description;
            }
            public int getGroupLocation() {
                return location;
            }
            public int getGroupLength() {
                return length;
            }
            public RecordItem[] getContents() {
                return contents;
            }
        };
    }

    /**
     * Returns the Field and Group contents of a given element
     * in a single array.
     *
     * @param  containerEl  element containing fields and groups as children
     * @param  ttype  table type
     * @return   array of Fields and Groups
     */
    private RecordItem[] getRecordItems( Element containerEl,
                                         TableType ttype ) {
        List<RecordItem> items = new ArrayList<>();
        for ( Node child = containerEl.getFirstChild(); child != null;
              child = child.getNextSibling() ) {
            if ( child instanceof Element ) {
                Element el = (Element) child;
                String tagName = el.getTagName();
                if ( ttype.getFieldTag().equals( tagName ) ) {
                    items.add( createField( el ) );
                }
                else if ( ttype.getGroupTag().equals( tagName ) ) {
                    items.add( createGroup( el, ttype ) );
                }
            }
        }
        return items.toArray( new RecordItem[ 0 ] );
    }

    /**
     * Returns an XPath expression to locate suitable Table elements within
     * a PDS4 label document.  The located elements are Table_Binary,
     * Table_Character or Table_Delimited elements, possibly restricted
     * to those within File_Area_Observational elements.
     *
     * @return  table-location XPath returning a NODESET
     */
    private String createTablesXpath() {
        StringBuffer sbuf = new StringBuffer();
        for ( TableType ttype : TableType.values() ) {
            String elName = ttype.getTableTag();
            if ( sbuf.length() > 0 ) {
                sbuf.append( " | " );
            }
            sbuf.append( "//" );
            if ( observationalOnly_ ) {
                sbuf.append( xpathEl( "File_Area_Observational" ) )
                    .append( "/" );
            }
            sbuf.append( xpathEl( elName ) );
        }
        return sbuf.toString();
    }

    /**
     * Returns an array of strings representing values which are to be
     * mapped to null data values on read.
     *
     * @param  specialEl  PDS4 Special_Constants element
     * @return  non-null array of value representations to map to blank values
     */
    private String[] getBlankConstants( Element specialEl ) {
        if ( specialEl == null ) {
            return new String[ 0 ];
        }
        else {
            List<String> list = new ArrayList<>();
            for ( Node child = specialEl.getFirstChild(); child != null;
                  child = child.getNextSibling() ) {
                if ( child instanceof Element ) {
                    Element el = (Element) child;
                    if ( blankSpecials_.contains( el.getTagName() ) ) {
                        String txt = DOMUtils.getTextContent( el );
                        if ( txt != null && txt.trim().length() > 0 ) {
                            list.add( txt.trim() );
                        }
                    }
                }
            }
            return list.toArray( new String[ 0 ] );
        }
    }

    /**
     * Creates an XPath instance.
     *
     * @return  xpath
     */
    private static XPath getXPath() {
        return XPathFactory.newInstance().newXPath();
    }

    /**
     * Utility method that returns the text content of the first child
     * of a given element with a given tag name.
     *
     * @param  el  context element
     * @param   childName  tag name of required child element
     * @return   text content of child, or null if not present
     */
    private static String getChildContent( Element el, String childName ) {
        Element child = DOMUtils.getChildElementByName( el, childName );
        return child == null ? null : DOMUtils.getTextContent( child );
    }

    /**
     * Returns an element of an XPath expression that selects for a given
     * element tag name.
     *
     * <p>This method currently returns an expression that selects using
     * the <code>local-name()</code> function rather than matching on the
     * element name in the usual way.  This is partly because I can't work
     * out a robust way of passing the relevant namespace information to
     * the XPath processor, and partly so that if the namespace changes
     * (or appears in a label with the wrong value) this is still going
     * to work.
     * For the record, I believe that the expected namespace is
     * "http://pds.nasa.gov/pds4/pds/v1".
     *
     * @param  elName  element tag name
     * @return  XPath expression segment
     */
    private static String xpathEl( String elName ) {
        return "*[local-name()='" + elName + "']";
    }

    /**
     * Maps the content of the PDS4 label record_delimiter element
     * to a character value.
     *
     * @param  delimTxt  text designation for delimiter
     * @return  actual delimiter
     */
    private static char getFieldDelimiterChar( String delimTxt )
            throws TableFormatException {
        if ( "comma".equalsIgnoreCase( delimTxt ) ) {
            return (char) 0x2c;
        }
        else if ( "horizontal tab".equalsIgnoreCase( delimTxt ) ) {
            return (char) 0x09;
        }
        else if ( "semicolon".equalsIgnoreCase( delimTxt ) ) {
            return (char) 0x3b;
        }
        else if ( "vertical bar".equalsIgnoreCase( delimTxt ) ) {
            return (char) 0x7c;
        }
        else {
            throw new TableFormatException( "Unknown record delimiter \""
                                          + delimTxt + "\"" );
        }
    }
}
