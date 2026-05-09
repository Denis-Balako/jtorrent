package io.github.denisbalako.bittorrent_client.torrent;

import io.github.denisbalako.bittorrent_client.torrent.model.BencodeDecodingException;
import io.github.denisbalako.bittorrent_client.torrent.model.BencodeValue;
import io.github.denisbalako.bittorrent_client.torrent.model.BencodeValue.BencodeString;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BencodeDecoderTest {

    private final BencodeDecoder decoder = new BencodeDecoder();

    // ---- integers ----

    @Test
    void decodesPositiveInteger() {
        assertThat(decode("i42e").asLong()).isEqualTo(42L);
    }

    @Test
    void decodesNegativeInteger() {
        assertThat(decode("i-17e").asLong()).isEqualTo(-17L);
    }

    @Test
    void decodesZero() {
        assertThat(decode("i0e").asLong()).isEqualTo(0L);
    }

    @Test
    void decodesLargeInteger() {
        assertThat(decode("i9223372036854775807e").asLong()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void rejectsNegativeZero() {
        assertThatThrownBy(() -> decode("i-0e"))
                .isInstanceOf(BencodeDecodingException.class)
                .hasMessageContaining("Negative zero");
    }

    @Test
    void rejectsLeadingZeroInInteger() {
        assertThatThrownBy(() -> decode("i01e"))
                .isInstanceOf(BencodeDecodingException.class)
                .hasMessageContaining("Leading zeros");
    }

    // ---- strings ----

    @Test
    void decodesEmptyString() {
        BencodeString v = (BencodeString) decode("0:");
        assertThat(v.value()).isEmpty();
    }

    @Test
    void decodesAsciiString() {
        assertThat(decode("5:hello").asString()).isEqualTo("hello");
    }

    @Test
    void decodesRawBinaryBytes() {
        byte[] raw = {0x00, 0x01, (byte) 0xFF, (byte) 0xFE};
        byte[] lengthPrefix = "4:".getBytes(StandardCharsets.ISO_8859_1);
        byte[] input = new byte[lengthPrefix.length + raw.length];
        System.arraycopy(lengthPrefix, 0, input, 0, lengthPrefix.length);
        System.arraycopy(raw, 0, input, lengthPrefix.length, raw.length);

        BencodeString v = (BencodeString) decoder.decode(input);
        assertThat(v.value()).isEqualTo(raw);
    }

    // ---- lists ----

    @Test
    void decodesEmptyList() {
        assertThat(decode("le").asList()).isEmpty();
    }

    @Test
    void decodesNestedLists() {
        List<BencodeValue> outer = decode("lli1eelee").asList();
        assertThat(outer).hasSize(2);
        assertThat(outer.get(0).asList()).hasSize(1);
        assertThat(outer.get(0).asList().getFirst().asLong()).isEqualTo(1L);
        assertThat(outer.get(1).asList()).isEmpty();
    }

    @Test
    void decodesMixedTypeList() {
        List<BencodeValue> values = decode("li42e5:helloe").asList();
        assertThat(values).hasSize(2);
        assertThat(values.get(0).asLong()).isEqualTo(42L);
        assertThat(values.get(1).asString()).isEqualTo("hello");
    }

    // ---- dicts ----

    @Test
    void decodesEmptyDict() {
        assertThat(decode("de").asDict()).isEmpty();
    }

    @Test
    void dictPreservesInsertionOrder() {
        LinkedHashMap<String, BencodeValue> dict = decode("d1:ai1e1:bi2e1:ci3ee").asDict();
        assertThat(dict.keySet()).containsExactly("a", "b", "c");
    }

    @Test
    void decodesNestedDict() {
        LinkedHashMap<String, BencodeValue> outer = decode("d5:innerd3:keyi99eee").asDict();
        LinkedHashMap<String, BencodeValue> inner = outer.get("inner").asDict();
        assertThat(inner.get("key").asLong()).isEqualTo(99L);
    }

    @Test
    void rejectsDuplicateDictKey() {
        assertThatThrownBy(() -> decode("d3:fooi1e3:fooi2ee"))
                .isInstanceOf(BencodeDecodingException.class)
                .hasMessageContaining("Duplicate");
    }

    // ---- invalid input ----

    @Test
    void rejectsTruncatedInteger() {
        assertThatThrownBy(() -> decode("i42"))
                .isInstanceOf(BencodeDecodingException.class)
                .hasMessageContaining("end of input");
    }

    @Test
    void rejectsWrongTypeMarker() {
        assertThatThrownBy(() -> decode("x42e"))
                .isInstanceOf(BencodeDecodingException.class)
                .hasMessageContaining("Unexpected byte");
    }

    @Test
    void rejectsNonNumericStringLength() {
        assertThatThrownBy(() -> decode("abc:hello"))
                .isInstanceOf(BencodeDecodingException.class);
    }

    @Test
    void rejectsTruncatedString() {
        assertThatThrownBy(() -> decode("10:short"))
                .isInstanceOf(BencodeDecodingException.class)
                .hasMessageContaining("exceeds input boundary");
    }

    @Test
    void rejectsTrailingBytes() {
        assertThatThrownBy(() -> decode("i1egarbage"))
                .isInstanceOf(BencodeDecodingException.class)
                .hasMessageContaining("Trailing");
    }

    @Test
    void rejectsTruncatedList() {
        assertThatThrownBy(() -> decode("li1e"))
                .isInstanceOf(BencodeDecodingException.class)
                .hasMessageContaining("end of input");
    }

    // ---- decodeInfoHash ----

    @Test
    void decodeInfoHashMatchesManualSha1() throws NoSuchAlgorithmException {
        // info value is "d4:name4:teste" — 14 bytes
        byte[] torrent = "d8:announce3:foo4:infod4:name4:testee"
                .getBytes(StandardCharsets.ISO_8859_1);
        byte[] infoDictBytes = "d4:name4:teste".getBytes(StandardCharsets.ISO_8859_1);
        byte[] expectedHash = MessageDigest.getInstance("SHA-1").digest(infoDictBytes);

        byte[] actualHash = decoder.decodeInfoHash(torrent);

        assertThat(actualHash).isEqualTo(expectedHash);
    }

    @Test
    void decodeInfoHashReturns20Bytes() {
        byte[] hash = decoder.decodeInfoHash(TorrentFixtures.SINGLE_FILE_TORRENT);
        assertThat(hash).hasSize(20);
    }

    @Test
    void decodeInfoHashThrowsWhenNoInfoKey() {
        byte[] torrent = "d8:announce3:fooe".getBytes(StandardCharsets.ISO_8859_1);
        assertThatThrownBy(() -> decoder.decodeInfoHash(torrent))
                .isInstanceOf(BencodeDecodingException.class)
                .hasMessageContaining("info");
    }

    @Test
    void decodeInfoHashThrowsForNonDict() {
        byte[] torrent = "li1ee".getBytes(StandardCharsets.ISO_8859_1);
        assertThatThrownBy(() -> decoder.decodeInfoHash(torrent))
                .isInstanceOf(BencodeDecodingException.class)
                .hasMessageContaining("not a dict");
    }

    // ---- helper ----

    private BencodeValue decode(String bencode) {
        return decoder.decode(bencode.getBytes(StandardCharsets.ISO_8859_1));
    }
}
