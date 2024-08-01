package uk.ac.starlink.ttools.plot2.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import uk.ac.starlink.ttools.gui.DashComboBox;

/**
 * ConfigKey for selecting line dash types.
 *
 * @author   Mark Taylor
 * @since    9 Sep 2014
 * @see      java.awt.BasicStroke#getDashArray
 */
public class DashConfigKey extends ChoiceConfigKey<float[]> {

    private static final Map<String,float[]> FIXED_MAP = createDashMap();

    /**
     * Constructor.
     *
     * @param  meta  key metadata
     */
    @SuppressWarnings("this-escape")
    public DashConfigKey( ConfigMeta meta ) {
        super( meta, float[].class, null, true );
        getOptionMap().putAll( FIXED_MAP );
    }

    public float[] decodeString( String sval ) {
        try {
            String[] parts = sval.split( "," );
            int np = parts.length;
            float[] fs = new float[ np ];
            for ( int i = 0; i < np; i++ ) {
                fs[ i ] = Float.parseFloat( parts[ i ] );
            }
            return fs;
        }
        catch ( NumberFormatException e ) {
            return null;
        }
    }

    public String stringifyValue( float[] dash ) {
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < dash.length; i++ ) {
            if ( sbuf.length() > 0 ) {
                sbuf.append( "," );
            }
            float f = dash[ i ];
            sbuf.append( f == (int) f ? Integer.toString( (int) f )
                                      : Float.toString( f ) );
        }
        return sbuf.toString();
    }

    public Specifier<float[]> createSpecifier() {
        return new ComboBoxSpecifier<float[]>( float[].class,
                                               new DashComboBox() );
    }

    /**
     * Returns a metadata object suitable for use with a DashConfigKey.
     *
     * @param  shortName  short key name
     * @param  longName  long key name
     * @return   dash config metadata
     */
    public static ConfigMeta createDashMeta( String shortName,
                                             String longName ) {
        ConfigMeta meta = new ConfigMeta( shortName, longName );
        meta.setStringUsage( "dot|dash|...|<a,b,...>" );
        meta.setShortDescription( "Line dash pattern" );
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
            "<p>Determines the dash pattern of the line drawn.",
            "If null (the default), the line is solid.",
            "</p>",
            "<p>Possible values for dashed lines are",
            nameList.toString() + ".",
            "You can alternatively supply a comma-separated list",
            "of on/off length values such as",
            "\"<code>4,2,8,2</code>\".",
            "</p>",
        } );
        return meta;
    }

    /**
     * Returns a map of known dash patterns by name.
     *
     * @return  name->pattern map
     */
    private static final Map<String,float[]> createDashMap() {
        Map<String,float[]> map = new LinkedHashMap<String,float[]>();
        map.put( "dot", new float[] { 1f, 2f, } );
        map.put( "dash", new float[] { 4f, 2f, } );
        map.put( "longdash", new float[] { 8f, 4f, } );
        map.put( "dotdash", new float[] { 12f, 3f, 3f, 3f, } );
        return Collections.unmodifiableMap( map );
    }
}
