import { describe, it, expect, vi, beforeEach } from "vitest";
import inquirer from "inquirer";
import {
  runInteractive,
  promptConfirmation,
  getFrameworkChoices,
  validateProjectName,
  validateProjectPurpose,
  validateVersion,
  ARCHITECTURE_CHOICES,
  LANGUAGE_CHOICES,
  INTERFACE_CHOICES,
  BUILD_TOOL_CHOICES,
  FRAMEWORK_CHOICES,
} from "../../src/interactive.js";
import { ProjectConfig } from "../../src/models.js";
import { CliError } from "../../src/exceptions.js";

vi.mock("inquirer", () => ({
  default: {
    prompt: vi.fn(),
  },
}));

const mockedPrompt = vi.mocked(inquirer.prompt);

interface DefaultInputs {
  readonly projectName: string;
  readonly projectPurpose: string;
  readonly architectureStyle: string;
  readonly language: string;
  readonly framework: string;
  readonly languageVersion: string;
  readonly frameworkVersion: string;
  readonly buildTool: string;
  readonly interfaceType: string;
  readonly domainDriven: boolean;
  readonly eventDriven: boolean;
}

const DEFAULT_INPUTS: DefaultInputs = {
  projectName: "my-tool",
  projectPurpose: "A test tool",
  architectureStyle: "library",
  language: "python",
  framework: "click",
  languageVersion: "3.9",
  frameworkVersion: "8.1",
  buildTool: "pip",
  interfaceType: "cli",
  domainDriven: false,
  eventDriven: false,
};

function setupPromptMock(overrides: Partial<DefaultInputs> = {}): void {
  const inputs = { ...DEFAULT_INPUTS, ...overrides };

  mockedPrompt
    .mockResolvedValueOnce({
      projectName: inputs.projectName,
      projectPurpose: inputs.projectPurpose,
    })
    .mockResolvedValueOnce({
      architectureStyle: inputs.architectureStyle,
      language: inputs.language,
    })
    .mockResolvedValueOnce({
      framework: inputs.framework,
      languageVersion: inputs.languageVersion,
      frameworkVersion: inputs.frameworkVersion,
      buildTool: inputs.buildTool,
    })
    .mockResolvedValueOnce({
      interfaceType: inputs.interfaceType,
      domainDriven: inputs.domainDriven,
      eventDriven: inputs.eventDriven,
    });
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe("runInteractive", () => {
  it("runInteractive_completeInput_returnsProjectConfig", async () => {
    setupPromptMock();

    const config = await runInteractive();

    expect(config).toBeInstanceOf(ProjectConfig);
  });

  it("runInteractive_completeInput_projectNamePreserved", async () => {
    setupPromptMock({ projectName: "my-custom-tool" });

    const config = await runInteractive();

    expect(config.project.name).toBe("my-custom-tool");
  });

  it("runInteractive_completeInput_projectPurposePreserved", async () => {
    setupPromptMock({ projectPurpose: "Custom purpose" });

    const config = await runInteractive();

    expect(config.project.purpose).toBe("Custom purpose");
  });

  it.each([
    ["library"],
    ["microservice"],
    ["monolith"],
  ] as const)(
    "runInteractive_architecture%s_setsStyleCorrectly",
    async (style) => {
      setupPromptMock({ architectureStyle: style });

      const config = await runInteractive();

      expect(config.architecture.style).toBe(style);
    },
  );

  it.each([
    [false],
    [true],
  ])(
    "runInteractive_domainDriven%s_setsFlag",
    async (flag) => {
      setupPromptMock({ domainDriven: flag });

      const config = await runInteractive();

      expect(config.architecture.domainDriven).toBe(flag);
    },
  );

  it.each([
    [false],
    [true],
  ])(
    "runInteractive_eventDriven%s_setsFlag",
    async (flag) => {
      setupPromptMock({ eventDriven: flag });

      const config = await runInteractive();

      expect(config.architecture.eventDriven).toBe(flag);
    },
  );

  it.each([
    ["cli"],
    ["rest"],
    ["grpc"],
    ["event-consumer"],
    ["event-producer"],
  ])(
    "runInteractive_interface%s_selectedCorrectly",
    async (iface) => {
      setupPromptMock({ interfaceType: iface });

      const config = await runInteractive();

      expect(config.interfaces[0].type).toBe(iface);
    },
  );

  it.each([
    { language: "python", version: "3.9", framework: "click", fwVersion: "8.1", build: "pip" },
    { language: "java", version: "21", framework: "quarkus", fwVersion: "3.17", build: "maven" },
    { language: "typescript", version: "5.7", framework: "nestjs", fwVersion: "10.4", build: "npm" },
    { language: "rust", version: "1.83", framework: "axum", fwVersion: "0.8", build: "cargo" },
    { language: "go", version: "1.23", framework: "gin", fwVersion: "1.10", build: "go" },
    { language: "kotlin", version: "2.1", framework: "ktor", fwVersion: "3.0", build: "gradle" },
  ])(
    "runInteractive_$language$framework_languageAndFrameworkCorrect",
    async ({ language, version, framework, fwVersion, build }) => {
      setupPromptMock({
        language,
        languageVersion: version,
        framework,
        frameworkVersion: fwVersion,
        buildTool: build,
      });

      const config = await runInteractive();

      expect(config.language.name).toBe(language);
      expect(config.language.version).toBe(version);
      expect(config.framework.name).toBe(framework);
      expect(config.framework.version).toBe(fwVersion);
      expect(config.framework.buildTool).toBe(build);
    },
  );

  it("runInteractive_completeInput_defaultSectionsApplied", async () => {
    setupPromptMock();

    const config = await runInteractive();

    expect(config.data.database.name).toBe("none");
    expect(config.infrastructure.container).toBe("docker");
    expect(config.security.frameworks).toEqual([]);
    expect(config.testing.smokeTests).toBe(true);
  });

  it("runInteractive_promptCalledFourTimes_correctOrder", async () => {
    setupPromptMock();

    await runInteractive();

    expect(mockedPrompt).toHaveBeenCalledTimes(4);
  });

  it.each([
    { lang: "python", expected: ["fastapi", "click", "django", "flask"] },
    { lang: "java", expected: ["quarkus", "spring-boot"] },
    { lang: "kotlin", expected: ["ktor"] },
    { lang: "go", expected: ["gin"] },
    { lang: "typescript", expected: ["nestjs"] },
    { lang: "rust", expected: ["axum"] },
  ])(
    "runInteractive_$langLanguage_frameworkFilteredCorrectly",
    async ({ lang, expected }) => {
      setupPromptMock({ language: lang });

      await runInteractive();

      const thirdCall = mockedPrompt.mock.calls[2];
      const questions = thirdCall[0] as Array<{ choices?: string[] }>;
      const frameworkQuestion = questions.find(
        (q: Record<string, unknown>) => q.name === "framework",
      ) as { choices: string[] };
      expect(frameworkQuestion.choices).toEqual(expected);
    },
  );

  it("runInteractive_promptRejects_errorPropagates", async () => {
    mockedPrompt.mockRejectedValueOnce(new Error("stdin closed"));

    await expect(runInteractive()).rejects.toThrow("stdin closed");
  });

  it("runInteractive_firstPromptPasses_secondRejects_errorPropagates", async () => {
    mockedPrompt
      .mockResolvedValueOnce({ projectName: "x", projectPurpose: "y" })
      .mockRejectedValueOnce(new Error("prompt interrupted"));

    await expect(runInteractive()).rejects.toThrow("prompt interrupted");
  });
});

describe("getFrameworkChoices", () => {
  it.each(
    Object.entries(FRAMEWORK_CHOICES),
  )(
    "getFrameworkChoices_%s_returnsExpectedList",
    (language, expected) => {
      expect(getFrameworkChoices(language)).toEqual(expected);
    },
  );

  it("getFrameworkChoices_unknownLanguage_fallsBackToOther", () => {
    expect(getFrameworkChoices("csharp")).toEqual(["other"]);
  });

  it("getFrameworkChoices_emptyString_fallsBackToOther", () => {
    expect(getFrameworkChoices("")).toEqual(["other"]);
  });
});

describe("choices constants", () => {
  it("architectureChoices_exactValues_matchSpec", () => {
    expect([...ARCHITECTURE_CHOICES]).toEqual([
      "library",
      "microservice",
      "monolith",
    ]);
  });

  it("languageChoices_exactValues_matchSpec", () => {
    expect([...LANGUAGE_CHOICES]).toEqual([
      "python",
      "java",
      "go",
      "kotlin",
      "typescript",
      "rust",
    ]);
  });

  it("interfaceChoices_exactValues_matchSpec", () => {
    expect([...INTERFACE_CHOICES]).toEqual([
      "rest",
      "grpc",
      "cli",
      "event-consumer",
      "event-producer",
    ]);
  });

  it("buildToolChoices_exactValues_matchSpec", () => {
    expect([...BUILD_TOOL_CHOICES]).toEqual([
      "pip",
      "maven",
      "gradle",
      "go",
      "cargo",
      "npm",
    ]);
  });

  it.each(
    Object.entries(FRAMEWORK_CHOICES),
  )(
    "frameworkChoices_%sKey_containsExpectedFrameworks",
    (language, expected) => {
      expect(FRAMEWORK_CHOICES[language]).toEqual(expected);
    },
  );
});

describe("validateProjectName", () => {
  it("validateProjectName_validName_returnsTrue", () => {
    expect(validateProjectName("my-project")).toBe(true);
  });

  it("validateProjectName_emptyString_returnsErrorMessage", () => {
    expect(validateProjectName("")).toBe("Project name cannot be empty.");
  });

  it("validateProjectName_whitespaceOnly_returnsErrorMessage", () => {
    expect(validateProjectName("   ")).toBe("Project name cannot be empty.");
  });

  it("validateProjectName_tooLong_returnsErrorMessage", () => {
    const longName = "a".repeat(101);
    expect(validateProjectName(longName)).toContain("at most 100");
  });

  it("validateProjectName_exactMaxLength_returnsTrue", () => {
    expect(validateProjectName("a".repeat(100))).toBe(true);
  });
});

describe("validateProjectPurpose", () => {
  it("validateProjectPurpose_validPurpose_returnsTrue", () => {
    expect(validateProjectPurpose("A useful tool")).toBe(true);
  });

  it("validateProjectPurpose_emptyString_returnsErrorMessage", () => {
    expect(validateProjectPurpose("")).toBe("Project purpose cannot be empty.");
  });

  it("validateProjectPurpose_tooLong_returnsErrorMessage", () => {
    const longPurpose = "a".repeat(501);
    expect(validateProjectPurpose(longPurpose)).toContain("at most 500");
  });

  it("validateProjectPurpose_exactMaxLength_returnsTrue", () => {
    expect(validateProjectPurpose("a".repeat(500))).toBe(true);
  });
});

describe("validateVersion", () => {
  it("validateVersion_validVersion_returnsTrue", () => {
    expect(validateVersion("3.9")).toBe(true);
  });

  it("validateVersion_emptyString_returnsErrorMessage", () => {
    expect(validateVersion("")).toBe("Version cannot be empty.");
  });

  it("validateVersion_tooLong_returnsErrorMessage", () => {
    const longVersion = "1".repeat(21);
    expect(validateVersion(longVersion)).toContain("at most 20");
  });

  it("validateVersion_exactMaxLength_returnsTrue", () => {
    expect(validateVersion("1".repeat(20))).toBe(true);
  });
});

describe("promptConfirmation", () => {
  it("promptConfirmation_negativeTimeout_throwsCliError", async () => {
    await expect(
      promptConfirmation("test?", true, -1),
    ).rejects.toThrow(CliError);
  });

  it("promptConfirmation_zeroTimeout_throwsCliError", async () => {
    await expect(
      promptConfirmation("test?", true, 0),
    ).rejects.toThrow(CliError);
  });

  it("promptConfirmation_userConfirms_returnsTrue", async () => {
    mockedPrompt.mockResolvedValueOnce({ confirmed: true });

    const result = await promptConfirmation("Proceed?", false, 5000);

    expect(result).toBe(true);
  });

  it("promptConfirmation_userDeclines_returnsFalse", async () => {
    mockedPrompt.mockResolvedValueOnce({ confirmed: false });

    const result = await promptConfirmation("Proceed?", true, 5000);

    expect(result).toBe(false);
  });

  it("promptConfirmation_defaultTrue_passedToPrompt", async () => {
    mockedPrompt.mockResolvedValueOnce({ confirmed: true });

    await promptConfirmation("Proceed?");

    const questions = mockedPrompt.mock.calls[0][0] as Array<{
      default?: boolean;
    }>;
    expect(questions[0].default).toBe(true);
  });

  it("promptConfirmation_promptTimesOut_throwsCliError", async () => {
    mockedPrompt.mockImplementationOnce(
      () => new Promise(() => {/* never resolves */}),
    );

    await expect(
      promptConfirmation("test?", true, 50),
    ).rejects.toThrow("Prompt timed out");
  });
});
