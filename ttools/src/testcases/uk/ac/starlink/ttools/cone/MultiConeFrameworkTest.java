package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.ttools.task.TableProducer;
import uk.ac.starlink.votable.VOTableBuilder;
import uk.ac.starlink.util.TestCase;
import uk.ac.starlink.util.URLDataSource;

public class MultiConeFrameworkTest extends TestCase {

    public MultiConeFrameworkTest( String name ) {
        super( name );
        Logger.getLogger( "uk.ac.starlink.ttools.cone" )
              .setLevel( Level.WARNING );
    }

    public void testLinear() throws Exception {
        doTestLinear( 1 );
        doTestLinear( 2 );
        doTestLinear( 33 );
    }

    private void doTestLinear( int parallelism ) throws Exception {
        int nIn = 4;
        int nOut = 2;
        ConeSearcher searcher = new LinearConeSearcher( nIn, nOut );

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

        SkyConeMatch2Producer bestMatcher = new SkyConeMatch2Producer(
                searcher, inProd,
                new JELQuerySequenceFactory( "RA + 0", "DEC", "0.5" ),
                parallelism, true, "*",
                JoinFixAction.NO_ACTION, JoinFixAction.NO_ACTION );
        StarTable bestResult = Tables.randomTable( bestMatcher.getTable() );
        assertEquals( messier.getRowCount(), bestResult.getRowCount() );
        assertEquals( messier.getColumnCount() + 3,
                      bestResult.getColumnCount() );

        SkyConeMatch2Producer allMatcher = new SkyConeMatch2Producer(
                searcher, inProd,
                new JELQuerySequenceFactory( "ucd$POS_EQ_RA_", "ucd$POS_EQ_DEC",
                                             "0.1 + 0.2" ),
                parallelism, false, "RA DEC",
                JoinFixAction.makeRenameDuplicatesAction( "_A" ),
                JoinFixAction.makeRenameDuplicatesAction( "_B" ) );
        StarTable allResult = Tables.randomTable( allMatcher.getTable() );
        assertEquals( messier.getRowCount() * nIn, allResult.getRowCount() );
        assertEquals( 2 + 3, allResult.getColumnCount() );

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
        SkyConeMatch2Producer matcher3 = new SkyConeMatch2Producer(
                searcher, inProd, qsFact3, parallelism, true, "",
                JoinFixAction.NO_ACTION, JoinFixAction.NO_ACTION );
        StarTable result3 = Tables.randomTable( matcher3.getTable() );
        assertEquals( 3, result3.getColumnCount() );
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
    }
}
