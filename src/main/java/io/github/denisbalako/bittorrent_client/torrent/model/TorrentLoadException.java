package io.github.denisbalako.bittorrent_client.torrent.model;

public class TorrentLoadException extends Exception {

    public TorrentLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
