package com.ruuvi.station.image

import android.content.Context
import android.content.ContentResolver
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.After
import org.junit.Test
import java.io.IOException
import java.io.File
import kotlin.io.path.createTempDirectory

class ImageInteractorTest {
    private val context = mockk<Context>()
    private val resources = mockk<Resources>()
    private val filesDirectory = createTempDirectory("image-interactor").toFile()
    private val interactor = ImageInteractor(context)

    @After
    fun tearDown() {
        filesDirectory.deleteRecursively()
        unmockkStatic(Bitmap::class, BitmapFactory::class)
    }

    @Test
    fun `saving a default background preserves the bundled jpeg bytes`() {
        val source = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())
        every { context.resources } returns resources
        every { context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) } returns filesDirectory
        every { resources.openRawResource(RESOURCE_ID) } returns source.inputStream()

        val savedImage = requireNotNull(
            interactor.saveResourceAsFile(SENSOR_ID, RESOURCE_ID)
        )

        try {
            assertArrayEquals(source, savedImage.readBytes())
        } finally {
            savedImage.delete()
        }
    }

    @Test
    fun `saving a default background removes a partial file when reading fails`() {
        every { context.resources } returns resources
        every { context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) } returns filesDirectory
        every { resources.openRawResource(RESOURCE_ID) } throws IOException("Resource unavailable")

        assertNull(interactor.saveResourceAsFile(SENSOR_ID, RESOURCE_ID))
        assertEquals(emptyList<String>(), filesDirectory.list()?.toList())
    }

    @Test
    fun `jpeg quality is configured for maximum fidelity`() {
        assertEquals(100, JPEG_QUALITY)
    }

    @Test
    fun `legacy decoder requests ARGB 8888 output`() {
        val contentResolver = mockk<ContentResolver>()
        val imageUri = mockk<Uri>()
        val expected = mockk<Bitmap>()
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(imageUri) } returns byteArrayOf().inputStream()
        mockkStatic(BitmapFactory::class)
        val options = slot<BitmapFactory.Options>()
        every {
            BitmapFactory.decodeStream(any(), null, capture(options))
        } returns expected

        assertSame(expected, interactor.getImage(imageUri))
        assertEquals(Bitmap.Config.ARGB_8888, options.captured.inPreferredConfig)
    }

    @Test
    fun `resizing backgrounds uses maximum JPEG quality`() {
        val imageUri = mockk<Uri>()
        val source = mockk<Bitmap>(relaxed = true)
        val output = mockk<Bitmap>(relaxed = true)
        every { source.width } returns 40
        every { source.height } returns 60
        every { output.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, any()) } returns true
        mockkStatic(Bitmap::class)
        every { Bitmap.createScaledBitmap(source, 40, 60, false) } returns output
        val imageInteractor = spyk(interactor)
        every { imageInteractor.getImage(imageUri) } returns source
        every { imageInteractor.rotate(source, 0f) } returns source
        val destination = File.createTempFile("resized", ".jpg", filesDirectory)

        try {
            imageInteractor.resize(destination.absolutePath, imageUri, rotation = 0)

            verify { output.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, any()) }
        } finally {
            destination.delete()
        }
    }

    private companion object {
        const val SENSOR_ID = "AA:BB:CC:DD:EE:FF"
        const val RESOURCE_ID = 1
    }
}
