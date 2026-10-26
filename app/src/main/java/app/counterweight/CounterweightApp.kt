package app.counterweight

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import app.counterweight.data.ChamberDb
import app.counterweight.data.DemoSeed
import app.counterweight.data.Repo
import app.counterweight.data.SettingsStore
import app.counterweight.domain.Catalog
import app.counterweight.notify.Bell
import app.counterweight.sound.Sfx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    val app: Context = context.applicationContext
    val db = ChamberDb.create(app)
    val catalog: Catalog by lazy { Catalog.load { name -> runCatching { app.assets.open("content/$name") }.getOrNull() } }
    val repo by lazy { Repo(db, catalog) }
    val settings = SettingsStore(app)
    val sfx = Sfx(app)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Fire-and-forget so a write survives the screen being closed right after tapping. */
    fun write(block: suspend AppContainer.() -> Unit) {
        scope.launch { block() }
    }
}

class CounterweightApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Bell.channel(this)
        container.scope.launch {
            container.catalog
            if (BuildConfig.DEMO) DemoSeed.seedIfEmpty(container)
            container.repo.refresh()
            container.sfx.load()
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as CounterweightApp).container

/** Resources in the in-app language, which AppCompat doesn't localise outside activities before Android 13. */
fun Context.localized(): Context {
    val tags = AppCompatDelegate.getApplicationLocales()
    if (tags.isEmpty) return this
    val config = Configuration(resources.configuration)
    config.setLocale(tags[0])
    return createConfigurationContext(config)
}

/** Is this context in Russian? */
val Context.ru: Boolean get() = resources.configuration.locales[0].language == "ru"
