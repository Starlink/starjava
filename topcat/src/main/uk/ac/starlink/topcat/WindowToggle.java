package uk.ac.starlink.topcat;

import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.Icon;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * ToggleButtonModel subclass that will lazily create and show/hide
 * a window.
 *
 * @author   Mark Taylor
 * @since    20 Jul 2017
 */
public abstract class WindowToggle extends ToggleButtonModel {

    private Window window_;

    /**
     * Constructor.
     *
     * @param  name  toggle button name
     * @param  icon  toggle button icon
     * @param  descrip   toggle button description
     */
    @SuppressWarnings("this-escape")
    public WindowToggle( String name, Icon icon, String descrip ) {
        super( name, icon, descrip );
        addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                boolean isVis = isSelected();
                if ( isVis && window_ == null ) {
                    window_ = createWindow();
                    window_.addWindowListener( new WindowAdapter() {
                        @Override
                        public void windowClosing( WindowEvent evt ) {
                            if ( isSelected() ) {
                                setSelected( false );
                            }
                        }
                        @Override
                        public void windowClosed( WindowEvent evt ) {
                            if ( isSelected() ) {
                                setSelected( false );
                            }
                        }
                    } );
                }
                if ( window_ != null ) {
                    window_.setVisible( isVis );
                }
            }
        } );
    }

    /**
     * This method is called to create the window to be shown when it
     * is first required.
     *
     * @return   window to display when toggle button is selected
     */
    protected abstract Window createWindow();
}
