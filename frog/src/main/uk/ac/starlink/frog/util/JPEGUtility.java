package uk.ac.starlink.frog.util;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import uk.ac.starlink.ast.gui.DecimalField;
import uk.ac.starlink.ast.gui.ScientificFormat;

/**
 * This class provides any JPEG utilities used in FROG. At present
 * this consists of the ability to print a given JComponent to a JPEG
 * file.
 * <p>
 * Do not rely on the jpeg codec anywhere else, as it may not be
 * present.
 *
 * @since $Date$
 * @author Peter W. Draper
 * @version $Id$
 */
public class JPEGUtility
{

    /**
     * Class of static methods, so no construction.
     */
    public void JPEGUtility () {
      // do nothing
    }
    
    /**
     * Display a dialog window to choose a file for storing a JPEG.
     * Offers the options to define the width and height and whether
     * to fit to the choosen width and height. The default width and
     * height are defined as the size of the a given component.
     */
    public static void showJPEGChooser( JComponent component )
    {
        JPEGChooser chooser = new JPEGChooser( component );
        chooser.show();
        if ( chooser.accepted() ) {
            printJPEG( chooser.getFile(), component,
                       chooser.getWidth(), chooser.getHeight(),
                       chooser.getFit() );
        }
    }

    /**
     * Create a JPEG file from the contents of a JComponent.
     *
     * @param outputFile the output file.
     * @param component the component to print
     * @param width the width of the JPEG
     * @param height the height of the JPEG
     * @param fit whether to scale the width and height of the component
     *            to fit. Normally you should pass width and height as
     *            the size of the component.
     */
    public static void printJPEG( File outputFile,
                                  JComponent component,
                                  int width, int height,
                                  boolean fit )
    {
        //  Get a BufferedImage to draw graphics in.
        BufferedImage image = new BufferedImage( width, height,
                                                 BufferedImage.TYPE_INT_RGB );

        //  Get the Graphics2D object needed to render into this.
        Graphics2D g2d = image.createGraphics();

        //  Scale the graphics to fit the width and height if
        //  requested.
        if ( fit ) {
            fitToWidthAndHeight( g2d, component, width, height );
        }

        //  Now make the JComponent draw into this.
        component.print( g2d );
        g2d.dispose();

        // JPEG-encode the image and write to file.
        try {
            OutputStream os =  new FileOutputStream( outputFile );
            JPEGImageEncoder encoder =  JPEGCodec.createJPEGEncoder( os );
            encoder.encode( image );
            os.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *  Scale the current graphic so that it fits within the
     *  given width and height.
     */
    static protected void fitToWidthAndHeight( Graphics2D g2,
                                               JComponent component,
                                               int width, int height )
    {
        double compWidth = (double) component.getWidth();
        double compHeight = (double) component.getHeight();
        double xscale = (double)width / compWidth;
        double yscale = (double)height / compHeight;
        if ( xscale < yscale ) {
            yscale = xscale;
        }
        else {
            xscale = yscale;
        }

        // Apply the scale.
        g2.scale( xscale, yscale );
    }

    //
    // Internal class for choosing a JPEG filename and selecting from
    // a set of common options.
    //
    static class JPEGChooser extends JDialog
    {
        /**
         * Whether exit request was OK, otherwise cancel.
         */
        protected boolean accepted = false;

        /**
         * The dialog contentpane.
         */
        protected JPanel contentPane;

        /**
         * Accept and exit button.
         */
        protected JButton okButton = new JButton();

        /**
         * Cancel and exit button.
         */
        protected JButton cancelButton = new JButton();

        /**
         * Scale graphics to fit the JPEG option.
         */
        protected JCheckBox fitButton = new JCheckBox();

        /**
         * X dimension for output file.
         */
        protected DecimalField xSize = null;

        /**
         * Y dimension for output file.
         */
        protected DecimalField ySize = null;

        /**
         * Name of the output file.
         */
        protected JTextField fileName = new JTextField();

        /**
         * Construct an instance with default configuration.
         */
        public JPEGChooser( JComponent component )
        {
            super();
            setModal( true );
            setTitle( Utilities.getTitle( "Print to JPEG image" ) );
            init( component );
        }

        /**
         * Construct an instance, setting the parent, window title and
         * whether the dialog is modal.
         */
        public JPEGChooser( JComponent component, Frame owner,
                            String title, boolean modal)
        {
            super( owner, title, modal );
            init( component );
        }

        /**
         * Start common initialisation sequence.
         */
        protected void init( JComponent component )
        {
            enableEvents( AWTEvent.WINDOW_EVENT_MASK );
            try {
                initUI( component );
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            this.accepted = false;
        }

        /**
         * Initialise the user interface.
         */
        private void initUI( JComponent component )
        {
            //  Get the dialog content pane and set the layout manager.
            contentPane = (JPanel) this.getContentPane();
            contentPane.setLayout( new BorderLayout() );
            contentPane.setBorder
                ( BorderFactory.createEmptyBorder( 10, 10, 10, 10 ) );

            //  Configure and populate the controls for selecting
            //  size and name options.
            JPanel centrePanel = new JPanel( new GridBagLayout() );
            centrePanel.setBorder
                (BorderFactory.createTitledBorder("JPEG image properties")); 

            //  Choose whether to scale to fit.
            fitButton.setSelected( false );

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets( 5, 5, 5, 5 );
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.weightx = 0.0;
            gbc.gridwidth = 1;
            centrePanel.add( new JLabel( "Scale to fit:" ), gbc );

            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.weightx = 1.0;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            centrePanel.add( fitButton, gbc );
            fitButton.setToolTipText
                ( "Scale plot graphics to fit the width and height" );

            //  Offer size of output JPEG in pixels.
            ScientificFormat scientificFormat = new ScientificFormat();
            xSize = new DecimalField( 0, 10, scientificFormat );
            xSize.setIntValue( component.getWidth() );

            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.weightx = 0.0;
            gbc.gridwidth = 1;
            centrePanel.add( new JLabel( "X size:" ), gbc );

            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.weightx = 1.0;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            centrePanel.add( xSize, gbc );
            xSize.setToolTipText( "Width, in pixels, of the "+
                                  "output image (default is actual size" );

            scientificFormat = new ScientificFormat();
            ySize = new DecimalField( 0, 10, scientificFormat );
            ySize.setIntValue( component.getHeight() );

            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.weightx = 0.0;
            gbc.gridwidth = 1;
            centrePanel.add( new JLabel( "Y size:" ), gbc );

            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.weightx = 1.0;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            centrePanel.add( ySize, gbc );
            ySize.setToolTipText( "Height, in pixels, of the "+
                                  "output image (default is actual size" );
            
            //  Get a name for the file.
            fileName.setText( Utilities.getReleaseName() + ".jpg" );

            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.weightx = 0.0;
            gbc.gridwidth = 1;
            centrePanel.add( new JLabel( "Output file:" ), gbc );

            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.weightx = 1.0;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            centrePanel.add( fileName, gbc );
            fileName.setToolTipText( "Name for the output JPEG file" );

            //  Configure and place the OK and Cancel buttons.
            JPanel buttonPanel = new JPanel();
            buttonPanel.setBorder( BorderFactory.createEmptyBorder(10,10,0,0));
            buttonPanel.setLayout( new BoxLayout( buttonPanel, 
                                                  BoxLayout.X_AXIS ) );
            buttonPanel.add( Box.createHorizontalGlue() );
            buttonPanel.add( okButton );
            buttonPanel.add( Box.createHorizontalGlue() );
            buttonPanel.add( cancelButton );
            buttonPanel.add( Box.createHorizontalGlue() );
            
            //  Set various close window buttons.
            okButton.setText( "OK" );
            okButton.addActionListener( new ActionListener() {
                    public void actionPerformed( ActionEvent e ) {
                        closeWindow( true );
                    }
                });
            okButton.setToolTipText( "Press to create the JPEG" );

            cancelButton.setText( "Cancel" );
            cancelButton.addActionListener( new ActionListener() {
                    public void actionPerformed( ActionEvent e ) {
                        closeWindow( false );
                    }
                });
            cancelButton.setToolTipText( "Press to cancel creation of JPEG" );

            //  Add main panels to contentPane.
            contentPane.add( buttonPanel, BorderLayout.SOUTH );
            contentPane.add( centrePanel, BorderLayout.CENTER );
            pack();
        }

        /**
         * Return the exit status of the dialog.
         */
        public boolean accepted()
        {
            return accepted;
        }

        /**
         * Return whether user opted for scaling the graphic to fit.
         */
        public boolean getFit()
        {
            return fitButton.isSelected();
        }

        /**
         * Return the width of the output file.
         */
        public int getWidth()
        {
            return xSize.getIntValue();
        }

        /**
         * Return the height of the output file.
         */
        public int getHeight()
        {
            return ySize.getIntValue();
        }

        /**
         * Return the name of the output file.
         */
        public File getFile()
        {
            return new File( fileName.getText() );
        }

        /**
         * Close the window. If argument is true then the result is accepted.
         */
        protected void closeWindow( boolean accepted )
        {
            this.accepted = accepted;
            hide();
        }
    }
}
