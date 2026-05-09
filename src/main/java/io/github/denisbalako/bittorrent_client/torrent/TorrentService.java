package io.github.denisbalako.bittorrent_client.torrent;

import io.github.denisbalako.bittorrent_client.torrent.model.TorrentLoadException;
import io.github.denisbalako.bittorrent_client.torrent.model.TorrentMetainfo;
import io.github.denisbalako.bittorrent_client.torrent.model.TorrentParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class TorrentService {

    private final TorrentParser torrentParser;

    public TorrentMetainfo loadFromFile(MultipartFile file) throws TorrentLoadException, TorrentParseException {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new TorrentLoadException("Failed to read uploaded file: " + file.getOriginalFilename(), e);
        }
        return parseAndLog(bytes);
    }

    public TorrentMetainfo loadFromPath(Path path) throws TorrentLoadException, TorrentParseException {
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new TorrentLoadException("Failed to read torrent file: " + path, e);
        }
        return parseAndLog(bytes);
    }

    private TorrentMetainfo parseAndLog(byte[] bytes) throws TorrentParseException {
        TorrentMetainfo meta = torrentParser.parse(bytes);
        log.info("Loaded torrent '{}' — {} piece(s), {} byte(s) total",
                meta.name(),
                meta.pieces().length / 20,
                meta.totalSize());
        return meta;
    }
}
