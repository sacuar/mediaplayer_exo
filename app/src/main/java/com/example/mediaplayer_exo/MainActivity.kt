package com.example.mediaplayer_exo

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Log
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ConcatenatingMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView

class MainActivity : AppCompatActivity() {

        private lateinit var playerView: PlayerView
        private lateinit var exoPlayer: ExoPlayer

        private val READ_EXTERNAL_STORAGE_PERMISSION_REQUEST = 1
        private val mediaFiles = mutableListOf<String>()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            playerView = findViewById(R.id.player_view)
            exoPlayer = ExoPlayer.Builder(this).build()
            playerView.player = exoPlayer

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    READ_EXTERNAL_STORAGE_PERMISSION_REQUEST
                )
            } else {
                loadMediaFiles()
                playMediaFiles()
            }
        }

        private fun loadMediaFiles() {
            val uri = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
            val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
            val selectionArgs = arrayOf(
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
            )
            val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

            contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                while (cursor.moveToNext()) {
                    val filePath = cursor.getString(columnIndex)
                    mediaFiles.add(filePath)
                }
            }
        }

        private fun playMediaFiles() {
            if (mediaFiles.isEmpty()) {
                Log.d("MainActivity", "No media files found.")
                return
            }

            val mediaSourceFactory = DefaultMediaSourceFactory(this)
            val concatenatingMediaSource = ConcatenatingMediaSource()

            mediaFiles.forEach { filePath ->
                val mediaItem = MediaItem.fromUri(Uri.parse(filePath))
                val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)
                concatenatingMediaSource.addMediaSource(mediaSource)
            }

            exoPlayer.setMediaSource(concatenatingMediaSource)
            exoPlayer.prepare()
            exoPlayer.play()
        }

        override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
        ) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            if (requestCode == READ_EXTERNAL_STORAGE_PERMISSION_REQUEST) {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadMediaFiles()
                    playMediaFiles()
                } else {
                    Log.d("MainActivity", "Read storage permission denied.")
                }
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            exoPlayer.release()
        }
    }
