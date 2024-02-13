package uk.ac.starlink.table.gui;

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

    private NumberFormat sciFormatDouble;
    private NumberFormat sciFormatFloat;
    private NumberFormat fixFormatDouble;
    private NumberFormat fixFormatFloat0;
    private NumberFormat fixFormatFloat1;
    private NumberFormat fixFormatFloat2;
    private NumberFormat fixFormatFloat3;
    private NumberFormat fixFormatFloat4;
    private NumberFormat fixFormatFloat5;
    private NumberFormat fixFormatFloat6;
    private NumberFormat intFormat;
    private NumberFormat longFormat;
    private char decimalPoint;
    private String decimalPointString;
    private boolean likeHeading;
    private String badText;
    private Object badValue = new Object();
    private Class<?> clazz;
    private Font font;

    private static JTable dummyTable;

    /**
     * Construct a new NumericCellRenderer with a hint about the values
     * it will be expected to render.  An attempt will be made to render
     * other objects, but the alignment and so on may not be so good.
     *
     * @param  clazz  the type of object it will expect to render on the whole
     */
    public NumericCellRenderer( Class<?> clazz ) {
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
        badText = "";
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
            setText( badText );
            return;
        }

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
                sbuf.append( nel <= MAX_SHOW_ELEMENTS ? ") " : ", ... " );
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
        else if ( obj instanceof Double ) {
            double dval = ((Double) obj).doubleValue();
            double aval = Math.abs( dval );
            if ( Double.isNaN( dval ) ) {
                return;
            }
            else if ( aval <= Double.MIN_VALUE ) {
                setText( formatFixedDouble( 0.0 ) );
                return;
            }
            else if ( aval < 1e-4 || aval >= 1e5 ) {
                setText( formatSciDouble( dval ) );
                return;
            }
            else {
                setText( formatFixedDouble( dval ) );
                return;
            }
        }
        else if ( obj instanceof Float ) {
            float fval = ((Float) obj).floatValue();
            float aval = Math.abs( fval );
            if ( Float.isNaN( fval ) ) {
                return;
            }
            else if ( aval <= Float.MIN_VALUE ) {
                boolean isNeg =
                    ( Float.floatToIntBits( fval ) & 0x80000000 ) != 0;
                setText( formatFixedFloat( isNeg ? -0.0f : 0.0f ) );
                return;
            }
            else if ( aval < 1e-4 || aval >= 1e5 ) {
                setText( formatSciFloat( fval ) );
                return;
            }
            else {
                setText( formatFixedFloat( fval ) );
                return;
            }
        }

        /* Is it a long? */
        else if ( obj instanceof Long ) {
            setText( ((Long) obj).toString() + " " );
        }

        /* Is it an integral number? */
        else if ( ( obj instanceof Integer ) ||
                  ( obj instanceof Short ) ||
                  ( obj instanceof Byte ) ) {
            setText( intFormat.format( ((Number) obj).intValue() ) + ' ' );
            return;
        }

        /* It's just an object. */
        else {
            setText( ' ' + obj.toString() + ' ' );
            return;
        }
    }

    /**
     * Sets a value to be regarded as bad when found in a non-header cell.
     * Any cell containing an object which <tt>equals()</tt> this value
     * will be represented specially in the table body (currently just blank).
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
     * Does fixed-type formatting of a <tt>double</tt> value.
     */
    private String formatFixedDouble( double dval ) {
        if ( fixFormatDouble == null ) {
            fixFormatDouble = NumberFormat.getInstance();
            if ( fixFormatDouble instanceof DecimalFormat ) {
                DecimalFormat dformat = (DecimalFormat) fixFormatDouble;
                dformat.applyPattern( " #####0.#####;-#####0.#####" );
                dformat.setDecimalSeparatorAlwaysShown( true );
                decimalPoint = dformat.getDecimalFormatSymbols()
                                      .getDecimalSeparator();
                decimalPointString = "" + decimalPoint;
            }
        }
        StringBuffer buf = new StringBuffer( 20 );
        buf.append( fixFormatDouble.format( dval ) );
        int dotpos = buf.indexOf( decimalPointString );
        if ( dotpos < 0 ) {
            dotpos = buf.length();
            buf.append( decimalPoint );
        }
        int pad = 7 - ( buf.length() - dotpos );
        for ( int i = 0; i < pad; i++ ) {
            buf.append( ' ' );
        }
        return buf.toString();
    }

    /**
     * Does fixed-type formatting of a <tt>float</tt> value.
     */
    private String formatFixedFloat( float fval ) {
        if ( fixFormatFloat0 == null ) {
            fixFormatFloat0 = NumberFormat.getInstance();
            if ( fixFormatFloat0 instanceof DecimalFormat ) {
                DecimalFormat dformat0 = (DecimalFormat) fixFormatFloat0;
                dformat0.setDecimalSeparatorAlwaysShown( true );
                decimalPoint = dformat0.getDecimalFormatSymbols()
                                       .getDecimalSeparator();
                decimalPointString = "" + decimalPoint;
            }
            fixFormatFloat1 = (NumberFormat) fixFormatFloat0.clone();
            fixFormatFloat2 = (NumberFormat) fixFormatFloat0.clone();
            fixFormatFloat3 = (NumberFormat) fixFormatFloat0.clone();
            fixFormatFloat4 = (NumberFormat) fixFormatFloat0.clone();
            fixFormatFloat5 = (NumberFormat) fixFormatFloat0.clone();
            fixFormatFloat6 = (NumberFormat) fixFormatFloat0.clone();
            if ( fixFormatFloat0 instanceof DecimalFormat ) {
                ((DecimalFormat) fixFormatFloat0)
                           .applyPattern( " 0.######;-0.######" );
                ((DecimalFormat) fixFormatFloat1)
                           .applyPattern( " 0.#####;-0.#####" );
                ((DecimalFormat) fixFormatFloat2)
                           .applyPattern( "#0.####;-#0.####" );
                ((DecimalFormat) fixFormatFloat3)
                           .applyPattern( " ##0.###;-##0.###" );
                ((DecimalFormat) fixFormatFloat4)
                           .applyPattern( " ###0.##;-###0.##" );
                ((DecimalFormat) fixFormatFloat5)
                           .applyPattern( " ####0.#;-####0.#" );
                ((DecimalFormat) fixFormatFloat6)
                           .applyPattern( " #####0.;-#####0." );
            }
        }
        StringBuffer buf = new StringBuffer( 20 );
        float aval = Math.abs( fval );
        NumberFormat fmt;
        if ( aval < 1e0 ) {
            fmt = fixFormatFloat0;
        }
        else if ( aval < 1e1 ) {
            fmt = fixFormatFloat1;
        }
        else if ( aval < 1e2 ) {
            fmt = fixFormatFloat2;
        }
        else if ( aval < 1e3 ) {
            fmt = fixFormatFloat3;
        }
        else if ( aval < 1e4 ) {
            fmt = fixFormatFloat4;
        }
        else if ( aval < 1e-5 ) {
            fmt = fixFormatFloat5;
        }
        else {
            fmt = fixFormatFloat6;
        }
        buf.append( fmt.format( fval ) );
        int dotpos = buf.indexOf( decimalPointString );
        if ( dotpos < 0 ) {
            dotpos = buf.length();
            buf.append( decimalPoint );
        }
        int pad = 8 - ( buf.length() - dotpos );
        for ( int i = 0; i < pad; i++ ) {
            buf.append( ' ' );
        }
        return buf.toString();
    }

    /**
     * Does scientific-type formatting of a <tt>double</tt> value.
     */
    private String formatSciDouble( double dval ) {
        if ( sciFormatDouble == null ) {
            sciFormatDouble = NumberFormat.getInstance();
            if ( sciFormatDouble instanceof DecimalFormat ) {
                ((DecimalFormat) sciFormatDouble)
                .applyPattern( " 0.000000E0;-0.000000E0" );
            }
        }
        StringBuffer buf = new StringBuffer( 20 );
        buf.append( sciFormatDouble.format( dval ) );
        int pad = 4 - ( buf.length() - buf.indexOf( "E" ) );
        for ( int i = 0; i < pad; i++ ) {
            buf.append( ' ' );
        }
        return buf.toString();
    }

    /**
     * Does scientific-type formatting of a <tt>float</tt> value.
     */
    private String formatSciFloat( float fval ) {
        if ( sciFormatFloat == null ) {
            sciFormatFloat = NumberFormat.getInstance();
            if ( sciFormatFloat instanceof DecimalFormat ) {
                ((DecimalFormat) sciFormatFloat)
                .applyPattern( " 0.00000E0;-0.00000E0" );
            }
        }
        StringBuffer buf = new StringBuffer( 20 );
        buf.append( sciFormatFloat.format( fval ) );
        int pad = 4 - ( buf.length() - buf.indexOf( "E" ) );
        for ( int i = 0; i < pad; i++ ) {
            buf.append( ' ' );
        }
        return buf.toString();
    }

}
