package io.github.denisbalako.bittorrent_client.torrent.model;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;

public sealed interface BencodeValue
    permits BencodeValue.BencodeInteger, BencodeValue.BencodeString,
    BencodeValue.BencodeList, BencodeValue.BencodeDict {

    default long asLong() {
        throw new BencodeDecodingException("Expected integer, got " + getClass().getSimpleName());
    }

    default String asString() {
        throw new BencodeDecodingException("Expected string, got " + getClass().getSimpleName());
    }

    default List<BencodeValue> asList() {
        throw new BencodeDecodingException("Expected list, got " + getClass().getSimpleName());
    }

    default LinkedHashMap<String, BencodeValue> asDict() {
        throw new BencodeDecodingException("Expected dict, got " + getClass().getSimpleName());
    }

    record BencodeInteger(long value) implements BencodeValue {
        @Override
        public long asLong() {
            return value;
        }
    }

    record BencodeString(byte[] value) implements BencodeValue {
        @Override
        public String asString() {
            return new String(value, StandardCharsets.UTF_8);
        }
    }

    record BencodeList(List<BencodeValue> values) implements BencodeValue {
        @Override
        public List<BencodeValue> asList() {
            return values;
        }
    }

    record BencodeDict(LinkedHashMap<String, BencodeValue> entries) implements BencodeValue {
        @Override
        public LinkedHashMap<String, BencodeValue> asDict() {
            return entries;
        }
    }
}
