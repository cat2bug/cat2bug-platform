package com.cat2bug.common.domain;

import com.cat2bug.common.constant.DefectConstants;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.function.Function;

/**
 * 缺陷类型 / 状态在 JSON 中与 Spring 线对齐：库里存序号，接口上类型输出名字，输入名字与数字都接受。
 */
public final class DefectEnumJson {

    private DefectEnumJson() {
    }

    public static class TypeSerializer extends JsonSerializer<Integer> {
        @Override
        public void serialize(Integer value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            String name = DefectConstants.typeName(value);
            if (name != null) {
                gen.writeString(name);
            } else {
                gen.writeNumber(value);
            }
        }
    }

    public static class TypeDeserializer extends JsonDeserializer<Integer> {
        @Override
        public Integer deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return parse(p, ctxt, DefectConstants::parseType);
        }
    }

    public static class StateDeserializer extends JsonDeserializer<Integer> {
        @Override
        public Integer deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return parse(p, ctxt, DefectConstants::parseState);
        }
    }

    private static Integer parse(JsonParser p, DeserializationContext ctxt, Function<String, Integer> parser)
            throws IOException {
        JsonToken token = p.currentToken();
        if (token == JsonToken.VALUE_NULL) {
            return null;
        }
        if (token != JsonToken.VALUE_STRING && token != JsonToken.VALUE_NUMBER_INT) {
            return (Integer) ctxt.handleUnexpectedToken(Integer.class, p);
        }
        String text = p.getText();
        try {
            return parser.apply(text);
        } catch (IllegalArgumentException e) {
            throw ctxt.weirdStringException(text, Integer.class, e.getMessage());
        }
    }
}
