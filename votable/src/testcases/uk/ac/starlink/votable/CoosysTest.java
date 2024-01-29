package uk.ac.starlink.votable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import junit.framework.TestCase;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.TimeMapper;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.SourceReader;
import uk.ac.starlink.util.URLDataSource;

/**
 * Note this tests Timesys as well as Coosys.
 */
public class CoosysTest extends TestCase {

    private final StoragePolicy storage_;
    private final StarTableFactory tfact_;

    public CoosysTest() {
        storage_ = StoragePolicy.PREFER_MEMORY;
        tfact_ = new StarTableFactory();
        tfact_.setStoragePolicy( storage_ );
        LogUtils.getLogger( "uk.ac.starlink.table" ).setLevel( Level.WARNING );
        LogUtils.getLogger( "uk.ac.starlink.fits" ).setLevel( Level.WARNING );
    }

    public void testRewrite()
            throws IOException, SAXException, TransformerException {
        URL votloc = getClass().getResource( "tgasmini.vot" );
        DataSource datsrc = new URLDataSource( votloc );
        VOElementFactory vofact = new VOElementFactory( storage_ );
        VOElement vodoc = vofact.makeVOElement( datsrc );
        StarTable t1 = new VOStarTable( ((TableElement)
                                         vodoc.getChildByName( "RESOURCE" )
                                        .getChildByName( "TABLE" )) );
        checkMeta( t1 );
        StarTable t2 = tfact_.makeStarTable( datsrc, "votable" );
        checkMeta( t2 );

        VOTableBuilder votBuilder = new VOTableBuilder();
        FitsPlusTableBuilder fpBuilder = new FitsPlusTableBuilder();
        ColFitsPlusTableBuilder cfpBuilder = new ColFitsPlusTableBuilder();

        VOTableWriter votWriter = new VOTableWriter();
        FitsPlusTableWriter fpWriter = new FitsPlusTableWriter();
        ColFitsPlusTableWriter cfpWriter = new ColFitsPlusTableWriter();

        votWriter.setVotableVersion( VOTableVersion.V14 );
        fpWriter.setVotableVersion( VOTableVersion.V14 );
        cfpWriter.setVotableVersion( VOTableVersion.V14 );

        checkMeta( roundTrip( t1, votWriter, votBuilder ) );
        checkMeta( roundTrip( t2, fpWriter, fpBuilder ) );
        checkMeta( roundTripFile( t1, cfpWriter, cfpBuilder ) );

        VOTableWriter vWriter = new VOTableWriter();
        assertEquals( VOTableVersion.V14, vWriter.getVotableVersion() );
        assertEquals( 3, countTimesys( roundTrip( t1, vWriter, votBuilder ) ) );
        assertEquals( 2, countElements( t1, "TIMESYS", vWriter ) );
        assertEquals( 3, countElements( t1, "COOSYS", vWriter ) );
        assertEquals( 0, countAttributes( t1, "COOSYS", "refposition",
                                          vWriter ) );
        AuxMeta meta14 = new AuxMeta( roundTrip( t1, vWriter, votBuilder ) );
        meta14.assertMeta( "ra", "CoosysRefposition", null );
        meta14.assertMeta( "l", "CoosysRefposition", null );

        vWriter.setVotableVersion( VOTableVersion.V15 );
        assertEquals( 3, countTimesys( roundTrip( t1, vWriter, votBuilder ) ) );
        assertEquals( 2, countElements( t1, "TIMESYS", vWriter ) );
        assertEquals( 3, countElements( t1, "COOSYS", vWriter ) );
        assertEquals( 1, countAttributes( t1, "COOSYS", "refposition",
                                          vWriter ) );
        AuxMeta meta15 = new AuxMeta( roundTrip( t1, vWriter, votBuilder ) );
        meta15.assertMeta( "ra", "CoosysRefposition", "BARYCENTER" );
        meta15.assertMeta( "l", "CoosysRefposition", null );

        vWriter.setVotableVersion( VOTableVersion.V13 );
        assertEquals( 0, countTimesys( roundTrip( t1, vWriter, votBuilder ) ) );
        assertEquals( 0, countElements( t1, "TIMESYS", vWriter ) );
        assertEquals( 3, countElements( t1, "COOSYS", vWriter ) );
        assertEquals( 0, countAttributes( t1, "COOSYS", "refposition",
                                          vWriter ) );
    }

    private StarTable roundTrip( StarTable table, StarTableWriter writer,
                                 TableBuilder builder )
            throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        writer.writeStarTable( table, bout );
        bout.close();
        return tfact_
              .makeStarTable( new ByteArrayInputStream( bout.toByteArray() ),
                              builder );
    }

    private StarTable roundTripFile( StarTable table, StarTableWriter writer,
                                     TableBuilder builder )
            throws IOException {
        File tmpFile =
            File.createTempFile( "tbl", "." + builder.getFormatName() );
        tmpFile.deleteOnExit();
        writer.writeStarTable( table, new FileOutputStream( tmpFile ) );
        try {
            return builder.makeStarTable( new FileDataSource( tmpFile ), true,
                                          storage_ );
        }
        finally {
            tmpFile.delete();
        }
    }

    private void checkMeta( StarTable table ) {
        AuxMeta meta = new AuxMeta( table );
        meta.assertMeta( "ra", "CoosysSystem", "ICRS" );
        meta.assertMeta( "dec", "CoosysSystem", "ICRS" );
        meta.assertMeta( "l", "CoosysSystem", "galactic" );
        meta.assertMeta( "b", "CoosysSystem", "galactic" );
        meta.assertMeta( "ra_ep2000", "CoosysSystem", "ICRS" );
        meta.assertMeta( "dec_ep2000", "CoosysSystem", "ICRS" );

        meta.assertMeta( "ra", "CoosysEpoch", "J2015.0" );
        meta.assertMeta( "dec", "CoosysEpoch", "J2015.0" );
        meta.assertMeta( "l", "CoosysEpoch", null );
        meta.assertMeta( "b", "CoosysEpoch", null );
        meta.assertMeta( "ra_ep2000", "CoosysEpoch", "J2000.0" );
        meta.assertMeta( "dec_ep2000", "CoosysEpoch", "J2000.0" );

        meta.assertMeta( "ref_epoch", "TimesysTimeorigin", null );
        meta.assertMeta( "ref_epoch", "TimesysTimescale", "UTC" );
        meta.assertMeta( "ref_epoch", "TimesysRefposition", "TOPOCENTER" );

        meta.assertMeta( "time1", "TimesysTimeorigin",
                         new Double( 2455197.5 ).toString() );
        meta.assertMeta( "time1", "TimesysTimescale", "TDB" );
        meta.assertMeta( "time1", "TimesysRefposition", "BARYCENTER" );

        meta.assertMeta( "LAUNCH_DATE", "TimesysTimescale", "TDB" );
        meta.assertMeta( "LAUNCH_DATE", "TimesysTimeorigin",
                         new Double( 2455197.5 ).toString() );
        meta.assertMeta( "LAUNCH_DATE", "TimesysRefposition", "BARYCENTER" );
    }

    private int countTimesys( StarTable table ) {
        int nt = 0;
        for ( int ic = 0; ic < table.getColumnCount(); ic++ ) {
            boolean hasTimesys = false;
            for ( DescribedValue aux :
                  table.getColumnInfo( ic ).getAuxData() ) {
                hasTimesys = hasTimesys
                          || aux.getInfo().getName().toLowerCase()
                                .startsWith( "timesys" );
            }
            if ( hasTimesys ) {
                nt++;
            }
        }
        return nt;
    }

    private int countElements( StarTable table, String tagName,
                               StarTableWriter writer )
            throws IOException, TransformerException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        writer.writeStarTable( table, bout );
        bout.close();
        Document doc =
            (Document)
            new SourceReader()
           .getDOM( new StreamSource(
                       new ByteArrayInputStream( bout.toByteArray() ) ) );
        return doc.getElementsByTagName( tagName ).getLength();
    }

    private int countAttributes( StarTable table, String tagName,
                                 String attName, StarTableWriter writer )
            throws IOException, TransformerException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        writer.writeStarTable( table, bout );
        bout.close();
        Document doc =
            (Document)
            new SourceReader()
           .getDOM( new StreamSource(
                       new ByteArrayInputStream( bout.toByteArray() ) ) );
        NodeList elList = doc.getElementsByTagName( tagName );
        int natt = 0;
        for ( int i = 0; i < elList.getLength(); i++ ) {
            Element el = (Element) elList.item( i );
            if ( el.hasAttribute( attName ) ) {
                natt++;
            }
        }
        return natt;
    }
    

    public void testTimeMapping() throws IOException {
        URL votloc = getClass().getResource( "gaiats.vot" );
        StarTable gaiats = tfact_.makeStarTable( new URLDataSource( votloc ) );
        gaiats = Tables.randomTable( gaiats );
        int nr = (int) gaiats.getRowCount();

        int icGaia = 2;
        int icMjd = 3;
        int icJd = 4;
        int icYear = 5;
        int icAbsyear = 6;
        int icIso = 7;
        TimeMapper tmGaia =
            (TimeMapper) gaiats.getColumnInfo( icGaia ).getDomainMappers()[ 0 ];
        TimeMapper tmMjd =
            (TimeMapper) gaiats.getColumnInfo( icMjd ).getDomainMappers()[ 0 ];
        TimeMapper tmJd =
            (TimeMapper) gaiats.getColumnInfo( icJd ).getDomainMappers()[ 0 ];
        TimeMapper tmYear =
            (TimeMapper) gaiats.getColumnInfo( icYear ).getDomainMappers()[ 0 ];
        TimeMapper tmAbsyear =
            (TimeMapper) gaiats.getColumnInfo( icAbsyear )
                               .getDomainMappers()[0];
        TimeMapper tmIso =
            (TimeMapper) gaiats.getColumnInfo( icIso ).getDomainMappers()[ 0 ];

        for ( int ir = 0; ir < nr; ir++ ) {
            Object[] row = gaiats.getRow( ir );
            double gday = ((Number) row[ icGaia ]).doubleValue();
            double mjd = ((Number) row[ icMjd ]).doubleValue();
            double jd = ((Number) row[ icJd ]).doubleValue();
            double dyear = ((Number) row[ icYear ]).doubleValue();
            double ayear = ((Number) row[ icAbsyear]).doubleValue();
            String iso = (String) row[ icIso ];
            assertTrue( jd > 2e6 && jd < 3e6 );
            assertTrue( mjd > 50000 && mjd < 60000 );
            assertTrue( gday > 0 && gday < 10000 );
            assertTrue( dyear > 0 && dyear < 25 );
            assertTrue( ayear > 2000 && ayear < 2025 );
            double tGaia = tmGaia.toUnixSeconds( gday );
            double tMjd = tmMjd.toUnixSeconds( mjd );
            double tJd = tmJd.toUnixSeconds( jd );
            double tYear = tmYear.toUnixSeconds( dyear );
            double tAyear = tmAbsyear.toUnixSeconds( ayear );
            double tIso = tmIso.toUnixSeconds( iso );
            assertEquals( tGaia, tMjd, 1e-3 );
            assertEquals( tGaia, tJd, 1e-3 );
            assertEquals( tGaia, tAyear, 1e-3 );
            assertEquals( tGaia, tIso, 1 );

            // I think this discrepancy is to do with the difference
            // between calendar years (365 or 366 days) and Julian years
            // (365.25 days).  In any case it checks that the result
            // is in the right ball park.
            assertEquals( tGaia, tYear, 12*60*60 );
        }
 
    }

    private class AuxMeta {
        final Map<String,ValueInfo> metaMap_;
        AuxMeta( StarTable table ) {
            metaMap_ = new HashMap<String,ValueInfo>();
            for ( int ic = 0; ic < table.getColumnCount(); ic++ ) {
                ColumnInfo cinfo = table.getColumnInfo( ic );
                metaMap_.put( cinfo.getName(), cinfo );
            }
            for ( DescribedValue dval : table.getParameters() ) {
                ValueInfo vinfo = dval.getInfo();
                metaMap_.put( vinfo.getName(), vinfo );
            }
        }
        void assertMeta( String colname, String auxName, String value ) {
            DescribedValue dval =
                metaMap_.get( colname )
               .getAuxDatumByName( auxName );
            if ( value == null ) {
                assertNull( dval );
            }
            else {
                assertNotNull( dval );
                assertEquals( value, dval.getValue() );
            }
        }
    }
}
