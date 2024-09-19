package uk.ac.starlink.topcat.join;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.ttools.task.JoinFixActionParameter;

/**
 * Component for selecting a column renaming policy, used when joining tables.
 *
 * @author   Mark Taylor
 * @since    16 Jun 2014
 */
public class JoinFixSelector extends JPanel {

    private final JComboBox<Scope> scopeSelector_;
    private final JTextField suffixField_;

    /**
     * Constructor.
     */
    JoinFixSelector() {
        scopeSelector_ = new JComboBox<Scope>( Scope.values() );
        scopeSelector_.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                Scope scope = (Scope) scopeSelector_.getSelectedItem();
                suffixField_.setEnabled( scope != null && scope.usesSuffix_ );
            }
        } );
        suffixField_ = new JTextField();
        setLayout( new BoxLayout( this, BoxLayout.X_AXIS ) );
        add( scopeSelector_ );
        add( Box.createHorizontalStrut( 10 ) );
        add( new JLabel( " Suffix: " ) );
        add( suffixField_ );
        scopeSelector_.setSelectedItem( Scope.DUPS );
    }

    /**
     * Returns the column renaming policy currently selected in this component.
     *
     * @return  join fix action
     */
    public JoinFixAction getJoinFixAction() {
        Scope scope = getScope();
        String suffix = suffixField_.getText();
        return scope.createFixAct( suffix );
    }

    /**
     * Returns the STILTS parameter value
     * corresponding to the current selection.
     *
     * @return  fixer
     */
    public JoinFixActionParameter.Fixer getFixer() {
        return getScope().fixer_;
    }

    /**
     * Returns the fixer suffix corresponding to the current selection,
     * or null if the current selection doesn't make use of a suffix.
     *
     * @return   fixer suffix, or null if not applicable
     */
    public String getFixerSuffix() {
        return getScope().usesSuffix_ ? suffixField_.getText() : null;
    }

    /**
     * Returns the field giving the suffix used for column deduplication.
     *
     * @return  suffix field
     */
    public JTextField getSuffixField() {
        return suffixField_;
    }

    /**
     * Returns the currently selected scope.
     *
     * @return  scope
     */
    private Scope getScope() {
        return scopeSelector_.getItemAt( scopeSelector_.getSelectedIndex() );
    }

    @Override
    public void setEnabled( boolean isEnabled ) {
        super.setEnabled( isEnabled );
        scopeSelector_.setEnabled( isEnabled );
        suffixField_.setEnabled( isEnabled );
    }

    /** Enumerates column inclusion policy for renaming. */
    private enum Scope {

        /** Rename no columns. */
        NONE( "None", false, JoinFixActionParameter.NONE ) {
            JoinFixAction createFixAct( String suffix ) {
                return JoinFixAction.NO_ACTION;
            }
        },

        /** Rename columns with duplicate names. */
        DUPS( "Duplicates", true, JoinFixActionParameter.DUPS ) {
            JoinFixAction createFixAct( String suffix ) {
                return JoinFixAction.makeRenameDuplicatesAction( suffix );
            }
        },

        /** Rename all columns. */
        ALL( "All", true, JoinFixActionParameter.ALL ) {
            JoinFixAction createFixAct( String suffix ) {
                return JoinFixAction.makeRenameAllAction( suffix );
            }
        };
        final String name_;
        final boolean usesSuffix_;
        final JoinFixActionParameter.Fixer fixer_;

        /**
         * Constructor.
         *
         * @param  name  scope name, user-readable
         * @param  usesSuffix  true iff suffix is not ignored
         * @param  fixer  STILTS parameter value corresponding to this scope
         */
        Scope( String name, boolean usesSuffix,
               JoinFixActionParameter.Fixer fixer ) {
            name_ = name;
            usesSuffix_ = usesSuffix;
            fixer_ = fixer;
        }

        /**
         * Creates a join fix action appropriate for this scope.
         *
         * @param   suffix base for deduplication; may not be used
         * @return  join fix action
         */
        abstract JoinFixAction createFixAct( String suffix );

        @Override
        public String toString() {
            return name_;
        }
    }
}
