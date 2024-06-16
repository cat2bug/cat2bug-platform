package com.cat2bug.ai.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.cat2bug.ai.annotaion.AIClass;
import com.cat2bug.ai.annotaion.AIField;
import com.cat2bug.common.utils.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-15 09:59
 * @Version: 1.0.0
 */
public class PromptUtils {
    /** 空字符 */
    private final static String EMPTY_STRING = "";

    public static <T> String objectToPrompt(Class<T> cls) throws InstantiationException, IllegalAccessException {
        return String.format("JSON格式为:\n%s\nJSON中的属性介绍如下:\n%s",
                JSON.toJSONString(PromptUtils.obj2Map(cls), SerializerFeature.WriteMapNullValue),
                PromptUtils.obj2Introduces(cls).stream().filter(t->StringUtils.isNotBlank(t)).collect(Collectors.joining("\n")));
    }

    /**
     * 对象转介绍
     * @param cls
     * @return
     */
    protected static List<String> obj2Introduces(Class cls) {
        List<String> introduces = new ArrayList<>();
        if(cls.getAnnotation(AIClass.class)==null) {
            return introduces;
        }
        Field[] fields = cls.getDeclaredFields();
        for(int i=0;i<fields.length;i++) {
            Field f = fields[i];
            introduces.add(field2Introduce(f));
            if(f.getType() == List.class) {
                ParameterizedType parameterizedType = (ParameterizedType) f.getGenericType();
                Class c = (Class)parameterizedType.getActualTypeArguments()[0];
                introduces.addAll(obj2Introduces(c));
            } else if (f.getType() == Set.class) {
                ParameterizedType parameterizedType = (ParameterizedType) f.getGenericType();
                Class c = (Class)parameterizedType.getActualTypeArguments()[0];
                introduces.addAll(obj2Introduces(c));
            }
        }
        return introduces;
    }

    /**
     * 字段转介绍
     * @param field
     * @return
     */
    protected static String field2Introduce(Field field) {
        AIField aiField = field.getAnnotation(AIField.class);
        if (aiField == null) return EMPTY_STRING;
        StringBuffer sb = new StringBuffer();
        if(StringUtils.isNotBlank(aiField.explain())) {
            try {
                sb.append(String.format("说明:%s;",aiField.explain()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(aiField.isRequired()) {
            sb.append("它是必填项;");
        }
        if(StringUtils.isNotBlank(aiField.minValue())) {
            try {
                sb.append(String.format("最小值=%s;",aiField.minValue()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(StringUtils.isNotBlank(aiField.maxValue())) {
            try {
                sb.append(String.format("最大值=%s;",aiField.maxValue()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(sb.length()>0) {
            return String.format("(属性名:%s;%s)",field.getName(),sb.toString());
        }
        return EMPTY_STRING;
    }

    /**
     * 对象转Map
     * @param cls   对象类
     * @return      Map
     * @param <T>   对象类型
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    protected static <T> Object obj2Map(Class<T> cls) throws InstantiationException, IllegalAccessException {
        Object clsInstance = cls.newInstance();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> src = mapper.convertValue(clsInstance, new TypeReference<Map<String, Object>>() {});
        // 如果类没有AIClass注解，返回全部属性
        if(cls.getAnnotation(AIClass.class)==null) {
            return src;
        }
        // 如果类有AIClass注解，只返回标记AIField的属性
        Map<String, Object> obj = new HashMap<>(); // 需要返回的数据
        Field[] fields = cls.getDeclaredFields();
        for(int i=0;i<fields.length;i++){
            Field f = fields[i];
            AIField aiField = f.getAnnotation(AIField.class);
            if(aiField ==null) continue;

            if(isBaseType(f.getType())){
                obj.put(f.getName(),src.get(f.getName()));
            } else if (f.getType() == List.class) {
                ParameterizedType parameterizedType = (ParameterizedType) f.getGenericType();
                Class c = (Class)parameterizedType.getActualTypeArguments()[0];
                obj.put(f.getName(), Arrays.asList(obj2Map(c)));
            } else if (f.getType() == Set.class) {
                ParameterizedType parameterizedType = (ParameterizedType) f.getGenericType();
                Class c = (Class)parameterizedType.getActualTypeArguments()[0];
                obj.put(f.getName(), Sets.newHashSet(obj2Map(c)));
            } else if (f.getType() == Object.class) {
                obj.put(f.getName(),obj2Map(f.getType()));
            } else {
                obj.put(f.getName(),src.get(f.getName()));
            }
        }
        return obj;
    }

    /**
     * 是否是基础类型
     * @param cls   类型
     * @return      true为基础类型
     * @param <T>   类型
     */
    protected static <T> boolean isBaseType(Class<T> cls) {
        return cls.isPrimitive() ||
                cls == Map.class    ||
                cls == String.class    ||
                cls == Integer.class    ||
                cls == Long.class    ||
                cls == Number.class    ||
                cls == Byte.class    ||
                cls == Character.class    ||
                cls == Boolean.class    ||
                cls == Double.class     ||
                cls == Short.class      ||
                cls == Float.class;
    }
}
