package io.wispershadow.tech.common.testbackend.controller

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.*
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.Part
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI
import java.nio.file.Paths
import java.util.*

@RestController
open class ReactiveBackendController {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ReactiveBackendController::class.java)
    }

    @GetMapping(value = ["/json"], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun getJson(request: ServerHttpRequest): Mono<Map<String, Any>> {
        request.headers.forEach{(key, value) ->
            logger.info("Header name: {} Value: {}", key, value)
        }
        request.cookies.forEach{(key, value) ->
            logger.info("Cookie name: {} Value: {}", key, value)
        }
        return Mono.just(java.util.Map.of<String, Any>("key1", "value1", "key2", "value2"))
    }

    @PutMapping(value = ["/json"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun putWithJson(@RequestBody requestBody: Map<String, Any>) {
        logger.info("Received request body: {}", requestBody)
    }

    @PostMapping(value = ["/form"], consumes = ["application/x-www-form-urlencoded"])
    fun postWithForm(exchange: ServerWebExchange): Mono<String> {
        val request = exchange.request
        request.headers.forEach{(key, value) ->
            logger.info("Header name: {} Value: {}", key, value)
        }
        request.cookies.forEach{(key, value) ->
            logger.info("Cookie name: {} Value: {}", key, value)
        }
        return exchange.formData.map { formData: MultiValueMap<String, String> ->
            formData.forEach { (key: String, value: List<String?>) ->
                logger.info("Form Key: $key Value: $value")
            }
            ""
        }.then(Mono.just("OK"))
    }


    @PostMapping(value = ["/multipartupload"], consumes = ["multipart/form-data"])
    fun uploadMultipartForm(
        request: ServerHttpRequest,
        @RequestBody partsMono: Mono<MultiValueMap<String, Part>>
    ): Mono<String> {
        return partsMono.flatMap { parts ->
            parts.forEach { (key, partList) ->
                partList.forEach { part ->
                    when (part) {
                        is FilePart -> {
                            logger.info("Received file part: {}", part.filename())
                        }
                        else -> {
                           part.content().map {
                               dataBuffer: DataBuffer ->
                               val bytes = ByteArray(dataBuffer.readableByteCount())
                               dataBuffer.read(bytes)
                               String(bytes)
                            }.subscribe {
                                logger.info("Received part: {} with content: {}", key, it)
                            }
                        }
                    }
                }
            }
            Mono.just("OK")
        }.onErrorResume { error ->
            logger.error("Error processing multipart upload", error)
            Mono.just("Error processing upload: ${error.message}")
        }
    }

    @PostMapping(value = ["/upload"], consumes = ["multipart/form-data"])
    fun uploadFileWithoutEntity(@RequestPart("files") filePartFlux: Flux<Part>): Mono<String> {
        return filePartFlux.flatMap { file ->
            logger.info("Recived file upload request, file name = {}", file.name())
            file.content()
        }.map{dataBuffer ->
            val dataInputStream = dataBuffer.asInputStream();
            val fileContent = String(dataInputStream.readAllBytes(), Charsets.UTF_8)
            logger.info("Received file upload request, file content = {}", fileContent)
        }.then(Mono.just("OK")).onErrorResume { error ->
            logger.error("Error processing file upload", error)
            Mono.just("Error uploading files: ${error.message}")
        }
    }

    @GetMapping(value = ["/download/{fileName}"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun downloadFileWithEntity(@PathVariable("fileName") fileName: String): Mono<ResponseEntity<Resource>> {
        val fileResourceMono = Mono.fromCallable<Resource> {
            //val path = Paths.get(fileName).toAbsolutePath().normalize().toString()
            //FileSystemResource(path)
            ClassPathResource(fileName)
        }
        return fileResourceMono.flatMap { resource: Resource ->
            val headers = HttpHeaders()
            headers.setContentDispositionFormData(fileName, fileName)
            Mono.just(
                ResponseEntity
                    .ok().cacheControl(CacheControl.noCache())
                    .headers(headers)
                    .body(resource)
            )
        }.onErrorResume { exception: Throwable ->
            Mono.just(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build()
            )
        }
    }



    @GetMapping(value = ["/redirect"])
    fun redirect(request: ServerHttpRequest, response: ServerHttpResponse): Mono<Void> {
        return Mono.fromRunnable(Runnable {
            val directPath: String? = request.queryParams.getFirst("target")
            directPath?.let {
                logger.info("Redirecting to: {}", it)
                response.setStatusCode(HttpStatus.FOUND)
                response.headers.location = URI.create(directPath)
            } ?: run {
                logger.warn("No target provided for redirect")
                response.setStatusCode(HttpStatus.BAD_REQUEST)
            }
        })
    }

    @GetMapping(value = ["/notModified"])
    fun notModified(response: ServerHttpResponse): Mono<Void> {
        response.setStatusCode(HttpStatus.NOT_MODIFIED)
        return response.setComplete()
    }

    private fun buildCookie(attributes: Map<String, String>, domain: Optional<String>,
                            sameSite: String = "Strict", httpOnly: Boolean = false,
                            secure: Boolean = false): List<ResponseCookie> {
        val cookies: MutableList<ResponseCookie> = mutableListOf()
        attributes.forEach { (key: String, value: String) ->
            val cookieBuilder = ResponseCookie.from(key, value)
                .path("/")
                .httpOnly(httpOnly)
                .secure(secure)
                .sameSite(sameSite)
                .maxAge(3600)
            val cookie = domain.map {
                cookieBuilder.domain(it)
            }.orElseGet {
                cookieBuilder
            }.build()
            cookies.add(cookie)
        }
        return cookies
    }
}