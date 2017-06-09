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
import java.util.logging.Logger;
import junit.framework.TestCase;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.URLDataSource;

public class CoosysTest extends TestCase {

    private final StoragePolicy storage_;
    private final StarTableFactory tfact_;

    public CoosysTest() {
        storage_ = StoragePolicy.PREFER_MEMORY;
        tfact_ = new StarTableFactory();
        tfact_.setStoragePolicy( storage_ );
        Logger.getLogger( "uk.ac.starlink" )
              .setLevel( Level.WARNING );
    }

    public void testRewrite() throws IOException, SAXException {
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

        checkMeta( roundTrip( t1, new VOTableWriter(), new VOTableBuilder() ) );
        checkMeta( roundTrip( t2, new FitsPlusTableWriter(),
                                  new FitsPlusTableBuilder() ) );
        checkMeta( roundTripFile( t1, new ColFitsPlusTableWriter(),
                                      new ColFitsPlusTableBuilder() ) );
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
    }

    private class AuxMeta {
        final Map<String,ColumnInfo> metaMap_;
        AuxMeta( StarTable table ) {
            metaMap_ = new HashMap<String,ColumnInfo>();
            for ( int ic = 0; ic < table.getColumnCount(); ic++ ) {
                ColumnInfo cinfo = table.getColumnInfo( ic );
                metaMap_.put( cinfo.getName(), cinfo );
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
                assertEquals( value, dval.getValue() );
            }
        }
    }
}
