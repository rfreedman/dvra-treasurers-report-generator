import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Locale

enum class ScrollbarPolicy {
    /** macOS-like: overlay thumb only while scrolling (briefly after). */
    OverlayTransient,

    /** Windows/Linux-like: visible whenever content overflows. */
    Persistent
}

fun scrollbarPolicyForOsName(osName: String): ScrollbarPolicy {
    val os = osName.lowercase(Locale.ROOT)
    return when {
        os.contains("mac") || os.contains("darwin") -> ScrollbarPolicy.OverlayTransient
        else -> ScrollbarPolicy.Persistent
    }
}

fun scrollbarPolicyForCurrentOs(): ScrollbarPolicy =
    scrollbarPolicyForOsName(System.getProperty("os.name", "unknown"))

/**
 * Platform-mimic vertical scrollbar for a [ScrollState]-backed column.
 * Hidden entirely when content does not overflow.
 */
@Composable
fun PlatformVerticalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    if (scrollState.maxValue <= 0) {
        return
    }

    val policy = remember { scrollbarPolicyForCurrentOs() }
    val adapter = rememberScrollbarAdapter(scrollState)

    when (policy) {
        ScrollbarPolicy.Persistent -> {
            VerticalScrollbar(
                adapter = adapter,
                modifier = modifier
            )
        }

        ScrollbarPolicy.OverlayTransient -> {
            var visible by remember { mutableStateOf(false) }

            LaunchedEffect(scrollState.isScrollInProgress) {
                if (scrollState.isScrollInProgress) {
                    visible = true
                } else {
                    delay(HIDE_DELAY_MS)
                    if (!scrollState.isScrollInProgress) {
                        visible = false
                    }
                }
            }

            val alpha by animateFloatAsState(
                targetValue = if (visible) 1f else 0f,
                animationSpec = tween(durationMillis = 180),
                label = "scrollbarAlpha"
            )

            if (visible || alpha > 0.01f) {
                VerticalScrollbar(
                    adapter = adapter,
                    modifier = modifier.graphicsLayer { this.alpha = alpha },
                    style = defaultScrollbarStyle().copy(
                        thickness = 7.dp,
                        unhoverColor = Color.Black.copy(alpha = 0.40f),
                        hoverColor = Color.Black.copy(alpha = 0.60f)
                    )
                )
            }
        }
    }
}

private const val HIDE_DELAY_MS = 1_000L
