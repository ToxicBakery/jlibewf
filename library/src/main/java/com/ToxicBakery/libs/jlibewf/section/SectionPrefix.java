package com.ToxicBakery.libs.jlibewf.section;

import com.ToxicBakery.libs.jlibewf.EWFIOException;
import com.ToxicBakery.libs.jlibewf.EWFSection;
import com.ToxicBakery.libs.jlibewf.EWFSegmentFileReader;

import java.io.File;
import java.io.IOException;

/**
 * An implementation of the prefix portion of a Section.
 * EWF Segment files contain sections such as "header section", "volume section",
 * or "table section".
 * Each section has a common section prefix.
 * Each section prefix defines the type of section it represents,
 * contains a pointer to the start of the next section prefix,
 * and contains an Adler32 checksum in order to verify that the byte fields
 * within the section prefix are not corrupted.
 */
public class SectionPrefix {
    /**
     * The size of the section prefix
     */
    public static final int SECTION_PREFIX_SIZE = 76;
    private static final int SECTION_TYPE_OFFSET = 0;
    private static final int NEXT_SECTION_OFFSET = 16;
    //    private static final int SECTION_ADLER32_OFFSET = 72;
    private static final int SECTION_SIZE_OFFSET = 24;
    private final String longFormat;
    /**
     * The section type.
     */
    private EWFSection.SectionType sectionType;
    /**
     * The file that this section prefix is associated with
     */
    private File file;
    /**
     * The offset address into the file where this section starts.
     */
    private long fileOffset;
    /**
     * The offset address into the file where the next section starts.
     */
    private long nextOffset;
    /**
     * The size of this section.
     */
    private long sectionSize;
    /**
     * The running chunk index.
     */
    private int chunkIndex;
    /**
     * The next chunk index.  If larger than chunkIndex, then this section contains a chunk table.
     * This is a convenience variable because <code>nextChunkIndex = chunkIndex + chunkCount</code>.
     */
    private int nextChunkIndex;
    /**
     * The chunk count.  If non-zero, then this section contains a chunk table of count chunks.
     */
    private int chunkCount;

    /**
     * Constructs a Section Prefix based on bytes from the given EWF file and address.
     *
     * @param reader             the segment file reader to use for reading EWF files
     * @param file               the file to read from
     * @param fileOffset         the byte offset address in the file to read from
     * @param previousChunkCount the running count of media chunks defined so far
     * @throws IOException If an I/O error occurs, which is possible if the read fails
     *                     or if the Adler32 checksum validation fails
     */
    public SectionPrefix(EWFSegmentFileReader reader, File file, long fileOffset, int previousChunkCount, String longFormat) throws IOException {
        final int SECTION_TYPE_STRING_LENGTH = 16;

        this.longFormat = longFormat;

        // read the section prefix bytes
        byte[] bytes = reader.readAdler32(file, fileOffset, SectionPrefix.SECTION_PREFIX_SIZE);

        // set the section type
        String sectionTypeString = EWFSegmentFileReader.bytesToString(bytes, SECTION_TYPE_OFFSET, SECTION_TYPE_STRING_LENGTH);
        try {
            sectionType = EWFSection.SectionType.getSectionType(sectionTypeString);
        } catch (IOException e) {
            throw new EWFIOException(e.getMessage(), file, fileOffset, longFormat);
        }

        // set the file
        this.file = file;

        // set the file offset
        this.fileOffset = fileOffset;

        // set the address of the next section
        nextOffset = EWFSegmentFileReader.bytesToLong(bytes, NEXT_SECTION_OFFSET);

        // set the section size
        sectionSize = EWFSegmentFileReader.bytesToLong(bytes, SECTION_SIZE_OFFSET);

        // set the chunk offset values
        chunkIndex = previousChunkCount;
        if (sectionType == EWFSection.SectionType.TABLE_TYPE) {
            // read the chunk count for this table section
            TableSection tableSection = new TableSection(reader, this, longFormat);
            nextChunkIndex = chunkIndex + tableSection.getTableChunkCount();
            chunkCount = tableSection.getTableChunkCount();
        } else {
            // leave the chunk count alone
            nextChunkIndex = chunkIndex;
            chunkCount = 0;
        }
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    public String toString() {
        return "Section Prefix type '" + sectionType.toString()
                + "' of file " + file.toString()
                + "\noffset " + String.format(longFormat, fileOffset)
                + " size " + String.format(longFormat, sectionSize)
                + " chunk index " + chunkIndex + " chunk count " + chunkCount;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public int getNextChunkIndex() {
        return nextChunkIndex;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public long getSectionSize() {
        return sectionSize;
    }

    public long getNextOffset() {
        return nextOffset;
    }

    public long getFileOffset() {
        return fileOffset;
    }

    public File getFile() {
        return file;
    }

    public EWFSection.SectionType getSectionType() {
        return sectionType;
    }

}
