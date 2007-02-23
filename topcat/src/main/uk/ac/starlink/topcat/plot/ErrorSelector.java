package uk.ac.starlink.topcat.plot;

import java.awt.Component;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Graphical component which allows the user to select the representation
 * of errors for a marker.
 *
 * @author   Mark Taylor
 * @since    23 Feb 2007
 */
public class ErrorSelector extends JPanel {

    private final int ndim_;
    private final JComboBox[] modeSelectors_;
    private final JComboBox renderSelector_;
    private final ActionForwarder actionForwarder_;
    private static final String[] AXIS_NAMES = new String[] { "X", "Y", "Z", };

    /**
     * Constructor.
     *
     * @param   ndim  potential dimensionality of error bars
     */
    public ErrorSelector( int ndim ) {
        setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
        ndim_ = ndim;
        actionForwarder_ = new ActionForwarder();

        modeSelectors_ = new JComboBox[ ndim ];
        Box modeLine = Box.createHorizontalBox();
        for ( int idim = 0; idim < ndim; idim++ ) {
            if ( idim > 0 ) {
                modeLine.add( Box.createHorizontalStrut( 10 ) );
            }
            modeSelectors_[ idim ] = new JComboBox( ErrorMode.getOptions() );
            modeSelectors_[ idim ].addActionListener( actionForwarder_ );
            modeSelectors_[ idim ].setSelectedIndex( 0 );
            modeLine.add( new JLabel( AXIS_NAMES[ idim ] + ": " ) );
            modeLine.add( new ShrinkWrapper( modeSelectors_[ idim ] ) );
        }
        modeLine.add( Box.createHorizontalGlue() );
        add( modeLine );
        add( Box.createVerticalStrut( 5 ) );
        renderSelector_ =
            new JComboBox( ndim == 2 ? ErrorRenderer.getOptions2d()
                                     : ErrorRenderer.getOptionsGeneral() );
        renderSelector_.addActionListener( actionForwarder_ );
        renderSelector_.setRenderer( new ErrorRendererRenderer() );
        renderSelector_.setSelectedIndex( 0 );
        Box renderLine = Box.createHorizontalBox();
        renderLine.add( new JLabel( "Shape: " ) );
        renderLine.add( new ShrinkWrapper( renderSelector_ ) );
        renderLine.add( Box.createHorizontalStrut( 5 ) );
        renderLine.add( new ComboBoxBumper( renderSelector_ ) );
        renderLine.add( Box.createHorizontalGlue() );
        add( renderLine );
        add( Box.createVerticalGlue() );
    }

    /**
     * Returns an array of the selectors used for error mode in each dimension.
     * Items in these selectors will be {@link ErrorMode} objects.
     *
     * @return   ndim-element array of selectors used for error mode objects
     */
    public JComboBox[] getModeSelectors() {
        return modeSelectors_;
    }

    /**
     * Returns the error style implied by the current state of this selector.
     *
     * @return  error style
     */
    public ErrorStyle getErrorStyle() {
        return new ErrorStyle( getModes(), getRenderer() );
    }

    /**
     * Sets the state of this selector to represent a given style.
     *
     * @param  style  error style
     */
    public void setErrorStyle( ErrorStyle style ) {
        setModes( style.getModes() );
        setRenderer( style.getRenderer() );
    }

    /**
     * Adds an action listener. 
     *
     * @param  listener  action listener
     */
    public void addActionListener( ActionListener listener ) {
        actionForwarder_.addListener( listener );
    }

    /**
     * Removes an action listener.
     *
     * @param  listener  action listener
     */
    public void removeActionListener( ActionListener listener ) {
        actionForwarder_.removeListener( listener );
    }

    /**
     * Returns the currently selected error renderer.
     *
     * @return  error renderer
     */
    private ErrorRenderer getRenderer() {
        return (ErrorRenderer) renderSelector_.getSelectedItem();
    }

    /**
     * Sets the currently selected error renderer.
     *
     * @param  renderer  error renderer
     */
    private void setRenderer( ErrorRenderer renderer ) {
        renderSelector_.setSelectedItem( renderer );
    }

    /**
     * Returns the modes currently selected by the error mode selectors.
     *
     * @return  ndim-element array of error modes
     */
    private ErrorMode[] getModes() {
        ErrorMode[] modes = new ErrorMode[ ndim_ ];
        for ( int idim = 0; idim < ndim_; idim++ ) {
            modes[ idim ] = (ErrorMode)
                            modeSelectors_[ idim ].getSelectedItem();
        }
        return modes;
    }

    /**
     * Sets the state of the error mode selectors.
     *
     * @param   modes  ndim-element array of error modes
     */
    private void setModes( ErrorMode[] modes ) {
        for ( int idim = 0; idim < ndim_; idim++ ) {
            ErrorMode mode = idim < modes.length ? modes[ idim ]
                                                 : ErrorMode.NONE;
            modeSelectors_[ idim ].setSelectedItem( mode );
        }
    }

    /**
     * Class which performs rendering of ErrorRenderer objects in a JComboBox.
     */
    private static class ErrorRendererRenderer extends BasicComboBoxRenderer {
        public Component getListCellRendererComponent( JList list, Object value,
                                                       int index,
                                                       boolean isSelected,
                                                       boolean hasFocus ) {
            Component c =
                super.getListCellRendererComponent( list, value, index,
                                                    isSelected, hasFocus );
            if ( c instanceof JLabel ) {
                JLabel label = (JLabel) c;
                Icon icon = value instanceof ErrorRenderer
                          ? new ColoredIcon( ((ErrorRenderer) value)
                                            .getLegendIcon(),
                                             c.getForeground() )
                          : null;
                label.setText( icon == null ? "??" : null );
                label.setIcon( icon );
            }
            return c;
        }
    }
}
