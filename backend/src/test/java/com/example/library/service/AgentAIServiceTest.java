package com.example.library.service;

import com.example.library.dto.response.AgentRunResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentAIServiceTest {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Test
    void loopsThroughToolResultBeforeReturningFinalAnswer() throws Exception {
        OkHttpClient client = mock(OkHttpClient.class);
        Call firstCall = mock(Call.class);
        Call secondCall = mock(Call.class);
        AgentToolService tools = mock(AgentToolService.class);
        ChatHistoryService history = mock(ChatHistoryService.class);
        OperationLogService logs = mock(OperationLogService.class);

        when(client.newCall(any(Request.class))).thenReturn(firstCall, secondCall);
        when(firstCall.execute()).thenReturn(response("""
                {"choices":[{"finish_reason":"tool_calls","message":{"role":"assistant","content":null,
                "tool_calls":[{"id":"call_1","type":"function","function":{"name":"get_library_statistics","arguments":"{}"}}]}}]}
                """));
        when(secondCall.execute()).thenReturn(response("""
                {"choices":[{"finish_reason":"stop","message":{"role":"assistant","content":"本馆共有100种图书。"}}]}
                """));
        when(tools.definitions()).thenReturn(List.of(Map.of("type", "function")));
        when(tools.displayName("get_library_statistics")).thenReturn("馆藏统计");
        when(tools.execute("get_library_statistics", "{}", "student001"))
                .thenReturn("{\"ok\":true,\"data\":{\"titleCount\":100}}");
        when(history.getRecentSessionHistory("student001", "session_123456", 8)).thenReturn(List.of());

        AgentAIService service = new AgentAIService(
                client, new ObjectMapper(), tools, history, logs);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "baseUrl", "https://api.deepseek.com");
        ReflectionTestUtils.setField(service, "model", "deepseek-v4-flash");
        ReflectionTestUtils.setField(service, "maxIterations", 6);
        ReflectionTestUtils.setField(service, "historyLimit", 8);

        AgentRunResponse result = service.runAgent(
                "图书馆有多少种书？", "student001", "session_123456");

        assertThat(result.answer()).isEqualTo("本馆共有100种图书。");
        assertThat(result.toolsUsed()).containsExactly("get_library_statistics");
        assertThat(result.iterations()).isEqualTo(2);
        verify(tools).execute("get_library_statistics", "{}", "student001");
        verify(history).saveChat(
                "student001", "session_123456", "图书馆有多少种书？", "本馆共有100种图书。", "agent", null, null);
    }

    @Test
    void promotesVerifiedEbookToolResultToStructuredNavigation() throws Exception {
        OkHttpClient client = mock(OkHttpClient.class);
        Call firstCall = mock(Call.class);
        Call secondCall = mock(Call.class);
        AgentToolService tools = mock(AgentToolService.class);
        ChatHistoryService history = mock(ChatHistoryService.class);
        OperationLogService logs = mock(OperationLogService.class);

        when(client.newCall(any(Request.class))).thenReturn(firstCall, secondCall);
        when(firstCall.execute()).thenReturn(response("""
                {"choices":[{"finish_reason":"tool_calls","message":{"role":"assistant","content":null,
                "tool_calls":[{"id":"call_read","type":"function","function":{"name":"open_verified_ebook","arguments":"{\\\"isbn_or_title\\\":\\\"红楼梦\\\",\\\"chapter\\\":10}"}}]}}]}
                """));
        when(secondCall.execute()).thenReturn(response("""
                {"choices":[{"finish_reason":"stop","message":{"role":"assistant","content":"已为你打开《红楼梦》第十回。"}}]}
                """));
        when(tools.definitions()).thenReturn(List.of(Map.of("type", "function")));
        when(tools.displayName("open_verified_ebook")).thenReturn("合规电子书阅读");
        when(tools.execute("open_verified_ebook", "{\"isbn_or_title\":\"红楼梦\",\"chapter\":10}", "student001"))
                .thenReturn("{\"ok\":true,\"data\":{\"type\":\"OPEN_EBOOK\",\"isbn\":\"isbn-1\",\"bookTitle\":\"红楼梦\",\"chapterNumber\":10,\"chapterCount\":120,\"message\":\"已通过权属核验\"}}");
        when(history.getRecentSessionHistory("student001", "session_123456", 8)).thenReturn(List.of());

        AgentAIService service = new AgentAIService(client, new ObjectMapper(), tools, history, logs);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "baseUrl", "https://api.deepseek.com");
        ReflectionTestUtils.setField(service, "model", "deepseek-v4-flash");
        ReflectionTestUtils.setField(service, "maxIterations", 6);
        ReflectionTestUtils.setField(service, "historyLimit", 8);

        AgentRunResponse result = service.runAgent(
                "打开《红楼梦》第十回", "student001", "session_123456");

        assertThat(result.navigation()).isNotNull();
        assertThat(result.navigation().type()).isEqualTo("OPEN_EBOOK");
        assertThat(result.navigation().isbn()).isEqualTo("isbn-1");
        assertThat(result.navigation().chapterNumber()).isEqualTo(10);
    }

    private Response response(String json) {
        Request request = new Request.Builder().url("https://api.deepseek.com/chat/completions").build();
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(json, JSON))
                .build();
    }
}
