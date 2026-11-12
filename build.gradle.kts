// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
}

// res(git submodule) 를 clone / pull. 빌드(preBuild) 전에 항상 실행된다.
tasks.register<Exec>("syncSubmodules") {
    group = "build setup"
    description = "Clone or pull git submodules (res)."
    workingDir = rootDir
    commandLine("bash", "scripts/sync-submodules.sh")
}

subprojects {
    tasks.matching { it.name == "preBuild" }.configureEach {
        dependsOn(rootProject.tasks.named("syncSubmodules"))
    }
}
