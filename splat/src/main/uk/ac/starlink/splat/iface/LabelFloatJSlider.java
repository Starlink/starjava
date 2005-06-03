package uk.ac.starlink.splat.iface;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.starlink.ast.gui.DecimalField;
import uk.ac.starlink.ast.gui.ScientificFormat;

/**
 * Creates a compound component that displays a label, floating
 * points text entry field and a coupled slider.
 * <p>
 * The major advantage of this component over a simple JSlider is that
 * the apparent representation of the values is as a range of floating
 * point values. The precision of the value is initially determined by
 * setting the number of steps between the slider limits.
 *
 * @author Peter W. Draper
 * @since $Date$
 * @since 17-OCT-2000
 * @version $Id$
 */
public class LabelFloatJSlider extends JPanel 
{
    /**
     * Reference to text field showing the current value.
     */
    protected DecimalField valueField;

    /**
     * The slider widget.
     */
    protected JSlider slider;

    /**
     * The label
     */
    protected JLabel label;
    
    /**
     * Slider model that supports floating point.
     */
    private FloatJSliderModel model;

    /**
     * Label text value.
     */
    protected String title;

    /**
     *  Create an instance of LabelFloatJSlider.
     */
    LabelFloatJSlider( String title, FloatJSliderModel model) 
    {
        this.title = title;
        this.model = model;
        final FloatJSliderModel localModel = model;

        //  Use a BoxLayout to arrange the widgets in a row.
        setLayout( new BoxLayout( this, BoxLayout.X_AXIS ) );

        // Add the label.
        label = new JLabel( title );

        // Add the text field.  It initially displays "0" and needs
        // to be at least 10 columns wide.
        ScientificFormat scientificFormat = new ScientificFormat();
        //scientificFormat.setMaximumFractionDigits( 6 );
        valueField = new DecimalField( 0, 10, scientificFormat ); 
        valueField.setDoubleValue( model.getDoubleValue() );
        valueField.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                localModel.setDoubleValue( valueField.getDoubleValue() );
            }
        });

        // Add the slider.
        slider = new JSlider( model );
        model.addChangeListener( new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                valueField.setDoubleValue( localModel.getDoubleValue() );
            }
        });

        // Put them in place.
        add( label );
        add( valueField );
        add( slider );
    }

    /**
     * Returns the current values
     */
    public double getValue() 
    {
        return model.getDoubleValue();
    }

    /**
     *  Test method.
     */
    public static void main( String args[] ) 
    {
        FloatJSliderModel model = new 
            FloatJSliderModel( 1.0, 1.0, 100.0, 0.01 );
        LabelFloatJSlider fslider = new LabelFloatJSlider( 
                                       "Data Value:", model );
        
        //  Create a frame to hold graphics.
        JFrame frame = new JFrame();
        frame.getContentPane().add( fslider );

        //  Make all components of window decide their sizes.
        frame.pack();

        //  Center the window on the screen.
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = frame.getSize();
        if ( frameSize.height > screenSize.height ) {
            frameSize.height = screenSize.height;
        }
        if ( frameSize.width > screenSize.width ) {
            frameSize.width = screenSize.width;
        }
        frame.setLocation( ( screenSize.width - frameSize.width ) / 2,
                           ( screenSize.height - frameSize.height ) / 2 ); 

        //  Make interface visible.
        frame.setVisible( true );

        //  Application exits when this window is closed.
        frame.addWindowListener( new WindowAdapter() {
            public void windowClosing( WindowEvent evt ) {
                System.exit( 1 );
            }
        });
    }
}
