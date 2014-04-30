package uk.ac.starlink.vo;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.MutableComboBoxModel;
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
    private RegistrySelectorModel model_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructs a registry selector with a new RI1 selection model.
     */
    public RegistrySelector() {
        this( new RegistrySelectorModel( RegistryProtocol.RI1 ) );
    }

    /**
     * Constructs a registry selector with a given selection model.
     *
     * @param  model  selection model
     */
    public RegistrySelector( RegistrySelectorModel model ) {

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
        comboBox_.setEditable( true );
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
                                model_.getProtocol()
                                      .discoverRegistryUrls( reg );
                            SwingUtilities.invokeLater( new Runnable() {
                                public void run() {
                                    updateSelector( acurls );
                                }
                            } );
                        }
                        catch ( IOException e ) {
                            logger_.warning( "Registry search failed: " + e );
                        }
                        finally {
                            SwingUtilities.invokeLater( new Runnable() {
                                public void run() {
                                    updateAction_.setEnabled( true );
                                }
                            } );
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

        /* Install the model. */
        setModel( model );
    }

    /**
     * Sets the selection model for this selector.
     *
     * @param  model   new model
     */
    public void setModel( RegistrySelectorModel model ) {
        model_ = model;
        ComboBoxModel urlModel = model.getUrlSelectionModel();
        comboBox_.setModel( urlModel );
        updateAction_.setEnabled( urlModel instanceof MutableComboBoxModel );
    }

    /**
     * Returns the selection model for this selector.
     *
     * @return  model
     */
    public RegistrySelectorModel getModel() {
        return model_;
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
        ComboBoxModel model = comboBox_.getModel();

        /* Work out which entries need to be added (not already present). */
        Set<String> values = new HashSet<String>();
        for ( int i = 0; i < model.getSize(); i++ ) {
            values.add( (String) model.getElementAt( i ) );
        }
        List<String> addUrls = new ArrayList<String>();
        for ( int i = 0; i < acurls.length; i++ ) {
            String acurl = acurls[ i ];
            if ( values.add( acurl ) ) {
                addUrls.add( acurl );
            }
        }

        /* Add them. */
        if ( addUrls.size() > 0 ) {
            if ( model instanceof MutableComboBoxModel ) {
                MutableComboBoxModel mmodel = (MutableComboBoxModel) model;
                for ( String url : addUrls ) {
                    mmodel.addElement( url );
                }
            }
            else {
                logger_.warning( "Can't add access URLs to immutable combo box"
                               + " (" + addUrls.size() + " new URLs ignored)" );
            }
        }
    }
}
