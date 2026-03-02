from __future__ import annotations

import pytest

from claude_setup.domain.stack_mapping import (
    get_cache_settings_key,
    get_database_settings_key,
    get_hook_template_key,
    get_settings_lang_key,
)


class TestGetHookTemplateKey:
    def test_java_maven(self):
        assert get_hook_template_key("java", "maven") == "java-maven"

    def test_java_gradle(self):
        assert get_hook_template_key("java", "gradle") == "java-gradle"

    def test_kotlin(self):
        assert get_hook_template_key("kotlin", "gradle") == "kotlin"

    def test_typescript(self):
        assert get_hook_template_key("typescript", "npm") == "typescript"

    def test_python_returns_empty(self):
        assert get_hook_template_key("python", "pip") == ""

    def test_go(self):
        assert get_hook_template_key("go", "go") == "go"

    def test_rust(self):
        assert get_hook_template_key("rust", "cargo") == "rust"

    def test_csharp(self):
        assert get_hook_template_key("csharp", "dotnet") == "csharp"

    def test_unknown_returns_empty(self):
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
    def test_all_entries(self, lang, build_tool, expected):
        assert get_settings_lang_key(lang, build_tool) == expected

    def test_unknown_returns_empty(self):
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
    def test_all_databases(self, db_name, expected):
        assert get_database_settings_key(db_name) == expected

    def test_unknown_returns_empty(self):
        assert get_database_settings_key("sqlite") == ""

    def test_none_returns_empty(self):
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
    def test_all_caches(self, cache_name, expected):
        assert get_cache_settings_key(cache_name) == expected

    def test_unknown_returns_empty(self):
        assert get_cache_settings_key("hazelcast") == ""

    def test_none_returns_empty(self):
        assert get_cache_settings_key("none") == ""
