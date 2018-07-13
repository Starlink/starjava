package uk.ac.starlink.topcat.activate;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import java.io.IOException;
import java.io.InterruptedIOException;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.Executor;
import uk.ac.starlink.topcat.LineBox;
import uk.ac.starlink.topcat.Outcome;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.Safety;
import uk.ac.starlink.topcat.TopcatJELUtils;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.jel.RandomJELRowReader;

/**
 * Activation type for executing an OS command.
 * This can also be done using the {@link JelActivationType}
 * using the {@link uk.ac.starlink.topcat.func.System} <code>exec</code>
 * functions, but this makes it more transparent from a UI point of view.
 *
 * @author   Mark Taylor
 * @since    26 Jan 2018
 */
public class ShellActivationType implements ActivationType {

    public String getName() {
        return "Run system command";
    }

    public String getDescription() {
        return "Executes a command in an Operating System shell";
    }

    public ActivatorConfigurator createConfigurator( TopcatModelInfo tinfo ) {
        return new ShellConfigurator( tinfo.getTopcatModel() );
    }

    public Suitability getSuitability( TopcatModelInfo tinfo ) {
        return Suitability.PRESENT;
    }

    /**
     * Configurator implementation for OS command execution.
     */
    private static class ShellConfigurator
            extends AbstractActivatorConfigurator {

        private final TopcatModel tcModel_;
        private final JTextField cmdField_;
        private final ArgsPanel argsPanel_;
        private final JCheckBox syncSelector_;
        private final JCheckBox captureSelector_;

        private static final String SYNC_KEY = "sync";
        private static final String CAPTURE_KEY = "capture";
        private static final String NWORD_KEY = "nword";
        private static final String IWORD_KEY = "word";

        /**
         * Constructor.
         *
         * @param  tcModel  topcat model
         */
        ShellConfigurator( TopcatModel tcModel ) {
            super( new JPanel( new BorderLayout() ) );
            JComponent panel = getPanel();
            tcModel_ = tcModel;

            /* Prepare input and display components. */
            cmdField_ = new JTextField();
            ActionForwarder forwarder = getActionForwarder();
            cmdField_.getCaret().addChangeListener( forwarder );
            argsPanel_ = new ArgsPanel( forwarder, 4 );
            syncSelector_ = new JCheckBox( "Synchronous", false );
            syncSelector_.addActionListener( forwarder );
            captureSelector_ = new JCheckBox( "Capture Output", true );
            captureSelector_.addActionListener( forwarder );

            /* Arrange components. */
            JComponent flagsLine = Box.createHorizontalBox();
            flagsLine.add( syncSelector_ );
            flagsLine.add( Box.createHorizontalStrut( 10 ) );
            flagsLine.add( captureSelector_ );
            flagsLine.add( Box.createHorizontalGlue() );
            JComponent centerPanel = new JPanel( new BorderLayout() );
            Box cmdLine = Box.createHorizontalBox();
            cmdLine.add( new JLabel( "Command: " ) );
            cmdLine.add( cmdField_ );
            JComponent apanel = new JPanel( new BorderLayout() );
            apanel.add( cmdLine, BorderLayout.NORTH );
            JComponent bpanel = new JPanel( new BorderLayout() );
            bpanel.add( argsPanel_, BorderLayout.NORTH );
            apanel.add( bpanel, BorderLayout.CENTER );
            centerPanel.add( apanel, BorderLayout.NORTH );

            /* Place components in control panel. */
            panel.add( centerPanel, BorderLayout.CENTER );
            panel.add( flagsLine, BorderLayout.SOUTH );
        }

        public Activator getActivator() {
            boolean isCapture = captureSelector_.isSelected();
            final EvaluateKit kit;
            try {
                kit = createKit( isCapture );
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
                return createKit( false ) == null
                     ? "No expression"
                     : null;
            }
            catch ( CompilationException e ) {
                return "Expression error: " + e.getMessage();
            }
        }

        public Safety getSafety() {
            String arg0 = getCommand();
            String[] exprs = getArgExpressions();
            return ( arg0 == null || arg0.trim().length() == 0 )
                && exprs.length == 0 
                       ? Safety.SAFE
                       : Safety.UNSAFE;
        }

        public ConfigState getState() {
            ConfigState state = new ConfigState();
            state.saveText( IWORD_KEY + 0, cmdField_ );
            List<ArgsPanel.Entry> entries = argsPanel_.entries_;
            int nWord = 1 + entries.size();
            state.setInt( NWORD_KEY, nWord );
            for ( int iw = 1; iw < nWord; iw++ ) {
                state.saveText( IWORD_KEY + iw,
                                entries.get( iw - 1 ).field_ );
            }
            state.saveFlag( SYNC_KEY, syncSelector_.getModel() );
            state.saveFlag( CAPTURE_KEY, captureSelector_.getModel() );
            return state;
        }

        public void setState( ConfigState state ) {
            int nWord = state.getInt( NWORD_KEY );
            argsPanel_.reset( nWord - 1 );
            state.restoreText( IWORD_KEY + 0, cmdField_ );
            List<ArgsPanel.Entry> entries = argsPanel_.entries_;
            for ( int iw = 1; iw < nWord; iw++ ) {
                state.restoreText( IWORD_KEY + iw,
                                   entries.get( iw - 1 ).field_ );
            }
            state.restoreFlag( SYNC_KEY, syncSelector_.getModel() );
            state.restoreFlag( CAPTURE_KEY, captureSelector_.getModel() );
        }

        /**
         * Returns the name of the system command to execute.
         * This is the first element of the argv vector to pass to
         * System.exec().
         *
         * @return  arg0
         */
        private String getCommand() {
            return cmdField_.getText();
        }

        /**
         * Returns the unevaluated expressions entered by the user
         * that provide the command arguments.
         * These are evaluated per-row using JEL at activation time.
         *
         * <p>Trailing blank arguments are omitted.
         *
         * @return   array of argument expressions, omitting any sequence
         *           of blank expressions at the end of the list
         */
        private String[] getArgExpressions() {
            String[] args = argsPanel_.getArgs();
            int narg = 0;
            for ( int i = 0; i < args.length; i++ ) {
                String txt = args[ i ];
                if ( txt == null ) {
                    txt = "";
                }
                txt = txt.trim();
                args[ i ] = txt;
                if ( txt.length() > 0 ) {
                    narg = i + 1;
                }
            }
            String[] argv = new String[ narg ];
            System.arraycopy( args, 0, argv, 0, narg );
            return argv;
        }

        /**
         * Interrogates the UI to get enough information to attempt to
         * execute a system command at some time in the future.
         *
         * @param  isCapture  true to capture output and return it as
         *                    the outcome message, false to let it go to stdout
         * @return  command evaluation kit, or null
         */
        EvaluateKit createKit( boolean isCapture ) throws CompilationException {
            String arg0 = getCommand();
            if ( arg0 == null || arg0.trim().length() == 0 ) {
                return null;
            }
            String[] exprs = getArgExpressions();
            int nexpr = exprs.length;
            RandomJELRowReader rowReader = tcModel_.createJELRowReader();
            Library lib = TopcatJELUtils.getLibrary( rowReader, true );
            CompiledExpression[] compExs = new CompiledExpression[ nexpr ];
            for ( int i = 0; i < nexpr; i++ ) {
                compExs[ i ] = Evaluator.compile( exprs[ i ], lib, null );
            }
            return new EvaluateKit( arg0, rowReader, compExs, exprs,
                                    isCapture );
        }
    }

    /**
     * Submits an argument vector representing an OS command for execution,
     * intercepting the stdout and stderr streams and presenting them
     * as part of the outcome.
     *
     * @param  argv  command path followed by arguments,
     *               in the style of Runtime.exec(String[])
     * @param  isCapture  true to capture output and return it as
     *                    the outcome message, false to let it go to stdout
     * @return   outcome
     */
    private static Outcome executeWords( String[] argv, boolean isCapture ) {
        Executor executor = Executor.createExecutor( argv );
        final int status;
        try {
            status = executor.executeSynchronously( isCapture );
        }
        catch ( IOException e ) {
            return Outcome.failure( e );
        }
        catch ( InterruptedException e ) {
            return Outcome.failure( "Job interrupted" );
        }
        String line = executor.getLine();
        boolean success = status == 0;
        final String msg;
        if ( isCapture ) {
            String out = executor.getOut();
            String err = executor.getErr();
            if ( success ) {
                if ( out != null && out.trim().length() > 0 ) {
                    msg = out;
                }
                else if ( err != null && err.trim().length() > 0 ) {
                    msg = err;
                }
                else {
                    msg = line;
                }
            }
            else {
                if ( err != null && err.trim().length() > 0 ) {
                    msg = err;
                }
                else if ( out != null && out.trim().length() > 0 ) {
                    msg = out;
                }
                else {
                    msg = line;
                }
            }
        }
        else {
            msg = line;
        }
        return success ? Outcome.success( msg ) : Outcome.failure( msg );
    }

    /**
     * Component that displays entry fields for command arguments.
     */
    private static class ArgsPanel extends JPanel {
        private final ChangeListener listener_;
        private final List<Entry> entries_;
        private final JComponent entriesBox_;
        final Action addAct_;
        final Action removeAct_;

        /**
         * Constructor.
         * An initial number of argument fields is supplied,
         * but the GUI provides options to change this number.
         *
         * @param  listener  listener to be notified whenever the content
         *                   might have changed
         * @param  intialCount  initial number of argument fields
         */
        ArgsPanel( ChangeListener listener, int initialCount ) {
            super( new BorderLayout() );
            listener_ = listener;
            entries_ = new ArrayList<Entry>();

            /* Actions to add or remove fields. */
            addAct_ = new BasicAction( "Add", ResourceIcon.ADD,
                                       "Add another argument field" ) {
                public void actionPerformed( ActionEvent evt ) {
                    addEntry();
                    countChanged();
                }
            };
            removeAct_ = new BasicAction( "Remove", ResourceIcon.SUBTRACT,
                                          "Remove the last argument field" ) {
                public void actionPerformed( ActionEvent evt ) {
                    removeEntry();
                    countChanged();
                }
            };

            /* Arrange components. */
            entriesBox_ = Box.createVerticalBox();
            add( entriesBox_, BorderLayout.NORTH );
            JComponent controlLine = Box.createHorizontalBox();
            JButton addButt = new JButton( addAct_ );
            JButton subButt = new JButton( removeAct_ );
            addButt.setHideActionText( true );
            subButt.setHideActionText( true );
            controlLine.add( Box.createHorizontalGlue() );
            controlLine.add( addButt );
            controlLine.add( Box.createHorizontalStrut( 5 ) );
            controlLine.add( subButt );
            JComponent controlBox = new JPanel( new BorderLayout() );
            controlBox.add( controlLine, BorderLayout.NORTH );
            add( controlBox, BorderLayout.CENTER );

            /* Add initial quota of fields. */
            for ( int i = 0; i < initialCount; i++ ) {
                addEntry();
            }
        }

        /**
         * Returns the currently entered arguments.
         *
         * @return  argument values, one for each field
         */
        public String[] getArgs() {
            int nent = entries_.size();
            String[] args = new String[ nent ];
            for ( int i = 0; i < nent; i++ ) {
                args[ i ] = entries_.get( i ).field_.getText();
            }
            return args;
        }

        /**
         * Clears the content of this panel and sets the number of
         * entries as specified.
         *
         * @param  nEntry  required number of entries
         */
        public void reset( int nEntry ) {
            while ( entries_.size() > 0 ) {
                removeEntry();
            }
            for ( int i = 0; i < nEntry; i++ ) {
                addEntry();
            }
            countChanged();
        }

        /**
         * Adds an argument field to the end of the list.
         */
        private void addEntry() {
            JComponent line = Box.createHorizontalBox();
            JTextField field = new JTextField();
            field.getCaret().addChangeListener( listener_ );
            line.add( new JLabel( "Arg #" + ( entries_.size() + 1 ) + ": " ) );
            line.add( field );
            entries_.add( new Entry( field, line ) );
            entriesBox_.add( line );
        }

        /**
         * Removes the last argument field from the end of the list.
         */
        private void removeEntry() {
            if ( entries_.size() > 0 ) {
                Entry entry = entries_.remove( entries_.size() - 1 );
                entry.field_.getCaret().removeChangeListener( listener_ );
                entriesBox_.remove( entry.container_ );
            }
        }

        /**
         * Invoked to update state if the number of fields might have changed.
         */
        private void countChanged() {
            listener_.stateChanged( new ChangeEvent( this ) );
            removeAct_.setEnabled( entries_.size() > 0 );
            revalidate();
            repaint();
        }

        /**
         * Aggregates an argument text field with the container holding it.
         */
        private static class Entry {
            final JTextField field_;
            final JComponent container_;

            /**
             * Constructor.
             *
             * @param  field  text field
             * @param  container   container component
             */
            Entry( JTextField field, JComponent container ) {
                field_ = field;
                container_ = container;
            }
        }
    }

    /**
     * Contains enough information to attempt to invoke an OS command,
     * given a row index.
     */
    private static class EvaluateKit {
        final String arg0_;
        final RandomJELRowReader rdr_;
        final CompiledExpression[] compExs_;
        final String[] exprs_;
        final boolean isCapture_;

        /**
         * Constructor.
         *
         * @param  arg0  path of executable command
         * @param  rdr   row reader providing JEL context
         * @param  compExs  array of compiled expressions,
         *                  one for each command-line argument
         * @param  exprs   array of uncompiled expressions matching
         *                 <code>compExs</code>
         * @param  isCapture  true to capture output and return it as
         *                    the outcome message, false to let it go to stdout
         */
        EvaluateKit( String arg0, RandomJELRowReader rdr,
                     CompiledExpression[] compExs, String[] exprs,
                     boolean isCapture ) {
            arg0_ = arg0;
            rdr_ = rdr;
            compExs_ = compExs;
            exprs_ = exprs;
            isCapture_ = isCapture;
        }

        /**
         * Executes this kit's command at a given row index.
         *
         * @param  lrow  row index
         * @return   outcome
         */
        public Outcome executeAtRow( long lrow ) {
            List<String> argv = new ArrayList<String>();
            argv.add( arg0_ );
            try {
                argv.addAll( Arrays.asList( evaluateWords( lrow ) ) );
            }
            catch ( IOException e ) {
                return Outcome.failure( e );
            }
            String[] args = argv.toArray( new String[ 0 ] );
            return executeWords( argv.toArray( new String[ 0 ] ), isCapture_ );
        }

        /**
         * Evaluates the compiled expresssions held by this kit
         * at a given row index and returns the results.
         *
         * @param  lrow  row index
         * @return   evaluated expressions, one per argument
         * @throws   IOException  with an informative message if any
         *           of the expressions causes an error
         */
        String[] evaluateWords( long lrow ) throws IOException {
            int nexpr = compExs_.length;
            String[] words = new String[ nexpr ];
            for ( int i = 0; i < nexpr; i++ ) {
                try {
                    Object result = rdr_.evaluateAtRow( compExs_[ i ], lrow );
                    words[ i ] = result == null ? null : result.toString();
                }
                catch ( Throwable e ) {
                    throw (IOException)
                          new IOException( "Bad expression \""
                                         + exprs_[ i ] + "\": "
                                         + e.getMessage() )
                         .initCause( e );
                }
            }
            return words;
        }
    }
}
