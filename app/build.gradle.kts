plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.water_app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.water_app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }



    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }


}

dependencies {
    implementation("com.itextpdf:itext7-core:7.2.5")
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3")
    implementation ("com.google.firebase:firebase-storage:20.3.0")
    implementation ("com.github.dhaval2404:colorpicker:2.3")
    implementation(libs.auto.value.annotations)
    annotationProcessor(libs.auto.value)

    implementation(libs.github.glide)
    annotationProcessor(libs.glide.compiler)

    implementation(libs.github.glide)
    implementation(libs.material.v190)
    implementation(libs.firebase.analytics)
    implementation(platform(libs.firebase.bom))
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler) // Use annotationProcessor instead of KSP
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}