dependencies {
    api(project(":api"))
    implementation(libs.mariadb.client)
    implementation(libs.hikaricp)
}
