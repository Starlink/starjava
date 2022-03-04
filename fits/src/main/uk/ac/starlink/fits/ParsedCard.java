package uk.ac.starlink.fits;

/**
 * Represents a single parsed FITS header card.
 * This is always the representation of a single 80-byte FITS header.
 *
 * @author   Mark Taylor
 * @since    4 Mar 2022
 */
public class ParsedCard<T> {

    private final String key_;
    private final CardType<T> type_;
    private final T value_;
    private final String comment_;

    /**
     * Constructor.
     *
     * @param  key  FITS keyword
     * @param  type  type of card
     * @param  value  header  value
     * @param  comment  comment text
     */
    public ParsedCard( String key, CardType<T> type, T value, String comment ) {
        key_ = key;
        type_ = type;
        value_ = value;
        comment_ = comment;
    }

    /**
     * Returns the FITS keyword.
     *
     * @return  keyword, may be null
     */
    public String getKey() {
        return key_;
    }

    /**
     * Returns the type of header card.
     *
     * @return  card type
     */
    public CardType<T> getType() {
        return type_;
    }

    /**
     * Returns the keyword value.
     *
     * @return  value, may be null
     */
    public T getValue() {
        return value_;
    }

    /**
     * Returns the comment text associated with this card.
     *
     * @return  comment, may be null
     */
    public String getComment() {
        return comment_;
    }
}
