package uk.ac.starlink.table.gui;

import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import uk.ac.starlink.array.NDArray;

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
        setHorizontalAlignment( LEFT );

        /* Is it null? */
        if ( value == null ) {
            return;
        }

        /* Is it a multi-element array? */
        Class cls = value.getClass();
        Class elType = cls.getComponentType();
        if ( elType != null && Array.getLength( value ) > 1 ) {
            int nel = Array.getLength( value );

            /* With multiple elements? */
            if ( nel > 1 ) {
                StringBuffer sb = new StringBuffer( "( " );
                int nshow = Math.min( nel, MAX_SHOW_ELEMENTS );
                for ( int i = 0; i < nshow; i++ ) {
                    if ( i > 0 ) {
                        sb.append( ", " );
                    }
                    sb.append( Array.get( value, i ) );
                }
                sb.append( ( nshow == nel ) ? " )" : "..." );
                setText( sb.toString() );
                return;
            }
        }

        /* We have a single object to format, either bare or as the sole
         * element of an array. */
        Object obj = ( elType == null ) ? value : Array.get( value, 0 );

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
        if ( ( obj instanceof Integer ) ||
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

        /* Where are these property names documented?  Don't know, but
         * you can find them in the source code of 
         * javax.swing.plaf.basic.BasicLookAndFeel,
         * javax.swing.plaf.metal.MetalLookAndFeel. */
        if ( likeHeading ) {
            setForeground( UIManager.getColor( "TableHeader.foreground" ) );
            setBackground( UIManager.getColor( "TableHeader.background" ) );
            setFont( UIManager.getFont( "TableHeader.font" ) );
            setHorizontalAlignment( SwingConstants.CENTER );
        }
        else {
            setForeground( UIManager.getColor( "Table.foreground" ) );
            setBackground( UIManager.getColor( "Table.background" ) );
            setFont( UIManager.getFont( "Table.font" ) );
            setHorizontalAlignment( SwingConstants.LEFT );
        }
    }
}
