package per.goweii.androidserver.runtime

import android.app.Application
import android.os.SystemClock
import android.util.Log
import androidx.annotation.WorkerThread
import dalvik.system.DexFile
import per.goweii.androidserver.runtime.annotation.RequestMapping
import per.goweii.androidserver.runtime.annotation.RestController
import java.util.concurrent.locks.ReentrantLock

internal object HttpRegistry {
    private const val TAG = "HttpRegistry"

    private val delegates = arrayListOf<HttpRequestDelegate>()

    private val scanLock = ReentrantLock()
    private val scanCondition = scanLock.newCondition()

    @Volatile
    private var scanState: Boolean? = null

    @WorkerThread
    fun loadDelegate(application: Application): List<HttpRequestDelegate> {
        if (scanState == true) {
            return ArrayList<HttpRequestDelegate>(delegates.size)
                .also { it.addAll(delegates) }
        }

        scanLock.lock()
        try {
            when (scanState) {
                true -> {
                    // do nothing
                }

                false -> {
                    scanCondition.await()
                }

                null -> {
                    scanState = false
                    try {
                        scan(application)
                        scanState = true
                        scanCondition.signalAll()
                    } catch (e: Throwable) {
                        scanState = null
                        throw e
                    }
                }
            }
        } finally {
            scanLock.unlock()
        }

        return ArrayList<HttpRequestDelegate>(delegates.size)
            .also { it.addAll(delegates) }
    }

    @WorkerThread
    private fun scan(application: Application) {
        Log.d("HttpRegistry", "start scan")

        val startTime = SystemClock.elapsedRealtime()
        try {
            val applicationInfo = application.applicationInfo
            val apkPaths = arrayListOf<String>(applicationInfo.sourceDir)
            applicationInfo.splitSourceDirs
                ?.filter { it.isNotBlank() }
                ?.forEach { apkPaths.add(it) }

            val delegates = arrayListOf<HttpRequestDelegate>()

            apkPaths.forEach {
                if (Thread.interrupted()) {
                    throw InterruptedException()
                }

                try {
                    val list = scanDex(application.classLoader, it)
                    delegates.addAll(list)
                } catch (e: InterruptedException) {
                    throw e
                }
            }

            delegates.sortBy { it.path }
            this.delegates.clear()
            this.delegates.addAll(delegates)
        } finally {
            val cost = SystemClock.elapsedRealtime() - startTime
            Log.d(TAG, "scan success, found ${delegates.size}, cost ${cost}ms")
        }
    }

    @Suppress("DEPRECATION")
    private fun scanDex(classLoader: ClassLoader, apkPath: String): List<HttpRequestDelegate> {
        val dexFile = DexFile(apkPath)
        val enumeration = dexFile.entries()

        val delegates = arrayListOf<HttpRequestDelegate>()

        while (enumeration.hasMoreElements()) {
            if (Thread.interrupted()) {
                throw InterruptedException()
            }

            val element = enumeration.nextElement()

            if (!maybeRestController(element)) continue

            Log.d(TAG, "scan: $element")

            val clazz = try {
                Class.forName(element, false, classLoader)
            } catch (e: InterruptedException) {
                throw e
            } catch (ignore: Throwable) {
                continue
            }

            val restController = clazz.getAnnotation(RestController::class.java)
            if (restController != null) {
                Log.i(TAG, "find: $element")
                try {
                    val list = parseRestController(clazz, restController)
                    delegates.addAll(list)
                } catch (e: InterruptedException) {
                    throw e
                } catch (e: Throwable) {
                    Log.e(TAG, "Failed to parse RestController '$clazz'", e)
                    throw e
                }
            }
        }

        return delegates
    }

    private val classNameRegex = """^[a-zA-Z_$][a-zA-Z0-9_$.]*$""".toRegex()
    private val rClassRegex = """\.R(\$.+$|$)""".toRegex()
    private val packageName = javaClass.`package`?.name ?: ""

    private fun maybeRestController(element: String): Boolean {
        if (element.startsWith(".")) return false
        if (element.endsWith(".")) return false
        if (element.startsWith("_COROUTINE.")) return false
        if (element.startsWith("android.")) return false
        if (element.startsWith("androidx.")) return false
        if (element.startsWith("google.")) return false
        if (element.startsWith("com.google.")) return false
        if (element.startsWith("kotlin.")) return false
        if (element.startsWith("kotlinx.")) return false
        if (element.startsWith("java.")) return false
        if (element.startsWith("dalvik.")) return false
        if (element.startsWith("org.jetbrains.")) return false
        if (element.startsWith("org.intellij.")) return false
        if (element.startsWith("com.koushikdutta.")) return false
        if (element.startsWith(packageName)) return false
        if (element.contains(rClassRegex)) return false
        return element.matches(classNameRegex)
    }

    private fun parseRestController(clazz: Class<*>, restController: RestController): List<HttpRequestDelegate> {
        val constructor = clazz.getConstructor()
        constructor.isAccessible = true
        val instance = constructor.newInstance()

        val delegates = arrayListOf<HttpRequestDelegate>()

        val methods = clazz.methods
        methods.forEach { method ->
            if (Thread.interrupted()) {
                throw InterruptedException()
            }

            method.isAccessible = true

            val requestMapping = method.getAnnotation(RequestMapping::class.java)
                ?: return@forEach

            val httpMethod = requestMapping.method
            val httpPath = StringBuilder(restController.path)
                .append(requestMapping.path)
                .toString()

            val requestMethod = RequestMethod(
                instance = instance,
                method = method,
            )

            val delegate = HttpRequestDelegate(
                method = httpMethod,
                path = httpPath,
                requestMethod = requestMethod,
            )

            delegates.add(delegate)
        }

        return delegates
    }
}