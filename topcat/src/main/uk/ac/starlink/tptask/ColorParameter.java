package uk.ac.starlink.tptask;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;

/**
 * Parameter for selecting a colour.
 *
 * @author   Mark Taylor
 * @since    8 Aug 2008
 */
public class ColorParameter extends Parameter {

    private Color colorValue_;

    /** Colours suitable for normal usage in plotting. */
    private static final NamedColor[] STANDARD_COLORS = new NamedColor[] {
        new NamedColor( "red", new Color( 0xf00000 ) ),
        new NamedColor( "blue", new Color( 0x0000f0 ) ),
        new NamedColor( "green", Color.green.darker() ),
        new NamedColor( "grey", Color.gray ),
        new NamedColor( "magenta", Color.magenta ),
        new NamedColor( "cyan", Color.cyan.darker() ),
        new NamedColor( "orange", Color.orange ),
        new NamedColor( "pink", Color.pink ),
        new NamedColor( "yellow", Color.yellow ),
        new NamedColor( "black", Color.black ),
    };

    /** Other known named colours. */
    private static final NamedColor[] MORE_COLORS = new NamedColor[] {
        new NamedColor( "white", Color.white ),
    };

    /** Colour name -> Color map. */
    private static final Map COLOR_MAP = createColorMap();

    /**
     * Constructor.
     *
     * @param  name  parameter name
     */
    public ColorParameter( String name ) {
        super( name );
        setUsage( "<rrggbb>|red|blue|..." );
    }

    /**
     * Returns an XML string, suitable for inclusion in a parameter description,
     * which explains the format of values accepted by this parameter.
     * The returned string is not enclosed in a &lt;p&gt; element.
     *
     * @return   format description XML string
     */
    public static String getFormatDescription() {
        StringBuffer sbuf = new StringBuffer()
            .append( "The value may be a 6-digit hexadecimal number giving\n" )
            .append( "red, green and blue intensities,\n" )
            .append( "e.g. <code>ff00ff</code> for magenta.\n" )
            .append( "Alternatively it may be the name of one of the\n" )
            .append( "pre-defined colours.\n" )
            .append( "These are currently\n" );
        NamedColor[] knownColors = getKnownColors();
        for ( int i = 0; i < knownColors.length; i++ ) {
            if ( i == knownColors.length - 1 ) {
                sbuf.append( " and " );
            }
            else if ( i > 0 ) {
                sbuf.append( ", " );
            }
            sbuf.append( knownColors[ i ] );
        }
        sbuf.append( "." );
        return sbuf.toString();
    }

    public void setValueFromString( Environment env, String sval ) 
            throws TaskException {
        String key = sval.toLowerCase();
        Color color;
        if ( COLOR_MAP.containsKey( key ) ) {
            color = (Color) COLOR_MAP.get( key );
        }
        else if ( sval.matches( "[0-9a-fA-F]{6}" ) ) {
            color = new Color( Integer.parseInt( sval, 16 ) );
        }
        else {
            throw new ParameterValueException( this, "Not known colour name or "
                                                   + "6-digit hex string" );
        }
        colorValue_ = color;
        super.setValueFromString( env, sval );
    }

    /**
     * Returns the value of this parameter as a Color object.
     *
     * @param  env  execution environment
     */
    public Color colorValue( Environment env ) throws TaskException {
        checkGotValue( env );
        return colorValue_;
    }

    /**
     * Sets the default value of this parameter to a given color object.
     *
     * @param  color   colour
     */
    public void setDefaultColor( Color color ) {
        setDefault( getStringValue( color ) );
    }

    /**
     * Returns a string representation (suitable for feeding to this parameter)
     * of a given colour.
     *
     * @param  color   colour
     * @return   string representation of colour
     */
    private static String getStringValue( Color color ) {
        NamedColor[] knownColors = getKnownColors();
        for ( int i = 0; i < knownColors.length; i++ ) {
            NamedColor namedColor = knownColors[ i ];
            if ( color.equals( namedColor.color_ ) ) {
                return namedColor.name_;
            }
        }
        return Integer.toString( color.getRGB() & 0x00ffffff, 16 );
    }

    /**
     * Returns all colours known by name by this class.
     *
     * @return  named colour array
     */
    private static NamedColor[] getKnownColors() {
        List list = new ArrayList();
        list.addAll( Arrays.asList( STANDARD_COLORS ) );
        list.addAll( Arrays.asList( MORE_COLORS ) );
        return (NamedColor[]) list.toArray( new NamedColor[ 0 ] );
    }

    /**
     * Constructs a mapping from colour name to colour.
     *
     * @return   name->color map
     */
    private static Map createColorMap() {
        NamedColor[] namedColors = getKnownColors();
        Map map = new HashMap( namedColors.length );
        for ( int i = 0; i < namedColors.length; i++ ) {
            NamedColor color = (NamedColor) namedColors[ i ];
            map.put( color.name_.toLowerCase(), color.color_ );
        }
        return map;
    }

    /**
     * Utility class which associates a name with a colour.
     */
    private static class NamedColor {
        final String name_;
        final Color color_;

        /**
         * Constructor.
         *
         * @param  name   colour name
         * @param  color  colour
         */
        NamedColor( String name, Color color ) {
            name_ = name;
            color_ = color;
        }

        public String toString() {
            return name_;
        }
    }
}
