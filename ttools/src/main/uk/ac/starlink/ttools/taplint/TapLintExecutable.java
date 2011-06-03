package uk.ac.starlink.ttools.taplint;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.util.CountMap;
import uk.ac.starlink.vo.ColumnMeta;
import uk.ac.starlink.vo.ForeignMeta;
import uk.ac.starlink.vo.TableMeta;
import uk.ac.starlink.vo.TapQuery;

/**
 * Performs TAP validation.
 *
 * @author   Mark Taylor
 * @since    3 Jun 2011
 */
public class TapLintExecutable implements Executable {

    private final URL serviceUrl_;
    private final Reporter reporter_;
    private static final String XSDS = "http://www.ivoa.net/xml";
    private static final Map<URL,Schema> schemaMap_ = new HashMap<URL,Schema>();
    private static final String[] KNOWN_COL_FLAGS =
        new String[] { "indexed", "primary", "nullable" };
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.taplint" );

    public TapLintExecutable( URL serviceUrl, Reporter reporter ) {
        serviceUrl_ = serviceUrl;
        reporter_ = reporter;
    }

    public void execute() throws IOException {
        validateXml( new URL( XSDS + "/VODataService/VODataService-v1.1.xsd" ),
                     new URL( serviceUrl_ + "/tables" ),
                     "TMV", "tables metadata" );
        reporter_.startSection( "TMC", "Checking content of tables metadata" );
        try {
            TableMeta[] tmetas = TapQuery.readTableMetadata( serviceUrl_ );
            checkTables( tmetas );
        }
        catch ( SAXException e ) {
            reporter_.report( Reporter.Type.ERROR, "FAIL",
                              "Can't parse table metadata well enough "
                            + "to check it" );
        }
        reporter_.endSection();
    }

    private void checkTables( TableMeta[] tmetas ) {
        if ( tmetas == null ) {
            reporter_.report( Reporter.Type.WARNING, "GONE",
                              "Table metadata absent" );
        }
        int nTable = tmetas.length;
        int nCol = 0;
        Map<String,TableMeta> tableMap = createNameMap( "table", 'T', tmetas );
        Map<String,Map<String,ColumnMeta>> colsMap =
            new HashMap<String,Map<String,ColumnMeta>>();
        for ( String tname : tableMap.keySet() ) {
            TableMeta tmeta = tableMap.get( tname );
            ColumnMeta[] cols = tmeta.getColumns();
            nCol += cols.length;
            Map<String,ColumnMeta> cmap = createNameMap( "column", 'C', cols );
            colsMap.put( tname, cmap );
        }
        int nForeign = 0;
        int nIndex = 0;
        CountMap<String> flagMap = new CountMap<String>();
        for ( String tname : tableMap.keySet() ) {
            TableMeta tmeta = tableMap.get( tname );
            Map<String,ColumnMeta> cmap = colsMap.get( tname );
            for ( String cname : cmap.keySet() ) {
                ColumnMeta col = cmap.get( cname );
                if ( col.isIndexed() ) {
                    nIndex++;
                }
                String[] flags = col.getFlags();
                for ( int ig = 0; ig < flags.length; ig++ ) {
                    flagMap.addItem( flags[ ig ] );
                }
            }
            ForeignMeta[] fkeys = tmeta.getForeignKeys();
            nForeign += fkeys.length;
            for ( int ik = 0; ik < fkeys.length; ik++ ) {
                ForeignMeta fkey = fkeys[ ik ];
                String targetTableName = fkey.getTargetTable();
                TableMeta targetTable = tableMap.get( targetTableName );
                if ( targetTable == null ) {
                    reporter_.report( Reporter.Type.ERROR, "FKNT",
                                      "Foreign Key using non-existent "
                                    + "target table "
                                    + '"' + fkey.getTargetTable() + '"' );
                }
                else {
                    Map<String,ColumnMeta> targetCmap =
                        colsMap.get( targetTableName );
                    ForeignMeta.Link[] links = fkey.getLinks();
                    for ( int il = 0; il < links.length; il++ ) {
                        ForeignMeta.Link link = links[ il ];
                        ColumnMeta fromCol = cmap.get( link.getFrom() );
                        ColumnMeta toCol = targetCmap.get( link.getTarget() );
                        if ( fromCol == null || toCol == null ) {
                            reporter_.report( Reporter.Type.ERROR, "FKLK",
                                              "Broken link " + link
                                            + " in foreign key " + fkey );
                        }
                    }
                }
            }
        }
        Collection<String> otherFlagList =
            new ArrayList<String>( flagMap.keySet() );
        otherFlagList.removeAll( Arrays.asList( KNOWN_COL_FLAGS ) );
        String[] otherFlags = otherFlagList.toArray( new String[ 0 ] );
        reporter_.report( Reporter.Type.SUMMARY, "SUMM",
                          "Tables: " + nTable + ", "
                        + "Columns: " + nCol + ", "
                        + "Foreign Keys: " + nForeign );
        reporter_.report( Reporter.Type.SUMMARY, "FLGS",
                          "Standard column flags: "
                        + summariseCounts( flagMap, KNOWN_COL_FLAGS ) );
        reporter_.report( Reporter.Type.SUMMARY, "FLGO",
                          "Other column flags: "
                        + summariseCounts( flagMap, otherFlags ) );
    }

    private String summariseCounts( CountMap<String> countMap, String[] keys ) {
        if ( keys.length == 0 ) {
            return "none";
        }
        StringBuffer sbuf = new StringBuffer();
        for ( int ik = 0; ik < keys.length; ik++ ) {
            String key = keys[ ik ];
            if ( ik > 0 ) {
                sbuf.append( ", " );
            }
            sbuf.append( key )
                .append( ": " )
                .append( countMap.getCount( key ) );
        }
        return sbuf.toString();
    }

    private void validateXml( URL schemaUrl, URL docUrl, String scode,
                              String description ) throws IOException {
        reporter_.startSection( scode, "Validating " + description
                                    + " against schema" );
        final Validator val;
        try {
            val = getSchema( schemaUrl ).newValidator();
        }
        catch ( SAXException e ) {
            throw (IOException) new IOException( "Error parsing schema" )
                               .initCause( e );
        }
        ReporterErrorHandler errHandler = new ReporterErrorHandler( reporter_ );
        val.setErrorHandler( errHandler );
        try {
            val.validate( new SAXSource( new InputSource( docUrl
                                                         .toString() ) ) );
        }
        catch ( SAXException e ) {
            if ( errHandler.getFatalCount() == 0 ) {
                throw (IOException) new IOException( "Unexpected parse error" )
                                   .initCause( e );
            }
        }
        reporter_.summariseUnreportedMessages( scode );
        reporter_.report( Reporter.Type.SUMMARY, "VALI",
                          errHandler.getSummary() );
        reporter_.endSection();
    }

    public static Schema getSchema( URL url ) throws SAXException {
        if ( ! schemaMap_.containsKey( url ) ) {
            logger_.info( "Compiling schema " + url );
            Schema schema = SchemaFactory
                           .newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI )
                           .newSchema( url );
            schemaMap_.put( url, schema );
        }
        return schemaMap_.get( url );
    }

    private <V> Map<String,V> createNameMap( String dName, char dChr,
                                             V[] values ) {
        Map<String,V> map = new LinkedHashMap<String,V>();
        for ( int iv = 0; iv < values.length; iv++ ) {
            V value = values[ iv ];
            String name = value.toString();
            if ( name == null || name.trim().length() == 0 ) {
                reporter_.report( Reporter.Type.ERROR, "" + dChr + "BLA",
                                  "Blank name for " + dName + " #" + iv );
            }
            if ( ! map.containsKey( name ) ) {
                map.put( name, value );
            }
            else {
                reporter_.report( Reporter.Type.WARNING, "" + dChr + "DUP",
                                  "Duplicate " + dName + " \"" + name + "\"" );
            }
        }
        return map;
    }

    public static void main( String[] args ) throws Exception {
      //new TapLintExecutable( null, System.out, true )
      //   .validateXml( new URL( args[ 0 ] ), new URL( args[ 1 ] ) );
        String surl = args.length > 0
                    ? args[ 0 ]
                    : "http://dc.zah.uni-heidelberg.de/__system__/tap/run/tap";
        Logger.getLogger( "uk.ac.starlink" ).setLevel( Level.WARNING );
        new TapLintExecutable( new URL( surl ), new Reporter( System.out, 4 ) )
           .execute();
    }
}
