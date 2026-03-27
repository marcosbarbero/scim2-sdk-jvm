# Release Guide

## Prerequisites

### 1. Sonatype Central Portal Account

Register at [central.sonatype.com](https://central.sonatype.com) and claim the `com.marcosbarbero` namespace.

Verification: Add a DNS TXT record to `marcosbarbero.com` with the verification code provided by Sonatype, or use the GitHub-based verification if available.

### 2. GPG Key

Generate a GPG key for signing artifacts:

```bash
gpg --full-generate-key
# Choose RSA, 4096 bits, no expiration, your name and email

# Export and publish the public key
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
gpg --keyserver keys.openpgp.org --send-keys YOUR_KEY_ID

# Export the private key for CI (base64 encoded)
gpg --export-secret-keys YOUR_KEY_ID | base64 > gpg-private-key.b64
```

### 3. GitHub Repository Secrets

Configure these secrets in the repository settings (Settings > Secrets > Actions):

| Secret | Description |
|---|---|
| `SONATYPE_USERNAME` | Sonatype Central Portal username (or token) |
| `SONATYPE_PASSWORD` | Sonatype Central Portal password (or token) |
| `GPG_PRIVATE_KEY` | Base64-encoded GPG private key |
| `GPG_PASSPHRASE` | Passphrase for the GPG key |

### 4. Maven Settings

The CI workflow uses these secrets automatically. For local releases, configure `~/.m2/settings.xml`:

```xml
<settings>
    <servers>
        <server>
            <id>central</id>
            <username>${env.SONATYPE_USERNAME}</username>
            <password>${env.SONATYPE_PASSWORD}</password>
        </server>
    </servers>
</settings>
```

## Release Process

Releases are fully automated via [Conventional Commits](https://www.conventionalcommits.org/) and semantic versioning.

### How it works

1. Every push to `main` triggers the `release.yml` workflow
2. The workflow analyzes commit messages since the last tag using [github-tag-action](https://github.com/mathieudutour/github-tag-action)
3. It determines the version bump type based on conventional commit prefixes:
   - `fix:` → **patch** bump (e.g., 1.0.0 → 1.0.1)
   - `feat:` → **minor** bump (e.g., 1.0.0 → 1.1.0)
   - `feat!:` or `BREAKING CHANGE:` → **major** bump (e.g., 1.0.0 → 2.0.0)
4. A new `v*` tag and GitHub Release are created automatically
5. The tag push triggers the publish job, which builds, signs, and deploys to Maven Central
6. The `central-publishing-maven-plugin` auto-publishes and waits until artifacts are live

### Manual release (if needed)

You can still create a release manually:

#### Option A: Release via GitHub UI

1. Go to the repository -> Releases -> "Create a new release"
2. Create a new tag (e.g., `v1.0.0`)
3. Add release title and notes
4. Click "Publish release"

#### Option B: Release via command line

```bash
git tag v1.0.0
git push origin v1.0.0
```

### Commit message conventions

Follow [Conventional Commits](https://www.conventionalcommits.org/) to control versioning:

```
fix: resolve null pointer in ScimUser parser        → patch
feat: add bulk operations support                   → minor
feat!: redesign ResourceHandler SPI                 → major
refactor: simplify filter parsing                   → patch (default bump)
docs: update API documentation                      → patch (default bump)
```

## Version Management

The project uses `${revision}` with `flatten-maven-plugin` for CI-friendly versioning:
- Development: `0.1.0-SNAPSHOT` (set in parent pom.xml)
- Release: version derived automatically from conventional commits, passed as `-Drevision=X.Y.Z`

## Checklist for 1.0.0-M1

- [ ] All tests pass (`mvn clean verify`)
- [ ] Mutation tests pass (`mvn verify -Pmutation -pl scim2-sdk-core`)
- [ ] Pact contract tests pass
- [ ] Documentation is up to date
- [ ] CLAUDE.md is current
- [ ] ADRs are complete
- [ ] GPG key published to key servers
- [ ] GitHub secrets configured
- [ ] Sonatype namespace claimed and verified
