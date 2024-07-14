package com.cat2bug.im.service;

import com.cat2bug.common.utils.spring.SpringUtils;
import com.cat2bug.im.domain.IMMessage;
import com.cat2bug.im.service.impl.DefaultMessageTemplateImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-07 01:49
 * @Version: 1.0.0
 */
@Service
public class IMService {
    private final static Logger log = LogManager.getLogger(IMMessage.class);

    private final static ExecutorService es = Executors.newFixedThreadPool(3);

    @Autowired
    private DefaultMessageTemplateImpl defaultMessageTemplateImpl;

    @Autowired
    private List<IIMService> iimServiceList;
    /**
     * 发送文本消息
     * @param projectId     项目ID
     * @param senderId      发送者ID
     * @param recipientIds  接收人ID集合
     * @param content       发送内容
     * @param group         分组
     */
    public <T> void sendMessage(Long projectId, String group, Long senderId, List<Long> recipientIds, String title, T content, IMessageTemplate messageTemplate) {
        iimServiceList.stream().forEach(im->{
            IIMFactoryService factory = SpringUtils.getBean(im.getMessageFactoryName());
            if(factory==null) {
                log.error("Bean 实例 {} 未找到，无法发送IM信息",im.getMessageFactoryName());
                return;
            }
            IMessageTemplate template = messageTemplate==null? defaultMessageTemplateImpl :messageTemplate;
            List<IMMessage> messageList = factory.createMessage(projectId,group,senderId,recipientIds,title,content,template);

            messageList.forEach(m->{
                es.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            im.sendNoticeMessage(m);
                        } catch (Exception e) {
                            log.error(e);
                        }
                    }
                });
            });
        });
    }
}
