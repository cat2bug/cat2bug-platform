package com.cat2bug.system.service.im;

import com.cat2bug.common.util.StringUtils;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Optional;

/**
 * IM 邮件渠道。SMTP 主机、账号、密码、发件人全部来自运行期 {@code quarkus.mailer.*}，
 * 只有 {@code cat2bug.mail.enabled=true} 且主机、发件人都非空时才发送。
 */
@ApplicationScoped
public class ImMailSender {

    private static final Logger LOG = Logger.getLogger(ImMailSender.class);

    @Inject
    Mailer mailer;

    @ConfigProperty(name = "cat2bug.mail.enabled", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "quarkus.mailer.host")
    Optional<String> host;

    @ConfigProperty(name = "quarkus.mailer.from")
    Optional<String> from;

    void onStart(@Observes StartupEvent event) {
        if (!isConfigured()) {
            LOG.info("邮件未配置（cat2bug.mail.enabled / quarkus.mailer.host / quarkus.mailer.from），IM 邮件渠道将被跳过");
        }
    }

    public boolean isConfigured() {
        return enabled && host.filter(StringUtils::isNotBlank).isPresent()
                && from.filter(StringUtils::isNotBlank).isPresent();
    }

    /**
     * 发送 HTML 邮件；未配置时直接返回 {@code false}，不抛异常。
     */
    public boolean send(String to, String subject, String html) {
        if (!isConfigured() || StringUtils.isBlank(to)) {
            return false;
        }
        mailer.send(Mail.withHtml(to.trim(), subject, html).setFrom(from.get()));
        return true;
    }
}
