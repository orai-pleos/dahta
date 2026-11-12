pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "DAHTA"

// res 는 별도 레포(DAHTA-res)의 git submodule.
// 클론 직후처럼 비어 있으면 빌드 시 자동으로 `git submodule update --init` 실행.
val resModuleDir = settingsDir.resolve("res")
if (!resModuleDir.resolve("build.gradle.kts").exists() && settingsDir.resolve(".gitmodules")
        .exists()
) {
    println("res submodule is missing -> running: git submodule update --init")
    val exitCode = ProcessBuilder("git", "submodule", "update", "--init", "--recursive")
        .directory(settingsDir)
        .inheritIO()
        .start()
        .waitFor()
    check(exitCode == 0) {
        "`git submodule update --init` failed (exit $exitCode). Run it manually in $settingsDir"
    }
}

include(":app")
include(":res")
