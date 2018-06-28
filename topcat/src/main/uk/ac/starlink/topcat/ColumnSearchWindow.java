package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.Box;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.util.gui.ComboBoxBumper;
import uk.ac.starlink.util.gui.ShrinkWrapper;
import uk.ac.starlink.util.gui.ValueButtonGroup;

/**
 * Dialog window that allows the user to search for text content in
 * cells of a column and highlight the corresponding rows.
 * This dialog is associated with the TableViewerWindow.
 *
 * @author   Mark Taylor
 * @since    28 Jun 2018
 */
public class ColumnSearchWindow extends AuxDialog {

    private final TableViewerWindow viewWindow_;
    private final TopcatModel tcModel_;
    private final JComboBox colSelector_;
    private final ValueButtonGroup<SearchSyntax> syntaxGroup_;
    private final ValueButtonGroup<SearchScope> scopeGroup_;
    private final JCheckBox caseToggle_;
    private final JTextField txtField_;
    private final ToggleButtonModel tackModel_;
    private final JProgressBar progBar_;
    private final Action searchAct_;
    private final Action cancelAct_;
    private ExecutorService executor_;
    private Future<?> searchJob_;

    /**
     * Constructor.
     *
     * @param  viewWindow  table viewer window
     * @param  tcModel  topcat model
     */
    public ColumnSearchWindow( TableViewerWindow viewWindow,
                               TopcatModel tcModel ) {
        super( "Search Column", viewWindow );
        viewWindow_ = viewWindow;
        tcModel_ = tcModel;
        ActionForwarder forwarder = new ActionForwarder();

        /* Column selector. */
        ColumnComboBoxModel colselModel =
                new RestrictedColumnComboBoxModel( tcModel.getColumnModel(),
                                                   false ) {
            public boolean acceptColumn( ColumnInfo info ) {
                return canSearchColumn( info );
            }
        };
        colSelector_ = new JComboBox( colselModel );
        colSelector_.setRenderer( new ColumnCellRenderer( colSelector_ ) );
        colSelector_.addActionListener( forwarder );
        JComponent colselLine = Box.createHorizontalBox();
        colselLine.add( colSelector_ );
        colselLine.add( Box.createHorizontalStrut( 5 ) );
        colselLine.add( new ComboBoxBumper( colSelector_ ) );

        /* Syntax selector. */
        syntaxGroup_ = new ValueButtonGroup<SearchSyntax>();
        JComponent syntaxBox = Box.createHorizontalBox();
        for ( SearchSyntax syntax : SearchSyntax.values() ) {
            JRadioButton butt = new JRadioButton( syntax.toString() );
            syntaxGroup_.add( butt, syntax );
            syntaxBox.add( butt );
        }
        syntaxGroup_.addChangeListener( forwarder );
        syntaxGroup_.setValue( SearchSyntax.values()[ 0 ] );

        /* Scope selector. */
        scopeGroup_ = new ValueButtonGroup<SearchScope>();
        JComponent scopeBox = Box.createHorizontalBox();
        for ( SearchScope scope : SearchScope.values() ) {
            JRadioButton butt = new JRadioButton( scope.toString() );
            scopeGroup_.add( butt, scope );
            scopeBox.add( butt );
        }
        scopeGroup_.addChangeListener( forwarder );
        scopeGroup_.setValue( SearchScope.values()[ 0 ] );

        /* Other input components. */
        caseToggle_ = new JCheckBox();
        txtField_ = new JTextField();
        txtField_.getCaret().addChangeListener( forwarder );

        /* Action to perform search. */
        searchAct_ =
                new BasicAction( "Search", null,
                                 "Search selected column for target string" ) {
            public void actionPerformed( ActionEvent evt ) {
                final Search search = createSearch();
                if ( search != null ) {
                    cancelSearch();
                    searchJob_ = getExecutor().submit( new Runnable() {
                        public void run() {
                            performSearch( search );
                        }
                    } );
                    updateActions();
                };
            }
        };
        forwarder.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                updateActions();
            }
        } );
        cancelAct_ = new BasicAction( "Stop", null,
                                      "Interrupt running search" ) {
            public void actionPerformed( ActionEvent evt ) {
                cancelSearch();
            }
        };
        updateActions();

        /* Button model for pinning the window open. */
        tackModel_ = new ToggleButtonModel( "Stay Open", ResourceIcon.KEEP_OPEN,
                                            "Keep window open"
                                          + " even after successful search" );

        /* Hitting return in text field starts search. */
        txtField_.addActionListener( searchAct_ );

        /* Place components. */
        LabelledComponentStack stack = new LabelledComponentStack();
        stack.addLine( "Column", colselLine );
        stack.addLine( "Syntax", syntaxBox );
        stack.addLine( "Scope", scopeBox );
        stack.addLine( "Case Sensitive", caseToggle_ );
        stack.addLine( "Search Text", txtField_ );
        stack.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
        JComponent controlLine = Box.createHorizontalBox();
        controlLine.add( Box.createHorizontalGlue() );
        controlLine.add( new JButton( cancelAct_ ) );
        controlLine.add( Box.createHorizontalStrut( 10 ) );
        controlLine.add( new JButton( searchAct_ ) );
        controlLine.add( Box.createHorizontalGlue() );
        controlLine.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
        progBar_ = placeProgressBar();
        JComponent main = new JPanel( new BorderLayout() );
        main.add( stack, BorderLayout.NORTH );
        main.add( controlLine, BorderLayout.SOUTH );
        getContentPane().add( main );

        /* Prepare for display. */
        getToolBar().add( tackModel_.createToolbarButton() );
        getToolBar().addSeparator();
        JMenu windowMenu = getWindowMenu();
        windowMenu.insert( tackModel_.createMenuItem(),
                           windowMenu.getItemCount() - 2 );
        addHelp( "ColumnSearchWindow" );
        pack();
    }

    /**
     * Indicates whether a given column can be searched by this window.
     *
     * @param  info   column metadata
     * @return   true iff this window is prepared to search contents of
     *                the described column
     */
    public boolean canSearchColumn( ColumnInfo info ) {
        Class<?> clazz = info.getContentClass();
        return String.class.isAssignableFrom( clazz )
            || Number.class.isAssignableFrom( clazz )
            || Boolean.class.isAssignableFrom( clazz );
    }

    /**
     * Programmatically configures the column to be searched by this window.
     *
     * @param   tcol  column
     */
    public void setColumn( TableColumn tcol ) {
        colSelector_.setSelectedItem( tcol );
    }

    @Override
    public void dispose() {
        cancelSearch();
        super.dispose();
    }

    /**
     * Cancels a running search operation.
     */
    private void cancelSearch() {
        if ( searchJob_ != null ) {
            searchJob_.cancel( true );
            searchJob_ = null;
        }
        progBar_.setModel( new DefaultBoundedRangeModel() );
        updateActions();
    }

    /**
     * Updates enabled state of the Cancel and Search actions
     * based on the current state of this window.
     */
    private void updateActions() {
        boolean isRunning = searchJob_ != null && !searchJob_.isDone();
        cancelAct_.setEnabled( isRunning );
        searchAct_.setEnabled( !isRunning && createSearch() != null );
    }

    /**
     * Creates a search specification based on the current configuration
     * of this window's input components.
     *
     * @return   search object if the GUI is configured to provide one,
     *           null if it is not
     */
    private Search createSearch() {
        TableColumn tcol = (TableColumn) colSelector_.getSelectedItem();
        String txt = txtField_.getText();
        if ( tcol != null && txt != null && txt.trim().length() > 0 ) {
            int jcol = tcol.getModelIndex();
            SearchSyntax syntax = syntaxGroup_.getValue();
            SearchScope scope = scopeGroup_.getValue();
            boolean isCaseSensitive = caseToggle_.isSelected();
            String regex = syntax.getRegex( txt );
            int flags = isCaseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
            final Pattern pattern;
            try {
                pattern = Pattern.compile( regex, flags );
            }
            catch ( PatternSyntaxException e ) {
                return null;
            }
            return new Search( jcol, pattern, scope );
        }
        else {
            return null;
        }
    }

    /**
     * Returns an executor to which jobs can be submitted.
     *
     * @return  lazily-created single-threaded executor
     */
    private ExecutorService getExecutor() {
        if ( executor_ == null ) {
            executor_ = Executors.newSingleThreadExecutor( new ThreadFactory() {
                public Thread newThread( Runnable r ) {
                    Thread thread = new Thread( r, "Searcher" );
                    thread.setDaemon( true );
                    return thread;
                }
            } );
        }
        return executor_;
    }

    /**
     * Does the work of searching according to a given specification,
     * updating the GUI asynchronously as it goes.
     *
     * <p>This method must be invoked from a non-EDT thread, and it
     * checks for thread interruption status as it goes.
     *
     * @param  search  search specification
     */
    private void performSearch( Search search ) {
        int jcol = search.jcol_;
        Pattern pattern = search.pattern_;
        SearchScope scope = search.scope_;
        StarTable dataModel = tcModel_.getDataModel();
        final ViewerTableModel viewModel = tcModel_.getViewModel();
        final ListSelectionModel rowSelModel =
            viewWindow_.getRowSelectionModel();
        long nfind = 0;
        long irow0 = -1;
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                rowSelModel.clearSelection();
            }
        } );

        /* Set up a progress bar.  Since we install a new model, this
         * will have the effect of throwing out any model that is still
         * being updated by previous invocations of this method. */
        int nrow = viewModel.getRowCount();
        final BoundedRangeModel progModel =
            new DefaultBoundedRangeModel(0, 0, 0, nrow );
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                progBar_.setModel( progModel );
            }
        } );

        /* We update the GUI as we go along.  If there are many matches,
         * this can be much slower than doing them all at once at the end.
         * However, it gives the user a chance to see progress and interrupt
         * if required.  This window isn't really intended for large numbers
         * of matches, so it's likely that the user will want to interrupt
         * it if it takes a long time anyway. */
        /* The alternative would be to identify all the matches and then
         * update the ListSelectionModel at the end.  The elapsed time
         * in that case *should* be much faster.
         * One problem with that is that if there is a very large number of
         * matches, that ListSelectionModel update can take a very long time,
         * and ties up the EDT.  That must be down to the implementation of
         * the DefaultListSelectionModel used for JTable row selection in the
         * TableViewerWindow and (probably) the way it talks to its listeners.
         * The ListSelectionModel itself only allows you to update content
         * by multiple calls to addSelectionInterval, so many calls are
         * potentially required.  To make these bulk updates in a safe way,
         * it would probably be necessary to implement a custom
         * ListSelectionModel that can be updated in bulk.
         * That wouldn't be too hard: you could steal (e.g.) the OpenJDK-6
         * DefaultListSelectionModel implementation which is based on a BitSet,
         * and just provide a public method to update the BitSet content
         * in one go. */
        long nextUpdate = System.currentTimeMillis();
        int krow = 0;
        for ( Iterator<Long> irowIt = viewModel.getRowIndexIterator();
              irowIt.hasNext() && ! Thread.currentThread().isInterrupted(); ) {
            long irow = irowIt.next().longValue();
            Object cell;
            try {
                cell = dataModel.getCell( irow, jcol );
            }
            catch ( IOException e ) {
                cell = null;
            }
            if ( cell != null ) {
                String txt = cell.toString();
                if ( scope.matches( pattern.matcher( txt ) ) ) {
                    final int jrow = viewModel.getViewRow( irow );
                    final boolean isFirst = nfind++ == 0;
                    if ( isFirst ) {
                        irow0 = irow;
                    }

                    /* Invoke the GUI update on the EDT synchronously.
                     * Otherwise, in the case of many matches,
                     * it tends to jam up the EDT with loads of
                     * scheduled requests and responsiveness suffers. */
                    try {
                        SwingUtilities.invokeAndWait( new Runnable() {
                            public void run() {
                                rowSelModel.addSelectionInterval( jrow, jrow );
                                if ( isFirst ) {
                                    viewWindow_.scrollToRow( jrow );
                                }
                            }
                        } );
                    }
                    catch ( InvocationTargetException e ) {
                        throw new RuntimeException( e );
                    }

                    /* Very important not to swallow interruptions here. */
                    catch ( InterruptedException e ) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            /* Update the progress bar if we haven't done it recently. */
            long time = System.currentTimeMillis();
            if ( time >= nextUpdate ) {
                nextUpdate = time + 200;
                final int krow0 = krow;
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        progModel.setValue( krow0 );
                    }
                } );
            }
            krow++;
        }

        /* If we complete successfully, but not if the window is tacked
         * or if no matches were found, consider disposing the window. */
        if ( !Thread.currentThread().isInterrupted() ) {
            final long nfind0 = nfind;
            final long irow00 = irow0;
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    updateActions();
                    progModel.setValue( 0 );
                    if ( nfind0 == 1 ) {
                        tcModel_.highlightRow( irow00 );
                    }
                    if ( nfind0 > 0 && ! tackModel_.isSelected() ) {
                        ColumnSearchWindow.this.dispose();
                    }
                }
            } );
        }
    }

    /**
     * Specifies a column content search.
     */
    private static class Search {
        final int jcol_;
        final Pattern pattern_;
        final SearchScope scope_;

        /**
         * Constructor.
         *
         * @param  jcol  column index in data table of column to search
         * @param  pattern   regular expression to match
         * @param  scope   search target scope
         */
        Search( int jcol, Pattern pattern, SearchScope scope ) {
            jcol_ = jcol;
            pattern_ = pattern;
            scope_ = scope;
        }
    }

    /**
     * Enum for search syntax languages.
     */
    private static enum SearchSyntax {

        /** Simple wildcards, just "*" and "?". */
        WILDCARD( "Wildcard" ) {
            public String getRegex( String txt ) {
                return ( "\\Q"
                       + txt.replaceAll( "[*]", "\\\\E.*\\\\Q" )
                            .replaceAll( "[?]", "\\\\E.\\\\Q" )
                       + "\\E" )
                      .replaceAll( "\\\\Q\\\\E", "" );
            }
        },

        /** Literal matching. */
        LITERAL( "Literal" ) {
            public String getRegex( String txt ) {
                return "\\Q" + txt + "\\E";
            }
        },

        /** Regular expressions. */
        REGEX( "Regex" ) {
            public String getRegex( String txt ) {
                return txt;
            }
        };

        final String name_;

        /**
         * Constructor.
         *
         * @param  name  user-readable syntax name
         */
        SearchSyntax( String name ) {
            name_ = name;
        }

        /**
         * Translates a match string in this syntax into a standard
         * regular expression.
         *
         * @param   txt   submitted match text in this syntax
         * @return   regular expression text corresponding to <code>txt</code>
         */
        public abstract String getRegex( String txt );

        @Override
        public String toString() {
            return name_;
        }
    }

    /**
     * Enum for search target scope.
     */
    private static enum SearchScope {

        /** Target string contains match text. */
        CONTAINS( "Contains" ) {
            public boolean matches( Matcher matcher ) {
                return matcher.find();
            }
        },

        /** Whole target string is matched by match text. */
        FULL( "Complete" ) {
            public boolean matches( Matcher matcher ) {
                return matcher.matches();
            }
        };

        final String name_;

        /**
         * Constrructor.
         *
         * @param  name  user-readable scope name
         */
        SearchScope( String name ) {
            name_ = name;
        }

        /**
         * Indicates whether a regex matcher matches according to this scope.
         *
         * @param  matcher  matcher
         * @return   true iff match is achieved
         */
        public abstract boolean matches( Matcher matcher );

        @Override
        public String toString() {
            return name_;
        }
    }
}
