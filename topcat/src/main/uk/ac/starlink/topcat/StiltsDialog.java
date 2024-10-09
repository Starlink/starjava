package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPanel;
import uk.ac.starlink.ttools.task.CommandFormatter;
import uk.ac.starlink.ttools.task.CredibleString;
import uk.ac.starlink.ttools.task.LineEnder;
import uk.ac.starlink.ttools.task.StiltsCommand;

/**
 * Dialogue displaying a STILTS command.
 *
 * @author   Mark Taylor
 * @since    19 Sep 2024
 */
public class StiltsDialog extends AuxDialog {

    private final StiltsReporter reporter_;
    private final BasicStiltsMonitor monitor_;
    private final MenuSelector<StiltsInvoker> invokerSelector_;
    private final MenuSelector<TopcatTableNamer> tableNamerSelector_;
    private final MenuSelector<LineEnder> lineEnderSelector_;
    private final MenuSelector<Integer> indentSelector_;
    private final JCheckBoxMenuItem dfltsToggle_;
    private final JCheckBoxMenuItem outToggle_;
    private final Action taskHelpAct_;
    private StiltsCommand command_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat" );
    private static final String SUN256 =
        "http://www.starlink.ac.uk/stilts/sun256/";

    /**
     * Constructor.
     *
     * @param   parent  parent window
     * @param   reporter   supplies the command
     * @param   size   preferred size of the monitor panel that displays
     *                 the STILTS command; if null some default will be used
     */
    @SuppressWarnings("this-escape")
    public StiltsDialog( Window parent, StiltsReporter reporter,
                         Dimension size ) {
        super( dialogTitle( parent ), parent );
        reporter_ = reporter;
        monitor_ = new BasicStiltsMonitor();
        JComponent content = new JPanel( new BorderLayout() );
        getContentPane().add( content, BorderLayout.CENTER );

        /* Add stilts command text panel. */
        JComponent textContainer =
            StiltsMonitor.wrapTextPanel( monitor_.getTextPanel() );
        if ( size != null ) {
            textContainer.setPreferredSize( size );
        }
        content.add( textContainer, BorderLayout.CENTER );

        /* Action to update the display.  The display should get updated
         * automatically, but this can force it. */
        Action updateAct =
            BasicAction.create( "Update", ResourceIcon.REDO,
                                "Force command update", evt -> update() );

        /* Set up menu items to configure the command formatting. */
        invokerSelector_ =
            new MenuSelector<StiltsInvoker>( "Invocation",
                                             StiltsInvoker.INVOKERS,
                                             StiltsInvoker.STILTS );
        tableNamerSelector_ =
            new MenuSelector<TopcatTableNamer>( "Table Names",
                                                TopcatTableNamer
                                               .getTableNamers() );
        lineEnderSelector_ =
            new MenuSelector<LineEnder>( "Line Endings", LineEnder.OPTIONS,
                                         LineEnder.BACKSLASH );
        indentSelector_ =
            new MenuSelector<Integer>( "Indent",
                                       IntStream.iterate( 0, i -> i + 1 )
                                                .limit( 9 ).boxed()
                                      .toArray( n -> new Integer[ n ] ) );
        indentSelector_.setSelectedItem( Integer.valueOf( 3 ) );
        dfltsToggle_ = new JCheckBoxMenuItem( "Include Defaults" );
        outToggle_ = new JCheckBoxMenuItem( "Suggest Output File" );
        outToggle_.setSelected( true );

        /* Ensure that the PlotStiltsMonitor is kept updated with the
         * configured formatter. */
        ActionListener updateListener = evt -> update();
        ActionForwarder fmtForwarder = new ActionForwarder();
        invokerSelector_.addActionListener( fmtForwarder );
        tableNamerSelector_.addActionListener( fmtForwarder );
        lineEnderSelector_.addActionListener( fmtForwarder );
        indentSelector_.addActionListener( fmtForwarder );
        dfltsToggle_.addChangeListener( fmtForwarder );
        outToggle_.addChangeListener( fmtForwarder );
        fmtForwarder.addActionListener( updateListener );
        content.addComponentListener( new ComponentAdapter() {
            @Override
            public void componentResized( ComponentEvent evt ) {
                update();
            }
        } );
        reporter.addStiltsListener( updateListener );

        /* Adjust the clipboard action so that it updates the text first.
         * This shouldn't be necessary, since the text ought to get updated
         * by cascading actions triggered by relevant parts of the GUI,
         * but there might be some omissions there, and this won't hurt. */
        Action clipAct0 = monitor_.getClipboardAction();
        Action clipAct =
            BasicAction
           .create( (String) clipAct0.getValue( Action.NAME ),
                    (Icon) clipAct0.getValue( Action.SMALL_ICON ),
                    (String) clipAct0.getValue( Action.SHORT_DESCRIPTION ),
                    evt -> {
                        update();
                        clipAct0.actionPerformed( evt );
                    } );

        /* Help on stilts command. */
        taskHelpAct_ =
            BasicAction
           .create( "Help for STILTS Command", ResourceIcon.STILTS_HELP,
                    "Show help for the current STILTS command in a web browser",
                    evt -> {
                        StiltsCommand cmd = createStiltsCommand();
                        if ( cmd != null ) {
                            String location = getTaskUrl( cmd.getTaskName() );
                            try {
                                TopcatUtils.getBrowserDesktop()
                                           .browse( new URI( location ) );
                            }
                            catch ( URISyntaxException | IOException e ) {
                                logger_.log( Level.WARNING,
                                             "Browser invocation failed ("
                                           + location + ")", e );
                            }
                        }
                    } );

        /* Formatting menu. */
        JMenu formatMenu = new JMenu( "Formatting" );
        formatMenu.add( invokerSelector_.getMenuItem() );
        formatMenu.add( tableNamerSelector_.getMenuItem() );
        formatMenu.add( lineEnderSelector_.getMenuItem() );
        formatMenu.add( indentSelector_.getMenuItem() );
        formatMenu.add( dfltsToggle_ );
        formatMenu.add( outToggle_ );

        /* Edit menu. */
        JMenu editMenu = new JMenu( "Edit" );
        editMenu.add( updateAct );
        editMenu.add( clipAct );

        /* Menu bar. */
        getJMenuBar().add( editMenu );
        getJMenuBar().add( formatMenu );

        /* Tool bar. */
        getToolBar().add( updateAct );
        getToolBar().add( clipAct );
        getToolBar().add( taskHelpAct_ );
        getToolBar().add( monitor_.getErrorAction() );
        getToolBar().addSeparator();

        /* Prepare for display. */
        addHelp( "StiltsDialog" );
        getHelpMenu().insert( taskHelpAct_, 0 );
        pack();
        update();
    }

    /**
     * Creates the StiltsCommand corresponding to the current state of
     * this window.
     *
     * @return  stilts command,
     *          may be null if insufficient/incorrect information
     */
    private StiltsCommand createStiltsCommand() {
        return reporter_
              .createStiltsCommand( tableNamerSelector_.getSelectedItem() );
    }

    /**
     * Updates the GUI according to the current state of this object.
     * Should be called if the state might have changed.
     */
    private void update() {
        CredibleString invocation =
            invokerSelector_.getSelectedItem().getInvocation();
        LineEnder lineEnder = lineEnderSelector_.getSelectedItem();
        int indent = indentSelector_.getSelectedItem().intValue();
        int cwidth = monitor_.getWidthCharacters();
        boolean includeDflts = dfltsToggle_.isSelected();
        boolean addSuggestions = outToggle_.isSelected();
        CommandFormatter formatter =
            new CommandFormatter( invocation, includeDflts, lineEnder,
                                  indent, cwidth, addSuggestions );
        StiltsCommand command = createStiltsCommand();
        monitor_.configure( command, formatter );
        monitor_.resetState();
        taskHelpAct_.setEnabled( command != null );
    }

    /**
     * Returns a suitable title for a StiltsDialog window serving a
     * supplied parent window.
     *
     * @param  parent  parent window
     */
    private static String dialogTitle( Window parent ) {
        String txt = "STILTS command";
        if ( parent instanceof Frame ) {
            String title = ((Frame) parent).getTitle();
            if ( title != null && title.trim().length() > 0 ) {
                txt += " for " + title;
            }
        }
        return txt;
    }

    /**
     * Returns the URL at which the task with a given name should
     * be documented.
     *
     * <p>At present this points to the expected location on the WWW of
     * the STILTS user manual, SUN/256.  That means it won't necessarily
     * pick up the right version of the documentation.
     * Perhaps I should bundle SUN/256 into the application jar file in
     * the same way as SUN/253, so it could point at the application's
     * web server.  But the content is quite a few extra Mb.
     *
     * @param  taskName  name of stilts task
     * @return  URL that should document the task
     */
    private static String getTaskUrl( String taskName ) {
        return SUN256 + taskName + ".html";
    }
}
