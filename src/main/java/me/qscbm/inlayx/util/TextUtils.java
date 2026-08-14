package me.qscbm.inlayx.util;

import java.util.ArrayList;
import java.util.List;

public class TextUtils {
    public static String toPlainText(String text) {
        final char[] chars = text.toCharArray();
        final int length = chars.length;
        int readPos = 0;
        int writePos = 0;

        while (readPos < length) {
            if (readPos + 1 < length) {
                if ((chars[readPos] == '§' || chars[readPos] == '&') && isColorCodeChar(chars[readPos + 1])) {
                    readPos += 2;
                    continue;
                }
            }

            chars[writePos++] = chars[readPos++];
        }

        return new String(chars, 0, writePos);
    }

    public static String toPlainText(String text, int index) {
        final char[] chars = text.toCharArray();
        final int length = chars.length;
        int readPos = 0;
        int writePos = 0;

        while (readPos <= index && readPos < length) {
            if (readPos + 1 < length) {
                if ((chars[readPos] == '§' || chars[readPos] == '&') && isColorCodeChar(chars[readPos + 1])) {
                    readPos += 2;
                    index += 2;
                    continue;
                }
            }

            chars[writePos++] = chars[readPos++];
        }

        return new String(chars, 0, writePos);
    }

    public static boolean isColorCodeChar(char c) {
        if (c < '0' || c > 'x') {
            return false;
        }
        if (c <= '9') {
            return true;
        }

        final int mask = 0b11011111;
        final int uc = c & mask;
        return (uc >= 'A' && uc <= 'F') || (uc >= 'K' && uc <= 'O') || uc == 'R' || uc == 'X';
    }

    public static List<String> split(String str, String character) {
        int off = 0;
        int next;
        List<String> list = new ArrayList<>(3);
        while ((next = str.indexOf(character, off)) != -1) {
            list.add(str.substring(off, next));
            off = next + 1;
        }
        if (off == 0) return List.of(str);

        list.add(str.substring(off));

        int resultSize = list.size();
        while (resultSize > 0 && list.get(resultSize - 1).isEmpty()) {
            resultSize--;
        }
        return list.subList(0, resultSize);
    }

    public static String translateAlternateColorCodes(String text) {
        final char[] chars = text.toCharArray();
        int i = 0;
        while (i < chars.length - 1) {
            if (chars[i] == '&' && isColorCodeChar(chars[i + 1])) {
                chars[i] = '§';
            }
            i++;
        }

        return new String(chars);
    }

    /**
     * 生成宝石等级对应的星级显示(★), 负数按 0 处理
     */
    public static String getStars(int level) {
        return "★".repeat(Math.max(0, level));
    }

    public static boolean isNumber(String text) {
        char[] chars = text.toCharArray();
        for (char c : chars) {
            switch (c) {
                case '1', '2', '3', '4', '5', '6', '7', '8', '9', '0' -> {}
                default -> {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean hasNumber(String text) {
        char[] chars = text.toCharArray();
        for (char c : chars) {
            switch (c) {
                case '1', '2', '3', '4', '5', '6', '7', '8', '9', '0' -> {
                    return true;
                }
                default -> {}
            }
        }
        return false;
    }

    public static List<String> tokenize(String string) {
        int cursor = 0;
        char[] chars = string.toCharArray();
        int length = chars.length;
        final List<String> output = new ArrayList<>(3);

        while (cursor < length) {
            final char c = chars[cursor];
            String s;
            if (c == '"') {
                cursor++;

                final int start = cursor;
                while (cursor < length && chars[cursor] != '"') {
                    cursor++;
                }
                final int end = cursor;

                if (cursor < length) {
                    cursor++;
                    if (cursor < length && chars[cursor] == ' ') {
                        cursor++;
                    }
                }
                s = new String(chars, start, end - start);
            } else {
                final int start = cursor;
                while (cursor < length && chars[cursor] != ' ') {
                    cursor++;
                }
                final int end = cursor;

                if (cursor < length) {
                    cursor++;
                }

                s = new String(chars, start, end - start);
            }
            output.add(s);
        }
        if (cursor > 0 && chars[cursor - 1] == ' ') {
            output.add("");
        }
        return output;
    }
}
