package uk.ac.starlink.datanode.nodes;

import java.awt.Component;
import javax.swing.Action;

/**
 * Defines an interface for nodes to display detailed information about
 * themselves.  Methods are provided for simple text markup (heading-value
 * pairs) and addition of deferred-constructions panels for custom display
 * of relevant data.
 *
 * @author   Mark Taylor (Starlink)
 */
public interface DetailViewer {

    /**
     * Adds a top-level title to the display.
     *
     * @param  title  title text
     */
    void addTitle( String title );

    /**
     * Adds a subheading to the display.
     *
     * @param  text  subheading text
     */
    void addSubHead( String text );

    /**
     * Adds a key-value paired information item for string data.
     *
     * @param  name   key text
     * @param  value  value
     */
    void addKeyedItem( String name, String value );

    /**
     * Adds a key-value paired information item for Object data.
     *
     * @param  name   key text
     * @param  value  value
     */
    void addKeyedItem( String name, Object value );

    /**
     * Adds a key-value paired information item for double data.
     *
     * @param  name   key text
     * @param  value  value
     */
    void addKeyedItem( String name, double value );

    /**
     * Adds a key-value paired information item for float data.
     *
     * @param  name   key text
     * @param  value  value
     */
    void addKeyedItem( String name, float value );

    /**
     * Adds a key-value paired information item for long data.
     *
     * @param  name   key text
     * @param  value  value
     */
    void addKeyedItem( String name, long value );

    /**
     * Adds a key-value paired information item for int data.
     *
     * @param  name   key text
     * @param  value  value
     */
    void addKeyedItem( String name, int value );
 
    /**
     * Adds a key-value paired information item for boolean data.
     *
     * @param  name   key text
     * @param  value  value
     */
    void addKeyedItem( String name, boolean value );

    /**
     * Logs an error in supplying data in some visible fashion.
     *
     * @param   err   error
     */
    void logError( Throwable err );

    /**
     * Adds a visible separator to the display.
     */
    void addSeparator();

    /**
     * Adds a small amount of space to the overview display.
     */
    void addSpace();

    /**
     * Adds unformatted text to the display.
     *
     * @param   text   text
     */
    void addText( String text );

    /**
     * Adds a component for optional display within this viewer.
     *
     * @param  title   title of the new component
     * @param  comp  component
     */
    void addPane( String title, Component comp );

    /**
     * Adds a deferred-construction component for optional display within
     * this viewer.  The component will draw itself at a fixed size and
     * will be contained within scrollbars if necessary.
     *
     * @param  title   title of the new component
     * @param  maker   component deferred factory
     */
    void addPane( String title, ComponentMaker maker );

    /**
     * Adds a new deferred-construction component which will 
     * draw itself at a size appropriate to the size of its container.
     * The <code>JComponent</code> returned by <code>maker</code>
     * should generally have a <code>paintComponent</code> method
     * which senses its actual size and draws itself accordingly,
     * something like this:
     * <pre>
     *     protected void paintComponent( Graphics g ) {
     *         super.paintComponent( g );
     *         doScaledPainting( getSize() );
     *     }
     * </pre>
     * or, perhaps for efficiency, more like this:
     * <pre>
     *     private Dimension lastSize;
     *     protected void paintComponent( Graphics g ) {
     *         super.paintComponent( g );
     *         Dimension size = getSize();
     *         if ( ! size.equals( lastSize ) ) {
     *             setPreferredSize( size );
     *             reconfigureComponentToSize( size );
     *         }
     *         doPainting();
     *     }
     * </pre>
     *
     * @param  title   title of the new component
     * @param  maker   component deferred factory
     */
    void addScalingPane( String title, ComponentMaker maker );
}
