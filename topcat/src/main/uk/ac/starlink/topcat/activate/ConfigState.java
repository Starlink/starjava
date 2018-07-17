package uk.ac.starlink.topcat.activate;

import gnu.jel.CompilationException;
import java.awt.Component;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.ButtonModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.text.JTextComponent;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;

/**
 * Object that can preserve the state of a collection of GUI components
 * in a way that is easy to de/serialize.
 * This object provides convenience methods on top of a String-&gt;String map,
 * which constitutes its only state, so that serialization to a
 * string context presents no problems.
 *
 * <p>The intention is that restoring from state encoded in this object
 * should be lenient, for instance ignoring unrecognised keys,
 * so that it can be used in contexts in which the format has changed
 * somewhat between serialization and deserialization, for instance
 * as a consequence of sofware updates.
 * The various <code>restore(key,component)</code> methods will not affect the
 * supplied GUI components in the case that the map contains no entry
 * for the given key.
 *
 * @author   Mark Taylor
 * @since    30 Apr 2018
 */
public class ConfigState {

    private final Map<String,String> map_;
    private static final String TRUE = "true";
    private static final String FALSE = "false";

    /**
     * Constructs an empty state object.
     */
    public ConfigState() {
        this( new LinkedHashMap() );
    }

    /**
     * Constructs an object populated by a given map.
     *
     * @param  map  map constituting this object's content
     */
    public ConfigState( Map<String,String> map ) {
        map_ = map;
    }

    /**
     * Returns the map on which this object is based.
     * It may, in general, be modified.
     *
     * @return  map
     */
    public Map<String,String> getMap() {
        return map_;
    }

    /**
     * Sets an entry of this map as an integer.
     *
     * @param   key   entry key
     * @param   value   integer value
     */
    public void setInt( String key, int value ) {
        map_.put( key, Integer.toString( value ) );
    }

    /**
     * Retrieves an entry of this map as an integer.
     *
     * @param   key   entry key
     * @return   integer value, or 0 if not present
     */
    public int getInt( String key ) {
        if ( map_.containsKey( key ) ) {
            try {
                return Integer.valueOf( map_.get( key ) );
            }
            catch ( RuntimeException e ) {
                return 0;
            }
        }
        else {
            return 0;
        }
    }

    /**
     * Stores the selection state of a button model in this map.
     *
     * @param  key  entry key
     * @param  model  toggle model containing state
     */
    public void saveFlag( String key, ButtonModel model ) {
        map_.put( key, model.isSelected() ? TRUE : FALSE );
    }

    /**
     * Restores the selection state of a toggle button model from this map.
     *
     * @param  key  entry key
     * @param  model  toggle model to be updated with state
     */
    public void restoreFlag( String key, ButtonModel model ) {
        String value = map_.get( key );
        if ( TRUE.equals( value ) ) {
            model.setSelected( true );
        }
        else if ( FALSE.equals( value ) ) {
            model.setSelected( false );
        }
    }

    /**
     * Stores the text content of a text component in this map.
     *
     * @param  key  entry key
     * @param  textComp  component containing state
     */
    public void saveText( String key, JTextComponent textComp ) {
        map_.put( key, textComp.getText() );
    }

    /**
     * Restores the text content of a text component from this map.
     *
     * @param  key  entry key
     * @param  textComp   text component to be updated with state
     */
    public void restoreText( String key, JTextComponent textComp ) {
        if ( map_.containsKey( key ) ) {
            textComp.setText( map_.get( key ) );
        }
    }

    /**
     * Stores the selection of a combo box in this map.
     * The text representation is stored.  The selector does not need
     * to contain entries that are strings, but the save/restore will
     * only work if at least the cell renderer is a JLabel whose
     * text property is manipulated to represent (uniquely) the value.
     *
     * @param  key  entry key
     * @param  selector   selection component containing state
     */
    public void saveSelection( String key, JComboBox selector ) {
        String sval = getStringValue( selector );
        if ( sval != null ) {
            map_.put( key, getStringValue( selector ) );
        }
    }

    /**
     * Restores the selection of a combo box from this map.
     *
     * @param  key  entry key
     * @param  selector  selection component to be updated with state
     */
    public void restoreSelection( String key, JComboBox selector ) {
        if ( map_.containsKey( key ) ) {
            String value = map_.get( key );
            int ix = getSelectorIndex( selector, value );
            if ( ix >= 0 ) {
                selector.setSelectedIndex( ix );
            }
            else if ( value == null || value.trim().length() == 0 ) {
                selector.setSelectedItem( null );
            }
            else if ( selector.isEditable() ) {
                if ( selector.getModel() instanceof ColumnDataComboBoxModel ) {
                    ColumnData cdata;
                    try {
                        cdata = ((ColumnDataComboBoxModel) selector.getModel())
                               .stringToColumnData( value );
                    }
                    catch ( CompilationException e ) {
                        cdata = null;
                    }
                    selector.setSelectedItem( cdata == null ? value : cdata );
                }
                else {
                    selector.setSelectedItem( value );
                }
            }
        }
    }

    /**
     * Returns a string value representing the state of a given combo box.
     * A null return means the value either cannot be retrieved
     * or cannot be stringified.
     *
     * @param  selector  combo box
     * @return  string representation of current selection, or null
     */
    private static String getStringValue( JComboBox selector ) {
        Object item = selector.getSelectedItem();
        if ( item == null ) {
            return "";
        }
        else if ( item instanceof String ) {
            return (String) item;
        }
        int index = selector.getSelectedIndex();
        ListCellRenderer renderer = selector.getRenderer();
        JList jlist = new JList();
        Component rendered =
            renderer.getListCellRendererComponent( jlist, item, index,
                                                   false, false );
        if ( rendered instanceof JLabel ) {
            return ((JLabel) rendered).getText();
        }
        else {
            return null;
        }
    }

    /**
     * Returns the index in a selection model at which a value with a
     * given stringification can be found.
     *
     * @param  selector  combo box
     * @param  svalue    required string value
     * @return  index of selection in combo box, or -1 if not found
     */
    private static int getSelectorIndex( JComboBox selector, String svalue ) {
        int nitem = selector.getItemCount();
        if ( svalue == null || svalue.length() == 0 ) {
            for ( int i = 0; i < nitem; i++ ) {
                if ( selector.getItemAt( i ) == null ) {
                    return i;
                }
            }
        }
        for ( int i = 0; i < nitem; i++ ) {
            if ( svalue.equals( selector.getItemAt( i ) ) ) {
                return i;
            }
        }
        ListCellRenderer renderer = selector.getRenderer();
        JList jlist = new JList();
        for ( int i = 0; i < nitem; i++ ) {
            Object item = selector.getItemAt( i );
            Component rendered =
                renderer.getListCellRendererComponent( jlist, item, i,
                                                       false, false );
            if ( rendered instanceof JLabel &&
                 svalue.equals( ((JLabel) rendered).getText() ) ) {
                return i;
            }
        }
        return -1;
    }
}
