package uk.ac.starlink.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class is equivalent to {@link java.io.PipedInputStream}. In the
 * interface it only adds a constructor which allows for specifying the buffer
 * size. Its implementation, however, is much simpler and a lot more efficient
 * than its equivalent. It doesn't rely on polling. Instead it uses proper
 * synchronization with its counterpart.
 *
 * Multiple readers can read from this stream concurrently. The block asked for
 * by a reader is delivered completely, or until the end of the stream if less
 * is available. Other readers can't come in between.
 * <p>
 * This class was pinched directly from the improved suggestion for
 * Sun's PipedOutputStream in Sun's bug report bug #4404700
 *
 * @author WD
 * @see <http://developer.java.sun.com/developer/bugParade/bugs/4404700.html>
 * @see FastPipedOutputStream
 */

@SuppressWarnings("cast")
class FastPipedInputStream extends InputStream

{

  byte[]                buffer;
  boolean               closed = false;
  int                   readLaps = 0;
  int                   readPosition = 0;
  FastPipedOutputStream     source;
  int                   writeLaps = 0;
  int                   writePosition = 0;



  /**
   * Creates an unconnected FastPipedInputStream with a default buffer size.
   */

  FastPipedInputStream() throws IOException
  {
    this(null);
  }



  /**
   * Creates a FastPipedInputStream with a default buffer size and connects it to
   * <code>source</code>.
   * @exception IOException It was already connected.
   */

  public
  FastPipedInputStream(FastPipedOutputStream source) throws IOException
  {
    this(source, 0x10000);
  }



  /**
   * Creates a PipedInputStream with buffer size <code>bufferSize</code> and
   * connects it to <code>source</code>.
   * @exception IOException It was already connected.
   */

  public
  FastPipedInputStream(FastPipedOutputStream source, int bufferSize) throws IOException
  {
    if (source != null)
    {
      connect(source);
    }

    buffer = new byte[bufferSize];
  }



  public int
  available() throws IOException
  {
    /* The circular buffer is inspected to see where the reader and the writer
     * are located.
     */

    return
      writePosition > readPosition /* The writer is in the same lap. */ ?
        writePosition - readPosition :
        (
          writePosition < readPosition /* The writer is in the next lap. */ ?
            buffer.length - readPosition + 1 + writePosition :
            /* The writer is at the same position or a complete lap ahead. */
            (writeLaps > readLaps ? buffer.length : 0)
        );
  }



  /**
   * @exception IOException The pipe is not connected.
   */

  public void
  close() throws IOException
  {
    if (source == null)
    {
      throw new IOException("Unconnected pipe");
    }

    synchronized (buffer)
    {
      closed = true;
      // Release any pending writers.
      buffer.notifyAll();
    }
  }



  /**
   * @exception IOException The pipe is already connected.
   */

  public void
  connect(FastPipedOutputStream source) throws IOException
  {
    if (this.source != null)
    {
      throw new IOException("Pipe already connected");
    }

    this.source = source;
    source.sink = this;
  }



  public void
  mark(int readLimit)
  {
    return;
  }



  public boolean
  markSupported()
  {
    return false;
  }



  public int
  read() throws IOException
  {
    byte[]      b = new byte[1];
    return read(b, 0, 1) == -1 ? -1 : ((int) 0xff & b[0]);
  }



  public int
  read(byte[] b) throws IOException
  {
    return read(b, 0, b.length);
  }



  /**
   * @exception IOException The pipe is not connected.
   */

  public int
  read(byte[] b, int off, int len) throws IOException
  {
    if (source == null)
    {
      throw new IOException("Unconnected pipe");
    }

    synchronized (buffer)
    {
      if (writePosition == readPosition && writeLaps == readLaps)
      {
        if (closed)
        {
          return -1;
        }

        // Wait for any writer to put something in the circular buffer.

        try
        {
          buffer.wait();
        }

        catch (InterruptedException e)
        {
          throw new IOException(e.getMessage());
        }

        // Try again.

        return read(b, off, len);
      }

      // Don't read more than the capacity indicated by len or what's available
      // in the circular buffer.

      int       amount =
        Math.min
        (
          len,
          (writePosition > readPosition ? writePosition : buffer.length) -
            readPosition
        );

      System.arraycopy(buffer, readPosition, b, off, amount);
      readPosition += amount;

      if (readPosition == buffer.length) // A lap was completed, so go back.
      {
        readPosition = 0;
        ++readLaps;
      }

      // The buffer is only released when the complete desired block was
      // obtained.

      if (amount < len)
      {
        int     second = read(b, off + amount, len - amount);

        return second == -1 ? amount : amount + second;
      }
      else
      {
        buffer.notifyAll();
      }

      return amount;
    }
  }

}
