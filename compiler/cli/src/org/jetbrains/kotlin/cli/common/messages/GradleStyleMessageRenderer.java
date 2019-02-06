/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.messages;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class GradleStyleMessageRenderer implements MessageRenderer {

    @Override
    public String renderPreamble() {
        return "";
    }

    @Override
    public String render(@NotNull CompilerMessageSeverity severity, @NotNull String message, @Nullable CompilerMessageLocation location) {
        StringBuilder result = new StringBuilder();

        if (severity == CompilerMessageSeverity.WARNING || severity == CompilerMessageSeverity.STRONG_WARNING) {
            result.append("w: ");
        } else if (severity == CompilerMessageSeverity.ERROR || severity == CompilerMessageSeverity.EXCEPTION) {
            result.append("e: ");
        } else if (severity == CompilerMessageSeverity.LOGGING || severity == CompilerMessageSeverity.OUTPUT) {
            result.append("v: ");
        } else { // INFO
            result.append("i: ");
        }

        if (location != null) {
            int line = location.getLine();
            int column = location.getColumn();

            result.append(location.getPath()).append(": ");

            if (line > 0 && column > 0) {
                result.append("(").append(line).append(", ");
                result.append(column).append("): ");
            }

            result.append(message);
        }

        return result.toString();
    }

    @Override
    public String renderUsage(@NotNull String usage) {
        return usage;
    }

    @Override
    public String renderConclusion() {
        return "";
    }

    @Override
    public String getName() {
        return "GradleStyle";
    }
}
