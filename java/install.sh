#!/usr/bin/env bash
# Install script for ia-dev-env CLI
# Supports macOS and Linux
#
# Usage:
#   bash install.sh              # Install to ~/.local/
#   bash install.sh --system     # Install to /usr/local/ (requires sudo)
#   bash install.sh --uninstall  # Remove installation
#   bash install.sh --help       # Show usage

set -euo pipefail

readonly INSTALLED_JAR_NAME="ia-dev-env.jar"
readonly REQUIRED_JAVA_VERSION=21
readonly PROGRAM_NAME="ia-dev-env"

# Resolved later from pom.xml
VERSION=""
JAR_NAME=""

# --- Colors ---

if [ -t 1 ]; then
    RED='\033[0;31m'
    GREEN='\033[0;32m'
    YELLOW='\033[1;33m'
    BLUE='\033[0;34m'
    BOLD='\033[1m'
    NC='\033[0m'
else
    RED=''
    GREEN=''
    YELLOW=''
    BLUE=''
    BOLD=''
    NC=''
fi

# --- Globals ---

INSTALL_MODE="user"
PREFIX=""
JAR_PATH=""
SKIP_BUILD=false
UNINSTALL=false
DEV_MODE=false

# --- Functions ---

usage() {
    cat <<EOF
${BOLD}ia-dev-env installer${NC}

${BOLD}USAGE:${NC}
    bash install.sh [OPTIONS]

${BOLD}OPTIONS:${NC}
    --system         Install to /usr/local/ (requires sudo)
    --prefix=DIR     Install to custom directory
    --jar=PATH       Use a pre-built JAR instead of building
    --skip-build     Skip Maven build (requires JAR in target/)
    --dev            Dev mode: regenerate golden files, manifests, run all tests, build, install
    --uninstall      Remove ia-dev-env installation
    --help           Show this help message

${BOLD}EXAMPLES:${NC}
    bash install.sh                    # Install to ~/.local/
    bash install.sh --dev              # Regen + test + build + install
    bash install.sh --system           # Install to /usr/local/
    bash install.sh --prefix=/opt      # Install to /opt/
    bash install.sh --jar=my.jar       # Use pre-built JAR
    bash install.sh --uninstall        # Remove installation
EOF
    exit 0
}

log_info() {
    printf "${BLUE}==>${NC} %s\n" "$1"
}

log_success() {
    printf "${GREEN}==>${NC} %s\n" "$1"
}

log_warn() {
    printf "${YELLOW}WARNING:${NC} %s\n" "$1"
}

log_error() {
    printf "${RED}ERROR:${NC} %s\n" "$1" >&2
}

die() {
    log_error "$1"
    exit 1
}

parse_args() {
    while [ $# -gt 0 ]; do
        case "$1" in
            --system)
                INSTALL_MODE="system"
                ;;
            --prefix=*)
                PREFIX="${1#--prefix=}"
                INSTALL_MODE="custom"
                ;;
            --jar=*)
                JAR_PATH="${1#--jar=}"
                ;;
            --skip-build)
                SKIP_BUILD=true
                ;;
            --dev)
                DEV_MODE=true
                ;;
            --uninstall)
                UNINSTALL=true
                ;;
            --help|-h)
                usage
                ;;
            *)
                die "Unknown option: $1 (use --help for usage)"
                ;;
        esac
        shift
    done
}

resolve_dirs() {
    case "$INSTALL_MODE" in
        user)
            APP_DIR="$HOME/.local/share/$PROGRAM_NAME"
            BIN_DIR="$HOME/.local/bin"
            SUDO_CMD=""
            ;;
        system)
            APP_DIR="/usr/local/share/$PROGRAM_NAME"
            BIN_DIR="/usr/local/bin"
            SUDO_CMD="sudo"
            ;;
        custom)
            APP_DIR="$PREFIX/share/$PROGRAM_NAME"
            BIN_DIR="$PREFIX/bin"
            if [ -w "$PREFIX" ] 2>/dev/null || [ -w "$(dirname "$PREFIX")" ] 2>/dev/null; then
                SUDO_CMD=""
            else
                SUDO_CMD="sudo"
            fi
            ;;
    esac
}

find_java() {
    if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
        echo "$JAVA_HOME/bin/java"
        return 0
    fi
    if command -v java >/dev/null 2>&1; then
        command -v java
        return 0
    fi
    return 1
}

get_java_major_version() {
    local java_cmd="$1"
    local version_output
    version_output=$("$java_cmd" -version 2>&1)
    local version
    version=$(echo "$version_output" \
        | head -1 \
        | sed -E 's/.*"([^"]+)".*/\1/' \
        | cut -d. -f1)
    if [ "$version" = "1" ]; then
        version=$(echo "$version_output" \
            | head -1 \
            | sed -E 's/.*"([^"]+)".*/\1/' \
            | cut -d. -f2)
    fi
    echo "$version"
}

check_java() {
    log_info "Checking Java installation..."
    local java_cmd
    if ! java_cmd=$(find_java); then
        cat >&2 <<'EOF'
ERROR: Java not found.

ia-dev-env requires Java 21 or later. Install it via:

  SDKMAN:   sdk install java 21-tem
  Homebrew: brew install openjdk@21
  Manual:   https://adoptium.net/temurin/releases/

Then either set JAVA_HOME or add java to your PATH.
EOF
        exit 1
    fi

    local major_version
    major_version=$(get_java_major_version "$java_cmd")
    if [ -z "$major_version" ] || [ "$major_version" -lt "$REQUIRED_JAVA_VERSION" ] 2>/dev/null; then
        die "Java $REQUIRED_JAVA_VERSION or later is required, but found Java ${major_version:-unknown}."
    fi

    log_success "Found Java $major_version ($java_cmd)"
}

check_maven() {
    if [ "$SKIP_BUILD" = true ] || [ -n "$JAR_PATH" ]; then
        return 0
    fi

    log_info "Checking Maven installation..."
    if ! command -v mvn >/dev/null 2>&1; then
        die "Maven not found. Install Maven 3.9+ or use --jar=PATH to provide a pre-built JAR."
    fi

    local mvn_version
    mvn_version=$(mvn --version 2>&1 | head -1)
    log_success "Found $mvn_version"
}

resolve_script_dir() {
    local source="${BASH_SOURCE[0]}"
    while [ -L "$source" ]; do
        local dir
        dir="$(cd -P "$(dirname "$source")" && pwd)"
        source="$(readlink "$source")"
        [[ "$source" != /* ]] && source="$dir/$source"
    done
    cd -P "$(dirname "$source")" && pwd
}

resolve_version() {
    local script_dir
    script_dir=$(resolve_script_dir)
    local pom_file="$script_dir/pom.xml"
    if [ ! -f "$pom_file" ]; then
        die "pom.xml not found in $script_dir. Cannot determine version."
    fi
    VERSION=$(sed -n '/<version>/{s/.*<version>\(.*\)<\/version>.*/\1/p;q;}' "$pom_file")
    if [ -z "$VERSION" ]; then
        die "Could not extract version from $pom_file"
    fi
    JAR_NAME="ia-dev-env-${VERSION}.jar"
    log_info "Detected version $VERSION from pom.xml"
}

dev_regenerate() {
    local script_dir
    script_dir=$(resolve_script_dir)

    if [ ! -f "$script_dir/pom.xml" ]; then
        die "pom.xml not found in $script_dir. --dev requires the source tree."
    fi

    log_info "Compiling project..."
    if ! (cd "$script_dir" && mvn compile test-compile -q); then
        die "Compilation failed."
    fi

    log_info "Regenerating expected-artifacts.json..."
    if ! (cd "$script_dir" && mvn exec:java \
        -Dexec.mainClass="dev.iadev.smoke.ExpectedArtifactsGenerator" \
        -Dexec.args="src/test/resources/smoke/expected-artifacts.json" \
        -q); then
        die "Manifest regeneration failed."
    fi

    log_info "Regenerating golden files..."
    if ! (cd "$script_dir" && mvn exec:java \
        -Dexec.mainClass="dev.iadev.golden.GoldenFileRegenerator" \
        -Dexec.classpathScope="test" \
        -q); then
        die "Golden file regeneration failed."
    fi

    log_info "Running all tests..."
    if ! (cd "$script_dir" && mvn verify -P all-tests -q); then
        die "Tests failed."
    fi

    log_success "Dev pipeline complete: regen + tests passed"
}

build_jar() {
    if [ -n "$JAR_PATH" ]; then
        if [ ! -f "$JAR_PATH" ]; then
            die "JAR not found at: $JAR_PATH"
        fi
        log_success "Using pre-built JAR: $JAR_PATH"
        return 0
    fi

    local script_dir
    script_dir=$(resolve_script_dir)
    local target_jar="$script_dir/target/$JAR_NAME"

    if [ "$SKIP_BUILD" = true ]; then
        if [ ! -f "$target_jar" ]; then
            die "JAR not found at $target_jar. Run 'mvn package' first or remove --skip-build."
        fi
        JAR_PATH="$target_jar"
        log_success "Using existing JAR: $JAR_PATH"
        return 0
    fi

    if [ ! -f "$script_dir/pom.xml" ]; then
        die "pom.xml not found in $script_dir. Run this script from the java/ directory or use --jar=PATH."
    fi

    log_info "Building fat JAR (this may take a minute)..."
    if ! (cd "$script_dir" && mvn clean package -DskipTests -q); then
        die "Maven build failed."
    fi

    if [ ! -f "$target_jar" ]; then
        die "Build succeeded but JAR not found at $target_jar"
    fi

    JAR_PATH="$target_jar"
    log_success "Built $JAR_PATH"
}

install_files() {
    log_info "Installing to $APP_DIR..."

    # Create directories
    $SUDO_CMD mkdir -p "$APP_DIR"
    $SUDO_CMD mkdir -p "$BIN_DIR"

    # Copy JAR
    $SUDO_CMD cp "$JAR_PATH" "$APP_DIR/$INSTALLED_JAR_NAME"

    # Write VERSION file
    echo "$VERSION" | $SUDO_CMD tee "$APP_DIR/VERSION" > /dev/null

    # Generate wrapper script
    local wrapper="$BIN_DIR/$PROGRAM_NAME"
    $SUDO_CMD tee "$wrapper" > /dev/null <<WRAPPER
#!/usr/bin/env bash
# ia-dev-env wrapper (installed)
# Generated by install.sh — do not edit manually.

set -euo pipefail

readonly REQUIRED_JAVA_VERSION=$REQUIRED_JAVA_VERSION
readonly JAR_PATH="$APP_DIR/$INSTALLED_JAR_NAME"

find_java() {
    if [ -n "\${JAVA_HOME:-}" ] && [ -x "\$JAVA_HOME/bin/java" ]; then
        echo "\$JAVA_HOME/bin/java"
        return 0
    fi
    if command -v java >/dev/null 2>&1; then
        command -v java
        return 0
    fi
    return 1
}

get_java_major_version() {
    local java_cmd="\$1"
    local version_output
    version_output=\$("\$java_cmd" -version 2>&1)
    local version
    version=\$(echo "\$version_output" \\
        | head -1 \\
        | sed -E 's/.*"([^"]+)".*/\1/' \\
        | cut -d. -f1)
    if [ "\$version" = "1" ]; then
        version=\$(echo "\$version_output" \\
            | head -1 \\
            | sed -E 's/.*"([^"]+)".*/\1/' \\
            | cut -d. -f2)
    fi
    echo "\$version"
}

main() {
    local java_cmd
    if ! java_cmd=\$(find_java); then
        echo "ERROR: Java not found. Install Java $REQUIRED_JAVA_VERSION+." >&2
        exit 1
    fi

    local major_version
    major_version=\$(get_java_major_version "\$java_cmd")
    if [ -z "\$major_version" ] || \\
       [ "\$major_version" -lt "\$REQUIRED_JAVA_VERSION" ] 2>/dev/null; then
        echo "ERROR: Java \$REQUIRED_JAVA_VERSION+ required, found Java \${major_version:-unknown}." >&2
        exit 1
    fi

    if [ ! -f "\$JAR_PATH" ]; then
        echo "ERROR: JAR not found at \$JAR_PATH" >&2
        echo "Reinstall with: bash install.sh" >&2
        exit 1
    fi

    # shellcheck disable=SC2086
    exec "\$java_cmd" \\
        \${IA_DEV_ENV_JAVA_OPTS:-} \\
        -jar "\$JAR_PATH" \\
        "\$@"
}

main "\$@"
WRAPPER
    $SUDO_CMD chmod +x "$wrapper"

    log_success "Installed $PROGRAM_NAME to $BIN_DIR/$PROGRAM_NAME"
}

ensure_path() {
    if [ "$INSTALL_MODE" != "user" ]; then
        return 0
    fi

    # Check if BIN_DIR is already in PATH
    if echo "$PATH" | tr ':' '\n' | grep -qx "$BIN_DIR"; then
        return 0
    fi

    local shell_rc=""
    local current_shell
    current_shell=$(basename "${SHELL:-bash}")

    case "$current_shell" in
        zsh)  shell_rc="$HOME/.zshrc" ;;
        bash)
            if [ -f "$HOME/.bashrc" ]; then
                shell_rc="$HOME/.bashrc"
            elif [ -f "$HOME/.bash_profile" ]; then
                shell_rc="$HOME/.bash_profile"
            else
                shell_rc="$HOME/.bashrc"
            fi
            ;;
        fish) shell_rc="$HOME/.config/fish/config.fish" ;;
        *)    shell_rc="$HOME/.profile" ;;
    esac

    local path_line="export PATH=\"$BIN_DIR:\$PATH\""
    if [ "$current_shell" = "fish" ]; then
        path_line="set -gx PATH $BIN_DIR \$PATH"
    fi

    # Check if already added
    if [ -f "$shell_rc" ] && grep -qF "$BIN_DIR" "$shell_rc"; then
        return 0
    fi

    log_info "Adding $BIN_DIR to PATH in $shell_rc..."
    echo "" >> "$shell_rc"
    echo "# Added by ia-dev-env installer" >> "$shell_rc"
    echo "$path_line" >> "$shell_rc"

    log_warn "Restart your shell or run: source $shell_rc"
}

do_uninstall() {
    resolve_dirs

    log_info "Uninstalling ia-dev-env..."

    local found=false

    if [ -f "$BIN_DIR/$PROGRAM_NAME" ]; then
        $SUDO_CMD rm -f "$BIN_DIR/$PROGRAM_NAME"
        log_success "Removed $BIN_DIR/$PROGRAM_NAME"
        found=true
    fi

    if [ -d "$APP_DIR" ]; then
        $SUDO_CMD rm -rf "$APP_DIR"
        log_success "Removed $APP_DIR"
        found=true
    fi

    if [ "$found" = false ]; then
        log_warn "No installation found in $APP_DIR or $BIN_DIR"
    else
        log_success "ia-dev-env uninstalled successfully."
        log_info "You may want to remove the PATH entry from your shell config."
    fi
}

print_success() {
    local jar_size
    jar_size=$(du -h "$APP_DIR/$INSTALLED_JAR_NAME" 2>/dev/null | cut -f1 | tr -d ' ')

    cat <<EOF

${GREEN}${BOLD}ia-dev-env installed successfully!${NC}

  ${BOLD}Version:${NC}  $VERSION
  ${BOLD}JAR:${NC}      $APP_DIR/$INSTALLED_JAR_NAME ($jar_size)
  ${BOLD}Wrapper:${NC}  $BIN_DIR/$PROGRAM_NAME

${BOLD}Quick start:${NC}
  $PROGRAM_NAME --version
  $PROGRAM_NAME generate --stack java-quarkus --output my-project/

${BOLD}Uninstall:${NC}
  bash install.sh --uninstall
EOF
}

# --- Main ---

main() {
    parse_args "$@"

    if [ "$UNINSTALL" = true ]; then
        resolve_dirs
        do_uninstall
        exit 0
    fi

    resolve_dirs
    check_java
    check_maven
    resolve_version

    if [ "$DEV_MODE" = true ]; then
        dev_regenerate
    fi

    build_jar
    install_files
    ensure_path
    print_success
}

main "$@"
