package com.ToxicBakery.libs.jlibewf.section;

import com.ToxicBakery.libs.jlibewf.EWFIOException;
import com.ToxicBakery.libs.jlibewf.EWFSection;
import com.ToxicBakery.libs.jlibewf.EWFSegmentFileReader;

import java.io.File;
import java.io.IOException;

/**
 * An implementation of the header section portion of a section.
 * The header section contains metadata about the series of EWF Segment files
 * such as case number and examiner name.
 * The header section portion does not include the section prefix.
 * <p>This class currently simply provides header text as a single string.
 * It may be improved to provide individual header fields.
 */
public class HeaderSection {
    private static final int HEADER_SECTION_OFFSET = 76;

    /**
     * The header text.
     * This class may be enhanced to provide formatted header fields.
     */
    private String headerText;

    /**
     * Reads the header section associated with the given section prefix
     * The header section bytes are decompressed and validated using zlib.
     *
     * @param sectionPrefix the section prefix from which this header section is composed
     * @throws IOException If an I/O error occurs, which is possible if the requested read fails
     */
    public HeaderSection(EWFSegmentFileReader reader, SectionPrefix sectionPrefix, String longFormat) throws IOException {
        File file = sectionPrefix.getFile();
        long fileOffset = sectionPrefix.getFileOffset();
        long sectionSize = sectionPrefix.getSectionSize();

        // make sure the section prefix is correct
        if (sectionPrefix.getSectionType() != EWFSection.SectionType.HEADER_TYPE) {
            throw new RuntimeException("Invalid section type");
        }

        // make sure the header section size is small enough to cast to int
        if (!EWFSection.isPositiveInt(sectionSize)) {
            throw new EWFIOException("Invalid large Header Section size", file, fileOffset, longFormat);
        }

        // read decompressed header section into bytes[]
        long address = fileOffset + HEADER_SECTION_OFFSET;
        int numBytes = (int) sectionSize - HEADER_SECTION_OFFSET;
        byte[] bytes = reader.readZLib(file, address, numBytes, EWFSegmentFileReader.DEFAULT_CHUNK_SIZE);

        // convert bytes to String
        headerText = new String(bytes);
    }

    /**
     * Returns all the header section text as a single string.
     *
     * @return the header section text as a single string
     */
    public String getHeaderText() {
        return headerText;
    }
}
