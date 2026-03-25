package com.marcosbarbero.scim2.spring.persistence.repository

import com.marcosbarbero.scim2.spring.persistence.entity.ScimResourceEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ScimResourceJpaRepository : JpaRepository<ScimResourceEntity, String> {
    fun findByResourceType(resourceType: String, pageable: Pageable): Page<ScimResourceEntity>
    fun findByResourceTypeAndExternalId(resourceType: String, externalId: String): ScimResourceEntity?
    fun countByResourceType(resourceType: String): Long
    fun deleteByIdAndResourceType(id: String, resourceType: String): Long
}
