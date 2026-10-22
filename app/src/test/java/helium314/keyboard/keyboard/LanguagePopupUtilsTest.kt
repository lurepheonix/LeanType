// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import helium314.keyboard.keyboard.internal.PopupKeySpec
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode.isLanguageSwitchItem
import java.text.Collator
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LanguagePopupUtilsTest {
    @Test fun verticalListForFewLanguages() {
        assertEquals(1, LanguagePopupUtils.computeColumns(1))
        assertEquals(1, LanguagePopupUtils.computeColumns(2))
        assertEquals(1, LanguagePopupUtils.computeColumns(4))
    }

    @Test fun gridOverflowForManyLanguages() {
        // MAX_VERTICAL_ROWS = 4 -> ceil(n / 4) columns, capped at MAX_COLUMNS = 5
        assertEquals(2, LanguagePopupUtils.computeColumns(5))
        assertEquals(2, LanguagePopupUtils.computeColumns(8))
        assertEquals(3, LanguagePopupUtils.computeColumns(9))
        assertEquals(3, LanguagePopupUtils.computeColumns(12))
        assertEquals(5, LanguagePopupUtils.computeColumns(20))
        assertEquals(5, LanguagePopupUtils.computeColumns(100))
    }

    @Test fun itemCodeRoundTrip() {
        for (index in 0 until KeyCode.LANGUAGE_SWITCH_ITEM_COUNT) {
            val code = KeyCode.languageSwitchItemCode(index)
            assertTrue(code.isLanguageSwitchItem(), "code $code should be a language item")
            assertEquals(index, KeyCode.languageSwitchItemIndex(code))
        }
        assertFalse(KeyCode.LANGUAGE_SWITCH.isLanguageSwitchItem())
        assertFalse(KeyCode.MULTIPLE_CODE_POINTS.isLanguageSwitchItem())
        assertFalse(0.isLanguageSwitchItem())
    }

    @Test fun unknownCodeMapsToNull() {
        assertNull(LanguagePopupUtils.subtypeForCode(KeyCode.LANGUAGE_SWITCH))
        assertNull(LanguagePopupUtils.subtypeForCode(42))
    }

    @Test fun displayOrderReadsAscendingTopToBottom() {
        // Popup renders array index 0 at the bottom: first-alphabetical must be last in the array
        val ascending = listOf("Apple", "Banana", "Cherry")
        assertEquals(
            listOf("Cherry", "Banana", "Apple"),
            LanguagePopupUtils.toBottomUpRowOrder(ascending, 1)
        )
    }

    @Test fun sortByNameUsesLocaleCollation() {
        val collator = Collator.getInstance(Locale.ENGLISH)
        // Raw code-unit order would put "Öl" after "Zebra"; locale collation sorts ö with o
        assertEquals(
            listOf("Apple", "Öl", "Zebra"),
            LanguagePopupUtils.sortByName(listOf("Zebra", "Öl", "Apple"), { it }, collator)
        )
    }

    @Test fun gridOrderReadsAscendingRowByRowFromTop() {
        // 8 items, 2 columns: top row [a0, a1] ... bottom row [a6, a7]
        val ascending = ('A'..'H').map { it.toString() }
        assertEquals(
            listOf("G", "H", "E", "F", "C", "D", "A", "B"),
            LanguagePopupUtils.toBottomUpRowOrder(ascending, 2)
        )
    }

    @Test fun gridOrderKeepsPartialRowOnTop() {
        // 10 items, 3 columns: top row [l0], then full rows; array starts at bottom row
        val ascending = (0..9).map { "l$it" }
        val arranged = LanguagePopupUtils.toBottomUpRowOrder(ascending, 3)
        assertEquals(
            listOf("l7", "l8", "l9", "l4", "l5", "l6", "l1", "l2", "l3", "l0"),
            arranged
        )
        // Display rows bottom-up: full chunks, partial top row last
        val arrayRowsBottomUp = arranged.chunked(3)
        assertEquals(listOf("l0"), arrayRowsBottomUp.last())
        // Full reading order top-to-bottom, left-to-right is ascending
        assertEquals(ascending, arrayRowsBottomUp.asReversed().flatten())
    }

    @Test fun singleRowKeepsAscendingOrder() {
        assertEquals(
            listOf("a", "b", "c"),
            LanguagePopupUtils.toBottomUpRowOrder(listOf("c", "a", "b").sorted(), 5)
        )
    }

    @Test fun directPopupKeySpecCtor() {
        val spec = PopupKeySpec(KeyCode.languageSwitchItemCode(3), "English")
        assertEquals(KeyCode.languageSwitchItemCode(3), spec.mCode)
        assertEquals("English", spec.mLabel)
        assertNull(spec.mOutputText)
    }
}
