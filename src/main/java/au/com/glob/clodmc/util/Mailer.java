package au.com.glob.clodmc.util;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import org.jspecify.annotations.NullMarked;

/** email helpers */
@NullMarked
public class Mailer {
  private static final String ADMIN_ADDR = "clod@glob.au";
  private static final String HOSTNAME = "in1-smtp.messagingengine.com";
  private static final String SENDER_NAME = "Clod Minecraft Server";
  private static final String SENDER_ADDR = "clod@glob.au";

  private Mailer() {}

  public static void emailAdmin(String subject) {
    emailAdmin(subject, subject);
  }

  public static void emailAdmin(String subject, String body) {
    Schedule.asynchronously(
        () -> {
          try {
            send(ADMIN_ADDR, subject, body);
          } catch (MailerException e) {
            Logger.warning(e.getMessage());
          }
        });
  }

  public static void send(String recipient, String subject, String body) throws MailerException {
    try {
      try (SMTPClient smtp = new SMTPClient(HOSTNAME)) {
        smtp.waitFor("220 ");
        smtp.sendAndWait("HELO glob.au", "250 ");
        smtp.sendAndWait("MAIL FROM: " + SENDER_ADDR, "250 ");
        smtp.sendAndWait("RCPT TO: " + recipient, "250 ");
        smtp.sendAndWait("DATA", "354 ");
        smtp.sendLine("Date: " + TimeUtil.utcNow().format(DateTimeFormatter.RFC_1123_DATE_TIME));
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
      throw new MailerException(e.getMessage());
    }
  }
}
