package uk.ac.starlink.ecsv;

/**
 * Defines an object that can extract ECSV header information from YAML.
 *
 * @author   Mark Taylor
 * @since    28 Apr 2020
 */
public interface YamlParser {

    /**
     * Extracts ECSV header information from lines of YAML.
     *
     * @param  lines   lines of YAML
     * @return   ECSV metadata structure
     */
    EcsvMeta parseMeta( String[] lines ) throws EcsvFormatException;
}
