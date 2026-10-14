package com.cat2bug.admin.util;

import com.cat2bug.common.ai.AIClass;
import com.cat2bug.common.ai.AIField;
import com.cat2bug.common.util.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 将带 {@link AIClass}/{@link AIField} 的 VO 转为 Ollama JSON 提示词模板（对齐 Spring {@code PromptUtils}）。
 */
public final class PromptUtils {

    private static final String EMPTY_STRING = "";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PromptUtils() {
    }

    public static <T> String objectToPrompt(Class<T> cls)
            throws ReflectiveOperationException, JsonProcessingException {
        return String.format("JSON格式为:\n%s\nJSON中的属性介绍如下:\n%s",
                MAPPER.writeValueAsString(obj2Map(cls)),
                obj2Introduces(cls).stream().filter(StringUtils::isNotBlank).collect(Collectors.joining("\n")));
    }

    private static List<String> obj2Introduces(Class<?> cls) {
        List<String> introduces = new ArrayList<>();
        if (cls.getAnnotation(AIClass.class) == null) {
            return introduces;
        }
        for (Field field : cls.getDeclaredFields()) {
            introduces.add(field2Introduce(field));
            if (field.getType() == List.class || field.getType() == Set.class) {
                ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
                Class<?> elementType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
                introduces.addAll(obj2Introduces(elementType));
            }
        }
        return introduces;
    }

    private static String field2Introduce(Field field) {
        AIField aiField = field.getAnnotation(AIField.class);
        if (aiField == null) {
            return EMPTY_STRING;
        }
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotBlank(aiField.explain())) {
            sb.append(String.format("说明:%s;", aiField.explain()));
        }
        if (aiField.isRequired()) {
            sb.append("它是必填项;");
        }
        if (StringUtils.isNotBlank(aiField.minValue())) {
            sb.append(String.format("最小值=%s;", aiField.minValue()));
        }
        if (StringUtils.isNotBlank(aiField.maxValue())) {
            sb.append(String.format("最大值=%s;", aiField.maxValue()));
        }
        if (sb.length() > 0) {
            return String.format("(属性名:%s;%s)", field.getName(), sb);
        }
        return EMPTY_STRING;
    }

    private static <T> Object obj2Map(Class<T> cls) throws ReflectiveOperationException {
        T instance = cls.getDeclaredConstructor().newInstance();
        Map<String, Object> src = MAPPER.convertValue(instance, new TypeReference<Map<String, Object>>() {
        });
        if (cls.getAnnotation(AIClass.class) == null) {
            return src;
        }
        Map<String, Object> obj = new HashMap<>();
        for (Field field : cls.getDeclaredFields()) {
            AIField aiField = field.getAnnotation(AIField.class);
            if (aiField == null) {
                continue;
            }
            if (isBaseType(field.getType())) {
                obj.put(field.getName(), src.get(field.getName()));
            } else if (field.getType() == List.class) {
                ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
                Class<?> elementType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
                obj.put(field.getName(), List.of(obj2Map(elementType)));
            } else if (field.getType() == Set.class) {
                ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
                Class<?> elementType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
                obj.put(field.getName(), new HashSet<>(List.of(obj2Map(elementType))));
            } else {
                obj.put(field.getName(), src.get(field.getName()));
            }
        }
        return obj;
    }

    private static boolean isBaseType(Class<?> cls) {
        return cls.isPrimitive()
                || cls == Map.class
                || cls == String.class
                || cls == Integer.class
                || cls == Long.class
                || cls == Number.class
                || cls == Byte.class
                || cls == Character.class
                || cls == Boolean.class
                || cls == Double.class
                || cls == Short.class
                || cls == Float.class;
    }
}
