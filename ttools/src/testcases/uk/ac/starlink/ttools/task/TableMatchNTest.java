package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;
import uk.ac.starlink.table.ArrayColumn;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.TableTestCase;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.votable.VOTableWriter;

public class TableMatchNTest extends TableTestCase {

    private final Random random_ = new Random( 11223344556677L );

    public TableMatchNTest( String name ) {
        super( name );
        LogUtils.getLogger( "uk.ac.starlink.util" ).setLevel( Level.WARNING );
        LogUtils.getLogger( "uk.ac.starlink.ttools" ).setLevel( Level.WARNING );
    }

    public void testGroupMatchN() throws IOException, TaskException {
        int np = 100;
        StarTable t1a = createNumberTable( np, 1 );
        StarTable t1b = createNumberTable( np, 1 );
        StarTable t1c = createNumberTable( np, 1 );
        StarTable t2 = createNumberTable( np, 2 );

        assertEquals( 6, groupMatchN( new StarTable[] { t1a, t1b, t1c, },
                                      new boolean[ 3 ], 0.5 )
                        .getColumnCount() );

        assertEquals( 100L, groupMatchN( new StarTable[] { t1a, t1b, t1c, },
                                         new boolean[ 3 ], 0.5 )
                           .getRowCount() );
        assertEquals( 100L, groupMatchN( new StarTable[] { t1a, t1b, t1c, t2, },
                                         new boolean[ 4 ], 0.5 )
                           .getRowCount() );

        // These ones just regression tests really, though I've eyeballed 
        // the results and they look about right.
        assertEquals(  89L, groupMatchN( new StarTable[] { t1a, t1b, t1c, },
                                         new boolean[ 3 ], 0.2 )
                           .getRowCount() );
        assertEquals(  89L, groupMatchN( new StarTable[] { t1c, t1a, t1b, },
                                         new boolean[ 3 ], 0.2 )
                           .getRowCount() );
        assertEquals(  47L, groupMatchN( new StarTable[] { t1a, t1b, t1c, },
                                         new boolean[ 3 ], 0.1 )
                           .getRowCount() );
        assertEquals(  18L, groupMatchN( new StarTable[] { t1a, t1b, t1c, },
                                         new boolean[ 3 ], 0.05 )
                           .getRowCount() );
        assertEquals(   0L, groupMatchN( new StarTable[] { t1a, t1b, t1c, },
                                         new boolean[ 3 ], 0.00001 )
                           .getRowCount() );
        assertEquals( 200L, groupMatchN( new StarTable[] { t1a, t1b, t1a, },
                                         new boolean[] { false, true, false },
                                         0.00001 ).getRowCount() );
    }

    public void testPairsMatchN() throws IOException, TaskException {
        int np = 100;
        StarTable t1a = createNumberTable( np, 1 );
        StarTable t1b = createNumberTable( np, 1 );
        StarTable t1c = createNumberTable( np, 1 );
        StarTable t2 = createNumberTable( np, 2 );

        assertEquals( 6, pairsMatchN( new StarTable[] { t1a, t1b, t1c, },
                                      new boolean[ 3 ], 0.5, 1 )
                        .getColumnCount() );

        assertEquals( 100L, pairsMatchN( new StarTable[] { t1a, t1b, t1c, },
                                         new boolean[ 3 ], 0.5, 2 )
                           .getRowCount() );
        assertEquals( 100L, pairsMatchN( new StarTable[] { t1a, t1b, t1c, t2, },
                                         new boolean[ 4 ], 0.5, 3 )
                           .getRowCount() );

        // These ones just regression tests really, though I've eyeballed 
        // the results and they look about right.
        assertEquals(  76L, pairsMatchN( new StarTable[] { t1a, t1b, t1c, },
                                         new boolean[ 3 ], 0.2, 1 )
                           .getRowCount() );
        assertEquals(  76L, pairsMatchN( new StarTable[] { t1c, t1a, t1b, },
                                         new boolean[ 3 ], 0.2, 2 )
                           .getRowCount() );
        assertEquals(  76L, pairsMatchN( new StarTable[] { t1c, t1b, t1a, },
                                         new boolean[ 3 ], 0.2, 3 )
                           .getRowCount() );
        assertEquals(  34L, pairsMatchN( new StarTable[] { t1a, t1b, t1c, },
                                         new boolean[ 3 ], 0.1, 1 )
                           .getRowCount() );
    }

    private StarTable pairsMatchN( StarTable[] tables, boolean[] useAll, 
                                   double err, int iref )
            throws IOException, TaskException {
        int nin= tables.length;
        MapEnvironment env = new MapEnvironment();
        env.setValue( "nin", Integer.toString( nin ) );
        env.setValue( "multimode", "pairs" );
        env.setValue( "iref", Integer.toString( iref ) );
        env.setValue( "matcher", "1d" );
        for ( int i = 0; i < nin; i++ ) {
            int i1 = i + 1;
            env.setValue( "in" + i1, tables[ i ] );
            env.setValue( "values" + i1, "DATA" );
            env.setValue( "join" + i1, useAll[ i ] ? "always" : "match" );
        }
        env.setValue( "params", Double.toString( err ) );
        new TableMatchN().createExecutable( env ).execute();
        StarTable result = env.getOutputTable( "omode" );
        if ( result != null ) {
            Tables.checkTable( result );
        }
        return Tables.randomTable( result );
    }

    private StarTable groupMatchN( StarTable[] tables, boolean[] useAll,
                                   double err )
            throws IOException, TaskException {
        int nin= tables.length;
        MapEnvironment env = new MapEnvironment();
        env.setValue( "nin", Integer.toString( nin ) );
        env.setValue( "multimode", "group" );
        env.setValue( "matcher", "1d" );
        for ( int i = 0; i < nin; i++ ) {
            int i1 = i + 1;
            env.setValue( "in" + i1, tables[ i ] );
            env.setValue( "values" + i1, "DATA" );
            env.setValue( "join" + i1, useAll[ i ] ? "always" : "match" );
        }
        env.setValue( "params", Double.toString( err ) );
        new TableMatchN().createExecutable( env ).execute();
        StarTable result = env.getOutputTable( "omode" );
        if ( result != null ) {
            Tables.checkTable( result );
        }
        return Tables.randomTable( result );
    }

    private StarTable createNumberTable( int nPoint, int mult )
            throws IOException {
        double[] data = new double[ nPoint * mult ];
        int[] index = new int[ nPoint * mult ];
        int j = 0;
        for ( int ip = 0; ip < nPoint; ip++ ) {
            for ( int im = 0; im < mult; im++ ) {
                index[ j ] = ip + 1;
                data[ j ] = ip + 1 + random_.nextGaussian() * 0.1;
                j++;
            }
        }
        ColumnStarTable table =
            ColumnStarTable.makeTableWithRows( nPoint * mult );
        table.addColumn( ArrayColumn.makeColumn( "INDEX", index ) );
        table.addColumn( ArrayColumn.makeColumn( "DATA", data ) );
        return table;
    }

    public static void main( String[] args ) throws IOException {
        int nPoint = Integer.parseInt( args[ 0 ] );
        int mult = args.length > 1 ? Integer.parseInt( args[ 1 ] )
                                 : 1;
        TableMatchNTest tmnt = new TableMatchNTest( "test" );
        tmnt.random_.setSeed( System.currentTimeMillis() );
        StarTable table = tmnt.createNumberTable( nPoint, mult );
        new VOTableWriter().writeStarTable( table, System.out );
    }
}
