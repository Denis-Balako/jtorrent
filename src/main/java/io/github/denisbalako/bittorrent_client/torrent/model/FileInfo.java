package io.github.denisbalako.bittorrent_client.torrent.model;

import java.util.List;

public record FileInfo(
    List<String> path,
    long length
) {
}
