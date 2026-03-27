-- SCIM 2.0 SDK: Resource storage table
-- This migration creates the scim_resources table for storing SCIM resources as JSON.
-- Compatible with: H2, PostgreSQL, MySQL
-- For database-specific schemas, see db/scim/schema-{database}.sql
-- Requires: spring-boot-starter-flyway + database-specific Flyway module (e.g., flyway-database-postgresql)

CREATE TABLE IF NOT EXISTS scim_resources (
    id              VARCHAR(255) NOT NULL PRIMARY KEY,
    resource_type   VARCHAR(100) NOT NULL,
    external_id     VARCHAR(255),
    display_name    VARCHAR(500),
    resource_json   TEXT NOT NULL,
    version         BIGINT NOT NULL DEFAULT 1,
    created         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_scim_resources_type ON scim_resources (resource_type);
CREATE INDEX IF NOT EXISTS idx_scim_resources_external_id ON scim_resources (resource_type, external_id);
CREATE INDEX IF NOT EXISTS idx_scim_resources_display_name ON scim_resources (resource_type, display_name);
