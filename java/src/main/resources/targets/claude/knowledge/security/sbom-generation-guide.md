# SBOM Generation Guide

## Overview

A Software Bill of Materials (SBOM) is a formal, machine-readable inventory of all components, libraries, and modules in a software artifact. SBOMs enable automated vulnerability tracking, license compliance, and supply chain risk management.

## CycloneDX vs SPDX

| Aspect | CycloneDX | SPDX |
|--------|-----------|------|
| Standard body | OWASP | Linux Foundation (ISO/IEC 5962:2021) |
| Primary focus | Security, vulnerability management | License compliance |
| Formats | JSON, XML, Protocol Buffers | JSON, RDF, YAML, tag-value, spreadsheet |
| VEX support | Native (built-in) | Via external documents |
| Services BOM | Yes (services, APIs) | Limited |
| ML/AI BOM | Yes (datasets, models) | Limited |
| Dependency graph | Full graph with relationship types | Flat list with relationships |
| Ecosystem tooling | Broad (CycloneDX CLI, cdxgen) | Broad (SPDX Tools, sbom-tool) |

**Recommendation:** Use CycloneDX for security-focused workflows and SPDX when ISO compliance or license auditing is the primary concern. Many organizations generate both.

## Generation by Build Tool

### Maven (Java/Kotlin)

**CycloneDX:**
```xml
<plugin>
    <groupId>org.cyclonedx</groupId>
    <artifactId>cyclonedx-maven-plugin</artifactId>
    <version>2.9.1</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>makeAggregateBom</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <projectType>application</projectType>
        <outputFormat>json</outputFormat>
        <outputName>sbom</outputName>
        <includeLicenseText>false</includeLicenseText>
    </configuration>
</plugin>
```

Command: `mvn cyclonedx:makeAggregateBom`

### Gradle (Java/Kotlin)

**CycloneDX:**
```groovy
plugins {
    id 'org.cyclonedx.bom' version '1.10.0'
}

cyclonedxBom {
    outputFormat = 'json'
    outputName = 'sbom'
    includeConfigs = ['runtimeClasspath']
}
```

Command: `gradle cyclonedxBom`

### npm / pnpm / yarn (JavaScript/TypeScript)

**CycloneDX:**
```bash
npx @cyclonedx/cdxgen -o sbom.json
```

### pip / Poetry (Python)

**CycloneDX:**
```bash
pip install cyclonedx-bom
cyclonedx-py environment -o sbom.json --output-format json
```

### Go

**CycloneDX:**
```bash
go install github.com/CycloneDX/cyclonedx-gomod/cmd/cyclonedx-gomod@latest
cyclonedx-gomod mod -json -output sbom.json
```

### Cargo (Rust)

**CycloneDX:**
```bash
cargo install cyclonedx-bom
cargo cyclonedx --format json
```

## CI Integration

### GitHub Actions

```yaml
- name: Generate SBOM
  run: |
    npx @cyclonedx/cdxgen -o sbom.json
  # OR for Maven:
  # mvn cyclonedx:makeAggregateBom

- name: Upload SBOM
  uses: actions/upload-artifact@v4
  with:
    name: sbom
    path: sbom.json

- name: Attest SBOM provenance
  uses: actions/attest-sbom@v2
  with:
    subject-path: 'target/*.jar'
    sbom-path: 'sbom.json'
```

### GitLab CI

```yaml
generate-sbom:
  stage: build
  script:
    - npx @cyclonedx/cdxgen -o sbom.json
  artifacts:
    paths:
      - sbom.json
    reports:
      cyclonedx: sbom.json
```

### Validation

After generation, validate the SBOM:

```bash
# Validate CycloneDX SBOM
npx @cyclonedx/cdxgen validate sbom.json

# Check for minimum required fields
# - components[].name
# - components[].version
# - components[].purl
# - components[].licenses[]
```

## SBOM Storage and Distribution

| Strategy | Description |
|----------|-------------|
| OCI registry | Attach SBOM as artifact alongside container image |
| Release assets | Include SBOM in GitHub/GitLab release artifacts |
| Dependency-Track | Upload to Dependency-Track for continuous monitoring |
| Artifact repository | Store in Nexus/Artifactory alongside build artifacts |

## SBOM Consumption

- **Vulnerability scanning:** Feed SBOM into Grype, Trivy, or Dependency-Track
- **License auditing:** Parse SBOM for license fields, compare against allow-list
- **Incident response:** Query SBOMs to find all affected deployments when a CVE is published
- **Regulatory compliance:** Provide SBOM to customers per Executive Order 14028 or EU CRA requirements
