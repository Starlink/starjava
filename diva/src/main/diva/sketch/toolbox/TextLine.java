package diva.sketch.toolbox;
import diva.sketch.recognition.Type;
import diva.sketch.recognition.TypedData;

/**
 * Native class that defines a line of text. It also defines the avg 
 * x-height and character width. 
 *
 * @author Niraj Shah  (niraj@eecs.berkeley.edu)
 * @rating Red
 */
public class TextLine implements TypedData {
    /**
     * The static type associated with this typed data.
     */
    public static final Type type = Type.makeType(TextLine.class);
    
    private double _charHeight;
    private double _charWidth;
	
    public TextLine(double ch, double cw) {
        _charHeight = ch;
        _charWidth = cw;
    }
	
    public Type getType() {
        return TextLine.type;
    }
	
    public double getCharHeight() {
        return _charHeight;
    }
	
    public double getCharWidth() {
        return _charWidth;
    }

    public boolean equals(Object o) {
        if(o instanceof TextLine) {
            TextLine tl = (TextLine)o;
            return (_charHeight == tl.getCharHeight() &&
                    _charWidth == tl.getCharWidth());
        }
        return false;
    }
	
    public String toString() {
        return "TextLine[ charHeight = " + _charHeight + 
		  ", charWidth = " + _charWidth + "]";
    }
}
