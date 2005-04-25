package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ListSelectionModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.us_vo.www.SimpleResource;
import uk.ac.starlink.util.gui.ErrorDialog;

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
    protected Action submitQueryAction_;
    protected Action cancelQueryAction_;
    protected JScrollPane scroller_;
    protected RegistryTable regTable_;
    private final RegistryQueryPanel queryPanel_;
    private final JComponent qBox_;
    private JComponent workingPanel_;
    private JComponent dataPanel_;
    private List activeItems_;
    private String workingMessage_;

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
        qBox_ = Box.createVerticalBox();
        queryPanel_ = new RegistryQueryPanel();
        activeItems_.add( queryPanel_ );
        qBox_.add( queryPanel_ );
        qBox_.add( Box.createVerticalStrut( 5 ) );

        /* Component to hold submit/cancel buttons. */
        JComponent controlLine = Box.createHorizontalBox();
        controlLine.add( Box.createHorizontalGlue() );
        controlLine.add( new JButton( cancelQueryAction_ ) );
        controlLine.add( Box.createHorizontalStrut( 5 ) );
        controlLine.add( new JButton( submitQueryAction_ ) );
        qBox_.add( controlLine );
        qBox_.add( Box.createVerticalStrut( 5 ) );
        
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
        add( qBox_, BorderLayout.NORTH );
        add( scroller_, BorderLayout.CENTER );
    }

    /**
     * Returns the panel which controls the registry query.
     *
     * @return  registry query panel
     */
    public RegistryQueryPanel getQueryPanel() {
        return queryPanel_;
    }

    /**
     * Invoking this method withdraws the parts of the GUI which permit the
     * user to specify a registry query, and peforms a fixed query without
     * further ado.  This effect cannot be reversed.
     *
     * @param  query  text of registry query
     * @param  workingMsg  message to display near progress bar while 
     *         query is ongoing
     */
    public void performAutoQuery( String query, String workingMsg ) {
        workingMessage_ = workingMsg;
        queryPanel_.getQuerySelector().setSelectedItem( query );
        remove( qBox_ );
        submitQuery();
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
        
        /* Get the query specification object. */
        final RegistryQuery query;
        try {
            query = queryPanel_.getRegistryQuery();
        }
        catch ( MalformedURLException e ) {
            ErrorDialog.showError( this, "Query Error", e ); 
            return;
        }

        /* Begin an asynchronous query on the registry. */
        setWorking( workingMessage_ );
        Thread worker = new Thread( "Registry query" ) {
            SimpleResource[] data;
            String errmsg;
            Thread wk = this;
            public void run() {
                Throwable error = null;
                try {
                    SimpleResource[] dat = query.performQuery();
                    if ( dat == null || dat.length == 0 ) {
                        errmsg = "No resources found for query";
                    }
                    else {
                        data = dat;
                    }
                }
                catch ( Throwable th ) {
                    error = th;
                }
                final Throwable error1 = error;
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        if ( queryWorker_ == wk ) {
                            if ( error1 != null ) {
                                ErrorDialog.showError( RegistryPanel.this,
                                                       "Query Error", error1 );
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

    public void setEnabled( boolean enabled ) {
        super.setEnabled( enabled );
        for ( Iterator it = activeItems_.iterator(); it.hasNext(); ) {
            Object item = it.next();
            if ( item instanceof Action ) {
                ((Action) item).setEnabled( enabled );
            }
            else if ( item instanceof Component ) {
                ((Component) item).setEnabled( enabled );
            }
        }
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
        setEnabled( ! working );
        cancelQueryAction_.setEnabled( working );
    }
}
