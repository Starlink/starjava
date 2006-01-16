package uk.ac.starlink.topcat.plot;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JLabel;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.util.gui.RenderingComboBox;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * StyleEditor for density plots.
 *
 * @author   Mark Taylor
 * @since    13 Jan 2006
 */
public class DensityStyleEditor extends StyleEditor {

    final RenderingComboBox styleSelector_;

    /**
     * Constructor.
     * The style editor can only choose elements of the supplied
     * <code>styles</code> array.
     *
     * @param   styles  available styles
     */
    public DensityStyleEditor( DensityStyle[] styles ) {

        Box colorBox = Box.createHorizontalBox();
        styleSelector_ = new RenderingComboBox( styles ) {
            protected Icon getRendererIcon( Object item ) {
                return ((Style) item).getLegendIcon();
            };
        };
        styleSelector_.addActionListener( this );
        colorBox.add( new JLabel( "Channel: " ) );
        colorBox.add( new ShrinkWrapper( styleSelector_ ) );
        colorBox.add( Box.createHorizontalStrut( 5 ) );
        colorBox.add( Box.createHorizontalGlue() );
        colorBox.setBorder( AuxWindow.makeTitledBorder( "Colour" ) );

        add( colorBox );
        add( Box.createHorizontalStrut( 300 ) );
    }

    public void setStyle( Style style ) {
        styleSelector_.setSelectedItem( style );
    }

    public Style getStyle() {
        return (Style) styleSelector_.getSelectedItem();
    }
}
