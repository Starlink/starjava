package uk.ac.starlink.topcat.plot;

import java.awt.Font;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.JLabel;

/**
 * Component whose text gives a number of (name, count) pairs.
 *
 * @author   Mark Taylor
 * @since    29 Nov 2005
 */
public class CountsLabel extends JLabel {

    private final String[] titles_;
    private final NumberFormat format_;

    /**
     * Constructs a new CountsLable with given count headings.
     *
     * @param  titles  count headings
     */
    @SuppressWarnings("this-escape")
    public CountsLabel( String[] titles ) {
        titles_ = titles;

        /* Set up the formatter used for formatting counts. */
        format_ = new DecimalFormat();
        ((DecimalFormat) format_).setGroupingSize( 3 );
        ((DecimalFormat) format_).setGroupingUsed( true );

        /* Make some cosmetic changes.  A monospaced font is suitable so
         * that the length of the text doesn't keep changing, which would
         * be visually distracting. */
        setFont( new Font( "Monospaced", getFont().getStyle(),
                           getFont().getSize() ) );
        setBorder( BorderFactory.createEtchedBorder() );

        /* Initialise. */
        setValues( null );
    }

    /**
     * Sets the count values to report.  The <code>values</code> array
     * should generally have the same number of elements as the 
     * <code>titles</code> array given at construction time.
     * It may however be <code>null</code> indicating no known values.
     * Currently, each of the counts will be formatted in the same
     * number of spaces as that of the longest (formatted) one.
     * This is an attempt to reduce the amount of visual changes when
     * the values are modified (only applies when used in certain ways,
     * obviously).
     *
     * @param  values  values array
     */
    public void setValues( int[] values ) {
        int nval = titles_.length;
        String[] sValues = new String[ nval ];
        if ( values == null ) {
            Arrays.fill( sValues, "   " );
        }
        else if ( values.length != nval ) {
            throw new IllegalArgumentException();
        }
        else {
            for ( int i = 0; i < nval; i++ ) {
                sValues[ i ] = format_.format( values[ i ] );
            }
        }
        int maxLeng = 0; 
        for ( int i = 0; i < nval; i++ ) {
            maxLeng = Math.max( maxLeng, sValues[ i ].length() );
        }
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < nval; i++ ) {
            sbuf.append( ' ' )
                .append( titles_[ i ] )
                .append( ": " );
            int pad = maxLeng - sValues[ i ].length();
            for ( int j = 0; j < pad; j++ ) {
                sbuf.append( ' ' );
            }
            sbuf.append( sValues[ i ] );
        }
        sbuf.append( ' ' );
        setText( sbuf.toString() );
    }
}
