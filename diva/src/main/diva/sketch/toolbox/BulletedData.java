

package diva.sketch.toolbox;
import diva.sketch.recognition.Type;
import diva.sketch.recognition.TypedData;

/**
 * Native class that defines a collection of TextLine's. It also defines 
 * the avg x-height and character width. 
 *
 * @author Niraj Shah  (niraj@eecs.berkeley.edu)
 * @rating Red
 */
public class BulletedData implements TypedData {
    /**
     * The static type associated with this typed data.
     */
    public static final Type type = Type.makeType(BulletedData.class);
    
    private double _charHeight;
    private double _charWidth;
	
    public BulletedData(double ch, double cw) {
        _charHeight = ch;
        _charWidth = cw;
    }
	
    public Type getType() {
        return BulletedData.type;
    }
	
    public double getCharHeight() {
        return _charHeight;
    }
	
    public double getCharWidth() {
        return _charWidth;
    }
	
    public String toString() {
        return "BulletedData[ charHeight = " + _charHeight + 
		  ", charWidth = " + _charWidth + "]";
    }
}
