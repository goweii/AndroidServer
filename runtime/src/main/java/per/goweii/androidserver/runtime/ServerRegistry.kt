package per.goweii.androidserver.runtime

import android.app.Application
import android.os.SystemClock
import android.util.Log
import androidx.annotation.WorkerThread
import dalvik.system.DexFile
import per.goweii.androidserver.runtime.annotation.RequestMapping
import per.goweii.androidserver.runtime.annotation.RestController
import per.goweii.androidserver.runtime.http.HttpDelegate
import per.goweii.androidserver.runtime.http.HttpPath
import per.goweii.androidserver.runtime.http.HttpRequest
import per.goweii.androidserver.runtime.http.HttpRequestMethod
import per.goweii.androidserver.runtime.ws.WebSocketController
import per.goweii.androidserver.runtime.ws.WebSocketDelegate
import java.util.concurrent.locks.ReentrantLock

internal object ServerRegistry {
    private const val TAG = "ServerRegistry"

    enum class ScanState {
        IDLE, SCANNING, COMPLETED
    }

    private val delegates = arrayListOf<RestDelegate>()

    private val scanLock = ReentrantLock()
    private val scanCondition = scanLock.newCondition()

    @Volatile
    private var scanState: ScanState = ScanState.IDLE

    @WorkerThread
    fun init(application: Application) {
        if (scanState == ScanState.COMPLETED) {
            return
        }

        scanLock.lock()
        try {
            when (scanState) {
                ScanState.IDLE -> {
                    scanState = ScanState.SCANNING
                    try {
                        scanInternal(application)
                        scanState = ScanState.COMPLETED
                        scanCondition.signalAll()
                    } catch (e: Throwable) {
                        scanState = ScanState.IDLE
                        throw e
                    }
                }

                ScanState.SCANNING -> scanCondition.await()
                ScanState.COMPLETED -> {}
            }
        } finally {
            scanLock.unlock()
        }
    }

    private fun waitScanDone() {
        if (scanState == ScanState.COMPLETED) {
            return
        }

        scanLock.lock()
        try {
            when (scanState) {
                ScanState.IDLE -> throw IllegalStateException("Did you forget to initialize?")
                ScanState.SCANNING -> scanCondition.await()
                ScanState.COMPLETED -> {}
            }
        } finally {
            scanLock.unlock()
        }
    }

    fun getDelegates(pathMatcher: PathMatcher): List<RestDelegate> {
        waitScanDone()
        return delegates
            .asSequence()
            .filter { pathMatcher.match(it.path.path) }
            .toList()
    }

    @WorkerThread
    private fun scanInternal(application: Application) {
        Log.d(TAG, "start scan")

        val startTime = SystemClock.elapsedRealtime()
        try {
            val applicationInfo = application.applicationInfo
            val apkPaths = arrayListOf<String>(applicationInfo.sourceDir)
            applicationInfo.splitSourceDirs
                ?.filter { it.isNotBlank() }
                ?.forEach { apkPaths.add(it) }

            val delegates = arrayListOf<RestDelegate>()

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

            this.delegates.clear()
            this.delegates.addAll(delegates)
        } finally {
            val cost = SystemClock.elapsedRealtime() - startTime
            Log.d(TAG, "scan success, found ${delegates.size}, cost ${cost}ms")
        }
    }

    @Suppress("DEPRECATION")
    private fun scanDex(classLoader: ClassLoader, apkPath: String): List<RestDelegate> {
        val dexFile = DexFile(apkPath)
        val enumeration = dexFile.entries()

        val delegates = arrayListOf<RestDelegate>()

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
                    val delegate = parseRestDelegate(clazz)
                    delegates.add(delegate)
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

    private fun parseRestDelegate(clazz: Class<*>): RestDelegate {
        val restController = clazz.getAnnotation(RestController::class.java)!!

        val httpPath = HttpPath(restController.path)

        val constructor = clazz.getConstructor()
        constructor.isAccessible = true
        val instance = constructor.newInstance()

        if (WebSocketController::class.java.isAssignableFrom(clazz)) {
            return WebSocketDelegate(
                path = httpPath,
                protocol = restController.protocol,
                instance = instance as WebSocketController,
            )
        }

        val requests = arrayListOf<HttpRequest>()

        val methods = clazz.methods
        methods.forEach { method ->
            if (Thread.interrupted()) {
                throw InterruptedException()
            }

            method.isAccessible = true

            val requestMapping = method.getAnnotation(RequestMapping::class.java)
                ?: return@forEach

            val request = HttpRequest(
                method = requestMapping.method,
                path = httpPath + requestMapping.path,
                requestMethod = HttpRequestMethod(
                    instance = instance,
                    method = method,
                ),
            )

            requests.add(request)
        }

        return HttpDelegate(
            path = httpPath,
            protocol = restController.protocol,
            requests = requests,
        )
    }
}