package uk.ac.starlink.topcat.activate;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.topcat.LineBox;
import uk.ac.starlink.topcat.Outcome;
import uk.ac.starlink.topcat.Safety;
import uk.ac.starlink.topcat.TopcatJELUtils;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.jel.RandomJELRowReader;

/**
 * ActivationType that allows user to execute custom code
 * using TOPCAT's expression language.
 *
 * <p>The expression that is evaluated may return an
 * {@link uk.ac.starlink.topcat.Outcome}; otherwise the output is
 * stringified and turned into an Outcome.
 *
 * @author   Mark Taylor
 * @since    23 Jan 2018
 */
public class JelActivationType implements ActivationType {

    public String getName() {
        return "Execute code";
    }

    public String getDescription() {
        return "Executes custom code using the expression language";
    }

    public ActivatorConfigurator createConfigurator( TopcatModelInfo tinfo ) {
        return new JelConfigurator( tinfo.getTopcatModel() );
    }

    public Suitability getSuitability( TopcatModelInfo tinfo ) {
        return Suitability.PRESENT;
    }

    /**
     * Configurator implementation for JEL.
     */
    private static class JelConfigurator extends AbstractActivatorConfigurator {

        private final TopcatModel tcModel_;
        private final JTextPane textPanel_;
        private final JCheckBox syncSelector_;
        private static final String TEXT_KEY = "text";
        private static final String SYNC_KEY = "sync";

        /**
         * Constructor.
         *
         * @param  tcModel   topcat model
         */
        JelConfigurator( TopcatModel tcModel ) {
            super( new JPanel( new BorderLayout() ) );
            JComponent panel = getPanel();
            tcModel_ = tcModel;
            textPanel_ = new JTextPane();
            syncSelector_ = new JCheckBox( "Synchronous", true );

            /* Ensure listeners are informed when configuration status
             * may have changed. */
            syncSelector_.addActionListener( getActionForwarder() );
            textPanel_.getCaret().addChangeListener( getActionForwarder() );

            /* Arrange components. */
            panel.add( new LineBox( "Executable Expression", null ),
                       BorderLayout.NORTH );
            panel.add( new JScrollPane( textPanel_ ), BorderLayout.CENTER );
            panel.add( new LineBox( null, syncSelector_ ), BorderLayout.SOUTH );
        }

        public Activator getActivator() {
            final ExecuteKit kit;
            try {
                kit = createKit();
            }
            catch ( CompilationException e ) {
                return null;
            }
            if ( kit == null ) {
                return null;
            }
            final boolean isSync = syncSelector_.isSelected();
            return new Activator() {
                public boolean invokeOnEdt() {
                    return isSync;
                }
                public Outcome activateRow( long lrow, ActivationMeta meta ) {
                    return kit.executeAtRow( lrow );
                }
            };
        }

        public String getConfigMessage() {
            try {
                return createKit() == null
                     ? "No expression"
                     : null;
            }
            catch ( CompilationException e ) {
                return "Expression error: " + e.getMessage();
            }
        }

        public Safety getSafety(){ 
            String expr = textPanel_.getText();
            return expr == null || expr.trim().length() == 0
                         ? Safety.SAFE
                         : Safety.UNSAFE;
        }

        public ConfigState getState() {
            ConfigState state = new ConfigState();
            state.saveText( TEXT_KEY, textPanel_ );
            state.saveFlag( SYNC_KEY, syncSelector_.getModel() );
            return state;
        }

        public void setState( ConfigState state ) {
            state.restoreText( TEXT_KEY, textPanel_ );
            state.restoreFlag( SYNC_KEY, syncSelector_.getModel() );
        }

        /**
         * Attempts to prepare an object that can execute the currently
         * configured expression.
         *
         * @return   execution kit, or null if expression is blank
         */
        ExecuteKit createKit() throws CompilationException {
            String expr = textPanel_.getText();
            if ( expr == null || expr.trim().length() == 0 ) {
                return null;
            }
            RandomJELRowReader rowReader = tcModel_.createJELRowReader();
            Library lib = TopcatJELUtils.getLibrary( rowReader, true );
            CompiledExpression compEx = Evaluator.compile( expr, lib, null );
            return new ExecuteKit( rowReader, compEx );
        }
    }

    /**
     * Can execute an expression at a given row.
     */
    private static class ExecuteKit {
        final RandomJELRowReader rdr_;
        final CompiledExpression compEx_;

        /**
         * Constructor.
         *
         * @param  rdr  row reader
         * @param  compEx  compiled expression for use with <code>rdr</code>
         */
        ExecuteKit( RandomJELRowReader rdr, CompiledExpression compEx ) {
            rdr_ = rdr;
            compEx_ = compEx;

            /* Set the failOnNull flag of the row reader true.
             * For activation actions the
             * side-effects are important rather than the results,
             * so it's important not to evaluate functions with bogus
             * (zero-instead-of-null) parameters in this context.
             * The default behaviour, which is evaluating them and
             * ignoring the result, is not sufficient here. */
            rdr_.setFailOnNull( true );
        }

        /**
         * Invokes this kit's expression for a given table row.
         *
         * @param  lrow   row index
         * @return   stringified result
         */
        public Outcome executeAtRow( long lrow ) {
            try {
                Object result = rdr_.evaluateAtRow( compEx_, lrow );
                if ( result instanceof Outcome ) {
                    return (Outcome) result;
                }
                else {
                    return Outcome.success( result == null
                                          ? null
                                          : result.toString() );
                }
            }
            catch ( Throwable e ) {
                return Outcome.failure( e );
            }
        }
    }
}
