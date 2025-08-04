package io.wispershadow.tech.common.reverseproxy

import io.wispershadow.tech.common.reverseproxy.config.ReverseProxySettingProperties
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.*
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.multipart.MultipartHttpServletRequest
import org.springframework.web.multipart.MultipartResolver
import org.springframework.web.multipart.support.StandardServletMultipartResolver
import org.springframework.web.util.UriComponentsBuilder
import java.io.OutputStream
import java.net.HttpCookie
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.*
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.Part
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException

object ReverseProxyUtils {
    private val logger: Logger = LoggerFactory.getLogger(ReverseProxyUtils::class.java)

    public val hopByHopHeaders: List<String> = mutableListOf(
        "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
        "te", "trailers", "transfer-encoding", "upgrade"
    )

    object ProxyCookieUtils {
        private const val COOKIE_KEY_SAMESITE = "samesite"
        private const val COOKIE_KEY_SECURE = "secure"
        private const val COOKIE_VALUE_STRICT = "strict"
        private const val COOKIE_VALUE_LAX = "lax"
        private const val COOKIE_VALUE_NONE = "none"

        private fun shouldRetainCookie(servletRequest: HttpServletRequest,
                                       servletResponse: HttpServletResponse, headerValue: String, cookie: HttpCookie,
                                       reverseProxySettingProperties: ReverseProxySettingProperties, discardReason: StringBuilder
        ): Boolean {
            val attributes = headerValue.split(";")
            var requireSecure = false
            var containsSecure = false
            attributes.forEach {attribute ->
                val keyValue = attribute.split("=")
                if (keyValue.isNotEmpty()) {
                    val key = keyValue[0].trim()
                    if (COOKIE_KEY_SAMESITE.equals(key, ignoreCase = true)) {
                        val value = if (keyValue.size > 1) {
                            URLDecoder.decode(keyValue[1].trim(), StandardCharsets.UTF_8)
                        }
                        else {
                            ""
                        }
                        if (COOKIE_VALUE_STRICT.equals(value, ignoreCase = true)) {
                            val requestServerName = servletRequest.serverName
                            val targetUri = URI.create(reverseProxySettingProperties.targetUriRoot)
                            val cookieOrigin = if ((cookie.domain != null)) cookie.domain else targetUri.host
                            if (!requestServerName.equals(cookieOrigin, ignoreCase = true)) {
                                discardReason.append("Samesite=Strict flag does not match request domain: ")
                                    .append(requestServerName).append(" and cookie origin: ").append(cookieOrigin)
                                return false
                            }
                        } else if (value.equals(COOKIE_VALUE_LAX, ignoreCase = true)) {
                            if (!HttpMethod.GET.matches(servletRequest.method)) {
                                discardReason.append("Samesite=Lax does not match request method: " + servletRequest.method)
                                return false
                            }
                        } else if (value.equals(COOKIE_VALUE_NONE, ignoreCase = true)) {
                            requireSecure = true
                        }
                    }
                    else if (COOKIE_KEY_SECURE.equals(key, ignoreCase = true)) {
                        containsSecure = true
                        if (!servletRequest.isSecure) {
                            discardReason.append("Secure flag does not match request protocol")
                            return false
                        }
                    }
                }

            }
            if (requireSecure && !containsSecure) {
                discardReason.append("Secure flag is required but not set for samesite=none")
                return false
            }
            return true
        }

        /**
         * Creates a proxy cookie from the original cookie.
         *
         * @param servletRequest original request
         * @param cookie original cookie
         * @return proxy cookie
         */
        private fun createProxyCookie(
            servletRequest: HttpServletRequest, cookie: HttpCookie,
            reverseProxySettingProperties: ReverseProxySettingProperties
        ): Cookie {
            return Cookie(cookie.name, cookie.value).apply {
                this.path = if (reverseProxySettingProperties.preserveCookiePath) cookie.path else  // preserve original cookie path
                    buildProxyCookiePath(servletRequest)
                this.maxAge = cookie.maxAge.toInt()
                // don't set cookie domain
                this.secure = servletRequest.isSecure && cookie.secure
                this.isHttpOnly = cookie.isHttpOnly
            }
        }

        /**
         * Create path for proxy cookie.
         *
         * @param servletRequest original request
         * @return proxy cookie path
         */
        private fun buildProxyCookiePath(servletRequest: HttpServletRequest): String {
            var path = servletRequest.contextPath // path starts with / or is empty string
            path += servletRequest.servletPath // servlet path starts with / or is empty string
            if (path.isEmpty()) {
                path = "/"
            }
            return path
        }


        fun copyProxyCookie(
            servletRequest: HttpServletRequest,
            servletResponse: HttpServletResponse, headerValue: String,
            reverseProxySettingProperties: ReverseProxySettingProperties
        ) {
            for (cookie in HttpCookie.parse(headerValue)) {
                val discardReason = StringBuilder()
                if (shouldRetainCookie(
                        servletRequest, servletResponse, headerValue, cookie,
                        reverseProxySettingProperties, discardReason
                    )
                ) {
                    val servletCookie: Cookie = createProxyCookie(servletRequest, cookie,
                        reverseProxySettingProperties)
                    servletResponse.addCookie(servletCookie)
                } else {
                    logger.warn("Discarding cookie: {}. Reason: {}", cookie, discardReason)
                }
            }
        }
    }

    object UrlHandler {
        public fun mapTargetUrlFromRequest(servletRequest: HttpServletRequest, reverseProxySettingProperties: ReverseProxySettingProperties): String {
            val requestUri = servletRequest.requestURI
            val prefix: String = reverseProxySettingProperties.sourceUriPrefix
            val targetUri = URI.create(reverseProxySettingProperties.targetUriRoot)
            require(!(targetUri.scheme == null || targetUri.host == null)) { "Missing target scheme or target host: " + reverseProxySettingProperties.targetUriRoot }
            if (requestUri != null) {
                if (requestUri.startsWith(prefix)) {
                    val sourceBuilder = UriComponentsBuilder.fromUriString(requestUri)
                    sourceBuilder.scheme(targetUri.scheme)
                    sourceBuilder.host(targetUri.host)
                    if (targetUri.port > 0) {
                        sourceBuilder.port(targetUri.port)
                    }
                    val completeTargetPath = buildCompleteTargetPath(requestUri, prefix, targetUri)
                    sourceBuilder.replacePath(completeTargetPath.toString())

                    val queryString = servletRequest.queryString
                    if (queryString != null) {
                        sourceBuilder.replaceQuery(queryString)
                    }
                    return sourceBuilder.build().toUriString()
                }
                else {
                    throw IllegalArgumentException("Request URI ${requestUri} does not start with prefix ${prefix}")
                }
            } else {
                throw IllegalArgumentException("Request URI or prefix is null")
            }
        }

        private fun buildCompleteTargetPath(requestUri: String, prefix: String, targetUri: URI): StringBuilder {
            try {
                val remaining = requestUri.substring(prefix.length)
                val targetPath = targetUri.path
                val completeTargetPath = java.lang.StringBuilder(500)
                if (targetPath != null) {
                    completeTargetPath.append(targetPath)
                }
                completeTargetPath.append(remaining)
                if (completeTargetPath.isEmpty() || completeTargetPath[0] != '/') {
                    completeTargetPath.insert(0, '/')
                }
                return completeTargetPath
            } catch (e: Exception) {
                throw RuntimeException(e.message)
            }
        }

    }

    object RequestHandler {

        private const val HEADER_NAME_XFORWARDEDFOR = "X-Forwarded-For"
        private const val HEADER_NAME_XFORWARDEDPROTO = "X-Forwarded-Proto"

        public fun prepareRequestEntity(requestUri: URI, httpMethod: HttpMethod, sourceRequest: HttpServletRequest,
                                        reverseProxySettingProperties: ReverseProxySettingProperties): RequestEntity<*> {
            val headers = HttpHeaders()
            headers.set(HttpHeaders.CONTENT_LENGTH, getContentLength(sourceRequest).toString())
            copyRequestHeaders(sourceRequest, headers, reverseProxySettingProperties)
            setXForwardedForHeader(sourceRequest, headers, reverseProxySettingProperties)
            if (hasRequestBody(sourceRequest)) {
                val multipartResolver = StandardServletMultipartResolver()
                if (isMultipartRequest(sourceRequest, multipartResolver)) {
                    val bodyParts: MultiValueMap<String, Any>  = LinkedMultiValueMap()
                    handleMultipartRequest(sourceRequest, headers, bodyParts, multipartResolver);
                    return RequestEntity(bodyParts, headers, httpMethod, requestUri);
                }
                else {
                    val inputStreamResource = copyMessageBody(sourceRequest)
                    return RequestEntity(inputStreamResource, headers, httpMethod, requestUri);
                }
            }
            else {
                return RequestEntity<Any>(headers, httpMethod, requestUri);
            }
        }


        private fun isMultipartRequest(request: HttpServletRequest, multipartResolver: MultipartResolver): Boolean {
            return (request is MultipartHttpServletRequest) || multipartResolver.isMultipart(request)
        }

        private fun handleMultipartRequest(sourceRequest: HttpServletRequest,
                                           headers: HttpHeaders, body: MultiValueMap<String, Any>,
                                           multipartResolver: MultipartResolver) {
            val multipartHttpServletRequest:  MultipartHttpServletRequest = if (sourceRequest is MultipartHttpServletRequest) { sourceRequest } else { multipartResolver.resolveMultipart(sourceRequest) }
            val executedFileParts = mutableSetOf<String>()
            multipartHttpServletRequest.fileMap.forEach{ (name, file) ->
                body.add(name, SuppressContentLengthResource(file.resource))
                executedFileParts.add(name)
            }
            multipartHttpServletRequest.parts.forEach{ part: Part ->
                val partName = part.name
                if (!executedFileParts.contains(partName)) {
                    body.add(part.name, String(part.inputStream.readAllBytes()))
                }
            }
        }



        private fun copyMessageBody(
            sourceRequest: HttpServletRequest
        ): InputStreamResource {
            return InputStreamResource(sourceRequest.inputStream)
        }

        private fun hasRequestBody(request: HttpServletRequest): Boolean {
            //spec: RFC 2616, sec 4.3: either of these two headers signal that there is a message body.
            return request.getHeader(HttpHeaders.CONTENT_LENGTH) != null ||
                    request.getHeader(HttpHeaders.TRANSFER_ENCODING) != null
        }

        private fun getContentLength(request: HttpServletRequest): Long {
            val contentLengthHeader =  request.getHeader(HttpHeaders.CONTENT_LENGTH)
            return Optional.ofNullable(contentLengthHeader).map {
                it.toLong()
            }.orElseGet {
                -1L
            }
        }

        private fun copyRequestHeaders(
            sourceRequest: HttpServletRequest, httpHeaders: HttpHeaders, reverseProxySettingProperties: ReverseProxySettingProperties
        ) {
            val enumerationOfHeaderNames = sourceRequest.headerNames
            while (enumerationOfHeaderNames.hasMoreElements()) {
                val headerName = enumerationOfHeaderNames.nextElement()
                copyRequestHeader(sourceRequest, httpHeaders, headerName, reverseProxySettingProperties)
            }
        }


        private fun copyRequestHeader(
            sourceRequest: HttpServletRequest,
            httpHeaders: HttpHeaders, headerName: String,
            reverseProxySettingProperties: ReverseProxySettingProperties
        ) {
            if (headerName.equals(HttpHeaders.CONTENT_LENGTH, ignoreCase = true)) {
                return
            }
            if (hopByHopHeaders.contains(headerName.lowercase())) {
                return
            }
            if (reverseProxySettingProperties.handleCompression && headerName.equals(HttpHeaders.ACCEPT_ENCODING, ignoreCase = true)) {
                return
            }
            val headers = sourceRequest.getHeaders(headerName)
            while (headers.hasMoreElements()) {
                var headerValue = headers.nextElement()
                //rewrite host header
                if ((!reverseProxySettingProperties.preserveHost) && headerName.equals(HttpHeaders.HOST, ignoreCase = true)) {
                    headerValue = getTargetHostHeaderValue(sourceRequest, reverseProxySettingProperties)
                }
                httpHeaders.add(headerName, headerValue)
            }
        }


        private fun getTargetHostHeaderValue(servletRequest: HttpServletRequest, reverseProxySettingProperties: ReverseProxySettingProperties
        ): String {
            val targetUri = URI.create(reverseProxySettingProperties.targetUriRoot)
            return getHostNameFromUri(targetUri)
        }


        private fun setXForwardedForHeader(
            servletRequest: HttpServletRequest,
            httpHeaders: HttpHeaders,
            reverseProxySettingProperties: ReverseProxySettingProperties
        ) {
            if (reverseProxySettingProperties.forwardIP) {
                val forHeaderName = HEADER_NAME_XFORWARDEDFOR
                var forHeader = servletRequest.remoteAddr
                val existingForHeader = servletRequest.getHeader(forHeaderName)
                if (existingForHeader != null) {
                    forHeader = "$existingForHeader, $forHeader"
                }
                httpHeaders.add(forHeaderName, forHeader)

                val protoHeaderName = HEADER_NAME_XFORWARDEDPROTO
                val protoHeader = servletRequest.scheme
                httpHeaders.add(protoHeaderName, protoHeader)
            }
        }

    }

    object ResponseHandler {
        private val logger: Logger = LoggerFactory.getLogger(ResponseHandler::class.java)

        fun convertResponseEntity(responseEntity: ResponseEntity<Resource>, servletRequest: HttpServletRequest,
                                  servletResponse: HttpServletResponse, reverseProxySettingProperties: ReverseProxySettingProperties) {
            copyResponseHeaders(responseEntity, servletRequest, servletResponse, reverseProxySettingProperties)
            val statusCode = responseEntity.statusCode;
            servletResponse.status = statusCode.value()
            if (statusCode.value() == HttpStatus.NOT_MODIFIED.value()) {
                servletResponse.setContentLength(0)
            } else {
                copyResponseEntity(responseEntity, servletRequest, servletResponse, reverseProxySettingProperties)
            }
        }


        private fun copyResponseHeaders(responseEntity: ResponseEntity<Resource>, servletRequest: HttpServletRequest,
                                        servletResponse: HttpServletResponse, reverseProxySettingProperties: ReverseProxySettingProperties) {
            responseEntity.headers.forEach { headerName, headerValues ->
                headerValues.forEach {headerValue ->
                    copyResponseHeader(servletRequest, servletResponse, headerName, headerValue, reverseProxySettingProperties)
                }
            }
        }

        private fun copyResponseHeader(servletRequest: HttpServletRequest, servletResponse: HttpServletResponse, headerName: String, headerValue: String,
                                       reverseProxySettingProperties: ReverseProxySettingProperties) {
            if (hopByHopHeaders.contains(headerName)) {
                return
            }
            if (headerName.equals(HttpHeaders.SET_COOKIE, ignoreCase = true) ||
                headerName.equals(HttpHeaders.SET_COOKIE2, ignoreCase = true)) {
                ProxyCookieUtils.copyProxyCookie(servletRequest, servletResponse, headerValue, reverseProxySettingProperties)

            } else if (headerName.equals(HttpHeaders.LOCATION, ignoreCase = true)) {
                // LOCATION Header may have to be rewritten.
                val modifiedHeaderValue: String = rewriteHeaderValueFromResponse(
                        servletRequest,
                        headerValue,
                        reverseProxySettingProperties
                    )
                logger.debug("Adding response header: name={}, value={}", headerName, modifiedHeaderValue)
                servletResponse.addHeader(headerName, modifiedHeaderValue)
            } else {
                logger.debug("Adding response header: name={}, value={}", headerName, headerValue)
                servletResponse.addHeader(headerName, headerValue)
            }
        }

        fun rewriteHeaderValueFromResponse(servletRequest: HttpServletRequest,  responseHostHeaderValue: String,
                                           reverseProxySettingProperties: ReverseProxySettingProperties): String {
            val targetUriRoot = reverseProxySettingProperties.targetUriRoot
            if (hostMatches(targetUriRoot, responseHostHeaderValue)) {
                /*-
                 * The URL points back to the back-end server.
                 * replace the backend server with the reverse proxy's host
                 */
                val curUrl = servletRequest.requestURL;
                return getHostNameFromUri(URI.create(curUrl.toString()))
            }
            return responseHostHeaderValue
        }

        private fun hostMatches(targetUriRoot: String, hostName: String): Boolean {
            val hostNameFromUri = getHostNameFromUri(URI.create(targetUriRoot))
            return hostNameFromUri.equals(hostName, ignoreCase = true)
        }

        fun copyResponseEntity(responseEntity: ResponseEntity<Resource>, servletRequest: HttpServletRequest,
                               servletResponse: HttpServletResponse, reverseProxySettingProperties: ReverseProxySettingProperties) {
            val body: Resource? = responseEntity.body
            body?.let {
                val inputStream = body.inputStream
                val os: OutputStream = servletResponse.outputStream
                val buffer = ByteArray(10 * 1024)
                var read: Int
                var total = 0
                while ((inputStream.read(buffer).also { read = it }) != -1) {
                    os.write(buffer, 0, read)
                    total += read
                    /*-
                     * Issue in Apache http client/JDK: if the stream from client is
                     * compressed, apache http client will delegate to GzipInputStream.
                     * The #available implementation of InflaterInputStream (parent of
                     * GzipInputStream) return 1 until EOF is reached. This is not
                     * consistent with InputStream#available, which defines:
                     *
                     *   A single read or skip of this many bytes will not block,
                     *   but may read or skip fewer bytes.
                     *
                     *  To work around this, a flush is issued always if compression
                     *  is handled by apache http client
                     */
                    if (reverseProxySettingProperties.handleCompression || inputStream.available() == 0 /* next is.read will block */) {
                        os.flush()
                    }
                }
                logger.debug("Conversion of response completed, total number of bytes copied: {}", total)
            }
            if (body == null) {
                logger.warn("Response body is null")
            }
        }

    }

    object ErrorHandler {
        fun handleException(exception: Exception, request: HttpServletRequest,
                            response: HttpServletResponse,
                            reverseProxySettingProperties: ReverseProxySettingProperties) {
            if (exception is HttpClientErrorException) {
                response.status = exception.statusCode.value()
                response.outputStream.write(exception.responseBodyAsByteArray)
            }
            else if (exception is HttpServerErrorException) {
                response.status = exception.statusCode.value()
                response.outputStream.write(exception.responseBodyAsByteArray)
            }
            else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, exception.message)
            }
        }
    }

    private fun getHostNameFromUri(targetUri: URI): String {
        return targetUri.host +
                (if (targetUri.port > 0) ":" + targetUri.port else "")
    }

}