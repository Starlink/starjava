package uk.ac.starlink.topcat.activate;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.astrogrid.samp.DataException;
import org.astrogrid.samp.Message;
import org.astrogrid.samp.SampUtils;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.Outcome;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.Safety;
import uk.ac.starlink.topcat.TopcatJELUtils;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.jel.RandomJELRowReader;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * ActivationType that can send a custom SAMP message.
 *
 * @author   Mark Taylor
 * @since    10 Mar 2021
 */
public class SendCustomActivationType implements ActivationType {

    public String getName() {
        return "SAMP Message";
    }

    public String getDescription() {
        return "Sends a SAMP message with custom MType and parameters"
             + " to external application(s)";
    }

    public ActivatorConfigurator createConfigurator( TopcatModelInfo tinfo ) {
        return new SampConfigurator( tinfo.getTopcatModel() );
    }

    public Suitability getSuitability( TopcatModelInfo tinfo ) {
        return Suitability.PRESENT;
    }

    /**
     * Tries to convert a java object to a SAMP-friendly value.
     *
     * @param  jelValue  result of JEL evaluation
     * @return   object suitable for inclusion in a SAMP Message map
     * @throws  DataException  if the object cannot be converted
     */
    private static Object toSampValue( Object jelValue ) {
        if ( jelValue == null ) {
            return "";
        }
        else if ( jelValue instanceof String ) {
            return jelValue;
        }
        else if ( jelValue instanceof Number ) {
            return jelValue.toString();
        }
        else if ( jelValue instanceof Boolean ) {
            return ((Boolean) jelValue).booleanValue() ? "1" : "0";
        }
        else if ( jelValue instanceof Map ) {
            return jelValue;
        }
        else if ( jelValue.getClass().isArray() ) {
            List<Object> list = new ArrayList<Object>();
            int nel = Array.getLength( jelValue );
            for ( int i = 0; i < nel; i++ ) {
                list.add( toSampValue( Array.get( jelValue, i ) ) );
            }
            return list;
        }
        else {
            throw new DataException( "Not SAMP-friendly value "
                                   + jelValue.getClass().getSimpleName() );
        }
    }

    /**
     * Configurator implementation for use with this activation type.
     */
    private static class SampConfigurator
            extends AbstractActivatorConfigurator {

        private final TopcatModel tcModel_;
        private final JTextField mtypeField_;
        private final JComboBox<Object> clientSelector_;
        private final ArgsPanel argsPanel_;
        private SampSender sender_;

        private static final String MTYPE_KEY = "mtype";
        private static final String NPARAM_KEY = "nparam";
        private static final String PNAME_KEY = "pname";
        private static final String PEXPR_KEY = "pexpr";

        /**
         * Constructor.
         *
         * @param  tcModel   topcat model
         */
        SampConfigurator( TopcatModel tcModel ) {
            super( new JPanel( new BorderLayout() ) );
            tcModel_ = tcModel;
            JComponent panel = getPanel();
            ActionForwarder forwarder = getActionForwarder();

            /* Prepare input and display components. */
            mtypeField_ = new JTextField();
            clientSelector_ = new JComboBox<Object>();
            clientSelector_.addActionListener( forwarder );
            mtypeField_.addActionListener( evt -> {
                setMType( mtypeField_.getText() );
                forwarder.actionPerformed( evt );
            } );
            argsPanel_ = new ArgsPanel( forwarder, 3 );

            /* Arrange components. */
            JComponent topBox = Box.createVerticalBox();
            Box mtypeLine = Box.createHorizontalBox();
            mtypeLine.add( new JLabel( "MType: " ) );
            mtypeLine.add( mtypeField_ );
            topBox.add( mtypeLine );
            topBox.add( Box.createVerticalStrut( 5 ) );
            Box targetLine = Box.createHorizontalBox();
            targetLine.add( new JLabel( "Target Client: " ) );
            targetLine.add( clientSelector_ );
            targetLine.add( Box.createHorizontalGlue() );
            topBox.add( targetLine );
            topBox.add( Box.createVerticalStrut( 10 ) );
            Box ptitleLine = Box.createHorizontalBox();
            ptitleLine.add( new JLabel( "Message parameters:" ) );
            ptitleLine.add( Box.createHorizontalGlue() );
            topBox.add( ptitleLine );
            panel.add( topBox, BorderLayout.NORTH );
            JPanel apanel = new JPanel( new BorderLayout() );
            panel.add( apanel, BorderLayout.CENTER );
            apanel.add( argsPanel_, BorderLayout.NORTH );
  
            setMType( "" );
        }

        public Activator getActivator() {
            String mtype = sender_.getMType();
            if ( mtype == null ||
                 mtype.trim().length() == 0 ||
                 ! sender_.hasClients() ) {
                return null;
            }
            RandomJELRowReader jelRdr = tcModel_.createJELRowReader();
            Map<String,CompiledExpression> exprMap;
            try {
                exprMap = getParamExpressions( jelRdr );
            }
            catch ( CompilationException e ) {
                return null;
            }
            return new SampActivator( sender_, jelRdr, exprMap );
        }

        public String getConfigMessage() {
            String mtype = sender_.getMType();
            if ( mtype == null || mtype.trim().length() == 0 ) {
                return "No MType specified";
            }
            String unText = sender_.getUnavailableText();
            if ( unText != null ) {
                return unText;
            }
            try {
                getParamExpressions( tcModel_.createJELRowReader() );
            }
            catch ( CompilationException e ) {
                return "Expression error: " + e.getMessage();
            }
            return null;
        }

        public Safety getSafety() {
            return Safety.UNSAFE;
        }

        public ConfigState getState() {
            ConfigState state = new ConfigState();
            state.saveText( MTYPE_KEY, mtypeField_ );
            List<ArgsPanel.Entry> entries = argsPanel_.entries_;
            int nParam = entries.size();
            state.setInt( NPARAM_KEY, nParam );
            for ( int ip = 0; ip < nParam; ip++ ) {
                ArgsPanel.Entry entry = entries.get( ip );
                state.saveText( PNAME_KEY + ip, entry.nameField_ );
                state.saveText( PEXPR_KEY + ip, entry.exprField_ );
            }
            return state;
        }

        public void setState( ConfigState state ) {
            state.restoreText( MTYPE_KEY, mtypeField_ );
            int nParam = state.getInt( NPARAM_KEY );
            argsPanel_.reset( nParam );
            List<ArgsPanel.Entry> entries = argsPanel_.entries_;
            for ( int ip = 0; ip < nParam; ip++ ) {
                ArgsPanel.Entry entry = entries.get( ip );
                state.restoreText( PNAME_KEY + ip, entry.nameField_ );
                state.restoreText( PEXPR_KEY + ip, entry.exprField_ );
            }
            setMType( mtypeField_.getText() );
        }

        /**
         * Sets the chosen MType for the message to send.
         *
         * @param mtype  MType
         */
        private void setMType( String mtype ) {
            ActionForwarder forwarder = getActionForwarder();
            if ( sender_ != null ) {
                sender_.getConnector().removeConnectionListener( forwarder );
            }
            sender_ = new SampSender( mtype );
            sender_.getConnector().addConnectionListener( forwarder );
            clientSelector_.getModel().removeListDataListener( forwarder );
            clientSelector_.setModel( sender_.getClientSelectionModel() );
            clientSelector_.getModel().addListDataListener( forwarder );
        }

        /**
         * Compiles the parameter value expressions currently entered in
         * the GUI and returns a parameterName-&gt;compiledExpression map.
         *
         * @param  jelRdr  row reader for the table
         * @return  parameterName -&gt; compiledExpression map
         * @throws  CompilationException   if any of the expressions are bad
         */
        private Map<String,CompiledExpression>
                getParamExpressions( RandomJELRowReader jelRdr )
                throws CompilationException {
            Library lib = TopcatJELUtils.getLibrary( jelRdr, false );
            final Map<String,CompiledExpression> exprMap =
                new LinkedHashMap<>();
            for ( Map.Entry<String,String> ent :
                  argsPanel_.getArgMap().entrySet() ) {
                String name = ent.getKey();
                String expr = ent.getValue();
                exprMap.put( name, Evaluator.compile( expr, lib, null ) );
            }
            return exprMap;
        }
    }

    /**
     * Activator implementation for use with this class.
     */
    private static class SampActivator implements Activator {
        final SampSender sender_;
        final RandomJELRowReader jelRdr_;
        final Map<String,CompiledExpression> exprMap_;

        /**
         * Constructor.
         *
         * @param   sender   sender
         * @param   jelRdr   row reader used to compile expressions
         * @param   exprMap  map from message parameter name
         *                   to compiled expression
         */
        SampActivator( SampSender sender, RandomJELRowReader jelRdr,
                       Map<String,CompiledExpression> exprMap ) {
            sender_ = sender;
            jelRdr_ = jelRdr;
            exprMap_ = exprMap;
        }

        public boolean invokeOnEdt() {
            return false;
        }

        public Outcome activateRow( long lrow, ActivationMeta meta ) {
            if ( meta != null && meta.isInhibitSend() ) {
                return Outcome.failure( "(no send to avoid ping pong)" );
            }
            final Message msg;
            try {
                Map<String,Object> paramMap = new LinkedHashMap<>();
                for ( Map.Entry<String,CompiledExpression> entry :
                      exprMap_.entrySet() ) {
                    String pname = entry.getKey();
                    CompiledExpression compEx = entry.getValue();
                    final Object jelValue;
                    try {
                        jelValue = jelRdr_.evaluateAtRow( compEx, lrow );
                    }
                    catch ( Throwable e ) {
                        return Outcome.failure( "Error evaluating " + pname
                                              + ": " + e );
                    }
                    paramMap.put( pname, toSampValue( jelValue ) );
                }
                msg = new Message( sender_.getMType(), paramMap );
                SampUtils.checkObject( msg );
            }
            catch ( DataException e ) {
                return Outcome.failure( e );
            }
            return sender_.activateMessage( msg );
        }
    }

    /**
     * Component that displays entry fields for parameter names and expressions.
     */
    private static class ArgsPanel extends JPanel {

        private final ChangeListener listener_;
        private final List<Entry> entries_;
        private final JComponent entriesBox_;
        private final Action addAct_;
        private final Action removeAct_;

        /**
         * Constructor.
         * An initial number of argument fields is supplied,
         * but the GUI provides options to change this number.
         *
         * @param  listener  listener to be notified whenever the content
         *                   might have changed
         * @param  intialCount  initial number of parameters
         */
        ArgsPanel( ChangeListener listener, int initialCount ) {
            super( new BorderLayout() );
            listener_ = listener;
            entries_ = new ArrayList<Entry>();

            /* Actions to add and remove fields. */
            addAct_ = new BasicAction( "Add", ResourceIcon.ADD,
                                       "Add another parameter field" ) {
                public void actionPerformed( ActionEvent evt ) {
                    addEntry();
                    countChanged();
                }
            };
            removeAct_ = new BasicAction( "Remove", ResourceIcon.SUBTRACT,
                                          "Remove the last parameter field" ) {
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
         * @return  name-expression pairs, one for each parameter
         */
        public Map<String,String> getArgMap() {
            Map<String,String> map = new LinkedHashMap<>();
            for ( Entry entry : entries_ ) {
                String name = entry.nameField_.getText();
                String value = entry.exprField_.getText();
                if ( name != null && name.trim().length() > 0 ) {
                    map.put( name, value );
                }
            }
            return map;
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
            JTextField nameField = new JTextField( 12 );
            JTextField exprField = new JTextField();
            nameField.getCaret().addChangeListener( listener_ );
            exprField.getCaret().addChangeListener( listener_ );
            JComponent line = Box.createHorizontalBox();
            line.add( new ShrinkWrapper( nameField ) );
            line.add( new JLabel( " = " ) );
            line.add( exprField );
            line.setBorder( BorderFactory.createEmptyBorder( 0, 0, 2, 0 ) );
            entries_.add( new Entry( nameField, exprField, line ) );
            entriesBox_.add( line );
        }

        /**
         * Removes the last argument field from the end of the list.
         */
        private void removeEntry() {
            if ( entries_.size() > 0 ) {
                Entry entry = entries_.remove( entries_.size() - 1 );
                entry.nameField_.getCaret().removeChangeListener( listener_ );
                entry.exprField_.getCaret().removeChangeListener( listener_ );
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
         * Aggregates parameter name and expression text fields
         * with the container holding them.
         */
        private static class Entry {
            final JTextField nameField_;
            final JTextField exprField_;
            final JComponent container_;
            Entry( JTextField nameField, JTextField exprField,
                   JComponent container ) {
                nameField_ = nameField;
                exprField_ = exprField;
                container_ = container;
            }
        }
    }
}
