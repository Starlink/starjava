package uk.ac.starlink.votable;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.TestCase;
import uk.ac.starlink.votable.datalink.ServiceDescriptor;
import uk.ac.starlink.votable.datalink.ServiceParam;

public class ServiceDescriptorTest extends TestCase {
 
    final StarTableFactory tfact_;

    public ServiceDescriptorTest() {
        tfact_ = new StarTableFactory();
        tfact_.setStoragePolicy( StoragePolicy.PREFER_MEMORY );
        LogUtils.getLogger( "uk.ac.starlink.fits" ).setLevel( Level.WARNING );
    }

    public void testTable() throws IOException, SAXException {
        URL votloc = getClass().getResource( "caom1.vot" );
        VOElement vodoc = new VOElementFactory( StoragePolicy.PREFER_MEMORY )
                         .makeVOElement( votloc );
        TableElement tabEl =
            (TableElement) vodoc.getElementsByVOTagName( "TABLE" ).item( 0 );
        VOStarTable table = new VOStarTable( tabEl );
        checkServiceDescriptor( table );

        checkServiceDescriptor( SerializerTest
                               .roundTrip( table,
                                           new VOTableWriter(),
                                           new VOTableBuilder() ) );
        checkServiceDescriptor( SerializerTest
                               .roundTrip( table,
                                           new FitsPlusTableWriter(),
                                           new FitsPlusTableBuilder() ) );
        checkServiceDescriptor( SerializerTest
                               .roundTrip( table, 
                                           new ColFitsPlusTableWriter(),
                                           new ColFitsPlusTableBuilder() ) );

        /* This isn't good, but it's true: when streaming the document,
         * post-TABLE-element metadata is ignored. */
        StarTable streamedTable = tfact_.makeStarTable( votloc.openStream(),
                                                        new VOTableBuilder() );
        assertEquals( 0, getServiceDescriptors( streamedTable ).length );
    }

    private static ServiceDescriptor[]
            getServiceDescriptors( StarTable table ) {
        List<ServiceDescriptor> sdList = new ArrayList<ServiceDescriptor>();
        for ( DescribedValue dval : table.getParameters() ) {
            Object value = dval.getValue();
            if ( value instanceof ServiceDescriptor ) {
                sdList.add( (ServiceDescriptor) value );
            }
        }
        return sdList.toArray( new ServiceDescriptor[ 0 ] );
    }

    private void checkServiceDescriptor( StarTable table ) {
        assertEquals( 9, table.getColumnCount() );
        assertEquals( 6, table.getRowCount() );
        assertEquals( "ID", table.getColumnInfo( 0 ).getName() );

        ServiceDescriptor[] sds = getServiceDescriptors( table );
        assertEquals( 2, sds.length );

        ServiceDescriptor sd1 = sds[ 0 ];
        ServiceDescriptor sd2 = sds[ 1 ];
        assertEquals( "soda-54fafffb-8cac-4278-b968-555b5496ab9f",
                      sd1.getDescriptorId() );
        assertEquals( "ivo://cadc.nrc.ca/caom2ops",
                      sd1.getResourceIdentifier() );
        assertEquals( "http://www.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/"
                      + "caom2ops/sync",
                      sd1.getAccessUrl() );

        ServiceParam[] params1 = sd1.getInputParams();
        assertEquals( 4, params1.length );
        ServiceParam idParam = params1[ 0 ];
        assertEquals( "ID", idParam.getName() );
        assertNull( idParam.getMinMax() );
        assertNull( idParam.getOptions() );
        assertArrayEquals( new int[] { -1 }, idParam.getArraysize() );
        assertTrue( Tables.isBlank( idParam.getUcd() ) );
        assertNull( idParam.getUtype() );
        assertNull( idParam.getXtype() );
        assertEquals( "ad:CFHT/1742833o.fits.fz", idParam.getValue() );

        ServiceParam circParam = params1[ 2 ];
        assertEquals( "CIRCLE", circParam.getName() );
        assertEquals( "double", circParam.getDatatype() );
        assertArrayEquals( new int[] { 3 }, circParam.getArraysize() );
        assertEquals( "pos.eq;obs.field", circParam.getUcd() );
        assertEquals( "deg", circParam.getUnit() );
        assertEquals( "circle", circParam.getXtype() );
        assertEquals( 0, circParam.getOptions().length );
        assertNull( null, circParam.getMinMax()[ 0 ] );
        assertNull( circParam.getValue() );
        assertEquals( "21.146975243848086 -33.923782327952054 "
                    + "0.2533289119106916",
                      circParam.getMinMax()[ 1 ] );
    }
}
