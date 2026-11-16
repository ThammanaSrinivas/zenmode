package com.zenlauncher.zenmode.island

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The island's state for the whole process: what is live, which peek is passing, whether the
 * card is open. Every source writes here and the window only reads [scene], so the island looks
 * the same whichever window carries it. Main thread only.
 */
object IslandStore {

    /** An open card closes by itself after this long without a touch. */
    const val OPEN_MILLIS = 8_000L
    /** At most this many peeks wait their turn; older ones are dropped, newest kept. */
    private const val QUEUE_MAX = 3

    private val handler = Handler(Looper.getMainLooper())
    private val live = LinkedHashMap<String, IslandItem>()
    private val queue = ArrayDeque<IslandPeek>()
    private var peek: IslandPeek? = null
    private var openId: String? = null
    private var hidden = false

    private val _scene = MutableStateFlow(IslandScene.EMPTY)
    val scene: StateFlow<IslandScene> = _scene.asStateFlow()

    /** Notifications kept back during the running Quiet session. */
    var held: Int = 0
        private set

    private val endPeek = Runnable { advance() }
    private val autoClose = Runnable { close() }

    fun liveItem(id: String): IslandItem? = live[id]

    fun putLive(item: IslandItem) {
        if (live[item.id] == item) return
        live[item.id] = item
        publish()
    }

    fun removeLive(id: String) {
        if (live.remove(id) == null) return
        if (openId == id) openId = null
        publish()
    }

    fun removeLiveWhere(test: (IslandItem) -> Boolean) {
        val gone = live.values.filter(test).map { it.id }
        if (gone.isEmpty()) return
        gone.forEach { live.remove(it) }
        if (openId in gone) openId = null
        publish()
    }

    /** A peek arrives: now if nothing passes, otherwise after the one showing. Same id replaces. */
    fun post(p: IslandPeek) {
        when {
            peek?.id == p.id -> {
                peek = p
                if (p.opens) openId = p.id
                handler.removeCallbacks(endPeek)
                handler.postDelayed(endPeek, p.millis)
                publish()
            }
            peek == null -> show(p)
            else -> {
                queue.removeAll { it.id == p.id }
                queue.addLast(p)
                while (queue.size > QUEUE_MAX) queue.removeFirst()
            }
        }
    }

    fun hold() {
        held++
        (live[QuietItem.ID] as? QuietItem)?.let { putLive(it.copy(held = held)) }
    }

    /** A new Quiet session starts counting from zero. */
    fun resetHeld() {
        held = 0
    }

    private fun show(p: IslandPeek) {
        peek = p
        if (p.opens) openId = p.id
        handler.removeCallbacks(endPeek)
        handler.postDelayed(endPeek, p.millis)
        publish()
    }

    private fun advance() {
        if (openId == peek?.id) openId = null
        peek = null
        val next = queue.removeFirstOrNull()
        if (next != null) show(next) else publish()
    }

    /** Opens the card on [id] (the pill, the bubble or a passing peek). */
    fun open(id: String, millis: Long = OPEN_MILLIS) {
        openId = id
        // A peek someone opened stays while they read it.
        if (peek?.id == id) handler.removeCallbacks(endPeek)
        handler.removeCallbacks(autoClose)
        handler.postDelayed(autoClose, millis)
        publish()
    }

    /** Keeps an open card open a little longer (the person is using it). */
    fun touch() {
        if (openId == null) return
        handler.removeCallbacks(autoClose)
        handler.postDelayed(autoClose, OPEN_MILLIS)
    }

    fun close() {
        handler.removeCallbacks(autoClose)
        val wasPeek = peek != null && openId == peek?.id
        openId = null
        if (wasPeek) advance() else publish()
    }

    /** Swiped away: a peek goes at once, a live thing closes back to its pill. */
    fun dismiss() {
        if (peek != null) {
            handler.removeCallbacks(endPeek)
            openId = null
            advance()
        } else {
            close()
        }
    }

    fun setHidden(value: Boolean) {
        if (hidden == value) return
        hidden = value
        if (value) openId = null
        publish()
    }

    /** Everything goes (the island was turned off, or lost its window). */
    fun clear() {
        handler.removeCallbacksAndMessages(null)
        live.clear()
        queue.clear()
        peek = null
        openId = null
        held = 0
        publish()
    }

    private fun publish() {
        _scene.value = IslandStage.scene(live.values, peek, openId, hidden)
    }
}
