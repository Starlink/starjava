package uk.ac.starlink.table.gui;

import java.awt.Color;
import java.awt.Font;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Provides better rendering of numeric table cells than the default 
 * JTable renderer.
 * Single-element primitive arrays are treated just like the corresponding
 * wrapper classes, and the first few elements of a multi-element array
 * are displayed.
 * Numeric values are displayed with decimal points aligned and so on.
 * This class is generally adapted to display of numeric values, but
 * it can render Strings or other Objects too.
 * <p>
 * Cell rendering can be further refined by extending this class and
 * overriding the {@link #setValue} method.
 *
 * @author   Mark Taylor (Starlink)
 */
public class NumericCellRenderer extends DefaultTableCellRenderer {

    /** The most elements that will be shown in a single cell. */
    public static final int MAX_SHOW_ELEMENTS = 10;

    private NumberFormat sciFormat;
    private NumberFormat fixFormat;
    private NumberFormat intFormat;
    private boolean likeHeading;
    private String badText;
    private Color badColor;
    private Color goodColor;
    private Object badValue = new Object();
    private Class clazz;
    private Font font;

    private static JTable dummyTable;

    /**
     * Construct a new NumericCellRenderer with a hint about the values
     * it will be expected to render.  An attempt will be made to render
     * other objects, but the alignment and so on may not be so good.
     *
     * @param  clazz  the type of object it will expect to render on the whole
     */
    public NumericCellRenderer( Class clazz ) {
        this.clazz = clazz;

        /* Set the font. */
        if ( Number.class.isAssignableFrom( clazz ) ) {
            setHorizontalAlignment( RIGHT );
            font = new Font( "Monospaced", Font.PLAIN, getFont().getSize() );
        }
        else {
            setHorizontalAlignment( LEFT );
        }

        /* Configure some formatters here. */
        intFormat = NumberFormat.getInstance();
        if ( intFormat instanceof DecimalFormat ) {
            ((DecimalFormat) intFormat)
            .applyPattern( " #########0;-#########0" );
        }

        /* Configure bad value representation. */
        goodColor = UIManager.getColor( "Table.foreground" );
        badColor = new Color( goodColor.getRed(),
                              goodColor.getGreen(),
                              goodColor.getBlue(),
                              goodColor.getAlpha() / 3 );
        badText = "BAD";
    }

    /**
     * Sets the font in which to render cell contents.
     *
     * @param  font the font to use for text rendering
     */
    public void setCellFont( Font font ) {
        this.font = font;
    }

    /**
     * Returns the font in which cell contents will be rendererd.
     *
     * @return  the font used for text rendering
     */
    public Font getCellFont() {
        return font;
    }

    /**
     * Sets the state of this renderer, overriding the method in 
     * <tt>DefaultTableCellRenderer</tt> to provide more intelligent behaviour.
     * <p>
     * Subclasses note: the work is done by invoking this object's
     * <tt>setText</tt> and possibly <tt>setIcon</tt> methods (remember this 
     * object is a <tt>javax.swing.JLabel</tt>).
     *
     * @param  value  the value to be rendered
     */
    protected void setValue( Object value ) {
        setText( null );
        Object obj;

        /* Is it bad? */
        if ( isBadValue( value ) ) {
            setForeground( badColor );
            setText( ' ' + badText + ' ' );
            return;
        }
        setForeground( goodColor );

        /* Is it null? */
        if ( value == null ) {
            return;
        }

        if ( font != null ) {
            setFont( font );
        }

        /* Is it an array? */
        if ( value.getClass().getComponentType() != null ) {
            int nel = Array.getLength( value );

            /* If it's a zero-element array, treat the cell as empty. */
            if ( nel == 0 ) {
                return;
            }
            
            /* If it has multiple elements, print some out. */
            else if ( nel > 1 ) {
                int limit = Math.min( nel, MAX_SHOW_ELEMENTS );
                StringBuffer sbuf = new StringBuffer( " (" );
                for ( int i = 0; i < limit; i++ ) {
                    if ( i > 0 ) {
                        sbuf.append( ", " );
                    }
                    sbuf.append( Array.get( value, i ) );
                }
                sbuf.append( ") " );
                setText( sbuf.toString() );
                return;
            }

            /* Single object in the array - treat it as a bare object
             * (this will wrap a primitive if necessary). */
            else {
                assert nel == 1;
                obj = Array.get( value, 0 );
            }
        }
        else {
            obj = value;
        }

        /* Now we have a single object to format. */

        /* Is it null? */
        if ( obj == null ) {
            return;
        }

        /* Is it a floating point number? */
        if ( obj instanceof Double || obj instanceof Float ) {
            double dval = ((Number) obj).doubleValue();
            double aval = Math.abs( dval );
            if ( Double.isNaN( dval ) ) {
                return;
            }
            else if ( obj instanceof Double && aval <= Double.MIN_VALUE ||
                      obj instanceof Float && aval <= Float.MIN_VALUE ) {
                setText( formatFixed( 0.0 ) );
                return;
            }
            else if ( aval < 1e-4 || aval >= 1e5 ) {
                setText( formatSci( dval ) );
                return;
            }
            else {
                setText( formatFixed( dval ) );
                return;
            }
        }

        /* Is it an integral number? */
        if ( ( obj instanceof Long ) ||
             ( obj instanceof Integer ) ||
             ( obj instanceof Short ) ||
             ( obj instanceof Byte ) ) {
            setText( intFormat.format( ((Number) obj).intValue() ) + ' ' );
        }

        /* It's just an object. */
        setText( ' ' + obj.toString() + ' ' );
        return;
    }

    /**
     * Sets a value to be regarded as bad when found in a non-header cell.
     * Any cell containing an object which <tt>equals()</tt> this value
     * will be represented specially in the table body (currently a 
     * greyed-out "BAD" string).
     * Note that <tt>null</tt> means that null objects will be regarded as
     * bad; if you want nulls to receive default treatment, call this
     * method with some otherwise-unreferenced object of type <tt>Object</tt>.
     *
     * @param  badValue  the special bad value
     */
    public void setBadValue( Object badValue ) {
        this.badValue = badValue;
    }

    /**
     * Returns the advised width for table cells rendered by this object,
     * on the assumption that the objects it is asked to render are
     * as per the constructor.
     *
     * @return  the advised cell width in pixels
     */
    public int getCellWidth() {
        Object testob;
        if ( clazz == null ) {
            return widthFor( "                " );
        }
        else if ( clazz.equals( Byte.class ) ) {
            return widthFor( new Byte( (byte) 0x7f ) );
        }
        else if ( clazz.equals( Short.class ) ) {
            return widthFor( new Short( (short) 0x7fff ) );
        }
        else if ( clazz.equals( Integer.class ) ) {
            return widthFor( new Integer( 0x7fffffff ) );
        }
        else if ( clazz.equals( Long.class ) ) {
            return widthFor( new Long( 0x7fffffffffffffffL ) );
        }
        else if ( clazz.equals( Float.class ) ) {
            widthFor( new Float( 10537. ) );
            return Math.max( widthFor( new Float( - Float.MAX_VALUE ) ),
                             widthFor( new Float( - ( 1e5 - Math.PI ) ) ) );
        }
        else if ( clazz.equals( Double.class ) ) {
            return Math.max( widthFor( new Double( - Double.MAX_VALUE ) ),
                             widthFor( new Double( - ( 1e5 - Math.PI ) ) ) );
        }
        else {
            return widthFor( "                " );
        }
    }

    /**
     * Returns the width in pixels of a cell required to render the given
     * object.
     *
     * @return  width in pixels
     */
    private int widthFor( Object ob ) {
        setValue( ob );
        return getPreferredSize().width + 2;
    }

    /**
     * Tests against the bad value.  Make sure that we can't get a 
     * NullPointerException here.
     */
    private boolean isBadValue( Object val ) {
        if ( badValue == null ) {
            return val == null;
        }
        else {
            return badValue.equals( val );
        }
    }

    /**
     * Does fixed-type formatting of a floating point value.
     */
    private String formatFixed( double dval ) {
        if ( fixFormat == null ) {
            fixFormat = NumberFormat.getInstance();
            if ( fixFormat instanceof DecimalFormat ) {
                ((DecimalFormat) fixFormat)
                .applyPattern( " #####0.######;-#####0.######" );
            }
        }
        StringBuffer buf = new StringBuffer( 20 );
        buf.append( fixFormat.format( dval ) );
        int dotpos = buf.indexOf( "." );
        if ( dotpos < 0 ) {
            dotpos = buf.length();
            buf.append( '.' );
        }
        int pad = 11 - ( buf.length() - dotpos );
        for ( int i = 0; i < pad; i++ ) {
            buf.append( ' ' );
        }
        return buf.toString();
    }

    /**
     * Does scientific-type formatting of a floating point value.
     */
    private String formatSci( double dval ) {
        if ( sciFormat == null ) {
            sciFormat = NumberFormat.getInstance();
            if ( sciFormat instanceof DecimalFormat ) {
                ((DecimalFormat) sciFormat)
                .applyPattern( " 0.000000E0;-0.000000E0" );
            }
        }
        StringBuffer buf = new StringBuffer( 20 );
        buf.append( sciFormat.format( dval ) );
        int pad = 4 - ( buf.length() - buf.indexOf( "E" ) );
        for ( int i = 0; i < pad; i++ ) {
            buf.append( ' ' );
        }
        return buf.toString();
    }

}
