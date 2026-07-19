package com.wechat.scanner

import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters

@Command(
    name = "wechat-class-scanner",
    version = ["1.0.0"],
    description = ["Scans WeChat APK(s) for class names and method signatures"]
)
class Main : Runnable {

    @Option(names = ["-o", "--output"], description = "Output file path (default: stdout)", defaultValue = "")
    var outputPath: String = ""

    @Option(names = ["-k", "--keywords"], description = "Comma-separated keywords to filter classes", defaultValue = "msg,conversation,conv,contact,storage,store,info,message")
    var keywords: String = ""

    @Option(names = ["-p", "--package-prefix"], description = "Package prefix to filter (e.g. com.tencent.mm)", defaultValue = "com.tencent")
    var packagePrefix: String = ""

    @Option(names = ["-m", "--include-methods"], description = "Include method signatures in output", defaultValue = "true")
    var includeMethods: Boolean = true

    @Parameters(index = "0..*", description = "APK file paths (base.apk + split_config.*.apk)")
    var apkPaths: List<String> = emptyList()

    override fun run() {
        if (apkPaths.isEmpty()) {
            System.err.println("Error: At least one APK path required")
            System.exit(1)
        }

        val keywordSet = keywords.split(",").map { it.trim().lowercase() }.toSet()
        val results = mutableMapOf<String, MutableList<String>>()

        for (apkPath in apkPaths) {
            scanApk(apkPath, packagePrefix, keywordSet, includeMethods, results)
        }

        val output = generateOutput(results, includeMethods)

        if (outputPath.isNotBlank()) {
            java.io.File(outputPath).writeText(output)
            println("Written to $outputPath")
        } else {
            println(output)
        }
    }

    private fun scanApk(
        apkPath: String,
        packagePrefix: String,
        keywords: Set<String>,
        includeMethods: Boolean,
        results: MutableMap<String, MutableList<String>>
    ) {
        println("Scanning $apkPath...")
        try {
            val dexFile = org.jf.dexlib2.dexbacked.DexBackedDexFileFactory.fromFile(java.io.File(apkPath))
            
            val classDefs = dexFile.classes
            var count = 0
            
            for (classDef in classDefs) {
                val className = classDef.name.replace('/', '.')
                
                if (!className.startsWith(packagePrefix)) continue
                
                val lowerName = className.lowercase()
                val matches = keywords.any { lowerName.contains(it) }
                if (!matches) continue

                val methods = if (includeMethods) {
                    classDef.methods.map { method ->
                        val params = method.parameterTypes.joinToString(", ") { it.replace('/', '.') }
                        val returnType = method.returnType.replace('/', '.')
                        "  ${method.name}($params)$returnType"
                    }
                } else emptyList()

                if (methods.isNotEmpty() || !includeMethods) {
                    results[className] = methods.toMutableList()
                    count++
                }
            }
            
            dexFile.close()
            println("  Found $count matching classes")
        } catch (e: Exception) {
            System.err.println("Error scanning $apkPath: ${e.message}")
        }
    }

    private fun generateOutput(results: Map<String, MutableList<String>>, includeMethods: Boolean): String {
        val sb = StringBuilder()
        val sortedClasses = results.keys.sorted()
        
        for (className in sortedClasses) {
            sb.appendLine(className)
            if (includeMethods) {
                for (method in results[className]!!) {
                    sb.appendLine(method)
                }
            }
        }
        
        return sb.toString()
    }
}

fun main(args: Array<String>) {
    val exitCode = CommandLine(Main()).execute(*args)
    System.exit(exitCode)
}