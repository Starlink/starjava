package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.net.URL;
import junit.framework.TestCase;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.example.GeojsonTableBuilder;

public class IOTest extends TestCase {

    public void testBeanConfig() throws IOException, TaskException {
        URL url = IOTest.class.getResource( "c2.geojson" );
        String loc = url.toString();
        String gjname = GeojsonTableBuilder.class.getName();
        StarTable out1 = loadTable( loc, gjname, false );
        assertEquals( 2, out1.getRowCount() );
        assertEquals( "shape", out1.getColumnInfo( 1 ).getName() );

        StarTable out2 =
            loadTable( loc, gjname + "(shapeColName=outline)", false );
        assertEquals( "outline", out2.getColumnInfo( 1 ).getName() );

        failLoadTable( loc, gjname + "(texture=fluffy)" );
        failLoadTable( loc, gjname = "(shapeColName=outline" );

        StarTable out3 = loadTable( loc, "geojson", true );
        assertEquals( "shape", out3.getColumnInfo( 1 ).getName() );

        StarTable out4 = loadTable( loc, "geojson(shapeColName=border)", true );
        assertEquals( "border", out4.getColumnInfo( 1 ).getName() );
    }

    private void failLoadTable( String in, String ifmt ) {
        Throwable error;
        try {
            loadTable( in, ifmt, true );
            error = null;
        }
        catch ( TaskException | IOException e ) {
            error = e;
        }
        assertTrue( error instanceof TaskException );
    }

    private StarTable loadTable( String in, String ifmt, boolean addGeojson )
            throws IOException, TaskException {
        MapEnvironment env = new MapEnvironment()
                            .setValue( "in", in )
                            .setValue( "ifmt", ifmt );
        if ( addGeojson ) {
            env.getTableFactory().getKnownBuilders()
                                 .add( new GeojsonTableBuilder() );
        }
        new TablePipe().createExecutable( env ).execute();
        return env.getOutputTable( "omode" );
    }
}
