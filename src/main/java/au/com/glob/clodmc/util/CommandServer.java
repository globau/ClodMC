package au.com.glob.clodmc.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class CommandServer implements Closeable {
  private final Socket socket;
  private final BufferedReader inStream;
  private final DataOutputStream outStream;

  CommandServer(String hostname) throws IOException {
    this.socket = new Socket(hostname, 25);
    this.socket.setSoTimeout(5000);
    this.inStream =
        new BufferedReader(
            new InputStreamReader(this.socket.getInputStream(), StandardCharsets.UTF_8));
    this.outStream = new DataOutputStream(this.socket.getOutputStream());
  }

  public void sendLine(String line) throws IOException {
    this.outStream.writeBytes("%s\r\n".formatted(line));
  }

  public String readLine() throws IOException {
    return this.inStream.readLine();
  }

  public void waitFor(String prefix) throws IOException {
    while (true) {
      String line = this.readLine();
      if (line.startsWith(prefix)) {
        return;
      }
    }
  }

  public void sendAndWait(String line, String prefix) throws IOException {
    this.sendLine(line);
    this.waitFor(prefix);
  }

  @Override
  public void close() throws IOException {
    this.socket.close();
  }
}
