package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ListSelectionModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.us_vo.www.SimpleResource;

/**
 * Displays the parameters of a registry query and its results.
 * The URL of a registry and the text of a query are displayed at the
 * top of the window, with query submit and cancel buttons.
 * When the submit button is pushed, the specified query is performed 
 * asynchronously on the selected registry.
 *
 * <p>Subclasses can be notified of the completion of a successful query 
 * by overriding the {@link #gotData} method.
 *
 * @author   Mark Taylor (Starlink)
 * @since    21 Dec 2004
 */
public class RegistryPanel extends JPanel {

    private Thread queryWorker_;
    protected JComboBox urlSelector_;
    protected JComboBox querySelector_;
    protected Action submitQueryAction_;
    protected Action cancelQueryAction_;
    protected JScrollPane scroller_;
    protected RegistryTable regTable_;
    private JComponent workingPanel_;
    private JComponent dataPanel_;
    private List activeItems_;

    /** Known registry URLs.  */
    public static String[] KNOWN_REGISTRIES = new String[] {
        "http://voservices.net/registry/registry.asmx",
    };

    /**
     * Constructs a RegistryPanel.
     */
    public RegistryPanel() {
        super( new BorderLayout() );
        activeItems_ = new ArrayList();

        /* Define actions for submit/cancel registry query. */
        cancelQueryAction_ = new AbstractAction( "Cancel Query" ) {
            public void actionPerformed( ActionEvent evt ) {
                cancelQuery();
            }
        };
        submitQueryAction_ = new AbstractAction( "Submit Query" ) {
            public void actionPerformed( ActionEvent evt ) {
                submitQuery();
            }
        };
        activeItems_.add( submitQueryAction_ );

        /* Create the component which will hold the query parameters. */
        JComponent qBox = Box.createVerticalBox();

        /* Registry URL selector. */
        JComponent urlLine = Box.createHorizontalBox();
        urlSelector_ = new JComboBox( KNOWN_REGISTRIES ) {
            public Dimension getMaximumSize() { 
                return getPreferredSize();
            }
        };
        urlSelector_.setSelectedIndex( 0 );
        urlLine.add( new JLabel( "Registry: " ) );
        urlLine.add( urlSelector_ );
        urlLine.add( Box.createHorizontalGlue() );
        activeItems_.add( urlSelector_ );
        qBox.add( urlLine );
        qBox.add( Box.createVerticalStrut( 5 ) );

        /* Query text selector. */
        JComponent queryLine = Box.createHorizontalBox();
        querySelector_ = new JComboBox();
        querySelector_.setEditable( true );
        queryLine.add( new JLabel( "Query: " ) );
        queryLine.add( querySelector_ );
        activeItems_.add( querySelector_ );
        qBox.add( queryLine );
        qBox.add( Box.createVerticalStrut( 5 ) );

        /* Component to hold submit/cancel buttons. */
        JComponent controlLine = Box.createHorizontalBox();
        controlLine.add( Box.createHorizontalGlue() );
        controlLine.add( new JButton( cancelQueryAction_ ) );
        controlLine.add( Box.createHorizontalStrut( 5 ) );
        controlLine.add( new JButton( submitQueryAction_ ) );
        qBox.add( controlLine );
        qBox.add( Box.createVerticalStrut( 5 ) );
        
        /* Scroll pane which will hold the main data component. 
         * At any point this will hold either workingPanel_ or dataPanel_,
         * according to whether a query is currently in progress. */
        scroller_ = new JScrollPane();
        scroller_.setBorder( BorderFactory.createEtchedBorder() );

        /* Create the working panel (it will be populated when shown). */
        workingPanel_ = new JPanel( new BorderLayout() );

        /* Create the table for display of query results. */
        regTable_ = new RegistryTable();
        regTable_.setColumnSelectionAllowed( false );
        regTable_.setRowSelectionAllowed( true );
        dataPanel_ = regTable_;
        setWorking( null );
        
        /* Place components. */
        add( qBox, BorderLayout.NORTH );
        add( scroller_, BorderLayout.CENTER );
    }

    /**
     * Called from the event dispatch thread when a successful 
     * registry query which returns 1 or more records has been completed.
     * The default implementation does nothing.
     *
     * @param  resources   non-empty array of resources returned from a
     *                     successful query
     */
    protected void gotData( SimpleResource[] resources ) {
    }

    /**
     * Returns the registry query text selector.
     *
     * @return  query selector
     */
    public JComboBox getQuerySelector() {
        return querySelector_;
    }

    /**
     * Returns query endpoint selector.
     *
     * @return  registry service URL selector
     */
    public JComboBox getURLSelector() {
        return urlSelector_;
    }

    /**
     * Returns an array of all the results from the most recently completed
     * registry query.
     *
     * @return   list of query results
     */
    public SimpleResource[] getResources() {
        return regTable_.getData();
    }

    /**
     * Returns an array of any of the results from the most recent 
     * registry query which are currently selected by the user.
     *
     * @return   list of any selected query results
     */
    public SimpleResource[] getSelectedResources() {
        ListSelectionModel smodel = getResourceSelectionModel();
        List sres = new ArrayList();
        SimpleResource[] data = getResources();
        for ( int i = smodel.getMinSelectionIndex();
              i <= smodel.getMaxSelectionIndex(); i++ ) {
            if ( smodel.isSelectedIndex( i ) ) {
                sres.add( data[ i ] );
            }
        }
        return (SimpleResource[]) sres.toArray( new SimpleResource[ 0 ] );
    }

    /**
     * Invoked when the Submit button is pressed.
     * Performs an asynchronous query on the registry.
     */
    public void submitQuery() {
        
        /* Get the registry endpoint URL. */
        String regServ = (String) urlSelector_.getSelectedItem();
        final URL regURL;
        try {
            regURL = new URL( regServ );
        }
        catch ( MalformedURLException e ) {
            String msg = regServ.trim().length() == 0 
                       ? "Blank registry URL"
                       : "Bad registry URL: " + regServ;
            JOptionPane.showMessageDialog( this, msg, "Query Error",
                                           JOptionPane.ERROR_MESSAGE );
            return;
        }

        /* Get the query string. */
        final String query = (String) querySelector_.getSelectedItem();
        if ( query.trim().length() == 0 ) {
            JOptionPane.showMessageDialog( this, "Blank query string",
                                           "Query Error",
                                           JOptionPane.ERROR_MESSAGE );
        }
        
        /* Begin an asynchronous query on the registry. */
        setWorking( "Performing Registry Query" );
        Thread worker = new Thread( "Registry query" ) {
            SimpleResource[] data;
            String errmsg;
            Thread wk = this;
            public void run() {
                try {
                    SimpleResource[] dat = new RegistryInterrogator( regURL )
                                          .getResources( query );
                    if ( dat == null || dat.length == 0 ) {
                        errmsg = "No resources found for query";
                    }
                    else {
                        data = dat;
                    }
                }
                catch ( Throwable th ) {
                    errmsg = th.getMessage();
                }
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        if ( queryWorker_ == wk ) {
                            if ( errmsg != null ) {
                                JOptionPane
                               .showMessageDialog( RegistryPanel.this, errmsg,
                                                   "Query Error",
                                                   JOptionPane.ERROR_MESSAGE );
                            }
                            else {
                                regTable_.setData( data );
                                gotData( data );
                            }
                            setWorking( null );
                        }
                    }
                } );
            }
        };
        queryWorker_ = worker;
        worker.start();
    }

    /**
     * Invoked when the cancel button is pressed.
     * Deactivates the current query.
     */
    public void cancelQuery() {
        if ( queryWorker_ != null ) {
            queryWorker_.interrupt();
            queryWorker_ = null;
        }
        setWorking( null );
    }

    /**
     * Returns the selection model used by the user to select resource items
     * from a completed query.
     *
     * @return   selection model (each item will be a <tt>SimpleResource</tt>)
     */
    public ListSelectionModel getResourceSelectionModel() {
        return regTable_.getSelectionModel();
    }

    /**
     * Constructs a menu which allows the user to select which attributes
     * of each displayed resource are visible.
     *
     * @param   name  menu name
     */
    public JMenu makeColumnVisibilityMenu( String name ) {
        return ((MetaColumnModel) regTable_.getColumnModel())
              .makeCheckBoxMenu( name );
    }

    /**
     * Configures this component to be working on a query or not.
     * If <tt>message</tt> is non-null, it is displayed to the user,
     * and normal interaction is suspended.  Otherwise, normal interaction
     * is resumed.
     *
     * @param  message  user-visible text or null for ready status
     */
    private void setWorking( String message ) {
        boolean working = message != null;
        if ( ! working ) {
            scroller_.setViewportView( dataPanel_ );
        }
        else {
            JComponent msgLine = Box.createHorizontalBox();
            msgLine.add( Box.createHorizontalGlue() );
            msgLine.add( new JLabel( message ) );
            msgLine.add( Box.createHorizontalGlue() );

            JComponent progLine = Box.createHorizontalBox();
            JProgressBar progBar = new JProgressBar();
            progBar.setIndeterminate( true );
            progLine.add( Box.createHorizontalGlue() );
            progLine.add( progBar );
            progLine.add( Box.createHorizontalGlue() );

            JComponent workBox = Box.createVerticalBox();
            workBox.add( Box.createVerticalGlue() );
            workBox.add( msgLine );
            workBox.add( Box.createVerticalStrut( 5 ) );
            workBox.add( progLine );
            workBox.add( Box.createVerticalGlue() );

            workingPanel_.removeAll();
            workingPanel_.add( workBox );
            scroller_.setViewportView( workingPanel_ );
        }
        for ( Iterator it = activeItems_.iterator(); it.hasNext(); ) {
            Object item = it.next();
            if ( item instanceof Action ) {
                ((Action) item).setEnabled( ! working );
            }
            else if ( item instanceof Component ) {
                ((Component) item).setEnabled( ! working );
            }
        }
        cancelQueryAction_.setEnabled( working );
    }
}
