from __future__ import annotations

import pytest

from ia_dev_env.domain.stack_mapping import (
    get_cache_settings_key,
    get_database_settings_key,
    get_hook_template_key,
    get_settings_lang_key,
)


class TestGetHookTemplateKey:
    def test_getHookTemplateKey_javaMaven_returnsJavaMaven(self):
        assert get_hook_template_key("java", "maven") == "java-maven"

    def test_getHookTemplateKey_javaGradle_returnsJavaGradle(self):
        assert get_hook_template_key("java", "gradle") == "java-gradle"

    def test_getHookTemplateKey_kotlinGradle_returnsKotlin(self):
        assert get_hook_template_key("kotlin", "gradle") == "kotlin"

    def test_getHookTemplateKey_typescriptNpm_returnsTypescript(self):
        assert get_hook_template_key("typescript", "npm") == "typescript"

    def test_getHookTemplateKey_pythonPip_returnsEmpty(self):
        assert get_hook_template_key("python", "pip") == ""

    def test_getHookTemplateKey_goGo_returnsGo(self):
        assert get_hook_template_key("go", "go") == "go"

    def test_getHookTemplateKey_rustCargo_returnsRust(self):
        assert get_hook_template_key("rust", "cargo") == "rust"

    def test_getHookTemplateKey_csharpDotnet_returnsCsharp(self):
        assert get_hook_template_key("csharp", "dotnet") == "csharp"

    def test_getHookTemplateKey_unknownLang_returnsEmpty(self):
        assert get_hook_template_key("unknown", "unknown") == ""


class TestGetSettingsLangKey:
    @pytest.mark.parametrize(
        "lang,build_tool,expected",
        [
            ("java", "maven", "java-maven"),
            ("java", "gradle", "java-gradle"),
            ("kotlin", "gradle", "java-gradle"),
            ("typescript", "npm", "typescript-npm"),
            ("python", "pip", "python-pip"),
            ("go", "go", "go"),
            ("rust", "cargo", "rust-cargo"),
            ("csharp", "dotnet", "csharp-dotnet"),
        ],
    )
    def test_getSettingsLangKey_allEntries_returnsExpected(self, lang, build_tool, expected):
        assert get_settings_lang_key(lang, build_tool) == expected

    def test_getSettingsLangKey_unknownLang_returnsEmpty(self):
        assert get_settings_lang_key("unknown", "x") == ""


class TestGetDatabaseSettingsKey:
    @pytest.mark.parametrize(
        "db_name,expected",
        [
            ("postgresql", "database-psql"),
            ("mysql", "database-mysql"),
            ("oracle", "database-oracle"),
            ("mongodb", "database-mongodb"),
            ("cassandra", "database-cassandra"),
        ],
    )
    def test_getDatabaseSettingsKey_allDatabases_returnsExpected(self, db_name, expected):
        assert get_database_settings_key(db_name) == expected

    def test_getDatabaseSettingsKey_unknownDb_returnsEmpty(self):
        assert get_database_settings_key("sqlite") == ""

    def test_getDatabaseSettingsKey_noneDb_returnsEmpty(self):
        assert get_database_settings_key("none") == ""


class TestGetCacheSettingsKey:
    @pytest.mark.parametrize(
        "cache_name,expected",
        [
            ("redis", "cache-redis"),
            ("dragonfly", "cache-dragonfly"),
            ("memcached", "cache-memcached"),
        ],
    )
    def test_getCacheSettingsKey_allCaches_returnsExpected(self, cache_name, expected):
        assert get_cache_settings_key(cache_name) == expected

    def test_getCacheSettingsKey_unknownCache_returnsEmpty(self):
        assert get_cache_settings_key("hazelcast") == ""

    def test_getCacheSettingsKey_noneCache_returnsEmpty(self):
        assert get_cache_settings_key("none") == ""
