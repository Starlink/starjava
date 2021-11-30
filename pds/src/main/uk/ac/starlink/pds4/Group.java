package uk.ac.starlink.pds4;

/**
 * Characterises a PDS4 Group_Field_* object.
 * These correspond to (possibly nested) groups of table fields.
 *
 * @author   Mark Taylor
 * @since    30 Nov 2021
 */
public interface Group extends RecordItem {

    /**
     * Returns the number of repetitions of this group.
     * This is the PDS4 <code>repetitions</code> item.
     *
     * @return  repetition count
     */
    int getRepetitions();

    /**
     * Returns the 1-based byte offset into the record at which
     * this group is found.
     * This is the PDS4 <code>group_location</code> item,
     * and only appears for Binary and Character groups.
     *
     * @return 1-based group location byte offset,
     *         or negative value for Delimited groups
     */
    int getGroupLocation();

    /**
     * Returns the number of bytes this group occupies in a fixed-length record.
     * This is the PDS4 <code>group_length</code> item,
     * and only appears for Binary and Character groups.
     *
     * @return  group total byte count,
     *          or negative value for Delimited groups
     */
    int getGroupLength();

    /**
     * Returns the optional group name.
     *
     * @return  group name, or null
     */
    String getName();

    /**
     * Returns the optional group description.
     *
     * @return  group description, or null
     */
    String getDescription();

    /**
     * Returns the fields or nested groups that are present in this group.
     *
     * @return   array of fields and groups
     */
    RecordItem[] getContents();
}
