package uk.ac.starlink.topcat.plot2;

import java.awt.Component;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import uk.ac.starlink.topcat.LineBox;
import uk.ac.starlink.ttools.plot.GraphicExporter;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.util.gui.CustomComboBoxRenderer;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Provides a GUI for exporting a plot to an external format,
 * generally to a file.
 *
 * @author   Mark Taylor
 * @since    12 Mar 2013
 */
public class PlotExporter {

    private final JFileChooser saveChooser_;
    private final JComboBox formatSelector_;
    private final JCheckBox bitmapButton_;
    private static final GraphicExporter[] EXPORTERS = createExporters();
    private static PlotExporter instance_;

    /**
     * Constructor.
     */
    public PlotExporter() {
        saveChooser_ = new JFileChooser( "." );
        saveChooser_.setDialogTitle( "Export Plot" );
        formatSelector_ = new JComboBox( EXPORTERS );
        bitmapButton_ = new JCheckBox( "Force Bitmap" );
        formatSelector_.setRenderer(
                new CustomComboBoxRenderer<GraphicExporter>
                                          ( GraphicExporter.class, "(auto)" ) {
            @Override
            protected String mapValue( GraphicExporter exporter ) {
                return exporter.getName();
            }
        } );
        JComponent formatBox = Box.createVerticalBox();
        formatBox.add( new LineBox( new JLabel( "File Format:" ) ) );
        formatBox.add( Box.createVerticalStrut( 5 ) );
        formatBox.add( new LineBox( new ShrinkWrapper( formatSelector_ ) ) );
        formatBox.add( Box.createVerticalStrut( 5 ) );
        formatBox.add( new LineBox( bitmapButton_ ) );
        formatBox.add( Box.createVerticalGlue() );
        saveChooser_.setAccessory( formatBox );
    }

    /**
     * Offers the user a GUI to export a supplied plot icon
     * in a user-chosen format.
     *
     * @param  parent   parent component for dialogue window
     * @param  ifact    supplies the icon to export
     */
    public void exportPlot( Component parent, IconFactory ifact ) {
        while ( saveChooser_.showDialog( parent, "Export Plot" )
                == JFileChooser.APPROVE_OPTION ) {
            File file = saveChooser_.getSelectedFile();
            GraphicExporter exporter = getExporter( file );
            if ( exporter == null ) {
                JOptionPane
               .showMessageDialog( parent,
                                   "Can't guess auto file format for " + file,
                                   "Save failure", JOptionPane.ERROR_MESSAGE );
            }
            else {
                Icon icon = ifact.getExportIcon( bitmapButton_.isSelected() );
                try {
                    attemptSave( icon, file, exporter );
                    return;
                }
                catch ( Exception e ) {
                    ErrorDialog.showError( parent, "Plot Export Error", e,
                                           "Failed to export plot in "
                                         + exporter.getName() + " format to "
                                         + file );
                }
            }
        }
    }

    /**
     * Attempts to write a given icon to a file in a particular
     * graphics format.
     *
     * @param  icon   image to paint
     * @param  file   destination file
     * @param  exporter   output graphics format handler
     * @throws  IOException  in case of write error
     */
    public void attemptSave( final Icon icon, File file,
                             GraphicExporter exporter )
            throws IOException {
        OutputStream out =
            new BufferedOutputStream( new FileOutputStream( file ) );
        try {
            exporter.exportGraphic( PlotUtil.toPicture( icon ), out );
        }
        finally {
            out.close();
        }
    }

    /**
     * Returns a single instance of this class.
     * You don't have to use it as a singleton, but doing it like that
     * allows it to retain current directory for output file etc. 
     *
     * @return  shared instance
     */
    public static PlotExporter getInstance() {
        if ( instance_ == null ) {
            instance_ = new PlotExporter();
        }
        return instance_;
    }

    /**
     * Returns a graphics output format handler to use for a given file.
     * If a handler is explicitly selected in the GUI that will be returned,
     * but otherwise (auto mode) the filename will be examined to see if
     * one or other of the available handlers looks likely.
     * If none can be found/guessed, null is returned.
     *
     * @param    file   destination file
     * @return   appropriate exporter, or null
     */
    private GraphicExporter getExporter( File file ) {
        Object fmtobj = formatSelector_.getSelectedItem();
        if ( fmtobj instanceof GraphicExporter ) {
            return (GraphicExporter) fmtobj;
        }
        assert fmtobj == null;
        String fname = file.getName();
        for ( int ie = 0; ie < EXPORTERS.length; ie++ ) {
            GraphicExporter exporter = EXPORTERS[ ie ];
            String[] suffixes = exporter == null ? new String[ 0 ]
                                                 : exporter.getFileSuffixes();
            for ( int is = 0; is < suffixes.length; is++ ) {
                if ( fname.toLowerCase()
                          .endsWith( suffixes[ is ].toLowerCase() ) ) {
                    return exporter;
                }
            }
        }
        return null;
    }

    /**
     * Returns the default list of available graphics output format handlers.
     *
     * @return   exporter list
     */
    private static GraphicExporter[] createExporters() {
        return PlotUtil.arrayConcat(
            new GraphicExporter[] { null },
            GraphicExporter.getKnownExporters( PlotUtil.LATEX_PDF_EXPORTER ) );
    }

    /**
     * Defines an object that can supply an icon for exporting.
     */
    public interface IconFactory {

        /**
         * Returns an icon for export.
         *
         * @param  forceBitmap   true to force bitmap output of vector graphics,
         *                       false to use default behaviour
         * @return  icon
         */
        Icon getExportIcon( boolean forceBitmap );
    }
}
