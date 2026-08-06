import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlatformScrollbarTest {

    @Test
    fun `macOS uses overlay transient policy`() {
        assertEquals(ScrollbarPolicy.OverlayTransient, scrollbarPolicyForOsName("Mac OS X"))
        assertEquals(ScrollbarPolicy.OverlayTransient, scrollbarPolicyForOsName("macOS"))
    }

    @Test
    fun `windows and linux use persistent policy`() {
        assertEquals(ScrollbarPolicy.Persistent, scrollbarPolicyForOsName("Windows 11"))
        assertEquals(ScrollbarPolicy.Persistent, scrollbarPolicyForOsName("Linux"))
    }
}
