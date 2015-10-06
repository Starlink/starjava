package uk.ac.starlink.ttools.plot2.config;

import java.awt.Color;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import uk.ac.starlink.ttools.gui.ColorComboBox;

/**
 * ConfigKey for selecting colours.
 * A null colour is optionally available, controlled by a toggle switch.
 *
 * @author   Mark Taylor
 * @since    9 Sep 2014
 */
public class ColorConfigKey extends ChoiceConfigKey<Color> {

    private final boolean allowHide_;

    private static final Map<String,Color> FIXED_MAP = createColorMap();

    /**
     * Constructor.
     *
     * @param  meta  metadata
     * @param  dflt  default value
     * @param  allowHide  true if hiding the colour, which results in a null
     *                    value, is a legal option
     */
    public ColorConfigKey( ConfigMeta meta, Color dflt,
                           boolean allowHide ) {
        super( meta, Color.class, dflt, allowHide );
        allowHide_ = allowHide;
        getOptionMap().putAll( FIXED_MAP );
    }

    public Color decodeString( String sval ) {
        final int rgb;
        try {
            rgb = Integer.parseInt( sval, 16 );
        }
        catch ( NumberFormatException e ) {
            return null;
        }
        return new Color( rgb );
    }

    public String stringifyValue( Color color ) {
        return String.format( "%06x", color.getRGB() & 0x00ffffff );
    }

    public Specifier<Color> createSpecifier() {
        Color[] colors = getOptionMap().values().toArray( new Color[ 0 ] );
        Specifier<Color> basic =
            new ComboBoxSpecifier<Color>( new ColorComboBox( colors ) );
        return allowHide_
             ? new ToggleSpecifier<Color>( basic, null, "Hide" )
             : basic;
    }

    /**
     * Returns a metadata object suitable for use with a ColorConfigKey.
     *
     * @param  shortName  key name for use in command-line interface
     * @param  longName  key name for use in GUI
     * @param  theItem   description of the item to use in free-form text,
     *                   for instance "the plot grid"
     * @return  colour config metadata
     */
    public static ConfigMeta createColorMeta( String shortName, String longName,
                                              String theItem ) {
        ConfigMeta meta = new ConfigMeta( shortName, longName );
        meta.setStringUsage( "<rrggbb>|red|blue|..." );
        meta.setShortDescription( "Color of " + theItem );
        StringBuffer nameList = new StringBuffer();
        for ( String name : FIXED_MAP.keySet() ) {
            if ( nameList.length() > 0 ) {
                nameList.append( ", " );
            }
            nameList.append( "<code>" )
                    .append( name )
                    .append( "</code>" );
        }
        meta.setXmlDescription( new String[] {
            "<p>The color of " + theItem + ".",
            "</p>",
            "<p>The value may be a six-digit hexadecimal number",
            "giving red, green and blue intensities,",
            " e.g.  \"<code>ff00ff</code>\" for magenta.",
            "Alternatively it may be the name of one of the",
            "pre-defined colors.",
            "These are currently",
            nameList.toString() + ".",
            "</p>",
        } );
        return meta;
    }

    /**
     * Returns a map of known colours by name.
     *
     * @return  name->colour map
     */
    private static Map<String,Color> createColorMap() {
        Map<String,Color> map = new LinkedHashMap<String,Color>();
        map.put( "red", new Color( 0xf00000 ) );
        map.put( "blue", new Color( 0x0000f0 ) );
        map.put( "green", Color.green.darker() );
        map.put( "grey", Color.gray );
        map.put( "magenta", Color.magenta );
        map.put( "cyan", Color.cyan.darker() );
        map.put( "orange", Color.orange );
        map.put( "pink", Color.pink );
        map.put( "yellow", Color.yellow );
        map.put( "black", Color.black );
        map.put( "white", Color.white );
        return Collections.unmodifiableMap( map );
    }
}
