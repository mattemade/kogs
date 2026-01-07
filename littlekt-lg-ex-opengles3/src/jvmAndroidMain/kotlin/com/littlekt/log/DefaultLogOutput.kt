package com.littlekt.log

/**
 * @author Colton Daily
 * @date 11/25/2021
 */
actual object DefaultLogOutput : com.littlekt.log.Logger.Output {
    actual override fun output(logger: _root_ide_package_.com.littlekt.log.Logger, level: _root_ide_package_.com.littlekt.log.Logger.Level, msg: Any?) =
        _root_ide_package_.com.littlekt.log.Logger.ConsoleLogOutput.output(logger, level, msg)
}