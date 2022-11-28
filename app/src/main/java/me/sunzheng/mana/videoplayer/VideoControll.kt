package me.sunzheng.mana.videoplayer

interface VideoController {
    fun setBrightness(value: Float)
    fun setVolume(value: Float)
    fun seekTo(value: Long)
    fun playState()
}