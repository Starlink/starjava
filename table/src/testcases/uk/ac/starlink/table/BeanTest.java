package uk.ac.starlink.table;

import java.io.IOException;
import junit.framework.TestCase;

public class BeanTest extends TestCase {

    public BeanTest( String name ) {
        super( name );
    }

    public void testBean() throws Exception {
        BeanStarTable st = new BeanStarTable( ABean.class );
        assertEquals( 2, st.getColumnCount() );
        assertEquals( 0L, st.getRowCount() );
        st.setData( new ABean[] {
            new ABean( 1, "One" ),
            new ABean( 2, "Two" ),
            new ABean( 3, "Three" ),
        } );
        assertEquals( 2, st.getColumnCount() );
        assertEquals( 3L, st.getRowCount() );
        assertTrue( st.isRandom() );
        assertEquals( Integer.valueOf( 3 ), st.getCell( 2L, 0 ) );
        assertEquals( "One", st.getCell( 0L, 1 ) );

        try {
            st.setData( new Object[ 0 ] );
            fail();
        }
        catch ( ClassCastException e ) {
        }
    }

    public static class ABean {
        int num_;
        String word_;
        public ABean( int num, String word ) {
            num_ = num;
            word_ = word;
        }
        public int getNumber() {
            return num_;
        }
        public String getWord() {
            return word_;
        }
    }
}
