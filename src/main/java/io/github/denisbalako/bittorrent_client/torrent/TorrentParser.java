package io.github.denisbalako.bittorrent_client.torrent;

import io.github.denisbalako.bittorrent_client.torrent.model.BencodeDecodingException;
import io.github.denisbalako.bittorrent_client.torrent.model.BencodeValue;
import io.github.denisbalako.bittorrent_client.torrent.model.BencodeValue.BencodeDict;
import io.github.denisbalako.bittorrent_client.torrent.model.BencodeValue.BencodeList;
import io.github.denisbalako.bittorrent_client.torrent.model.FileInfo;
import io.github.denisbalako.bittorrent_client.torrent.model.TorrentMetainfo;
import io.github.denisbalako.bittorrent_client.torrent.model.TorrentParseException;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Component
public class TorrentParser {

    private final BencodeDecoder decoder = new BencodeDecoder();

    public TorrentMetainfo parse(byte[] rawTorrent) throws TorrentParseException {
        if (rawTorrent == null || rawTorrent.length == 0) {
            throw new TorrentParseException("'rawTorrent': input is empty");
        }

        BencodeValue root;
        try {
            root = decoder.decode(rawTorrent);
        } catch (BencodeDecodingException e) {
            throw new TorrentParseException("Failed to decode bencode: " + e.getMessage(), e);
        }
        if (!(root instanceof BencodeDict rootDict)) {
            throw new TorrentParseException("'root': top-level value must be a dict");
        }

        LinkedHashMap<String, BencodeValue> top = rootDict.entries();

        String announceUrl = requireString(top, "announce");
        List<String> announceList = parseAnnounceList(top);

        BencodeValue infoValue = top.get("info");
        if (!(infoValue instanceof BencodeDict infoDict)) {
            throw new TorrentParseException("'info': missing or not a dict");
        }
        LinkedHashMap<String, BencodeValue> info = infoDict.entries();

        String name = requireString(info, "name");
        if (name.isEmpty()) {
            throw new TorrentParseException("'name': must not be empty");
        }

        long pieceLength = requireLong(info, "piece length");
        if (pieceLength <= 0) {
            throw new TorrentParseException("'piece length': must be positive, got " + pieceLength);
        }

        byte[] pieces = requireBytes(info, "pieces");
        if (pieces.length % 20 != 0) {
            throw new TorrentParseException(
                    "'pieces': length must be divisible by 20, got " + pieces.length);
        }

        boolean singleFile = info.containsKey("length");
        boolean multiFile = info.containsKey("files");

        if (!singleFile && !multiFile) {
            throw new TorrentParseException("'length'/'files': at least one must be present");
        }

        List<FileInfo> files;
        long totalSize;

        if (singleFile) {
            long length = requireLong(info, "length");
            totalSize = length;
            files = List.of(new FileInfo(List.of(name), length));
        } else {
            files = parseFiles(info);
            totalSize = files.stream().mapToLong(FileInfo::length).sum();
        }

        byte[] infoHash;
        try {
            infoHash = decoder.decodeInfoHash(rawTorrent);
        } catch (BencodeDecodingException e) {
            throw new TorrentParseException("Failed to compute info hash: " + e.getMessage(), e);
        }

        return new TorrentMetainfo(announceUrl, announceList, name, pieceLength, pieces,
                totalSize, files, infoHash);
    }

    private List<String> parseAnnounceList(LinkedHashMap<String, BencodeValue> top)
            throws TorrentParseException {
        BencodeValue raw = top.get("announce-list");
        if (raw == null) {
            return List.of();
        }
        if (!(raw instanceof BencodeList outerList)) {
            throw new TorrentParseException("'announce-list': must be a list");
        }
        List<String> urls = new ArrayList<>();
        for (int i = 0; i < outerList.values().size(); i++) {
            BencodeValue tier = outerList.values().get(i);
            if (!(tier instanceof BencodeList tierList)) {
                throw new TorrentParseException(
                        "'announce-list[" + i + "]': tier must be a list");
            }
            for (int j = 0; j < tierList.values().size(); j++) {
                BencodeValue url = tierList.values().get(j);
                try {
                    urls.add(url.asString());
                } catch (Exception e) {
                    throw new TorrentParseException(
                            "'announce-list[" + i + "][" + j + "]': must be a string");
                }
            }
        }
        return List.copyOf(urls);
    }

    private List<FileInfo> parseFiles(LinkedHashMap<String, BencodeValue> info)
            throws TorrentParseException {
        BencodeValue raw = info.get("files");
        if (!(raw instanceof BencodeList fileList)) {
            throw new TorrentParseException("'files': must be a list");
        }
        List<FileInfo> result = new ArrayList<>(fileList.values().size());
        for (int i = 0; i < fileList.values().size(); i++) {
            BencodeValue entry = fileList.values().get(i);
            if (!(entry instanceof BencodeDict fileDict)) {
                throw new TorrentParseException("'files[" + i + "]': must be a dict");
            }
            LinkedHashMap<String, BencodeValue> fd = fileDict.entries();

            long length = requireLong(fd, "length");

            BencodeValue pathValue = fd.get("path");
            if (!(pathValue instanceof BencodeList pathList)) {
                throw new TorrentParseException("'files[" + i + "].path': must be a list");
            }
            List<String> path = new ArrayList<>(pathList.values().size());
            for (int j = 0; j < pathList.values().size(); j++) {
                BencodeValue segment = pathList.values().get(j);
                try {
                    path.add(segment.asString());
                } catch (Exception e) {
                    throw new TorrentParseException(
                            "'files[" + i + "].path[" + j + "]': must be a string");
                }
            }
            result.add(new FileInfo(List.copyOf(path), length));
        }
        return List.copyOf(result);
    }

    private static String requireString(LinkedHashMap<String, BencodeValue> dict, String field)
            throws TorrentParseException {
        BencodeValue v = dict.get(field);
        if (v == null) {
            throw new TorrentParseException("'" + field + "': required field is missing");
        }
        try {
            return v.asString();
        } catch (Exception e) {
            throw new TorrentParseException("'" + field + "': must be a string");
        }
    }

    private static long requireLong(LinkedHashMap<String, BencodeValue> dict, String field)
            throws TorrentParseException {
        BencodeValue v = dict.get(field);
        if (v == null) {
            throw new TorrentParseException("'" + field + "': required field is missing");
        }
        try {
            return v.asLong();
        } catch (Exception e) {
            throw new TorrentParseException("'" + field + "': must be an integer");
        }
    }

    private static byte[] requireBytes(LinkedHashMap<String, BencodeValue> dict, String field)
            throws TorrentParseException {
        BencodeValue v = dict.get(field);
        if (v == null) {
            throw new TorrentParseException("'" + field + "': required field is missing");
        }
        if (!(v instanceof BencodeValue.BencodeString bs)) {
            throw new TorrentParseException("'" + field + "': must be a string (byte array)");
        }
        return bs.value();
    }
}
