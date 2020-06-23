package uk.ac.starlink.topcat.activate;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;
import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import uk.ac.starlink.auth.AuthManager;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.topcat.LineBox;
import uk.ac.starlink.topcat.Outcome;
import uk.ac.starlink.topcat.Safety;
import uk.ac.starlink.topcat.TopcatJELUtils;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.jel.RandomJELRowReader;
import uk.ac.starlink.util.IOUtils;

/**
 * Activation type that downloads a URL to local storage.
 *
 * @author   Mark Taylor
 * @since    9 May 2018
 */
public class DownloadActivationType implements ActivationType {

    public String getName() {
        return "Download URL";
    }

    public String getDescription() {
        return "Download the resource in a URL column to local disk";
    }

    public ActivatorConfigurator createConfigurator( TopcatModelInfo tinfo ) {
        return new DownloadConfigurator( tinfo );
    }

    public Suitability getSuitability( TopcatModelInfo tinfo ) {
        return tinfo.getUrlSuitability();
    }

    /**
     * Implementation that downloads a resource to local disk.
     */
    private static class DownloadConfigurator extends UrlColumnConfigurator {
        private final TopcatModel tcModel_;
        private final JTextField dirField_;
        private final JTextField filenameField_;
        private static final String DIR_KEY = "dir";
        private static final String FILENAME_KEY = "filename";
        private static JFileChooser chooser_;

        /**
         * Constructor.
         *
         * @param  tinfo  topcat model information
         */
        DownloadConfigurator( TopcatModelInfo tinfo ) {
            super( tinfo, "Resource URL", new ColFlag[] { ColFlag.URL, } );
            setLocationLabel( "Resource URL" );
            tcModel_ = tinfo.getTopcatModel();
            JComponent queryPanel = getQueryPanel();
            dirField_ = new JTextField() {
                @Override
                public Dimension getMaximumSize() {
                    return new Dimension( super.getMaximumSize().width,
                                          super.getPreferredSize().height );
                }
            };
            filenameField_ = new JTextField();
            Action dirAction = new AbstractAction( "Browse" ) {
                public void actionPerformed( ActionEvent evt ) {
                    File dir = browseDirectory();
                    if ( dir != null ) {
                        dirField_.setText( dir.toString() );
                    }
                }
            };
            dirField_.addActionListener( getActionForwarder() );
            filenameField_.getCaret().addChangeListener( getActionForwarder() );
            JComponent dirLine = Box.createHorizontalBox();
            dirLine.add( new JLabel( "Directory: " ) );
            dirLine.add( dirField_ );
            dirLine.add( Box.createHorizontalStrut( 5 ) );
            dirLine.add( new JButton( dirAction ) );
            queryPanel.add( new LineBox( "Filename Expression",
                                         filenameField_ ) );
            queryPanel.add( Box.createVerticalStrut( 5 ) );
            queryPanel.add( dirLine );
        }

        protected Activator createActivator( ColumnData cdata ) {
            boolean invokeOnEdt = false;
            final String dir = dirField_.getText();
            final EvaluateKit filenameKit;
            try {
                filenameKit = createFilenameKit();
            }
            catch ( CompilationException e ) {
                return null;
            }
            if ( filenameKit == null ) {
                return null;
            }
            return new UrlColumnActivator( cdata, invokeOnEdt ) {
                protected Outcome activateUrl( URL url, long lrow ) {
                    Object filenameObj;
                    try {
                        filenameObj = filenameKit.evaluateAtRow( lrow );
                    }
                    catch ( Throwable e ) {
                        return Outcome.failure( "Error getting filename: "
                                              + e );
                    }
                    if ( filenameObj == null ) {
                        return Outcome.failure( "No filename" );
                    }
                    String filename = filenameObj.toString();
                    if ( filename.trim().length() == 0 ) {
                        return Outcome.failure( "No filename" );
                    }
                    try {
                        download( url, filename, dir );
                    }
                    catch ( IOException e ) {
                        return Outcome.failure( e );
                    }
                    return Outcome.success( filename );
                }
            };
        }

        public String getConfigMessage( ColumnData cdata ) {
            try {
                return createFilenameKit() == null
                     ? "No filename specified"
                     : null;
            }
            catch ( CompilationException e ) {
                return "Filename expression error: " + e.getMessage();
            }
        }

        public Safety getSafety() {
            return Safety.UNSAFE;
        }

        public ConfigState getState() {
            ConfigState state = getUrlState();
            state.saveText( DIR_KEY, dirField_ );
            state.saveText( FILENAME_KEY, filenameField_ );
            return state;
        }

        public void setState( ConfigState state ) {
            setUrlState( state );
            state.restoreText( DIR_KEY, dirField_ );
            state.restoreText( FILENAME_KEY, filenameField_ );
        }

        /**
         * Returns an object that can determine the specified filename
         * at a given table row.
         *
         * @return  filename evaluator
         */
        private EvaluateKit createFilenameKit() throws CompilationException {
            String expr = filenameField_.getText();
            if ( expr == null || expr.trim().length() == 0 ) {
                return null;
            }
            RandomJELRowReader rowReader = tcModel_.createJELRowReader();
            Library lib = TopcatJELUtils.getLibrary( rowReader, false );
            CompiledExpression compEx = Evaluator.compile( expr, lib, null );
            return new EvaluateKit( rowReader, compEx );
        }

        /**
         * Pops up a file chooser and allows the user to select a
         * destination directory.
         *
         * @return  selected directory, or null if selection was cancelled
         */
        private File browseDirectory() {

            /* The chooser is static, and deliberately shared between
             * instances of this class; it's likely that the user will want
             * to save things in similar places. */
            if ( chooser_ == null ) {
                chooser_ = new JFileChooser( "." );
                chooser_.setApproveButtonText( "Select" );
                chooser_.setDialogTitle( "Select download directory" );
                chooser_.setDialogType( JFileChooser.CUSTOM_DIALOG );
                chooser_.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
                chooser_.setMultiSelectionEnabled( false );
            }
            int status = chooser_.showDialog( getQueryPanel(), "Select" );
            return status == JFileChooser.APPROVE_OPTION
                 ? chooser_.getSelectedFile()
                 : null;
        }
    }

    /**
     * Copies a remote resource to local storage.
     *
     * @param  url  remote resource
     * @param  filename  relative or absolute name of local file
     * @param  dir  directory context for filename; may be null
     */
    public static void download( URL url, String filename, String dir )
            throws IOException {
        File file = dir != null && dir.trim().length() > 0
                  ? new File( dir, filename )
                  : new File( filename );
        InputStream urlIn = null;
        OutputStream fileOut = null;
        try {
            urlIn = AuthManager.getInstance().openStream( url );
            fileOut = new FileOutputStream( file );
            IOUtils.copy( urlIn, fileOut );
        }
        finally {
            if ( urlIn != null ) {
                urlIn.close();
            }
            if ( fileOut != null ) {
                fileOut.close();
            }
        }
    }

    /**
     * Defines an object that can evaluate an expression at a given row
     * of a table.  This is just an aggregation of a row reader and a
     * compiled expression.
     */
    private static class EvaluateKit {
        final RandomJELRowReader rdr_;
        final CompiledExpression compEx_;

        /**
         * Constructor.
         *
         * @param  rdr  row reader
         * @param  compEx  compiled expression for use with <code>rdr</code>
         */
        EvaluateKit( RandomJELRowReader rdr, CompiledExpression compEx ) {
            rdr_ = rdr;
            compEx_ = compEx;
        }

        /**
         * Evaluates this kit's expression at a given row of the table.
         *
         * @param  lrow  row index
         * @return   evaluated expression
         */
        public Object evaluateAtRow( long lrow ) throws Throwable {
            return rdr_.evaluateAtRow( compEx_, lrow );
        }
    }
}
