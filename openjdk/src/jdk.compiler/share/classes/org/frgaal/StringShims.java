/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.frgaal;

import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StringShims {

    //copied from nb-java-x, originally under GPLv2+CPE:

    /**
     * Returns a string whose value is this string, with incidental
     * {@linkplain Character#isWhitespace(int) white space} removed from
     * the beginning and end of every line.
     * <p>
     * Incidental {@linkplain Character#isWhitespace(int) white space}
     * is often present in a text block to align the content with the opening
     * delimiter. For example, in the following code, dots represent incidental
     * {@linkplain Character#isWhitespace(int) white space}:
     * <blockquote><pre>
     * String html = """
     * ..............&lt;html&gt;
     * ..............    &lt;body&gt;
     * ..............        &lt;p&gt;Hello, world&lt;/p&gt;
     * ..............    &lt;/body&gt;
     * ..............&lt;/html&gt;
     * ..............""";
     * </pre></blockquote>
     * This method treats the incidental
     * {@linkplain Character#isWhitespace(int) white space} as indentation to be
     * stripped, producing a string that preserves the relative indentation of
     * the content. Using | to visualize the start of each line of the string:
     * <blockquote><pre>
     * |&lt;html&gt;
     * |    &lt;body&gt;
     * |        &lt;p&gt;Hello, world&lt;/p&gt;
     * |    &lt;/body&gt;
     * |&lt;/html&gt;
     * </pre></blockquote>
     * First, the individual lines of this string are extracted as if by using
     * {@link String#lines()}.
     * <p>
     * Then, the <i>minimum indentation</i> (min) is determined as follows.
     * For each non-blank line (as defined by {@link String#isBlank()}), the
     * leading {@linkplain Character#isWhitespace(int) white space} characters are
     * counted. The leading {@linkplain Character#isWhitespace(int) white space}
     * characters on the last line are also counted even if
     * {@linkplain String#isBlank() blank}. The <i>min</i> value is the smallest
     * of these counts.
     * <p>
     * For each {@linkplain String#isBlank() non-blank} line, <i>min</i> leading
     * {@linkplain Character#isWhitespace(int) white space} characters are removed,
     * and any trailing {@linkplain Character#isWhitespace(int) white space}
     * characters are removed. {@linkplain String#isBlank() Blank} lines are
     * replaced with the empty string.
     *
     * <p>
     * Finally, the lines are joined into a new string, using the LF character
     * {@code "\n"} (U+000A) to separate lines.
     *
     * @apiNote
     * This method's primary purpose is to shift a block of lines as far as
     * possible to the left, while preserving relative indentation. Lines
     * that were indented the least will thus have no leading
     * {@linkplain Character#isWhitespace(int) white space}.
     * The line count of the result will be the same as line count of this
     * string.
     * If this string ends with a line terminator then the result will end
     * with a line terminator.
     *
     * @implNote
     * This method treats all {@linkplain Character#isWhitespace(int) white space}
     * characters as having equal width. As long as the indentation on every
     * line is consistently composed of the same character sequences, then the
     * result will be as described above.
     *
     * @return string with incidental indentation removed and line
     *         terminators normalized
     *
     * @see String#lines()
     * @see String#isBlank()
     * @see String#indent(int)
     * @see Character#isWhitespace(int)
     *
     * @since 13
     *
     * ATdeprecated  This method is associated with text blocks, a preview language feature.
     *              Text blocks and/or this method may be changed or removed in a future release.
     */
//        @Deprecated(forRemoval=true, since="13")
    public static String stripIndent(String str) {
        int length = str.length();
        if (length == 0) {
            return "";
        }
        char lastChar = str.charAt(length - 1);
        boolean optOut = lastChar == '\n' || lastChar == '\r';
        java.util.List<String> lines = lines(str).collect(Collectors.toList());
        final int outdent = optOut ? 0 : outdent(lines);
        return lines.stream()
            .map(line -> {
                int firstNonWhitespace = indexOfNonWhitespace(line);
                int lastNonWhitespace = lastIndexOfNonWhitespace(line);
                int incidentalWhitespace = Math.min(outdent, firstNonWhitespace);
                return firstNonWhitespace > lastNonWhitespace
                    ? "" : line.substring(incidentalWhitespace, lastNonWhitespace);
            })
            .collect(Collectors.joining("\n", "", optOut ? "\n" : ""));
    }

    private static int outdent(java.util.List<String> lines) {
        // Note: outdent is guaranteed to be zero or positive number.
        // If there isn't a non-blank line then the last must be blank
        int outdent = Integer.MAX_VALUE;
        for (String line : lines) {
            int leadingWhitespace = indexOfNonWhitespace(line);
            if (leadingWhitespace != line.length()) {
                outdent = Integer.min(outdent, leadingWhitespace);
            }
        }
        String lastLine = lines.get(lines.size() - 1);
        if (isBlank(lastLine)) {
            outdent = Integer.min(outdent, lastLine.length());
        }
        return outdent;
    }

    /**
     * Returns a string whose value is this string, with escape sequences
     * translated as if in a string literal.
     * <p>
     * Escape sequences are translated as follows;
     * <table class="striped">
     *   <caption style="display:none">Translation</caption>
     *   <thead>
     *   <tr>
     *     <th scope="col">Escape</th>
     *     <th scope="col">Name</th>
     *     <th scope="col">Translation</th>
     *   </tr>
     *   </thead>
     *   <tbody>
     *   <tr>
     *     <th scope="row">{@code \u005Cb}</th>
     *     <td>backspace</td>
     *     <td>{@code U+0008}</td>
     *   </tr>
     *   <tr>
     *     <th scope="row">{@code \u005Ct}</th>
     *     <td>horizontal tab</td>
     *     <td>{@code U+0009}</td>
     *   </tr>
     *   <tr>
     *     <th scope="row">{@code \u005Cn}</th>
     *     <td>line feed</td>
     *     <td>{@code U+000A}</td>
     *   </tr>
     *   <tr>
     *     <th scope="row">{@code \u005Cf}</th>
     *     <td>form feed</td>
     *     <td>{@code U+000C}</td>
     *   </tr>
     *   <tr>
     *     <th scope="row">{@code \u005Cr}</th>
     *     <td>carriage return</td>
     *     <td>{@code U+000D}</td>
     *   </tr>
     *   <tr>
     *     <th scope="row">{@code \u005C"}</th>
     *     <td>double quote</td>
     *     <td>{@code U+0022}</td>
     *   </tr>
     *   <tr>
     *     <th scope="row">{@code \u005C'}</th>
     *     <td>single quote</td>
     *     <td>{@code U+0027}</td>
     *   </tr>
     *   <tr>
     *     <th scope="row">{@code \u005C\u005C}</th>
     *     <td>backslash</td>
     *     <td>{@code U+005C}</td>
     *   </tr>
     *   <tr>
     *     <th scope="row">{@code \u005C0 - \u005C377}</th>
     *     <td>octal escape</td>
     *     <td>code point equivalents</td>
     *   </tr>
     *   </tbody>
     * </table>
     *
     * @implNote
     * This method does <em>not</em> translate Unicode escapes such as "{@code \u005cu2022}".
     * Unicode escapes are translated by the Java compiler when reading input characters and
     * are not part of the string literal specification.
     *
     * @throws IllegalArgumentException when an escape sequence is malformed.
     *
     * @return String with escape sequences translated.
     *
     * @jls 3.10.7 Escape Sequences
     *
     * @since 13
     *
     * ATdeprecated  This method is associated with text blocks, a preview language feature.
     *              Text blocks and/or this method may be changed or removed in a future release.
     */
    public static String translateEscapes(String str) {
        if (str.isEmpty()) {
            return "";
        }
        char[] chars = str.toCharArray();
        int length = chars.length;
        int from = 0;
        int to = 0;
        while (from < length) {
            char ch = chars[from++];
            if (ch == '\\') {
                ch = from < length ? chars[from++] : '\0';
                switch (ch) {
                case 'b':
                    ch = '\b';
                    break;
                case 'f':
                    ch = '\f';
                    break;
                case 'n':
                    ch = '\n';
                    break;
                case 'r':
                    ch = '\r';
                    break;
                case 's':
                    ch = ' ';
                    break;
                case 't':
                    ch = '\t';
                    break;
                case '\'':
                case '\"':
                case '\\':
                    // as is
                    break;
                case '0': case '1': case '2': case '3':
                case '4': case '5': case '6': case '7':
                    int limit = Integer.min(from + (ch <= '3' ? 2 : 1), length);
                    int code = ch - '0';
                    while (from < limit) {
                        ch = chars[from];
                        if (ch < '0' || '7' < ch) {
                            break;
                        }
                        from++;
                        code = (code << 3) | (ch - '0');
                    }
                    ch = (char)code;
                    break;
                case '\n':
                    continue;
                case '\r':
                    if (from < length && chars[from] == '\n') {
                        from++;
                    }
                    continue;
                default: {
                    String msg = String.format(
                        "Invalid escape sequence: \\%c \\\\u%04X",
                        ch, (int)ch);
                    throw new IllegalArgumentException(msg);
                }
                }
            }

            chars[to++] = ch;
        }

        return new String(chars, 0, to);
    }

    private static int indexOfNonWhitespace(String str) {
        return indexOfNonWhitespace(str.toCharArray());
    }

    private static int lastIndexOfNonWhitespace(String str) {
        return lastIndexOfNonWhitespace(str.toCharArray());
    }

    /**
     * Returns {@code true} if the string is empty or contains only
     * {@linkplain Character#isWhitespace(int) white space} codepoints,
     * otherwise {@code false}.
     *
     * @return {@code true} if the string is empty or contains only
     *         {@linkplain Character#isWhitespace(int) white space} codepoints,
     *         otherwise {@code false}
     *
     * @see Character#isWhitespace(int)
     *
     * @since 11
     */
    public static boolean isBlank(String str) {
        return indexOfNonWhitespace(str) == str.length();
    }

    /**
     * Returns a string whose value is this string, with all leading
     * {@linkplain Character#isWhitespace(int) white space} removed.
     * <p>
     * If this {@code String} object represents an empty string,
     * or if all code points in this string are
     * {@linkplain Character#isWhitespace(int) white space}, then an empty string
     * is returned.
     * <p>
     * Otherwise, returns a substring of this string beginning with the first
     * code point that is not a {@linkplain Character#isWhitespace(int) white space}
     * up to and including the last code point of this string.
     * <p>
     * This method may be used to trim
     * {@linkplain Character#isWhitespace(int) white space} from
     * the beginning of a string.
     *
     * @return  a string whose value is this string, with all leading white
     *          space removed
     *
     * @see Character#isWhitespace(int)
     *
     * @since 11
     */
    public static String stripLeading(String str) {
        String ret = stripLeading(str.toCharArray());
        return ret == null ? str : ret;
    }
    public static String stripLeading(char[] value) {
        int length = value.length;
        int left = indexOfNonWhitespace(value);
        if (left == length) {
            return "";
        }
        return (left != 0) ? newString(value, left, length - left) : null;
    }

    public static String stripTrailing(String str) {
        String ret = stripTrailing(str.toCharArray());
        return ret == null ? str : ret;
    }

    public static String stripTrailing(char[] value) {
        int length = value.length >>> 1;
        int right = lastIndexOfNonWhitespace(value);
        return (right != length) ? newString(value, 0, right) : null;
    }
    public static int indexOfNonWhitespace(char[] value) {
        int length = value.length;
        int left = 0;
        while (left < length) {
            int codepoint = codePointAt(value, left, length);
            if (codepoint != ' ' && codepoint != '\t' && !Character.isWhitespace(codepoint)) {
                break;
            }
            left += Character.charCount(codepoint);
        }
        return left;
    }
    private static int codePointAt(char[] value, int index, int end, boolean checked) {
        assert index < end;
        if (checked) {
            checkIndex(index, value);
        }
        char c1 = getChar(value, index);
        if (Character.isHighSurrogate(c1) && ++index < end) {
            if (checked) {
                checkIndex(index, value);
            }
            char c2 = getChar(value, index);
            if (Character.isLowSurrogate(c2)) {
               return Character.toCodePoint(c1, c2);
            }
        }
        return c1;
    }

    public static int codePointAt(char[] value, int index, int end) {
       return codePointAt(value, index, end, false /* unchecked */);
    }
    public static int lastIndexOfNonWhitespace(char[] value) {
        int length = value.length;
        int right = length;
        while (0 < right) {
            int codepoint = codePointBefore(value, right);
            if (codepoint != ' ' && codepoint != '\t' && !Character.isWhitespace(codepoint)) {
                break;
            }
            right -= Character.charCount(codepoint);
        }
        return right;
    }
    public static int codePointBefore(char[] value, int index) {
        return codePointBefore(value, index, false /* unchecked */);
    }
    private static int codePointBefore(char[] value, int index, boolean checked) {
        --index;
        if (checked) {
            checkIndex(index, value);
        }
        char c2 = getChar(value, index);
        if (Character.isLowSurrogate(c2) && index > 0) {
            --index;
            if (checked) {
                checkIndex(index, value);
            }
            char c1 = getChar(value, index);
            if (Character.isHighSurrogate(c1)) {
               return Character.toCodePoint(c1, c2);
            }
        }
        return c2;
    }
    public static void checkIndex(int off, char[] val) {
        checkIndex(off, length(val));
    }
    static void checkIndex(int index, int length) {
        if (index < 0 || index >= length) {
            throw new StringIndexOutOfBoundsException("index " + index +
                                                      ",length " + length);
        }
    }
    static char getChar(char[] val, int index) {
        assert index >= 0 && index < length(val) : "Trusted caller missed bounds check";
        return val[index];
    }
    public static int length(char[] value) {
        return value.length;
    }
    public static String newString(char[] val, int index, int len) {
        return new String(val, index, len);
    }
    public static Stream<String> lines(String str) {
        return lines(str.toCharArray());
    }
    static Stream<String> lines(char[] value) {
        return StreamSupport.stream(LinesSpliterator.spliterator(value), false);
    }

    private final static class LinesSpliterator implements Spliterator<String> {
        private char[] value;
        private int index;        // current index, modified on advance/split
        private final int fence;  // one past last index

        private LinesSpliterator(char[] value, int start, int length) {
            this.value = value;
            this.index = start;
            this.fence = start + length;
        }

        private int indexOfLineSeparator(int start) {
            for (int current = start; current < fence; current++) {
                char ch = getChar(value, current);
                if (ch == '\n' || ch == '\r') {
                    return current;
                }
            }
            return fence;
        }

        private int skipLineSeparator(int start) {
            if (start < fence) {
                if (getChar(value, start) == '\r') {
                    int next = start + 1;
                    if (next < fence && getChar(value, next) == '\n') {
                        return next + 1;
                    }
                }
                return start + 1;
            }
            return fence;
        }

        private String next() {
            int start = index;
            int end = indexOfLineSeparator(start);
            index = skipLineSeparator(end);
            return newString(value, start, end - start);
        }

        @Override
        public boolean tryAdvance(Consumer<? super String> action) {
            if (action == null) {
                throw new NullPointerException("tryAdvance action missing");
            }
            if (index != fence) {
                action.accept(next());
                return true;
            }
            return false;
        }

        @Override
        public void forEachRemaining(Consumer<? super String> action) {
            if (action == null) {
                throw new NullPointerException("forEachRemaining action missing");
            }
            while (index != fence) {
                action.accept(next());
            }
        }

        @Override
        public Spliterator<String> trySplit() {
            int half = (fence + index) >>> 1;
            int mid = skipLineSeparator(indexOfLineSeparator(half));
            if (mid < fence) {
                int start = index;
                index = mid;
                return new LinesSpliterator(value, start, mid - start);
            }
            return null;
        }

        @Override
        public long estimateSize() {
            return fence - index + 1;
        }

        @Override
        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL;
        }

        static LinesSpliterator spliterator(char[] value) {
            return new LinesSpliterator(value, 0, value.length);
        }
    }

    public static String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    public static String strip(String text) {
        return stripLeading(stripTrailing(text));
    }

}
