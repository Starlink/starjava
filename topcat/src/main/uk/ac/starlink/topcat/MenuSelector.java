package uk.ac.starlink.topcat;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

/**
 * Provides a menu item with a submenu that can select one of a given
 * set of options.  At present, the list of options is fixed.
 *
 * @author   Mark Taylor
 * @since    19 Sep 2017
 */
public class MenuSelector<T> {

    private final JMenu menu_;
    private final List<ActionListener> listeners_;
    private T selected_;

    /**
     * Constructs a menu selector with an explicit default.
     *
     * @param  name   menu name
     * @param  options  list of options that can be selected
     * @param  dflt    initial default value;
     *                 should be one of <code>options</code>
     */
    @SuppressWarnings("this-escape")
    public MenuSelector( String name, T[] options, T dflt ) {
        menu_ = new JMenu( name );
        listeners_ = new ArrayList<ActionListener>(); 
        ButtonGroup grp = new ButtonGroup();
        for ( T opt : options ) {
            final T option = opt;
            final OptionButton<T> butt = new OptionButton<T>( option );
            butt.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent evt ) {
                    if ( butt.isSelected() ) {
                        if ( selected_ != option ) {
                            selected_ = option;
                            fireActionEvent( evt );
                        }
                    }
                }
            } );
            grp.add( butt );
            menu_.add( butt );
        }
        setSelectedItem( dflt );
    }

    /**
     * Constructs a menu selector with an implicit default,
     * the first element of the supplied options array.
     *
     * @param  name   menu name
     * @param  options  list of options that can be selected
     */
    public MenuSelector( String name, T[] options ) {
        this( name, options, options.length > 0 ? options[ 0 ] : null );
    }

    /**
     * Returns the menu item that presents this selector.
     * It has a submenu hanging off it that provides the actual
     * selection options (currently JRadioButtonMenuItems).
     *
     * @return  menu item
     */
    public JMenuItem getMenuItem() {
        return menu_;
    }

    /**
     * Returns the currently selected option.
     *
     * @return  selected option
     */
    public T getSelectedItem() {
        return selected_;
    }

    /**
     * Sets the currently selected option.
     *
     * @param  option  option to select
     * @throws   IllegalArgumentException  if <code>option</code> is not
     *           one of the options
     */
    public void setSelectedItem( T option ) {
        int n = menu_.getItemCount();
        for ( int i = 0; i < n; i++ ) {
            JMenuItem menuItem = menu_.getItem( i );
            if ( menuItem instanceof OptionButton &&
                 ((OptionButton<?>) menuItem).option_.equals( option ) ) {
                menuItem.doClick();
                return;
            }
        }
        throw new IllegalArgumentException( "Not a known option: " + option );
    }

    /**
     * Adds a listener that will be notified whenever the selection changes.
     *
     * @param  l  listener to add
     */
    public void addActionListener( ActionListener l ) {
        listeners_.add( l );
    }

    /**
     * Removes a previously-added listener.
     *
     * @param  l  listener to remove
     */
    public void removeActionListener( ActionListener l ) {
        listeners_.remove( l );
    }

    /**
     * Notifies listeners of an event.
     *
     * @param   evt  event to pass on
     */
    private void fireActionEvent( ActionEvent evt ) {
        for ( ActionListener l : listeners_ ) {
            l.actionPerformed( evt );
        }
    }

    /**
     * Submenu item.
     */
    private static class OptionButton<T> extends JRadioButtonMenuItem {
        final T option_;

        /**
         * Constructor.
         *
         * @param  option  option this menu item chooses
         */
        OptionButton( T option ) {
            super( String.valueOf( option ) );
            option_ = option;
        }
    }
}
