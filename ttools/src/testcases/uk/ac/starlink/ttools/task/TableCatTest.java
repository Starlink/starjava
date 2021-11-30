package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.ttools.QuickTable;
import uk.ac.starlink.ttools.TableTestCase;

public class TableCatTest extends TableTestCase {

    final QuickTable t1_ = new QuickTable( 2, new ColumnData[] {
        col( "index", new int[] { 1, 2 } ),
        col( "name", new String[] { "milo", "theo", } ),
    }, "table_1" );

    final QuickTable t2_ = new QuickTable( 3, new ColumnData[] {
        col( "ix", new int[] { 1, 2, 3, } ),
        col( "atkname", new String[] { "charlotte", "jonathon", "gerald", } ),
    }, "table_2" );

    public TableCatTest( String name ) {
        super( name );
        Logger.getLogger( "uk.ac.starlink.votable" ).setLevel( Level.WARNING );
        Logger.getLogger( "uk.ac.starlink.table.storage" )
              .setLevel( Level.WARNING );
    }

    public void test2() throws Exception {

        MapEnvironment env2 = new MapEnvironment()
                             .setValue( "nin", "2" )
                             .setValue( "in1", t1_ )
                             .setValue( "in2", t2_ );
        new TableCatN().createExecutable( env2 ).execute();
        StarTable out2 = env2.getOutputTable( "omode" );

        MapEnvironment envN = new MapEnvironment()
                             .setValue( "in", new StarTable[] { t1_, t2_, } );
        new TableCat().createExecutable( envN ).execute();
        StarTable outN = envN.getOutputTable( "omode" );

        MapEnvironment envL = new MapEnvironment()
                             .setValue( "in", new StarTable[] { t1_, t2_, } )
                             .setValue( "lazy", "true" );
        new TableCat().createExecutable( envL ).execute();
        StarTable outL = envL.getOutputTable( "omode" );

        Tables.checkTable( out2 );
        Tables.checkTable( outN );
        Tables.checkTable( outL );

        assertSameData( out2, outN );
        assertSameData( outL, outN );

        assertArrayEquals( new String[] { "index", "name" },
                           getColNames( out2 ) );
        assertArrayEquals( box( new int[] { 1, 2, 1, 2, 3, } ),
                           getColData( out2, 0 ) );
        assertArrayEquals( new Object[] { "milo", "theo",
                                          "charlotte", "jonathon", "gerald",  },
                           getColData( out2, 1 ) );
    }

    public void testFilter() throws Exception {
        MapEnvironment env2 = new MapEnvironment()
                             .setValue( "nin", "2" )
                             .setValue( "in2", t2_ )
                             .setValue( "in1", t1_ )
                             .setValue( "icmd1", "tail 1" )
                             .setValue( "icmd2", "tail 1" )
                             .setValue( "ocmd", "keepcols '2 1'" );
        new TableCatN().createExecutable( env2 ).execute();
        StarTable out2 = env2.getOutputTable( "omode" );
        assertArrayEquals( box( new int[] { 2, 3, } ),
                           getColData( out2, 1 ) );

        MapEnvironment envN = new MapEnvironment()
                             .setValue( "in",
                                        new StarTable[] { t1_, t2_, t1_, } )
                             .setValue( "icmd", "tail 1" )
                             .setValue( "ocmd", "keepcols '2 1'" );
        new TableCat().createExecutable( envN ).execute();
        StarTable outN = envN.getOutputTable( "omode" );
        assertArrayEquals( box( new int[] { 2, 3, 2 } ),
                           getColData( outN, 1 ) );

        MapEnvironment envL = new MapEnvironment()
                             .setValue( "in",
                                        new StarTable[] { t1_, t2_, t1_, } )
                             .setValue( "icmd", "tail 1" )
                             .setValue( "ocmd", "keepcols '2 1'" )
                             .setValue( "lazy", "true" );
        new TableCat().createExecutable( envL ).execute();
        StarTable outL = envL.getOutputTable( "omode" );
        assertArrayEquals( box( new int[] { 2, 3, 2 } ),
                           getColData( outL, 1 ) );
        assertSameData( outL, outN );
    }

    public void testAddCols() throws Exception {
        MapEnvironment env2 = new MapEnvironment()
                             .setValue( "nin", "2" )
                             .setValue( "in1", t1_ )
                             .setValue( "in2", t2_ )
                             .setValue( "seqcol", "seq" )
                             .setValue( "loccol", "loc" )
                             .setValue( "uloccol", "uloc" );
        new TableCatN().createExecutable( env2 ).execute();
        StarTable out2 = env2.getOutputTable( "omode" );

        MapEnvironment envN = new MapEnvironment()
                             .setValue( "in", new StarTable[] { t1_, t2_, } )
                             .setValue( "seqcol", "seq" )
                             .setValue( "loccol", "loc" )
                             .setValue( "uloccol", "uloc" );
        new TableCat().createExecutable( envN ).execute();
        StarTable outN = envN.getOutputTable( "omode" );

        MapEnvironment envL = new MapEnvironment()
                             .setValue( "in", new StarTable[] { t1_, t2_, } )
                             .setValue( "seqcol", "seq" )
                             .setValue( "loccol", "loc" )
                             .setValue( "uloccol", "uloc" )
                             .setValue( "lazy", "true" );
        new TableCat().createExecutable( envL ).execute();
        StarTable outL = envL.getOutputTable( "omode" );
        assertSameData( outL, outN );

        assertSameData( out2, outN );
        assertArrayEquals(
            new String[] { "index", "name", "seq", "loc", "uloc"  },
            getColNames( out2 ) );
        assertArrayEquals(
            new String[] { "index", "name", "seq", "loc", "uloc" },
            getColNames( outN ) );
        Integer i1 = Integer.valueOf( 1 );
        Integer i2 = Integer.valueOf( 2 );
        assertArrayEquals( new Object[] { i1, i1, i2, i2, i2 },
                           getColData( out2, 2 ) );
        String t1 = "table_1";
        String t2 = "table_2";
        assertArrayEquals( new Object[] { t1, t1, t2, t2, t2, },
                           getColData( outN, 3 ) );
        String n1 = "1";
        String n2 = "2";
        assertArrayEquals( new Object[] { n1, n1, n2, n2, n2, },
                           getColData( out2, 4 ) );
    }

    public void testMulti() throws Exception {
        URL vurl = TableTestCase.class.getResource( "vizier.xml" );
        String colfilter =
            "keepcols 'recno ucd$pos_eq_ra_main ucd$pos_eq_dec_main'";

        MapEnvironment env1 = new MapEnvironment()
           .setValue( "in", vurl.toString() )
           .setValue( "ifmt", "votable" )
           .setValue( "icmd", colfilter )
           .setValue( "multi", "false" );
        new TableCat().createExecutable( env1 ).execute();
        StarTable out1 = env1.getOutputTable( "omode" );
        assertEquals( 1, out1.getRowCount() );

        MapEnvironment envX = new MapEnvironment()
           .setValue( "in", vurl.toString() )
           .setValue( "ifmt", "votable" )
           .setValue( "multi", "true" );
        try {
            new TableCat().createExecutable( envX ).execute();
            fail();
        }
        catch ( IOException e ) { // could be TaskException ?
            // columns incompatible
        }

        MapEnvironment envN = new MapEnvironment()
           .setValue( "in", vurl.toString() )
           .setValue( "ifmt", "votable" )
           .setValue( "icmd", colfilter )
           .setValue( "multi", "true" );
        new TableCat().createExecutable( envN ).execute();
        StarTable outN = envN.getOutputTable( "omode" );
        assertEquals( 159, outN.getRowCount() );
        assertEquals( 3, outN.getColumnCount() );

        MapEnvironment envN1 = new MapEnvironment()
           .setValue( "in", vurl.toString() )
           .setValue( "ifmt", "votable" )
           .setValue( "icmd", colfilter )
           .setValue( "ocmd", "head " + Long.toString( out1.getRowCount() ) )
           .setValue( "multi", "true" );
        new TableCat().createExecutable( envN1 ).execute();
        StarTable outN1 = envN1.getOutputTable( "omode" );
        assertSameData( out1, outN1 );
    }
}
