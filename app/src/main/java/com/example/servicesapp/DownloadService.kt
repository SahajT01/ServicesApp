package com.example.servicesapp

import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class DownloadService : Service() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val urls = intent?.getStringArrayListExtra("urls")

        // Show a Toast message when download starts
        handler.post {
            Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
        }

        // Start downloading files in a background thread
        thread {
            urls?.forEach { url ->
                downloadFile(url)
            }

            // Show a Toast message when download finishes
            handler.post {
                Toast.makeText(this, "Download completed", Toast.LENGTH_SHORT).show()
            }

            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Not used
        return null
    }

    private fun downloadFile(urlStr: String) {
        try {
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()

            // Check for successful response code or throw error
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                handler.post {
                    Toast.makeText(
                        this,
                        "Server returned HTTP ${connection.responseCode}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return
            }

            // Get input stream
            val input: InputStream = BufferedInputStream(connection.inputStream)
            val fileName = urlStr.substring(urlStr.lastIndexOf('/') + 1)

            // Save the file to the Downloads directory using MediaStore
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/")
            }

            val resolver = contentResolver
            val uri: Uri? = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)

            if (uri == null) {
                handler.post {
                    Toast.makeText(this, "Failed to create file", Toast.LENGTH_SHORT).show()
                }
                return
            }

            val outputStream = resolver.openOutputStream(uri)

            if (outputStream == null) {
                handler.post {
                    Toast.makeText(this, "Failed to open output stream", Toast.LENGTH_SHORT).show()
                }
                return
            }

            // Write to file
            val data = ByteArray(4096)
            var count: Int
            while (input.read(data).also { count = it } != -1) {
                outputStream.write(data, 0, count)
            }

            outputStream.flush()
            outputStream.close()
            input.close()

            // Notify file downloaded
            handler.post {
                Toast.makeText(this, "Downloaded: $fileName", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            handler.post {
                Toast.makeText(this, "Error downloading file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}