package uk.ac.starlink.votable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Validator;
import junit.framework.TestCase;
import org.xml.sax.InputSource;
import uk.ac.starlink.table.ArrayColumn;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.StarTable;
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

    public static void checkTable( StarTable table ) throws Exception {
        StarTableWriter[] writers = VOTableWriter.getStarTableWriters();
        Validator validator = VOTableSchema.getSchema( "1.1" ).newValidator();
        for ( int i = 0; i < writers.length; i++ ) {
            VOTableWriter vowriter = (VOTableWriter) writers[ i ];
            ByteArrayOutputStream ostrm = new ByteArrayOutputStream();
            vowriter.writeStarTable( table, ostrm );
            validator.validate( new SAXSource(
                                    new InputSource(
                                        new ByteArrayInputStream(
                                            ostrm.toByteArray() ) ) ) );
        }
    }

    public static void checkTables( StarTable[] tables ) throws Exception {
        StarTableWriter[] writers = VOTableWriter.getStarTableWriters();
        Validator validator = VOTableSchema.getSchema( "1.1" ).newValidator();
        for ( int i = 0; i < writers.length; i++ ) {
            VOTableWriter vowriter = (VOTableWriter) writers[ i ];
            ByteArrayOutputStream ostrm = new ByteArrayOutputStream();
            vowriter.writeStarTables( Tables.arrayTableSequence( tables ),
                                      ostrm );
            validator.validate( new SAXSource(
                                    new InputSource(
                                        new ByteArrayInputStream(
                                            ostrm.toByteArray() ) ) ) );
        }
    }

    public static StarTable createTestTable( int nrow ) {
        int[] cd1 = new int[ nrow ];
        float[] cd2 = new float[ nrow ];
        String[] cd3 = new String[ nrow ];
        for ( int i = 0; i < nrow; i++ ) {
            int j = i + 1;
            cd1[ i ] = j;
            cd2[ i ] = (float) j;
            cd3[ i ] = "row " + j;
        }
        ColumnInfo ci1 = new ColumnInfo( "ints", Integer.class, null );
        ColumnInfo ci2 = new ColumnInfo( "floats", Float.class, null );
        ColumnInfo ci3 = new ColumnInfo( "strings", String.class, null );
        ColumnStarTable t0 = ColumnStarTable.makeTableWithRows( nrow );
        t0.addColumn( ArrayColumn.makeColumn( ci1, cd1 ) );
        t0.addColumn( ArrayColumn.makeColumn( ci2, cd2 ) );
        t0.addColumn( ArrayColumn.makeColumn( ci3, cd3 ) );
        return t0;
    }
}
