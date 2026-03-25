CREATE TABLE scim_resources (
    id              VARCHAR2(255) NOT NULL PRIMARY KEY,
    resource_type   VARCHAR2(100) NOT NULL,
    external_id     VARCHAR2(255),
    display_name    VARCHAR2(500),
    resource_json   CLOB NOT NULL,
    version         NUMBER(19) DEFAULT 1 NOT NULL,
    created         TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    last_modified   TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL
);

CREATE INDEX idx_scim_resources_type ON scim_resources (resource_type);
CREATE INDEX idx_scim_resources_external_id ON scim_resources (resource_type, external_id);
CREATE INDEX idx_scim_resources_display_name ON scim_resources (resource_type, display_name);
