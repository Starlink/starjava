package uk.ac.starlink.oldfits;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;

/**
 * Controlled access to a collection of FITS header cards.
 * Currently decorates the {@link nom.tam.fits.Header} class, but 
 * could be implemented on top of some other (possibly custom) 
 * header cards parser implementation.
 *
 * @author   Mark Taylor
 * @since    12 Nov 2007
 */
public class HeaderCards {

    private final Header hdr_;
    private final Collection<String> usedSet_;

    /** Keywords which are never used as table parameters. */
    public final String[] BORING_KEYS = {
        "XTENSION",
        "PCOUNT",
        "GCOUNT",
        "BITPIX",
        "NAXIS",
        "END",
    };

    /**
     * Constructor.
     *
     * @param   hdr   FITS header object
     */
    public HeaderCards( Header hdr ) {
        hdr_ = hdr;
        usedSet_ = new HashSet<String>();
    }

    /**
     * Returns the integer value for a card with a given key.
     * 
     * @param  key  header keyword
     * @return   integer value, or null
     */
    public Integer getIntValue( String key ) {
        return containsKey( key ) ? Integer.valueOf( hdr_.getIntValue( key ) )
                                  : null;
    }

    /**
     * Returns the long value for a card with a given key.
     *
     * @param  key  header keyword
     * @return  long value, or null
     */
    public Long getLongValue( String key ) {
        return containsKey( key ) ? Long.valueOf( hdr_.getLongValue( key ) )
                                  : null;
    }

    /**
     * Returns the double value for a card with a given key.
     *
     * @param  key  header keyword
     * @return  double value, or null
     */
    public Double getDoubleValue( String key ) {
        return containsKey( key ) ? Double.valueOf( hdr_.getDoubleValue( key ) )
                                  : null;
    }

    /**
     * Returns the string value for a card with a given key.
     *
     * @param   key  header keyword
     * @return  string value, or null
     */
    public String getStringValue( String key ) {
        return containsKey( key ) ? hdr_.findCard( key ).getValue()
                                  : null;
    }

    /**
     * Marks a given keyword as used.  This is invoked by all the
     * <code>get*Value</code> methods.
     *
     * @param  key  header keyword
     */
    public void useKey( String key ) {
        if ( hdr_.containsKey( key ) ) {
            usedSet_.add( key );
        }
    }

    /**
     * Indicates whether the header collection contains a card with the 
     * given keyword.
     *
     * @param   key  header keyword
     * @return   true iff <code>key</code> is present
     */
    public boolean containsKey( String key ) {
        if ( hdr_.containsKey( key ) ) {
            usedSet_.add( key );
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Returns an array of DescribedValue objects suitable for use as
     * items of per-table metadata derived from this FITS header.
     * This contains entries for every card which has <em>not</em>
     * already been queried within this object (for which
     * {@link #useKey} has not been explicitly or implicitly called.
     * Certain standard structural FITS keywords ({@link #BORING_KEYS})
     * are ignored as well.
     *
     * <p>The idea is that keywords which have already been read to 
     * determine table structure do not need to be recorded separately
     * since their meaning is implicitly included in the table metadata
     * already.
     *
     * @return   array of table parameters relating to this object
     */
    public DescribedValue[] getUnusedParams() {

        /* Get ready to ignore some keys. */
        Collection<String> excludes = new HashSet<String>( usedSet_ );
        excludes.addAll( Arrays.asList( BORING_KEYS ) );

        /* Go through all cards turning them into DescribedValues. */
        List<DescribedValue> paramList = new ArrayList<DescribedValue>();
        Map<String,DescribedValue> paramMap =
            new HashMap<String,DescribedValue>();
        for ( HeaderCard card : FitsConstants.headerIterable( hdr_ ) ) {
            String name = card.getKey();

            /* HISTORY and COMMENT cards are special - they are multi-valued.
             * Store them in StringBuffer types for now. */
            if ( "HISTORY".equals( name ) || "COMMENT".equals( name ) ) {
                String sval = card.toString().trim();
                String value = sval.length() >= 8 ? sval.substring( 8 )
                                                  : "";
                if ( ! paramMap.containsKey( name ) ) {
                    ValueInfo info =
                        new DefaultValueInfo( name, StringBuffer.class,
                                              "FITS " + name + " card values" );
                    DescribedValue dval =
                        new DescribedValue( info, new StringBuffer( value ) );
                    paramMap.put( name, dval );
                    paramList.add( dval );
                }
                else {
                    DescribedValue dval = paramMap.get( name );
                    ((StringBuffer) dval.getValue()).append( '\n' )
                                                    .append( value );
                }
            }

            /* Others are one DescribedValue per item.  If a key appears
             * more than once it will just be overwritten.  Shouldn't normally
             * happen. */
            else if ( name != null && name.trim().length() > 0 &&
                      ! excludes.contains( name ) ) {
                if ( ! paramMap.containsKey( name ) ) {
                    DescribedValue dval = toDescribedValue( card );
                    paramMap.put( name, dval );
                    paramList.add( dval );
                }
            }
        }

        /* Turn any StringBuffer-valued parameters into String-valued ones. */
        DescribedValue[] params = paramList.toArray( new DescribedValue[ 0 ] );
        for ( int i = 0; i < params.length; i++ ) {
            if ( params[ i ].getInfo().getContentClass()
                 == StringBuffer.class ) {
                ValueInfo info0 = params[ i ].getInfo();
                Object value0 = params[ i ].getValue();
                ValueInfo info1 =
                    new DefaultValueInfo( info0.getName(), String.class,
                                          info0.getDescription() );
                Object value1 = ((StringBuffer) value0).toString();
                params[ i ] = new DescribedValue( info1, value1 );
            }
            DescribedValue dval = params[ i ];
            Object val = params[ i ].getValue();
            assert val == null 
                || params[ i ].getInfo().getContentClass()
                                        .isAssignableFrom( val.getClass() );
        }

        /* Return the result. */
        return params;
    }

    /**
     * Turns a single header card into a DescribedValue.
     *
     * @param   card   header card
     * @return   metadata item containing the information from <code>card</code>
     */
    private static DescribedValue toDescribedValue( HeaderCard card ) {
        String key = card.getKey();
        String comment = card.getComment();
        Object value = toObject( card.getValue() );
        Class<?> clazz = value == null ? String.class : value.getClass();
        DefaultValueInfo info = new DefaultValueInfo( key, clazz );
        if ( comment != null && comment.trim().length() > 0 ) {
            info.setDescription( comment.trim() );
        }
        return new DescribedValue( info, value );
    }

    /**
     * Decodes a FITS header card value string into a suitable Object value.
     *
     * @param  sval  value string
     * @return   object value
     */
    private static Object toObject( String sval ) {
        if ( sval == null ) {
            return null;
        }
        sval = sval.trim();
        if ( sval.length() == 0 ) {
            return null;
        }
        int sleng = sval.length();
        if ( sval.charAt( 0 ) == '\'' && sval.charAt( sleng - 1 ) == '\'' ) {
            return sval.substring( 1, sleng - 1 );
        }
        if ( "T".equals( sval ) ) {
            return Boolean.TRUE;
        }
        if ( "F".equals( sval ) ) {
            return Boolean.FALSE;
        }
        try {
            return Integer.valueOf( sval );
        }
        catch ( NumberFormatException e ) {
        }
        try {
            return Long.valueOf( sval );
        }
        catch ( NumberFormatException e ) {
        }
        try {
            return Double.valueOf( sval.replaceFirst( "[dD]", "e" ) );
        }
        catch ( NumberFormatException e ) {
        }
        return sval;
    }
}
