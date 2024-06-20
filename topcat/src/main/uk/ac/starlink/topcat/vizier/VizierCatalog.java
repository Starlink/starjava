package uk.ac.starlink.topcat.vizier;

/**
 * Constains all known information about a catalogue resource.
 *
 * @author   Mark Taylor
 * @since    3 Nov 2009
 */
public class VizierCatalog implements Queryable {
    private final String name_;
    private final String description_;
    private final Integer density_;
    private final String[] lambdas_;
    private final String[] astros_;
    private final Integer cpopu_;
    private final Float ipopu_;
    private Integer nTable_;
    private Long nRow_;

    /**
     * Constructor.
     *
     * @param  name  name
     * @param  description  description
     * @param  density  density
     * @param  lambdas  terms from wavelength controlled vocabulary
     * @param  astros   terms from astronomy controlled vocabulary
     * @param  cpopu   integer giving popularity
     * @param  ipopu   float giving popularity
     */
    VizierCatalog( String name, String description, Integer density,
                   String[] lambdas, String[] astros, Integer cpopu,
                   Float ipopu ) {
        name_ = name;
        description_ = description;
        density_ = density;
        lambdas_ = lambdas;
        astros_ = astros;
        cpopu_ = cpopu;
        ipopu_ = ipopu;
    }

    /**
     * Returns catalogue name.
     *
     * @return name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns catalogue description.
     *
     * @return  description
     */
    public String getDescription() {
        return description_;
    }

    /**
     * Returns measure of density (on sky?).
     *
     * @return  density
     */
    public Integer getDensity() {
        return density_;
    }

    /**
     * Returns wavelength categories covered by this catalogue.
     *
     * @return  terms from wavelength controlled vocabulary
     */
    public String[] getLambdas() {
        return lambdas_;
    }

    /**
     * Returns astronomy type categories covered by this catalogue.
     *
     * @return  terms from astronomy controlled vocabulary
     */
    public String[] getAstros() {
        return astros_;
    }

    /**
     * Returns popularity as number of calls.
     *
     * @return  integer giving popularity
     */
    public Integer getCpopu() {
        return cpopu_;
    }

    /**
     * Returns popularity score.
     *
     * @return  float giving popularity
     */
    public Float getIpopu() {
        return ipopu_;
    }

    /**
     * Sets the number of tables reported for this catalog.
     * Only makes much sense for a top-level entry.
     *
     * @param   nTable  number of subordinate tables
     */
    public void setTableCount( int nTable ) {
        nTable_ = Integer.valueOf( nTable );
    }

    /**
     * Returns the number of tables reported for this catalog.
     *
     * @return  number of subordinate tables
     */
    public Integer getTableCount() {
        return nTable_;
    }

    /**
     * Sets the number of rows reported for this table.
     * Only makes much sense for a single table.
     *
     * @param  nRow  row count
     */
    public void setRowCount( Long nRow ) {
        nRow_ = nRow;
    }

   
    /**
     * Returns the number of rows reported for this table.
     *
     * @return  row count
     */
    public Long getRowCount() {
        return nRow_;
    }

    public String getQuerySource() {
        return name_;
    }

    public String getQueryId() {
        return name_.replace( '/', '.' );
    }
}
