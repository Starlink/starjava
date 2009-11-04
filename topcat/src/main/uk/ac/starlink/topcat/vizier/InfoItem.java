package uk.ac.starlink.topcat.vizier;

/**
 * Represents one of the static VizieR resources as obtained by the
 * VizierInfo class.
 *
 * @author   Mark Taylor
 * @since    3 Nov 2009
 */
public class InfoItem {
    private final String name_;
    private final String title_;
    private final Integer krows_;

    /**
     * Constructor.
     *
     * @param  name  item name
     * @param  title  item short description
     * @param  krows  number of thousands of rows (approx)
     */
    public InfoItem( String name, String title, Integer krows ) {
        name_ = name;
        title_ = title;
        krows_ = krows;
    }

    /**
     * Returns the item short name.
     *
     * @return  name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns the item short description.
     *
     * @return  title
     */
    public String getTitle() {
        return title_;
    }

    /**
     * Returns the number of thousands of rows.
     *
     * @return krows
     */
    public Integer getKrows() {
        return krows_;
    }
}
