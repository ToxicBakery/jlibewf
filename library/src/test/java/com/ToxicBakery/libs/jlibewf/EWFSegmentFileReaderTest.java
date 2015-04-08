package com.ToxicBakery.libs.jlibewf;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class EWFSegmentFileReaderTest {

    private static byte[] getBytesForLong(long value) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(value);
        return buffer.array();
    }

    @Test
    public void testBytesToLong_ZeroValue() throws Exception {
        byte[] longBytes = getBytesForLong(0);
        long readValue = EWFSegmentFileReader.bytesToLong(longBytes, 0);
        Assert.assertEquals(0, readValue);
    }

    @Test
    public void testBytesToLong_MinValue() throws Exception {
        byte[] longBytes = getBytesForLong(Long.MIN_VALUE);
        long readValue = EWFSegmentFileReader.bytesToLong(longBytes, 0);
        Assert.assertEquals(Long.MIN_VALUE, readValue);
    }

    @Test
    public void testBytesToLong_MaxValue() throws Exception {
        byte[] longBytes = getBytesForLong(Long.MAX_VALUE);
        long readValue = EWFSegmentFileReader.bytesToLong(longBytes, 0);
        Assert.assertEquals(Long.MAX_VALUE, readValue);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testBytesToLong_InvalidNegativeOffset() throws Exception {
        byte[] longBytes = new byte[8];
        EWFSegmentFileReader.bytesToLong(longBytes, -1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testBytesToLong_InvalidExceedBoundsOffset() throws Exception {
        byte[] longBytes = new byte[8];
        EWFSegmentFileReader.bytesToLong(longBytes, 4);
    }

    @Test
    public void testBytesToUInt_ZeroValue() throws Exception {
        byte[] longBytes = getBytesForLong(0);
        long readValue = EWFSegmentFileReader.bytesToUInt(longBytes, 0);
        Assert.assertEquals(0, readValue);
    }

    @Test
    public void testBytesToUInt_NegativeValue() throws Exception {
        byte[] longBytes = getBytesForLong(-1);
        long readValue = EWFSegmentFileReader.bytesToUInt(longBytes, 0);
        Assert.assertEquals(Integer.MAX_VALUE * 2L + 1L, readValue);
    }

    @Test
    public void testBytesToUInt_MaxValue() throws Exception {
        long value = 2147483648L;
        byte[] longBytes = getBytesForLong(value);
        long readValue = EWFSegmentFileReader.bytesToUInt(longBytes, 0);
        Assert.assertEquals(value, readValue);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testBytesToUInt_InvalidNegativeOffset() throws Exception {
        byte[] longBytes = new byte[8];
        EWFSegmentFileReader.bytesToUInt(longBytes, -1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testBytesToUInt_InvalidExceedBoundsOffset() throws Exception {
        byte[] longBytes = new byte[8];
        EWFSegmentFileReader.bytesToUInt(longBytes, 8);
    }


    @Test
    public void testBytesToString_ProperTermination() throws Exception {
        byte[] stringBytes = {0x74, 0x65, 0x73, 0x74, 0x00, 0x74, 0x65, 0x73, 0x74};
        String output = EWFSegmentFileReader.bytesToString(stringBytes, 0, stringBytes.length);
        Assert.assertTrue("test".equals(output));
    }

    @Test
    public void testBytesToString_ProperLength() throws Exception {
        byte[] stringBytes = {0x74, 0x65, 0x73, 0x74, 0x00, 0x74, 0x65, 0x73, 0x74};
        String output = EWFSegmentFileReader.bytesToString(stringBytes, 0, 3);
        Assert.assertTrue("tes".equals(output));
    }

    @Test
    public void testBytesToString_FullLength() throws Exception {
        byte[] stringBytes = {0x74, 0x65, 0x73, 0x74, 0x74, 0x65, 0x73, 0x74};
        String output = EWFSegmentFileReader.bytesToString(stringBytes, 0, stringBytes.length);
        //noinspection SpellCheckingInspection
        Assert.assertTrue("testtest".equals(output));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testBytesToString_InvalidNegativeOffset() throws Exception {
        byte[] stringBytes = new byte[8];
        EWFSegmentFileReader.bytesToString(stringBytes, -1, 7);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testBytesToString_InvalidExceedBoundsOffset() throws Exception {
        byte[] stringBytes = new byte[8];
        EWFSegmentFileReader.bytesToString(stringBytes, 8, 8);
    }

    @Test
    public void testIsValidFirstEWFFilename_FirstFileName() throws Exception {
        File file = new File("/test.E01");
        Assert.assertTrue(EWFSegmentFileReader.isValidFirstEWFFilename(file));
    }

    @Test
    public void testIsValidFirstEWFFilename_SecondFileName() throws Exception {
        File file = new File("/test.E02");
        Assert.assertTrue(!EWFSegmentFileReader.isValidFirstEWFFilename(file));
    }

    @Test
    public void testIsValidFirstEWFFilename_NoExtension() throws Exception {
        File file = new File("/test");
        Assert.assertTrue(!EWFSegmentFileReader.isValidFirstEWFFilename(file));
    }

    @Test
    public void testIsValidFirstEWFFilename_InvalidLength() throws Exception {
        File file = new File("/t");
        Assert.assertTrue(!EWFSegmentFileReader.isValidFirstEWFFilename(file));
    }

    @Test
    public void testGetNextFile_E01() throws Exception {
        File file = new File("/test.E01");
        File nextFile = EWFSegmentFileReader.getNextFile(file);
        Assert.assertEquals("test.E02", nextFile.getName());
    }

    @Test
    public void testGetNextFile_E99() throws Exception {
        File file = new File("/test.E99");
        File nextFile = EWFSegmentFileReader.getNextFile(file);
        Assert.assertEquals("test.EAA", nextFile.getName());
    }

    @Test(expected = IOException.class)
    public void testGetNextFile_NoExtension() throws Exception {
        File file = new File("/test");
        EWFSegmentFileReader.getNextFile(file);
    }

    @Test(expected = IOException.class)
    public void testGetNextFile_InvalidLength() throws Exception {
        File file = new File("/t");
        EWFSegmentFileReader.getNextFile(file);
    }

    @Test(expected = IOException.class)
    public void testInvalidFileSignature() throws Exception {
        File file = null;
        EWFSegmentFileReader ewfSegmentFileReader = null;
        try {
            file = File.createTempFile("test", ".E01");

            final byte[] writtenBytes = new byte[]{0, 1, 2, 3, 4, 5, 6, 7};

            OutputStream outputStream = new FileOutputStream(file);
            outputStream.write(writtenBytes);
            outputStream.close();

            ewfSegmentFileReader = new EWFSegmentFileReader("%1$d (0x%1$08x)");
            ewfSegmentFileReader.readRaw(file, 0, 8);
        } finally {
            if (ewfSegmentFileReader != null) {
                ewfSegmentFileReader.closeFileChannel();
            }

            if (file != null) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
    }

    @Test
    public void testValidFileSignature() throws Exception {
        File file = null;
        try {
            file = File.createTempFile("test", ".E01");

            final byte[] writtenBytes = new byte[]{0x45, 0x56, 0x46, 0x09, 0x0d, 0x0a, (byte) 0xff, 0x00};

            OutputStream outputStream = new FileOutputStream(file);
            outputStream.write(writtenBytes);
            outputStream.close();

            EWFSegmentFileReader ewfSegmentFileReader = new EWFSegmentFileReader("%1$d (0x%1$08x)");
            byte[] readBytes = ewfSegmentFileReader.readRaw(file, 0, 8);
            Assert.assertArrayEquals(writtenBytes, readBytes);
            ewfSegmentFileReader.closeFileChannel();
        } finally {
            if (file != null) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
    }

}