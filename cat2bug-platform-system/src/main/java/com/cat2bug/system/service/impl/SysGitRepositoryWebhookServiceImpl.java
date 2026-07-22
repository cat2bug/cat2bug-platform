package com.cat2bug.system.service.impl;

import com.cat2bug.common.constant.HttpStatus;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.config.GitWebhookProperties;
import com.cat2bug.system.domain.SysGitRepository;
import com.cat2bug.system.domain.SysGitRepositoryWebhook;
import com.cat2bug.system.domain.SysGitWebhookDelivery;
import com.cat2bug.system.domain.git.SysGitRepositoryWebhookRequest;
import com.cat2bug.system.domain.git.SysGitRepositoryWebhookSecretReveal;
import com.cat2bug.system.domain.git.SysGitRepositoryWebhookVo;
import com.cat2bug.system.mapper.SysGitRepositoryMapper;
import com.cat2bug.system.mapper.SysGitRepositoryWebhookMapper;
import com.cat2bug.system.mapper.SysGitWebhookDeliveryMapper;
import com.cat2bug.system.service.ISysGitRepositoryWebhookService;
import com.cat2bug.system.service.git.GitRepositoryAccessService;
import com.cat2bug.system.service.git.GitWebhookEnqueueService;
import com.cat2bug.system.service.git.GitWebhookEventTypes;
import com.cat2bug.system.service.git.GitWebhookPayloadBuilder;
import com.cat2bug.system.service.git.GitWebhookSecretCipher;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysGitRepositoryWebhookServiceImpl implements ISysGitRepositoryWebhookService
{
    @Autowired
    private SysGitRepositoryWebhookMapper webhookMapper;

    @Autowired
    private SysGitWebhookDeliveryMapper deliveryMapper;

    @Autowired
    private SysGitRepositoryMapper repositoryMapper;

    @Autowired
    private GitRepositoryAccessService gitRepositoryAccessService;

    @Autowired
    private GitWebhookSecretCipher secretCipher;

    @Autowired
    private GitWebhookEnqueueService enqueueService;

    @Autowired
    private GitWebhookProperties properties;

    @Override
    public List<SysGitRepositoryWebhookVo> listWebhooks(Long repositoryId)
    {
        assertManage(repositoryId);
        requireRepository(repositoryId);
        return webhookMapper.selectByRepositoryId(repositoryId).stream()
                .map(this::toVo)
                .collect(Collectors.toList());
    }

    @Override
    public SysGitRepositoryWebhookVo getWebhook(Long repositoryId, Long webhookId)
    {
        assertManage(repositoryId);
        return toVo(requireWebhook(repositoryId, webhookId));
    }

    @Override
    @Transactional
    public SysGitRepositoryWebhookSecretReveal createWebhook(Long repositoryId, SysGitRepositoryWebhookRequest request)
    {
        assertManage(repositoryId);
        requireRepository(repositoryId);
        validateRequest(request, true);
        String plainSecret = resolvePlainSecret(request.getSecret());
        List<String> events = normalizeEvents(request.getEvents());

        SysGitRepositoryWebhook row = new SysGitRepositoryWebhook();
        row.setRepositoryId(repositoryId);
        row.setUrl(request.getUrl().trim());
        row.setAlias(trimToNull(request.getAlias()));
        row.setSecretCipher(secretCipher.encrypt(plainSecret));
        try
        {
            row.setEvents(GitWebhookPayloadBuilder.serializeSubscriptionEvents(events));
        }
        catch (JsonProcessingException e)
        {
            throw new ServiceException(MessageUtils.message("git.webhook.events-invalid"), HttpStatus.BAD_REQUEST);
        }
        row.setActive(request.getActive() == null || Boolean.TRUE.equals(request.getActive()) ? 1 : 0);
        row.setCreateBy(SecurityUtils.getUsername());
        row.setCreateTime(DateUtils.getNowDate());
        row.setUpdateBy(row.getCreateBy());
        row.setUpdateTime(row.getCreateTime());
        webhookMapper.insert(row);

        SysGitRepositoryWebhookSecretReveal reveal = new SysGitRepositoryWebhookSecretReveal();
        reveal.setWebhook(toVo(row));
        reveal.setSecret(plainSecret);
        return reveal;
    }

    @Override
    @Transactional
    public SysGitRepositoryWebhookVo updateWebhook(Long repositoryId, Long webhookId,
            SysGitRepositoryWebhookRequest request)
    {
        assertManage(repositoryId);
        SysGitRepositoryWebhook existing = requireWebhook(repositoryId, webhookId);
        if (request == null)
        {
            throw badRequest("git.webhook.required");
        }
        if (StringUtils.isNotEmpty(request.getUrl()))
        {
            validateUrl(request.getUrl());
            existing.setUrl(request.getUrl().trim());
        }
        if (request.getAlias() != null)
        {
            existing.setAlias(trimToNull(request.getAlias()));
        }
        if (request.getEvents() != null && !request.getEvents().isEmpty())
        {
            List<String> events = normalizeEvents(request.getEvents());
            try
            {
                existing.setEvents(GitWebhookPayloadBuilder.serializeSubscriptionEvents(events));
            }
            catch (JsonProcessingException e)
            {
                throw new ServiceException(MessageUtils.message("git.webhook.events-invalid"), HttpStatus.BAD_REQUEST);
            }
        }
        if (request.getActive() != null)
        {
            existing.setActive(Boolean.TRUE.equals(request.getActive()) ? 1 : 0);
        }
        existing.setUpdateBy(SecurityUtils.getUsername());
        existing.setUpdateTime(DateUtils.getNowDate());
        webhookMapper.update(existing);
        return toVo(existing);
    }

    @Override
    @Transactional
    public int deleteWebhook(Long repositoryId, Long webhookId)
    {
        assertManage(repositoryId);
        requireWebhook(repositoryId, webhookId);
        return webhookMapper.deleteByRepositoryIdAndWebhookId(repositoryId, webhookId);
    }

    @Override
    @Transactional
    public SysGitRepositoryWebhookSecretReveal regenerateSecret(Long repositoryId, Long webhookId)
    {
        assertManage(repositoryId);
        SysGitRepositoryWebhook existing = requireWebhook(repositoryId, webhookId);
        String plainSecret = GitWebhookSecretCipher.generatePlainSecret();
        existing.setSecretCipher(secretCipher.encrypt(plainSecret));
        existing.setUpdateBy(SecurityUtils.getUsername());
        existing.setUpdateTime(DateUtils.getNowDate());
        webhookMapper.update(existing);

        SysGitRepositoryWebhookSecretReveal reveal = new SysGitRepositoryWebhookSecretReveal();
        reveal.setWebhook(toVo(existing));
        reveal.setSecret(plainSecret);
        return reveal;
    }

    @Override
    public List<SysGitWebhookDelivery> listDeliveries(Long repositoryId, Long webhookId)
    {
        assertManage(repositoryId);
        requireWebhook(repositoryId, webhookId);
        return deliveryMapper.selectRecentByWebhookId(webhookId, properties.getDeliveryKeepCount());
    }

    @Override
    public void sendTestPing(Long repositoryId, Long webhookId)
    {
        assertManage(repositoryId);
        requireWebhook(repositoryId, webhookId);
        enqueueService.enqueuePing(repositoryId, webhookId);
    }

    private SysGitRepositoryWebhookVo toVo(SysGitRepositoryWebhook row)
    {
        SysGitRepositoryWebhookVo vo = new SysGitRepositoryWebhookVo();
        vo.setWebhookId(row.getWebhookId());
        vo.setRepositoryId(row.getRepositoryId());
        vo.setUrl(row.getUrl());
        vo.setAlias(row.getAlias());
        vo.setEvents(GitWebhookPayloadBuilder.parseSubscriptionEvents(row.getEvents()));
        vo.setActive(row.isActiveFlag());
        vo.setSecretConfigured(StringUtils.isNotEmpty(row.getSecretCipher()));
        vo.setCreateTime(row.getCreateTime());
        vo.setUpdateTime(row.getUpdateTime());
        return vo;
    }

    private SysGitRepositoryWebhook requireWebhook(Long repositoryId, Long webhookId)
    {
        SysGitRepositoryWebhook row = webhookMapper.selectByRepositoryIdAndWebhookId(repositoryId, webhookId);
        if (row == null)
        {
            throw new ServiceException(MessageUtils.message("git.webhook.not-found"), HttpStatus.NOT_FOUND);
        }
        return row;
    }

    private SysGitRepository requireRepository(Long repositoryId)
    {
        SysGitRepository repo = repositoryMapper.selectSysGitRepositoryByRepositoryId(repositoryId);
        if (repo == null)
        {
            throw new ServiceException(MessageUtils.message("git.repo.not-found"), HttpStatus.NOT_FOUND);
        }
        return repo;
    }

    private void validateRequest(SysGitRepositoryWebhookRequest request, boolean requireAll)
    {
        if (request == null)
        {
            throw badRequest("git.webhook.required");
        }
        if (requireAll && StringUtils.isEmpty(request.getUrl()))
        {
            throw badRequest("git.webhook.url-required");
        }
        if (StringUtils.isNotEmpty(request.getUrl()))
        {
            validateUrl(request.getUrl());
        }
        if (requireAll && (request.getEvents() == null || request.getEvents().isEmpty()))
        {
            throw badRequest("git.webhook.events-required");
        }
        if (request.getEvents() != null)
        {
            normalizeEvents(request.getEvents());
        }
    }

    private static List<String> normalizeEvents(List<String> events)
    {
        List<String> normalized = new ArrayList<>();
        for (String event : events)
        {
            if (StringUtils.isEmpty(event))
            {
                continue;
            }
            String value = event.trim();
            if (!GitWebhookEventTypes.ALL_SUBSCRIPTIONS.contains(value))
            {
                throw badRequest("git.webhook.events-invalid");
            }
            if (!normalized.contains(value))
            {
                normalized.add(value);
            }
        }
        if (normalized.isEmpty())
        {
            throw badRequest("git.webhook.events-required");
        }
        return normalized;
    }

    private static void validateUrl(String url)
    {
        try
        {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)))
            {
                throw badRequest("git.webhook.url-invalid");
            }
        }
        catch (IllegalArgumentException e)
        {
            throw badRequest("git.webhook.url-invalid");
        }
    }

    private static String resolvePlainSecret(String secret)
    {
        if (StringUtils.isEmpty(secret))
        {
            return GitWebhookSecretCipher.generatePlainSecret();
        }
        String trimmed = secret.trim();
        if (trimmed.length() < 8)
        {
            throw badRequest("git.webhook.secret-too-short");
        }
        return trimmed;
    }

    private static String trimToNull(String value)
    {
        if (value == null)
        {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void assertManage(Long repositoryId)
    {
        if (!gitRepositoryAccessService.canManage(SecurityUtils.getUserId(), repositoryId))
        {
            throw new ServiceException(MessageUtils.message("no-permission"), HttpStatus.FORBIDDEN);
        }
    }

    private static ServiceException badRequest(String messageKey)
    {
        return new ServiceException(MessageUtils.message(messageKey), HttpStatus.BAD_REQUEST);
    }
}
