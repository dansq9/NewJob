package app.ascend.i18n

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * In-app language selection that works on every supported API level (minSdk 24).
 *
 * The chosen BCP-47 language tag is stored in a tiny synchronous SharedPreferences
 * file so it can be read in [android.app.Activity.attachBaseContext] (which runs
 * before any async store is ready). [wrap] applies it to the Activity's resource
 * configuration; picking a new language persists the tag and recreates the Activity.
 *
 * `null`/blank tag = follow the system language. Pair this with
 * res/xml/locales_config.xml so Android 13+ also shows Ascend under
 * Settings → Apps → Ascend → Language.
 */
object LocaleManager {
    private const val PREFS = "ascend_locale"
    private const val KEY = "app_locale_tag"

    /** Languages the app ships translations for. First entry = follow system. */
    val supported: List<AppLanguage> = listOf(
        AppLanguage(tag = "", endonym = "System default", english = "System default"),
        AppLanguage(tag = "en", endonym = "English", english = "English"),
        AppLanguage(tag = "es", endonym = "Español", english = "Spanish"),
    )

    fun persistedTag(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "") ?: ""

    fun setLanguage(context: Context, tag: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .apply { if (tag.isBlank()) remove(KEY) else putString(KEY, tag) }
            .apply()
    }

    /** Wrap a base context with the persisted locale. Call from attachBaseContext. */
    fun wrap(base: Context): Context {
        val tag = persistedTag(base)
        if (tag.isBlank()) return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)   // RTL-aware for future locales (e.g. Arabic)
        return base.createConfigurationContext(config)
    }
}

/** A user-selectable language. [tag] "" = follow the system. */
data class AppLanguage(val tag: String, val endonym: String, val english: String)
