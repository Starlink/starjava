package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.util.ContentCoding;

/**
 * Window which displays a simple view of a TAP query and its result.
 *
 * @author   Mark Taylor
 * @since    9 Jun 2015
 */
public class QuickLookWindow extends JFrame {

    private final TapQuery tq_;
    private final StarTableFactory tfact_;
    private final ContentCoding coding_;
    private final JTabbedPane tabber_;
    private final JComponent resultPanel_;
    private final ExecutorService executor_;
    private Future<?> job_;

    /**
     * Constructor.
     *
     * @param   tq  tap query to execute
     * @param   tfact   table factory
     * @param   coding  configures HTTP compression
     */
    @SuppressWarnings("this-escape")
    public QuickLookWindow( TapQuery tq, StarTableFactory tfact,
                            ContentCoding coding ) {
        setTitle( "TAP Quick Look" );
        tq_ = tq;
        tfact_ = tfact;
        coding_ = coding;

        /* Set up task cancellation on window close. */
        setDefaultCloseOperation( DISPOSE_ON_CLOSE );
        Action closeAct = new AbstractAction( "Close" ) {
            public void actionPerformed( ActionEvent evt ) {
                dispose();
            }
        };
        addWindowListener( new WindowAdapter() {
            public void windowClosed( WindowEvent evt ) {
                executor_.shutdownNow();
                setResultComponent( createLabelPanel( "Cancelled." ) );
            }
        } );
        executor_ = Executors.newSingleThreadExecutor( new ThreadFactory() {
            public Thread newThread( Runnable r ) {
                Thread thread = new Thread( r, "TapQuickLook" );
                thread.setDaemon( true );
                return thread;
            }
        } );

        /* Set up window components. */
        tabber_ = new JTabbedPane();
        tabber_.setPreferredSize( new Dimension( 400, 180 ) );
        Container content = getContentPane();
        content.setLayout( new BorderLayout() );
        content.add( new JLabel( "TAP Quick Look" ), BorderLayout.NORTH );
        content.add( tabber_, BorderLayout.CENTER );
        JComponent buttonLine = Box.createHorizontalBox();
        buttonLine.add( Box.createHorizontalGlue() );
        buttonLine.add( new JButton( closeAct ) );
        buttonLine.add( Box.createHorizontalGlue() );
        buttonLine.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
        content.add( buttonLine, BorderLayout.SOUTH );
        resultPanel_ = new JPanel( new BorderLayout() );
        tabber_.add( "Query", createTextPanel( tq.getAdql() ) );
        tabber_.add( "Result", resultPanel_ );
        pack();
    }

    /**
     * Begins execution of this window's TAP query.
     * The query is performed asynchronously, and the window will be
     * update when it completes.
     */
    public void executeQuery() {
        setResultComponent( createLabelPanel( "Running ..." ) );
        job_ = executor_.submit( new Runnable() {
            public void run() {
                try {
                    final StarTable table =
                        tq_.executeSync( tfact_.getStoragePolicy(), coding_ );
                    schedule( new Runnable() {
                        public void run() {
                            displayTable( table );
                        }
                    } );
                }
                catch ( final IOException e ) {
                    schedule( new Runnable() {
                        public void run() {
                            displayError( e );
                        }
                    } );
                }
            }
            private void schedule( final Runnable r ) {
                if ( ! job_.isCancelled() ) {
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            if ( ! job_.isCancelled() ) {
                                r.run();
                            }
                        }
                    } );
                }
            }
        } );
    }

    /**
     * Displays the result table.
     *
     * @param  table  table to display
     */
    private void displayTable( StarTable table ) {
        StarJTable jt = new StarJTable( table, true );
        jt.configureColumnWidths( 350, 10000 );
        setResultComponent( new JScrollPane( jt ) );
    }

    /**
     * Displays an execution error.
     *
     * @param  error  error to display
     */
    private void displayError( IOException error ) {
        StringWriter traceWriter = new StringWriter();
        error.printStackTrace( new PrintWriter( traceWriter ) );
        String trace = traceWriter.toString();
        setResultComponent( createTextPanel( trace ) );
    }

    /**
     * Presents a given component in the Result tab of this window.
     *
     * @param   comp  new contents of result tab
     */
    private void setResultComponent( JComponent comp ) {
        resultPanel_.removeAll();
        resultPanel_.add( comp );
        resultPanel_.revalidate();
        resultPanel_.repaint();
        tabber_.setSelectedIndex( 1 );
    }

    /**
     * Returns a component to display given multi-line text.
     *
     * @param  txt  text content
     * @return  scroll pane containing text component
     */
    private static JComponent createTextPanel( String txt ) {
        JTextArea textPanel = new JTextArea();
        textPanel.setEditable( false );
        textPanel.setLineWrap( false );
        textPanel.setText( txt );
        textPanel.setCaretPosition( 0 );
        return new JScrollPane( textPanel );
    }

    /**
     * Returns a component to display a short text message.
     *
     * @param  txt  text content
     * @return   component containing text
     */
    private static JComponent createLabelPanel( String txt ) {
        JComponent hbox = Box.createHorizontalBox();
        hbox.add( Box.createHorizontalGlue() );
        hbox.add( new JLabel( txt ) );
        hbox.add( Box.createHorizontalGlue() );
        JComponent vbox = Box.createVerticalBox();
        vbox.add( Box.createVerticalGlue() );
        vbox.add( hbox );
        vbox.add( Box.createVerticalGlue() );
        return vbox;
    }
}
