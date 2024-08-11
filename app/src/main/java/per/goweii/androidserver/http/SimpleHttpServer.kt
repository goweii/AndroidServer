package per.goweii.androidserver.http

import android.app.Application
import android.util.Log
import com.koushikdutta.async.AsyncSSLSocketWrapper
import com.koushikdutta.async.AsyncSocket
import per.goweii.androidserver.runtime.HttpServer
import per.goweii.androidserver.runtime.PathMatcher
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class SimpleHttpServer : HttpServer() {
    override val port: Int = 24720
    override val pathMatcher: PathMatcher = PathMatcher()
        .includePathStartWith("/api")

    private val sslContext: SSLContext by lazy {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)

        val androidCAStore = KeyStore.getInstance("AndroidCAStore")
        androidCAStore.load(null, null)

        val aliases = androidCAStore.aliases()
        while (aliases.hasMoreElements()) {
            val alias = aliases.nextElement()
            Log.d("SimpleHttpServer", "AndroidCAStore alias = $alias")
            val entry = androidCAStore.getEntry(alias, null)
            keyStore.setEntry(alias, entry, null)
        }

        val keyManagerFactory = KeyManagerFactory.getInstance("X509")
        keyManagerFactory.init(keyStore, null)

        val trustManagerFactory = TrustManagerFactory.getInstance("X509")
        trustManagerFactory.init(keyStore)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, SecureRandom())

        sslContext
    }

    override fun onAcceptSocket(socket: AsyncSocket) {
        val sslContext = sslContext
        val sslEngine = sslContext.createSSLEngine()

        sslEngine.needClientAuth = true
        sslEngine.enabledProtocols = arrayOf("TLSv1.1", "TLSv1.2")

        AsyncSSLSocketWrapper.handshake(
            socket, host, port,
            sslEngine, null,
            null, false
        ) { e, sslSocket ->
            if (sslSocket != null) {
                super.onAcceptSocket(sslSocket)
            } else {
                e?.printStackTrace()
            }
        }
    }
}