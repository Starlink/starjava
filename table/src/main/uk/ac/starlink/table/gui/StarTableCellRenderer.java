package uk.ac.starlink.table.gui;

import java.awt.Color;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import javax.swing.BorderFactory;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import uk.ac.starlink.table.ShapedArray;

/**
 * Provides better rendering of table cells than the default JTable renderer.
 * Single-element primitive arrays are treated just like the corresponding
 * wrapper classes, and the first few elements of a multi-element array
 * are displayed.
 * <p>
 * Cell rendering can be further refined by extending this class and
 * overriding the {@link #setValue} method.
 * Resulting instances can be installed for use in a JTable by using the
 * {@link javax.swing.JTable#setDefaultRenderer} method.
 *
 * @author   Mark Taylor (Starlink)
 */
public class StarTableCellRenderer extends DefaultTableCellRenderer {

    /** The most elements that will be shown in a single cell. */
    public static final int MAX_SHOW_ELEMENTS = 10;

    private NumberFormat sciFormat;
    private NumberFormat fixFormat;
    private NumberFormat intFormat;
    private boolean likeHeading;
    private String badText;
    private Color badColor;
    private Color bgColor;
    private Color fgColor;
    private Object badValue = new Object();

    /**
     * Construct a new StarTableCellRenderer.
     * Equivalent to <tt>StarTableCellRenderer(false)</tt>.
     */
    public StarTableCellRenderer() {
        this( false );
    }

    /**
     * Construct a new StarTableCellRenderer indicating whether it should
     * use a heading-like style or not.
     *
     * @param  likeHeading  true iff the rendered cells want to look like 
     *         a heading.
     */
    public StarTableCellRenderer( boolean likeHeading ) {

        /* Set up heading style. */
        setHeadingStyle( likeHeading );

        /* Configure some formatters. */
        sciFormat = NumberFormat.getInstance();
        if ( sciFormat instanceof DecimalFormat ) {
            ((DecimalFormat) sciFormat)
            .applyPattern( " 0.000000E0;-0.000000E0" );
        }

        fixFormat = NumberFormat.getInstance();
        if ( fixFormat instanceof DecimalFormat ) {
            ((DecimalFormat) fixFormat)
            .applyPattern( " #####0.0#####;-#####0.0#####" );
        }

        intFormat = NumberFormat.getInstance();
        if ( intFormat instanceof DecimalFormat ) {
            ((DecimalFormat) intFormat)
            .applyPattern( " #########0;-#########0" );
        }

        /* Configure bad value representation. */
        Color goodColor = UIManager.getColor( "Table.foreground" );
        badColor = new Color( goodColor.getRed(),
                              goodColor.getGreen(),
                              goodColor.getBlue(),
                              goodColor.getAlpha() / 3 );
        badText = "BAD";
    }


    /**
     * Sets the state of this renderer, overriding the method in 
     * DefaultTableCellRenderer to provide more intelligent behaviour.
     * <p>
     * Subclasses note: the work is done by invoking this object's
     * <tt>setText</tt> and <tt>setIcon</tt> methods (remember this 
     * object is a <tt>javax.swing.JLabel</tt>).
     *
     * @param  value  the value to be rendered
     */
    protected void setValue( Object value ) {
        setText( null );
        setIcon( null );
        setBackground( bgColor );
        setForeground( fgColor );
        setHorizontalAlignment( LEFT );

        /* Is it bad? */
        if ( ( ! likeHeading ) && isBadValue( value ) ) {
            setForeground( badColor );
            setText( badText );
            return;
        }

        /* Is it null? */
        if ( value == null ) {
            return;
        }

        /* Is it a ShapedArray? */
        Object arrayObj = null;
        int[] dims = null;
        if ( value instanceof ShapedArray ) {
            ShapedArray sa = (ShapedArray) value;
            int[] d = sa.getDims();
            dims = ( d.length > 1 ) ? d : null;
            arrayObj = sa.getData();
        }

        /* Is it an array? */
        if ( value.getClass().getComponentType() != null ) {
            arrayObj = value;
            dims = null;
        }
            
        /* Deal with an array-like object. */
        Object obj;
        if ( arrayObj != null ) {
            int nel = Array.getLength( arrayObj );

            /* If it's a zero-element array, treat the cell as empty. */
            if ( nel == 0 ) {
                return;
            }
            
            /* If it has multiple elements, print some out. */
            else if ( nel > 1 ) {
                setText( elementsNdim( arrayObj, dims, nel ) );
                return;
            }

            /* Single object in the array - treat it as a bare object
             * (this will wrap a primitive if necessary). */
            else {
                assert nel == 1;
                obj = Array.get( arrayObj, 0 );
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
                setText( fixFormat.format( 0.0 ) );
            }
            else if ( aval < 1e-4 || aval > 1e5 ) {
                setText( sciFormat.format( dval ) );
                return;
            }
            else {
                setText( fixFormat.format( dval ) );
                return;
            }
        }

        /* Is it an integral number? */
        if ( ( obj instanceof Long ) ||
             ( obj instanceof Integer ) ||
             ( obj instanceof Short ) ||
             ( obj instanceof Byte ) ) {
            setHorizontalAlignment( RIGHT );
            setText( intFormat.format( ((Number) obj).intValue() ) );
        }

        /* It's just an object. */
        setText( obj.toString() );
        return;
    }

    /**
     * Configures the style of this renderer to look like a header cell or
     * like a body cell (background colour etc).
     *
     * @param  likeHeading  true iff the rendered cells want to look like 
     *         a heading.
     *         By default it looks like a cell in the table body.
     */
    public void setHeadingStyle( boolean likeHeading ) {
        this.likeHeading = likeHeading;

        /* Where are these property names documented?  Don't know, but
         * you can find them in the source code of 
         * javax.swing.plaf.basic.BasicLookAndFeel,
         * javax.swing.plaf.metal.MetalLookAndFeel. */
        if ( likeHeading ) {
            bgColor = UIManager.getColor( "TableHeader.background" );
            fgColor = UIManager.getColor( "TableHeader.foreground" );
            setFont( UIManager.getFont( "TableHeader.font" ) );
            setHorizontalAlignment( SwingConstants.CENTER );
        }
        else {
            bgColor = UIManager.getColor( "Table.background" );
            fgColor = UIManager.getColor( "Table.foreground" );
            setFont( UIManager.getFont( "Table.font" ) );
            setHorizontalAlignment( SwingConstants.LEFT );
        }
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
     * Surrounds the text with a little bit of padding prior to calling
     * the superclass (JLabel) implementation.
     *
     * @param   text  the cell text
     */
    public void setText( String text ) {
        super.setText( ( text == null ) ? null : ( ' ' + text + ' ' ) );
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
     * Constructs a string displaying the first few elements of a given
     * N-dimensional array.
     *
     * @param  array  an array of objects
     * @param  dims   the dimensions of <tt>array</tt>
     * @param  nel    the number of elements in <tt>array</tt> 
     *                (may not all get printed)
     */
    private static String elementsNdim( Object array, int[] dims, int nel ) {
        int nshow = Math.min( nel, MAX_SHOW_ELEMENTS );
        StringBuffer sb = new StringBuffer();

        /* 1-dimensional. */
        if ( dims == null || dims.length <= 1 ) {
            sb.append( '(' );
            for ( int i = 0; i < nshow; i++ ) {
                if ( i > 0 ) {
                    sb.append( ", " );
                }
                sb.append( Array.get( array, i ) );
            }
            if ( nshow == nel ) {
                sb.append( ')' );
            }
        }

        /* Multi-dimensional. */
        else {
            appendElements( sb, array, 0, dims, nshow );
        }

        if ( nshow < nel ) {
            sb.append( ( nshow == nel ) ? " )" : "..." );
        }
        return sb.toString();
    }

    /**
     * Recursive routine for printing out elements of a multidimensional
     * array with brackets in the right place.
     */
    private static int appendElements( StringBuffer sb, Object array,
                                       int pos, int[] dims, int total ) {
        if ( pos < total ) {
            sb.append( '(' );
            if ( dims.length == 1 ) {
                int limit = dims[ 0 ];
                for ( int i = 0; i < limit; i++ ) {
                    sb.append( Array.get( array, pos ) );
                    if ( i < limit - 1 ) {
                        sb.append( ", " );
                    }
                    else {
                        sb.append( ")" );
                    }
                    pos++;
                    if ( pos >= total && i < limit - 1 ) {
                        pos++;
                        break;
                    }
                }
            }
            else {
                int ndim = dims.length;
                int[] subdims = new int[ ndim - 1 ];
                System.arraycopy( dims, 0, subdims, 0, ndim - 1 );
                int limit = dims[ ndim - 1 ];
                for ( int i = 0; i < limit && pos < total; i++ ) {
                    pos = appendElements( sb, array, pos, subdims, total );
                    if ( pos > total ) {
                        break;
                    }
                    if ( i < limit - 1 ) {
                        sb.append( ", " );
                    }
                    else {
                        sb.append( ")" );
                    }
                }
            }
        }
        return pos;
    }
}
