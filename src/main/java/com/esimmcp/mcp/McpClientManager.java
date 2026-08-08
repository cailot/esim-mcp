package com.esimmcp.mcp;

import com.esimmcp.config.AppConfig;
import com.esimmcp.config.McpServerSpec;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manages STDIO MCP clients for Brave, Playwright, Sequential Thinking, Supabase, and Gmail.
 */
public final class McpClientManager implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(McpClientManager.class);

    private final Map<String, McpSyncClient> clients = new LinkedHashMap<>();

    private McpClientManager() {
    }

    public static McpClientManager create(AppConfig config) {
        McpClientManager manager = new McpClientManager();
        manager.connect(config.braveServer());
        manager.connect(config.playwrightServer());
        manager.connect(config.sequentialServer());
        manager.connect(config.supabaseServer());
        manager.connect(config.gmailServer());
        return manager;
    }

    private void connect(McpServerSpec spec) {
        if (!spec.enabled()) {
            log.warn("MCP server '{}' is disabled (empty command)", spec.name());
            return;
        }

        try {
            ServerParameters.Builder params = ServerParameters.builder(spec.command())
                    .args(spec.args());
            for (Map.Entry<String, String> entry : spec.env().entrySet()) {
                params.addEnvVar(entry.getKey(), entry.getValue());
            }

            StdioClientTransport transport =
                    new StdioClientTransport(params.build(), McpJsonDefaults.getMapper());

            McpSyncClient client = McpClient.sync(transport)
                    .requestTimeout(Duration.ofSeconds(60))
                    .clientInfo(new McpSchema.Implementation("esim-mcp", "0.1.0"))
                    .build();

            client.initialize();
            clients.put(spec.name(), client);
            log.info("Connected MCP server '{}'", spec.name());
        } catch (Exception e) {
            log.error("Failed to connect MCP server '{}': {}", spec.name(), e.getMessage());
        }
    }

    public Optional<McpSyncClient> client(String name) {
        return Optional.ofNullable(clients.get(name));
    }

    public McpSchema.CallToolResult callTool(String serverName, String toolName, Map<String, Object> arguments) {
        McpSyncClient client = clients.get(serverName);
        if (client == null) {
            throw new IllegalStateException("MCP server not connected: " + serverName);
        }
        log.info("Calling MCP tool {}.{} with args keys={}", serverName, toolName, arguments.keySet());
        return client.callTool(
                McpSchema.CallToolRequest.builder(toolName)
                        .arguments(arguments)
                        .build()
        );
    }

    public String extractText(McpSchema.CallToolResult result) {
        if (result == null || result.content() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (McpSchema.Content content : result.content()) {
            if (content instanceof McpSchema.TextContent text) {
                if (!sb.isEmpty()) {
                    sb.append('\n');
                }
                sb.append(text.text());
            }
        }
        return sb.toString();
    }

    @Override
    public void close() {
        for (Map.Entry<String, McpSyncClient> entry : clients.entrySet()) {
            try {
                entry.getValue().closeGracefully();
                log.info("Closed MCP server '{}'", entry.getKey());
            } catch (Exception e) {
                log.warn("Error closing MCP server '{}': {}", entry.getKey(), e.getMessage());
            }
        }
        clients.clear();
    }
}
