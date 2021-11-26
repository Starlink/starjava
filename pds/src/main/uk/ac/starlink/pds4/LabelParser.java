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
import uk.ac.starlink.util.URLUtils;

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

    /**
     * Default parser.
     */
    public LabelParser() {
        this( true );
    }

    /**
     * Custom constructor.
     */
    public LabelParser( boolean observationalOnly ) {
        observationalOnly_ = observationalOnly;
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
        URL parent;
        try {
            URI labelUri = url.toURI();
            parent = labelUri.getPath().endsWith( "/" )
                   ? labelUri.resolve( ".." ).toURL()
                   : labelUri.resolve( "." ).toURL();
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
        URL parentUrl =
            URLUtils.makeFileURL( file.getAbsoluteFile().getParentFile() );
        return parseLabel( in, parentUrl );
    }

    /**
     * Parses the label file from a given InputStream to create a Label object.
     *
     * @param  in  input stream, closed unconditionally on exit
     * @param  contextUrl   context URL
     * @return   parsed label object
     */
    public Label parseLabel( InputStream in, URL contextUrl )
            throws IOException {
        try {
            return attemptParseLabel( in, contextUrl );
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
     * @param  contextUrl   context URL
     * @return   parsed label object
     */
    private Label attemptParseLabel( InputStream in, URL contextUrl )
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
            public URL getContextUrl() {
                return contextUrl;
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
        addFields( ttype, recordEl, fieldList, "" );

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
                    public Field[] getFields() {
                        return fieldList.toArray( new Field[ 0 ] );
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
                    public Field[] getFields() {
                        return fieldList.toArray( new Field[ 0 ] );
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
     * @param   nameSuffix   suffix to be applied to field name
     * @return  field object
     */
    private static Field createField( Element fEl, String nameSuffix ) {

        /* Extract basic metadata. */
        String name = getChildContent( fEl, "name" ) + nameSuffix;
        String unit = getChildContent( fEl, "unit" );
        String description = getChildContent( fEl, "description" );
        String dataType = getChildContent( fEl, "data_type" );

        /* Get a field decoding type. */
        FieldType ftype = FieldType.getFieldType( dataType );

        /* Location and Length are only present for Field_Binary and
         * Field_Character.  Use dummy values for Field_Delmited. */
        String locationTxt = getChildContent( fEl, "field_location" );
        int location = locationTxt == null || locationTxt.trim().length() == 0
                     ? -1
                     : Integer.parseInt( locationTxt );
        String lengTxt = getChildContent( fEl, "field_length" );
        int length = lengTxt == null || lengTxt.trim().length() == 0
                   ? -1
                   : Integer.parseInt( lengTxt );

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
        };
    }

    /**
     * Recursively add Field objects found in a given element to a given list.
     * Field_* children of the element are added directly, and Group_*
     * elements are recursively handed back to this routine.
     *
     * @param  ttype  table type
     * @param  parent   element with Field_* or Group_* children
     * @param  fieldList   list into which to accumulate fields
     * @param  suffix   suffix to apply to field names
     */
    private void addFields( TableType ttype, Element parent,
                            List<Field> fieldList, String suffix ) {
        for ( Node child = parent.getFirstChild(); child != null;
              child = child.getNextSibling() ) {
            if ( child instanceof Element ) {
                Element el = (Element) child;
                String tagName = el.getTagName();
                if ( ttype.getFieldTag().equals( tagName ) ) {
                    fieldList.add( createField( el, suffix ) );
                }
                else if ( ttype.getGroupTag().equals( tagName ) ) {
                    int nrep = Integer
                              .parseInt( getChildContent( el, "repetitions" ) );
                    for ( int irep = 0; irep < nrep; irep++ ) {
                        addFields( ttype, el, fieldList,
                                   suffix + "_" + ( irep + 1 ) );
                    }
                }
            }
        }
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
