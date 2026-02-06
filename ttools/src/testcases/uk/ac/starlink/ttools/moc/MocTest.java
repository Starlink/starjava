package uk.ac.starlink.ttools.moc;

import java.io.IOException;
import java.net.URL;
import java.util.function.LongFunction;
import java.util.logging.Level;
import junit.framework.TestCase;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.task.MapEnvironment;
import uk.ac.starlink.ttools.task.MocShape;
import uk.ac.starlink.util.LogUtils;

public class MocTest extends TestCase {

    static {
        LogUtils.getLogger( "uk.ac.starlink.table" )
                .setLevel( Level.WARNING );
        LogUtils.getLogger( "uk.ac.starlink.ttools.mode" )
                .setLevel( Level.WARNING );
    }

    public void testImpl() throws IOException, TaskException {
        URL shapesUrl = MocTest.class.getResource( "../func/polygons.vot" );
        StarTable shapeTable =
            new StarTableFactory()
           .makeStarTable( shapesUrl.toString(), "votable" );
        MapEnvironment env0= new MapEnvironment()
           .setValue( "in", shapeTable )
           .setValue( "coords", "vertices" )
           .setValue( "mocfmt", MocStreamFormat.SUMMARY )
           .setValue( "out", "-" );

        // Check different implementations.
        MocImpl[] impls = getMocImpls();
        for ( int order = 0; order < 12; order++ ) {
            MapEnvironment env1 = new MapEnvironment( env0 );
            env1.setValue( "order", Integer.valueOf( order ) );
            env1.setValue( "mocimpl", MocImpl.CDS );
            new MocShape().createExecutable( env1 ).execute();
            String cdsSummary = env1.getOutputText();
            for ( MocImpl impl : impls ) {
                MapEnvironment env2 = new MapEnvironment( env0 );
                env2.setValue( "order", Integer.valueOf( order ) );
                env2.setValue( "mocimpl", impl );
                new MocShape().createExecutable( env2 ).execute();
                assertEquals( cdsSummary, env2.getOutputText() );
            }
        }

        // regression
        for ( MocImpl impl : impls ) {
            MapEnvironment env3 = new MapEnvironment( env0 );
            env3.setValue( "order", Integer.valueOf( 0 ) );
            env3.setValue( "mocimpl", impl );
            env3.setValue( "mocfmt", MocStreamFormat.ASCII );
            new MocShape().createExecutable( env3 ).execute();
            assertEquals( "0/0-4 8-11", env3.getOutputText().trim() );
        }
    }

    private MocImpl[] getMocImpls() {
        return new MocImpl[] {
            MocImpl.BITSET,
            MocImpl.CDS,
            MocImpl.AUTO,
            createBagImpl( "only_bits",
                           s -> s == (int) s ? new BitSetBag( (int) s )
                                             : new MultiBitSetBag( s ) ),
            createBagImpl( "multibits",
                           s -> {
                               long bankSize = s / 12L;
                               while ( bankSize > Integer.MAX_VALUE ) {
                                   bankSize = bankSize >> 1;
                               }
                               return new MultiBitSetBag( s, (int) bankSize );
                           } ),
            createBagImpl( "ints",
                           s -> s == (int) s ? new IntegerBag( 1000 )
                                             : new LongBag( 1000 ) ),
            createBagImpl( "longs",
                           s -> new LongBag( 1000 ) ),
        };
    }

    private static MocImpl createBagImpl( String name,
                                          LongFunction<IndexBag> bagFactory ) {
        return new MocImpl( name, "test" ) {
            public MocBuilder createMocBuilder( int mocOrder ) {
                return new BagMocBuilder( mocOrder, bagFactory );
            }
        };
    }
}
