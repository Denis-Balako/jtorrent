package io.github.denisbalako.bittorrent_client.torrent;

import io.github.denisbalako.bittorrent_client.torrent.model.BencodeDecodingException;
import io.github.denisbalako.bittorrent_client.torrent.model.BencodeValue;
import io.github.denisbalako.bittorrent_client.torrent.model.BencodeValue.BencodeDict;
import io.github.denisbalako.bittorrent_client.torrent.model.BencodeValue.BencodeInteger;
import io.github.denisbalako.bittorrent_client.torrent.model.BencodeValue.BencodeList;
import io.github.denisbalako.bittorrent_client.torrent.model.BencodeValue.BencodeString;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class BencodeDecoder {

    public BencodeValue decode(byte[] input) {
        if (input == null || input.length == 0) {
            throw new BencodeDecodingException("Input is empty");
        }
        int[] cursor = {0};
        BencodeValue result = decodeValue(input, cursor);
        if (cursor[0] != input.length) {
            throw new BencodeDecodingException(
                    "Trailing bytes after decoded value at index " + cursor[0]);
        }
        return result;
    }

    /**
     * Finds the raw byte range of the "info" value inside a bencoded torrent dict,
     * then returns SHA-1 of those bytes. The range is captured before any parsing,
     * so the hash covers the original encoded bytes exactly.
     */
    public byte[] decodeInfoHash(byte[] rawTorrent) {
        if (rawTorrent == null || rawTorrent.length == 0) {
            throw new BencodeDecodingException("Input is empty");
        }
        if (rawTorrent[0] != 'd') {
            throw new BencodeDecodingException("Top-level value is not a dict");
        }

        int[] cursor = {1};
        while (cursor[0] < rawTorrent.length && rawTorrent[cursor[0]] != 'e') {
            int keyStart = cursor[0];
            BencodeValue keyValue = decodeValue(rawTorrent, cursor);
            if (!(keyValue instanceof BencodeString)) {
                throw new BencodeDecodingException(
                        "Dict key is not a string at index " + keyStart);
            }
            String key = keyValue.asString();
            int valueStart = cursor[0];
            if ("info".equals(key)) {
                skipValue(rawTorrent, cursor);
                int valueEnd = cursor[0];
                return sha1(rawTorrent, valueStart, valueEnd);
            }
            skipValue(rawTorrent, cursor);
        }
        throw new BencodeDecodingException("No 'info' key found in torrent dict");
    }

    private BencodeValue decodeValue(byte[] input, int[] cursor) {
        requireNotEnd(input, cursor[0], "value");
        byte b = input[cursor[0]];
        if (b == 'i') {
            return decodeInteger(input, cursor);
        }
        if (b == 'l') {
            return decodeList(input, cursor);
        }
        if (b == 'd') {
            return decodeDict(input, cursor);
        }
        if (b >= '0' && b <= '9') {
            return decodeString(input, cursor);
        }
        throw new BencodeDecodingException(
                "Unexpected byte '" + (char) b + "' (0x" + Integer.toHexString(b & 0xFF)
                        + ") at index " + cursor[0]);
    }

    private BencodeInteger decodeInteger(byte[] input, int[] cursor) {
        cursor[0]++; // consume 'i'
        int start = cursor[0];
        while (cursor[0] < input.length && input[cursor[0]] != 'e') {
            cursor[0]++;
        }
        requireNotEnd(input, cursor[0], "'e' to close integer");
        String digits = new String(input, start, cursor[0] - start);
        cursor[0]++; // consume 'e'
        if (digits.isEmpty()) {
            throw new BencodeDecodingException("Empty integer at index " + start);
        }
        if (digits.equals("-0")) {
            throw new BencodeDecodingException("Negative zero is not a valid bencoded integer");
        }
        if (digits.length() > 1 && digits.charAt(0) == '0') {
            throw new BencodeDecodingException("Leading zeros in integer '" + digits + "'");
        }
        if (digits.length() > 2 && digits.charAt(0) == '-' && digits.charAt(1) == '0') {
            throw new BencodeDecodingException("Leading zeros in negative integer '" + digits + "'");
        }
        try {
            return new BencodeInteger(Long.parseLong(digits));
        } catch (NumberFormatException e) {
            throw new BencodeDecodingException("Invalid integer '" + digits + "'", e);
        }
    }

    private BencodeString decodeString(byte[] input, int[] cursor) {
        int colonPos = cursor[0];
        while (colonPos < input.length && input[colonPos] != ':') {
            colonPos++;
        }
        if (colonPos >= input.length) {
            throw new BencodeDecodingException(
                    "Missing ':' in string length starting at index " + cursor[0]);
        }
        String lengthStr = new String(input, cursor[0], colonPos - cursor[0]);
        int length;
        try {
            length = Integer.parseInt(lengthStr);
        } catch (NumberFormatException e) {
            throw new BencodeDecodingException(
                    "Invalid string length '" + lengthStr + "' at index " + cursor[0], e);
        }
        if (length < 0) {
            throw new BencodeDecodingException(
                    "Negative string length " + length + " at index " + cursor[0]);
        }
        cursor[0] = colonPos + 1; // move past ':'
        int end = cursor[0] + length;
        if (end > input.length) {
            throw new BencodeDecodingException(
                    "String of length " + length + " at index " + cursor[0]
                            + " exceeds input boundary");
        }
        byte[] bytes = new byte[length];
        System.arraycopy(input, cursor[0], bytes, 0, length);
        cursor[0] = end;
        return new BencodeString(bytes);
    }

    private BencodeList decodeList(byte[] input, int[] cursor) {
        cursor[0]++; // consume 'l'
        List<BencodeValue> values = new ArrayList<>();
        while (cursor[0] < input.length && input[cursor[0]] != 'e') {
            values.add(decodeValue(input, cursor));
        }
        requireNotEnd(input, cursor[0], "'e' to close list");
        cursor[0]++; // consume 'e'
        return new BencodeList(values);
    }

    private BencodeDict decodeDict(byte[] input, int[] cursor) {
        cursor[0]++; // consume 'd'
        LinkedHashMap<String, BencodeValue> entries = new LinkedHashMap<>();
        while (cursor[0] < input.length && input[cursor[0]] != 'e') {
            int keyIndex = cursor[0];
            BencodeValue keyValue = decodeValue(input, cursor);
            if (!(keyValue instanceof BencodeString)) {
                throw new BencodeDecodingException(
                        "Dict key is not a string at index " + keyIndex);
            }
            String key = keyValue.asString();
            if (entries.containsKey(key)) {
                throw new BencodeDecodingException("Duplicate dict key '" + key + "'");
            }
            entries.put(key, decodeValue(input, cursor));
        }
        requireNotEnd(input, cursor[0], "'e' to close dict");
        cursor[0]++; // consume 'e'
        return new BencodeDict(entries);
    }

    // Advances cursor past one complete bencoded value without allocating a BencodeValue tree.
    private void skipValue(byte[] input, int[] cursor) {
        requireNotEnd(input, cursor[0], "value to skip");
        byte b = input[cursor[0]];
        if (b == 'i') {
            cursor[0]++;
            while (cursor[0] < input.length && input[cursor[0]] != 'e') cursor[0]++;
            requireNotEnd(input, cursor[0], "'e' to close integer");
            cursor[0]++;
        } else if (b == 'l' || b == 'd') {
            cursor[0]++;
            while (cursor[0] < input.length && input[cursor[0]] != 'e') skipValue(input, cursor);
            requireNotEnd(input, cursor[0], "'e' to close " + (b == 'l' ? "list" : "dict"));
            cursor[0]++;
        } else if (b >= '0' && b <= '9') {
            int colonPos = cursor[0];
            while (colonPos < input.length && input[colonPos] != ':') colonPos++;
            if (colonPos >= input.length) {
                throw new BencodeDecodingException(
                        "Missing ':' in string length starting at index " + cursor[0]);
            }
            int length = Integer.parseInt(new String(input, cursor[0], colonPos - cursor[0]));
            cursor[0] = colonPos + 1 + length;
            if (cursor[0] > input.length) {
                throw new BencodeDecodingException("String length exceeds input boundary");
            }
        } else {
            throw new BencodeDecodingException(
                    "Unexpected byte 0x" + Integer.toHexString(b & 0xFF)
                            + " at index " + cursor[0] + " while skipping");
        }
    }

    private static void requireNotEnd(byte[] input, int index, String expected) {
        if (index >= input.length) {
            throw new BencodeDecodingException(
                    "Unexpected end of input, expected " + expected);
        }
    }

    private static byte[] sha1(byte[] input, int offset, int end) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(input, offset, end - offset);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 not available", e);
        }
    }
}
