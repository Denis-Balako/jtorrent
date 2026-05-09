package io.github.denisbalako.bittorrent_client.torrent.model;

public class TorrentParseException extends Exception {

    public TorrentParseException(String message) {
        super(message);
    }

    public TorrentParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
