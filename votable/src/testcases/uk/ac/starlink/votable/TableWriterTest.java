package uk.ac.starlink.votable;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import junit.framework.TestCase;
import org.xml.sax.InputSource;
import org.iso_relax.verifier.Verifier;
import uk.ac.starlink.table.ArrayColumn;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableWriter;

public class TableWriterTest extends TestCase {

    public TableWriterTest( String name ) {
        super( name );
    }

    public void testTableWriter() throws Exception {
        int nrow = 23;
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
        checkTable( t0 );
    }

    public static void checkTable( StarTable table ) throws Exception {
        StarTableWriter[] writers = VOTableWriter.getStarTableWriters();
        Verifier verifier = VOTableSchema.getSchema( "1.1" ).newVerifier();
        for ( int i = 0; i < writers.length; i++ ) {
            VOTableWriter vowriter = (VOTableWriter) writers[ i ];
            ByteArrayOutputStream ostrm = new ByteArrayOutputStream();
            vowriter.writeStarTable( table, ostrm );
            verifier.verify( new InputSource(
                                 new ByteArrayInputStream(
                                     ostrm.toByteArray() ) ) );
        }
    }
}
