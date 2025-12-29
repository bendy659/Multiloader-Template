package ru.benos.gradle

import java.io.File

val File.exists: Boolean get() = this@exists.exists()
val File.readText: String get() = this@readText.readText()