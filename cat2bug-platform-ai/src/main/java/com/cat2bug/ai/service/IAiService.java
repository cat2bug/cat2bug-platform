package com.cat2bug.ai.service;

import java.util.List;

public interface IAiService {
    /**
     * 问答
     * @param moduleName    模型名称
     * @param prompt        问题
     * @param stream        是否通过流数据返回
     * @param cls           返回数据类型
     * @return              回答内容
     * @param <T>           回答的内容类型
     */
    public <T> T generate(String moduleName, String prompt, boolean stream, Class<T> cls);

    /**
     * 显示模型信息
     * @param moduleName    模型名称
     * @param cls           返回类型
     * @return              模型信息对象
     */
    public <T> T getModuleInfo(String moduleName, Class<T> cls);

    /**
     * 获取模型信息
     * @param cls           返回类型
     * @return              模型信息对象
     */
    public <T> List<T> getModuleList(Class<T> cls);

    /**
     * 拉取模型
     * @param moduleName    模型名称
     * @param stream        是否通过流数据返回
     * @return              如果stream=false，返回推送结果，如果stream=true，返回null
     */
    public String pullModule(String moduleName, boolean stream);

    /**
     * 删除模型
     * @param moduleName    模型名称
     * @return              true删除成功
     */
    public boolean removeModule(String moduleName);
}
