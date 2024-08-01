package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Displays a GUI for locating TAP services by subject.
 *
 * @author   Mark Taylor
 * @since    30 Jun 2015
 */
public class TapServiceFinderPanel extends JPanel {

    private final JTextField keywordField_;
    private final AndButton andButton_;
    private final JTree sTree_;
    private final Action startAct_;
    private final Action cancelAct_;
    private final List<ActionListener> listeners_;
    private final Map<TapServiceFinder.Target,JCheckBox> targetSelMap_;
    private final ExecutorService serviceReaderExecutor_;
    private TapServiceFinder serviceFinder_;
    private Future<TapServiceFinder.Service[]> serviceReader_;
    private Thread activeWorker_;
    private TapServiceFinder.Service selectedService_;
    private TapServiceFinder.Table selectedTable_;

    /** Bound property name for currently selected TapServiceFinder.Service. */
    public static final String TAP_SERVICE_PROPERTY = "TAP_SERVICE_PROPERTY";

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public TapServiceFinderPanel() {
        super( new BorderLayout() );
        listeners_ = new ArrayList<ActionListener>();
        serviceReaderExecutor_ = Executors.newCachedThreadPool();

        /* Actions to start and stop a search operation. */
        startAct_ = new AbstractAction( "Find Services" ) {
            public void actionPerformed( ActionEvent evt ) {
                if ( isEnabled() ) {
                    setWorker( createQueryWorker( createConstraint() ) );
                }
            }
        };
        cancelAct_ = new AbstractAction( "Cancel" ) {
            public void actionPerformed( ActionEvent evt ) {
                setWorker( null );
            }
        };

        /* Components for entering search parameters. */
        keywordField_ = new JTextField();
        keywordField_.addActionListener( startAct_ );
        andButton_ = new AndButton( true );
        JComponent targetLine = Box.createHorizontalBox();
        targetLine.add( new JLabel( "Match Fields: " ) );
        TapServiceFinder.Target[] targets = TapServiceFinder.Target.values();
        targetSelMap_ = new LinkedHashMap<TapServiceFinder.Target,JCheckBox>();
        for ( TapServiceFinder.Target target : targets ) {
            JCheckBox checkBox = new JCheckBox( target.getDisplayName() );
            checkBox.setSelected( true );
            checkBox.setToolTipText( "Match keywords against "
                                   + target.getDisplayName() );
            targetLine.add( Box.createHorizontalStrut( 5 ) );
            targetLine.add( checkBox );
            targetSelMap_.put( target, checkBox );
        }
        targetLine.add( Box.createHorizontalGlue() );

        /* Tree to display results. */
        sTree_ = new JTree();
        sTree_.setRootVisible( true );
        sTree_.setShowsRootHandles( false );
        sTree_.setCellRenderer( TapServiceTreeModel.createCellRenderer() );

        /* Fix it so that the basic selection type is a service,
         * but tables within that service can be selected too. */
        final TreeSelectionModel selModel = sTree_.getSelectionModel();
        selModel.setSelectionMode( TreeSelectionModel
                                  .DISCONTIGUOUS_TREE_SELECTION );
        sTree_.addTreeSelectionListener( new TreeSelectionListener() {
            public void valueChanged( TreeSelectionEvent evt ) {
                doctorSelection( selModel, evt );
            }
        } );

        /* Notify listeners if selection changes. */
        sTree_.addTreeSelectionListener( new TreeSelectionListener() {
            public void valueChanged( TreeSelectionEvent evt ) {
                updateTreeSelection( evt.getNewLeadSelectionPath() );
            }
        } );

        /* Notify listeners if a selection is made (double-click). */
        sTree_.addMouseListener( new MouseAdapter() {
            public void mousePressed( MouseEvent evt ) {
                TreePath selPath =
                    sTree_.getPathForLocation( evt.getX(), evt.getY() );
                if ( selPath != null ) {
                    updateTreeSelection( selPath );
                    if ( evt.getClickCount() == 2 ) {
                        ActionEvent actEvt =
                            new ActionEvent( this, 0, "click2" );
                        for ( ActionListener l : listeners_ ) {
                            l.actionPerformed( actEvt );
                        }
                    }
                }
            }
        } );

        JComponent keywordLine = Box.createHorizontalBox();
        keywordLine.add( new JLabel( "Keywords: " ) );
        keywordLine.add( keywordField_ );
        keywordLine.add( Box.createHorizontalStrut( 5 ) );
        keywordLine.add( andButton_ );

        JComponent buttLine = Box.createHorizontalBox();
        buttLine.add( Box.createHorizontalGlue() );
        buttLine.add( new JButton( cancelAct_ ) );
        buttLine.add( Box.createHorizontalStrut( 5 ) );
        buttLine.add( new JButton( startAct_ ) );

        JComponent entryBox = Box.createVerticalBox();
        entryBox.add( keywordLine );
        entryBox.add( targetLine );
        entryBox.add( buttLine );
        entryBox.add( Box.createVerticalStrut( 5 ) );

        JComponent treePanel = new JScrollPane( sTree_ );
        
        add( entryBox, BorderLayout.NORTH );
        add( treePanel, BorderLayout.CENTER );

        /* Ensure that when the window is first posted if not before
         * (e.g. by another component calling setServiceFinder),
         * a search for services is initiated. */
        addAncestorListener( new AncestorListener() {
            public void ancestorAdded( AncestorEvent evt ) {
                if ( serviceFinder_ == null ) {
                    setServiceFinder( new GlotsServiceFinder() );
                }
                TapServiceFinderPanel.this.removeAncestorListener( this );
            }
            public void ancestorMoved( AncestorEvent evt ) {
            }
            public void ancestorRemoved( AncestorEvent evt ) {
            }
        } );

        /* Initialise state. */
        setTreeModel( null );
        setWorker( null );
    }

    /**
     * Returns the TAP service currently selected in this panel's GUI.
     * 
     * @see    #TAP_SERVICE_PROPERTY
     * @return   selected service object
     */
    public TapServiceFinder.Service getSelectedService() {
        return selectedService_;
    }

    /**
     * Returns the TAP table currently selected in this panel's GUI.
     *
     * @return  selected table object
     */
    public TapServiceFinder.Table getSelectedTable() {
        return selectedTable_;
    }

    /**
     * Returns the IVOID corresponding to a given TAP service URL, if known.
     *
     * @param  serviceUrl  service URL for TAP service
     * @return  ivoid   service registry identifier, or null if not recognised
     */
    public String getIvoid( URL serviceUrl ) {
        TapServiceFinder.Service[] services = getServices();
        if ( services != null && serviceUrl != null ) {
            String surl = serviceUrl.toString();
            for ( TapServiceFinder.Service service : services ) {
                if ( surl.equals( service.getServiceUrl() ) ) {
                    String ivoid = service.getId();
                    if ( ivoid != null ) {
                        return ivoid;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Adds a listener that is notified if a selection is made.
     * This currently corresponds to a double-click on the tree.
     *
     * @param   l  listener to add
     */
    public void addActionListener( ActionListener l ) {
        listeners_.add( l );
    }

    /**
     * Removes a previously added action listener.
     *
     * @param  l  listener to remove
     */
    public void removeActionListener( ActionListener l ) {
        listeners_.remove( l );
    }

    /**
     * Returns the most recently read list of all TAP services.
     * If none is currently available, null is returned.
     * This method does not block, and is suitable for invocation from
     * the event dispatch thread.
     *
     * @return  list of TAP services, or null
     */
    private TapServiceFinder.Service[] getServices() {
        Future<TapServiceFinder.Service[]> serviceReader = serviceReader_;
        if ( serviceReader != null ) {
            try {
                return serviceReader.get( 0, TimeUnit.MILLISECONDS );
            }
            catch ( ExecutionException e ) {
                return null;
            }
            catch ( InterruptedException e ) {
                return null;
            }
            catch ( TimeoutException e ) {
                return null;
            }
        }
        else {
            return null;
        }
    }

    /**
     * Sets the object which will locate TAP services.
     * Calling this method initiates an asynchronous search for TAP services.
     *
     * @param  finder  new finder
     */
    public void setServiceFinder( final TapServiceFinder finder ) {
        if ( serviceReader_ != null ) {
            serviceReader_.cancel( true );
        }
        serviceFinder_ = finder;

        /* Initiate a read of all TAP services. */
        serviceReader_ = serviceReaderExecutor_
                        .submit( new Callable<TapServiceFinder.Service[]>() {
            public TapServiceFinder.Service[] call() throws IOException {
                return finder.readAllServices();
            }
        } );

        /* Schedule a worker to display the result when it is obtained,
         * if no other worker has been installed by then. */
        setWorker( createQueryWorker( createConstraint() ) );
    }

    /**
     * Returns the object which locates TAP services.
     *
     * @return  finder
     */
    public TapServiceFinder getServiceFinder() {
        return serviceFinder_;
    }

    /**
     * Sets the tree model to be displayed.
     *
     * @param  treeModel   model, may be null
     */
    private void setTreeModel( TreeModel treeModel ) {
        sTree_.setModel( treeModel == null ? new TapServiceTreeModel( null )
                                           : treeModel );
    }

    /**
     * Prepares a query constraint object based on the current state
     * of this component.
     *
     * @return   TAP service query constraint,
     *           or null if no valid query is specified
     */
    private TapServiceFinder.Constraint createConstraint() {

        /* Get search terms. */
        String wordTxt = keywordField_.getText();
        if ( wordTxt == null || wordTxt.trim().length() == 0 ) {
            return null;
        }
        final String[] words = wordTxt.trim().split( "\\s+" );

        /* Get search targets. */
        List<TapServiceFinder.Target> targetList =
            new ArrayList<TapServiceFinder.Target>();
        for ( TapServiceFinder.Target target : targetSelMap_.keySet() ) {
            if ( targetSelMap_.get( target ).isSelected() ) {
                targetList.add( target );
            }
        }
        final TapServiceFinder.Target[] targets =
            targetList.toArray( new TapServiceFinder.Target[ 0 ] );
        if ( targets.length == 0 ) {
            return null;
        }

        /* Determine search term combination logic. */
        final boolean isAnd = andButton_.isAnd();

        /* Bundle them all up and return. */
        return new TapServiceFinder.Constraint() {
            public String[] getKeywords() {
                return words;
            }
            public TapServiceFinder.Target[] getTargets() {
                return targets;
            }
            public boolean isAndKeywords() {
                return isAnd;
            }
        };
    }

    /**
     * Constructs a running thread that executes a given search for
     * TAP services and on completion will
     * <ol>
     * <li>uninstall itself</li>
     * <li>if still installed either update this component's
     *     tree model in the GUI or post a popup window explaining
     *     what went wrong</li>
     * </ol>
     *
     * @param  constraint   describes the service search to be performed
     * @return  worker thread, not started yet
     */
    private Thread
            createQueryWorker( final TapServiceFinder.Constraint constraint ) {
        return new Thread( "TAP subject query" ) {
            private final Thread thisWorker = this;
            public void run() {
                if ( isActive() ) {
                    try {

                        /* Indicate that there may be a delay. */
                        submit( new Runnable() {
                            public void run() {
                                TreeModel smodel =
                                    new TapServiceTreeModel( "Searching ..." );
                                setTreeModel( smodel );
                            }
                        } );

                        /* Obtain the list of all known services.
                         * This may block, but only the first time it's
                         * called. */
                        TapServiceFinder.Service[] services =
                            serviceReader_.get();

                        /* Create a tree to display.  This may require
                         * another search. */
                        final TreeModel tModel =
                            TapServiceTreeModel
                           .readTreeModel( services, serviceFinder_,
                                           constraint );

                        /* On success, pass the result to the GUI. */
                        submit( new Runnable() {
                            public void run() {
                                setTreeModel( tModel );
                            }
                        } );
                    }

                    /* On failure, pop up an error report. */
                    catch ( IOException err ) {
                        showError( "Service search error", err );
                    }
                    catch ( ExecutionException err ) {
                        showError( "Service search error", err.getCause() );
                    }
                    catch ( InterruptedException err ) {
                        showError( "Service search interrupted", err );
                        Thread.currentThread().interrupt();
                    }

                    /* In any case, uninstall ourself. */
                    finally {
                        submit( new Runnable() {
                            public void run() {
                                assert isActive();
                                setWorker( null );
                                assert ! isActive();
                            }
                        } );
                    }
                }
            }

            /**
             * Executes a supplied runnable on the Event Dispatch Thread
             * if this thread is still the unique active worker for
             * its host panel.
             *
             * @param   r  runnable to execute if circumstances are favourable
             */
            private void submit( final Runnable r ) {
                if ( isActive() ) {
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            if ( isActive() ) {
                                r.run();
                            }
                        }
                    } );
                }
            }

            /**
             * Pops up an error dialog on the Event Dispatch Thread
             * if this thread is still the unique active worker for
             * its host panel.
             *
             * @param  title  error heading
             * @return   err  exception
             */
            private void showError( final String title, final Throwable err ) {
                submit( new Runnable() {
                    public void run() {
                        ErrorDialog.showError( TapServiceFinderPanel.this,
                                               title, err );
                    }
                } );
            }

            /**
             * Indicates whether this thread is still the unique active worker
             * for its host panel.
             *
             * @return  true  iff this panel is active
             */
            private boolean isActive() {
                return this == activeWorker_;
            }
        };
    }

    /**
     * Installs a given worker thread as the sole active worker for this
     * panel.
     *
     * @param  worker  thread to install, not started yet
     */
    private void setWorker( Thread worker ) {

        /* This atomic update has the effect of simultaneously installing
         * the supplied worker and uninstalling any previously active one. */
        activeWorker_ = worker;
        if ( worker != null ) {
            worker.setDaemon( true );
            worker.start();
        }

        /* Update actions accordingly. */
        boolean hasWorker = activeWorker_ != null;
        cancelAct_.setEnabled( hasWorker );
        startAct_.setEnabled( ! hasWorker );
    }

    /**
     * Fixes up the selection model for the JTree displaying TAP service
     * results.  The selection model allows multiple selections, but the
     * idea is that only one service should ever be selected, and at most
     * one child (probably a table) of that service in addition.
     * It's so that you can point at a table and cause selection of the
     * corresponding service, or point at the service itself.
     * Enforce this by listening to the selection model and making
     * adjustments to it every time it changes.
     *
     * @param  selModel  tree selection model
     * @param  evt   tree selection event
     */
    private void doctorSelection( TreeSelectionModel selModel,
                                  TreeSelectionEvent evt ) {

        /* Selection is empty - do nothing. */
        TreePath newPath = evt.getNewLeadSelectionPath();
        if ( newPath == null ) {
            return;
        }

        /* Get the service corresponding to the recently added path. */
        TreePath servicePath = TapServiceTreeModel.getServicePath( newPath );

        /* Get all the currently selected paths. */
        Set<TreePath> currentPaths = new HashSet<TreePath>();
        TreePath[] cps = selModel.getSelectionPaths();
        if ( cps != null ) {
            currentPaths.addAll( Arrays.asList( cps ) );
        }

        /* Prepare a list of the paths not present that should be,
         * and those present that should not be.  Make sure these are
         * both empty if the current state is OK to avoid infinite loops. */
        Set<TreePath> pathsToAdd = new HashSet<TreePath>();
        Set<TreePath> pathsToRemove = new HashSet<TreePath>();

        /* If the added node is a service node, remove any paths belonging
         * to different service nodes. */
        if ( servicePath != null && servicePath.equals( newPath ) ) {
            for ( TreePath p : selModel.getSelectionPaths() ) { 
                TreePath sp1 = TapServiceTreeModel.getServicePath( p );
                if ( sp1 != null && ! sp1.equals( servicePath ) ) {
                    pathsToRemove.add( p );
                }
            }
        }

        /* If the added node is a desendant of a service node, make sure
         * that its service is present, and that no other nodes descending
         * from its service (siblings etc) are present. */
        else if ( servicePath != null ) {
            if ( ! currentPaths.contains( servicePath ) ) {
                pathsToAdd.add( servicePath );
            }
            for ( TreePath p : currentPaths ) {
                if ( servicePath
                    .equals( TapServiceTreeModel.getServicePath( p ) ) &&
                     ! p.equals( servicePath ) &&
                     ! p.equals( newPath ) ) { 
                    pathsToRemove.add( p );
                }
            }
        }

        /* Update the selection model as determined. */
        if ( pathsToAdd.size() > 0 ) {
            selModel.addSelectionPaths( pathsToAdd
                                       .toArray( new TreePath[ 0 ] ) );
        }
        if ( pathsToRemove.size() > 0 ) {
            selModel.removeSelectionPaths( pathsToRemove
                                          .toArray( new TreePath[ 0 ] ) );
        }
    }

    /**
     * Forces an update of the currently selected service to that
     * indicated by a tree path.  Should be called if the user has
     * selected a new tree row.
     *
     * @param  path  new selection path (may be null)
     */
    private void updateTreeSelection( TreePath path ) {
        TapServiceFinder.Service oldService = selectedService_;
        selectedService_ = TapServiceTreeModel.getService( path );
        selectedTable_ = TapServiceTreeModel.getTable( path );

        /* Jump through an extra hoop to ensure that property listeners
         * are informed even if the property hasn't actually changed.
         * This might be a mild abuse of the PropertyChangeListener contract,
         * but it should be harmless, and it's required in this case to
         * make sure the TAP URL is updated to the currently selected
         * value even if the selected value in the tree hasn't changed
         * (since the user might have changed it in other components). */
        if ( oldService != null && oldService.equals( selectedService_ ) ) {
            firePropertyChange( TAP_SERVICE_PROPERTY, oldService, null );
            oldService = null;
        }
        firePropertyChange( TAP_SERVICE_PROPERTY,
                            oldService, selectedService_ );
    }

    /**
     * Tests the GUI.
     */
    public static void main( String[] args ) {
        javax.swing.JFrame frm = new javax.swing.JFrame();
        TapServiceFinderPanel tsp = new TapServiceFinderPanel();
        tsp.keywordField_.setText( "brown dwarf" );
        tsp.andButton_.setAnd( true );
        tsp.setPreferredSize( new java.awt.Dimension( 420, 200 ) );
        frm.getContentPane().add( tsp );
        frm.pack();
        frm.setVisible( true );
    }
}
