package uk.ac.starlink.ttools.plot2.config;

import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Defines a set of colours with associated names.
 * This is quite like a map, but the colours can be retrieved with
 * variant keys (modified spellings, case folding etc).
 * A couple of useful instances are supplied.
 *
 * @author   Mark Taylor
 * @since    23 Feb 2017
 */
public abstract class NamedColorSet {

    private final Map<String,Color> origMap_;
    private final Map<String,Color> normMap_;

    private static final Map<String,Color> CSS_MAP = createCssMap();

    /**
     * Standard CSS/SVG/HTML/JS list of colours (140 entries).
     * @see <a href="https://www.w3.org/TR/css3-color/#svg-color"
     *         >CSS Color Module Level 3 standard sec 4.3</a>
     */
    public static final NamedColorSet CSS =
        createCssColorSet( CSS_MAP );

    /** CSS with all the very light colours (except White) removed. */
    public static final NamedColorSet CSS_DARK =
        createCssColorSet( filterDark( CSS_MAP ) );

    /**
     * Constructor.
     *
     * @param   map  name-&gt;color map
     */
    @SuppressWarnings("this-escape")
    public NamedColorSet( Map<String,Color> map ) {
        origMap_ = map;
        normMap_ = new HashMap<String,Color>( map.size() );
        for ( Map.Entry<String,Color> entry : origMap_.entrySet() ) {
            normMap_.put( toKey( entry.getKey() ), entry.getValue() );
        }
    }

    /**
     * Returns the colour map on which this named set is based.
     *
     * @return  map
     */
    public Map<String,Color> getMap() {
        return origMap_;
    }

    /**
     * Returns the colour for a given name.
     * This name is normalised using the {@link #toKey} method before matching.
     *
     * @param  name  approximate name
     * @return   colour, or null
     */
    public Color getColor( String name ) {
        return normMap_.get( toKey( name ) );
    }

    /**
     * Normalises a colour name to turn it into a map key.
     *
     * @param    name  approximate name
     * @return  name used for matching
     */
    public abstract String toKey( String name );

    /**
     * Returns the list of colours defined by section "Extended color keywords"
     * of the CSS3 standard.
     * Actually I downloaded these RGB values from
     * http://www.javascripter.net/faq/colornam.htm,
     * but I think it's the same list.
     * This list is apparently used by standard browsers, javascript engines,
     * SVG etc as well as CSS.
     * The capitalisation is mine.
     *
     * @see <a href="https://www.w3.org/TR/css3-color/#svg-color"
     *         >CSS Color Module Level 3 standard sec 4.3</a>
     */
    private static Map<String,Color> createCssMap() {
        Map<String,Color> map = new LinkedHashMap<String,Color>();
        putColor( map, 0xF0F8FF, "AliceBlue" );
        putColor( map, 0xFAEBD7, "AntiqueWhite" );
        putColor( map, 0x00FFFF, "Aqua" );
        putColor( map, 0x7FFFD4, "Aquamarine" );
        putColor( map, 0xF0FFFF, "Azure" );
        putColor( map, 0xF5F5DC, "Beige" );
        putColor( map, 0xFFE4C4, "Bisque" );
        putColor( map, 0x000000, "Black" );
        putColor( map, 0xFFEBCD, "BlanchedAlmond" );
        putColor( map, 0x0000FF, "Blue" );
        putColor( map, 0x8A2BE2, "BlueViolet" );
        putColor( map, 0xA52A2A, "Brown" );
        putColor( map, 0xDEB887, "Burlywood" );
        putColor( map, 0x5F9EA0, "CadetBlue" );
        putColor( map, 0x7FFF00, "Chartreuse" );
        putColor( map, 0xD2691E, "Chocolate" );
        putColor( map, 0xFF7F50, "Coral" );
        putColor( map, 0x6495ED, "CornflowerBlue" );
        putColor( map, 0xFFF8DC, "Cornsilk" );
        putColor( map, 0xDC143C, "Crimson" );
        putColor( map, 0x00FFFF, "Cyan" );
        putColor( map, 0x00008B, "DarkBlue" );
        putColor( map, 0x008B8B, "DarkCyan" );
        putColor( map, 0xB8860B, "DarkGoldenrod" );
        putColor( map, 0xA9A9A9, "DarkGray" );
        putColor( map, 0x006400, "DarkGreen" );
        putColor( map, 0xBDB76B, "DarkKhaki" );
        putColor( map, 0x8B008B, "DarkMagenta" );
        putColor( map, 0x556B2F, "DarkOliveGreen" );
        putColor( map, 0xFF8C00, "DarkOrange" );
        putColor( map, 0x9932CC, "DarkOrchid" );
        putColor( map, 0x8B0000, "DarkRed" );
        putColor( map, 0xE9967A, "DarkSalmon" );
        putColor( map, 0x8FBC8F, "DarkSeagreen" );
        putColor( map, 0x483D8B, "DarkSlateBlue" );
        putColor( map, 0x2F4F4F, "DarkSlateGray" );
        putColor( map, 0x00CED1, "DarkTurquoise" );
        putColor( map, 0x9400D3, "DarkViolet" );
        putColor( map, 0xFF1493, "DeepPink" );
        putColor( map, 0x00BFFF, "DeepSkyBlue" );
        putColor( map, 0x696969, "DimGray" );
        putColor( map, 0x1E90FF, "DodgerBlue" );
        putColor( map, 0xB22222, "FireBrick" );
        putColor( map, 0xFFFAF0, "FloralWhite" );
        putColor( map, 0x228B22, "ForestGreen" );
        putColor( map, 0xFF00FF, "Fuchsia" );
        putColor( map, 0xDCDCDC, "Gainsboro" );
        putColor( map, 0xF8F8FF, "Ghostwhite" );
        putColor( map, 0xFFD700, "Gold" );
        putColor( map, 0xDAA520, "Goldenrod" );
        putColor( map, 0x808080, "Gray" );
        putColor( map, 0x008000, "Green" );
        putColor( map, 0xADFF2F, "GreenYellow" );
        putColor( map, 0xF0FFF0, "Honeydew" );
        putColor( map, 0xFF69B4, "HotPink" );
        putColor( map, 0xCD5C5C, "IndianRed" );
        putColor( map, 0x4B0082, "Indigo" );
        putColor( map, 0xFFFFF0, "Ivory" );
        putColor( map, 0xF0E68C, "Khaki" );
        putColor( map, 0xE6E6FA, "Lavender" );
        putColor( map, 0xFFF0F5, "LavenderBlush" );
        putColor( map, 0x7CFC00, "LawnGreen" );
        putColor( map, 0xFFFACD, "LemonChiffon" );
        putColor( map, 0xADD8E6, "LightBlue" );
        putColor( map, 0xF08080, "LightCoral" );
        putColor( map, 0xE0FFFF, "LightCyan" );
        putColor( map, 0xFAFAD2, "LightGoldenrodYellow" );
        putColor( map, 0x90EE90, "LightGreen" );
        putColor( map, 0xD3D3D3, "LightGrey" );
        putColor( map, 0xFFB6C1, "LightPink" );
        putColor( map, 0xFFA07A, "LightSalmon" );
        putColor( map, 0x20B2AA, "LightSeagreen" );
        putColor( map, 0x87CEFA, "LightSkyBlue" );
        putColor( map, 0x778899, "LightSlateGray" );
        putColor( map, 0xB0C4DE, "LightSteelBlue" );
        putColor( map, 0xFFFFE0, "LightYellow" );
        putColor( map, 0x00FF00, "Lime" );
        putColor( map, 0x32CD32, "LimeGreen" );
        putColor( map, 0xFAF0E6, "Linen" );
        putColor( map, 0xFF00FF, "Magenta" );
        putColor( map, 0x800000, "Maroon" );
        putColor( map, 0x66CDAA, "MediumAquamarine" );
        putColor( map, 0x0000CD, "MediumBlue" );
        putColor( map, 0xBA55D3, "MediumOrchid" );
        putColor( map, 0x9370DB, "MediumPurple" );
        putColor( map, 0x3CB371, "MediumSeaGreen" );
        putColor( map, 0x7B68EE, "MediumSlateBlue" );
        putColor( map, 0x00FA9A, "MediumSpringGreen" );
        putColor( map, 0x48D1CC, "MediumTurquoise" );
        putColor( map, 0xC71585, "MediumVioletRed" );
        putColor( map, 0x191970, "MidnightBlue" );
        putColor( map, 0xF5FFFA, "MintCream" );
        putColor( map, 0xFFE4E1, "MistyRose" );
        putColor( map, 0xFFE4B5, "Moccasin" );
        putColor( map, 0xFFDEAD, "NavajoWhite" );
        putColor( map, 0x000080, "Navy" );
        putColor( map, 0xFDF5E6, "OldLace" );
        putColor( map, 0x808000, "Olive" );
        putColor( map, 0x6B8E23, "OliveDrab" );
        putColor( map, 0xFFA500, "Orange" );
        putColor( map, 0xFF4500, "OrangeRed" );
        putColor( map, 0xDA70D6, "Orchid" );
        putColor( map, 0xEEE8AA, "PaleGoldenrod" );
        putColor( map, 0x98FB98, "PaleGreen" );
        putColor( map, 0xAFEEEE, "PaleTurquoise" );
        putColor( map, 0xDB7093, "PaleVioletRed" );
        putColor( map, 0xFFEFD5, "PapayaWhip" );
        putColor( map, 0xFFDAB9, "PeachPuff" );
        putColor( map, 0xCD853F, "Peru" );
        putColor( map, 0xFFC0CB, "Pink" );
        putColor( map, 0xDDA0DD, "Plum" );
        putColor( map, 0xB0E0E6, "PowderBlue" );
        putColor( map, 0x800080, "Purple" );
        putColor( map, 0xFF0000, "Red" );
        putColor( map, 0xBC8F8F, "RosyBrown" );
        putColor( map, 0x4169E1, "RoyalBlue" );
        putColor( map, 0x8B4513, "SaddleBrown" );
        putColor( map, 0xFA8072, "Salmon" );
        putColor( map, 0xFAA460, "SandyBrown" );
        putColor( map, 0x2E8B57, "SeaGreen" );
        putColor( map, 0xFFF5EE, "SeaShell" );
        putColor( map, 0xA0522D, "Sienna" );
        putColor( map, 0xC0C0C0, "Silver" );
        putColor( map, 0x87CEEB, "SkyBlue" );
        putColor( map, 0x6A5ACD, "SlateBlue" );
        putColor( map, 0x708090, "SlateGray" );
        putColor( map, 0xFFFAFA, "Snow" );
        putColor( map, 0x00FF7F, "SpringGreen" );
        putColor( map, 0x4682B4, "SteelBlue" );
        putColor( map, 0xD2B48C, "Tan" );
        putColor( map, 0x008080, "Teal" );
        putColor( map, 0xD8BFD8, "Thistle" );
        putColor( map, 0xFF6347, "Tomato" );
        putColor( map, 0x40E0D0, "Turquoise" );
        putColor( map, 0xEE82EE, "Violet" );
        putColor( map, 0xF5DEB3, "Wheat" );
        putColor( map, 0xFFFFFF, "White" );
        putColor( map, 0xF5F5F5, "WhiteSmoke" );
        putColor( map, 0xFFFF00, "Yellow" );
        putColor( map, 0x9ACD32, "YellowGreen" );
        return map;
    }

    /**
     * Filters any entries out of a colour map if they are pretty close
     * to white.  White itself is retained if present.
     *
     * @param  map  input map (not changed)
     * @return   output map (new object)
     */
    private static Map<String,Color> filterDark( Map<String,Color> map ) {
        map = new LinkedHashMap<String,Color>( map );
        for ( Iterator<Color> it = map.values().iterator(); it.hasNext(); ) {
            Color color = it.next();
            if ( color.getRed() > 0xc0 &&
                 color.getGreen() > 0xc0 &&
                 color.getBlue() > 0xc0 &&
                 ! Color.WHITE.equals( color ) ) {
                it.remove();
            }
        }
        return map;
    }

    /**
     * Adds a name-&gt;colour entry to a given map.
     *
     * @param   map   map to extend
     * @param   rgb   RRGGBB integer value
     * @param   name  colour name
     */
    private static void putColor( Map<String,Color> map, int rgb,
                                  String name ) {
        map.put( name, new Color( rgb ) );
    }

    /**
     * Returns a NamedColorSet using CSS key normalisation rules.
     *
     * @param  map   canonicalName-&gt;color map
     */
    private static NamedColorSet createCssColorSet( Map<String,Color> map ) {
        return new NamedColorSet( Collections.unmodifiableMap( map ) ) {
            public String toKey( String name ) {
                if ( name != null ) {
                    name = name.toLowerCase()
                               .replaceAll( "[ _-]", "" )
                               .replaceAll( "grey", "gray" );
                }
                return name;
            }
        };
    }
}
