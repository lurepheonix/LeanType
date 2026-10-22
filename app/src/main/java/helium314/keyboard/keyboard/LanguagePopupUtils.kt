// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import android.view.inputmethod.InputMethodSubtype
import helium314.keyboard.keyboard.internal.PopupKeySpec
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode.isLanguageSwitchItem
import helium314.keyboard.latin.RichInputMethodManager
import helium314.keyboard.latin.utils.SubtypeLocaleUtils.displayName
import helium314.keyboard.latin.utils.SubtypeSettings
import java.text.Collator
import java.util.Locale
import kotlin.math.min

/**
 * Builds the content of the language-switch popup shown on long-press of the language key.
 *
 * Only subtypes enabled in LeanType are listed (never other IMEs). Few languages are shown as a
 * single vertical column; many languages overflow into a fixed-order grid (see [computeColumns]).
 */
object LanguagePopupUtils {
    /** Maximum number of rows before the vertical list overflows into a multi-column grid. */
    const val MAX_VERTICAL_ROWS = 4

    /** Upper bound for grid columns, same as regular popup keys. */
    const val MAX_COLUMNS = 5

    data class LanguagePopupData(
        val specs: Array<PopupKeySpec>,
        val columns: Int,
        val subtypes: List<InputMethodSubtype>
    )

    @Volatile private var lastShownByCode: Map<Int, InputMethodSubtype> = emptyMap()

    fun computeColumns(count: Int): Int {
        if (count <= MAX_VERTICAL_ROWS) return 1
        return min(MAX_COLUMNS, (count + MAX_VERTICAL_ROWS - 1) / MAX_VERTICAL_ROWS)
    }

    /**
     * Sorts items alphabetically by display name using [collator] (ascending).
     */
    fun <T> sortByName(items: List<T>, nameOf: (T) -> String, collator: Collator): List<T> =
        items.sortedWith { a, b -> collator.compare(nameOf(a), nameOf(b)) }

    /**
     * Reorders an ascending list for the popup grid: PopupKeysKeyboard renders array index 0
     * at the bottom row and grows upwards, filling each row left-to-right. The ascending list
     * is split into row chunks from the top (partial chunk first, matching the popup's
     * partial-top-row convention) and chunks are laid bottom-up, so the popup reads A-Z
     * top-to-bottom, left-to-right. [columns] must be the final column count.
     */
    fun <T> toBottomUpRowOrder(ascending: List<T>, columns: Int): List<T> {
        val cols = columns.coerceAtLeast(1)
        // A single row is the top row, rendered left-to-right: keep ascending order.
        if (ascending.size <= cols) return ascending.toList()
        val topCount = ascending.size % cols
        val rows = if (topCount == 0) {
            ascending.chunked(cols)
        } else {
            listOf(ascending.take(topCount)) + ascending.drop(topCount).chunked(cols)
        }
        return rows.asReversed().flatten()
    }

    /** @return popup data, or null when there is nothing to show (0-1 enabled subtypes). */
    fun buildPopupData(): LanguagePopupData? {
        val enabled = try {
            SubtypeSettings.getEnabledSubtypes()
        } catch (_: Exception) {
            return null
        }
        if (enabled.size <= 1) return null
        // Display names are localized in the system locale, so compare with its collation rules
        // (raw code-unit order mis-sorts accented characters and mixed scripts).
        val collator = runCatching { Collator.getInstance(Locale.getDefault()) }.getOrNull()
            ?: Collator.getInstance()
        // Ascending order here; PopupKeysKeyboard.Builder reorders into bottom-up row order
        // with the final column count when building the popup.
        val sorted = sortByName(enabled, { subtype ->
            try {
                subtype.displayName()
            } catch (_: Exception) {
                ""
            }
        }, collator)
        val current = try {
            RichInputMethodManager.getInstance().currentSubtype.rawSubtype
        } catch (_: Exception) {
            null
        }
        val specs = Array(sorted.size) { index ->
            val subtype = sorted[index]
            val name = try {
                subtype.displayName()
            } catch (_: Exception) {
                ""
            }
            val label = if (subtype == current) "✓ $name" else name
            PopupKeySpec(KeyCode.languageSwitchItemCode(index), label.ifEmpty { "?" })
        }
        // Keyed by code (not position): Builder may reorder specs into bottom-up row order.
        lastShownByCode = sorted.mapIndexed { index, subtype ->
            KeyCode.languageSwitchItemCode(index) to subtype
        }.toMap()
        return LanguagePopupData(specs, computeColumns(sorted.size), sorted)
    }

    /** Maps a popup item code back to the subtype selected by the user, or null. */
    fun subtypeForCode(code: Int): InputMethodSubtype? {
        if (!code.isLanguageSwitchItem()) return null
        return lastShownByCode[code]
    }
}
