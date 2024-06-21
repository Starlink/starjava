package uk.ac.starlink.table.gui;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import uk.ac.starlink.table.ValueInfo;

/**
 * A <code>TableCellRenderer</code> which does its rendering according to
 * a <code>ValueInfo</code> object which describes the values which it
 * is expected to have to render.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ValueInfoCellRenderer extends DefaultTableCellRenderer {

    private ValueInfo vinfo;
    private int maxLength = 10000;

    /**
     * Constructs a renderer from a <code>ValueInfo</code> object.
     *
     * @param vinfo  the info describing the type of object to be rendered
     */
    public ValueInfoCellRenderer( ValueInfo vinfo ) {
        this.vinfo = vinfo;
    }

    /**
     * Sets the state of this renderer, overriding the method in 
     * DefaultTableCellRenderer to provide more intelligent behaviour.
     * <p>
     * Subclasses note: the work is done by invoking this object's
     * <code>setText</code> and/or <code>setIcon</code> methods (remember this 
     * object is a <code>javax.swing.JLabel</code>).
     *
     * @param  value  the value to be rendered
     */
    protected void setValue( Object value ) {
        setText( ' ' + vinfo.formatValue( value, maxLength ) + ' ' );
    }

    /**
     * Sets the length in characters at which cell value representations 
     * will be truncated.
     * 
     * @param  maxLength  the maximum number of characters to write into
     *         a cell
     */
    public void setMaxLength( int maxLength ) {
        this.maxLength = maxLength;
    }

    /**
     * Gets the length in characters at which cell value representations
     * will be truncated.
     *
     * @return  the maximum number of characters to be written into a cell
     */
    public int getMaxLength() {
        return maxLength;
    }
}
