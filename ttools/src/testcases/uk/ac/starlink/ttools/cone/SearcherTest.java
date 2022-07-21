package uk.ac.starlink.ttools.cone;

import java.util.logging.Level;
import java.util.regex.Pattern;
import junit.framework.TestCase;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowListStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.vo.SiaFormatOption;
import uk.ac.starlink.vo.SiaVersion;

public class SearcherTest extends TestCase {

    public void setUp() {
        LogUtils.getLogger( "uk.ac.starlink.ttools.cone" )
                .setLevel( Level.SEVERE );
    }

    public void testSsaGuess() {
        ColumnInfo[] infos = new ColumnInfo[] {
            createCol( "a1", Double.class, "pos.eq.ra;meta.main" ),
            createCol( "a2", Float.class, "pos.eq.dec;meta.main" ),
            createCol( "b1", Double.class, "POS_EQ_RA" ),
            createCol( "b2", Double.class, "POS_EQ_DEC" ),
            createCol( "RA", Double.class, null ),
            createCol( "DEC_J2000", Double.class, null ),
        };
        StarTable table = new RowListStarTable( infos );
        SsaConeSearcher cs =
            new SsaConeSearcher( null, null, false, null, ContentCoding.NONE );

        assertEquals( 0, cs.getRaIndex( table ) );
        assertEquals( 1, cs.getDecIndex( table ) );

        infos[ 0 ].setUnitString( "radians" );
        assertEquals( -1, cs.getRaIndex( table ) );
        infos[ 0 ].setUnitString( "degrees" );
        assertEquals( 0, cs.getRaIndex( table ) );

        infos[ 0 ].setContentClass( String.class );
        infos[ 1 ].setContentClass( Object.class );
        assertEquals( 2, cs.getRaIndex( table ) );
        assertEquals( 3, cs.getDecIndex( table ) );

        infos[ 2 ].setUCD( "POS_EQ_RA_MAIN" );
        infos[ 3 ].setUCD( "POS_EQ_DEC_MAIN" );
        assertEquals( 2, cs.getRaIndex( table ) );
        assertEquals( 3, cs.getDecIndex( table ) );

        infos[ 2 ].setUCD( "POx_EQ_RA_MAIN" );
        infos[ 3 ].setUCD( "POxEQ_DEC_MAIN" );
        assertEquals( 4, cs.getRaIndex( table ) );
        assertEquals( 5, cs.getDecIndex( table ) );

        infos[ 4 ].setName( "c1" );
        infos[ 5 ].setName( "c2" );
        assertEquals( -1, cs.getRaIndex( table ) );
        assertEquals( -1, cs.getDecIndex( table ) );
    }

    public void testDalGuess() {
        workConeSearcherGuess( new ServiceConeSearcher( null, 0,
                                                        false, null ) );
        workConeSearcherGuess( new SiaConeSearcher( (String) null,
                                                    SiaVersion.V10,
                                                    (SiaFormatOption) null,
                                                    false,
                                                    (StarTableFactory) null,
                                                    ContentCoding.NONE ) );
    }

    public void workConeSearcherGuess( ConeSearcher cs ) {
        ColumnInfo[] infos = new ColumnInfo[] {
            createCol( "a1", Double.class, "POS_EQ_RA_MAIN" ),
            createCol( "a2", Double.class, "POS_EQ_DEC_MAIN" ),
            createCol( "b1", Double.class, "pos.eq.ra;meta.main" ),
            createCol( "b2", Float.class, "pos.eq.dec;meta.main" ),
            createCol( "RA", Double.class, null ),
            createCol( "DEC2000", Double.class, null ),
        };
        StarTable table = new RowListStarTable( infos );

        assertEquals( 0, cs.getRaIndex( table ) );
        assertEquals( 1, cs.getDecIndex( table ) );

        infos[ 0 ].setContentClass( String.class );
        infos[ 1 ].setContentClass( Object.class );
        assertEquals( 2, cs.getRaIndex( table ) );
        assertEquals( 3, cs.getDecIndex( table ) );

        infos[ 2 ].setUCD( "POx_EQ_RA_MAIN" );
        infos[ 3 ].setUCD( "POxEQ_DEC_MAIN" );
        assertEquals( 4, cs.getRaIndex( table ) );
        assertEquals( 5, cs.getDecIndex( table ) );

        infos[ 4 ].setName( "c1" );
        infos[ 5 ].setName( "c2" );
        assertEquals( -1, cs.getRaIndex( table ) );
        assertEquals( -1, cs.getDecIndex( table ) );
    }

    private static ColumnInfo createCol( String name, Class clazz,
                                         String ucd ) {
        ColumnInfo info = new ColumnInfo( name, clazz, null );
        info.setUCD( ucd );
        return info;
    }
}
