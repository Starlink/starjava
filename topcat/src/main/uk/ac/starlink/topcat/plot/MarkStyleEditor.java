package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Component;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import uk.ac.starlink.topcat.AuxWindow;

/**
 * StyleEditor implementation for editing {@link MarkStyle} objects.
 *
 * @author   Mark Taylor
 * @since    10 Jan 2005
 */
public class MarkStyleEditor extends StyleEditor {

    private final JCheckBox markFlagger_;
    private final JComboBox shapeSelector_;
    private final JComboBox sizeSelector_;
    private final JComboBox colorSelector_;

    private static final int MAX_SIZE = 5;
    private static final Color[] COLORS = Styles.COLORS;
    private static final MarkShape[] SHAPES = new MarkShape[] {
            MarkShape.FILLED_CIRCLE,
            MarkShape.OPEN_CIRCLE,
            MarkShape.CROSS,
            MarkShape.CROXX,
            MarkShape.OPEN_SQUARE,
            MarkShape.OPEN_DIAMOND,
            MarkShape.OPEN_TRIANGLE_UP,
            MarkShape.OPEN_TRIANGLE_DOWN,
            MarkShape.FILLED_SQUARE,
            MarkShape.FILLED_DIAMOND,
            MarkShape.FILLED_TRIANGLE_UP,
            MarkShape.FILLED_TRIANGLE_DOWN,
        };

    /**
     * Constructor.
     */
    public MarkStyleEditor() {
        super();

        /* Marker box. */
        markFlagger_ = new JCheckBox( "Plot Marker" );
        markFlagger_.setSelected( true );
        markFlagger_.addActionListener( this );

        /* Shape selector. */
        shapeSelector_ =
            new JComboBox( new DefaultComboBoxModel( SHAPES ) );
        shapeSelector_.setSelectedIndex( 0 );
        shapeSelector_.setRenderer( new MarkRenderer() {
            public MarkShape getMarkShape( int index ) {
                return (MarkShape) shapeSelector_.getItemAt( index );
            }
            public Color getMarkColor() {
                return Color.BLACK;
            }
            public int getMarkSize() {
                return 5;
            }
        } );
        shapeSelector_.addActionListener( this );

        /* Size selector. */
        sizeSelector_ = new JComboBox( createNumberedModel( MAX_SIZE + 1 ) );
        sizeSelector_.setSelectedIndex( 0 );
        sizeSelector_.setRenderer( new MarkRenderer( true ) {
            public int getMarkSize( int index ) {
                return index;
            }
            public Color getMarkColor() {
                return Color.BLACK;
            }
            public MarkShape getMarkShape() {
                return MarkShape.OPEN_SQUARE;
            }
        } );
        sizeSelector_.addActionListener( this );

        /* Colour selector. */
        colorSelector_ = new JComboBox( new DefaultComboBoxModel( COLORS ) );
        colorSelector_.setSelectedIndex( 0 );
        colorSelector_.setRenderer( new MarkRenderer() {
            public Color getMarkColor( int index ) {
                return (Color) colorSelector_.getItemAt( index );
            }
            public int getMarkSize() {
                return 5;
            }
            public MarkShape getMarkShape() {
                return MarkShape.FILLED_SQUARE;
            }
        } );
        colorSelector_.addActionListener( this );

        /* Place components. */
        JComponent markBox = Box.createHorizontalBox();
        markBox.add( markFlagger_ );
        markBox.add( Box.createHorizontalStrut( 10 ) );
        markBox.add( new JLabel( "Shape: " ) );
        markBox.add( shapeSelector_ );
        markBox.add( Box.createHorizontalStrut( 5 ) );
        markBox.add( new ComboBoxBumper( shapeSelector_ ) );
        markBox.add( Box.createHorizontalStrut( 10 ) );
        markBox.add( new JLabel( "Size: " ) );
        markBox.add( sizeSelector_ );
        markBox.add( Box.createHorizontalStrut( 5 ) );
        markBox.add( new ComboBoxBumper( sizeSelector_ ) );
        markBox.add( Box.createHorizontalStrut( 10 ) );
        markBox.add( new JLabel( "Colour: " ) );
        markBox.add( colorSelector_ );
        markBox.add( Box.createHorizontalStrut( 5 ) );
        markBox.add( new ComboBoxBumper( colorSelector_ ) );
        markBox.setBorder( AuxWindow.makeTitledBorder( "Marker" ) );
        add( markBox );
    }

    public void setStyle( Style style ) {
        MarkStyle mstyle = (MarkStyle) style;
        shapeSelector_.setSelectedItem( mstyle.getShapeId() );
        sizeSelector_.setSelectedIndex( mstyle.getSize() );
        colorSelector_.setSelectedItem( mstyle.getColor() );
    }

    public Style getStyle() {
        return getStyle( (MarkShape) shapeSelector_.getSelectedItem(),
                         sizeSelector_.getSelectedIndex(),
                         (Color) colorSelector_.getSelectedItem() );
    }

    /**
     * Constructs a new ComboBoxModel which contains Integers numbered from
     * 0 to <code>count-1</code>.
     *
     * @param   count  number of entries in the model
     * @return  new ComboBoxModel filled with Integers
     */
    private static ComboBoxModel createNumberedModel( int count ) {
        Object[] items = new Object[ count ];
        for ( int i = 0; i < count; i++ ) {
            items[ i ] = new Integer( i );
        }
        return new DefaultComboBoxModel( items );
    }

    /**
     * Returns a MarkStyle described by a shape, size and colour.
     *
     * @param  shape  marker shape
     * @param  size   marker size (>=1)
     * @param  color  marker colour
     * @return  marker
     */
    private static MarkStyle getStyle( MarkShape shape, int size,
                                       Color color ) {
        return size == 0 ? MarkShape.POINT.getStyle( color, 1 )
                         : shape.getStyle( color, size );
    }

    /**
     * ComboBoxRenderer class suitable for rendering MarkStyles.
     */
    private class MarkRenderer extends BasicComboBoxRenderer {
        private boolean useText_;
        MarkRenderer() {
            this( false );
        }
        MarkRenderer( boolean useText ) {
            useText_ = useText;
        }
        MarkShape getMarkShape( int itemIndex ) {
            return getMarkShape();
        }
        MarkShape getMarkShape() {
            return (MarkShape) shapeSelector_.getSelectedItem();
        }
        int getMarkSize( int itemIndex ) {
            return getMarkSize();
        }
        int getMarkSize() {
            return sizeSelector_.getSelectedIndex();
        }
        Color getMarkColor( int itemIndex ) {
            return getMarkColor();
        }
        Color getMarkColor() {
            return (Color) colorSelector_.getSelectedItem();
        }
        public Component getListCellRendererComponent( JList list, Object value,
                                                       int index,
                                                       boolean isSelected,
                                                       boolean hasFocus ) {
            Component c =
                super.getListCellRendererComponent( list, value, index,
                                                    isSelected, hasFocus );
            if ( c instanceof JLabel ) {
                JLabel label = (JLabel) c;
                if ( ! useText_ ) {
                    setText( null );
                }
                MarkStyle style = index >= 0 ? getStyle( getMarkShape( index ),
                                                         getMarkSize( index ),
                                                         getMarkColor( index ) )
                                             : getStyle( getMarkShape(),
                                                         getMarkSize(),
                                                         getMarkColor() );
                label.setIcon( Styles.getLegendIcon( style, 15, 15 ) );
            }
            return c;
        }
    }

}
