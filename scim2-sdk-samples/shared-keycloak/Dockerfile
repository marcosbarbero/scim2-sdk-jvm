FROM maven:3.9-eclipse-temurin-17 AS plugin-build
WORKDIR /build
RUN git clone https://github.com/suvera/keycloak-scim2-storage.git . && \
    mvn clean install -DskipTests -B

FROM quay.io/keycloak/keycloak:26.0
COPY --from=plugin-build /build/target/suvera-keycloak-scim2-outbound-provisioning-jar-with-dependencies.jar /opt/keycloak/providers/
COPY entrypoint.sh /opt/keycloak/entrypoint.sh
USER root
RUN chmod +x /opt/keycloak/entrypoint.sh
USER keycloak
ENTRYPOINT ["/opt/keycloak/entrypoint.sh"]
