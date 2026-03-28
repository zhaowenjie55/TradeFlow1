package com.globalvibe.arbitrage.common.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

@Component
public class JdbcJsonSupport {

    private final ObjectMapper objectMapper;

    public JdbcJsonSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PGobject toJsonb(Object value) {
        if (value == null) {
            return null;
        }
        try {
            PGobject jsonObject = new PGobject();
            jsonObject.setType("jsonb");
            jsonObject.setValue(objectMapper.writeValueAsString(value));
            return jsonObject;
        } catch (JsonProcessingException | SQLException ex) {
            throw new IllegalStateException("JSON 序列化失败", ex);
        }
    }

    public <T> T fromJson(String value, Class<T> type) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("JSON 反序列化失败", ex);
        }
    }

    public <T> T fromJson(String value, TypeReference<T> typeReference) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(value, typeReference);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("JSON 反序列化失败", ex);
        }
    }
}
