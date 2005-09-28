package uk.ac.starlink.ttools.task;

import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.ttools.QuickTable;
import uk.ac.starlink.ttools.TableTestCase;

public class TableCatTest extends TableTestCase {

    public TableCatTest( String name ) {
        super( name );
    }

    public void test2() throws Exception {
        StarTable t1 = new QuickTable( 2, new ColumnData[] {
            col( "index", new int[] { 1, 2 } ),
            col( "name", new String[] { "milo", "theo", } ),
        } );

        StarTable t2 = new QuickTable( 2, new ColumnData[] {
            col( "ix", new int[] { 1, 2 } ),
            col( "atkname", new String[] { "charlotte", "jonathon", } ),
        } );

        MapEnvironment env = new MapEnvironment()
                            .setValue( "in1", t1 )
                            .setValue( "in2", t2 );
        new TableCat().createExecutable( env ).execute();
        StarTable out = env.getOutputTable( "omode" );

        Tables.checkTable( out );

        assertArrayEquals( new String[] { "index", "name" },
                           getColNames( out ) );
        assertArrayEquals( box( new int[] { 1, 2, 1, 2, } ),
                           getColData( out, 0 ) );
        assertArrayEquals( new Object[] { "milo", "theo",
                                          "charlotte", "jonathon", },
                           getColData( out, 1 ) );
    }
}
