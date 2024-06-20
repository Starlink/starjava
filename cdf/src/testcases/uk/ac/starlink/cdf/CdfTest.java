package uk.ac.starlink.cdf;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;
import uk.ac.bristol.star.cdf.EpochFormatter;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.TimeMapper;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.TestCase;
import uk.ac.starlink.util.URLDataSource;

public class CdfTest extends TestCase {

    static {
        LogUtils.getLogger( CdfStarTable.class.getName() )
                .setLevel( Level.SEVERE );
    }

    public void testExample2() throws IOException {
        StarTable ex2 = readTable( "example2.cdf" );
        assertArrayEquals( new float[] { -165, -150 },
                           ex2.getParameterByName( "Longitude" ).getValue() );
        assertArrayEquals( new float[] { 40, 30 },
                           ex2.getParameterByName( "Latitude" ).getValue() );
        assertEquals( "An example CDF (2).",
                      ((String) ex2.getParameterByName( "TITLE" ).getValue())
                     .trim() );
        assertEquals( 2, ex2.getColumnCount() );
        assertEquals( "Time", ex2.getColumnInfo( 0 ).getName() );
        assertEquals( "Temperature", ex2.getColumnInfo( 1 ).getName() );
        assertEquals( 24, ex2.getRowCount() );
        for ( int i = 0; i < 24; i++ ) {
            assertEquals( Integer.valueOf( i * 100 ), ex2.getCell( i, 0 ) );
            float[] temp = (float[]) ex2.getCell( i, 1 );
            assertEquals( 4, temp.length );
            for ( int j = 0; j < 4; j++ ) {
                assertTrue( temp[ j ] >= 10 && temp[ j ] < 30 );
            }
        }

        /* Note this involves rearranging from row- to column-major. */
        assertArrayEquals( new float[] { 20.0f, 19.2f, 21.7f, 20.7f },
                           ex2.getCell( 0, 1 ) );
    }

    public void testUy() throws IOException {
        StarTable uy = readTable( "uy_m0_grb_19971223_v01.cdf" );
        assertEquals( 4, uy.getColumnCount() );

        DescribedValue ackParam = uy.getParameterByName( "Acknowledgement" );
        ValueInfo ackInfo = ackParam.getInfo();
        assertEquals( String[].class, ackInfo.getContentClass() );
        String[] ackValue = (String[]) ackParam.getValue();
        assertEquals( 6, ackValue.length );
        assertEquals( "C. Tranquille of the ", ackValue[ 4 ] );

        DescribedValue itParam = uy.getParameterByName( "Instrument_type" );
        assertEquals( String.class, itParam.getInfo().getContentClass() );
        assertEquals( "Gamma and X-Rays", itParam.getValue() );

        int iep = 0;
        ColumnInfo epInfo = uy.getColumnInfo( iep );
        DescribedValue vmin = epInfo.getAuxDatumByName( "VALIDMIN" );
        assertEquals( Double.class, vmin.getInfo().getContentClass() );
        assertEquals( "1990-10-06T00:00:00.000",
                      new EpochFormatter()
                     .formatEpoch( ((Double) vmin.getValue()).doubleValue() ) );
        assertEquals( "1997-12-23T00:02:30.000",
                      new EpochFormatter()
                     .formatEpoch( ((Double) uy.getCell( 0, iep ))
                                  .doubleValue() ) );

        int icr = 3;
        ColumnInfo crInfo = uy.getColumnInfo( icr );
        assertEquals( "Count_Rate", crInfo.getName() );
        assertEquals( "data", crInfo.getAuxDatumByName( "VAR_TYPE" )
                                    .getValue() );
        assertEquals( "c/s", crInfo.getUnitString() );
        assertEquals( 479.94f, ((Float) uy.getCell( 0, icr )).floatValue(),
                               .01 );
        assertEquals( 479.53f, ((Float) uy.getCell( 1, icr )).floatValue(),
                               .01 );
        assertTrue( Tables.isBlank( uy.getCell( 2, icr ) ) );

        assertEquals( 288, uy.getRowCount() );
    }

    public void testGeocpi0() throws IOException {
        StarTable geo = readTable( "geocpi0.cdf" );
        assertEquals( 18, geo.getColumnCount() );
        int ihpv = 9;
        ColumnInfo hpvInfo = geo.getColumnInfo( ihpv );
        assertEquals( "HP_V", hpvInfo.getName() );
        assertEquals( "km/sec", hpvInfo.getUnitString() );
        assertEquals( "Bulk flow velocity for ions in the range 1<E<50000 eV ",
                      hpvInfo.getDescription() );
        assertEquals( null, hpvInfo.getAuxDatumByName( "CATDESC" ) );
        assertEquals( "Ion bulk Flow, HP",
                      hpvInfo.getAuxDatumByName( "FIELDNAM" ).getValue() );
        assertArrayEquals( new float[] { -4e3f, -4e3f },
                           hpvInfo.getAuxDatumByName( "VALIDMIN" ).getValue() );
        assertArrayEquals( new float[] { +4e3f, +4e3f },
                           hpvInfo.getAuxDatumByName( "VALIDMAX" ).getValue() );
        assertArrayEquals( new float[] { -255, 77 }, geo.getCell( 0, ihpv ),
                           0.5f );
        assertArrayEquals( new float[] { -399, 86 }, geo.getCell( 1, ihpv ),
                           0.5f );
        assertArrayEquals( new float[] { -262, 129 }, geo.getCell( 2, ihpv ),
                           0.5f );
        assertArrayEquals( new float[] { Float.NaN, Float.NaN },
                           geo.getCell( 3, ihpv ) );
    }

    public void testDomain() throws IOException {
        StarTable tst = readTable( "test.cdf" );
        assertEquals( 17, tst.getColumnCount() );
        int icEp = 11;
        int icEp16 = 12;
        int icTt2k = 14;
        ColumnInfo epInfo = tst.getColumnInfo( icEp );
        ColumnInfo ep16Info = tst.getColumnInfo( icEp16 );
        ColumnInfo tt2kInfo = tst.getColumnInfo( icTt2k );
        assertEquals( "ep", epInfo.getName() );
        assertEquals( "ep16", ep16Info.getName() );
        assertEquals( "tt2000", tt2kInfo.getName() );
        assertEquals( Double.class, epInfo.getContentClass() );
        assertEquals( double[].class, ep16Info.getContentClass() );
        assertArrayEquals( new int[] { 2 }, ep16Info.getShape() );
        assertEquals( Long.class, tt2kInfo.getContentClass() );
        TimeMapper epMapper = getSingleTimeMapper( epInfo );
        TimeMapper ep16Mapper = getSingleTimeMapper( ep16Info );
        TimeMapper tt2kMapper = getSingleTimeMapper( tt2kInfo );

        // These dates obtained by running NASA cdfdump on the test file
        assertEquals( "1998-01-02 03:04:05.666",
                      getDate( tst.getCell( 1, icEp ), epMapper ) );
        assertEquals( "2004-11-29 15:55:23.030",
                      getDate( tst.getCell( 0, icEp16 ), ep16Mapper ) );
        assertEquals( "2008-12-31 23:59:58.123",
                      getDate( tst.getCell( 0, icTt2k ), tt2kMapper ) );
    }

    private TimeMapper getSingleTimeMapper( ValueInfo info ) {
        DomainMapper[] mappers = info.getDomainMappers();
        assertEquals( 1, mappers.length );
        DomainMapper mapper = mappers[ 0 ];
        assertTrue( mapper instanceof TimeMapper );
        return (TimeMapper) mapper;
    }

    private String getDate( Object tval, TimeMapper tmapper ) {
        long unixMillis = (long) ( tmapper.toUnixSeconds( tval ) * 1e3 );
        SimpleDateFormat fmt =
            new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss.SSS", Locale.UK );
        fmt.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        return fmt.format( new Date( unixMillis ) );
    }

    private CdfStarTable readTable( String name ) throws IOException {
        URL url = getClass().getResource( name );
        StarTable table = new CdfTableBuilder()
                         .makeStarTable( new URLDataSource( url ), false,
                                         StoragePolicy.PREFER_MEMORY );
        Tables.checkTable( table );
        return (CdfStarTable) table;
    }
}
