package uk.ac.starlink.topcat.plot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Style editor for histogram bars.
 *
 * @author   Mark Taylor
 * @since    11 Jan 2006
 */
public class BarStyleEditor extends StyleEditor {

    private final JComboBox colorSelector_;
    private final JComboBox formSelector_;
    private final JComboBox placeSelector_;
    private final JComboBox thickSelector_;

    private static final int MAX_THICK = 8;
    private static final Color[] COLORS = Styles.COLORS;
    private static final float[][] DASHES = Styles.DASHES;
    private static final BarStyle.Form[] FORMS = new BarStyle.Form[] {
        BarStyle.FORM_OPEN,
        BarStyle.FORM_FILLED,
        BarStyle.FORM_FILLED3D,
        BarStyle.FORM_TOP,
        BarStyle.FORM_SPIKE,
    };
    private static final BarStyle.Placement[] PLACEMENTS =
            new BarStyle.Placement[] {
        BarStyle.PLACE_OVER,
        BarStyle.PLACE_ADJACENT,
    };

    /**
     * Constructor.
     */
    public BarStyleEditor() {
        super();

        colorSelector_ = new JComboBox( COLORS );
        colorSelector_.addActionListener( this );
        formSelector_ = new JComboBox( FORMS );
        formSelector_.addActionListener( this );
        placeSelector_ = new JComboBox( PLACEMENTS );
        placeSelector_.addActionListener( this );
        thickSelector_ = new JComboBox( createNumberedModel( MAX_THICK ) );
        thickSelector_.addActionListener( this );

        JComponent colorBox = Box.createHorizontalBox();
        colorBox.add( new JLabel( "Color: " ) );
        colorBox.add( new ShrinkWrapper( colorSelector_ ) );
        colorBox.add( Box.createHorizontalStrut( 5 ) );
        colorBox.add( new ComboBoxBumper( colorSelector_ ) );
        colorBox.add( Box.createHorizontalGlue() );

        JComponent formBox = Box.createHorizontalBox();
        formBox.add( new JLabel( "Bar Form: " ) );
        formBox.add( new ShrinkWrapper( formSelector_ ) );
        formBox.add( Box.createHorizontalStrut( 5 ) );
        formBox.add( new ComboBoxBumper( formSelector_ ) );
        formBox.add( Box.createHorizontalGlue() );
 
        JComponent placeBox = Box.createHorizontalBox();
        placeBox.add( new JLabel( "Bar Placement: " ) );
        placeBox.add( new ShrinkWrapper( placeSelector_ ) );
        placeBox.add( Box.createHorizontalGlue() );

        JComponent thickBox = Box.createHorizontalBox();
        thickBox.add( new JLabel( "Line Thickness: " ) );
        thickBox.add( new ShrinkWrapper( thickSelector_ ) );
        thickBox.add( Box.createHorizontalStrut( 5 ) );
        thickBox.add( new ComboBoxBumper( thickSelector_ ) );
        thickBox.add( Box.createHorizontalGlue() );

        JComponent barBox = Box.createVerticalBox();
        barBox.add( colorBox );
        barBox.add( formBox );
        barBox.add( placeBox );
        barBox.add( thickBox );
        barBox.setBorder( AuxWindow.makeTitledBorder( "Bars" ) );
        add( barBox );
    }

    public void setStyle( Style st ) {
        BarStyle style = (BarStyle) st;
        colorSelector_.setSelectedItem( style.getColor() );
        formSelector_.setSelectedItem( style.getForm() );
        placeSelector_.setSelectedItem( style.getPlacement() );
        Stroke stroke = style.getStroke();
        int thick = stroke instanceof BasicStroke
                  ? (int) ((BasicStroke) stroke).getLineWidth()
                  : 1;
        thickSelector_.setSelectedIndex( thick - 1 );
    }

    public Style getStyle() {
        return getStyle( (Color) colorSelector_.getSelectedItem(),
                         (BarStyle.Form) formSelector_.getSelectedItem(),
                         (BarStyle.Placement) placeSelector_.getSelectedItem(),
                         thickSelector_.getSelectedIndex() + 1, null );
    }

    private static Style getStyle( Color color, BarStyle.Form form,
                                   BarStyle.Placement placement, int thick,
                                   float[] dash ) {
        Stroke stroke = new BasicStroke( (float) thick, BasicStroke.CAP_SQUARE,
                                         BasicStroke.JOIN_MITER, 10f, dash,
                                         0f );
        if ( form == BarStyle.FORM_TOP ) {
            placement = BarStyle.PLACE_OVER;
        }
        return new BarStyle( color, stroke, form, placement );
    }
}
