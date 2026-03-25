# SCIM 2.0 SDK :: BOM (Bill of Materials)

Import this BOM to manage all SCIM 2.0 SDK module versions consistently.

## Usage

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.marcosbarbero</groupId>
            <artifactId>scim2-sdk-bom</artifactId>
            <version>${scim2-sdk.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- Then use modules without version: -->
<dependency>
    <groupId>com.marcosbarbero</groupId>
    <artifactId>scim2-sdk-core</artifactId>
</dependency>
```
