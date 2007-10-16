package uk.ac.starlink.ttools.cone;

import java.util.logging.Level;
import java.util.logging.Logger;
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
                true, "*" );
        StarTable bestResult = Tables.randomTable( bestMatcher.getTable() );
        assertEquals( messier.getRowCount(), bestResult.getRowCount() );
        assertEquals( messier.getColumnCount() + 3,
                      bestResult.getColumnCount() );

        SkyConeMatch2Producer allMatcher = new SkyConeMatch2Producer(
                searcher, inProd,
                new JELQuerySequenceFactory( "ucd$POS_EQ_RA_", "ucd$POS_EQ_DEC",
                                             "0.1 + 0.2" ),
                false, "RA DEC" );
        StarTable allResult = Tables.randomTable( allMatcher.getTable() );
        assertEquals( messier.getRowCount() * nIn, allResult.getRowCount() );
        assertEquals( 2 + 3, allResult.getColumnCount() );
    }
}
