package uk.ac.starlink.table;

/**
 * Hook for adding behaviour to StarTableFactory table loading.
 *
 * @author   Mark Taylor
 * @since    28 Sep 2017
 * @see    StarTableFactory
 */
public interface TablePreparation {

    /**
     * Performs arbitrary operations on a given table that has been
     * loaded by a given input handler.
     *
     * @param   table  table that has just been loaded
     * @param   builder   the input handler that loaded it if known,
     *                    otherwise null
     * @return   the table that will be returned from table creation methods;
     *           may or may not be equal to the input <code>table</code>
     */
    StarTable prepareLoadedTable( StarTable table, TableBuilder builder );
}
