package uk.ac.starlink.vo;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

/**
 * Component for selecting a registry service.
 * This looks mostly like a combo box with registry URLs in it,
 * but also allows the user to query the registry for other registry services,
 * which then populate the combo box futher.
 *
 * @author   Mark Taylor
 * @since    9 Feb 2009
 */
public class RegistrySelector extends JPanel {

    private final JComboBox comboBox_;
    private final Action updateAction_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     */
    public RegistrySelector() {

        /* Set up a selector box. */
        comboBox_ = new JComboBox() {
            public Dimension getPreferredSize() {
                Dimension size = new Dimension( super.getPreferredSize() );
                size.width = Math.min( size.width, 400 );
                return size;
            }
            public void configureEditor( ComboBoxEditor editor, Object item ) {
                super.configureEditor( editor, item );
                Component comp = editor.getEditorComponent();
                if ( comp instanceof JTextComponent ) {
                    ((JTextComponent) comp).setCaretPosition( 0 );
                }
            }
        };
        comboBox_.setModel( new DefaultComboBoxModel( RegistryQuery
                                                     .REGISTRIES ) );
        comboBox_.setEditable( true );
        comboBox_.setSelectedIndex( 0 );
        comboBox_.setToolTipText( "Endpoint of VOResource 1.0"
                                + " registry service" );

        /* Set up an action to update the selector box contents. */
        updateAction_ = new AbstractAction( "Update Registry List" ) {
            public void actionPerformed( ActionEvent evt ) {
                final String reg = (String) comboBox_.getSelectedItem();
                updateAction_.setEnabled( false );
                new Thread( "Registry search" ) {
                    public void run() {
                        try {
                            final String[] acurls =
                                RegistryQuery.getSearchableRegistries( reg );
                            SwingUtilities.invokeLater( new Runnable() {
                                public void run() {
                                    updateAction_.setEnabled( true );
                                    updateSelector( acurls );
                                }
                            } );
                        }
                        catch ( IOException e ) {
                            logger_.warning( "Registry search failed: " + e );
                        }
                    }
                }.start();
            }
        };
        updateAction_.putValue( Action.SHORT_DESCRIPTION,
                                "Search the registry for searchable registries"
                              + ", and update options in the selector" );

        /* Place components. */
        setLayout( new BoxLayout( this, BoxLayout.X_AXIS ) );
        add( new JLabel( "Registry: " ) );
        add( comboBox_ );
    }

    public void setEnabled( boolean enabled ) {
        super.setEnabled( enabled );
        comboBox_.setEnabled( enabled );
    }

    /**
     * Returns the currently selected registry Access URL.
     *
     * @return  access URL
     */
    public String getUrl() {
        return (String) comboBox_.getSelectedItem();
    }

    /**
     * Returns an action which will update the list of registries by making
     * a search in the registry for suitable registry entries.
     * Although this sounds neat, at time of writing it's not much use since
     * the relevant entries in the registry are largely incorrect.
     *
     * @return   registry update action
     */
    public Action getRegistryUpdateAction() {
        return updateAction_;
    }

    /**
     * Adds new access URLs to the combo box.  Must be called from the
     * event dispatch thread.
     *
     * @param  acurls  new access URLs
     */
    private void updateSelector( String[] acurls ) {
        Vector vec = new Vector();
        vec.addAll( Arrays.asList( RegistryQuery.REGISTRIES ) );
        vec.addAll( Arrays.asList( acurls ) );
        Dimension size = comboBox_.getPreferredSize();
        comboBox_.setModel( new DefaultComboBoxModel( vec ) );
        comboBox_.setPreferredSize( size );
    }
}
