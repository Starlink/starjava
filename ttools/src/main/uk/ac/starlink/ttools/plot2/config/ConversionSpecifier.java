package uk.ac.starlink.ttools.plot2.config;

import java.awt.event.ActionListener;
import javax.swing.JComponent;
import uk.ac.starlink.ttools.plot2.ReportMap;

/**
 * Specifier implementation that adapts an existing one to dispense
 * values of a different parameterised type.
 * Implementations of methods to convert between the input (<code>I</code>)
 * and output (<code>O</code>) types must be provided.
 *
 * @author   Mark Taylor
 * @since    13 Feb 2014
 */
public abstract class ConversionSpecifier<I,O> implements Specifier<O> {

    private final Specifier<I> baseSpec_;

    /**
     * Constructor.
     *
     * @param  baseSpec   specifier on which this one is based
     */
    protected ConversionSpecifier( Specifier<I> baseSpec ) {
        baseSpec_ = baseSpec;
    }

    /**
     * Converts a value from input (base) to output type.
     *
     * @param  inValue  input type value
     * @return  output type value
     */
    protected abstract O inToOut( I inValue );

    /**
     * Converts a value from output to input (base) type.
     *
     * @param  outValue   output type value
     * @return  input type value
     */
    protected abstract I outToIn( O outValue );

    public JComponent getComponent() {
        return baseSpec_.getComponent();
    }

    public O getSpecifiedValue() {
        return inToOut( baseSpec_.getSpecifiedValue() );
    }

    public void setSpecifiedValue( O outValue ) {
        baseSpec_.setSpecifiedValue( outToIn( outValue ) );
    }

    public void addActionListener( ActionListener listener ) {
        baseSpec_.addActionListener( listener );
    }

    public void removeActionListener( ActionListener listener ) {
        baseSpec_.removeActionListener( listener );
    }

    public void submitReport( ReportMap report ) {
        baseSpec_.submitReport( report );
    }

    public boolean isXFill() {
        return baseSpec_.isXFill();
    }
}
