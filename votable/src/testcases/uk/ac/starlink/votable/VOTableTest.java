package uk.ac.starlink.votable;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.net.URL;
import javax.xml.transform.dom.DOMSource;
import org.xml.sax.SAXException;
import org.w3c.dom.NodeList;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.TestCase;

public class VOTableTest extends TestCase {

    public VOTableTest( String name ) {
        super( name );
        LogUtils.getLogger( "uk.ac.starlink.table.storage" )
                .setLevel( Level.WARNING );
    }

    public void testTable() throws SAXException, IOException {
        checkTable( StoragePolicy.PREFER_MEMORY );
        checkTable( StoragePolicy.PREFER_DISK );
        checkTable( StoragePolicy.DISCARD );
    }

    private void checkTable( StoragePolicy policy ) 
            throws SAXException, IOException {
        URL votloc = getClass().getResource( "docexample.xml" );
        VOElement vot = new VOElementFactory( policy ).makeVOElement( votloc );

        VOElement defs = vot.getChildByName( "DEFINITIONS" );
        VOElement coosys = defs.getChildByName( "COOSYS" );
        assertTrue( coosys.hasAttribute( "equinox" ) );
        assertEquals( "2000.", coosys.getAttribute( "equinox" ) );
        assertEquals( "myJ2000", coosys.getID() );
        assertEquals( "", coosys.getAttribute( "nope" ) );

        TimesysElement timesys =
            (TimesysElement) defs.getChildByName( "TIMESYS" );
        assertTrue( timesys.hasAttribute( "refposition" ) );
        assertEquals( "UTC", timesys.getAttribute( "timescale" ) );
        assertEquals( 2400000.5, timesys.getTimeOrigin() );

        NodeList fieldList = vot.getElementsByVOTagName( "FIELD" );
        assertEquals( 5, fieldList.getLength() );
        FieldElement raEl = (FieldElement) fieldList.item( 1 );
        FieldElement decEl = (FieldElement) fieldList.item( 2 );
        assertEquals( "RA", raEl.getAttribute( "name" ) );
        assertEquals( "Dec", decEl.getAttribute( "name" ) );
        VOElement coosysEl = (VOElement) raEl.getCoosys();
        assertEquals( "COOSYS", coosysEl.getVOTagName() );
        assertEquals( "2000.", coosysEl.getAttribute( "epoch" ) );

        VOElement res = vot.getChildByName( "RESOURCE" );
        VOElement[] params = res.getChildrenByName( "PARAM" );

        ParamElement obsParam = (ParamElement) params[ 0 ];
        String pdesc = obsParam.getDescription();
        assertTrue( pdesc.startsWith( "This parameter is designed" ) );
        String pval = obsParam.getValue();
        String pobj = (String) obsParam.getObject();
        assertEquals( pval, pobj );

        ParamElement epochParam = (ParamElement) params[ 1 ];
        assertEquals( "Epoch", epochParam.getName() );
        double mjdEpoch = ((Double) epochParam.getObject()).doubleValue();
        assertEquals( 54291.25, mjdEpoch );
        TimesysElement tsys = epochParam.getTimesys();
        assertEquals( 2400000.5, tsys.getTimeOrigin() );
        assertEquals( "UTC", tsys.getAttribute( "timescale" ) );
 
        TableElement tab = (TableElement) res.getChildrenByName( "TABLE" )[ 0 ];
        int ncol = tab.getFields().length;
        assertEquals( 5, ncol );
        long nrow = tab.getNrows();

        if ( policy == StoragePolicy.DISCARD ) {
            assertEquals( 0L, nrow );
        }
        else {
            assertEquals( 3L, nrow );

            VOStarTable stab = new VOStarTable( tab );
            assertEquals( tab.getNrows(), stab.getRowCount() );
            assertEquals( tab.getFields().length, stab.getColumnCount() );

            ColumnInfo raInfo = stab.getColumnInfo( 1 );
            ColumnInfo decInfo = stab.getColumnInfo( 2 );
            ColumnInfo timeInfo = stab.getColumnInfo( 4 );
            assertEquals( "RA", raInfo.getName() );
            assertEquals( "Dec", decInfo.getName() );
            assertEquals( "ObsTime", timeInfo.getName() );
            assertEquals( "deg", raInfo.getUnitString() );
            assertEquals( "POS_EQ_DEC", decInfo.getUCD() );
            assertEquals( "2000.",
                          raInfo.getAuxDatumValue( VOStarTable
                                                  .COOSYS_EPOCH_INFO,
                                                   String.class ) );
            assertEquals( "2000.",
                          decInfo.getAuxDatumByName( "CoosysEpoch" )
                                 .getValue() );
            assertEquals( "UTC",
                          timeInfo.getAuxDatumValue( VOStarTable
                                                    .TIMESYS_TIMESCALE_INFO,
                                                     String.class ) );
            assertEquals( "BARYCENTER",
                          timeInfo.getAuxDatumValue( VOStarTable
                                                    .TIMESYS_REFPOSITION_INFO,
                                                     String.class ) );
            assertEquals( "MJD-origin",
                          timeInfo.getAuxDatumByName( "TimesysTimeorigin" )
                                  .getValue() );

            RowSequence rseq = stab.getRowSequence();
            RowSequence rstep = tab.getData().getRowSequence();
            List rows = new ArrayList();
            for ( int ir = 0; ir < nrow; ir++ ) {
                assertTrue( rseq.next() );
                assertTrue( rstep.next() );
                Object[] row = rstep.getRow();
                assertNotNull( row );
                assertEquals( ncol, row.length );
                for ( int ic = 0; ic < ncol; ic++ ) {
                    if ( row[ ic ] == null ||
                         row[ ic ].getClass().getComponentType() == null ) {
                        assertEquals( rseq.getCell( ic ), row[ ic ] );
                    }
                    else if ( Array.getLength( row[ ic ] ) == 1 ) {
                        assertEquals( rseq.getCell( ic ), 
                                      Array.get( row[ ic ], 0 ) );
                    }
                }
                rows.add( row );
            }
            assertTrue( ! rstep.next() );
            assertTrue( ! rseq.next() );
            rseq.close();

            assertEquals( "Procyon",
                          ((String) ((Object[]) rows.get( 0 ))[ 0 ]).trim() );
            assertEquals( "Vega",
                          ((String) ((Object[]) rows.get( 1 ))[ 0 ]).trim() );
            assertEquals( 12, 
                          ((int[]) ((Object[]) rows.get( 0 ))[ 3 ]).length );
            assertEquals( 6,
                          ((int[]) ((Object[]) rows.get( 1 ))[ 3 ]).length );

            Object[] row2 = (Object[]) rows.get( 2 );
            assertEquals( ncol, row2.length );
            for ( int icol = 0; icol < ncol; icol++ ) {
                Object cell = row2[ icol ];
                assertTrue( cell == null ||
                            cell instanceof Number &&
                                Float.isNaN( ((Number) cell).floatValue() ) ||
                            cell instanceof String &&
                                ((String) cell).length() == 0 );
            }

            DescribedValue obsParameter =
                stab.getParameterByName( obsParam.getName() );
            assertTrue( stab.getParameters().contains( obsParameter ) );
            ValueInfo obsInfo = obsParameter.getInfo();
            assertEquals( obsParam.getValue(), obsParameter.getValue() );
            assertEquals( obsParam.getName(), obsInfo.getName() );
            assertEquals( String.class, obsInfo.getContentClass() );
            assertEquals( obsParam.getDescription(), obsInfo.getDescription() );

            DescribedValue epochParameter =
                stab.getParameterByName( epochParam.getName() );
            assertTrue( stab.getParameters().contains( epochParameter ) );
        }
    }
}
