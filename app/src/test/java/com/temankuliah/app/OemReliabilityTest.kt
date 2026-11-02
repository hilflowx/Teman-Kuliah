package com.temankuliah.app

import com.temankuliah.app.domain.OemVendor
import com.temankuliah.app.domain.getVendorGuide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OemReliabilityTest {

    @Test
    fun `detects Xiaomi vendor correctly`() {
        assertEquals(OemVendor.XIAOMI, OemVendor.fromManufacturer("Xiaomi"))
        assertEquals(OemVendor.XIAOMI, OemVendor.fromManufacturer("Redmi"))
        assertEquals(OemVendor.XIAOMI, OemVendor.fromManufacturer("POCO"))
    }

    @Test
    fun `detects Samsung vendor correctly`() {
        assertEquals(OemVendor.SAMSUNG, OemVendor.fromManufacturer("Samsung"))
        assertEquals(OemVendor.SAMSUNG, OemVendor.fromManufacturer("samsung"))
    }

    @Test
    fun `detects Oppo and Realme vendor correctly`() {
        assertEquals(OemVendor.OPPO, OemVendor.fromManufacturer("OPPO"))
        assertEquals(OemVendor.OPPO, OemVendor.fromManufacturer("realme"))
        assertEquals(OemVendor.OPPO, OemVendor.fromManufacturer("OnePlus"))
    }

    @Test
    fun `detects Vivo vendor correctly`() {
        assertEquals(OemVendor.VIVO, OemVendor.fromManufacturer("vivo"))
        assertEquals(OemVendor.VIVO, OemVendor.fromManufacturer("iQOO"))
    }

    @Test
    fun `detects Huawei vendor correctly`() {
        assertEquals(OemVendor.HUAWEI, OemVendor.fromManufacturer("HUAWEI"))
        assertEquals(OemVendor.HUAWEI, OemVendor.fromManufacturer("Honor"))
    }

    @Test
    fun `fallback to Other vendor for unknown manufacturer`() {
        assertEquals(OemVendor.OTHER, OemVendor.fromManufacturer("Google"))
        assertEquals(OemVendor.OTHER, OemVendor.fromManufacturer("Motorola"))
        assertEquals(OemVendor.OTHER, OemVendor.fromManufacturer(""))
    }

    @Test
    fun `vendor guide provides actionable steps for all vendors`() {
        OemVendor.entries.forEach { vendor ->
            val guide = getVendorGuide(vendor)
            assertTrue("Guide title must not be blank for $vendor", guide.vendorName.isNotBlank())
            assertTrue("Guide summary must not be blank for $vendor", guide.summary.isNotBlank())
            assertFalse("Guide steps must have at least 2 steps for $vendor", guide.steps.size < 2)
            guide.steps.forEach { step ->
                assertTrue("Each step must not be blank for $vendor", step.isNotBlank())
            }
        }
    }

    @Test
    fun `Xiaomi guide mentions Autostart and lock screen`() {
        val guide = getVendorGuide(OemVendor.XIAOMI)
        assertTrue(guide.steps.any { it.contains("Mulai Otomatis") || it.contains("Autostart") })
        assertTrue(guide.steps.any { it.contains("Layar Kunci") })
    }

    @Test
    fun `Samsung guide mentions never sleeping apps`() {
        val guide = getVendorGuide(OemVendor.SAMSUNG)
        assertTrue(guide.steps.any { it.contains("tidak pernah tidur") })
    }
}
