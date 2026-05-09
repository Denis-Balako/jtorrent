package io.github.denisbalako.bittorrent_client.torrent.model;

import java.util.List;

public record TorrentMetainfo(
        String announceUrl,
        List<String> announceList,
        String name,
        long pieceLength,
        byte[] pieces,
        long totalSize,
        List<FileInfo> files,
        byte[] infoHash
) {}
