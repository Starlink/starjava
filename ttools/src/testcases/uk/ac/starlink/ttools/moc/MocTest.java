package uk.ac.starlink.ttools.moc;

import java.io.IOException;
import java.net.URL;
import java.util.function.LongFunction;
import java.util.logging.Level;
import junit.framework.TestCase;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TimeMapper;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.AreaDomain;
import uk.ac.starlink.ttools.mode.MocShapeMode;
import uk.ac.starlink.ttools.mode.TimeIntervalType;
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

    public void testStmoc() throws Exception {

        // This is a regression test, and not a particularly good one,
        // the data does not attempt to probe any interesting cases,
        // except by accident?
        // The result it's testing against is output from MOCpy (0.19.1);
        // that's known to be buggy at the version I used in some
        // circumstances (sensitive to the order of input values,
        // see https://github.com/cds-astro/mocpy/issues/206),
        // but probably it's doing the right thing for this data;
        // anyhow it agrees with mocshape, though note that some of
        // the code being compared originates in the same place
        // (CDS, not independent implementation).
        MapEnvironment stenv = new MapEnvironment();
        stenv.setValue( "moctype", MocShapeMode.MocType.STMOC );
        stenv.setValue( "in",
                        MocTest.class.getResource( "gtc1.csv" ).toString() );
        stenv.setValue( "order", Integer.valueOf( 1 ) );
        stenv.setValue( "shape", AreaDomain.POINT_MAPPER );
        stenv.setValue( "coords", "array(ra,dec)" );
        stenv.setValue( "torder", Integer.valueOf( 20 ) );
        stenv.setValue( "tshape", TimeIntervalType.RANGE );
        stenv.setValue( "t0", "initime" );
        stenv.setValue( "t1", "endtime" );
        new MocShape().createExecutable( stenv ).execute();
        String sttxt = stenv.getOutputText().trim().replaceAll( "\\s+", " " );
        assertEquals( "t20/96468 s1/7 t20/96509 s1/27 t20/96546 s1/11 "
                    + "t20/96575 s1/17 22 t20/96584 s1/7 t20/96587 s1/12 "
                    + "t20/96588 s1/0 t20/96593 s1/2 t20/96674 s1/28",
                      sttxt );

        MapEnvironment stenv1 = new MapEnvironment( stenv );
        stenv1.setValue( "order", Integer.valueOf( 0 ) );
        stenv1.setValue( "torder", Integer.valueOf( 15 ) );
        new MocShape().createExecutable( stenv1 ).execute();
        String sttxt1 = stenv1.getOutputText().trim().replaceAll( "\\s+", " " );
        assertEquals( "t15/3014 s0/1 t15/3015 s0/6 t15/3017 s0/2 4-5 "
                    + "t15/3018 s0/0-1 3 t15/3021 s0/7",
                      sttxt1 );

        MapEnvironment tenv = new MapEnvironment( stenv );
        tenv.setValue( "moctype", MocShapeMode.MocType.TMOC );
        new MocShape().createExecutable( tenv ).execute();
        String ttxt = tenv.getOutputText().trim().replaceAll( "\\s+", " " );
        assertEquals( "20/96468 96509 96546 96575 96584 96587-96588 "
                    + "96593 96674",
                      ttxt );

        MapEnvironment tenv1 = new MapEnvironment( tenv );
        tenv1.setValue( "torder", Integer.valueOf( 15 ) );
        tenv1.setValue( "ttype", TimeMapper.ISO_8601 );
        new MocShape().createExecutable( tenv1 ).execute();
        String ttxt1 = tenv1.getOutputText().trim().replaceAll( "\\s+", " " );
        assertEquals( "14/1507 15/3017-3018 3021", ttxt1 );

        MapEnvironment senv = new MapEnvironment( stenv );
        senv.setValue( "moctype", MocShapeMode.MocType.SMOC );
        new MocShape().createExecutable( senv ).execute();
        String stxt = senv.getOutputText().trim().replaceAll( "\\s+", " " );
        assertEquals( "1/0 2 7 11-12 17 22 27-28", stxt );

        MapEnvironment senv1 = new MapEnvironment( senv );
        senv1.setValue( "order", Integer.valueOf( 0 ) );
        new MocShape().createExecutable( senv1 ).execute();
        String stxt1 = senv1.getOutputText().trim().replaceAll( "\\s+", " " );
        assertEquals( "0/0-7", stxt1 );
    }

    private MocImpl[] getMocImpls() {
        return new MocImpl[] {
            MocImpl.BITSET,
            MocImpl.CDS,
            MocImpl.CDS_BATCH,
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
        return new MocImpl( name, "test", false ) {
            public MocBuilder createMocBuilder( int mocOrder ) {
                return new BagMocBuilder( mocOrder, bagFactory );
            }
        };
    }
}
