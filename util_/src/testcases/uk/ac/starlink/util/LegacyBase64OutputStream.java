/* 
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 * 
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 * 
 * The Original Code is the Netscape Security Services for Java.
 * 
 * The Initial Developer of the Original Code is Netscape
 * Communications Corporation.  Portions created by Netscape are 
 * Copyright (C) 1998-2000 Netscape Communications Corporation.  All
 * Rights Reserved.
 * 
 * Contributor(s):
 * 
 * Alternatively, the contents of this file may be used under the
 * terms of the GNU General Public License Version 2 or later (the
 * "GPL"), in which case the provisions of the GPL are applicable 
 * instead of those above.  If you wish to allow use of your 
 * version of this file only under the terms of the GPL and not to
 * allow others to use your version of this file under the MPL,
 * indicate your decision by deleting the provisions above and
 * replace them with the notice and other provisions required by
 * the GPL.  If you do not delete the provisions above, a recipient
 * may use your version of this file under either the MPL or the
 * GPL.
 */

/*
 * We would like to eventually use Sun's base64 encoder, but it is not
 * public yet.  It is in the sun.misc package in JDK1.2.
 */
package uk.ac.starlink.util;

import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * An output stream filter that takes arbitrary bytes and outputs their
 * base64 encoding.  Call {@link #endBase64} {@link #close}
 * to write out the final padding.
 * The class also automatically puts line breaks in the output stream.
 * <p>
 * Minor modifications made for use in Starlink software:
 * constructors now take OutputStream not PrintStream, assertion 
 * mechanism changed.
 *
 * @author  Mozilla project
 * @author  Mark Taylor (Starlink)
 */
public class LegacyBase64OutputStream extends FilterOutputStream {

        static byte[] endLine;
        static {
            try {
                endLine = System.getProperty( "line.separator" ).getBytes();
            }
            catch ( SecurityException e ) {
                endLine = new byte[] { (byte) '\n' };
            }
        }

	/**
	 * Create a stream that does not insert line breaks.  To have line
	 * breaks, use the other constructor.
	 */
	public LegacyBase64OutputStream(OutputStream out) {
		super(out);
		charsOnLine = 0;
		inputCount = 0;
		input = new byte[3];
		doLineBreaks = false;
	}

	/**
	 * @param quadsPerLine Number of 4-character blocks to write before
	 * 	outputting a line break.  For example, for 76-characters per line,
	 *	pass in 76/4 = 19.
	 */
	public LegacyBase64OutputStream(OutputStream out, int quadsPerLine) {
		this(out);
		doLineBreaks = true;
		assert quadsPerLine>0 : "quadsPerLine must be > 0";
		charsPerLine = quadsPerLine*4;
	}
		

	public void write(int oneByte) throws IOException {
		input[inputCount++] = (byte)(0xff & oneByte);
		if(inputCount==3) {
			outputOneAtom();
			if(doLineBreaks) {
				if(charsOnLine==charsPerLine) {
                                        out.write(endLine);
					charsOnLine=0;
				}
				assert charsOnLine < charsPerLine;
			}
		}
		assert inputCount < 3;
	}

	/**
	 * This invokes {@link #endBase64} and 
         * closes the next stream downstream.
	 */
	public void close() throws IOException {
		endBase64();
		out.close();
	}

	/**
	 * Calling this will put the ending padding on the base64 stream,
	 * so don't call it until you have no data left.  The class does no
	 * unnecessary buffering, so you probably shouldn't call it at all.
	 */
	public void endBase64() throws IOException {
		if(inputCount > 0) {
			outputOneAtom();
		}
		if(doLineBreaks) {
			out.write(endLine);
			charsOnLine = 0;
		}
	}

	public void write(byte[] buffer) throws IOException {
		write(buffer, 0, buffer.length);
	}

	public void write(byte[] buffer, int offset, int count) throws IOException {
		int i;
		int byteCount;

		for(i=offset, byteCount=0; byteCount < count; i++, byteCount++) {
			write(buffer[i]);
		}
	}

    private final static char encoding[] = {
    //   0   1   2   3   4   5   6   7
        'A','B','C','D','E','F','G','H', // 0
        'I','J','K','L','M','N','O','P', // 1
        'Q','R','S','T','U','V','W','X', // 2
        'Y','Z','a','b','c','d','e','f', // 3
        'g','h','i','j','k','l','m','n', // 4
        'o','p','q','r','s','t','u','v', // 5
        'w','x','y','z','0','1','2','3', // 6
        '4','5','6','7','8','9','+','/'  // 7
    };

	/**
	 * Output 3 bytes of input as 4 bytes of base-64 encoded output.
	 * If there are fewer than 3 bytes available, the output will be
	 * padded according to the spec.  Padding should only happen at the end
	 * of the stream.
	 */
	private void outputOneAtom() throws IOException {
		byte[] output = new byte[4];

		if(inputCount==0) {
			return;
		}

		// If we have fewer than 3 bytes, pad the rest with zeroes
		if(inputCount<3) {
			input[2]=0;
			if(inputCount<2) {
				input[1]=0;
			}
		}

		// Do a base64 encoding
		output[0] = (byte) encoding[ (input[0]>>>2) & 0x3f ];
		output[1] = (byte) encoding[ (input[0]<<4)&0x30 | (input[1]>>>4)&0xf ];
		output[2] = (byte) encoding[ (input[1]<<2)&0x3c | (input[2]>>>6)&0x3 ];
		output[3] = (byte) encoding[ input[2]&0x3f ];

		// add padding
		if(inputCount<3) {
			output[3] = (byte)'=';
			if(inputCount<2) {
				output[2] = (byte)'=';
			}
		}

		out.write(output);
		inputCount=0;
		if(doLineBreaks) {
			charsOnLine += 4;
		}
	}

	//////////////////////////////////////////////////////////////////////
	// Private data
	//////////////////////////////////////////////////////////////////////
	private byte[] input;	// buffered input, max 3 bytes (after 3 we write
							// them out base64-encoded)
	private short inputCount;	// number of bytes in input buffer
	private short charsOnLine;  // number of bytes on the current line
	private int charsPerLine;  // maximum characters per line
	private static final int DEFAULT_QUADS_PER_LINE=16; //64 chars
	private boolean doLineBreaks;
}
