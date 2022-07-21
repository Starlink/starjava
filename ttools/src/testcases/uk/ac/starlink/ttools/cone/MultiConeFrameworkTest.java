package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import java.util.logging.Level;
import uk.ac.starlink.table.ColumnPermutedStarTable;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.TableTestCase;
import uk.ac.starlink.ttools.cone.Coverage;
import uk.ac.starlink.ttools.task.TableProducer;
import uk.ac.starlink.votable.VOTableBuilder;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.TestCase;
import uk.ac.starlink.util.URLDataSource;

public class MultiConeFrameworkTest extends TableTestCase {

    private static final ConeErrorPolicy errAct = ConeErrorPolicy.ABORT;

    public MultiConeFrameworkTest( String name ) {
        super( name );
        LogUtils.getLogger( "uk.ac.starlink.ttools.cone" )
                .setLevel( Level.WARNING );
        LogUtils.getLogger( "uk.ac.starlink.table" ).setLevel( Level.WARNING );
    }

    public void testLinear() throws Exception {
        StarTable t1f = doTestLinear( 1, false );
        int[] colMap = new int[ t1f.getColumnCount() ];
        for ( int i = 0; i < colMap.length; i++ ) {
            colMap[ i ] = i;
        }
        StarTable t1 = doTestLinear( 1, true );
        int[] nThreads = new int[] { 2, 8, 33, };
        for ( int i = 0; i < nThreads.length; i++ ) {
            StarTable tn = doTestLinear( nThreads[ i ], true );
            assertSameData( t1, tn );
            assertSameData( t1f, new ColumnPermutedStarTable( tn, colMap ) );
        }
    }

    private StarTable doTestLinear( int parallelism, boolean addScore )
            throws Exception {
        int nIn = 4;
        int nOut = 2;
        ConeSearcher searcher = new LinearConeSearcher( nIn, nOut );
        String scoreCol = addScore ? "dist" : null;

        final StarTable messier = 
            new VOTableBuilder()
           .makeStarTable( new URLDataSource( getClass()
                                             .getResource( "../messier.xml" ) ),
                           true, StoragePolicy.PREFER_MEMORY );
        TableProducer inProd = new TableProducer() {
            public StarTable getTable() {
                return messier;
            }
        };

        ConeMatcher bestMatcher = new ConeMatcher(
                searcher, errAct, inProd,
                new JELQuerySequenceFactory( "RA + 0", "DEC", "0.5" ), true,
                null, false, true, parallelism, "*", scoreCol,
                JoinFixAction.NO_ACTION, JoinFixAction.NO_ACTION );
        StarTable bestResult = Tables.randomTable( getTable( bestMatcher ) );

        assertEquals( messier.getRowCount(), bestResult.getRowCount() );
        assertEquals( messier.getColumnCount() + 3 + ( addScore ? 1 : 0 ),
                      bestResult.getColumnCount() );

        ConeMatcher eachMatcher = new ConeMatcher(
                searcher, errAct, inProd,
                new JELQuerySequenceFactory( "RA + 0", "DEC", "0.5" ), true,
                null, true, true, parallelism, "*", scoreCol,
                JoinFixAction.NO_ACTION, JoinFixAction.NO_ACTION );
        StarTable eachResult = Tables.randomTable( getTable( eachMatcher ) );

        assertSameData( bestResult, eachResult );
        assertEquals( messier.getRowCount(), eachResult.getRowCount() );

        ConeMatcher allMatcher = new ConeMatcher(
                searcher, errAct, inProd,
                new JELQuerySequenceFactory( "ucd$POS_EQ_RA_", "ucd$POS_EQ_DEC",
                                             "0.1 + 0.2" ),
                false, null, false, true, parallelism, "RA DEC", scoreCol,
                JoinFixAction.makeRenameDuplicatesAction( "_A" ),
                JoinFixAction.makeRenameDuplicatesAction( "_B" ) );
        StarTable allResult = Tables.randomTable( getTable( allMatcher ) );

        assertEquals( messier.getRowCount() * nIn, allResult.getRowCount() );
        assertEquals( 2 + 3 + ( addScore ? 1 : 0 ),
                      allResult.getColumnCount() );

        Coverage footNorth = new HemisphereCoverage( true );
        Coverage footSouth = new HemisphereCoverage( false );
        ConeMatcher footMatcherN = new ConeMatcher(
                searcher, errAct, inProd,
                new JELQuerySequenceFactory( "RA + 0", "DEC", "0.5" ), true,
                footNorth, false, true, parallelism, "*", scoreCol,
                JoinFixAction.NO_ACTION, JoinFixAction.NO_ACTION );
        ConeMatcher footMatcherS = new ConeMatcher(
                searcher, errAct, inProd,
                new JELQuerySequenceFactory( "RA + 0", "DEC", "0.5" ), true,
                footSouth, false, true, parallelism, "*", scoreCol,
                JoinFixAction.NO_ACTION, JoinFixAction.NO_ACTION );
        StarTable footResultN = Tables.randomTable( getTable( footMatcherN ) );
        StarTable footResultS = Tables.randomTable( getTable( footMatcherS ) );
        long nrN = footResultN.getRowCount();
        long nrS = footResultS.getRowCount();
        assertTrue( nrN > 10 );
        assertTrue( nrS > 10 );
        long nrBoth = nrN + nrS - bestResult.getRowCount();
        assertTrue( nrBoth > 0 );
        assertTrue( nrBoth < 10 );

        if ( parallelism == 1 ) { // else order of requests is non-deterministic
            ConeSearcher searcher2 = new GappyConeSearcher( searcher, false ) {
                protected boolean isGap( int irow ) {
                    return irow % 2 == 0;
                }
            };

            ConeMatcher bestMatcher2 = new ConeMatcher(
                    searcher2, errAct, inProd,
                    new JELQuerySequenceFactory( "RA + 0", "DEC", "0.5" ), true,
                    null, false, true, parallelism, "*", scoreCol,
                    JoinFixAction.NO_ACTION, JoinFixAction.NO_ACTION );
            StarTable bestResult2 =
                Tables.randomTable( getTable( bestMatcher2 ) );
            assertEquals( messier.getRowCount() / 2,
                          bestResult2.getRowCount() );

            ConeMatcher eachMatcher2 = new ConeMatcher(
                    searcher2, errAct, inProd,
                    new JELQuerySequenceFactory( "RA + 0", "DEC", "0.5" ), true,
                    null, true, true, parallelism, "*", scoreCol,
                    JoinFixAction.NO_ACTION, JoinFixAction.NO_ACTION );
            StarTable eachResult2 =
                Tables.randomTable( getTable( eachMatcher2 ) );
            assertEquals( messier.getRowCount(), eachResult2.getRowCount() );

            ConeMatcher allMatcher2 = new ConeMatcher(
                    searcher2, errAct, inProd,
                    new JELQuerySequenceFactory( "ucd$POS_EQ_RA_",
                                                 "ucd$POS_EQ_DEC",
                                                 "0.1 + 0.2" ),
                    false, null, false, true, parallelism, "RA DEC", scoreCol,
                    JoinFixAction.makeRenameDuplicatesAction( "_A" ),
                    JoinFixAction.makeRenameDuplicatesAction( "_B" ) );
            StarTable allResult2 =
                Tables.randomTable( getTable( allMatcher2 ) );
            assertEquals( messier.getRowCount() * nIn / 2,
                          allResult2.getRowCount() );
        }

        if ( addScore ) {
            int iscore = allResult.getColumnCount() - 1;
            assertEquals( scoreCol,
                          allResult.getColumnInfo( iscore ).getName() );
            RowSequence rseq = allResult.getRowSequence();
            double maxFrac = 0;
            while ( rseq.next() ) {
                double score = ((Number) rseq.getCell( iscore )).doubleValue();
                double fraction = score / 0.3;
                maxFrac = Math.max( fraction, maxFrac );
                assertTrue( fraction >= 0 );
                assertTrue( fraction <= 1.0 );
            }
            rseq.close();
            assertTrue( maxFrac > 0.5 );
        }

        final int iRa = 5;
        final int iDec = 6;
        assertEquals( "RA", messier.getColumnInfo( iRa ).getName() );
        assertEquals( "DEC", messier.getColumnInfo( iDec ).getName() );
        assertEquals( "RA_A", allResult.getColumnInfo( 0 ).getName() );
        assertEquals( "DEC_A", allResult.getColumnInfo( 1 ).getName() );
        assertEquals( "ID", allResult.getColumnInfo( 2 ).getName() );
        assertEquals( "RA_B", allResult.getColumnInfo( 3 ).getName() );
        assertEquals( "Dec_B", allResult.getColumnInfo( 4 ).getName() );
        QuerySequenceFactory qsFact3 = new QuerySequenceFactory() {
            public ConeQueryRowSequence createQuerySequence( StarTable table )
                    throws IOException {
                return ColumnQueryRowSequence
                      .createFixedRadiusSequence( table, iRa, iDec, 0.5 );
            }
        };
        ConeMatcher matcher3 = new ConeMatcher(
                searcher, errAct, inProd, qsFact3, true, null, false, true,
                parallelism, "", scoreCol,
                JoinFixAction.NO_ACTION, JoinFixAction.NO_ACTION );
        StarTable result3 = Tables.randomTable( getTable( matcher3 ) );
        assertEquals( 3 + ( addScore ? 1 : 0 ), result3.getColumnCount() );
        assertEquals( "ID", result3.getColumnInfo( 0 ).getName() );
        assertEquals( "RA", result3.getColumnInfo( 1 ).getName() );
        assertEquals( "Dec", result3.getColumnInfo( 2 ).getName() );
        assertEquals( messier.getRowCount(), result3.getRowCount() );
        RowSequence rseq1 = messier.getRowSequence();
        RowSequence rseq2 = result3.getRowSequence();
        while ( rseq1.next() ) {
            assertTrue( rseq2.next() );
            assertEquals( rseq1.getCell( iRa ), rseq2.getCell( 1 ) );
            assertEquals( rseq1.getCell( iDec ), rseq2.getCell( 2 ) );
        }
        assertTrue( ! rseq1.next() );
        assertTrue( ! rseq2.next() );
        rseq1.close();
        rseq2.close();

        return allResult;
    }

    private static StarTable getTable( ConeMatcher coneMatcher )
            throws IOException, TaskException {
        ConeMatcher.ConeWorker worker = coneMatcher.createConeWorker();
        new Thread( worker ).run();
        return worker.getTable();
    }

    /**
     * Test coverage that covers a hemisphere at a time.
     */
    private static class HemisphereCoverage implements Coverage {
        private final boolean isNorth_;
        HemisphereCoverage( boolean isNorth ) {
            isNorth_ = isNorth;
        }
        public void initCoverage() {
        }
        public Amount getAmount() {
            return Amount.SOME_SKY;
        }
        public boolean discOverlaps( double alphaDeg, double deltaDeg,
                                     double radiusDeg ) {
            return isNorth_ ? deltaDeg > -radiusDeg
                            : deltaDeg < +radiusDeg;
        }
    }
}
