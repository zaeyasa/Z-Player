package com.example.zplayer.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.zplayer.data.model.AudioFile
import com.example.zplayer.data.repository.AudioRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val repository: AudioRepository,
    context: Context
) : ViewModel() {

    private val _player = ExoPlayer.Builder(context).build()
    val player: ExoPlayer get() = _player

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSong = MutableStateFlow<AudioFile?>(null)
    val currentSong: StateFlow<AudioFile?> = _currentSong.asStateFlow()

    private val _playlist = MutableStateFlow<List<AudioFile>>(emptyList())
    val playlist: StateFlow<List<AudioFile>> = _playlist.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    
    // For seekbar max value
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _technicalDetails = MutableStateFlow("Loading Format...")
    val technicalDetails: StateFlow<String> = _technicalDetails.asStateFlow()

    init {
        _player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                // Update current song when track changes physically in player
                // Logic to match current media item to song in playlist
                val currentMediaIndex = _player.currentMediaItemIndex
                if (currentMediaIndex >= 0 && currentMediaIndex < _playlist.value.size) {
                    _currentSong.value = _playlist.value[currentMediaIndex]
                }
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _duration.value = _player.duration.coerceAtLeast(0L)
                }
            }
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                super.onTracksChanged(tracks)
                updateTechnicalDetails()
            }
        })
        
        // Polling for progress update
        viewModelScope.launch {
            while (isActive) {
                if (_player.isPlaying) {
                    _currentPosition.value = _player.currentPosition
                }
                delay(500) // Update every 500ms
            }
        }
    }

    fun loadAudioFiles() {
        viewModelScope.launch {
            val files = repository.getAudioFiles()
            _playlist.value = files
            if (files.isNotEmpty()) {
                setupPlayer(files)
            }
        }
    }

    private fun setupPlayer(files: List<AudioFile>) {
        _player.clearMediaItems()
        files.forEach { audioFile ->
            _player.addMediaItem(MediaItem.fromUri(audioFile.uri))
        }
        _player.prepare()
        // Determine initial song without auto-playing immediately if you prefer
        // _player.playWhenReady = false 
        if (files.isNotEmpty()) {
            _currentSong.value = files[0]
        }
    }

    fun playPause() {
        if (_player.isPlaying) {
            _player.pause()
        } else {
            _player.play()
        }
    }

    fun playSong(audioFile: AudioFile) {
        val index = _playlist.value.indexOf(audioFile)
        if (index != -1) {
            _player.seekTo(index, 0)
            _player.play()
            _currentSong.value = audioFile
        }
    }

    fun next() {
        if (_player.hasNextMediaItem()) {
            _player.seekToNextMediaItem()
        }
    }

    fun previous() {
        if (_player.hasPreviousMediaItem()) {
            _player.seekToPreviousMediaItem()
        } else {
             _player.seekTo(0) // Restart song if no previous
        }
    }
    
    fun seekTo(position: Long) {
        _player.seekTo(position)
        _currentPosition.value = position // Update immediately for UI responsiveness
    }

    private fun updateTechnicalDetails() {
        val groups = _player.currentTracks.groups
        var format: androidx.media3.common.Format? = null

        for (i in 0 until groups.size) {
            val group = groups[i]
            if (group.type == androidx.media3.common.C.TRACK_TYPE_AUDIO && group.isSelected) {
                 // Get the format of the selected track in this audio group
                 for (j in 0 until group.length) {
                     if (group.isTrackSelected(j)) {
                         format = group.getTrackFormat(j)
                         break
                     }
                 }
            }
            if (format != null) break
        }

        if (format == null) {
            // Fallback to audioFormat if tracks analysis fails, or just general properties
            format = _player.audioFormat
        }

        if (format != null) {
            val sb = StringBuilder()
            
            // Format / MimeType
            val mime = format.sampleMimeType ?: "Unknown"
            val type = when {
                mime.contains("mpeg") -> "MP3"
                mime.contains("flac") -> "FLAC"
                mime.contains("mp4") || mime.contains("aac") -> "AAC"
                mime.contains("raw") || mime.contains("wav") -> "WAV"
                mime.contains("vorbis") -> "OGG"
                else -> mime.substringAfter("/")
            }.uppercase()
            sb.append(type)
            
            // Sample Rate
            if (format.sampleRate != -1) {
                sb.append(" / ")
                val khz = format.sampleRate / 1000f
                // Format directly to remove trailing zeros if integer
                if (khz % 1.0f == 0f) {
                     sb.append("${khz.toInt()}kHz")
                } else {
                     sb.append("${khz}kHz")
                }
            }
            
            // Bit Depth (Estimate)
            val encoding = format.pcmEncoding
            val bitDepth = when (encoding) {
                androidx.media3.common.C.ENCODING_PCM_8BIT -> "8-bit"
                androidx.media3.common.C.ENCODING_PCM_16BIT -> "16-bit"
                androidx.media3.common.C.ENCODING_PCM_24BIT -> "24-bit"
                androidx.media3.common.C.ENCODING_PCM_32BIT -> "32-bit"
                androidx.media3.common.C.ENCODING_PCM_FLOAT -> "32-bit Float"
                else -> null
            }
            
            if (bitDepth != null) {
                sb.append(" / $bitDepth")
            }
            
            _technicalDetails.value = sb.toString()
        } else {
             // Deep Fallback: guess from extension
            val current = _currentSong.value
            if (current != null) {
                 val path = current.uri.path
                 val ext = path?.substringAfterLast('.', "")?.uppercase()
                 if (!ext.isNullOrEmpty()) {
                     _technicalDetails.value = "$ext / Ready"
                 } else {
                     _technicalDetails.value = "Unknown / Ready"
                 }
            } else {
                _technicalDetails.value = "No Audio"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        _player.release()
    }
}

class PlayerViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlayerViewModel(AudioRepository(context), context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
