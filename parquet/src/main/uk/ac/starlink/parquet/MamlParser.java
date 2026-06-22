package uk.ac.starlink.parquet;

import uk.ac.starlink.table.TableFormatException;

/**
 * Implementation-independent interface for parsing MAML metadata.
 *
 * @author   Mark Taylor
 * @since    22 Jun 2026
 */
public interface MamlParser {

    /**
     * Parse a YAML string to produce a MamlMetadata object.
     *
     * @param   txt  input YAML
     * @return   MAML metadata structure
     * @throws   TableFormatException   if YAML is incorrect
     */
    MamlMetadata parseMaml( String txt ) throws TableFormatException;
}
