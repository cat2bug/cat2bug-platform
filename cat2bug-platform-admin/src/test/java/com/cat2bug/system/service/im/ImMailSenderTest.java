package com.cat2bug.system.service.im;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImMailSenderTest {

    private final List<Mail> sent = new ArrayList<>();
    private final Mailer recordingMailer = mails -> sent.addAll(List.of(mails));

    @Test
    void skipsWithoutErrorWhenDisabled() {
        ImMailSender sender = sender(false, "smtp.example.com", "noreply@example.com");

        assertFalse(sender.isConfigured());
        assertDoesNotThrow(() -> assertFalse(sender.send("a@example.com", "标题", "<p>x</p>")));
        assertTrue(sent.isEmpty());
    }

    @Test
    void skipsWhenFromMissing() {
        ImMailSender sender = sender(true, "smtp.example.com", null);

        assertFalse(sender.isConfigured());
        assertFalse(sender.send("a@example.com", "标题", "<p>x</p>"));
        assertTrue(sent.isEmpty());
    }

    @Test
    void sendsHtmlMailWhenConfigured() {
        ImMailSender sender = sender(true, "smtp.example.com", "noreply@example.com");

        assertTrue(sender.send(" a@example.com ", "[处理中]缺陷:#3 登录失败", "<p>缺陷通知</p>"));

        assertEquals(1, sent.size());
        Mail mail = sent.get(0);
        assertEquals(List.of("a@example.com"), mail.getTo());
        assertEquals("[处理中]缺陷:#3 登录失败", mail.getSubject());
        assertEquals("<p>缺陷通知</p>", mail.getHtml());
        assertEquals("noreply@example.com", mail.getFrom());
    }

    @Test
    void blankRecipientIsSkipped() {
        ImMailSender sender = sender(true, "smtp.example.com", "noreply@example.com");

        assertFalse(sender.send(" ", "t", "h"));
        assertTrue(sent.isEmpty());
    }

    private ImMailSender sender(boolean enabled, String host, String from) {
        ImMailSender sender = new ImMailSender();
        sender.mailer = recordingMailer;
        sender.enabled = enabled;
        sender.host = Optional.ofNullable(host);
        sender.from = Optional.ofNullable(from);
        return sender;
    }
}
