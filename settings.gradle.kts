rootProject.name = "movecraft-parent"
include(":movecraft-v1_20_6")
include(":movecraft-api")
include(":movecraft-datapack")
include(":movecraft")
project(":movecraft-v1_20_6").projectDir = file("v1_20_6")
project(":movecraft-api").projectDir = file("api")
project(":movecraft-datapack").projectDir = file("datapack")
project(":movecraft").projectDir = file("Movecraft")
