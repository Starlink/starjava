package uk.ac.starlink.splat.iface;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import uk.ac.starlink.ast.gui.DecimalField;
import uk.ac.starlink.ast.gui.ScientificFormat;

/**
 * Creates a compound component that displays a floating point text
 * entry field and a coupled slider. The text field is optional so
 * this component may also be just used as a floating point slider.
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
public class FloatJSlider extends JPanel 
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
     * Whether the text field is on show.
     */
    protected boolean showTextField = true;

    /**
     * Slider model that supports floating point.
     */
    private FloatJSliderModel model;

    /**
     *  Create an instance of FloatJSlider with text entry field.
     */
    FloatJSlider( FloatJSliderModel model ) {
        this( model, true );
    }

    /**
     *  Create an instance of FloatJSlider, with optional text entry field.
     */
    FloatJSlider( FloatJSliderModel model, boolean showTextField ) 
    {
        this.model = model;
        this.showTextField = showTextField;

        //  Use a BoxLayout to arrange the widgets in a row.
        setLayout( new BoxLayout( this, BoxLayout.X_AXIS ) );

        if ( showTextField ) {
            // Add the text field.  It initially displays "0" and needs
            // to be at least 10 columns wide.
            ScientificFormat scientificFormat = new ScientificFormat();
            valueField = new DecimalField( 0, 10, scientificFormat );
            valueField.setDoubleValue( model.getDoubleValue() );
            valueField.addActionListener( new ActionListener() {
                    public void actionPerformed( ActionEvent e ) {
                        syncModel();
                        fireStateChanged( new ChangeEvent( this ) );
                    }
                });
        }
            
        // Add the slider.
        slider = new JSlider( model );
        model.addChangeListener( new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                syncText();
                fireStateChanged( e );
            }
        });

        // Put them in place.
        if ( showTextField ) {
            add( valueField );
        }
        add( slider );
    }

    /**
     *  Syncronise the model to the text field value.
     */
    protected void syncModel()
    {
        model.setDoubleValue( valueField.getDoubleValue() );
    }

    /**
     *  Syncronise the text field to the model (slider) value.
     */
    protected void syncText()
    {
        if ( showTextField ) {
            valueField.setDoubleValue( model.getDoubleValue() );
        }
    }

    /**
     * Returns the current values
     */
    public double getValue() {
        return model.getDoubleValue();
    }

    /**
     * Set the tooltip for the text and slider.
     */
    public void setToolTipText( String tip ) {
        if ( showTextField ) {
            valueField.setToolTipText( tip );
        }
        slider.setToolTipText( tip );
        super.setToolTipText( tip );
    }

    /**
     * Enable both components.
     */
    public void enable()
    {
        slider.setEnabled( true );
        if ( showTextField ) {
            valueField.setEnabled( true );
        }
    }

    /**
     * Disable both components.
     */
    public void disable()
    {
        slider.setEnabled( false );
        if ( showTextField ) {
            valueField.setEnabled( false );
        }
    }


//
//  ChangeListener interface proxy to that of the Slider.
//
    protected EventListenerList listeners = new EventListenerList();

    /**
     * Adds a ChangeListener.
     */
    public void addChangeListener( ChangeListener l ) {
        listeners.add( ChangeListener.class, l );
    }

    /**
     * Forward a ChangeEvent from the slider.
     */
    protected void fireStateChanged( ChangeEvent e ) {
        Object[] la = listeners.getListenerList();
        for (int i = la.length - 2; i >= 0; i -= 2) {
            if (la[i]==ChangeListener.class) {
                ((ChangeListener)la[i+1]).stateChanged( e );
            }
        }
    }

    /**
     *  Test method.
     */
    public static void main( String args[] ) {

        FloatJSliderModel model = new
            FloatJSliderModel( 1.0, 1.0, 100.0, 0.01 );
        FloatJSlider fslider = new FloatJSlider( model );

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
