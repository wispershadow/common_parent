package io.wispershadow.tech.common.reverseproxy

import org.springframework.core.io.AbstractResource
import org.springframework.core.io.Resource
import java.io.InputStream

class SuppressContentLengthResource(private val delegateResource: Resource): AbstractResource() {

    override fun exists(): Boolean {
        return delegateResource.exists()
    }

    override fun isOpen(): Boolean {
        return delegateResource.isOpen
    }

    override fun contentLength(): Long {
        return -1
    }

    override fun getFilename(): String? {
        return delegateResource.filename
    }

    override fun getInputStream(): InputStream {
        return delegateResource.inputStream
    }

    override fun getDescription(): String {
        return delegateResource.description
    }
}