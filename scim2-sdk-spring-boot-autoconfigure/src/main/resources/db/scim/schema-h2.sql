CREATE TABLE IF NOT EXISTS scim_resources (
    id              VARCHAR(255) NOT NULL PRIMARY KEY,
    resource_type   VARCHAR(100) NOT NULL,
    external_id     VARCHAR(255),
    display_name    VARCHAR(500),
    resource_json   CLOB NOT NULL,
    version         BIGINT NOT NULL DEFAULT 1,
    created         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_scim_resources_type ON scim_resources (resource_type);
CREATE INDEX IF NOT EXISTS idx_scim_resources_external_id ON scim_resources (resource_type, external_id);
