package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.table.TableColumn;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.util.gui.ComboBoxBumper;
import uk.ac.starlink.util.gui.ValueButtonGroup;

/**
 * Dialog window that allows the user to search for text content in
 * cells of a column of a displayed table.
 *
 * <p>This is a partial implementation that provides the basic GUI,
 * but it must be extended to do something useful such as actually
 * perform a search and message a user with the results.
 *
 * <p>It could quite easily be generalised to search for text in
 * something that isn't a table column.
 *
 * @author   Mark Taylor
 * @since    14 Sep 2023
 */
public class ColumnSearchWindow extends AuxDialog {

    private final ActionForwarder forwarder_;
    private final JComboBox<TableColumn> colSelector_;
    private final ValueButtonGroup<SearchSyntax> syntaxGroup_;
    private final ValueButtonGroup<SearchScope> scopeGroup_;
    private final JCheckBox caseToggle_;
    private final JTextField txtField_;
    private final ToggleButtonModel tackModel_;
    private final JComponent controlBox_;
    private Action searchAct_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat" );

    /**
     * Constructor.
     * Does not pack components.
     *
     * @param  title  title of dialogue window
     * @param  owner  window that owns this dialogue
     * @param  colselLabel  label for selector of the column to use
     * @param  colSelectorModel  model for selector of the column to use
     */
    public ColumnSearchWindow( String title, Window owner,
                               String colselLabel,
                               ComboBoxModel<TableColumn> colSelectorModel ) {
        super( title, owner );
        forwarder_ = new ActionForwarder();

        /* Syntax selector. */
        syntaxGroup_ = new ValueButtonGroup<SearchSyntax>();
        JComponent syntaxBox = Box.createHorizontalBox();
        for ( SearchSyntax syntax : SearchSyntax.values() ) {
            JRadioButton butt = new JRadioButton( syntax.toString() );
            syntaxGroup_.add( butt, syntax );
            syntaxBox.add( butt );
        }
        syntaxGroup_.addChangeListener( forwarder_ );
        syntaxGroup_.setValue( SearchSyntax.values()[ 0 ] );

        /* Scope selector. */
        scopeGroup_ = new ValueButtonGroup<SearchScope>();
        JComponent scopeBox = Box.createHorizontalBox();
        for ( SearchScope scope : SearchScope.values() ) {
            JRadioButton butt = new JRadioButton( scope.toString() );
            scopeGroup_.add( butt, scope );
            scopeBox.add( butt );
        }
        scopeGroup_.addChangeListener( forwarder_ );
        scopeGroup_.setValue( SearchScope.values()[ 0 ] );

        /* Other input components. */
        caseToggle_ = new JCheckBox();
        txtField_ = new JTextField();
        txtField_.getCaret().addChangeListener( forwarder_ );

        /* Button model for pinning the window open. */
        tackModel_ = new ToggleButtonModel( "Stay Open", ResourceIcon.KEEP_OPEN,
                                            "Keep window open"
                                          + " even after successful search" );

        /* Column selector component. */
        colSelector_ = new JComboBox<TableColumn>( colSelectorModel );
        colSelector_.setRenderer( new ColumnCellRenderer( colSelector_ ) );
        colSelector_.addActionListener( forwarder_ );
        Box colselLine = Box.createHorizontalBox();
        colselLine.add( colSelector_ );
        colselLine.add( Box.createHorizontalStrut( 5 ) );
        colselLine.add( new ComboBoxBumper( colSelector_ ) );

        /* Place components. */
        LabelledComponentStack stack = new LabelledComponentStack();
        stack.addLine( colselLabel, colselLine );
        stack.addLine( "Syntax", syntaxBox );
        stack.addLine( "Scope", scopeBox );
        stack.addLine( "Case Sensitive", caseToggle_ );
        stack.addLine( "Search Text", txtField_ );
        stack.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
        JComponent main = new JPanel( new BorderLayout() );
        controlBox_ = Box.createHorizontalBox();
        main.add( stack, BorderLayout.NORTH );
        main.add( controlBox_, BorderLayout.SOUTH );
        getContentPane().add( main );

        /* Prepare for display. */
        getToolBar().add( tackModel_.createToolbarButton() );
        getToolBar().addSeparator();
        JMenu windowMenu = getWindowMenu();
        windowMenu.insert( tackModel_.createMenuItem(),
                           windowMenu.getItemCount() - 2 );
        addHelp( "ColumnSearchWindow" );
    }

    /**
     * Returns the action forwarder used by the components in this GUI.
     *
     * @return  action forwarder
     */
    public ActionForwarder getActionForwarder() {
        return forwarder_;
    }

    /**
     * Returns a container into which control buttons may be put.
     *
     * @return  control box
     */
    public JComponent getControlBox() {
        return controlBox_;
    }

    /**
     * Returns the text component into which the user enters search terms.
     *
     * @return  search field
     */
    public JTextField getTextField() {
        return txtField_;
    }

    /**
     * Returns the component used for selecting columns to search.
     *
     * @return  column selector
     */
    public JComboBox<TableColumn> getColumnSelector() {
        return colSelector_;
    }

    /**
     * Should be called when a search is completed.
     * Must be called from the Event Dispatch Thread.
     *
     * <p>At present, the behaviour is to dispose this dialogue if the
     * search was successful, unless the GUI has been configured otherwise.
     *
     * @param  success  true iff the search was successful (terms found)
     */
    public void searchCompleted( boolean success ) {
        if ( success && ! tackModel_.isSelected() ) {
            dispose();
        }
    }

    /**
     * Programmatically configures the column to be searched by this window.
     *
     * @param   tcol  column
     */
    public void setColumn( TableColumn tcol ) {
        colSelector_.setSelectedItem( tcol );
    }

    /**
     * Creates a search specification based on the current configuration
     * of this window's input components.
     *
     * @return   search object if the GUI is configured to provide one,
     *           null if it is not
     */
    public Search createSearch() {
        TableColumn tcol = (TableColumn) colSelector_.getSelectedItem();
        if ( tcol != null ) {
            int jcol = tcol.getModelIndex();
            Pattern pattern = getPattern();
            SearchScope scope = getSearchScope();
            return pattern == null ? null : new Search( jcol, pattern, scope );
        }
        else {
            return null;
        }
    }

    /**
     * Returns a search pattern based on the current configuration
     * of this window's input components.
     *
     * @return   search pattern if the GUI is configured to provide one,
     *           otherwise null
     */
    private Pattern getPattern() {
        String txt = txtField_.getText();
        if ( txt != null && txt.trim().length() > 0 ) {
            SearchSyntax syntax = syntaxGroup_.getValue();
            boolean isCaseSensitive = caseToggle_.isSelected();
            String regex = syntax.getRegex( txt );
            int flags = isCaseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
            final Pattern pattern;
            try {
                return Pattern.compile( regex, flags );
            }
            catch ( PatternSyntaxException e ) {
                logger_.info( "Bad pattern \"" + regex + "\": " + e );
                return null;
            }
        }
        else {
            return null;
        }
    }

    /**
     * Returns the scope configured by the GUI.
     *
     * @return  search scope object, not null
     */
    private SearchScope getSearchScope() {
        return scopeGroup_.getValue();
    }

    /**
     * Specifies a column content search.
     */
    public static class Search {
        private final int jcol_;
        private final Pattern pattern_;
        private final SearchScope scope_;

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

        /**
         * Returns the model index of the column to search.
         *
         * @return  column model index
         */
        public int getColumnIndex() {
            return jcol_;
        }

        /**
         * Returns the regular expression defining the search.
         *
         * @return  compiled regex
         */
        public Pattern getPattern() {
            return pattern_;
        }

        /**
         * Returns the scope of the search.
         *
         * @return  scope
         */
        public SearchScope getScope() {
            return scope_;
        }
    }

    /**
     * Enum for search target scope.
     */
    public static enum SearchScope {

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
}
