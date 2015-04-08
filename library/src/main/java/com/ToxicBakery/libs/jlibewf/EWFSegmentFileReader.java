package com.ToxicBakery.libs.jlibewf;

/*
 * The software provided here is released by the Naval Postgraduate
 * School, an agency of the U.S. Department of Navy.  The software
 * bears no warranty, either expressed or implied. NPS does not assume
 * legal liability nor responsibility for a User's use of the software
 * or the results of such use.
 *
 * Please note that within the United States, copyright protection,
 * under Section 105 of the United States Code, Title 17, is not
 * available for any work of the United States Government and/or for
 * any works created by United States Government employees. User
 * acknowledges that this software contains work which was created by
 * NPS government employees and is therefore in the public domain and
 * not subject to copyright.
 *
 * Released into the public domain on December 17, 2010 by Bruce Allen.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.zip.Adler32;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * The <code>com.ToxicBakery.libs.jlibewf.EWFSegmentFileReader</code> class provides accessors for reading EWF files formatted
 * in the .E01 format.
 */
public class EWFSegmentFileReader {

    /**
     * The start address of the first section in an EWF file
     */
    public static final int FILE_FIRST_SECTION_START_ADDRESS = 13;
    /**
     * The default chunk size of media chunks
     */
    public static final int DEFAULT_CHUNK_SIZE = 64 * 512;
    /**
     * EWF file signature magic number and file offset
     */
    private static final byte[] EWF_SIGNATURE = {0x45, 0x56, 0x46, 0x09, 0x0d, 0x0a, (byte) 0xff, 0x00};
    private static final byte[] SERIAL_E99 = new byte[]{'E', '9', '9'};
    private static final byte[] SERIAL_EAA = new byte[]{'E', 'A', 'A'};

    /**
     * object for calculating an Adler32 checksum
     */
    private final Adler32 adler32;

    /**
     * inflater for decompressing chunks
     */
    private final Inflater inflater;

    private final String longFormat;
    private File currentOpenedFile;
    private FileInputStream currentOpenedFileInputStream;
    private FileChannel currentOpenedFileChannel;

    /**
     * Sets the format for formatting long to string.
     * This format is used for preparing log reports containing long numbers.
     * The default value is <code>"%1$d (0x%1$08x)"</code>
     */
    EWFSegmentFileReader(String longFormat) {
        adler32 = new Adler32();
        inflater = new Inflater();
        this.longFormat = longFormat;
    }

    /**
     * Returns a <code>long</code> in little-endian form from the specified bytes.
     *
     * @param bytes       the bytes from which the <code>long</code> value is to be extracted
     * @param arrayOffset the offset in the byte array from which the value is to be extracted
     * @return the <code>long</code> value
     * @throws IndexOutOfBoundsException of there are insufficient bytes to perform the conversion
     */
    public static long bytesToLong(byte[] bytes, int arrayOffset) {
        // validate that the bytes are available
        if (arrayOffset < 0 || bytes.length < arrayOffset + 8) {
            throw new IndexOutOfBoundsException();
        }

        // read the long
        return (bytes[arrayOffset] & 0xFFL)
                + ((bytes[arrayOffset + 1] & 0xFFL) << 8)
                + ((bytes[arrayOffset + 2] & 0xFFL) << 16)
                + ((bytes[arrayOffset + 3] & 0xFFL) << 24)
                + ((bytes[arrayOffset + 4] & 0xFFL) << 32)
                + ((bytes[arrayOffset + 5] & 0xFFL) << 40)
                + ((bytes[arrayOffset + 6] & 0xFFL) << 48)
                + ((bytes[arrayOffset + 7] & 0xFFL) << 56);
    }

    /**
     * Reads four bytes in little-endian form from the array and returns it as an unsigned int
     * inside a <code>long</code>.
     *
     * @param bytes       the bytes from which the unsigned integer value is to be extracted
     * @param arrayOffset the offset in the byte array from which the value is to be extracted
     * @return the unsigned integer value in a <code>long</code>.
     * @throws IndexOutOfBoundsException of there are insufficient bytes to perform the conversion
     */
    public static long bytesToUInt(byte[] bytes, int arrayOffset) {
        // validate that the bytes are available
        if (arrayOffset < 0 || bytes.length < arrayOffset + 4) {
            throw new IndexOutOfBoundsException();
        }

        // read the unsigned Integer
        return (bytes[arrayOffset] & 0xFFL)
                + ((bytes[arrayOffset + 1] & 0xFFL) << 8)
                + ((bytes[arrayOffset + 2] & 0xFFL) << 16)
                + ((bytes[arrayOffset + 3] & 0xFFL) << 24);
    }

    /**
     * Returns a <code>String</code> from the specified bytes.
     *
     * @param bytes       the bytes from which the <code>int</code> value is to be extracted
     * @param arrayOffset the offset in the byte array from which the <code>String</code>
     *                    is to be extracted
     * @param length      the maximum length to parse if the /0 terminator is not present
     * @return the <code>String</code> value residing at the requested bytes
     * @throws IndexOutOfBoundsException of there are insufficient bytes to perform the conversion
     */
    public static String bytesToString(byte[] bytes, int arrayOffset, int length) {
        // validate that the bytes are available
        if (arrayOffset < 0 || bytes.length < arrayOffset + length) {
            throw new IndexOutOfBoundsException();
        }

        // read the bytes, stopping at end or at /0 within length
        int i;
        for (i = 0; i < length; i++) {
            if (bytes[arrayOffset + i] == 0) {
                break;
            }
        }

        // return String from range or from /0 terminated portion
        return new String(bytes, 0, i);
    }

    /**
     * Returns a printable report of the specified bytes.
     *
     * @param bytes the bytes to format
     * @return the formatted byte log
     */
    private static String makeByteLog(String text, byte[] bytes) {
        StringBuilder buffer = new StringBuilder();

        // generate text
        buffer.append("\n");
        buffer.append(text);
        buffer.append(", size: ").append(bytes.length);
        buffer.append("\n");

        int i;

        // add bytes as text
        for (i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            if (b > 31 && b < 127) {
                buffer.append((char) b);    // printable
            } else {
                buffer.append(".");    // not printable
            }
        }
        buffer.append("\n");

        // add bytes as hex
        for (i = 0; i < bytes.length; i++) {
            buffer.append(String.format("%1$02x", bytes[i]));
            buffer.append(" ");
        }
        buffer.append("\n");

        // return the byte log as a string
        return buffer.toString();
    }

    /**
     * Indicates whether the specified filename is a valid first EWF filename
     *
     * @param file is the filename to validate as a valid first EWF filename
     * @return true if the filename is a valid first EWF filename
     */
    public static boolean isValidFirstEWFFilename(File file) {
        // convert File to String
        String fileString = file.getAbsolutePath();

        // validate that there is space for the suffix and that the "." is in the right place
        int length = fileString.length();
        if (length < 4 || fileString.charAt(length - 4) != '.') {
            return false;
        }

        // get the suffix
        byte[] byteSuffix = fileString.substring(length - 3, length).getBytes();

        // validate each suffix byte
        return byteSuffix[0] == 'E'
                && byteSuffix[1] == '0'
                && byteSuffix[2] == '1';
    }

    /**
     * Returns the next serial file in the EWF file naming sequence
     *
     * @param previousFile the previous serial file in the EWF file naming sequence
     * @throws IOException If an I/O error occurs, which is possible if the next file
     *                     cannot be formulated from the previous file
     */
    public static File getNextFile(File previousFile) throws IOException {

        // convert File to String
        String fileString = previousFile.getAbsolutePath();
        int length = fileString.length();

        // validate that there is space for the suffix and that the "." is in the right place
        if (length < 4 || fileString.charAt(length - 4) != '.') {
            throw new IOException("Invalid E01 filename: filename too short: " + fileString);
        }

        // get the prefix and the suffix
        String stringPrefix = fileString.substring(0, length - 4);
        byte[] byteSuffix = fileString.substring(length - 3, length).getBytes();

        // increment byteSuffix according to the .E01 specs
        if (byteSuffix[0] == SERIAL_E99[0]
                && byteSuffix[1] == SERIAL_E99[1]
                && byteSuffix[2] == SERIAL_E99[2]) {
            // E99 transitions to EAA
            byteSuffix[0] = SERIAL_EAA[0];
            byteSuffix[1] = SERIAL_EAA[1];
            byteSuffix[2] = SERIAL_EAA[2];
        } else if (byteSuffix[1] >= '0' && byteSuffix[1] <= '9') {
            // manage digits
            if (byteSuffix[2] == '9') {
                byteSuffix[2] = '0';
                byteSuffix[1]++;
            } else {
                byteSuffix[2]++;
            }
        } else {
            // manage letters
            byteSuffix[2]++;
            if (byteSuffix[2] == '[') {
                byteSuffix[2] = 'A';
                byteSuffix[1]++;
                if (byteSuffix[1] == '[') {
                    byteSuffix[1] = 'A';
                    byteSuffix[0]++;
                }
            }
        }

        // compose the filename and return the next file
        String nextFilename = stringPrefix + "." + new String(byteSuffix);
        return new File(nextFilename);
    }

    /**
     * Indicates whether the bytes match the E01 file signature
     *
     * @param fileChannel the channel that is to be validated
     * @return true if the signature matches
     * @throws IOException if the signature cannot be read from the file channel
     */
    private static boolean isValidE01Signature(FileChannel fileChannel) throws IOException {
        // map and read the file's EWF signature into ewfSignature
        MappedByteBuffer mappedByteBuffer;
        byte[] ewfSignature = new byte[EWF_SIGNATURE.length];
        mappedByteBuffer = fileChannel.map(MapMode.READ_ONLY, 0, EWF_SIGNATURE.length);
        mappedByteBuffer.load();
        mappedByteBuffer.get(ewfSignature, 0, ewfSignature.length);

        // validate the file's EWF signature
        for (int i = 0; i < EWF_SIGNATURE.length; i++) {
            if (ewfSignature[i] != EWF_SIGNATURE[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Opens a <code>FileChannel</code> for a file and verifies that it is of the correct file type.
     * If the previous open was the same file, then the already opened file channel is returned.
     *
     * @param file The file to open the file channel for
     * @return The file channel associated with the file
     * @throws IOException If an I/O error occurs, which is possible if the file is invalid
     *                     or if the file's EWF signature is invalid
     */
    private FileChannel openFileChannel(File file) throws IOException {

        if (file == currentOpenedFile) {
            // the file channel is already active
            return currentOpenedFileChannel;

        } else {
            // close any currently opened file
            closeFileChannel();

            // open file
            currentOpenedFile = file;
            currentOpenedFileInputStream = new FileInputStream(file);
            currentOpenedFileChannel = currentOpenedFileInputStream.getChannel();

            // since the file is being opened, validate its signature
            if (!isValidE01Signature(currentOpenedFileChannel)) {
                throw new IOException("Invalid E01 file signature");
            }

            return currentOpenedFileChannel;
        }
    }

    /**
     * Closes the currently open file channel, if open, releasing resources.
     */
    void closeFileChannel() throws IOException {

        // close the file channel and the file input stream, and clear the current file references
        if (currentOpenedFileChannel != null) {
            currentOpenedFileChannel.close();
            currentOpenedFileChannel = null;
        }
        if (currentOpenedFileInputStream != null) {
            currentOpenedFileInputStream.close();
            currentOpenedFileInputStream = null;
        }
        currentOpenedFile = null;
    }

    /**
     * Returns the bytes from the specified EWF file and offset.
     * An IOException is thrown if the byte buffer cannot be read or completely filled.
     *
     * @param file       the file to read from
     * @param fileOffset the byte offset address in the file to read from
     * @param numBytes   the number of bytes to read
     * @throws IOException If an I/O error occurs, which is possible if the requested read fails
     */
    public byte[] readRaw(File file, long fileOffset, int numBytes) throws IOException {

        // open the file channel for the file
        FileChannel fileChannel = openFileChannel(file);

        try {

            // map and load the byte range
            MappedByteBuffer mappedByteBuffer;
            mappedByteBuffer = fileChannel.map(MapMode.READ_ONLY, fileOffset, numBytes);
            mappedByteBuffer.load();
            byte[] bytes = new byte[numBytes];
            mappedByteBuffer.get(bytes, 0, numBytes);
            return bytes;

        } catch (IOException e) {
            // the read failed
            throw new EWFIOException("Unable to read from file", file, fileOffset, longFormat);
        }
    }

    /**
     * Returns the bytes from the specified EWF file and offset.
     * The last four bytes are the Adler32 checksum, which is checked.
     * An IOException is thrown if the bytes cannot be read or if the Adler32 checksum fails.
     *
     * @param file       the file to read from
     * @param fileOffset the byte offset address in the file to read from
     * @param numBytes   the number of bytes to read
     * @throws IOException If the bytes cannot be read or if the Adler32 checksum fails
     */
    public byte[] readAdler32(File file, long fileOffset, int numBytes) throws IOException {
        // verify proper input
        if (numBytes <= 4) {
            throw new EWFIOException("Invalid Adler32 read too short: " + numBytes + " bytes", file, fileOffset, longFormat);
        }

        // read the raw bytes
        byte[] bytes = readRaw(file, fileOffset, numBytes);

        // calculate the Adler32 checksum
        adler32.reset();
        adler32.update(bytes, 0, numBytes - 4);

        // check the Adler32 checksum
        long expectedValue = bytesToUInt(bytes, bytes.length - 4);
        if (adler32.getValue() != expectedValue) {
            EWFFileReader.logger.error("Invalid Adler32 checksum: Calculated value "
                    + String.format(longFormat, adler32.getValue())
                    + " is not equal to expected value "
                    + String.format(longFormat, expectedValue)
                    + "\n" + makeByteLog("Bytes failing Adler32 checksum", bytes));
            throw new EWFIOException("Invalid Adler32 checksum on " + numBytes + " bytes", file, fileOffset, longFormat);
        }

        // return the requested bytes
        return bytes;
    }

    /**
     * Returns the decompressed bytes from the specified EWF file and offset.
     * The bytes must properly decompress.
     * An IOException is thrown if the bytes cannot be read or if the decompression fails.
     *
     * @param file       the file to read from
     * @param fileOffset the byte offset address in the file to read from
     * @param numBytes   the number of bytes to read
     * @throws IOException If the bytes cannot be read or if the decompression fails
     */
    public byte[] readZLib(File file, long fileOffset, int numBytes, int chunkSize) throws IOException {

        // read the raw bytes
        byte[] inBytes = readRaw(file, fileOffset, numBytes);

        // allocate temp space for the deflated bytes
        byte[] outBytes = new byte[chunkSize];

        // reset the inflater
        inflater.reset();

        // run the inflater
        inflater.setInput(inBytes, 0, inBytes.length);

        // get the output in outBytes
        int decompressedLength;
        try {
            decompressedLength = inflater.inflate(outBytes);
        } catch (DataFormatException e) {
            // the compressed data format is invalid
            throw new EWFIOException(e.getMessage(), file, fileOffset, longFormat);
        }

        if (!inflater.finished()) {
            // fail on error
            throw new EWFIOException("Inflater not finished: "
                    + inflater.getTotalIn() + " in, " + inflater.getTotalOut() + " out, "
                    + inflater.getRemaining() + " remaining."
                    + "  Needs input = " + inflater.needsInput(),
                    file, fileOffset, longFormat);
        }

        // return the deflated bytes
        if (decompressedLength == chunkSize) {
            // return the array
            return outBytes;
        } else {
            //  Copy to a new buffer of the correct size
            byte[] cpy = new byte[decompressedLength];
            System.arraycopy(outBytes, 0, cpy, 0, decompressedLength);
            return cpy;
        }
    }

}

