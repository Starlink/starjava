package uk.ac.starlink.ttools.func;

import java.net.URL;
import java.util.logging.Level;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.formats.AsciiTableBuilder;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.TestCase;

public class KCorrectionsTest extends TestCase {

    public KCorrectionsTest() {
        LogUtils.getLogger( "uk.ac.starlink.table" ).setLevel( Level.WARNING );
    }

    public void testCalculations() throws Exception {
        URL testDataUrl = KCorrectionsTest.class.getResource( "kcorrData.txt" );
        StarTable table =
            new AsciiTableBuilder()
           .makeStarTable( DataSource.makeDataSource( testDataUrl ),
                           true, StoragePolicy.PREFER_MEMORY );
        table = Tables.randomTable( table );
        RowSequence rseq = table.getRowSequence();
        double tol = 1e-10;
        while ( rseq.next() ) {
            Object[] row = rseq.getRow();
            String fname = (String) row[ 0 ];
            double redshift = ((Number) row[ 1 ]).doubleValue();
            String c1name = (String) row[ 2 ];
            String c2name = (String) row[ 3 ];
            double cvalue = ((Number) row[ 4 ]).doubleValue();
            double kcor = ((Number) row[ 5 ]).doubleValue();
            KCorrections.KFilter filter = getFilter( fname );
            KCorrections.KColor color = getColor( c1name, c2name );
            assertEquals( kcor,
                          KCorrections.kCorr( filter, redshift, color, cvalue ),
                          tol );
        }
        rseq.close();
    }

    private KCorrections.KFilter getFilter( String fname )
            throws Exception {
        KCorrections.KFilter filter =
            (KCorrections.KFilter)
            KCorrections.class.getField( "KCF_" + fname ).get( null );
        return filter;
    }

    private KCorrections.KColor getColor( String c1name, String c2name )
            throws Exception {
        KCorrections.KColor color =
            (KCorrections.KColor)
            KCorrections.class.getField( "KCC_" + c1name + c2name ).get( null );
        return color;
    }
}
