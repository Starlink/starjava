package uk.ac.starlink.topcat.plot2;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.ttools.plot2.task.PlotCommandFormatter;
import uk.ac.starlink.ttools.plot2.task.Suffixer;
import uk.ac.starlink.ttools.task.CommandFormatter;
import uk.ac.starlink.ttools.task.CredibleString;
import uk.ac.starlink.ttools.task.LineEnder;
import uk.ac.starlink.ttools.task.TableNamer;
import uk.ac.starlink.util.gui.ComboBoxBumper;
import uk.ac.starlink.util.gui.TallWrapper;

/**
 * Fixed Control implementation that can display a STILTS command to
 * reproduce the currently visible plot.
 *
 * @author   Mark Taylor
 * @since    19 Sep 2017
 */
public class StiltsControl extends TabberControl {

    private final PlotPanel<?,?> plotPanel_;
    private final boolean isMultiZone_;
    private final ToggleButtonModel windowToggle_;
    private boolean configured_;

    /** List of suffixers suitable for per-zone parameters. */
    public static final Suffixer[] ZONE_SUFFIXERS = new Suffixer[] {
        Suffixer.createAlphaSuffixer( "-Alpha", "-", true, true ),
        Suffixer.createAlphaSuffixer( "-alpha", "-", true, false ),
        Suffixer.createNumericSuffixer( "-Numeric", "-", true ),
        Suffixer.createNumericSuffixer( "Numeric", "", true ),
        Suffixer.createAlphaSuffixer( "Alpha", "", true, true ),
    };

    /** List of suffixers suitable for per-layer parameters. */
    public static final Suffixer[] LAYER_SUFFIXERS = new Suffixer[] {
        Suffixer.createNumericSuffixer( "_Numeric", "_", true ),
        Suffixer.createAlphaSuffixer( "_Alpha", "_", true, true ),
        Suffixer.createAlphaSuffixer( "_alpha", "_", true, false ),
        Suffixer.createNumericSuffixer( "Numeric", "", true ),
        Suffixer.createAlphaSuffixer( "Alpha", "", true, true ),
    };

    /**
     * Constructor.
     *
     * @param  plotPanel   plot panel
     * @param  isMultiZone   true if the possibility of multiple zones
     *                       should be accounted for in the command
     * @param  windowToggle  model for posting a separate window
     *                       displaying the command text
     */
    public StiltsControl( PlotPanel<?,?> plotPanel, boolean isMultiZone,
                          ToggleButtonModel windowToggle ) {
        super( "STILTS", ResourceIcon.STILTS );
        plotPanel_ = plotPanel;
        isMultiZone_ = isMultiZone;
        windowToggle_ = windowToggle;
    }

    @Override
    public JComponent getPanel() {
        if ( ! configured_ ) {
            configure();
            configured_ = true;
        }
        return super.getPanel();
    }

    /**
     * Called to configure the display before it is first shown.
     * Once active, this *may* consume non-negligable resources,
     * and in many cases it will never become active, so we do this
     * lazily.
     */
    private void configure() {
        final StiltsMonitor monitor = new StiltsMonitor( plotPanel_ );

        /* Set up panel for command format control. */
        final FormatPanel formatPanel = new FormatPanel( isMultiZone_ );

        /* Set up panel for command display. */
        JComponent cmdPanel = new JPanel( new BorderLayout() );
        cmdPanel.add( StiltsMonitor.wrapTextPanel( monitor.getTextPanel() ),
                      BorderLayout.CENTER );
        JComponent actionLine = Box.createHorizontalBox();
        actionLine.add( Box.createHorizontalGlue() );
        actionLine.add( new TallWrapper(
                            new JButton( monitor.getClipboardAction() ) ) );
        actionLine.add( Box.createHorizontalStrut( 10 ) );
        actionLine.add( new TallWrapper(
                            new JButton( monitor.getExecuteAction() ) ) );
        actionLine.add( Box.createHorizontalStrut( 10 ) );
        JToggleButton windowButton = windowToggle_.createButton();
        windowButton.setText( "Window" );
        windowButton.setIcon( null );
        actionLine.add( new TallWrapper( windowButton ) );
        actionLine.add( Box.createHorizontalStrut( 10 ) );
        actionLine.add( new TallWrapper(
                            new JButton( monitor.getErrorAction() ) ) );
        actionLine.add( Box.createHorizontalGlue() );
        actionLine.setBorder( BorderFactory.createEmptyBorder( 5, 0, 0, 0 ) );
        cmdPanel.add( actionLine, BorderLayout.SOUTH );

        /* Ensure that the StiltsMonitor is kept updated with the
         * configured formatter. */
        formatPanel.forwarder_.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                formatPanel.updateFormatter( monitor );
            }
        } );
        cmdPanel.addComponentListener( new ComponentAdapter() {
            @Override
            public void componentResized( ComponentEvent evt ) {
                formatPanel.updateFormatter( monitor );
            }
        } );
        formatPanel.updateFormatter( monitor );

        /* Configure tabs. */
        addControlTab( "Command", cmdPanel, false );
        addControlTab( "Formatting", formatPanel, true );
    }

    /**
     * Panel that displays command format configuration options.
     */
    private static class FormatPanel extends LabelledComponentStack {
        private final JComboBox<StiltsInvoker> invokerSelector_;
        private final JComboBox<Suffixer> zoneSuffixSelector_;
        private final JComboBox<Suffixer> layerSuffixSelector_;
        private final JComboBox<TableNamer> tableNamerSelector_;
        private final JCheckBox dfltsButton_;
        private final JComboBox<LineEnder> lineEnderSelector_;
        private final SpinnerNumberModel indentSpinner_;
        private final ActionForwarder forwarder_;

        /**
         * Constructor.
         *
         * @param  isMultiZone  wehther multiple zones may appear
         */
        FormatPanel( boolean isMultiZone ) {
            forwarder_ = new ActionForwarder();

            /* Set up controls. */
            invokerSelector_ =
                new JComboBox<StiltsInvoker>( StiltsInvoker.INVOKERS );
            invokerSelector_
               .setSelectedItem( StiltsInvoker.TOPCAT );
            zoneSuffixSelector_ = new JComboBox<Suffixer>( ZONE_SUFFIXERS );
            zoneSuffixSelector_
               .setSelectedItem( zoneSuffixSelector_.getItemAt( 0 ) );
            layerSuffixSelector_ = new JComboBox<Suffixer>( LAYER_SUFFIXERS );
            layerSuffixSelector_
               .setSelectedItem( layerSuffixSelector_.getItemAt( 0 ) );
            tableNamerSelector_ =
                new JComboBox<TableNamer>( TopcatLayer.getLayerTableNamers() );
            tableNamerSelector_
               .setSelectedItem( tableNamerSelector_.getItemAt( 0 ) );
            dfltsButton_ = new JCheckBox();
            lineEnderSelector_ = new JComboBox<LineEnder>( LineEnder.OPTIONS );
            lineEnderSelector_.setSelectedItem( LineEnder.BACKSLASH );
            indentSpinner_ =
                new SpinnerNumberModel( 3, 0, 8, 1 );

            /* Arrange for GUI changes to message listeners. */
            invokerSelector_.addActionListener( forwarder_ );
            zoneSuffixSelector_.addActionListener( forwarder_ );
            layerSuffixSelector_.addActionListener( forwarder_ );
            tableNamerSelector_.addActionListener( forwarder_ );
            dfltsButton_.addActionListener( forwarder_ );
            lineEnderSelector_.addActionListener( forwarder_ );
            indentSpinner_.addChangeListener( forwarder_ );

            /* Add them to the component. */
            addLine( "Invocation", withBumper( invokerSelector_ ) );
            addLine( "Layer Suffixes", withBumper( layerSuffixSelector_ ) );
            if ( isMultiZone ) {
                addLine( "Zone Suffixes", withBumper( zoneSuffixSelector_ ) );
            }
            addLine( "Table Names", withBumper( tableNamerSelector_ ) );
            addLine( "Include Defaults", dfltsButton_ );
            addLine( "Line Endings", withBumper( lineEnderSelector_ ) );
            addLine( "Indent", new JSpinner( indentSpinner_ ) );
        }

        /**
         * Interrogates the state of this control's GUI and updates the
         * formatting configuration of a given StiltsMonitor.
         *
         * @param  monitor to update
         */
        void updateFormatter( StiltsMonitor monitor ) {
            CredibleString invocation =
                invokerSelector_
               .getItemAt( invokerSelector_.getSelectedIndex() )
               .getInvocation();
            Suffixer zoneSuffixer =
                zoneSuffixSelector_
               .getItemAt( zoneSuffixSelector_.getSelectedIndex() );
            Suffixer layerSuffixer =
                layerSuffixSelector_
               .getItemAt( layerSuffixSelector_.getSelectedIndex() );
            TableNamer namer =
                tableNamerSelector_
               .getItemAt( tableNamerSelector_.getSelectedIndex() );
            boolean includeDflts =
                dfltsButton_.isSelected();
            LineEnder lineEnder =
                lineEnderSelector_
               .getItemAt( lineEnderSelector_.getSelectedIndex() );
            int indent =
                indentSpinner_.getNumber().intValue();
            int cwidth = monitor.getWidthCharacters();
            CommandFormatter formatter =
                new PlotCommandFormatter( invocation, includeDflts, lineEnder,
                                          indent, cwidth );
            monitor.configure( formatter, namer, layerSuffixer, zoneSuffixer );
        }

        /**
         * Adds a bumper to a JComboBox.
         *
         * @param  combo  unadorned JComboBox
         * @return  component containing combobox + bumper
         */
        private static JComponent withBumper( JComboBox<?> combo ) {
            JComponent line = Box.createHorizontalBox();
            line.add( combo );
            line.add( Box.createHorizontalStrut( 5 ) );
            line.add( new ComboBoxBumper( combo ) );
            return line;
        }
    }
}
