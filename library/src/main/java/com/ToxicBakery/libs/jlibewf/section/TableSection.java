package com.ToxicBakery.libs.jlibewf.section;

import com.ToxicBakery.libs.jlibewf.EWFIOException;
import com.ToxicBakery.libs.jlibewf.EWFSection;
import com.ToxicBakery.libs.jlibewf.EWFSegmentFileReader;

import java.io.File;
import java.io.IOException;

/**
 * An implementation of the table section portion of a section.
 * The table section contains the chunk count and table base offset.
 */
public class TableSection {
    /**
     * the offset into the chunk table offset array.
     */
    public static final int OFFSET_ARRAY_OFFSET = 100;
    private static final int CHUNK_COUNT_OFFSET = 76;
    private static final int TABLE_BASE_OFFSET = 84;
    /**
     * the number of chunks in this table section.
     */
    private int tableChunkCount;
    /**
     * the base offset of the integer chunk offsets with respect to the beginning of the file.
     */
    private long tableBaseOffset;

    /**
     * Constructs a table section based on bytes from the given EWF file and address.
     *
     * @param reader        the EWF reader instance to use for reading
     * @param sectionPrefix the section prefix from which this header section is composed
     * @throws IOException If an I/O error occurs, which is possible if the requested read fails
     */
    public TableSection(EWFSegmentFileReader reader, SectionPrefix sectionPrefix, String longFormat) throws IOException {

        File file = sectionPrefix.getFile();
        long fileOffset = sectionPrefix.getFileOffset();
        long sectionSize = sectionPrefix.getSectionSize();

        // make sure the section prefix is correct
        if (sectionPrefix.getSectionType() != EWFSection.SectionType.TABLE_TYPE) {
            throw new RuntimeException("Invalid section type");
        }

        // validate section size
        if (sectionSize < OFFSET_ARRAY_OFFSET) {
            throw new EWFIOException("table section chunk count size is too small", file, fileOffset, longFormat);
        }

        // read the table section
        long address = fileOffset + SectionPrefix.SECTION_PREFIX_SIZE;
        int numBytes = OFFSET_ARRAY_OFFSET - SectionPrefix.SECTION_PREFIX_SIZE;
        byte[] bytes = reader.readAdler32(file, address, numBytes);

        // set table section values

        // tableChunkCount
        long longChunkCount = EWFSegmentFileReader.bytesToUInt(bytes, CHUNK_COUNT_OFFSET - SectionPrefix.SECTION_PREFIX_SIZE);
        // make sure the value is valid before typecasting it
        if (!EWFSection.isPositiveInt(longChunkCount)) {
            throw new EWFIOException("Invalid chunk count", file, fileOffset, longFormat);
        }
        tableChunkCount = (int) longChunkCount;

        // tableBaseOffset
        tableBaseOffset = EWFSegmentFileReader.bytesToLong(bytes, TABLE_BASE_OFFSET - SectionPrefix.SECTION_PREFIX_SIZE);
    }

    /**
     * Provides a visual representation of this object.
     */
    public String toString() {
        return "TableSection: tableChunkCount: " + tableChunkCount + " tableBaseOffset: " + tableBaseOffset;
    }

    public int getTableChunkCount() {
        return tableChunkCount;
    }

    public long getTableBaseOffset() {
        return tableBaseOffset;
    }

}
