package io.github.denisbalako.bittorrent_client.torrent.model;

public class BencodeDecodingException extends RuntimeException {

    public BencodeDecodingException(String message) {
        super(message);
    }

    public BencodeDecodingException(String message, Throwable cause) {
        super(message, cause);
    }
}
