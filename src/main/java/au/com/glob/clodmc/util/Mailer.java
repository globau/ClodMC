package au.com.glob.clodmc.util;

import au.com.glob.clodmc.ClodMC;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public class Mailer {
  private Mailer() {}

  public static class MailerError extends Exception {
    public MailerError(@NotNull String message) {
      super(message);
    }
  }

  public static void emailAdmin(@NotNull String subject) throws MailerError {
    emailAdmin(subject, subject);
  }

  public static void emailAdmin(@NotNull String subject, @NotNull String body) throws MailerError {
    String addr = ClodMC.instance.getConfig().getString("mailer.admin-addr");
    if (addr != null) {
      send(addr, subject, body);
    }
  }

  public static void send(@NotNull String recipient, @NotNull String subject, @NotNull String body)
      throws MailerError {
    Config config = ClodMC.instance.getConfig();
    String hostname = config.getString("mailer.hostname");
    String senderAddr = config.getString("mailer.sender-addr");
    String senderName = config.getString("mailer.sender-name");

    try {
      try (SMTP smtp = new SMTP(Objects.requireNonNull(hostname))) {
        smtp.waitFor("220 ");
        smtp.sendAndWait("HELO glob.au", "250 ");
        smtp.sendAndWait("MAIL FROM: " + senderAddr, "250 ");
        smtp.sendAndWait("RCPT TO: " + recipient, "250 ");
        smtp.sendAndWait("DATA", "354 ");
        smtp.sendLine("Date: " + smtp.currentDate());
        smtp.sendLine("From: " + senderName + " <" + senderAddr + ">");
        smtp.sendLine("To: " + recipient);
        smtp.sendLine("Subject: " + subject);
        smtp.sendLine("");
        smtp.sendLine(body);
        smtp.sendLine("");
        smtp.sendAndWait(".", "250 ");
        smtp.sendAndWait("QUIT", "221 ");
      }
    } catch (IOException e) {
      throw new MailerError(e.getMessage());
    }
  }

  private static class SMTP implements Closeable {
    private final @NotNull DateFormat df = DateFormat.getDateInstance(DateFormat.FULL, Locale.US);
    private final @NotNull Socket socket;
    private final @NotNull BufferedReader inStream;
    private final @NotNull DataOutputStream outStream;

    SMTP(@NotNull String hostname) throws IOException {
      this.socket = new Socket(hostname, 25);
      this.socket.setSoTimeout(5000);
      this.inStream = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
      this.outStream = new DataOutputStream(this.socket.getOutputStream());
    }

    public @NotNull String currentDate() {
      return this.df.format(new Date());
    }

    public void sendLine(@NotNull String line) throws IOException {
      this.outStream.writeBytes(line + "\r\n");
    }

    public @NotNull String readLine() throws IOException {
      return this.inStream.readLine();
    }

    public void waitFor(@NotNull String prefix) throws IOException {
      while (true) {
        String line = this.readLine();
        if (line.startsWith(prefix)) {
          return;
        }
      }
    }

    public void sendAndWait(@NotNull String line, @NotNull String prefix) throws IOException {
      this.sendLine(line);
      this.waitFor(prefix);
    }

    @Override
    public void close() throws IOException {
      this.socket.close();
    }
  }
}
