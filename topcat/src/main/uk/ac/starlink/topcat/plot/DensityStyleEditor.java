package uk.ac.starlink.topcat.plot;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.ttools.plot.DensityStyle;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.util.gui.RenderingComboBox;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * StyleEditor for density plots.
 *
 * @author   Mark Taylor
 * @since    13 Jan 2006
 */
public class DensityStyleEditor extends StyleEditor {

    final RenderingComboBox<DensityStyle> styleSelector_;

    /**
     * Constructor.
     * The style editor can only choose elements of the supplied
     * <code>styles</code> array.
     *
     * @param   styles  available styles
     * @param   rgbModel  toggler selected for RGB mode and unselected for
     *                    indexed mode
     */
    @SuppressWarnings("this-escape")
    public DensityStyleEditor( DensityStyle[] styles,
                               final ToggleButtonModel rgbModel ) {

        Box colorBox = Box.createHorizontalBox();
        styleSelector_ = new RenderingComboBox<DensityStyle>( styles ) {
            protected Icon getRendererIcon( DensityStyle style ) {
                return style.getLegendIcon();
            };
        };
        styleSelector_.addActionListener( this );
        final JLabel chanLabel = new JLabel( "Channel: " );
        colorBox.add( chanLabel );
        colorBox.add( new ShrinkWrapper( styleSelector_ ) );
        colorBox.add( Box.createHorizontalStrut( 5 ) );
        colorBox.add( Box.createHorizontalGlue() );
        colorBox.setBorder( AuxWindow.makeTitledBorder( "Colour" ) );

        add( colorBox );
        add( Box.createHorizontalStrut( 300 ) );

        ChangeListener rgbListener = new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                boolean isRgb = rgbModel.isSelected();
                chanLabel.setEnabled( isRgb );
                styleSelector_.setEnabled( isRgb );
            }
        };
        rgbModel.addChangeListener( rgbListener );
        rgbListener.stateChanged( null );
    }

    public void setStyle( Style style ) {
        styleSelector_.setSelectedItem( style );
    }

    public Style getStyle() {
        return (Style) styleSelector_.getSelectedItem();
    }

    public String getHelpID() {
        return "DensityStyleEditor";
    }
}
