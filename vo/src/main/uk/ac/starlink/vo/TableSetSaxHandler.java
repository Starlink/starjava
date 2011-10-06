package uk.ac.starlink.vo;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

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

    private TableMeta[] tables_;
    private List<TableMeta> tableList_;
    private List<ColumnMeta> columnList_;
    private List<ForeignMeta> foreignList_;
    private List<ForeignMeta.Link> linkList_;
    private List<String> flagList_;
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
     * Returns the array of table metadata objects which have been
     * read by this parser.  Only non-empty following a parse.
     *
     * @return   table descriptions
     */
    public TableMeta[] getTables() {
        return tables_;
    }

    @Override
    public void startDocument() {
        tableList_ = new ArrayList<TableMeta>();
    }

    @Override
    public void endDocument() {
        tables_ = tableList_.toArray( new TableMeta[ 0 ] );
        tableList_ = null;
    }

    @Override
    public void startElement( String uri, String localName, String qName,
                              Attributes atts ) {
        txtbuf_.setLength( 0 );
        String tname = getTagName( uri, localName, qName );
        if ( "table".equals( tname ) ) {
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
            table_.columns_ = columnList_.toArray( new ColumnMeta[ 0 ] );
            columnList_ = null;
            table_.foreignKeys_ = foreignList_.toArray( new ForeignMeta[ 0 ] );
            foreignList_ = null;
            if ( tableList_ != null ) {
                tableList_.add( table_ );
            }
            table_ = null;
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
     * URL and extract the TableMeta objects from it.
     *
     * @param  url  containing a TableSet document or similar
     */
    public static TableMeta[] readTableSet( URL url )
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
        if ( conn instanceof HttpURLConnection ) {
            HttpURLConnection hconn = (HttpURLConnection) conn;
            int code = hconn.getResponseCode();
            if ( code != HttpURLConnection.HTTP_OK ) {
                throw new IOException( "Table resource access failure (" + code 
                                     + " " + hconn.getResponseMessage() + ")" );
            }
        }
        InputStream in = new BufferedInputStream( conn.getInputStream() );
        try {
            parser.parse( in, tsHandler );
            return tsHandler.getTables();
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
        TableMeta[] tables = readTableSet( new URL( args[ 0 ] ) );
        for ( int it = 0; it < tables.length; it++ ) {
            TableMeta table = tables[ it ];
            out.println( table.getName() );
            ColumnMeta[] cols = table.getColumns();
            for ( int ic = 0; ic < cols.length; ic++ ) {
                ColumnMeta col = cols[ ic ];
                out.println( "    " + col.getName() );
            }
        }
    }
}
