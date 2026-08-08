package com.esimmcp.config;

import java.util.List;
import java.util.Map;

/**
 * STDIO launch specification for one MCP server process.
 */
public final class McpServerSpec {

    private final String name;
    private final String command;
    private final List<String> args;
    private final Map<String, String> env;

    public McpServerSpec(String name, String command, List<String> args, Map<String, String> env) {
        this.name = name;
        this.command = command == null ? "" : command;
        this.args = List.copyOf(args);
        this.env = Map.copyOf(env);
    }

    public String name() {
        return name;
    }

    public String command() {
        return command;
    }

    public List<String> args() {
        return args;
    }

    public Map<String, String> env() {
        return env;
    }

    public boolean enabled() {
        return !command.isBlank();
    }
}
