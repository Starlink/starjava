package uk.ac.starlink.ttools.plot2.config;

import uk.ac.starlink.ttools.plot2.ReportKey;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.ReportMeta;
import uk.ac.starlink.ttools.plot2.layer.Combiner;
import uk.ac.starlink.ttools.plot2.layer.Unit;

/**
 * ConfigKey for unit selection.
 * The main trick this OptionConfigKey subclass has is to manage
 * Reporting of a plot's <code>Combiner.Type</code>,
 * so it can work in concert with a Combiner config key to provide
 * a more comprehensible GUI.
 * If the Combiner is not density-like, then this selector is
 * irrelevant, and so the GUI component is disabled.
 * Client PlotLayers should assemble ReportMaps with the ReportKey
 * from this ConfigKeys'
 * {@link #getCombinerTypeReportKey} method filled in appropriately.
 *
 * @author   Mark Taylor
 * @since    25 Jan 2018
 */
public class PerUnitConfigKey<U extends Unit> extends OptionConfigKey<U> {

    private final ReportKey<Combiner.Type> ctypeRepkey_;

    /**
     * Constructor.
     *
     * @param   meta   metadata
     * @param   clazz  class to which all the possible options belong
     * @param   options  array of possible values for this key
     * @param   dflt   default option, should be one of <code>options</code>
     */
    public PerUnitConfigKey( ConfigMeta meta, Class<U> clazz, U[] options,
                             U dflt ) {
        super( meta, clazz, options, dflt, false );
        ctypeRepkey_ =
            ReportKey
           .createObjectKey( new ReportMeta( "ctype", "Combiner Type" ),
                             Combiner.Type.class, false );
        setOptionUsage();
        addOptionsXml();
    }

    /**
     * Returns a ReportKey that client PlotLayers should fill in
     * when generating their ReportMap.
     *
     * @return  combiner key report key
     */
    public ReportKey<Combiner.Type> getCombinerTypeReportKey() {
        return ctypeRepkey_;
    }

    public String getXmlDescription( U unit ) {
        return unit.getDescription();
    }

    @Override
    public Specifier<U> createSpecifier() {
        Specifier<U> specifier = new ComboBoxSpecifier<U>( getOptions() ) {
            public String stringify( U value ) {
                return valueToString( value );
            }
            @Override
            public void submitReport( ReportMap reportMap ) {
                Combiner.Type ctype = reportMap.get( ctypeRepkey_ );
                boolean isEnabled = ctype == null 
                                 || Combiner.Type.DENSITY.equals( ctype );
                getComboBox().setEnabled( isEnabled );
            }
        };
        specifier.setSpecifiedValue( getDefaultValue() );
        return specifier;
    }
}
