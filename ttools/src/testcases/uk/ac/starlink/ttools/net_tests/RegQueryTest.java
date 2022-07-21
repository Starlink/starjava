package uk.ac.starlink.ttools.net_tests;

import java.util.logging.Level;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.ttools.TableTestCase;
import uk.ac.starlink.ttools.task.MapEnvironment;
import uk.ac.starlink.ttools.task.RegQuery;
import uk.ac.starlink.util.LogUtils;

public class RegQueryTest extends TableTestCase {

    public RegQueryTest( String name ) {
        super( name );
        LogUtils.getLogger( "uk.ac.starlink.registry" )
                .setLevel( Level.WARNING );
        LogUtils.getLogger( "uk.ac.starlink.vo" ).setLevel( Level.WARNING );
    }

    public void testData() throws Exception {
        MapEnvironment env = new MapEnvironment()
            .setValue( "query", "identifier like '%astrogrid%'" )
            .setValue( "ocmd", "keepcols ID" );
        new RegQuery().createExecutable( env ).execute();
        StarTable result = env.getOutputTable( "omode" );
        Tables.checkTable( result );
        assertTrue( result.getRowCount() > (long) 2e1 &&
                    result.getRowCount() < (long) 2e6 );
        RowSequence rseq = result.getRowSequence();
        while ( rseq.next() ) {
            assertTrue( rseq.getCell( 0 ).toString().indexOf( "astrogrid" )
                        > 1 );
        }
        rseq.close();
    }

    public void testQueries() throws Exception {
        tryQuery( "capability/@standardID = 'ivo://ivoa.net/std/SSA'",
                  10, 100 );
        tryQuery( "capability/@standardID = 'ivo://ivoa.net/std/ConeSearch'"
                + " and title like '%Sloan%'", 20, 200 );
    }

    private void tryQuery( String text, int loCount, int hiCount )
            throws Exception {
        MapEnvironment env = new MapEnvironment()
            .setValue( "query", text );
        new RegQuery().createExecutable( env ).execute();
        StarTable result = env.getOutputTable( "omode" );
        Tables.checkTable( result );
        long nrow = result.getRowCount();
        assertTrue( nrow >= loCount );
        assertTrue( nrow <= hiCount );
    }
}
