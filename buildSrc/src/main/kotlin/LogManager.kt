class LogManager(val prefix: String) {
    private enum class LogLevel(val ansiCode: String, val icon: String) {
        DEBUG("\u001B[48;5;234;38;5;250m", "🐛"),
        INFO("\u001B[48;5;23;38;5;87m", "ℹ️"),
        WARN("\u001B[1;48;5;94;38;5;226m", "⚠️"),
        ERROR("\u001B[1;48;5;52;38;5;196m", "❌"),
        SUCCESS("\u001B[1;48;5;22;38;5;46m", "✅"),

        RESET("\u001B[0m", " ")
    }

    private fun logging(msg: String, logLevel: LogLevel, fatal: Boolean = false) {
        val buildPrefix = "${logLevel.ansiCode} [$prefix | ${logLevel.name} ${logLevel.icon}]"

        if (!fatal) println(" $buildPrefix $msg ${LogLevel.RESET.ansiCode}")
        else kotlin.error(" $buildPrefix $msg ${LogLevel.RESET.ansiCode}")
    }

    fun debug(msg: String) = logging(msg, LogLevel.DEBUG)
    fun info(msg: String) = logging(msg, LogLevel.INFO)
    fun warn(msg: String) = logging(msg, LogLevel.WARN)
    fun error(msg: String, fatal: Boolean = true) = logging(msg, LogLevel.ERROR, fatal)
    fun success(msg: String) = logging(msg, LogLevel.SUCCESS)
}