package xunsky.net.okhttp;

import android.util.Log;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import okhttp3.Connection;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpHeaders;
import okio.Buffer;
import okio.BufferedSource;

public final class LogInterceptor implements Interceptor {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    public interface Logger {
        void log(String message);
    }


    public LogInterceptor(Logger logger) {
        this.logger = logger;
    }

    private final Logger logger;

    @Override
    public Response intercept(Chain chain) throws IOException {
        StringBuilder sb = new StringBuilder();

        Request request = chain.request();

        boolean logHeaders = true;

        RequestBody requestBody = request.body();
        boolean hasRequestBody = requestBody != null;

        Connection connection = chain.connection();
        Protocol protocol = connection != null ? connection.protocol() : Protocol.HTTP_1_1;
        String requestStartMessage = " \n--> " + request.method() + ' ' + request.url() + ' ' + protocol;
        sb.append("\n").append(requestStartMessage).append("\n");

        {
            if (hasRequestBody) {
                // Request body headers are only present when installed as a network interceptor. Force
                // them to be included (when available) so there values are known.
                if (requestBody.contentType() != null) {
                    sb.append("Content-Type: " + requestBody.contentType()).append("\n");
                }
                if (requestBody.contentLength() != -1) {
                    sb.append("Content-Length: " + requestBody.contentLength() + "\n");
                }
            }

            Headers headers = request.headers();
            for (int i = 0, count = headers.size(); i < count; i++) {
                String name = headers.name(i);
                // Skip headers from the request body as they are explicitly logged above.
                if (!"Content-Type".equalsIgnoreCase(name) && !"Content-Length".equalsIgnoreCase(name)) {
                    sb.append(name + ": " + headers.value(i)+"\n");
                }
            }

            if (!hasRequestBody) {
                sb.append("--> END " + request.method() + "\n\n");
            } else if (bodyEncoded(request.headers())) {
                sb.append("--> END " + request.method() + " (encoded body omitted)\n");
            } else {
                Buffer buffer = new Buffer();
                requestBody.writeTo(buffer);

                Charset charset = UTF8;
                MediaType contentType = requestBody.contentType();
                if (contentType != null) {
                    charset = contentType.charset(UTF8);
                }

                sb.append(" \n");
                if (isPlaintext(buffer)) {
                    sb.append(buffer.readString(charset)).append("\n");
                    sb.append("--> END " + request.method()
                            + " (" + requestBody.contentLength() + "-byte body)\n");
                } else {
                    sb.append("--> END " + request.method() + " (binary "
                            + requestBody.contentLength() + "-byte body omitted)\n");
                }
            }
        }

        long startNs = System.nanoTime();
        Response response;
        try {
            response = chain.proceed(request);
        } catch (Exception e) {
            sb.append("<-- HTTP FAILED: " + e + "\n");
            throw e;
        }
        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

        ResponseBody responseBody = response.body();
        long contentLength = responseBody.contentLength();
        String bodySize = contentLength != -1 ? contentLength + "-byte" : "unknown-length";
        sb.append("<-- " + response.code() + ' ' + response.message() + ' '
                + response.request().url() + " (" + tookMs + "ms" + (false ? ", "
                + bodySize + " body" : "") + ')' + "\n");

        {
            Headers headers = response.headers();
            for (int i = 0, count = headers.size(); i < count; i++) {
                sb.append(headers.name(i) + ": " + headers.value(i) + "\n");
            }

            if (!HttpHeaders.hasBody(response)) {
                sb.append("<-- END HTTP" + "\n");
            } else if (bodyEncoded(response.headers())) {
                sb.append("<-- END HTTP (encoded body omitted)\n");
            } else {
                BufferedSource source = responseBody.source();
                source.request(Long.MAX_VALUE); // Buffer the entire body.
                Buffer buffer = source.buffer();

                Charset charset = UTF8;
                MediaType contentType = responseBody.contentType();
                if (contentType != null) {
                    try {
                        charset = contentType.charset(UTF8);
                    } catch (UnsupportedCharsetException e) {
                        sb.append("Couldn't decode the response body; charset is likely malformed.\n");
                        sb.append("<-- END HTTP\n");
                        logger.log(sb.toString());
                        return response;
                    }
                }

                if (!isPlaintext(buffer)) {
                    sb.append("<-- END HTTP (binary " + buffer.size() + "-byte body omitted)\n");
                    logger.log(sb.toString());
                    return response;
                }

                if (contentLength != 0) {
                    sb.append("\n")
                            .append(buffer.clone().readString(charset) + "\n");
                }
                sb.append("<-- END HTTP (" + buffer.size() + "-byte body)" + "\n");
            }
        }

        logger.log(sb.toString());
        return response;
    }

    /**
     * Returns true if the body in question probably contains human readable text. Uses a small sample
     * of code points to detect unicode control characters commonly used in binary file signatures.
     */
    static boolean isPlaintext(Buffer buffer) {
        try {
            Buffer prefix = new Buffer();
            long byteCount = buffer.size() < 64 ? buffer.size() : 64;
            buffer.copyTo(prefix, 0, byteCount);
            for (int i = 0; i < 16; i++) {
                if (prefix.exhausted()) {
                    break;
                }
                int codePoint = prefix.readUtf8CodePoint();
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false;
                }
            }
            return true;
        } catch (EOFException e) {
            return false; // Truncated UTF-8 sequence.
        }
    }

    private boolean bodyEncoded(Headers headers) {
        String contentEncoding = headers.get("Content-Encoding");
        return contentEncoding != null && !contentEncoding.equalsIgnoreCase("identity");
    }
}