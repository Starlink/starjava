package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.ttools.gui.ColorComboBox;
import uk.ac.starlink.ttools.gui.DashComboBox;
import uk.ac.starlink.ttools.gui.MarkStyleSelectors;
import uk.ac.starlink.ttools.gui.ThicknessComboBox;
import uk.ac.starlink.ttools.plot.ErrorMode;
import uk.ac.starlink.ttools.plot.ErrorRenderer;
import uk.ac.starlink.ttools.plot.MarkShape;
import uk.ac.starlink.ttools.plot.MarkStyle;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.util.gui.ComboBoxBumper;
import uk.ac.starlink.util.gui.ShrinkWrapper;
import uk.ac.starlink.util.gui.ValueButtonGroup;

/**
 * StyleEditor implementation for suitable for a 
 * {@link uk.ac.starlink.ttools.plot.LinesPlot}.
 * The style objects used are (currently) 
 * {@link uk.ac.starlink.ttools.plot.MarkStyle}s.
 *
 * @author   Mark Taylor
 * @since    14 Mar 2006
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class LinesStyleEditor extends StyleEditor {

    private final ErrorModeSelectionModel[] errorModeModels_;
    private final ColorComboBox colorSelector_;
    private final ThicknessComboBox thickSelector_;
    private final DashComboBox dashSelector_;
    private final JComboBox shapeSelector_;
    private final JComboBox sizeSelector_;
    private final JComboBox errorSelector_;
    private final ValueButtonGroup<List<Boolean>> lineMarkSelector_;

    private static final int MAX_THICK = 6;

    /**
     * Constructor.
     *
     * @param  errorRenderers  list of error renderers to be available from
     *         this style editor
     * @param   defaultRenderer  default error renderer to use if no other
     *          is known
     * @param  errorModeModels  error mode selection models
     */
    @SuppressWarnings("this-escape")
    public LinesStyleEditor( ErrorRenderer[] errorRenderers,
                             ErrorRenderer defaultRenderer,
                             ErrorModeSelectionModel[] errorModeModels ) {
        super();
        errorModeModels_ = errorModeModels;

        /* Lines/markers selection. */
        JRadioButton lineButton = new JRadioButton( "Line", true );
        JRadioButton markButton = new JRadioButton( "Markers" );
        JRadioButton bothButton = new JRadioButton( "Both" );
        lineMarkSelector_ = new ValueButtonGroup<List<Boolean>>();
        lineMarkSelector_.add( lineButton, booleanList( true, false ) );
        lineMarkSelector_.add( markButton, booleanList( false, true ) );
        lineMarkSelector_.add( bothButton, booleanList( true, true ) );
        lineMarkSelector_.addChangeListener( this );

        /* Colour selector. */
        colorSelector_ = new ColorComboBox();
        colorSelector_.addActionListener( this );

        /* Line style selectors. */
        thickSelector_ = new ThicknessComboBox( MAX_THICK );
        thickSelector_.addActionListener( this );
        dashSelector_ = new DashComboBox();
        dashSelector_.addActionListener( this );

        /* Marker style selectors. */
        shapeSelector_ = MarkStyleSelectors.createShapeSelector();
        shapeSelector_.addActionListener( this );
        sizeSelector_ = MarkStyleSelectors.createSizeSelector();
        sizeSelector_.addActionListener( this );

        /* Error style selector. */
        errorSelector_ =
            MarkStyleSelectors.createErrorSelector( errorRenderers,
                                                    defaultRenderer,
                                                    errorModeModels );
        errorSelector_.addActionListener( this );
        for ( int idim = 0; idim < errorModeModels.length; idim++ ) {
            errorModeModels[ idim ].addActionListener( this );
        }

        /* Place components. */
        JComponent colorBox = Box.createHorizontalBox();
        colorBox.add( new JLabel( "Colour: " ) );
        colorBox.add( new ShrinkWrapper( colorSelector_ ) );
        colorBox.add( Box.createHorizontalStrut( 5 ) );
        colorBox.add( new ComboBoxBumper( colorSelector_ ) );
        colorBox.add( Box.createHorizontalStrut( 5 ) );
        colorBox.add( Box.createHorizontalGlue() );

        JComponent lineMarkBox = Box.createHorizontalBox();
        lineMarkBox.add( lineButton );
        lineMarkBox.add( Box.createHorizontalStrut( 5 ) );
        lineMarkBox.add( markButton );
        lineMarkBox.add( Box.createHorizontalStrut( 5 ) );
        lineMarkBox.add( bothButton );
        lineMarkBox.add( Box.createHorizontalGlue() );

        JComponent lineBox = Box.createHorizontalBox();
        lineBox.add( new JLabel( "Thickness: " ) );
        lineBox.add( new ShrinkWrapper( thickSelector_ ) );
        lineBox.add( Box.createHorizontalStrut( 5 ) );
        lineBox.add( new ComboBoxBumper( thickSelector_ ) );
        lineBox.add( Box.createHorizontalStrut( 10 ) );
        lineBox.add( new JLabel( "Dash: " ) );
        lineBox.add( new ShrinkWrapper( dashSelector_ ) );
        lineBox.add( Box.createHorizontalStrut( 5 ) );
        lineBox.add( new ComboBoxBumper( dashSelector_ ) );
        lineBox.add( Box.createHorizontalStrut( 5 ) );
        lineBox.add( Box.createHorizontalGlue() );

        JComponent markBox = Box.createHorizontalBox();
        markBox.add( new JLabel( "Size: " ) );
        markBox.add( new ShrinkWrapper( sizeSelector_ ) );
        markBox.add( Box.createHorizontalStrut( 5 ) );
        markBox.add( new ComboBoxBumper( sizeSelector_ ) );
        markBox.add( Box.createHorizontalStrut( 10 ) );
        markBox.add( new JLabel( "Shape: " ) );
        markBox.add( new ShrinkWrapper( shapeSelector_ ) );
        markBox.add( Box.createHorizontalStrut( 5 ) );
        markBox.add( new ComboBoxBumper( shapeSelector_ ) );
        markBox.add( Box.createHorizontalStrut( 5 ) );
        markBox.add( Box.createHorizontalGlue() );

        JComponent errorBox = Box.createHorizontalBox();
        errorBox.add( new JLabel( "Error Bars: " ) );
        errorBox.add( new ShrinkWrapper( errorSelector_ ) );
        errorBox.add( Box.createHorizontalStrut( 5 ) );
        errorBox.add( new ComboBoxBumper( errorSelector_ ) );
        errorBox.add( Box.createHorizontalStrut( 5 ) );
        errorBox.add( Box.createHorizontalGlue() );

        JComponent displayBox = Box.createVerticalBox();
        JComponent d1Box = Box.createHorizontalBox();
        d1Box.add( colorBox );
        d1Box.add( Box.createHorizontalStrut( 10 ) );
        d1Box.add( errorBox );
        displayBox.add( d1Box );
        displayBox.add( Box.createVerticalStrut( 5 ) );
        displayBox.add( lineMarkBox );
        displayBox.setBorder( AuxWindow.makeTitledBorder( "Display" ) );
        add( displayBox );

        lineBox.setBorder( AuxWindow.makeTitledBorder( "Line" ) );
        add( lineBox );

        markBox.setBorder( AuxWindow.makeTitledBorder( "Markers" ) );
        add( markBox );
    }

    public void setStyle( Style style ) {
        MarkStyle mstyle = (MarkStyle) style;
        shapeSelector_.setSelectedItem( mstyle.getShapeId() );
        sizeSelector_.setSelectedIndex( mstyle.getSize() );
        colorSelector_.setSelectedItem( mstyle.getColor() );
        lineMarkSelector_
           .setValue( booleanList( mstyle.getLine() == MarkStyle.DOT_TO_DOT,
                                   ! mstyle.getHidePoints() ) );
        thickSelector_.setSelectedThickness( mstyle.getLineWidth() );
        dashSelector_.setSelectedDash( mstyle.getDash() );
        errorSelector_.setSelectedItem( mstyle.getErrorRenderer() );
    }

    public Style getStyle() {
        List<Boolean> lineMark = lineMarkSelector_.getValue();
        return getStyle( (MarkShape) shapeSelector_.getSelectedItem(),
                         sizeSelector_.getSelectedIndex(),
                         colorSelector_.getSelectedColor(),
                         lineMark.get( 0 ).booleanValue(),
                         lineMark.get( 1 ).booleanValue(),
                         (ErrorRenderer) errorSelector_.getSelectedItem(),
                         thickSelector_.getSelectedThickness(),
                         dashSelector_.getSelectedDash() );
    }

    public String getHelpID() {
        return "LinesStyleEditor";
    }

    protected void refreshState() {
        super.refreshState();
        List<Boolean> lineMark = lineMarkSelector_.getValue();
        boolean hasLine = lineMark.get( 0 ).booleanValue();
        boolean hasMark = lineMark.get( 1 ).booleanValue();
        boolean hasError =
            errorModeModels_[ 0 ].getErrorMode() != ErrorMode.NONE ||
            errorModeModels_[ 1 ].getErrorMode() != ErrorMode.NONE;
        thickSelector_.setEnabled( hasLine );
        dashSelector_.setEnabled( hasLine );
        sizeSelector_.setEnabled( hasMark );
        shapeSelector_.setEnabled( hasMark );
        errorSelector_.setEnabled( hasError );
    }

    /**
     * Returns a style as described by its attributes.
     *
     * @param  shape  marker shape
     * @param  size   marker size
     * @param  color  marker colour
     * @param  hasLine  whether lines are plotted
     * @param  hasMark  whether markers are plotted
     * @param  errorRenderer  error renderer
     * @param  thick  line thickness
     * @param  dash   line dash pattern
     * @return line/marker style
     */
    private static MarkStyle getStyle( MarkShape shape, int size, Color color,
                                       boolean hasLine, boolean hasMark,
                                       ErrorRenderer errorRenderer, int thick,
                                       float[] dash ) {
        MarkStyle style = size == 0 ? MarkShape.POINT.getStyle( color, 0 )
                                    : shape.getStyle( color, size );
        style.setLine( hasLine ? MarkStyle.DOT_TO_DOT : null );
        style.setHidePoints( ! hasMark );
        style.setErrorRenderer( errorRenderer );
        style.setLineWidth( thick );
        style.setDash( dash );
        return style;
    }

    /**
     * Utility method which turns a pair of <code>boolean</code> values
     * into a two-element <code>List</code> of <code>Boolean</code> objects.
     *
     * @param   flag1  first value
     * @param   flag2  second value
     * @return  list containing Boolean objects with values flag1, flag2
     */
    private static List<Boolean> booleanList( boolean flag1, boolean flag2 ) {
        return Arrays.asList( new Boolean[] { Boolean.valueOf( flag1 ),
                                              Boolean.valueOf( flag2 ), } );
    }
}
