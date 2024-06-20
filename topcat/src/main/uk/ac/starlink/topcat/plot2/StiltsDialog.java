package uk.ac.starlink.topcat.plot2;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.Window;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.AuxDialog;
import uk.ac.starlink.topcat.MenuSelector;
import uk.ac.starlink.ttools.plot2.task.StiltsPlotFormatter;
import uk.ac.starlink.ttools.plot2.task.Suffixer;
import uk.ac.starlink.ttools.task.CredibleString;
import uk.ac.starlink.ttools.task.TableNamer;
import uk.ac.starlink.ttools.task.LineEnder;
import uk.ac.starlink.util.gui.TallWrapper;

/**
 * Dialog window that displays a STILTS command to reproduce
 * the currently visible plot.
 *
 * @author   Mark Taylor
 * @since    19 Sep 2017
 */
public class StiltsDialog extends AuxDialog {

    private final MenuSelector<StiltsInvoker> invokerSelector_;
    private final MenuSelector<Suffixer> zoneSuffixSelector_;
    private final MenuSelector<Suffixer> layerSuffixSelector_;
    private final MenuSelector<TableNamer> tableNamerSelector_;
    private final JCheckBoxMenuItem dfltsToggle_;
    private final MenuSelector<LineEnder> lineEnderSelector_;
    private final MenuSelector<Integer> indentSelector_;

    /**
     * Constructor.
     *
     * @param   parent  parent window
     * @param  plotPanel   plot panel
     * @param  isMultiZone   true if the possibility of multiple zones
     *                       should be accounted for in the command
     */
    public StiltsDialog( Window parent, PlotPanel<?,?> plotPanel,
                         boolean isMultiZone ) {
        super( "STILTS Export", parent );
        JComponent content = new JPanel( new BorderLayout() );
        getContentPane().add( content, BorderLayout.CENTER );
        final StiltsMonitor monitor = new StiltsMonitor( plotPanel );

        /* Add stilts command text panel. */
        content.add( StiltsMonitor.wrapTextPanel( monitor.getTextPanel() ),
                     BorderLayout.CENTER );

        /* Add a panel for action controls. */
        JComponent actionLine = Box.createHorizontalBox();
        actionLine.add( Box.createHorizontalGlue() );
        Action[] acts = {
            monitor.getClipboardAction(),
            monitor.getExecuteAction(),
        };
        for ( Action act : acts ) {
            actionLine.add( Box.createHorizontalStrut( 5 ) );
            actionLine.add( new TallWrapper( new JButton( act ) ) );
            actionLine.add( Box.createHorizontalStrut( 5 ) );
        }
        actionLine.add( Box.createHorizontalGlue() );
        actionLine.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
        content.add( actionLine, BorderLayout.SOUTH );

        /* Set up menu items to configure the command formatting. */
        invokerSelector_ =
            new MenuSelector<StiltsInvoker>( "Invocation",
                                             StiltsInvoker.INVOKERS,
                                             StiltsInvoker.TOPCAT );
        zoneSuffixSelector_ =
            new MenuSelector<Suffixer>( "Zone Suffixes",
                                        StiltsPlotFormatter.ZONE_SUFFIXERS );
        layerSuffixSelector_ =
            new MenuSelector<Suffixer>( "Layer Suffixes",
                                        StiltsPlotFormatter.LAYER_SUFFIXERS );
        tableNamerSelector_ =
            new MenuSelector<TableNamer>( "Table Names",
                                          TopcatLayer.getLayerTableNamers() );
        dfltsToggle_ = new JCheckBoxMenuItem( "Include Defaults" );
        lineEnderSelector_ =
            new MenuSelector<LineEnder>( "Line Endings", LineEnder.OPTIONS,
                                         LineEnder.BACKSLASH );
        indentSelector_ =
            new MenuSelector<Integer>( "Indent", new Integer[] {
                Integer.valueOf( 0 ), Integer.valueOf( 1 ),
                Integer.valueOf( 2 ), Integer.valueOf( 3 ),
                Integer.valueOf( 4 ), Integer.valueOf( 5 ),
                Integer.valueOf( 6 ), Integer.valueOf( 7 ),
                Integer.valueOf( 8 ),
            }, Integer.valueOf( 3 ) );

        /* Ensure that the StiltsMonitor is kept updated with the
         * configured formatter. */
        ActionForwarder fmtForwarder = new ActionForwarder();
        invokerSelector_.addActionListener( fmtForwarder );
        zoneSuffixSelector_.addActionListener( fmtForwarder );
        layerSuffixSelector_.addActionListener( fmtForwarder );
        tableNamerSelector_.addActionListener( fmtForwarder );
        dfltsToggle_.addChangeListener( fmtForwarder );
        lineEnderSelector_.addActionListener( fmtForwarder );
        indentSelector_.addActionListener( fmtForwarder );
        fmtForwarder.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                updateFormatter( monitor );
            }
        } );
        content.addComponentListener( new ComponentAdapter() {
            @Override
            public void componentResized( ComponentEvent evt ) {
                updateFormatter( monitor );
            }
        } );
        updateFormatter( monitor );

        /* Formatting menu. */
        JMenu formatMenu = new JMenu( "Formatting" );
        formatMenu.add( invokerSelector_.getMenuItem() );
        formatMenu.add( layerSuffixSelector_.getMenuItem() );
        if ( isMultiZone ) {
            formatMenu.add( zoneSuffixSelector_.getMenuItem() );
        }
        formatMenu.add( tableNamerSelector_.getMenuItem() );
        formatMenu.add( dfltsToggle_ );
        formatMenu.add( lineEnderSelector_.getMenuItem() );
        formatMenu.add( indentSelector_.getMenuItem() );

        /* Edit menu. */
        JMenu editMenu = new JMenu( "Edit" );
        editMenu.add( monitor.getClipboardAction() );

        /* Menu bar. */
        getJMenuBar().add( editMenu );
        getJMenuBar().add( formatMenu );

        /* Toolbar. */
        getToolBar().add( monitor.getErrorAction() );
        getToolBar().addSeparator();

        /* Prepare for display. */
        addHelp( "StiltsControl" );
        pack();
    }

    /**
     * Interrogates the state of this control's GUI and updates the
     * formatting configuration of a given StiltsMonitor.
     *
     * @param  monitor to update
     */
    private void updateFormatter( StiltsMonitor monitor ) {
        CredibleString invocation =
            invokerSelector_.getSelectedItem().getInvocation();
        Suffixer zoneSuffixer = zoneSuffixSelector_.getSelectedItem();
        Suffixer layerSuffixer = layerSuffixSelector_.getSelectedItem();
        TableNamer tableNamer = tableNamerSelector_.getSelectedItem();
        boolean includeDflts = dfltsToggle_.isSelected();
        LineEnder lineEnder = lineEnderSelector_.getSelectedItem();
        int indent = indentSelector_.getSelectedItem().intValue();
        int cwidth = monitor.getWidthCharacters();
        StiltsPlotFormatter formatter =
            new StiltsPlotFormatter( invocation, zoneSuffixer, layerSuffixer,
                                     includeDflts, lineEnder, indent, cwidth,
                                     tableNamer );
        monitor.setFormatter( formatter );
    }
}
