package uk.ac.starlink.ttools.plot2.config;

import java.awt.Color;
import java.util.logging.Level;
import junit.framework.TestCase;
import uk.ac.starlink.util.LogUtils;

public class ColorTest extends TestCase {

    public ColorTest() {
        LogUtils.getLogger( "uk.ac.starlink.ttools.plot2" )
                .setLevel( Level.WARNING );
    }

    public void testConfigKey() throws ConfigException {
        ConfigKey<Color> key = StyleKeys.COLOR;
        assertEquals( new Color( 0x00DEAD ), key.stringToValue( "00dead" ) );
        assertEquals( new Color( 0xF00F00 ), key.stringToValue( "#F00F00" ) );
        assertEquals( Color.BLACK, key.stringToValue( "0x000000" ) );
        assertEquals( new Color( 0xFF69B4 ), key.stringToValue( "hotpink" ) );
        assertEquals( new Color( 0x87CEEB ), key.stringToValue( "Sky Blue" ) );
        assertEquals( new Color( 0xF0F8FF ),
                      key.stringToValue( "alice-blue" ) );

        try {
            key.stringToValue( "Hooloovooloo" );
            fail();
        }
        catch ( ConfigException e ) {
        }

        try {
            key.stringToValue( "0xaabbccd" );
            fail();
        }
        catch ( ConfigException e ) {
        }
    }

    public void testNamedColorSet() {
        NamedColorSet css = NamedColorSet.CSS;
        NamedColorSet dark = NamedColorSet.CSS_DARK;
        assertEquals( 140, css.getMap().size() );
        assertEquals( new Color( 0x008080 ), css.getColor( "TEAL" ) );
        assertEquals( new Color( 0x008080 ), dark.getColor( "TEAL" ) );
        assertNull( css.getColor( "Octarine" ) );
        assertEquals( new Color( 0xF5F5DC ), css.getColor( "beige" ) );
        assertNull( dark.getColor( "beige" ) );
    }

}
