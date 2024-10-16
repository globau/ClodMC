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
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/** email helpers */
public class Mailer {
  private static final @NotNull String ADMIN_ADDR = "clod@glob.au";
  private static final @NotNull String HOSTNAME = "in1-smtp.messagingengine.com";
  private static final @NotNull String SENDER_NAME = "Clod Minecraft Server";
  private static final @NotNull String SENDER_ADDR = "clod@glob.au";

  private Mailer() {}

  public static class MailerError extends Exception {
    public MailerError(@NotNull String message) {
      super(message);
    }
  }

  public static void emailAdmin(@NotNull String subject) {
    emailAdmin(subject, subject);
  }

  public static void emailAdmin(@NotNull String subject, @NotNull String body) {
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            ClodMC.instance,
            () -> {
              try {
                send(ADMIN_ADDR, subject, body);
              } catch (MailerError e) {
                Logger.warning(e.getMessage());
              }
            });
  }

  public static void send(@NotNull String recipient, @NotNull String subject, @NotNull String body)
      throws MailerError {

    try {
      try (SMTP smtp = new SMTP()) {
        smtp.waitFor("220 ");
        smtp.sendAndWait("HELO glob.au", "250 ");
        smtp.sendAndWait("MAIL FROM: " + SENDER_ADDR, "250 ");
        smtp.sendAndWait("RCPT TO: " + recipient, "250 ");
        smtp.sendAndWait("DATA", "354 ");
        smtp.sendLine("Date: " + smtp.currentDate());
        smtp.sendLine("From: " + SENDER_NAME + " <" + SENDER_ADDR + ">");
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

    SMTP() throws IOException {
      this.socket = new Socket(HOSTNAME, 25);
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
