package io.wispershadow.tech.common.utils

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

object SpringContextUtils: ApplicationContextAware {

    @JvmStatic
    private var applicationContext: ApplicationContext? = null

    override fun setApplicationContext(p0: ApplicationContext) {
        applicationContext = p0
    }


    @JvmStatic
    fun <T> getBean(beanClass: Class<T>): T {
        return applicationContext!!.getBean(beanClass)
    }

    @JvmStatic
    fun <T> getBean(name: String, beanClass: Class<T>): T {
        return applicationContext!!.getBean(name, beanClass)
    }
}