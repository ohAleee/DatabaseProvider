dependencies {
    api(project(":api"))
    implementation(libs.clickhouse.jdbc)
    implementation(libs.hikaricp)
}
