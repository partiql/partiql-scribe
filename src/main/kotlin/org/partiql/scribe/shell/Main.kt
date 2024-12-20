/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */
@file:JvmName("Main")

package org.partiql.scribe.shell

import picocli.CommandLine
import kotlin.system.exitProcess

/**
 * Run a PartiQL Transpiler REPL.
 */
public fun main(args: Array<String>) {
    val command = CommandLine(REPL())
    val exitCode = command.execute(*args)
    exitProcess(exitCode)
}

@CommandLine.Command(
    name = "scribe",
    mixinStandardHelpOptions = true,
    descriptionHeading = "%n@|bold,underline,yellow The PartiQL Transpiler Debug REPL|@%n",
    description = ["This REPL is used for debugging the transpiler"],
    showDefaultValues = true
)
internal class REPL : Runnable {

    override fun run() {
        Shell().start()
    }
}
