import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.io.File
import java.util.Properties

data class WindowPlacement(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
) {
    companion object {
        const val KEY_X = "windowX"
        const val KEY_Y = "windowY"
        const val KEY_WIDTH = "windowWidth"
        const val KEY_HEIGHT = "windowHeight"

        val DEFAULT = WindowPlacement(
            x = 100f,
            y = 80f,
            width = 960f,
            height = 960f
        )

        const val MIN_WIDTH = 480f
        const val MIN_HEIGHT = 400f
    }
}

/**
 * Load window placement from a `.dvratrg` properties file.
 * Missing or invalid keys fall back to [WindowPlacement.DEFAULT] fields.
 */
fun loadWindowPlacement(configFile: File = ConfigUtil.defaultConfigFile()): WindowPlacement {
    if (!configFile.exists()) {
        return WindowPlacement.DEFAULT
    }

    val props = loadConfigProperties(configFile)
    return WindowPlacement(
        x = props.floatProperty(WindowPlacement.KEY_X, WindowPlacement.DEFAULT.x),
        y = props.floatProperty(WindowPlacement.KEY_Y, WindowPlacement.DEFAULT.y),
        width = props.floatProperty(WindowPlacement.KEY_WIDTH, WindowPlacement.DEFAULT.width),
        height = props.floatProperty(WindowPlacement.KEY_HEIGHT, WindowPlacement.DEFAULT.height)
    )
}

/**
 * Merge window placement into [configFile], preserving other properties.
 */
fun saveWindowPlacement(
    placement: WindowPlacement,
    configFile: File = ConfigUtil.defaultConfigFile()
) {
    saveMergedProperties(
        mapOf(
            WindowPlacement.KEY_X to placement.x.toString(),
            WindowPlacement.KEY_Y to placement.y.toString(),
            WindowPlacement.KEY_WIDTH to placement.width.toString(),
            WindowPlacement.KEY_HEIGHT to placement.height.toString()
        ),
        configFile = configFile
    )
}

/**
 * Clamp [placement] so the window stays within a visible screen.
 * Prefers the screen that contains the window center; otherwise the closest screen.
 */
fun clampWindowPlacement(
    placement: WindowPlacement,
    screens: List<Rectangle> = currentScreenBounds()
): WindowPlacement {
    if (screens.isEmpty()) {
        return placement.copy(
            width = placement.width.coerceAtLeast(WindowPlacement.MIN_WIDTH),
            height = placement.height.coerceAtLeast(WindowPlacement.MIN_HEIGHT)
        )
    }

    val centerX = placement.x + placement.width / 2f
    val centerY = placement.y + placement.height / 2f

    val containing = screens.firstOrNull { it.contains(centerX.toInt(), centerY.toInt()) }
    val screen = containing ?: screens.minBy { screen ->
        val sx = screen.x + screen.width / 2.0
        val sy = screen.y + screen.height / 2.0
        val dx = centerX - sx
        val dy = centerY - sy
        dx * dx + dy * dy
    }

    val maxWidth = screen.width.toFloat().coerceAtLeast(WindowPlacement.MIN_WIDTH)
    val maxHeight = screen.height.toFloat().coerceAtLeast(WindowPlacement.MIN_HEIGHT)

    val width = placement.width.coerceIn(WindowPlacement.MIN_WIDTH, maxWidth)
    val height = placement.height.coerceIn(WindowPlacement.MIN_HEIGHT, maxHeight)

    val minX = screen.x.toFloat()
    val minY = screen.y.toFloat()
    val maxX = (screen.x + screen.width).toFloat() - width
    val maxY = (screen.y + screen.height).toFloat() - height

    return WindowPlacement(
        x = placement.x.coerceIn(minX, maxX.coerceAtLeast(minX)),
        y = placement.y.coerceIn(minY, maxY.coerceAtLeast(minY)),
        width = width,
        height = height
    )
}

fun loadClampedWindowPlacement(
    configFile: File = ConfigUtil.defaultConfigFile()
): WindowPlacement = clampWindowPlacement(loadWindowPlacement(configFile))

fun currentScreenBounds(): List<Rectangle> {
    if (GraphicsEnvironment.isHeadless()) {
        return emptyList()
    }
    return GraphicsEnvironment.getLocalGraphicsEnvironment()
        .screenDevices
        .map { it.defaultConfiguration.bounds }
}

private fun Properties.floatProperty(key: String, default: Float): Float {
    val raw = getProperty(key)?.trim().orEmpty()
    if (raw.isEmpty()) {
        return default
    }
    return raw.toFloatOrNull() ?: default
}
