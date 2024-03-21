package uk.ac.starlink.table;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import junit.framework.TestCase;
import uk.ac.starlink.fits.ColFitsTableBuilder;
import uk.ac.starlink.fits.FitsTableBuilder;
import uk.ac.starlink.fits.FitsTableWriter;
import uk.ac.starlink.fits.HealpixFitsTableWriter;
import uk.ac.starlink.table.formats.CsvTableBuilder;
import uk.ac.starlink.table.formats.CsvTableWriter;
import uk.ac.starlink.util.ByteArrayDataSource;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.votable.FitsPlusTableBuilder;
import uk.ac.starlink.votable.VOTableBuilder;
import uk.ac.starlink.votable.VOTableWriter;
import uk.ac.starlink.votable.UnifiedFitsTableWriter;

public class HealpixTest extends TestCase {

    public HealpixTest() {
        LogUtils.getLogger( "uk.ac.starlink.fits" ).setLevel( Level.WARNING );
    }

    public void testIO() throws IOException {
        StarTable t1 = createHealpixTable( 3, true, 'C', -1 );
        StarTable t2 = createHealpixTable( 6, false, 'x', 25 );
        StarTable t3 = createHealpixTable( 5, true, 'G', 16 );
        t3.getParameters().clear();
        StarTable[] tables = { t1, t2, t3 };

        assertTrue( HealpixTableInfo.isHealpix( t1.getParameters() ) );
        assertTrue( HealpixTableInfo.isHealpix( t2.getParameters() ) );
        assertFalse( HealpixTableInfo.isHealpix( t3.getParameters() ) );

        checkRoundTrip( new FitsTableWriter(), new FitsTableBuilder(),
                        tables, true );
        checkRoundTrip( new UnifiedFitsTableWriter(),
                        new FitsPlusTableBuilder(),
                        tables, true );
        checkRoundTrip( new VOTableWriter(), new VOTableBuilder(),
                        tables, true );
        FitsTableWriter colfitsTableWriter = new FitsTableWriter();
        colfitsTableWriter.setColfits( true );
        checkRoundTrip( colfitsTableWriter, new ColFitsTableBuilder(),
                        tables, false );
        checkRoundTrip( new CsvTableWriter(), new CsvTableBuilder(),
                        tables, false );

        HealpixFitsTableWriter hpfWriter = new HealpixFitsTableWriter();
        checkRoundTrip( hpfWriter, new FitsTableBuilder(),
                        new StarTable[] { t1 }, true );
        hpfWriter.writeStarTable( t2, new ByteArrayOutputStream() );
        try {
            hpfWriter.writeStarTable( t3, new ByteArrayOutputStream() );
            fail();
        }
        catch ( TableFormatException e ) {
            // because it's not HEALPix-like
        }
    }

    private void checkRoundTrip( StarTableWriter writer, TableBuilder reader,
                                 StarTable[] tables, boolean isPreserve )
            throws IOException {
        for ( StarTable t1 : tables ) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            writer.writeStarTable( t1, out );
            DataSource datsrc =
                new ByteArrayDataSource( "buf", out.toByteArray() );
            StarTable t2 =
                reader.makeStarTable( datsrc, true,
                                      StoragePolicy.PREFER_MEMORY );
            List<DescribedValue> params1 = t1.getParameters();
            List<DescribedValue> params2 = t2.getParameters();
            HealpixTableInfo hpxInfo1 = HealpixTableInfo.isHealpix( params1 )
                                      ? HealpixTableInfo.fromParams( params1 )
                                      : null;
            HealpixTableInfo hpxInfo2 = HealpixTableInfo.isHealpix( params2 )
                                      ? HealpixTableInfo.fromParams( params2 )
                                      : null;
            if ( isPreserve && hpxInfo1 != null ) {
                assertEquals( hpxInfo1, hpxInfo2 );
                assertEquals( hpxInfo1.toString(), hpxInfo2.toString() );
            }
            else {
                assertNull( hpxInfo2 );
            }
        }
    }

    private static StarTable createHealpixTable( int level, boolean isNest,
                                                 char sysChar, int rowmax ) {
        int nsky = (int) ( 12L << ( 2*level ) );
        int nrow = rowmax > 0 ? Math.min( rowmax, nsky ) : nsky;
        boolean isExplicit = nrow != nsky;
        int[] ipix = new int[ nrow ];
        double[] data = new double[ nrow ];
        for ( int irow = 0; irow < nrow; irow++ ) {
            ipix[ irow ] = irow;
            data[ irow ] = -100 * irow;
        }
        ColumnData ipixCol = ArrayColumn.makeColumn( "hpx" + level, ipix );
        ColumnData dataCol = ArrayColumn.makeColumn( "data", data );
        ColumnStarTable table = ColumnStarTable.makeTableWithRows( nrow );
        if ( isExplicit ) {
            table.addColumn( ipixCol );
        }
        table.addColumn( dataCol );
        String ipixColname = isExplicit ? ipixCol.getColumnInfo().getName()
                                        : null;
        HealpixTableInfo.HpxCoordSys csys =
             HealpixTableInfo.HpxCoordSys.fromCharacter( sysChar );
        HealpixTableInfo hpxInfo =
            new HealpixTableInfo( level, isNest, ipixColname, csys );
        table.getParameters().addAll( Arrays.asList( hpxInfo.toParams() ) );
        return table;
    }
}
