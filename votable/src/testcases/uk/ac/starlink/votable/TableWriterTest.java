package uk.ac.starlink.votable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import junit.framework.TestCase;
import org.xml.sax.InputSource;
import uk.ac.starlink.table.ArrayColumn;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.Tables;

public class TableWriterTest extends TestCase {

    public TableWriterTest( String name ) {
        super( name );
        Logger.getLogger( "uk.ac.starlink.votable" ).setLevel( Level.SEVERE );
    }

    public void testTableWriter() throws Exception {
        checkTable( createTestTable( 23 ) );
    }

    public void testTablesWriter() throws Exception {
        checkTables( new StarTable[] { createTestTable( 23 ) } );
        checkTables( new StarTable[] { createTestTable( 1 ),
                                       createTestTable( 10 ),
                                       createTestTable( 100 ) } );
        checkTables( new StarTable[ 0 ] );
    }

    public void testOutputConfig() throws Exception {
        StarTable table = new StarTableFactory().makeStarTable( ":loop:10" );
        assertTrue( outputContains( table, "votable", "TABLEDATA" ) );
        assertFalse( outputContains( table, "votable", "BINARY" ) );
        assertTrue( outputContains( table, "votable(dataFormat=BINARY2)",
                                    "BINARY2" ) );
        assertFalse( outputContains( table, "votable", "VOTable/v1.1" ) );
        assertTrue( outputContains( table, "votable(votableVersion=V11)",
                                    "VOTable/v1.1" ) );
        assertTrue( outputContains( table,
                                    "votable(votableVersion=V14, "
                                          + "writeSchemaLocation=false, "
                                          + ",dataFormat=BINARY)",
                                    "<BINARY>" ) );
    }

    private boolean outputContains( StarTable table, String handlerName,
                                    String txt )
            throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StarTableOutput sto = new StarTableOutput();
        StarTableWriter handler = sto.getHandler( handlerName );
        handler.writeStarTable( table, out );
        String ser = new String( out.toByteArray(), "UTF-8" );
        return ser.indexOf( txt ) >= 0;
    }

    public static void checkTable( StarTable table ) throws Exception {
        VOTableWriter[] writers = getAllTableWriters();
        for ( int i = 0; i < writers.length; i++ ) {
            VOTableWriter vowriter = writers[ i ];
            ByteArrayOutputStream ostrm = new ByteArrayOutputStream();
            vowriter.writeStarTable( table, ostrm );
            validate( vowriter.getVotableVersion(), ostrm.toByteArray() );
        }
    }

    public static void checkTables( StarTable[] tables ) throws Exception {
        VOTableWriter[] writers = getAllTableWriters();
        for ( int i = 0; i < writers.length; i++ ) {
            VOTableWriter vowriter = writers[ i ];
            ByteArrayOutputStream ostrm = new ByteArrayOutputStream();
            vowriter.writeStarTables( Tables.arrayTableSequence( tables ),
                                      ostrm );
            validate( vowriter.getVotableVersion(), ostrm.toByteArray() );
        }
    }

    private static void validate( VOTableVersion version, byte[] content )
            throws Exception {
        Schema schema = version.getSchema();
        if ( schema != null ) {
            schema.newValidator()
                  .validate( new SAXSource(
                                 new InputSource(
                                     new ByteArrayInputStream( content ) ) ) );
        }
    }

    private static StarTable createTestTable( int nrow ) {
        return AutoStarTable.getDemoTable( 100 );
    }

    public static VOTableWriter[] getAllTableWriters() {
        List<VOTableWriter> list = new ArrayList<VOTableWriter>();
        for ( VOTableVersion version :
              VOTableVersion.getKnownVersions().values() ) {
            for ( DataFormat format :
                  Arrays.asList( new DataFormat[] {
                      DataFormat.TABLEDATA,
                      DataFormat.BINARY,
                      DataFormat.FITS,
                      DataFormat.BINARY2,
                  } ) ) {
                if ( format != DataFormat.BINARY2 || version.allowBinary2() ) {
                    list.add( new VOTableWriter( format, true, version ) );
                }
            }
        }
        return list.toArray( new VOTableWriter[ 0 ] );
    }
}
