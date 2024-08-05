package uk.ac.starlink.vo;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import uk.ac.starlink.auth.AuthManager;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.util.URLUtils;

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
    private int iSchema_;
    private int iTable_;
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
     * @param    includeNaked  if false, only the schemas actually encountered
     *                         are returned; if true, then any naked tables
     *                         will be included in a dummy schema in the result
     * @return   fully populated table metadata
     */
    public SchemaMeta[] getSchemas( boolean includeNaked ) {
        if ( schemas_ == null ||
             ! includeNaked ||
             nakedTables_ == null ||
             nakedTables_.length == 0 ) {
            return schemas_;
        }
        else {
            List<SchemaMeta> list =
                new ArrayList<SchemaMeta>( schemas_.length + 1 );
            list.addAll( Arrays.asList( schemas_ ) );
            int nNaked = nakedTables_.length;
            logger_.warning( "Using " + nNaked
                           + " tables declared outside of any schema" );
            SchemaMeta dummySchema =
                SchemaMeta.createDummySchema( "<no_schema>" );
            dummySchema.setTables( nakedTables_ );
            list.add( dummySchema );
            return list.toArray( new SchemaMeta[ 0 ] );
        }
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
            schema_.index_ = Integer.valueOf( ++iSchema_ );
            tableList_ = new ArrayList<TableMeta>();
        }
        else if ( "table".equals( tname ) ) {
            table_ = new TableMeta();
            table_.index_ = Integer.valueOf( ++iTable_ );
            String type = atts.getValue( "", "type" );
            if ( type != null ) {
                table_.type_ = type;
            }
            table_.extras_.putAll( getAttMap( atts, new String[] { "type" } ) );
            columnList_ = new ArrayList<ColumnMeta>();
            foreignList_ = new ArrayList<ForeignMeta>();
        }
        else if ( "column".equals( tname ) ) {
            column_ = new ColumnMeta();
            column_.extras_.putAll( getAttMap( atts, new String[] { "std" } ) );
            flagList_ = new ArrayList<String>();
        }
        else if ( "dataType".equals( tname ) ) {
            String xtype = atts.getValue( "", "extendedType" );
            String arraysize = atts.getValue( "", "arraysize" );
            String xschema = atts.getValue( "", "extendedSchema" );
            String delim = atts.getValue( "", "delim" );
            column_.xtype_ = xtype;
            column_.arraysize_ = arraysize;
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
            ColumnMeta[] cols = columnList_.toArray( new ColumnMeta[ 0 ] );
            retypeExtras( cols );
            table_.setColumns( cols );
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
            else if ( "nrows".equals( tname ) ) {
                table_.nrows_ = txt;
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
        return handler.getSchemas( true );
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
        for ( SchemaMeta schema : handler.getSchemas( false ) ) {
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
        URLConnection conn = AuthManager.getInstance().connect( url, coding );
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
     * Returns a map containing attribute name/value pairs.
     * Any attribute whose name is present in a supplied list is excluded
     * from the result.
     * Output map keys strip any namespace prefix (up to a colon).
     *
     * @param  atts  attributes object
     * @param  ignoreNames  list of attribute names to ignore
     * @return   name/value map
     */
    private static Map<String,String> getAttMap( Attributes atts,
                                                 String[] ignoreNames ) {
        Collection<String> ignores =
            new HashSet<String>( Arrays.asList( ignoreNames ) );
        Map<String,String> map = new LinkedHashMap<String,String>();
        int n = atts.getLength();
        for ( int i = 0; i < n; i++ ) {
            String name = atts.getQName( i ).replaceFirst( ".*:", "" );
            if ( ! ignores.contains( name ) ) {
                map.put( name, atts.getValue( i ) );
            }
        }
        return map;
    }

    /**
     * Takes a group of ColumnMeta objects and tries to convert the
     * members of the columns' Extras maps so that they have values
     * which are of a consistent numeric type, rather than just being
     * strings.  It just looks at all the entries under a particular
     * extras key, and sees if it can come up with a numeric type
     * that they can all be converted to.  This isn't essential,
     * but it means that downstream use of the extras items can
     * present them in a way which is more sensitive to their content,
     * for instance allowing them to be sorted numerically in JTables.
     * We have to do it like this, since the extras information is just
     * gathered from attribute information as strings,
     * and there isn't really any other way to work out what type they are
     * supposed to be.
     *
     * @param  cols  list of column metadata objects whose Extras maps
     *               should be made consistent;
     *               extras map values (but not keys) may be rewritten
     *               by this method
     */
    private static void retypeExtras( ColumnMeta[] cols ) {

        /* Assemble a list of possible conversions for each distinct
         * extras map key that appears in any of the columns. */
        Map<String,List<Converter>> convMap =
            new HashMap<String,List<Converter>>();
        List<Converter> allConvs = Arrays.asList( Converter.values() );
        for ( ColumnMeta col : cols ) {
            for ( Map.Entry<String,Object> extra :
                  col.getExtras().entrySet() ) {
                String key = extra.getKey();
                Object value = extra.getValue();
                if ( value instanceof String && ! "".equals( value ) ) {
                    String sval = (String) value;
                    if ( ! convMap.containsKey( key ) ) {
                        convMap.put( key,
                                     new ArrayList<Converter>( allConvs ) );
                    }
                    for ( Iterator<Converter> it =
                              convMap.get( key ).iterator();
                          it.hasNext(); ) {
                        Converter conv = it.next();
                        try {
                            conv.convert( sval );
                        }
                        catch ( NumberFormatException e ) {
                            it.remove();
                        }
                    }
                }
            }
        }

        /* For each extras entry in each column, use the preferred
         * conversion determined in the previous step to rewrite it
         * as a typed value where applicable. */
        for ( ColumnMeta col : cols ) {
            for ( Map.Entry<String,Object> entry :
                  col.getExtras().entrySet() ) {
                Object value = entry.getValue();
                if ( value instanceof String ) {
                    final Object newValue;
                    String sval = (String) value;
                    if ( sval.length() == 0 ) {
                        newValue = null;
                    }
                    else {
                        List<Converter> convs = convMap.get( entry.getKey() );
                        if ( convs != null && convs.size() > 0 ) {
                            newValue = convs.get( 0 ).convert( sval );
                        }
                        else {
                            newValue = value;
                        }
                    }
                    entry.setValue( newValue );
                }
            }
        }
    }

    /**
     * Provides options for converting a string to a typed value.
     * More specific (more desirable) conversions appear earlier in
     * the list.
     */
    private enum Converter {
        LONG() {
            public Object convert( String txt ) {
                return Long.valueOf( txt );
            }
        },
        INTEGER() {
            public Object convert( String txt ) {
                return Integer.valueOf( txt );
            }
        },
        DOUBLE() {
            public Object convert( String txt ) {
                return Double.valueOf( txt );
            }
        },
        STRING() {
            public Object convert( String txt ) {
                return txt;
            }
        };

        /**
         * Converts a text item to a typed value.  The return class is
         * always the same for a given instance of this enum.
         *
         * If the conversion can't be performed, a NumberFormatException
         * is thrown.  That's not very good practice, since throwing an
         * exception is much more expensive than a normal return,
         * but it's somewhat hard work to determine whether one of these
         * conversions will work without actually trying it, and
         * this method will not get called sufficiently often to be
         * a bottleneck.
         *
         * @param   txt   input string
         * @return   converted value
         * @throws NumberFormatException  if the conversion can't be performed
         */
        abstract Object convert( String txt );
    }

    /**
     * Main method to parse a tableset from the command line.
     *
     * @param args  first element is a URL to read from
     */
    public static void main( String[] args ) throws IOException, SAXException {
        java.io.PrintStream out = System.out;
        TableSetSaxHandler tsHandler =
            populateHandler( URLUtils.newURL( args[ 0 ] ), ContentCoding.GZIP );
        for ( SchemaMeta schema : tsHandler.getSchemas( false ) ) {
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
