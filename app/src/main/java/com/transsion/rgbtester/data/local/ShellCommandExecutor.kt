package com.transsion.rgbtester.data.local

import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

/**
 * Shell Command Executor for RGB LED Control
 *
 * Supports both root and non-root execution methods.
 * For sysfs access, root is typically required on production devices.
 */
class ShellCommandExecutor {

    companion object {
        private const val TAG = "ShellExecutor"
        private const val SU_BINARY = "su"
        private const val SH_BINARY = "sh"
    }

    /**
     * Execute command with root privileges
     */
    fun executeRoot(vararg commands: String): ShellResult {
        return executeCommands(true, *commands)
    }

    /**
     * Execute command without root (regular shell)
     */
    fun executeShell(vararg commands: String): ShellResult {
        return executeCommands(false, *commands)
    }

    /**
     * Execute a single command and return output
     */
    fun executeSingle(command: String, useRoot: Boolean = true): ShellResult {
        return executeCommands(useRoot, command)
    }

    private fun executeCommands(useRoot: Boolean, vararg commands: String): ShellResult {
        val process: Process
        val outputLines = mutableListOf<String>()
        val errorLines = mutableListOf<String>()
        var exitCode = -1

        try {
            // Start process
            val binary = if (useRoot) SU_BINARY else SH_BINARY
            process = Runtime.getRuntime().exec(binary)

            // Write commands to shell
            DataOutputStream(process.outputStream).use { outputStream ->
                for (command in commands) {
                    outputStream.writeBytes("$command\n")
                    outputStream.flush()
                }
                outputStream.writeBytes("exit\n")
                outputStream.flush()
            }

            // Read output
            Thread {
                try {
                    BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            outputLines.add(line!!)
                            Log.d(TAG, "OUT: $line")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading stdout", e)
                }
            }.start()

            // Read error stream
            Thread {
                try {
                    BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            errorLines.add(line!!)
                            Log.e(TAG, "ERR: $line")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading stderr", e)
                }
            }.start()

            // Wait for process to complete
            exitCode = process.waitFor()

            // Small delay to ensure streams are read
            Thread.sleep(100)

        } catch (e: Exception) {
            Log.e(TAG, "Error executing commands", e)
            errorLines.add(e.message ?: "Unknown error")
        }

        return ShellResult(
            success = exitCode == 0 && errorLines.isEmpty(),
            exitCode = exitCode,
            output = outputLines,
            error = errorLines
        )
    }

    /**
     * Check if root access is available
     */
    fun checkRootAccess(): Boolean {
        val result = executeRoot("id")
        return result.success && result.output.any {
            it.contains("uid=0") || it.contains("root")
        }
    }

    /**
     * Check if a file/path exists
     */
    fun fileExists(path: String): Boolean {
        val result = executeRoot("test -e $path && echo EXISTS || echo NOT_FOUND")
        return result.output.any { it.contains("EXISTS") }
    }

    /**
     * Read file content
     */
    fun readFile(path: String): String? {
        val result = executeRoot("cat $path 2>/dev/null")
        return if (result.success && result.output.isNotEmpty()) {
            result.output.joinToString("\n")
        } else null
    }

    /**
     * Write to file (sysfs)
     */
    fun writeFile(path: String, value: String): Boolean {
        val result = executeRoot("echo '$value' > $path")
        return result.success
    }

    /**
     * List directory contents
     */
    fun listDirectory(path: String): List<String> {
        val result = executeRoot("ls -1 $path 2>/dev/null")
        return result.output.filter { it.isNotBlank() }
    }

    /**
     * Set file permissions
     */
    fun setPermissions(path: String, permissions: String): Boolean {
        val result = executeRoot("chmod $permissions $path")
        return result.success
    }

    /**
     * Get available triggers for a LED
     */
    fun getAvailableTriggers(ledPath: String): List<String> {
        val triggerPath = "$ledPath/trigger"
        val content = readFile(triggerPath) ?: return emptyList()

        // Parse trigger list format: [none] timer heartbeat ...
        // Current trigger is enclosed in square brackets
        return content
            .replace("[", "")
            .replace("]", "")
            .split(" ")
            .filter { it.isNotBlank() }
    }

    /**
     * Get current trigger for a LED
     */
    fun getCurrentTrigger(ledPath: String): String? {
        val triggerPath = "$ledPath/trigger"
        val content = readFile(triggerPath) ?: return null

        // Find text between [ ]
        val regex = Regex("\\[([^\\]]+)\\]")
        return regex.find(content)?.groupValues?.get(1)
    }
}

/**
 * Result of shell command execution
 */
data class ShellResult(
    val success: Boolean,
    val exitCode: Int,
    val output: List<String>,
    val error: List<String>
) {
    val outputText: String
        get() = output.joinToString("\n")

    val errorText: String
        get() = error.joinToString("\n")
}
