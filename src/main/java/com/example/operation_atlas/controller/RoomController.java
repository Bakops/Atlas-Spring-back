package com.example.operation_atlas.controller;

import com.example.operation_atlas.dto.CreateRoomRequest;
import com.example.operation_atlas.dto.JoinRequest;
import com.example.operation_atlas.dto.RoomSnapshot;
import com.example.operation_atlas.dto.SubmitRequest;
import com.example.operation_atlas.model.GameRoom;
import com.example.operation_atlas.service.GameService;
import com.example.operation_atlas.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final GameService gameService;
    private final RateLimitService rateLimitService;

    public RoomController(GameService gameService, RateLimitService rateLimitService) {
        this.gameService = gameService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping
    public ResponseEntity<?> createRoom(@Valid @RequestBody CreateRoomRequest request,
                                        HttpServletRequest httpRequest) {
        rateLimitService.checkGameActionLimit(getClientIp(httpRequest));

        GameRoom room = gameService.createRoom(request.getPseudo());
        com.example.operation_atlas.dto.RoomSnapshot snapshot = com.example.operation_atlas.dto.RoomSnapshot.fromRoom(room);

        Map<String, Object> response = new HashMap<>();
        response.put("room", snapshot);
        response.put("playerId", room.getPlayers().get(0).getId());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<?> joinRoom(@PathVariable String roomId,
                                      @Valid @RequestBody JoinRequest request,
                                      HttpServletRequest httpRequest) {
        rateLimitService.checkGameActionLimit(getClientIp(httpRequest));

        GameRoom room = gameService.joinRoom(request.getJoinCode(), request.getPseudo());
        RoomSnapshot snapshot = RoomSnapshot.fromRoom(room);

        String playerId = room.getPlayers().stream()
                .filter(p -> p.getPseudo().equals(request.getPseudo()))
                .findFirst()
                .map(p -> p.getId())
                .orElse(null);

        Map<String, Object> response = new HashMap<>();
        response.put("room", snapshot);
        response.put("playerId", playerId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{roomId}/state")
    public ResponseEntity<?> getRoomState(@PathVariable String roomId,
                                          @RequestParam(required = false) Integer since) {
        GameRoom room = gameService.getRoom(roomId);
        RoomSnapshot snapshot = RoomSnapshot.fromRoom(room);


        if (since != null && room.getVersion() == since) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(snapshot);
    }

    @PostMapping("/{roomId}/start")
    public ResponseEntity<?> startGame(@PathVariable String roomId,
                                       HttpServletRequest httpRequest) {
        rateLimitService.checkGameActionLimit(getClientIp(httpRequest));
        gameService.startGame(roomId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/puzzle/{continent}")
    public ResponseEntity<?> submitPuzzle(@PathVariable String roomId,
                                          @PathVariable String continent,
                                          @Valid @RequestBody SubmitRequest request,
                                          HttpServletRequest httpRequest) {
        rateLimitService.checkGameActionLimit(getClientIp(httpRequest));
        gameService.submitPuzzle(roomId, continent, request.getAnswer(), request.getPlayerId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/hint/{continent}")
    public ResponseEntity<?> requestHint(@PathVariable String roomId,
                                         @PathVariable String continent,
                                         HttpServletRequest httpRequest) {
        rateLimitService.checkGameActionLimit(getClientIp(httpRequest));
        gameService.requestHint(roomId, continent);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/meta")
    public ResponseEntity<?> submitMeta(@PathVariable String roomId,
                                        @Valid @RequestBody SubmitRequest request,
                                        HttpServletRequest httpRequest) {
        rateLimitService.checkGameActionLimit(getClientIp(httpRequest));
        gameService.submitMeta(roomId, request.getAnswer());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/final")
    public ResponseEntity<?> submitFinal(@PathVariable String roomId,
                                         @Valid @RequestBody SubmitRequest request,
                                         HttpServletRequest httpRequest) {
        rateLimitService.checkGameActionLimit(getClientIp(httpRequest));
        gameService.submitFinal(roomId, request.getAnswer());
        return ResponseEntity.ok().build();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
