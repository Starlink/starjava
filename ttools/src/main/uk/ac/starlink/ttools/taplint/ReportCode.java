package uk.ac.starlink.ttools.taplint;

/**
 * Labels a taplint report.
 * This interface just aggregates a report type and a 4-character label.
 *
 * @author   Mark Taylor
 * @since    11 Jun 2014
 */
public interface ReportCode {

    /**
     * Returns the type of this code.
     *
     * @return  type
     */
    ReportType getType();

    /**
     * Returns the 4-character label of this code.
     *
     * @return  4-character label
     */
    String getLabel();
}
