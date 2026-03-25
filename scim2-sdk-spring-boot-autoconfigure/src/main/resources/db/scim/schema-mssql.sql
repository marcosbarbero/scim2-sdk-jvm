IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'scim_resources')
CREATE TABLE scim_resources (
    id              NVARCHAR(255) NOT NULL PRIMARY KEY,
    resource_type   NVARCHAR(100) NOT NULL,
    external_id     NVARCHAR(255),
    display_name    NVARCHAR(500),
    resource_json   NVARCHAR(MAX) NOT NULL,
    version         BIGINT NOT NULL DEFAULT 1,
    created         DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    last_modified   DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET()
);

CREATE INDEX idx_scim_resources_type ON scim_resources (resource_type);
CREATE INDEX idx_scim_resources_external_id ON scim_resources (resource_type, external_id);
CREATE INDEX idx_scim_resources_display_name ON scim_resources (resource_type, display_name);
