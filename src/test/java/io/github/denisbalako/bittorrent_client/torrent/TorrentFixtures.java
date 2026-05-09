package io.github.denisbalako.bittorrent_client.torrent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Pre-built minimal bencoded torrent byte arrays for use in tests.
 * Keys are in lexicographic order as required by the bencode spec.
 */
class TorrentFixtures {

    // d8:announce30:http://tracker.example.com/ann4:info{SINGLE_FILE_INFO}e
    static final byte[] SINGLE_FILE_TORRENT;

    // same but with announce-list: [["http://tracker.example.com/ann"]]
    static final byte[] TORRENT_WITH_ANNOUNCE_LIST;

    // multi-file: two files (data/file=100 bytes, data/file2=200 bytes), 1 piece
    static final byte[] MULTI_FILE_TORRENT;

    // raw info dict bytes for SINGLE_FILE_TORRENT — used to verify infoHash
    static final byte[] SINGLE_FILE_INFO_BYTES;

    static {
        try {
            byte[] pieceHash = new byte[20]; // one SHA-1 slot, all zeros

            // info dict: length=262144, name="test.file", piece length=262144, 1 piece hash
            ByteArrayOutputStream info = new ByteArrayOutputStream();
            w(info, "d");
            w(info, "6:lengthi262144e");
            w(info, "4:name9:test.file");
            w(info, "12:piece lengthi262144e");
            w(info, "6:pieces20:");
            info.write(pieceHash);
            w(info, "e");
            SINGLE_FILE_INFO_BYTES = info.toByteArray();

            // single-file torrent
            ByteArrayOutputStream single = new ByteArrayOutputStream();
            w(single, "d8:announce30:http://tracker.example.com/ann4:info");
            single.write(SINGLE_FILE_INFO_BYTES);
            w(single, "e");
            SINGLE_FILE_TORRENT = single.toByteArray();

            // single-file torrent + announce-list
            ByteArrayOutputStream withAnnList = new ByteArrayOutputStream();
            w(withAnnList, "d8:announce30:http://tracker.example.com/ann");
            w(withAnnList, "13:announce-listll30:http://tracker.example.com/annee");
            w(withAnnList, "4:info");
            withAnnList.write(SINGLE_FILE_INFO_BYTES);
            w(withAnnList, "e");
            TORRENT_WITH_ANNOUNCE_LIST = withAnnList.toByteArray();

            // multi-file torrent
            // info keys: files < name < piece length < pieces
            // file entry keys: length < path
            ByteArrayOutputStream multi = new ByteArrayOutputStream();
            w(multi, "d8:announce30:http://tracker.example.com/ann4:infod");
            w(multi, "5:filesl");
            w(multi, "d6:lengthi100e4:pathl4:data4:fileee");
            w(multi, "d6:lengthi200e4:pathl4:data5:file2ee");
            w(multi, "e");                    // end files list
            w(multi, "4:name4:test");
            w(multi, "12:piece lengthi262144e");
            w(multi, "6:pieces20:");
            multi.write(pieceHash);
            w(multi, "e");                    // end info dict
            w(multi, "e");                    // end top dict
            MULTI_FILE_TORRENT = multi.toByteArray();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static void w(ByteArrayOutputStream out, String s) throws IOException {
        out.write(s.getBytes(StandardCharsets.ISO_8859_1));
    }
}
