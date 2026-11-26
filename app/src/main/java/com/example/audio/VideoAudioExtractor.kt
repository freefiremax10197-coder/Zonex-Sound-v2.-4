package com.example.audio

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import java.io.File
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object VideoAudioExtractor {

  suspend fun extractAudioIfVideo(context: Context, sourceUri: Uri): Result<Pair<Uri, String>> =
    withContext(Dispatchers.IO) {
      runCatching {
        val mime = context.contentResolver.getType(sourceUri).orEmpty()
        if (!mime.startsWith("video/")) {
          return@runCatching sourceUri to "audio_clip.mp3"
        }

        val outDir = File(context.cacheDir, "extracted_audio").apply { mkdirs() }
        val outFile = File(outDir, "extracted_${System.currentTimeMillis()}.m4a")

        val extractor = MediaExtractor()
        extractor.setDataSource(context, sourceUri, null)

        var audioTrackIndex = -1
        var audioFormat: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
          val format = extractor.getTrackFormat(i)
          val trackMime = format.getString(MediaFormat.KEY_MIME).orEmpty()
          if (trackMime.startsWith("audio/")) {
            audioTrackIndex = i
            audioFormat = format
            break
          }
        }

        if (audioTrackIndex < 0 || audioFormat == null) {
          extractor.release()
          error("Selected video has no audio track.")
        }

        extractor.selectTrack(audioTrackIndex)
        val muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val muxTrack = muxer.addTrack(audioFormat)
        muxer.start()

        val maxBufferSize =
          if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE).coerceAtLeast(64 * 1024)
          } else {
            256 * 1024
          }
        val buffer = ByteBuffer.allocate(maxBufferSize)
        val bufferInfo = android.media.MediaCodec.BufferInfo()

        while (true) {
          val sampleSize = extractor.readSampleData(buffer, 0)
          if (sampleSize < 0) break
          bufferInfo.offset = 0
          bufferInfo.size = sampleSize
          bufferInfo.presentationTimeUs = extractor.sampleTime
          @Suppress("WrongConstant")
          bufferInfo.flags = extractor.sampleFlags
          muxer.writeSampleData(muxTrack, buffer, bufferInfo)
          extractor.advance()
        }

        muxer.stop()
        muxer.release()
        extractor.release()

        Uri.fromFile(outFile) to outFile.name
      }
    }
}
