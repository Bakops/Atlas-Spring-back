package com.example.operation_atlas.controller;

import com.example.operation_atlas.service.GameService;
import com.example.operation_atlas.service.RateLimitService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class WebSocketController {

    private final GameService gameService;
    private final RateLimitService rateLimitService;

    public WebSocketController(GameService gameService, RateLimitService rateLimitService) {
        this.gameService = gameService;
        this.rateLimitService = rateLimitService;
    }

    @MessageMapping("/rooms/{roomId}/puzzle")
    public void submitPuzzle(@DestinationVariable String roomId,
                             @Payload Map<String, String> payload) {
        String continent = payload.get("continent");
        String answer = payload.get("answer");
        String playerId = payload.get("playerId");

        gameService.submitPuzzle(roomId, continent, answer, playerId);
    }

    @MessageMapping("/rooms/{roomId}/hint")
    public void requestHint(@DestinationVariable String roomId,
                            @Payload Map<String, String> payload) {
        String continent = payload.get("continent");
        gameService.requestHint(roomId, continent);
    }

    @MessageMapping("/rooms/{roomId}/meta")
    public void submitMeta(@DestinationVariable String roomId,
                           @Payload Map<String, String> payload) {
        String answer = payload.get("answer");
        gameService.submitMeta(roomId, answer);
    }

    @MessageMapping("/rooms/{roomId}/final")
    public void submitFinal(@DestinationVariable String roomId,
                            @Payload Map<String, String> payload) {
        String answer = payload.get("answer");
        gameService.submitFinal(roomId, answer);
    }

    @MessageMapping("/rooms/{roomId}/chat")
    public void sendChat(@DestinationVariable String roomId,
                         @Payload Map<String, String> payload) {
        String message = payload.get("message");
        String playerId = payload.get("playerId");

        if (message == null || message.length() > 200) {
            return;
        }

        rateLimitService.checkChatLimit(playerId);
        gameService.sendChatMessage(roomId, playerId, message);
    }
}