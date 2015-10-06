package uk.ac.starlink.vo;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import uk.ac.starlink.util.ContentCoding;

/**
 * Parses an XML document which describes Tabular Data as prescribed by
 * the VODataService standard.  It will pick up &lt;table&gt; elements
 * (of type vs:Table), or elements that look like them, to build a picture
 * of the tables and their columns and foreign keys.
 * XML documents of this type are exposed by VOSI and TAP services.
 *
 * <p>The easiest way to make use of this class is via the static method
 * {@link #readTableSet} or the convenience {@link #main} method.
 *
 * @see  <a href="http://www.ivoa.net/Documents/VODataService/"
 *          >IVOA VODataService Recommendation</a>
 */
public class TableSetSaxHandler extends DefaultHandler {

    private SchemaMeta[] schemas_;
    private TableMeta[] nakedTables_;
    private List<SchemaMeta> schemaList_;
    private List<TableMeta> tableList_;
    private List<ColumnMeta> columnList_;
    private List<ForeignMeta> foreignList_;
    private List<ForeignMeta.Link> linkList_;
    private List<TableMeta> nakedTableList_;
    private List<String> flagList_;
    private SchemaMeta schema_;
    private TableMeta table_;
    private ColumnMeta column_;
    private ForeignMeta foreign_;
    private ForeignMeta.Link link_;
    private final StringBuffer txtbuf_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     */
    public TableSetSaxHandler() {
       txtbuf_ = new StringBuffer();
    }

    /** 
     * Returns the array of schema metadata objects which have been
     * read by this parser.  Only non-empty following a parse.
     *
     * @return   fully populated table metadata
     */
    public SchemaMeta[] getSchemas() {
        return schemas_;
    }

    /**
     * Returns the array of table metadata objects which were found
     * outside of any schema.  Only non-empty following a parse.
     *
     * @return  table metadata
     */
    public TableMeta[] getNakedTables() {
        return nakedTables_;
    }

    @Override
    public void startDocument() {
        schemaList_ = new ArrayList<SchemaMeta>();
        nakedTableList_ = new ArrayList<TableMeta>();
        tableList_ = nakedTableList_;
    }

    @Override
    public void endDocument() {
        schemas_ = schemaList_.toArray( new SchemaMeta[ 0 ] );
        schemaList_ = null;
        nakedTables_ = nakedTableList_.toArray( new TableMeta[ 0 ] );
        nakedTableList_ = null;
    }

    @Override
    public void startElement( String uri, String localName, String qName,
                              Attributes atts ) {
        txtbuf_.setLength( 0 );
        String tname = getTagName( uri, localName, qName );
        if ( "schema".equals( tname ) ) {
            schema_ = new SchemaMeta();
            tableList_ = new ArrayList<TableMeta>();
        }
        else if ( "table".equals( tname ) ) {
            table_ = new TableMeta();
            String type = atts.getValue( "", "type" );
            if ( type != null ) {
                table_.type_ = type;
            }
            columnList_ = new ArrayList<ColumnMeta>();
            foreignList_ = new ArrayList<ForeignMeta>();
        }
        else if ( "column".equals( tname ) ) {
            column_ = new ColumnMeta();
            flagList_ = new ArrayList<String>();
        }
        else if ( "foreignKey".equals( tname ) ) {
            foreign_ = new ForeignMeta();
            linkList_ = new ArrayList<ForeignMeta.Link>();
        }
        else if ( "fkColumn".equals( tname ) ) {
            link_ = new ForeignMeta.Link();
        }
    }

    @Override
    public void endElement( String uri, String localName, String qName ) {
        String txt = txtbuf_.toString();
        txtbuf_.setLength( 0 );
        String tname = getTagName( uri, localName, qName );
        if ( "fkColumn".equals( tname ) ) {
            assert link_ != null;
            if ( link_.from_ != null && link_.target_ != null ) {
                linkList_.add( link_ );
            }
            else {
                logger_.info( "fkColumn lacks from and/or target column"
                            + " - ignored" );
            }
            link_ = null;
        }
        else if ( "foreignKey".equals( tname ) ) {
            assert foreign_ != null;
            foreign_.links_ = linkList_.toArray( new ForeignMeta.Link[ 0 ] );
            linkList_ = null;
            if ( foreignList_ != null ) {
                foreignList_.add( foreign_ );
            }
            foreign_ = null;
        }
        else if ( "column".equals( tname ) ) {
            assert column_ != null;
            column_.flags_ = flagList_.toArray( new String[ 0 ] );
            flagList_ = null;
            if ( columnList_ != null ) {
                columnList_.add( column_ );
            }
            column_ = null;
        }
        else if ( "table".equals( tname ) ) {
            assert table_ != null;
            table_.setColumns( columnList_.toArray( new ColumnMeta[ 0 ] ) );
            columnList_ = null;
            table_.setForeignKeys( foreignList_
                                  .toArray( new ForeignMeta[ 0 ] ) );
            foreignList_ = null;
            if ( tableList_ != null ) {
                tableList_.add( table_ );
            }
            table_ = null;
        }
        else if ( "schema".equals( tname ) ) {
            assert schema_ != null;
            schema_.setTables( tableList_.toArray( new TableMeta[ 0 ] ) );
            tableList_ = nakedTableList_;
            if ( schemaList_ != null ) {
                schemaList_.add( schema_ );
            }
            schema_ = null;
        }
        else if ( link_ != null ) {
            if ( "fromColumn".equals( tname ) ) {
                link_.from_ = txt;
            }
            else if ( "targetColumn".equals( tname ) ) {
                link_.target_ = txt;
            }
        }
        else if ( foreign_ != null ) {
            if ( "targetTable".equals( tname ) ) {
                foreign_.targetTable_ = txt;
            }
            else if ( "description".equals( tname ) ) {
                foreign_.description_ = txt;
            }
            else if ( "utype".equals( tname ) ) {
                foreign_.utype_ = txt;
            }
        }
        else if ( column_ != null ) {
            if ( "name".equals( tname ) ) {
                column_.name_ = txt;
            }
            else if ( "description".equals( tname ) ) {
                column_.description_ = txt;
            }
            else if ( "unit".equals( tname ) ) {
                column_.unit_ = txt;
            }
            else if ( "ucd".equals( tname ) ) {
                column_.ucd_ = txt;
            }
            else if ( "utype".equals( tname ) ) {
                column_.utype_ = txt;
            }
            else if ( "dataType".equals( tname ) ) {
                column_.dataType_ = txt;  // also has attributes
            }
            else if ( "flag".equals( tname ) ) {
                flagList_.add( txt );
            }
        }
        else if ( table_ != null ) {
            if ( "name".equals( tname ) ) {
                table_.name_ = txt;
            }
            else if ( "title".equals( tname ) ) {
                table_.title_ = txt;
            }
            else if ( "description".equals( tname ) ) {
                table_.description_ = txt;
            }
            else if ( "utype".equals( tname ) ) {
                table_.utype_ = txt;
            }
        }
        else if ( schema_ != null ) {
            if ( "name".equals( tname ) ) {
                schema_.name_ = txt;
            }
            else if ( "title".equals( tname ) ) {
                schema_.title_ = txt;
            }
            else if ( "description".equals( tname ) ) {
                schema_.description_ = txt;
            }
            else if ( "utype".equals( tname ) ) {
                schema_.utype_ = txt;
            }
        }
    }

    @Override
    public void characters( char[] ch, int start, int length ) {
        txtbuf_.append( ch, start, length );
    }

    @Override
    public void ignorableWhitespace( char[] ch, int start, int length ) {
    }

    @Override
    public void startPrefixMapping( String prefix, String uri ) {
    }

    @Override
    public void endPrefixMapping( String prefix ) {
    }

    @Override
    public void processingInstruction( String target, String data ) {
    }

    @Override
    public void skippedEntity( String name ) {
    }

    @Override
    public void setDocumentLocator( Locator locator ) {
    }

    /**
     * Utility method to get the unadorned tag name of an element
     * without worrying about namespaces.
     *
     * @param   uri  namespace URI
     * @param   localName  local name
     * @param   qName  qualified name, if available
     */
    private String getTagName( String uri, String localName, String qName ) {
        return localName != null && localName.length() > 0
             ? localName
             : qName.replaceFirst( ".*:", "" );
    }

    /**
     * Uses an instance of this class to read an XML document from a given
     * URL and extract the SchemaMeta objects it represents.
     *
     * @param  url  containing a TableSet document or similar
     * @param  coding  configures HTTP content-coding
     * @return   array of schema metadata objects giving table metadata
     */
    public static SchemaMeta[] readTableSet( URL url, ContentCoding coding )
            throws IOException, SAXException {
        TableSetSaxHandler handler = populateHandler( url, coding );
        List<SchemaMeta> schemaList = new ArrayList<SchemaMeta>();
        schemaList.addAll( Arrays.asList( handler.getSchemas() ) );
        TableMeta[] nakedTables = handler.getNakedTables();
        int nNaked = nakedTables.length;
        if ( nNaked > 0 ) {
            logger_.warning( "Using " + nNaked
                           + " tables declared outside of any schema" );
            SchemaMeta dummySchema = new SchemaMeta();
            dummySchema.name_ = "<no_schema>";
            dummySchema.setTables( nakedTables );
            schemaList.add( dummySchema );
        }
        return schemaList.toArray( new SchemaMeta[ 0 ] );
    }

    /**
     * Uses an instance of this class to read an XML document from a given
     * URL and extracts a flat list of all the TableMeta objects it
     * represents.
     * This includes all the tables in schemas, as well as any outside
     * any <code>&lt;schema&gt;</code> element.
     *
     * @param  url  containing a TableSet document or similar
     * @param  coding  configures HTTP content-coding
     * @return  flat list of all tables
     */
    public static TableMeta[] readTables( URL url, ContentCoding coding )
            throws IOException, SAXException {
        TableSetSaxHandler handler = populateHandler( url, coding );
        List<TableMeta> tlist = new ArrayList<TableMeta>();
        for ( SchemaMeta schema : handler.getSchemas() ) {
            tlist.addAll( Arrays.asList( schema.getTables() ) );
        }
        tlist.addAll( Arrays.asList( handler.getNakedTables() ) );
        return tlist.toArray( new TableMeta[ 0 ] );
    }
    
    /**
     * Uses an instance of this class to parse the document at a given URL.
     *
     * @param  url  containing a TableSet document or similar
     * @param  coding  configures HTTP content-coding
     * @return   handler containing located items
     */
    public static TableSetSaxHandler populateHandler( URL url,
                                                      ContentCoding coding )
            throws IOException, SAXException {
        SAXParserFactory spfact = SAXParserFactory.newInstance();
        SAXParser parser;
        try {
            spfact.setNamespaceAware( false );
            spfact.setValidating( false );
            parser = spfact.newSAXParser();
        }
        catch ( ParserConfigurationException e ) {
            throw (IOException) new IOException( "SAX trouble" ).initCause( e );
        }
        catch ( SAXException e ) {
            throw (IOException) new IOException( "SAX trouble" ).initCause( e );
        }
        TableSetSaxHandler tsHandler = new TableSetSaxHandler();
        URLConnection conn = url.openConnection();
        coding.prepareRequest( conn );
        if ( conn instanceof HttpURLConnection ) {
            HttpURLConnection hconn = (HttpURLConnection) conn;
            int code = hconn.getResponseCode();
            if ( code != HttpURLConnection.HTTP_OK ) {
                throw new IOException( "Table resource access failure (" + code 
                                     + " " + hconn.getResponseMessage() + ")" );
            }
        }
        InputStream in =
            new BufferedInputStream( coding.getInputStream( conn ) );
        try {
            parser.parse( in, tsHandler );
            return tsHandler;
        }
        finally {
            in.close();
        }
    }

    /**
     * Main method to parse a tableset from the command line.
     *
     * @param args  first element is a URL to read from
     */
    public static void main( String[] args ) throws IOException, SAXException {
        java.io.PrintStream out = System.out;
        TableSetSaxHandler tsHandler =
            populateHandler( new URL( args[ 0 ] ), ContentCoding.GZIP );
        for ( SchemaMeta schema : tsHandler.getSchemas() ) {
            out.println( schema.getName() );
            for ( TableMeta table : schema.getTables() ) {
                out.println( "    " + table.getName() );
                for ( ColumnMeta col : table.getColumns() ) {
                    out.println( "        " + col.getName() );
                }
            }
        }
        TableMeta[] nakedTables = tsHandler.getNakedTables();
        if ( nakedTables.length > 0 ) {
            out.println( "No schema: " );
            for ( TableMeta table : nakedTables ) {
                out.println( "    " + table.getName() );
                for ( ColumnMeta col : table.getColumns() ) {
                    out.println( "        " + col.getName() );
                }
            }
        }
    }
}
