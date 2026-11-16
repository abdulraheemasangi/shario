package org.zeroshare.app.engine

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.zeroshare.app.core.constants.ProtocolConstants
import org.zeroshare.app.data.engine.ChunkResumeManager
import java.io.File
import java.io.FileOutputStream

class ChunkResumeTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testResumeOffsetCalculationAlignedToChunkBoundary() {
        val manager = ChunkResumeManager()
        val partFile = tempFolder.newFile("test_file.part")

        // Simulate 2.5 MB already written (2 full 1MB chunks + 0.5 MB partial)
        val writtenBytes = (2.5 * ProtocolConstants.CHUNK_SIZE_BYTES).toInt()
        FileOutputStream(partFile).use { fos ->
            fos.write(ByteArray(writtenBytes))
        }

        val totalExpectedSize = 10L * ProtocolConstants.CHUNK_SIZE_BYTES
        val resumeOffset = manager.calculateResumeOffset(
            fileId = 1,
            partFile = partFile,
            expectedFileSize = totalExpectedSize
        )

        // Must align to 2 full chunks (2 * 1024 * 1024 bytes)
        val expectedAlignedOffset = 2L * ProtocolConstants.CHUNK_SIZE_BYTES
        assertEquals(expectedAlignedOffset, resumeOffset.startByteOffset)
        assertEquals(2, resumeOffset.nextChunkIndex)
    }

    @Test
    fun testResumeWithNonExistentFileStartsAtZero() {
        val manager = ChunkResumeManager()
        val nonExistent = File(tempFolder.root, "does_not_exist.part")

        val resumeOffset = manager.calculateResumeOffset(
            fileId = 99,
            partFile = nonExistent,
            expectedFileSize = 5000L
        )

        assertEquals(0L, resumeOffset.startByteOffset)
        assertEquals(0, resumeOffset.nextChunkIndex)
    }
}
