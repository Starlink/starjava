package uk.ac.starlink.ttools.task;

import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.TableTestCase;
import uk.ac.starlink.ttools.QuickTable;

public class TableCopyTest extends TableTestCase {

    public TableCopyTest( String name ) {
        super( name );
    }

    public void testCopy() throws Exception {
        StarTable in = new QuickTable( 4, new ColumnData[] {
            col( "x", new int[] { 1, 2, 3, 4 } ),
            col( "y", new int[] { 9, 8, 7, 6 } ),
            col( "z", new Integer[] { Integer.valueOf( 3 ), null, null, null }),
        } );
        MapEnvironment env = new MapEnvironment()
                            .setValue( "in", in );
        new TableCopy().createExecutable( env ).execute();
        StarTable out = env.getOutputTable( "out" );
        assertArrayEquals( new String[] { "x", "y", "z" },
                           getColNames( out ) );
        assertSameData( in, out );
    }

}
