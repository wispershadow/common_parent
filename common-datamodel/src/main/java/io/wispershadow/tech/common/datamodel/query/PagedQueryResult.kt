package io.wispershadow.tech.common.datamodel.query

class PagedQueryResult<T> (val totalCount: Long, val currentPageData: List<T>)