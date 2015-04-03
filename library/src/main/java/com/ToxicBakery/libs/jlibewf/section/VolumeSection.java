package com.ToxicBakery.libs.jlibewf.section;

import com.ToxicBakery.libs.jlibewf.EWFIOException;
import com.ToxicBakery.libs.jlibewf.EWFSection;
import com.ToxicBakery.libs.jlibewf.EWFSegmentFileReader;

import java.io.File;
import java.io.IOException;

/**
 * An implementation of the volume section portion of a section.
 * The <code>VolumeSection</code> class contains volume section values
 * such as chunk count and sectors per chunk.
 */
public class VolumeSection {
    private static final int CHUNK_COUNT_OFFSET = 80;
    private static final int SECTORS_PER_CHUNK_OFFSET = 84;
    private static final int BYTES_PER_SECTOR_OFFSET = 88;
    private static final int SECTOR_COUNT_OFFSET = 92;
    private static final int VOLUME_SECTION_SIZE = 1128;

    /**
     * The chunk count.
     */
    private int volumeChunkCount;
    /**
     * Sectors per chunk.
     */
    private int sectorsPerChunk;
    /**
     * Bytes per Sector.
     */
    private int bytesPerSector;
    /**
     * Sector count.
     */
    private int sectorCount;

    /**
     * Constructs a volume section based on bytes from the given EWF file and address.
     *
     * @param sectionPrefix the section prefix from which this header section is composed
     * @throws IOException If an I/O error occurs, which is possible if the requested read fails
     *                     or if the Adler32 checksum validation fails
     */
    public VolumeSection(EWFSegmentFileReader reader, SectionPrefix sectionPrefix, String longFormat) throws IOException {
        File file = sectionPrefix.getFile();
        long fileOffset = sectionPrefix.getFileOffset();
        long sectionSize = sectionPrefix.getSectionSize();

        // make sure the section prefix is correct
        if (sectionPrefix.getSectionType() != EWFSection.SectionType.VOLUME_TYPE) {
            throw new RuntimeException("Invalid section type");
        }

        // make sure the section size is not smaller than the volume section data structure
        if (sectionSize < VOLUME_SECTION_SIZE) {
            throw new EWFIOException("Invalid small Volume Section size", file, fileOffset, longFormat);
        }

        // read volume section into bytes[]
        long address = fileOffset + SectionPrefix.SECTION_PREFIX_SIZE;
        int numBytes = VOLUME_SECTION_SIZE - SectionPrefix.SECTION_PREFIX_SIZE;
        byte[] bytes = reader.readAdler32(file, address, numBytes);

        // now read the Volume Section's values

        long longChunkCount = EWFSegmentFileReader.bytesToUInt(bytes, CHUNK_COUNT_OFFSET - SectionPrefix.SECTION_PREFIX_SIZE);
        // make sure the chunk count is valid before typecasting it
        if (!EWFSection.isPositiveInt(longChunkCount)) {
            throw new EWFIOException("Invalid chunk count", file, fileOffset, longFormat);
        }
        volumeChunkCount = (int) longChunkCount;

        // int sectorsPerChunk
        long longSectorsPerChunk = EWFSegmentFileReader.bytesToUInt(bytes, SECTORS_PER_CHUNK_OFFSET - SectionPrefix.SECTION_PREFIX_SIZE);
        // make sure the value is valid before typecasting it
        if (!EWFSection.isPositiveInt(longSectorsPerChunk)) {
            throw new EWFIOException("Invalid sectors per chunk", file, fileOffset, longFormat);
        }
        sectorsPerChunk = (int) longSectorsPerChunk;

        // int bytesPerSector
        long longBytesPerSector = EWFSegmentFileReader.bytesToUInt(bytes, BYTES_PER_SECTOR_OFFSET - SectionPrefix.SECTION_PREFIX_SIZE);
        // make sure the value is valid before typecasting it
        if (!EWFSection.isPositiveInt(longBytesPerSector)) {
            throw new EWFIOException("Invalid bytes per sector", file, fileOffset, longFormat);
        }
        bytesPerSector = (int) longBytesPerSector;

        // int sectorCount
        long longSectorCount = EWFSegmentFileReader.bytesToUInt(bytes, SECTOR_COUNT_OFFSET - SectionPrefix.SECTION_PREFIX_SIZE);
        // make sure the value is valid before typecasting it
        if (!EWFSection.isPositiveInt(longSectorCount)) {
            throw new EWFIOException("Invalid sector count", file, fileOffset, longFormat);
        }
        sectorCount = (int) longSectorCount;
    }

    @SuppressWarnings("unused")
    public int getVolumeChunkCount() {
        return volumeChunkCount;
    }

    public int getSectorsPerChunk() {
        return sectorsPerChunk;
    }

    public int getBytesPerSector() {
        return bytesPerSector;
    }

    @SuppressWarnings("unused")
    public int getSectorCount() {
        return sectorCount;
    }

}
