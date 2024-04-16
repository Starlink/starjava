package uk.ac.starlink.util;

import java.util.Date;

public class BeanConfigTest extends TestCase {

    public void testDate() throws ReflectiveOperationException, LoadException {
        Date now = createObj( "java.util.Date", Date.class );
        assertFalse( now.getYear() == 100 );
        Date t2k = createObj( "java.util.Date(year=100)", Date.class );
        assertEquals( 100, t2k.getYear() );
    }

    public void testTBean() throws ReflectiveOperationException, LoadException {
        TBean tb0 = createTBean( "()" );
        assertEquals( 0, tb0.ival_ );
        assertEquals( 0.0, tb0.dval_ );
        assertEquals( null, tb0.primary_ );
        assertEquals( null, tb0.text_ );

        TBean tb1 = createTBean( "(ival=23,primary= BLUE, text=elder thing)" );
        assertEquals( 23, tb1.ival_ );
        assertEquals( Primary.BLUE, tb1.primary_ );
        assertEquals( "elder thing", tb1.text_ );

        assertEquals( 42, createTBean( "(ival=0x2a)" ).ival_ );
        assertEquals( Primary.GREEN,
                      createTBean( "(ival=09,primary=Green)" ).primary_ );
        assertNull( createTBean( "(primary=)" ).primary_ );

        assertEquals( Dir.LEFT, createTBean( "( dir = LEFT )" ).dir_ );
        assertEquals( TBean.NORTH, createTBean( "(dir=NORTH)" ).dir_ );
        assertEquals( TBean.NORTH, createTBean( "(dir=North)" ).dir_ );

        assertEquals( Dir.LEFT, createTBean( "(dir=links)" ).dir_ );
        assertEquals( Dir.RIGHT, createTBean( "(dir=droite)" ).dir_ );
        try {
            createTBean( "(dir=sideways)" );
            fail();
        }
        catch ( LoadException e ) {
            assertTrue( e.getMessage().toLowerCase().indexOf( "usage" ) > 0 );
        }

        assertEquals( null, createTBean( "(dir=)" ).dir_ );

        assertFalse( createTBean( "()" ).flag_ );
        assertFalse( createTBean( "()" ).flagObj_.booleanValue() );
        assertTrue( createTBean( "(flag=true)" ).flag_ );
        assertTrue( createTBean( "(flagObj=True)" ).flagObj_ );
        assertNull( createTBean( "(flagObj=null)" ).flagObj_ );
        assertNull( createTBean( "(flagObj=)" ).flagObj_ );
    }

    private static Object createObj( String txt )
            throws ReflectiveOperationException, LoadException {
        BeanConfig config = BeanConfig.parseSpec( txt );
        Class<?> clazz = Class.forName( config.getBaseText() );
        Object target = clazz.newInstance();
        config.configBean( target );
        return target;
    }

    private static <T> T createObj( String txt, Class<T> clazz )
            throws ReflectiveOperationException, LoadException {
        return clazz.cast( createObj( txt ) );
    }

    private static TBean createTBean( String arglist )
            throws ReflectiveOperationException, LoadException {
        return createObj( TBean.class.getName() + arglist, TBean.class );
    }

    static class TBean {

        int ival_;
        double dval_;
        String text_;
        boolean flag_;
        Boolean flagObj_ = Boolean.FALSE;
        Primary primary_;
        Dir dir_;

        public static final Dir NORTH = new Dir();
        public static final Dir SOUTH = new Dir();

        public void setIval( int ival ) {
            ival_ = ival;
        }
        public void setDval( double dval ) {
            dval_ = dval;
        }
        public void setText( String text ) {
            text_ = text;
        }
        public void setFlag( boolean flag ) {
            flag_ = flag;
        }
        public void setFlagObj( Boolean flagObj ) {
            flagObj_ = flagObj;
        }
        public void setPrimary( Primary primary ) {
            primary_ = primary;
        }
        public void setDir( Dir dir ) {
            dir_ = dir;
        }

        public static Dir toDirInstance( String txt ) {
            if ( "gauche".equals( txt ) ) {
                return Dir.LEFT;
            }
            else if ( "droite".equals( txt ) ) {
                return Dir.RIGHT;
            }
            else {
                return null;
            }
        }
    }

    private enum Primary { RED, BLUE, GREEN };

    static class Dir {
        public static final Dir LEFT = new Dir();
        public static final Dir RIGHT = new Dir();
        public static Dir valueOf( String txt ) {
            if ( "links".equals( txt ) ) {
                return LEFT;
            }
            else if ( "recht".equals( txt ) ) {
                return RIGHT;
            }
            else {
                return null;
            }
        }
    }
}
