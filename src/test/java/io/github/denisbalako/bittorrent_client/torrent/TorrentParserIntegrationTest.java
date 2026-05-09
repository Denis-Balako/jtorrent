package io.github.denisbalako.bittorrent_client.torrent;

import io.github.denisbalako.bittorrent_client.torrent.model.TorrentMetainfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loads a torrent file from src/test/resources/fixtures/ubuntu_test.torrent and exercises the
 * full parse path.
 */
class TorrentParserIntegrationTest {

    private static final Path FIXTURE_PATH =
            Path.of("src/test/resources/fixtures/ubuntu_test.torrent");

    private static final TorrentParser PARSER = new TorrentParser();

    @BeforeAll
    static void generateFixture() throws IOException {
        Files.createDirectories(FIXTURE_PATH.getParent());
        Files.write(FIXTURE_PATH, TorrentFixtures.SINGLE_FILE_TORRENT);
    }

    @Test
    void parsesFixtureFile() throws Exception {
        byte[] raw = Files.readAllBytes(FIXTURE_PATH);
        TorrentMetainfo meta = PARSER.parse(raw);

        assertThat(meta.name()).isNotBlank();
        assertThat(meta.announceUrl()).isNotBlank();
        assertThat(meta.pieceLength()).isPositive();
        assertThat(meta.pieces().length % 20).isZero();
        assertThat(meta.pieces().length / 20).isPositive(); // at least one piece
        assertThat(meta.infoHash()).hasSize(20);
        assertThat(meta.totalSize()).isNotNegative();
        assertThat(meta.files()).isNotEmpty();
    }

    @Test
    void infoHashIsDeterministic() throws Exception {
        byte[] raw = Files.readAllBytes(FIXTURE_PATH);
        TorrentMetainfo first = PARSER.parse(raw);
        TorrentMetainfo second = PARSER.parse(raw);

        assertThat(first.infoHash()).isEqualTo(second.infoHash());
    }
}
