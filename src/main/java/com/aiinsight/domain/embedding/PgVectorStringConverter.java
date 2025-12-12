package com.aiinsight.domain.embedding;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;

/**
 * PostgreSQL pgvector 컬럼을 문자열("[v1,v2,...]")로 매핑하기 위한 컨버터.
 */
@Converter(autoApply = false)
public class PgVectorStringConverter implements AttributeConverter<String, PGobject> {

    @Override
    public PGobject convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            PGobject obj = new PGobject();
            obj.setType("vector");
            obj.setValue(attribute);
            return obj;
        } catch (Exception e) {
            throw new IllegalStateException("pgvector 변환 실패", e);
        }
    }

    @Override
    public String convertToEntityAttribute(PGobject dbData) {
        return dbData != null ? dbData.getValue() : null;
    }
}
