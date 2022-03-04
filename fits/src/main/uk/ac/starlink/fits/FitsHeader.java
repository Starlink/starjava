package uk.ac.starlink.fits;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;

/**
 * Represents the header part of a FITS HDU that has been read.
 * It is composed of a number of ParsedCards.
 *
 * <p>The FITS 4.0 long-string syntax is supported.
 *
 * @author   Mark Taylor
 * @since    4 Mar 2022
 */
public class FitsHeader {

    private final ParsedCard<?>[] cards_;
    private final Map<String,CommentedValue> map_;
    private final Set<String> usedSet_;
    private final boolean isLax_;

    /** Keywords which are never used as table parameters. */
    public final String[] BORING_KEYS = {
        "XTENSION",
        "PCOUNT",
        "GCOUNT",
        "BITPIX",
        "NAXIS",
        "END",
    };

    private static final BigInteger BIG_INT_MAX =
        BigInteger.valueOf( Integer.MAX_VALUE );
    private static final BigInteger BIG_INT_MIN =
        BigInteger.valueOf( Integer.MIN_VALUE );
    private static final BigInteger BIG_LONG_MAX =
        BigInteger.valueOf( Long.MAX_VALUE );
    private static final BigInteger BIG_LONG_MIN =
        BigInteger.valueOf( Long.MIN_VALUE );

    /**
     * Constructor.
     * All the header cards composing the header must be included
     * so that the header knows its length in bytes.
     *
     * @param  cards  header cards composing the header
     */
    public FitsHeader( ParsedCard<?>[] cards ) {
        cards_ = cards;
        map_ = createMap( cards );
        usedSet_ = new HashSet<String>();
        isLax_ = true;
    }

    /**
     * Returns the header cards of which this header is composed.
     *
     * @return  ordered sequence of parsed header cards
     */
    public ParsedCard<?>[] getCards() {
        return cards_;
    }

    /**
     * Returns the untyped value for a card with a given key,
     * if one exists.
     *
     * @param  key  header keyword
     * @return   value, or null
     */
    public Object getValue( String key ) {
        if ( map_.containsKey( key ) ) {
            useKey( key );
            return map_.get( key ).value_;
        }
        else {
            return null;
        }
    }

    /**
     * Returns the content of a card with a given key as a DescribedValue,
     * if such a key exists.  Any comment text is provided as the
     * {@link uk.ac.starlink.table.ValueInfo#getDescription()}.
     *
     * @param   key  header keyword
     * @return   described value, or null
     */
    public DescribedValue getDescribedValue( String key ) {
        CommentedValue cvalue = map_.get( key );
        if ( cvalue == null ) {
            return null;
        }
        else {
            useKey( key );
            return toDescribedValue( key, cvalue );
        }
    }

    /**
     * Returns the integer value for a card with a given key,
     * if one exists.
     *
     * @param  key  header keyword
     * @return   integer value, or null
     */
    public Integer getIntValue( String key ) {
        Object value = getValue( key );
        if ( value instanceof Number ) {
            return Integer.valueOf( ((Number) value).intValue() );
        }
        else if ( value instanceof String && isLax_ ) {
            try {
                return Integer.parseInt( (String) value );
            }
            catch ( NumberFormatException e ) {
                return null;
            }
        }
        else {
            return null;
        }
    }

    /**
     * Returns the long value for a card with a given key,
     * if one exists.
     *
     * @param  key  header keyword
     * @return  long value, or null
     */
    public Long getLongValue( String key ) {
        Object value = getValue( key );
        if ( value instanceof Number ) {
            return Long.valueOf( ((Number) value).longValue() );
        }
        else if ( value instanceof String && isLax_ ) {
            try {
                return Long.parseLong( (String) value );
            }
            catch ( NumberFormatException e ) {
                return null;
            }
        }
        else {
            return null;
        }
    }

    /**
     * Returns the double value for a card with a given key,
     * if one exists.
     *
     * @param  key  header keyword
     * @return  double value, or null
     */
    public Double getDoubleValue( String key ) {
        Object value = getValue( key );
        if ( value instanceof Number ) {
            return Double.valueOf( ((Number) value).doubleValue() );
        }
        else if ( value instanceof String && isLax_ ) {
            try {
                return Double
                      .parseDouble( ((String) value).replace( 'D', 'E' ) );
            }
            catch ( NumberFormatException e ) {
                return null;
            }
        }
        else {
            return null;
        }
    }

    /**
     * Returns a numeric value for a card with a given key,
     * if one exists.
     * This may be a BigInteger or BigDecimal if no other Number class
     * can represent the serialized value.
     *
     * @param  key  header keyword
     * @return  numeric value, or null
     */
    public Number getNumberValue( String key ) {
        Object value = getValue( key );
        if ( value instanceof Number ) {
            return (Number) value;
        }
        else if ( value instanceof String && isLax_ ) {
            try {
                return new BigDecimal( ((String) value).replace( 'D', 'E' ) );
            }
            catch ( NumberFormatException e ) {
                return null;
            }
        }
        else {
            return null;
        }
    }

    /**
     * Returns the boolean value for a card with a given key,
     * if one exists.
     *
     * @param  key  header keyword
     * @return  boolean value, or null
     */
    public Boolean getBooleanValue( String key ) {
        Object value = getValue( key );
        if ( value instanceof Boolean ) {
            return (Boolean) value;
        }
        else if ( value instanceof String && isLax_ ) {
            String sval = ((String) value).trim();
            if ( "T".equals( sval ) ) {
                return Boolean.TRUE;
            }
            else if ( "F".equals( sval ) ) {
                return Boolean.FALSE;
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }

    /**
     * Returns the string value for a card with a given key,
     * if one exists.
     *
     * @param   key  header keyword
     * @return  string value, or null
     */
    public String getStringValue( String key ) {
        Object value = getValue( key );
        return value == null ? null : value.toString();
    }

    /**
     * Returns an integer value for a card with a given key,
     * or throws an exception if none exists.
     *
     * @param  key  header keyword
     * @return  integer value
     * @throws  HeaderValueException   if no suitable card exists
     */
    public int getRequiredIntValue( String key ) throws HeaderValueException {
        Integer valObj = getIntValue( key );
        if ( valObj == null ) {
            throw new HeaderValueException( "Missing header " + key );
        }
        else {
            return valObj.intValue();
        }
    }

    /**
     * Returns a long value for a card with a given key,
     * or throws an exception if none exists.
     *
     * @param  key  header keyword
     * @return  long value
     * @throws  HeaderValueException   if no suitable card exists
     */
    public long getRequiredLongValue( String key ) throws HeaderValueException {
        Long valObj = getLongValue( key );
        if ( valObj == null ) {
            throw new HeaderValueException( "Missing header " + key );
        }
        else {
            return valObj.longValue();
        }
    }

    /**
     * Returns a long value for a card with a given key,
     * or throws an exception if none exists.
     *
     * @param  key  header keyword
     * @return  long value
     * @throws  HeaderValueException   if no suitable card exists
     */
    public String getRequiredStringValue( String key )
            throws HeaderValueException {
        String val = getStringValue( key );
        if ( val == null ) {
            throw new HeaderValueException( "Missing header " + key );
        }
        else {
            return val;
        }
    }

    /**
     * Marks a given keyword as used.  This is invoked by all the
     * <code>get*Value</code> methods.
     *
     * @param  key  header keyword
     */
    public void useKey( String key ) {
        usedSet_.add( key );
    }

    /**
     * Returns the number of FITS blocks occupied by this header.
     *
     * @return  number of 2880-byte blocks in header
     */
    public long getHeaderBlockCount() {
        return FitsUtil.roundUp( cards_.length * FitsUtil.CARD_LENG,
                                 FitsUtil.BLOCK_LENG )
             / FitsUtil.BLOCK_LENG;
    }

    /**
     * Returns the number of FITS blocks occupied by the Data part of the
     * HDU described by this header.
     *
     * @return  number of 2880-byte blocks in data part of HDU
     */
    public long getDataBlockCount() throws HeaderValueException {
        Integer nAxis = getIntValue( "NAXIS" );
        Integer bitPix = getIntValue( "BITPIX" );
        if ( nAxis == null || bitPix == null ) {
            return 0;
        }
        int naxis = nAxis.intValue();
        if ( naxis == 0 ) {
            return 0;
        }
        int bitpix = bitPix.intValue();
        Integer nAxis1 = getIntValue( "NAXIS1" );
        boolean isRandomGroups =
               nAxis1 != null
            && nAxis1.intValue() == 0
            && Boolean.TRUE.equals( getBooleanValue( "SIMPLE" ) )
            && Boolean.TRUE.equals( getBooleanValue( "GROUPS" ) );
        long nel = 1;
        for ( int i = isRandomGroups ? 2 : 1; i <= naxis; i++ ) {
            Long nAxisI = getLongValue( "NAXIS" + i );
            if ( nAxisI == null ) {
                throw new HeaderValueException( "Missing NAXIS" + i );
            }
            nel *= nAxisI.longValue();
        }
        long pcount = 0;
        long gcount = 1;
        if ( getStringValue( "XTENSION" ) != null || isRandomGroups ) {
            Long pCount = getLongValue( "PCOUNT" );
            Long gCount = getLongValue( "GCOUNT" );
            if ( pCount != null ) {
                pcount = pCount.longValue();
            }
            if ( gCount != null ) {
                gCount = gCount.longValue();
            }
        }
        long nbyte = ( nel + pcount ) * gcount * Math.abs( bitpix ) / 8;
        return FitsUtil.roundUp( nbyte, FitsUtil.BLOCK_LENG )
             / FitsUtil.BLOCK_LENG;
    }

    /**
     * Returns the number of bytes occupied by this header,
     * rounded up to an integer number of blocks.
     *
     * @return   {@link #getHeaderBlockCount()} * 2880
     */
    public long getHeaderByteCount() {
        return FitsUtil.BLOCK_LENG * getHeaderBlockCount();
    }

    /**
     * Returns the number of bytes occupied by the Data part of the
     * HDU described by this header,
     * rounded up to an integer number of blocks.
     *
     * @return   {@link #getDataBlockCount()} * 2880
     */
    public long getDataByteCount() throws HeaderValueException {
        return FitsUtil.BLOCK_LENG * getDataBlockCount();
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
        List<DescribedValue> paramList = new ArrayList<>();

        /* Get ready to ignore some keys. */
        Collection<String> excludes = new HashSet<String>( usedSet_ );
        excludes.addAll( Arrays.asList( BORING_KEYS ) );

        /* Go through all map entries turning them into DescribedValues. */
        for ( Map.Entry<String,CommentedValue> entry : map_.entrySet() ) {
            String key = entry.getKey();
            CommentedValue cvalue = entry.getValue();
            if ( ! excludes.contains( key ) ) {
                paramList.add( toDescribedValue( key, cvalue ) );
            }
        }

        /* Do the same for HISTORY and COMMENT cards too. */
        List<String> comments = new ArrayList<>();
        List<String> histories = new ArrayList<>();
        for ( ParsedCard<?> card : cards_ ) {
            CardType<?> type = card.getType();
            if ( type.equals( CardType.HISTORY ) ) {
                histories.add( card.getComment() );
            }
            else if ( type.equals( CardType.COMMENT ) ) {
                comments.add( card.getComment() );
            }
        }
        if ( comments.size() > 0 ) {
            paramList.add( toDescribedValue( "COMMENT", comments ) );
        }
        if ( histories.size() > 0 ) {
            paramList.add( toDescribedValue( "HISTORY", histories ) );
        }

        /* Return the result. */
        return paramList.toArray( new DescribedValue[ 0 ] );
    }

    /**
     * Constructs a DescribedValue from a key and commented value.
     *
     * @param  key  header key
     * @param  cvalue   value
     */
    private static DescribedValue toDescribedValue( String key,
                                                    CommentedValue cvalue ) {
        Object value = cvalue.value_;
        String comment = cvalue.comment_;
        ValueInfo info = new DefaultValueInfo( key, value.getClass(), comment );
        return new DescribedValue( info, value );
    }

    /**
     * Constructs a DescribedValue from a key and a list of free text lines.
     *
     * @param  key  header key
     * @param  lines   content text
     */
    private static DescribedValue toDescribedValue( String key,
                                                    List<String> lines ) {
        ValueInfo info =
            new DefaultValueInfo( key, String[].class,
                                  "FITS " + key + " card values" );
        return new DescribedValue( info, lines.toArray( new String[ 0 ] ) );
    }

    /**
     * Repackages a Number object as a sensible value for use as general
     * metadata.
     *
     * @param  value  general number value
     * @return   number value of a reasonable type
     */
    private static Number toCompactNumber( Number value ) {
        if ( value instanceof BigInteger ) {
            BigInteger ibig = (BigInteger) value;
            if ( ibig.compareTo( BIG_INT_MIN ) >= 0 &&
                 ibig.compareTo( BIG_INT_MAX ) <= 0 ) {
                return Integer.valueOf( ibig.intValue() );
            }
            else if ( ibig.compareTo( BIG_LONG_MIN ) >= 0 &&
                      ibig.compareTo( BIG_LONG_MAX ) <= 0 ) {
                return Long.valueOf( ibig.longValue() );
            }
            else {
                return ibig;
            }
        }
        else if ( value instanceof BigDecimal ) {
            return Double.valueOf( ((BigDecimal) value).doubleValue() );
        }
        else {
            return value;
        }
    }

    /**
     * Packages a list of parsed header cards as a data structure that
     * can be queried by key.
     * Continued-string (long-string) values as per section 4.2.1.2 of
     * the FITS 4.0 standard are supported.
     *
     * @param  cards  header cards
     * @return   key-&gt;value map
     */
    private static Map<String,CommentedValue>
            createMap( ParsedCard<?>[] cards ) {
        int ncard = cards.length;
        Map<String,CommentedValue> map = new LinkedHashMap<>( ncard );
        for ( int ic = 0; ic < ncard; ic++ ) {
            ParsedCard<?> card = cards[ ic ];
            String key = card.getKey();
            Object value = card.getValue();
            String comment = card.getComment();
            if ( key != null && value != null ) {
                if ( value instanceof String ) {
                    StringBuffer vbuf = new StringBuffer( (String) value );
                    StringBuffer cbuf = new StringBuffer();
                    if ( comment != null && comment.trim().length() > 0 ) {
                        cbuf.append( comment );
                    }
                    while ( vbuf.length() > 0 &&
                            vbuf.charAt( vbuf.length() - 1 ) == '&' &&
                            ic < ncard - 1 &&
                            cards[ ic + 1 ].getType() == CardType.CONTINUE ) {
                        ParsedCard<?> card1 = cards[ ++ic ];
                        Object value1 = card1.getValue();
                        String comment1 = card1.getComment();
                        if ( value1 instanceof String ) {
                            vbuf.setLength( vbuf.length() - 1 );
                            vbuf.append( (String) value1 );
                        }
                        if ( comment1 != null &&
                             comment1.trim().length() > 0 ) {
                            if ( cbuf.length() > 0 ) {
                                cbuf.append( ' ' );
                            }
                            cbuf.append( comment1 );
                        }
                    }
                    map.put( key,
                             new CommentedValue( vbuf.toString(),
                                                 cbuf.length() > 0
                                                     ? cbuf.toString()
                                                     : null ) );
                }
                else {
                    if ( value instanceof Number ) {
                        value = toCompactNumber( (Number) value );
                    }
                    map.put( key, new CommentedValue( value, comment ) );
                }
            }
        }
        return map;
    }

    /**
     * Aggregates a value with a FITS comment string.
     */
    private static class CommentedValue {
        final Object value_;
        final String comment_;

        /** 
         * Constructor.
         *
         * @param  value   value
         * @param  comment  comment text, or null
         */
        CommentedValue( Object value, String comment ) {
            value_ = value;
            comment_ = comment;
        }
    }
}
