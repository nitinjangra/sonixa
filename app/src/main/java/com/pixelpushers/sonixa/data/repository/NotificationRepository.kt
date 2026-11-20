package com.pixelpushers.sonixa.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class NotificationCategory(val displayName: String) {
    PLAYBACK_ISSUES("Playback issues"),
    FUN_FACTS("Fun facts"),
    REMINDERS("Reminders"),
    RELEASE_NOTES("Release notes"),
    SYSTEM("System")
}

data class AppNotification(
    val id: Long = System.currentTimeMillis() + (0..1000).random(),
    val title: String,
    val message: String,
    val category: NotificationCategory,
    val timestamp: Long = System.currentTimeMillis()
)

@Singleton
class NotificationRepository @Inject constructor() {

    companion object {
        val AUDIOPHILE_FUN_FACTS = listOf(
            Pair("Audiophile Fact: FLAC Compression", "FLAC (Free Lossless Audio Codec) reduces file sizes by 50-60% without throwing away a single bit of original audio data."),
            Pair("Audiophile Fact: Dynamic Range", "24-bit audio provides 16.7 million discrete amplitude steps compared to 65,536 in 16-bit audio, offering incredible micro-detail in quiet passages."),
            Pair("Audiophile Fact: Nyquist Theorem", "Sampling audio at 44.1kHz (CD quality) perfectly reconstructs frequencies up to 22.05kHz, covering the absolute upper limit of human hearing."),
            Pair("Audiophile Fact: Tube Amplifiers", "Vacuum tube amplifiers introduce warm even-order harmonic distortion, which the human ear perceives as pleasing and musically rich."),
            Pair("Audiophile Fact: Planar Magnetic Drivers", "Planar magnetic headphones use an ultra-thin diaphragm suspended between magnets, delivering lightning-fast transient response and ultra-low distortion."),
            Pair("Audiophile Fact: Room Acoustics", "Room reflections and standing waves account for up to 50% of the sound quality experienced in a listening room, often mattering more than speaker price."),
            Pair("Audiophile Fact: Balanced Audio", "Balanced audio cables use differential signaling to cancel out electromagnetic interference picked up over long cable runs."),
            Pair("Audiophile Fact: Bit Depth & Dynamic Range", "Every increase of 1 bit in digital audio depth adds 6 dB of dynamic range, which is why 24-bit audio is prized by mastering engineers."),
            Pair("Audiophile Fact: Moving Coil Speakers", "The moving coil loudspeaker driver, invented by Rice and Kellogg in 1925, remains the foundational acoustic design used in 99% of modern speakers."),
            Pair("Audiophile Fact: Custom IEMs", "High-end custom In-Ear Monitors (IEMs) use precision 3D-printed shells molded directly from impressions of the user's ear canals for absolute isolation."),
            Pair("Audiophile Fact: Audio Jitter", "Jitter is timing error in digital clock signals that can cause subtle phase distortion and harshness in digital-to-analog converters (DACs)."),
            Pair("Audiophile Fact: Active Crossovers & DSP", "Digital Signal Processing (DSP) and active crossovers allow modern wireless speakers to achieve flat frequency responses previously impossible with passive analog filters."),
            Pair("Audiophile Fact: Bone Conduction Audio", "Bone conduction headphones bypass the eardrum entirely, transmitting audio vibrations directly through skull bones to the cochlea."),
            Pair("Audiophile Fact: Open-Back Soundstage", "Open-back headphones allow air to pass freely through the earcups, producing a wide, natural soundstage resembling live speakers."),
            Pair("Audiophile Fact: Master Tape Bandwidth", "Master tapes recorded at 30 inches per second (IPS) on reel-to-reel tape machines possess bandwidth and warmth that engineers spend decades trying to emulate."),
            Pair("Audiophile Fact: Harman Target Curve", "The Harman Target response curve is scientifically tuned to replicate the sound of flat studio monitors interacting with an average human ear and room."),
            Pair("Audiophile Fact: Electrostatic Headphones", "Electrostatic headphones require high-voltage polarizing bias (often 500V+) to move an ultralight charged film diaphragm suspended between stators."),
            Pair("Audiophile Fact: R-2R Ladder DACs", "R-2R ladder DACs use precision resistor networks to convert digital samples directly to analog voltage without Delta-Sigma noise shaping."),
            Pair("Audiophile Fact: HRTF Spatial Audio", "HRTF (Head-Related Transfer Function) describes how the human head, torso, and pinna shape sounds differently depending on direction, enabling realistic 3D spatial audio."),
            Pair("Audiophile Fact: Vinyl vs Digital", "Vinyl records have physical groove limitations, whereas 24-bit/96kHz digital audio captures dynamic range exceeding 144 dB, far beyond human hearing.")
        )
    }

    private val initialNotifications = listOf(
        AppNotification(
            title = "Welcome to Sonixa v2.1.0",
            message = "Sonixa v2.1.0 installed successfully. Enjoy high-resolution offline FLAC playback! Check out the v2.1.0 Release Notes & Changelog in Settings.",
            category = NotificationCategory.RELEASE_NOTES
        ),
        AppNotification(
            title = "Sleep Timer Reminder",
            message = "Use the Sleep Timer shortcut in Now Playing to automatically pause music when falling asleep.",
            category = NotificationCategory.REMINDERS
        )
    )

    private val _notifications = MutableStateFlow<List<AppNotification>>(initialNotifications)
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    fun addNotification(title: String, message: String, category: NotificationCategory) {
        val notif = AppNotification(title = title, message = message, category = category)
        _notifications.value = listOf(notif) + _notifications.value
    }

    fun clearNotifications() {
        _notifications.value = emptyList()
    }
}
