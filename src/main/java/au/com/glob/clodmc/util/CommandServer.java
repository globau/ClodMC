package au.com.glob.clodmc.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** tcp socket wrapper for command-response protocols like smtp */
@NullMarked
public class CommandServer implements Closeable {
  private static final int SOCKET_TIMEOUT_MS = 5000;

  private final Socket socket;
  private final BufferedReader inStream;
  private final DataOutputStream outStream;

  CommandServer(final String hostname, final int port) throws IOException {
    this.socket = new Socket(hostname, port);
    this.socket.setSoTimeout(SOCKET_TIMEOUT_MS);
    this.inStream =
        new BufferedReader(
            new InputStreamReader(this.socket.getInputStream(), StandardCharsets.UTF_8));
    this.outStream = new DataOutputStream(this.socket.getOutputStream());
  }

  // send a line of text with crlf termination
  public void sendLine(final String line) throws IOException {
    this.outStream.writeBytes("%s\r\n".formatted(line));
  }

  // read a line of text from the server
  public @Nullable String readLine() throws IOException {
    return this.inStream.readLine();
  }

  // read and discard lines until one starts with the given prefix
  public void waitFor(final String prefix) throws IOException {
    while (true) {
      final String line = this.readLine();
      if (line == null || line.startsWith(prefix)) {
        return;
      }
    }
  }

  // send a command and wait for a response with the given prefix
  public void sendAndWait(final String line, final String prefix) throws IOException {
    this.sendLine(line);
    this.waitFor(prefix);
  }

  @Override
  public void close() throws IOException {
    this.inStream.close();
    this.outStream.close();
    this.socket.close();
  }
}
