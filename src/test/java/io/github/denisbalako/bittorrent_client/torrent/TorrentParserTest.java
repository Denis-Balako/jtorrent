package io.github.denisbalako.bittorrent_client.torrent;

import io.github.denisbalako.bittorrent_client.torrent.model.TorrentMetainfo;
import io.github.denisbalako.bittorrent_client.torrent.model.TorrentParseException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TorrentParserTest {

    private final TorrentParser parser = new TorrentParser();

    // ---- single-file mode ----

    @Test
    void parsesSingleFileTorrent() throws Exception {
        TorrentMetainfo meta = parser.parse(TorrentFixtures.SINGLE_FILE_TORRENT);

        assertThat(meta.announceUrl()).isEqualTo("http://tracker.example.com/ann");
        assertThat(meta.name()).isEqualTo("test.file");
        assertThat(meta.pieceLength()).isEqualTo(262144L);
        assertThat(meta.pieces()).hasSize(20);
        assertThat(meta.totalSize()).isEqualTo(262144L);
        assertThat(meta.files()).hasSize(1);
        assertThat(meta.files().get(0).length()).isEqualTo(262144L);
        assertThat(meta.files().get(0).path()).containsExactly("test.file");
        assertThat(meta.announceList()).isEmpty();
    }

    // ---- multi-file mode ----

    @Test
    void parsesMultiFileTorrent() throws Exception {
        TorrentMetainfo meta = parser.parse(TorrentFixtures.MULTI_FILE_TORRENT);

        assertThat(meta.name()).isEqualTo("test");
        assertThat(meta.files()).hasSize(2);
        assertThat(meta.files().get(0).path()).containsExactly("data", "file");
        assertThat(meta.files().get(0).length()).isEqualTo(100L);
        assertThat(meta.files().get(1).path()).containsExactly("data", "file2");
        assertThat(meta.files().get(1).length()).isEqualTo(200L);
        assertThat(meta.totalSize()).isEqualTo(300L);
    }

    // ---- announce-list ----

    @Test
    void flattensAnnounceListTiers() throws Exception {
        TorrentMetainfo meta = parser.parse(TorrentFixtures.TORRENT_WITH_ANNOUNCE_LIST);

        assertThat(meta.announceList()).containsExactly("http://tracker.example.com/ann");
    }

    // ---- infoHash ----

    @Test
    void infoHashIs20Bytes() throws Exception {
        TorrentMetainfo meta = parser.parse(TorrentFixtures.SINGLE_FILE_TORRENT);

        assertThat(meta.infoHash()).hasSize(20);
    }

    @Test
    void infoHashMatchesSha1OfRawInfoBytes() throws Exception, NoSuchAlgorithmException {
        byte[] expectedHash = MessageDigest.getInstance("SHA-1")
                .digest(TorrentFixtures.SINGLE_FILE_INFO_BYTES);

        TorrentMetainfo meta = parser.parse(TorrentFixtures.SINGLE_FILE_TORRENT);

        assertThat(meta.infoHash()).isEqualTo(expectedHash);
    }

    // ---- validation: missing required fields ----

    @Test
    void throwsWhenAnnounceMissing() {
        byte[] torrent = "d4:infod6:lengthi0e4:name4:test12:piece lengthi0e6:pieces0:ee"
                .getBytes(StandardCharsets.ISO_8859_1);
        assertThatThrownBy(() -> parser.parse(torrent))
                .isInstanceOf(TorrentParseException.class)
                .hasMessageContaining("announce");
    }

    @Test
    void throwsWhenInfoMissing() {
        byte[] torrent = "d8:announce3:fooe".getBytes(StandardCharsets.ISO_8859_1);
        assertThatThrownBy(() -> parser.parse(torrent))
                .isInstanceOf(TorrentParseException.class)
                .hasMessageContaining("info");
    }

    @Test
    void throwsWhenNameMissing() {
        byte[] torrent = buildInfoTorrent("d6:lengthi0e12:piece lengthi262144e6:pieces20:"
                + "\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0e");
        assertThatThrownBy(() -> parser.parse(torrent))
                .isInstanceOf(TorrentParseException.class)
                .hasMessageContaining("name");
    }

    @Test
    void throwsWhenNameIsEmpty() {
        byte[] torrent = buildInfoTorrent("d6:lengthi0e4:name0:12:piece lengthi262144e6:pieces20:"
                + "\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0e");
        assertThatThrownBy(() -> parser.parse(torrent))
                .isInstanceOf(TorrentParseException.class)
                .hasMessageContaining("name");
    }

    @Test
    void throwsWhenNeitherLengthNorFilesPresent() {
        byte[] torrent = buildInfoTorrent("d4:name4:test12:piece lengthi262144e6:pieces20:"
                + "\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0e");
        assertThatThrownBy(() -> parser.parse(torrent))
                .isInstanceOf(TorrentParseException.class)
                .hasMessageContaining("length");
    }

    // ---- validation: pieces ----

    @Test
    void throwsWhenPiecesLengthNotDivisibleBy20() {
        // pieces = 19 bytes — not a multiple of 20
        byte[] torrent = buildInfoTorrent("d6:lengthi0e4:name4:test12:piece lengthi262144e6:pieces19:"
                + "\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0e");
        assertThatThrownBy(() -> parser.parse(torrent))
                .isInstanceOf(TorrentParseException.class)
                .hasMessageContaining("pieces");
    }

    @Test
    void throwsWhenPieceLengthIsZero() {
        byte[] torrent = buildInfoTorrent("d6:lengthi0e4:name4:test12:piece lengthi0e6:pieces0:e");
        assertThatThrownBy(() -> parser.parse(torrent))
                .isInstanceOf(TorrentParseException.class)
                .hasMessageContaining("piece length");
    }

    @Test
    void throwsWhenPieceLengthIsNegative() {
        byte[] torrent = buildInfoTorrent("d6:lengthi0e4:name4:test12:piece lengthi-1e6:pieces0:e");
        assertThatThrownBy(() -> parser.parse(torrent))
                .isInstanceOf(TorrentParseException.class)
                .hasMessageContaining("piece length");
    }

    // ---- helper ----

    private byte[] buildInfoTorrent(String infoDict) {
        String torrent = "d8:announce3:foo4:info" + infoDict + "e";
        return torrent.getBytes(StandardCharsets.ISO_8859_1);
    }
}
