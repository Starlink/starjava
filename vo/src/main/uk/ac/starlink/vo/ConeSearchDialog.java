package uk.ac.starlink.vo;

import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ListSelectionModel;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import org.us_vo.www.SimpleResource;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.LoadWorker;
import uk.ac.starlink.table.gui.TableConsumer;
import uk.ac.starlink.table.gui.TableLoadDialog;

/**
 * Table load dialogue which allows cone searches.  Cone search services
 * are obtained from a registry.
 *
 * @author   Mark Taylor (Starlink)
 * @since    21 Dec 2004
 */
public class ConeSearchDialog extends JPanel implements TableLoadDialog {

    private final Action okAction_;
    private final Action cancelAction_;
    private final RegistryPanel regPanel_;
    private final ValueField raField_;
    private final ValueField decField_;
    private final ValueField srField_;
    private JDialog dialog_;
    private SearchSpec selectedSearch_;
    private static Boolean available_;
    private static final Logger logger_ = Logger.getLogger( "uk.ac.starlink" );

    /**
     * Constructor.
     */
    public ConeSearchDialog() {
        super( new BorderLayout() );

        /* Define actions for OK and Cancel buttons. */
        okAction_ = new AbstractAction( "Submit Search" ) {
            public void actionPerformed( ActionEvent evt ) {
                submitSearch();
            }
        };
        cancelAction_ = new AbstractAction( "Cancel Search" ) {
            public void actionPerformed( ActionEvent evt ) {
                cancelSearch();
            }
        };
        okAction_.setEnabled( false );

        /* Construct and configure a panel which knows how to query the
         * registry and display the result. */
        regPanel_ = new RegistryPanel();
        regPanel_.getResourceSelectionModel()
                 .setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        regPanel_.getResourceSelectionModel()
                 .addListSelectionListener( new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                okAction_.setEnabled( regPanel_.getSelectedResources()
                                               .length == 1 );
            }
        } );
        JComboBox qselector = regPanel_.getQuerySelector();
        qselector.setModel( new DefaultComboBoxModel( new String[] {
            "serviceType like 'CONE'",
        } ) );
        qselector.setSelectedIndex( 0 );
        add( regPanel_, BorderLayout.CENTER );

        /* Add controls for performing the actual cone search. */
        JComponent controlBox = Box.createVerticalBox();
        add( controlBox, BorderLayout.SOUTH );

        /* Add fields for entering query parameters. */
        raField_ = ValueField.makeRADegreesField();
        decField_ = ValueField.makeDecDegreesField();
        srField_ = ValueField.makeRadiusDegreesField();
        raField_.getEntryField().addActionListener( okAction_ );
        decField_.getEntryField().addActionListener( okAction_ );
        srField_.getEntryField().addActionListener( okAction_ );
        ControlPanel qPanel = new ControlPanel();
        qPanel.addField( raField_, new JLabel( "(J2000)" ) );
        qPanel.addField( decField_, new JLabel( "(J2000)" ) );
        qPanel.addField( srField_ );
        controlBox.add( qPanel );
        controlBox.add( Box.createVerticalStrut( 5 ) );

        /* Add OK and Cancel buttons. */
        JComponent actLine = Box.createHorizontalBox();
        actLine.add( Box.createHorizontalGlue() );
        actLine.add( new JButton( cancelAction_ ) );
        actLine.add( Box.createHorizontalStrut( 5 ) );
        actLine.add( new JButton( okAction_ ) );
        controlBox.add( actLine );

        /* Cosmetics. */
        Border lineBorder = BorderFactory.createLineBorder( Color.BLACK );
        Border gapBorder = BorderFactory.createEmptyBorder( 5, 5, 5, 5 );
        Border etchedBorder = BorderFactory.createEtchedBorder();
        regPanel_.setBorder( BorderFactory.createTitledBorder(
            lineBorder, "Available Cone Search Services" ) );
        controlBox.setBorder(
            BorderFactory.createTitledBorder(
                lineBorder, "Cone Search Parameters" ) );
        setBorder( gapBorder );
        setPreferredSize( new Dimension( 600, 500 ) );
    }

    public String getName() {
        return "Cone Search";
    }

    public String getDescription() {
        return "Obtain tables using cone search web services";
    }

    public boolean isAvailable() {
        if ( available_ == null ) {
            try {
                Class c = this.getClass();
                c.forName( "net.ivoa.www.xml.VORegistry.v0_3.Registry" );
                c.forName( "org.us_vo.www.Registry" );
                available_ = Boolean.TRUE;
            }
            catch ( Throwable th ) {
                logger_.info( "WSDL classes unavailable" + " (" + th + ")" );
                available_ = Boolean.FALSE;
            }
        }
        return available_.booleanValue();
    }

    /**
     * Returns the RA value currently filled in.
     *
     * @return   J2000 right ascension in degrees
     */
    public double getRA() {
        return raField_.getValue();
    }

    /**
     * Returns the Dec value currently filled in.
     *
     * @return   J2000 declination in degrees
     */
    public double getDec() {
        return decField_.getValue();
    }

    /**
     * Returns the search radius value currently filled in.
     *
     * @return   search radius in degrees
     */
    public double getSR() {
        return srField_.getValue();
    }

    /**
     * Returns the verbosity flag currently filled in.
     *
     * @return  verbosity flag
     */
    public int getVerb() {
        return 0;
    }

    public boolean showLoadDialog( Component parent,
                                   final StarTableFactory factory,
                                   ComboBoxModel formatModel,
                                   TableConsumer consumer ) {

        /* Check state. */
        if ( dialog_ != null ) {
            throw new IllegalStateException( "Dialogue already active" );
        }

        /* Create a dialogue containing this component. */
        Frame frame = null;
        if ( parent != null ) {
            frame = parent instanceof Frame
                  ? (Frame) parent
                  : (Frame) SwingUtilities.getAncestorOfClass( Frame.class,
                                                               parent );
        }

        /* Obtain and pop up a modal dialogue based on this component. */
        dialog_ = createDialog( parent );
        selectedSearch_ = null;
        dialog_.show();
        final SearchSpec search = selectedSearch_;

        /* If the dialog is no longer active it's been cancelled for some
         * reason - don't try to proceed. */
        if ( dialog_ == null ) {
            return false;
        }
        dialog_ = null;

        /* Recover the table asynchronously and feed it to the consumer. */
        String id = search.toString();
        new LoadWorker( consumer, id ) {
            protected StarTable attemptLoad() throws IOException {
                return search.coner_.performSearch( search.ra_, search.dec_,
                                                    search.sr_, search.verb_,
                                                    factory );
            }
        }.invoke();
        return true;
    }

    /**
     * Constructs a modal dialogue based on this component.
     *
     * @param   parent  parent component
     * @return  dialog
     */
    public JDialog createDialog( Component parent ) {

        /* Work out the new dialogue's master. */
        Frame frame = null;
        if ( parent != null ) {
            frame = parent instanceof Frame
                  ? (Frame) parent
                  : (Frame) SwingUtilities.getAncestorOfClass( Frame.class,
                                                               parent );
        }

        /* Create the dialogue containing this component. */
        JDialog dialog = new JDialog( frame, "Cone Search", true );
        dialog.getContentPane().setLayout( new BorderLayout() );
        dialog.getContentPane().add( this, BorderLayout.CENTER );

        /* Add a menu which allows selective display of resource attributes. */
        JMenu colMenu = regPanel_.makeColumnVisibilityMenu( "Display" );
        dialog.setJMenuBar( new JMenuBar() );
        dialog.getJMenuBar().add( colMenu );

        /* Prepare and return dialogue. */
        dialog.setLocationRelativeTo( parent );
        dialog.pack();
        return dialog;
    }

    /**
     * Invoked when the submit button is hit.
     * Attempts to prepare and store a SearchSpec based on the 
     * current state of this component, and deactivate and dispose 
     * the dialogue on success.
     */
    protected void submitSearch() {
        if ( dialog_ == null ) {
            return;
        }
        final SimpleResource[] ress = regPanel_.getSelectedResources();
        String errmsg;
        if ( ress.length == 1 ) {
            try {
                selectedSearch_ = new SearchSpec() {{
                    ra_ = getRA(); 
                    dec_ = getDec();
                    sr_ = getSR();
                    verb_ = getVerb();
                    coner_ = new ConeSearch( ress[ 0 ] );
                }};
                dialog_.dispose();
                return;
            }
            catch ( IllegalArgumentException e ) {
                errmsg = e.getMessage();
            }
        }
        else {
            errmsg = "No cone search service selected";
        }
        JOptionPane.showMessageDialog( dialog_, errmsg,
                                       "Search Specification Error",
                                       JOptionPane.ERROR_MESSAGE );
    }

    /**
     * Invoked when the cancel button is hit.
     * Deactivates and disposes the dialogue.
     */
    protected void cancelSearch() {
        if ( dialog_ == null ) {
            return;
        }
        dialog_.dispose();
        dialog_ = null;
    }

    /**
     * Struct-like class which specifies the details of a cone search.
     */
    private static class SearchSpec {
        ConeSearch coner_;
        double ra_;
        double dec_;
        double sr_;
        int verb_;
        URL getURL() {
            return coner_.getSearchURL( ra_, dec_, sr_, verb_ );
        }
        public String toString() {
            return coner_.toString();
        }
    }

    /**
     * Helper class to deal with GridBagLayout.
     */
    private static class ControlPanel extends JPanel {
        GridBagLayout layer = new GridBagLayout();
        Insets ins = new Insets( 2, 2, 2, 2 );
        int gx = 0;
        GridBagConstraints[] gbcs = new GridBagConstraints[] {
            new GridBagConstraints() {{
                gridx = gx++; gridy = 0; anchor = WEST; insets = ins;
            }},
            new GridBagConstraints() {{
                gridx = gx++; gridy = 0; anchor = WEST; insets = ins;
                fill = BOTH;
            }},
            new GridBagConstraints() {{
                gridx = gx++; gridy = 0; anchor = WEST; insets = ins;
                fill = HORIZONTAL;
            }},
            new GridBagConstraints() {{
                gridx = gx++; gridy = 0; anchor = WEST; insets = ins;
            }},
            new GridBagConstraints() {{
                gridx = gx++; gridy = 0;
                weightx = 1.0;
            }},
        };

        ControlPanel() {
            setLayout( layer );
        }

        void addField( ValueField vf, JComponent trailer ) {
            JComponent[] comps = new JComponent[] {
                vf.getLabel(),
                vf.getEntryField(),
                vf.getConverterSelector(),
                trailer,
                new JPanel(),
            };
            for ( int i = 0; i < comps.length; i++ ) {
                if ( comps[ i ] != null ) {
                    layer.setConstraints( comps[ i ], gbcs[ i ] );
                    add( comps[ i ] );
                    gbcs[ i ].gridy++;
                }
            }
        }

        void addField( ValueField vf ) {
            addField( vf, null );
        }
    }

}
