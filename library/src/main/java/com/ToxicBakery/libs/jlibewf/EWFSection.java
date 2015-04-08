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

import com.ToxicBakery.libs.jlibewf.section.SectionPrefix;
import com.ToxicBakery.libs.jlibewf.section.TableSection;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The <code>com.ToxicBakery.libs.jlibewf.EWFSection</code> class provides objects that map to section structures
 * in EWF files formatted in the .E01 format.
 */
public class EWFSection {

    private static final Map<String, SectionType> MAP = new HashMap<>();

    /**
     * Indicates whether the specified long is a positive integer.
     * Use this before typecasting from <code>long</code> to <code>int</code>.
     *
     * @param longInt the long that should be a positive integer
     * @return true if the value is a positive integer
     */
    public static boolean isPositiveInt(long longInt) {
        return longInt <= Integer.MAX_VALUE && longInt >= 0;
    }

    /**
     * The <code>SectionType</code> class manages valid section types.
     */
    public static final class SectionType {
        /**
         * The Header Section type.
         */
        public static final SectionType HEADER_TYPE = new SectionType("header");
        /**
         * The Volume Section type.
         */
        public static final SectionType VOLUME_TYPE = new SectionType("volume");
        /**
         * The Table Section type.
         */
        public static final SectionType TABLE_TYPE = new SectionType("table");
        /**
         * The Next Section type.
         */
        public static final SectionType NEXT_TYPE = new SectionType("next");
        /**
         * The Done Section type.
         */
        public static final SectionType DONE_TYPE = new SectionType("done");
        /**
         * header2 Section type.
         */
        @SuppressWarnings("unused")
        public static final SectionType HEADER2_TYPE = new SectionType("header2");
        /**
         * Disk Section type.
         */
        @SuppressWarnings("unused")
        public static final SectionType DISK_TYPE = new SectionType("disk");
        /**
         * Data Section type.
         */
        @SuppressWarnings("unused")
        public static final SectionType DATA_TYPE = new SectionType("data");
        /**
         * Sectors Section type.
         */
        @SuppressWarnings("unused")
        public static final SectionType SECTORS_TYPE = new SectionType("sectors");
        /**
         * Table2 Section type.
         */
        @SuppressWarnings("unused")
        public static final SectionType TABLE2_TYPE = new SectionType("table2");
        /**
         * LTree Section type.
         */
        @SuppressWarnings("unused")
        public static final SectionType LTREE_TYPE = new SectionType("ltree");
        /**
         * Digest Section type.
         */
        @SuppressWarnings("unused")
        public static final SectionType DIGEST_TYPE = new SectionType("digest");
        /**
         * Hash Section type.
         */
        @SuppressWarnings("unused")
        public static final SectionType HASH_TYPE = new SectionType("hash");

        // the name of the Section type.
        private String sectionTypeString;

        private SectionType(String sectionTypeString) {
            // make sure the type is not added twice
            if (MAP.containsKey(sectionTypeString)) {
                throw new RuntimeException("Duplicate SectionType constant");
            }

            // add the section type
            this.sectionTypeString = sectionTypeString;
            MAP.put(sectionTypeString, this);
        }

        /**
         * Returns the SectionType object associated with this section type.
         *
         * @return the SectionType object associated with this section type.
         * @throws IOException if the requested section type is not a valid section type
         */
        public static SectionType getSectionType(String sectionTypeString) throws IOException {
            SectionType sectionType = MAP.get(sectionTypeString);

            // It is an IO exception if the section type is not recognized
            if (sectionType == null) {
                throw new IOException("Invalid Section type: '" + sectionTypeString + "'");
            }
            return sectionType;
        }

        /**
         * Provides a visual representation of this object.
         *
         * @return the string name of this section type object.
         */
        public String toString() {
            return sectionTypeString;
        }
    }

    /**
     * An implementation of the chunk table portion of a table section.
     * The table section contains accessors for reading a chunk entry.
     */
    public static class ChunkTable {

        private final String longFormat;
        private final int chunkCount;
        private final File file;
        private final long fileOffset;

        private byte[] bytes;

        /**
         * Constructs a chunk table from the given section prefix.
         *
         * @param sectionPrefix the section prefix from which this header section is composed
         * @throws IOException If an I/O error occurs, which is possible if the requested read fails
         */
        public ChunkTable(EWFSegmentFileReader reader, SectionPrefix sectionPrefix, String longFormat) throws IOException {

            this.longFormat = longFormat;
            file = sectionPrefix.getFile();
            fileOffset = sectionPrefix.getFileOffset();
            chunkCount = sectionPrefix.getChunkCount();

            // make sure the section prefix is correct
            if (sectionPrefix.getSectionType() != SectionType.TABLE_TYPE) {
                throw new RuntimeException("Invalid section type");
            }

            // validate section size
            if (sectionPrefix.getSectionSize() < TableSection.OFFSET_ARRAY_OFFSET + chunkCount * 4) {
                throw new EWFIOException("table section chunk table size is too small", file, fileOffset, longFormat);
            }

            // read the chunk table into bytes[]
            long address = fileOffset + TableSection.OFFSET_ARRAY_OFFSET;
            int numBytes;
            if (sectionPrefix.getSectionSize() >= TableSection.OFFSET_ARRAY_OFFSET + chunkCount * 4 + 4) {

                // add 4 bytes for the Adler32 checksum for the chunk table
                numBytes = chunkCount * 4 + 4;

                // read table with Adler32 checksum bytes
                bytes = reader.readAdler32(file, address, numBytes);

                // Although not required by the spec, note if there are extra bytes in the table section
                if (sectionPrefix.getSectionSize() != TableSection.OFFSET_ARRAY_OFFSET + chunkCount * 4 + 4) {
                    EWFFileReader.logger.info("com.ToxicBakery.libs.jlibewf.EWFSection.ChunkTable: Note: File " + file.toString()
                            + " chunk table contains extra bytes at section " + sectionPrefix.toString());
                }

            } else if (sectionPrefix.getSectionSize() == TableSection.OFFSET_ARRAY_OFFSET + chunkCount * 4) {

                // old way has no Adler32 checksum for the chunk table
                numBytes = chunkCount * 4;

                // read table without Adler32 checksum bytes
                bytes = reader.readRaw(file, address, numBytes);

                // note that the old format with no checksum was encountered
                EWFFileReader.logger.info("com.ToxicBakery.libs.jlibewf.EWFSection.ChunkTable: Note: File " + file.toString()
                        + " does not use an offset array Adler32 checksum at section "
                        + sectionPrefix.toString());

            } else {
                // too large for no Adler32 but too small for Adler32
                throw new EWFIOException("invalid table section size for chunk table", file, fileOffset, longFormat);
            }
        }

        /**
         * Returns the chunk start offset at the given index
         *
         * @param chunkTableIndex the index within this chunk table
         * @return the chunk start offset from the table
         */
        public long getChunkStartOffset(int chunkTableIndex) throws EWFIOException {
            // make sure the chunk index is valid
            if (chunkTableIndex >= chunkCount) {
                throw new EWFIOException("Invalid chunk index: " + chunkTableIndex, file, fileOffset, longFormat);
            }

            // get the file offset to the media chunk base
            long chunkStartOffset = EWFSegmentFileReader.bytesToUInt(bytes, chunkTableIndex * 4);

            // strip out any MSB used to indicate compressed zlib encoding
            chunkStartOffset &= 0x7fffffff;

            return chunkStartOffset;
        }

        /**
         * Returns whether the given chunk is compressed
         *
         * @param chunkTableIndex the index within this chunk table
         * @return true if the given chunk is compressed, false if not
         */
        public boolean isCompressedChunk(int chunkTableIndex) throws EWFIOException {
            // make sure the chunk index is valid
            if (chunkTableIndex >= chunkCount) {
                throw new EWFIOException("Invalid chunk index: " + chunkTableIndex, file, fileOffset, longFormat);
            }

            // get the file offset to the media chunk base
            long chunkStartOffset = EWFSegmentFileReader.bytesToUInt(bytes, chunkTableIndex * 4);

            // determine if compression is used for the chunk
            return (chunkStartOffset & 0x80000000) != 0;
        }

    }
}

