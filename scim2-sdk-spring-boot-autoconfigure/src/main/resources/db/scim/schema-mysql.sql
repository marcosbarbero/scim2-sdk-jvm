CREATE TABLE IF NOT EXISTS scim_resources (
    id              VARCHAR(255) NOT NULL PRIMARY KEY,
    resource_type   VARCHAR(100) NOT NULL,
    external_id     VARCHAR(255),
    display_name    VARCHAR(500),
    resource_json   LONGTEXT NOT NULL,
    version         BIGINT NOT NULL DEFAULT 1,
    created         TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    last_modified   TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_scim_resources_type ON scim_resources (resource_type);
CREATE INDEX idx_scim_resources_external_id ON scim_resources (resource_type, external_id);
CREATE INDEX idx_scim_resources_display_name ON scim_resources (resource_type, display_name);
