/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.symmetric.csv;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;

/**
 * A stream based writer for writing delimited text data to a file or a stream.
 */
public class CsvWriter {
    private PrintWriter outputStream = null;
    private String fileName = null;
    private boolean firstColumn = true;
    private boolean useCustomRecordDelimiter = false;
    private Charset charset = null;
    // this holds all the values for switches that the user is allowed to set
    private UserSettings userSettings = new UserSettings();
    private boolean initialized = false;
    private boolean closed = false;
    /**
     * Double up the text qualifier to represent an occurrence of the text qualifier.
     */
    public static final int ESCAPE_MODE_DOUBLED = 1;
    /**
     * Use a backslash character before the text qualifier to represent an occurrence of the text qualifier.
     */
    public static final int ESCAPE_MODE_BACKSLASH = 2;

    /**
     * Creates a {@link org.jumpmind.symmetric.csv.CsvWriter CsvWriter} object using a file as the data destination.
     * 
     * @param fileName
     *            The path to the file to output the data.
     * @param delimiter
     *            The character to use as the column delimiter.
     * @param charset
     *            The {@link java.nio.charset.Charset Charset} to use while writing the data.
     */
    public CsvWriter(String fileName, char delimiter, Charset charset) {
        if (fileName == null) {
            throw new IllegalArgumentException("Parameter fileName can not be null.");
        }
        if (charset == null) {
            throw new IllegalArgumentException("Parameter charset can not be null.");
        }
        this.fileName = fileName;
        userSettings.Delimiter = delimiter;
        this.charset = charset;
    }

    /**
     * Creates a {@link org.jumpmind.symmetric.csv.CsvWriter CsvWriter} object using a file as the data destination.&nbsp;Uses a comma as the column delimiter
     * and ISO-8859-1 as the {@link java.nio.charset.Charset Charset}.
     * 
     * @param fileName
     *            The path to the file to output the data.
     */
    public CsvWriter(String fileName) {
        this(fileName, Letters.COMMA, Charset.forName("ISO-8859-1"));
    }

    /**
     * Creates a {@link org.jumpmind.symmetric.csv.CsvWriter CsvWriter} object using a Writer to write data to.
     * 
     * @param outputStream
     *            The stream to write the column delimited data to.
     * @param delimiter
     *            The character to use as the column delimiter.
     */
    public CsvWriter(Writer outputStream, char delimiter) {
        if (outputStream == null) {
            throw new IllegalArgumentException("Parameter outputStream can not be null.");
        }
        this.outputStream = new PrintWriter(outputStream);
        userSettings.Delimiter = delimiter;
        initialized = true;
    }

    /**
     * Creates a {@link org.jumpmind.symmetric.csv.CsvWriter CsvWriter} object using an OutputStream to write data to.
     * 
     * @param outputStream
     *            The stream to write the column delimited data to.
     * @param delimiter
     *            The character to use as the column delimiter.
     * @param charset
     *            The {@link java.nio.charset.Charset Charset} to use while writing the data.
     */
    public CsvWriter(OutputStream outputStream, char delimiter, Charset charset) {
        this(new OutputStreamWriter(outputStream, charset), delimiter);
    }

    /**
     * Gets the character being used as the column delimiter.
     * 
     * @return The character being used as the column delimiter.
     */
    public char getDelimiter() {
        return userSettings.Delimiter;
    }

    /**
     * Sets the character to use as the column delimiter.
     * 
     * @param delimiter
     *            The character to use as the column delimiter.
     */
    public void setDelimiter(char delimiter) {
        userSettings.Delimiter = delimiter;
    }

    public char getRecordDelimiter() {
        return userSettings.RecordDelimiter;
    }

    /**
     * Sets the character to use as the record delimiter.
     * 
     * @param recordDelimiter
     *            The character to use as the record delimiter. Default is combination of standard end of line characters for Windows, Unix, or Mac.
     */
    public void setRecordDelimiter(char recordDelimiter) {
        useCustomRecordDelimiter = true;
        userSettings.RecordDelimiter = recordDelimiter;
    }

    /**
     * Gets the character to use as a text qualifier in the data.
     * 
     * @return The character to use as a text qualifier in the data.
     */
    public char getTextQualifier() {
        return userSettings.TextQualifier;
    }

    /**
     * Sets the character to use as a text qualifier in the data.
     * 
     * @param textQualifier
     *            The character to use as a text qualifier in the data.
     */
    public void setTextQualifier(char textQualifier) {
        userSettings.TextQualifier = textQualifier;
    }

    /**
     * Whether text qualifiers will be used while writing data or not.
     * 
     * @return Whether text qualifiers will be used while writing data or not.
     */
    public boolean getUseTextQualifier() {
        return userSettings.UseTextQualifier;
    }

    /**
     * Sets whether text qualifiers will be used while writing data or not.
     * 
     * @param useTextQualifier
     *            Whether to use a text qualifier while writing data or not.
     */
    public void setUseTextQualifier(boolean useTextQualifier) {
        userSettings.UseTextQualifier = useTextQualifier;
    }

    public int getEscapeMode() {
        return userSettings.EscapeMode;
    }

    public void setEscapeMode(int escapeMode) {
        userSettings.EscapeMode = escapeMode;
    }

    public void setComment(char comment) {
        userSettings.Comment = comment;
    }

    public char getComment() {
        return userSettings.Comment;
    }

    /**
     * Whether fields will be surrounded by the text qualifier even if the qualifier is not necessarily needed to escape this field.
     * 
     * @return Whether fields will be forced to be qualified or not.
     */
    public boolean getForceQualifier() {
        return userSettings.ForceQualifier;
    }

    /**
     * Use this to force all fields to be surrounded by the text qualifier even if the qualifier is not necessarily needed to escape this field. Default is
     * false.
     * 
     * @param forceQualifier
     *            Whether to force the fields to be qualified or not.
     */
    public void setForceQualifier(boolean forceQualifier) {
        userSettings.ForceQualifier = forceQualifier;
    }

    public String getNullString() {
        return userSettings.NullString;
    }

    public void setNullString(String nullString) {
        userSettings.NullString = nullString;
    }

    /**
     * Writes another column of data to this record.
     * 
     * @param content
     *            The data for the new column.
     * @param preserveSpaces
     *            Whether to preserve leading and trailing whitespace in this column of data.
     * @exception IOException
     *                Thrown if an error occurs while writing data to the destination stream.
     */
    public void write(String content, boolean preserveSpaces) throws IOException {
        checkClosed();
        checkInit();
        // BEGIN <erilong@users.sourceforge.net>
        // if (content == null) {
        // content = "";
        // }
        // END
        if (!firstColumn) {
            outputStream.write(userSettings.Delimiter);
        }
        boolean textQualify = userSettings.ForceQualifier;
        // BEGIN <erilong@users.sourceforge.net>
        // We want a null to be an empty unquoted element
        if (content == null) {
            content = userSettings.NullString;
            textQualify = false;
            outputStream.write(content);
            firstColumn = false;
            return;
        }
        // We want an empty string to be a quoted element
        else if (content.equals("")) {
            textQualify = true;
        }
        // END
        if (!preserveSpaces && content.length() > 0) {
            content = content.trim();
        }
        if (!textQualify
                && userSettings.UseTextQualifier
                && (content.indexOf(userSettings.TextQualifier) > -1
                        || content.indexOf(userSettings.Delimiter) > -1
                        || (!useCustomRecordDelimiter && (content.indexOf(Letters.LF) > -1 || content
                                .indexOf(Letters.CR) > -1))
                        || (useCustomRecordDelimiter && content.indexOf(userSettings.RecordDelimiter) > -1)
                        || (firstColumn && content.length() > 0 && content.charAt(0) == userSettings.Comment))) {
            textQualify = true;
        }
        if (userSettings.UseTextQualifier && !textQualify && content.length() > 0 && preserveSpaces) {
            char firstLetter = content.charAt(0);
            if (firstLetter == Letters.SPACE || firstLetter == Letters.TAB) {
                textQualify = true;
            }
            if (!textQualify && content.length() > 1) {
                char lastLetter = content.charAt(content.length() - 1);
                if (lastLetter == Letters.SPACE || lastLetter == Letters.TAB) {
                    textQualify = true;
                }
            }
        }
        if (textQualify) {
            outputStream.write(userSettings.TextQualifier);
            if (userSettings.EscapeMode == ESCAPE_MODE_BACKSLASH) {
                content = replace(content, "" + Letters.BACKSLASH, "" + Letters.BACKSLASH + Letters.BACKSLASH);
                content = replace(content, "" + userSettings.TextQualifier, "" + Letters.BACKSLASH
                        + userSettings.TextQualifier);
            } else {
                content = replace(content, "" + userSettings.TextQualifier, "" + userSettings.TextQualifier
                        + userSettings.TextQualifier);
            }
        } else if (userSettings.EscapeMode == ESCAPE_MODE_BACKSLASH) {
            content = replace(content, "" + Letters.BACKSLASH, "" + Letters.BACKSLASH + Letters.BACKSLASH);
            content = replace(content, "" + userSettings.Delimiter, "" + Letters.BACKSLASH + userSettings.Delimiter);
            if (useCustomRecordDelimiter) {
                content = replace(content, "" + userSettings.RecordDelimiter, "" + Letters.BACKSLASH
                        + userSettings.RecordDelimiter);
            } else {
                content = replace(content, "" + Letters.CR, "" + Letters.BACKSLASH + Letters.CR);
                content = replace(content, "" + Letters.LF, "" + Letters.BACKSLASH + Letters.LF);
            }
            if (firstColumn && content.length() > 0 && content.charAt(0) == userSettings.Comment) {
                if (content.length() > 1) {
                    content = "" + Letters.BACKSLASH + userSettings.Comment + content.substring(1);
                } else {
                    content = "" + Letters.BACKSLASH + userSettings.Comment;
                }
            }
        }
        outputStream.write(content);
        if (textQualify) {
            outputStream.write(userSettings.TextQualifier);
        }
        firstColumn = false;
    }

    /**
     * Writes another column of data to this record.&nbsp;Does not preserve leading and trailing whitespace in this column of data.
     * 
     * @param content
     *            The data for the new column.
     * @exception IOException
     *                Thrown if an error occurs while writing data to the destination stream.
     */
    public void write(String content) throws IOException {
        write(content, false);
    }

    public void writeComment(String commentText) throws IOException {
        checkClosed();
        checkInit();
        outputStream.write(userSettings.Comment);
        outputStream.write(commentText);
        if (useCustomRecordDelimiter) {
            outputStream.write(userSettings.RecordDelimiter);
        } else {
            outputStream.println();
        }
        firstColumn = true;
    }

    /**
     * Writes a new record using the passed in array of values.
     * 
     * @param values
     *            Values to be written.
     * 
     * @param preserveSpaces
     *            Whether to preserver leading and trailing spaces in columns while writing out to the record or not.
     * 
     * @throws IOException
     *             Thrown if an error occurs while writing data to the destination stream.
     */
    public void writeRecord(String[] values, boolean preserveSpaces) throws IOException {
        if (values != null && values.length > 0) {
            for (int i = 0; i < values.length; i++) {
                write(values[i], preserveSpaces);
            }
            endRecord();
        }
    }

    /**
     * Writes a new record using the passed in array of values.
     * 
     * @param values
     *            Values to be written.
     * 
     * @throws IOException
     *             Thrown if an error occurs while writing data to the destination stream.
     */
    public void writeRecord(String[] values) throws IOException {
        writeRecord(values, false);
    }

    /**
     * Ends the current record by sending the record delimiter.
     * 
     * @exception IOException
     *                Thrown if an error occurs while writing data to the destination stream.
     */
    public void endRecord() throws IOException {
        checkClosed();
        checkInit();
        if (useCustomRecordDelimiter) {
            outputStream.write(userSettings.RecordDelimiter);
        } else {
            outputStream.println();
        }
        firstColumn = true;
    }

    /**
     * 
     */
    private void checkInit() throws IOException {
        if (!initialized) {
            if (fileName != null) {
                outputStream = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileName), charset));
            }
            initialized = true;
        }
    }

    /**
     * Clears all buffers for the current writer and causes any buffered data to be written to the underlying device.
     */
    public void flush() {
        outputStream.flush();
    }

    /**
     * Closes and releases all related resources.
     */
    public void close() {
        if (!closed) {
            close(true);
            closed = true;
        }
    }

    /**
     * 
     */
    private void close(boolean closing) {
        if (!closed) {
            if (closing) {
                charset = null;
            }
            try {
                if (initialized) {
                    outputStream.close();
                }
            } catch (Exception e) {
                // just eat the exception
            }
            outputStream = null;
            closed = true;
        }
    }

    /**
     * 
     */
    private void checkClosed() throws IOException {
        if (closed) {
            throw new IOException("This instance of the CsvWriter class has already been closed.");
        }
    }

    @Override
    protected void finalize() {
        close(false);
    }

    private static class Letters {
        public static final char LF = '\n';
        public static final char CR = '\r';
        public static final char QUOTE = '"';
        public static final char COMMA = ',';
        public static final char SPACE = ' ';
        public static final char TAB = '\t';
        public static final char POUND = '#';
        public static final char BACKSLASH = '\\';
        public static final char NULL = '\0';
    }

    private static class UserSettings {
        // having these as publicly accessible members will prevent
        // the overhead of the method call that exists on properties
        public char TextQualifier;
        public boolean UseTextQualifier;
        public char Delimiter;
        public char RecordDelimiter;
        public char Comment;
        public int EscapeMode;
        public boolean ForceQualifier;
        public String NullString;

        public UserSettings() {
            TextQualifier = Letters.QUOTE;
            UseTextQualifier = true;
            Delimiter = Letters.COMMA;
            RecordDelimiter = Letters.NULL;
            Comment = Letters.POUND;
            EscapeMode = ESCAPE_MODE_DOUBLED;
            ForceQualifier = false;
            NullString = "";
        }
    }

    public static String replace(String original, String pattern, String replace) {
        final int len = pattern.length();
        int found = original.indexOf(pattern);
        if (found > -1) {
            StringBuilder sb = new StringBuilder();
            int start = 0;
            while (found != -1) {
                sb.append(original.substring(start, found));
                sb.append(replace);
                start = found + len;
                found = original.indexOf(pattern, start);
            }
            sb.append(original.substring(start));
            return sb.toString();
        } else {
            return original;
        }
    }
}