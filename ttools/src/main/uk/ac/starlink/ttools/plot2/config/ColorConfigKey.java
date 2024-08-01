package uk.ac.starlink.ttools.plot2.config;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.starlink.ttools.gui.ColorComboBox;

/**
 * ConfigKey for selecting colours.
 * A null colour is optionally available, controlled by a toggle switch.
 *
 * <p>Some of the colours come from Paul Tol's colour scheme notes;
 * see <a href="https://personal.sron.nl/~pault/">Paul Tol's Notes</a> page
 * and <a href="https://personal.sron.nl/~pault/colourschemes.pdf"
 *        >SRON/EPS/TN/09-002</a>.
 * The version of the Tech Note used here is dated 29 December 2012.
 *
 * @author   Mark Taylor
 * @since    9 Sep 2014
 */
public class ColorConfigKey extends ChoiceConfigKey<Color> {

    private final boolean allowHide_;

    // These present some alternative colour lists; currently only classic
    // is actually used.
    public static final Map<String,Color> SRON5_COLORS =
        Collections.unmodifiableMap( createSron5Colors() );
    public static final Map<String,Color> SRON7_COLORS =
        Collections.unmodifiableMap( createSron7Colors() );
    public static final Map<String,Color> SRONBRIGHT_COLORS =
        Collections.unmodifiableMap( createSronBrightColors() );
    public static final Map<String,Color> CLASSIC_COLORS =
        Collections.unmodifiableMap( createClassicColors() );

    /**
     * Default list of named colours for use with this config key.
     * Includes entries for all the <code>COLORNAME_</code> keys.
     * Could use one of the other colour maps.
     */
    private static final Map<String,Color> STANDARD_COLORS = CLASSIC_COLORS;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2.config" );

    /** Standard colour name for red. */
    public static final String COLORNAME_RED = "red";

    /** Standard colour name for black. */
    public static final String COLORNAME_BLACK = "black";

    /** Standard colour name for grey. */
    public static final String COLORNAME_GREY = "grey";

    /** Standard colour name for light grey. */
    public static final String COLORNAME_LIGHTGREY = "light_grey";

    private static final Pattern RGB_REGEX =
        Pattern.compile( "(?:0x|#|)([0-9a-fA-F]{6})" );
    private static final NamedColorSet NAMED_COLORS = NamedColorSet.CSS;

    /**
     * Constructs a config key using the default colour option list.
     *
     * <p>The supplied <code>dfltName</code> names one of the colours in the
     * default map.  The static final <code>COLORNAME_*</code> members
     * are guaranteed to be represented.
     * If null is supplied, a sensible default (the first in the list)
     * is used.
     *
     * @param  meta  metadata
     * @param  dfltName   name of default colour, or null
     * @param  allowHide  true if hiding the colour, which results in a null
     *                    value, is a legal option
     */
    public ColorConfigKey( ConfigMeta meta, String dfltName,
                           boolean allowHide ) {
        this( meta, dfltName, allowHide, STANDARD_COLORS );
    }

    /**
     * Constructs a config key using a supplied colour option list.
     *
     * @param  meta  metadata
     * @param  dfltName   name of default colour;
     *                    should be one of the keys in <code>colorOpts</code>
     *                    or null
     * @param  allowHide  true if hiding the colour, which results in a null
     *                    value, is a legal option
     * @param  colorOpts  name-&gt; colour map
     */
    @SuppressWarnings("this-escape")
    public ColorConfigKey( ConfigMeta meta, String dfltName,
                           boolean allowHide, Map<String,Color> colorOpts ) {
        super( meta, Color.class, getColorByName( colorOpts, dfltName ),
               allowHide );
        allowHide_ = allowHide;
        getOptionMap().putAll( colorOpts );
    }

    public Color decodeString( String sval ) {
        return decodeColorName( sval );
    }

    public String stringifyValue( Color color ) {
        return String.format( "%06x", color.getRGB() & 0x00ffffff );
    }

    public Specifier<Color> createSpecifier() {
        Color[] colors = getOptionMap().values().toArray( new Color[ 0 ] );
        List<Specifier<Color>> specifiers = new ArrayList<Specifier<Color>>();
        specifiers.add( new ComboBoxSpecifier<Color>(
                            Color.class,
                            new ColorComboBox( colors ) ) );
        specifiers.add( new ChooserColorSpecifier( colors[ 0 ] ) );
        Specifier<Color> basic =
            new MultiSpecifierPanel<Color>( false, colors[ 0 ], specifiers );
        return allowHide_
             ? new ToggleSpecifier<Color>( basic, null, "Hide" )
             : basic;
    }

    /**
     * Returns a metadata object suitable for use with a ColorConfigKey.
     * The standard colour set is used.
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
        for ( String name : STANDARD_COLORS.keySet() ) {
            if ( nameList.length() > 0 ) {
                nameList.append( ", " );
            }
            nameList.append( "<code>" )
                    .append( name )
                    .append( "</code>" );
        }
        meta.setXmlDescription( new String[] {
            "<p>The color of " + theItem + ",",
            "given by name or as a hexadecimal RGB value.",
            "</p>",
            "<p>The standard plotting colour names are",
            nameList.toString() + ".",
            "However, many other common colour names (too many to list here)",
            "are also understood.",
            "The list currently contains those colour names understood",
            "by most web browsers,",
            "from <code>AliceBlue</code> to <code>YellowGreen</code>,",
            "listed e.g. in the",
            "<em>Extended color keywords</em> section of",
            "the <webref url='https://www.w3.org/TR/css-color-3/#svg-color'"
                      + ">CSS3</webref> standard.",
            "</p>",
            "<p>Alternatively, a six-digit hexadecimal number <em>RRGGBB</em>",
            "may be supplied,",
            "optionally prefixed by \"<code>#</code>\" or \"<code>0x</code>\",",
            "giving red, green and blue intensities,",
            "e.g.  \"<code>ff00ff</code>\", \"<code>#ff00ff</code>\"",
            "or \"<code>0xff00ff</code>\" for magenta.",
            "</p>",
        } );
        return meta;
    }

    /**
     * Turns a string into a colour by looking at known colour names
     * or using RRGGBB syntax.
     *
     * @param  sval   string colour identifier
     * @return  colour named by sval, or null if none is identified
     */
    public static Color decodeColorName( String sval ) {
        Matcher rgbMatcher = RGB_REGEX.matcher( sval );
        if ( rgbMatcher.matches() ) {
            int rgb = Integer.parseInt( rgbMatcher.group( 1 ), 16 );
            return new Color( rgb );
        }
        Color named = NAMED_COLORS.getColor( sval );
        if ( named != null ) {
            return named;
        }
        return null;
    }

    /**
     * Returns an array of the colour options suitable for plotting
     * normal markers.
     *
     * @return  colour option array
     */
    public static Color[] getPlottingColors() {
        Map<String,Color> map =
            new LinkedHashMap<String,Color>( STANDARD_COLORS );
        map.remove( COLORNAME_LIGHTGREY );
        map.remove( COLORNAME_BLACK );
        for ( Iterator<Color> it = map.values().iterator(); it.hasNext(); ) {
            if ( Color.WHITE.equals( it.next() ) ) {
                it.remove();
            }
        }
        return map.values().toArray( new Color[ 0 ] );
    }

    /**
     * Retrieves a colour in the standard map by its name.
     * If one of the <code>COLORNAME_*</code> static members is used
     * as the name, the colour ought to be present.  If the requested
     * colour can't be found, a warning is issued and some other colour
     * is returned.
     *
     * @param  map  name-&gt;colour map
     * @param  name   colour name, or null to pick any old colour
     * @return  non-null colour (as long as the supplied map isn't empty)
     */
    private static Color getColorByName( Map<String,Color> map,
                                         String name ) {
        if ( map.containsKey( name ) ) {
            return map.get( name );
        }
        else {
            Map.Entry<String,Color> entry = map.entrySet().iterator().next();
            if ( name != null ) {
                logger_.warning( "Unknown colour \"" + name + "\""
                               + " - use default \"" + entry.getKey() + "\"" );
            }
            return entry.getValue();
        }
    }

    /**
     * Returns the default plotting colours used by TOPCAT, at least in
     * early versions.
     *
     * @return  name-&gt;colour map
     */
    public static Map<String,Color> createClassicColors() {
        Map<String,Color> map = new LinkedHashMap<String,Color>();
        map.put( COLORNAME_RED, new Color( 0xf00000 ) );
        map.put( "blue", new Color( 0x0000f0 ) );
        map.put( "green", Color.green.darker() );
        map.put( COLORNAME_GREY, Color.gray );
        map.put( "magenta", Color.magenta );
        map.put( "cyan", Color.cyan.darker() );
        map.put( "orange", Color.orange );
        map.put( "pink", Color.pink );
        map.put( "yellow", Color.yellow );
        map.put( COLORNAME_BLACK, Color.black );
        map.put( COLORNAME_LIGHTGREY, Color.lightGray );
        map.put( "white", Color.white );
        return map;
    }

    /**
     * Returns a group of colours using the second (5-colour) row 
     * of figure 13 in SRON/EPS/TN/09-002.
     *
     * @return  name-&gt;colour map
     */
    public static Map<String,Color> createSron5Colors() {
        Map<String,Color> map = new LinkedHashMap<String,Color>();
        map.put( COLORNAME_RED, new Color( 0xd92120 ) );
        map.put( "orange", new Color( 0xe39c37 ) );
        map.put( "green", new Color( 0x7db874 ) );
        map.put( "blue", new Color( 0x529db7 ) );
        map.put( "indigo", new Color( 0x404096 ) );
        map.put( COLORNAME_GREY, Color.GRAY );
        map.put( COLORNAME_BLACK, Color.BLACK );
        map.put( COLORNAME_LIGHTGREY, Color.LIGHT_GRAY );
        return map;
    }

    /**
     * Returns a group of colours using the fourth (7-colour) row 
     * of figure 13 in SRON/EPS/TN/09-002.
     *
     * @return  name-&gt;colour map
     */
    public static Map<String,Color> createSron7Colors() {
        Map<String,Color> map = new LinkedHashMap<String,Color>();
        map.put( COLORNAME_RED, new Color( 0xd92120 ) );
        map.put( "blue", new Color( 0x539eb6 ) );
        map.put( "green", new Color( 0x6db388 ) );
        map.put( "yellow", new Color( 0xcab843 ) );
        map.put( "orange", new Color( 0xe78532 ) );
        map.put( "indigo", new Color( 0x3f60ae ) );
        map.put( "violet", new Color( 0x781c81 ) );
        map.put( COLORNAME_GREY, new Color( 0x808080 ) );
        map.put( COLORNAME_BLACK, Color.BLACK );
        map.put( COLORNAME_LIGHTGREY, new Color( 0xc0c0c0 ) );
        return map;
    }

    /**
     * Returns a group of colours based on the "Alternative Colour Scheme"
     * on Paul Tol's page, but not in the TechNode.
     * Bright yellow is omitted on the grounds that it's too light.
     *
     * @return  name-&gt;colour map
     */
    public static Map<String,Color> createSronBrightColors() {
        Map<String,Color> map = new LinkedHashMap<String,Color>();
        map.put( COLORNAME_RED, new Color( 0xee3333 ) );
        map.put( "blue", new Color( 0x3366aa ) );
        map.put( "green", new Color( 0x66aa55 ) );
        map.put( "yellow", new Color( 0xcccc55 ) );
        map.put( "purple", new Color( 0x992288 ) );
        map.put( "orange", new Color( 0xee7722 ) );
        map.put( "cyan", new Color( 0x11aa99 ) );
        // map.put( "brightyellow", new Color( 0xffee33 ) );
        map.put( COLORNAME_GREY, new Color( 0x777777 ) );
        map.put( COLORNAME_BLACK, Color.BLACK );
        map.put( COLORNAME_LIGHTGREY, new Color( 0xdddddd ) );
        return map;
    }
}
