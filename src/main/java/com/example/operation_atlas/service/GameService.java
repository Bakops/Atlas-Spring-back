package com.example.operation_atlas.service;


import com.example.operation_atlas.exception.GameException;
import com.example.operation_atlas.model.GameRoom;
import com.example.operation_atlas.model.GameStage;
import com.example.operation_atlas.model.Player;
import com.example.operation_atlas.model.PuzzleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {

    private static final Logger log = LoggerFactory.getLogger(GameService.class);
    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private final Map<String, String> joinCodeToRoomId = new ConcurrentHashMap<>();
    private final PuzzleService puzzleService;
    private final com.example.operation_atlas.service.SnapshotService snapshotService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${atlas.room.ttl.minutes:30}")
    private int roomTtlMinutes;

    public GameService(PuzzleService puzzleService,
                       com.example.operation_atlas.service.SnapshotService SnapshotService,
                       SimpMessagingTemplate messagingTemplate) {
        this.puzzleService = puzzleService;
        this.snapshotService = SnapshotService;
        this.messagingTemplate = messagingTemplate;
    }

    public GameRoom createRoom(String creatorPseudo) {
        String roomId = UUID.randomUUID().toString();
        String joinCode = generateJoinCode();

        GameRoom room = new GameRoom(roomId, joinCode);
        Player creator = new Player(UUID.randomUUID().toString(), creatorPseudo);
        room.getPlayers().add(creator);

        // Tirer 3 continents au sort parmi les 6 disponibles
        List<String> allContinents = new ArrayList<>(Arrays.asList(
                "EUROPE", "ASIA", "AMERICAS", "AFRICA", "OCEANIA", "ANTARCTICA"
        ));
        Collections.shuffle(allContinents);
        List<String> selectedContinents = new ArrayList<>(allContinents.subList(0, 3));
        room.setDraw(selectedContinents);

        // Initialiser solved et hintsUsed en fonction des continents tirés
        room.initializePuzzlesForDraw();

        log.info("Room {} drawn continents: {}", roomId, selectedContinents);

        rooms.put(roomId, room);
        joinCodeToRoomId.put(joinCode, roomId);

        log.info("Room created: {} with code {}", roomId, joinCode);
        return room;
    }

    public GameRoom joinRoom(String joinCode, String pseudo) {
        String roomId = joinCodeToRoomId.get(joinCode.toUpperCase());
        if (roomId == null) {
            throw new GameException("ERR_ROOM_NOT_FOUND", "Room not found");
        }

        GameRoom room = rooms.get(roomId);
        if (room == null) {
            throw new GameException("ERR_ROOM_NOT_FOUND", "Room not found");
        }

        if (room.getPlayers().size() >= 4) {
            throw new GameException("ERR_ROOM_FULL", "Room is full (max 4 players)");
        }

        Player newPlayer = new Player(UUID.randomUUID().toString(), pseudo);
        room.getPlayers().add(newPlayer);
        room.incrementVersion();

        broadcastSnapshot(room);
        log.info("Player {} joined room {}", pseudo, roomId);
        return room;
    }

    public GameRoom getRoom(String roomId) {
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            throw new GameException("ERR_ROOM_NOT_FOUND", "Room not found");
        }
        return room;
    }

    public void startGame(String roomId) {
        GameRoom room = getRoom(roomId);
        if (room.getPlayers().size() < 2) {
            throw new GameException("ERR_STAGE", "Need at least 2 players to start");
        }
        room.setStage(GameStage.PLAY);
        broadcastStageChange(room);
        log.info("Game started in room {}", roomId);
    }

    public void submitPuzzle(String roomId, String continent, String answer, String playerId) {
        GameRoom room = getRoom(roomId);

        if (room.getStage() != GameStage.PLAY) {
            throw new GameException("ERR_STAGE", "Cannot submit puzzle at this stage");
        }

        String continentKey = continent.toLowerCase().substring(0, 2);
        if (room.getSolved().getOrDefault(continentKey, false)) {
            throw new GameException("ERR_ALREADY_SOLVED", "Puzzle already solved");
        }

        PuzzleResult result;
        switch (continent.toUpperCase()) {
            case "EUROPE":
                result = puzzleService.validateEurope(answer);
                break;
            case "ASIA":
                result = puzzleService.validateAsia(answer);
                break;
            case "AMERICAS":
                result = puzzleService.validateAmericas(answer);
                break;
            case "AFRICA":
                result = puzzleService.validateAfrica(answer);
                break;
            case "OCEANIA":
                result = puzzleService.validateOceania(answer);
                break;
            case "ANTARCTICA":
                result = puzzleService.validateAntarctica(answer);
                break;
            default:
                throw new GameException("ERR_INVALID_CONTINENT", "Invalid continent");
        }

        if (result.isSuccess()) {
            room.getSolved().put(continentKey, true);
            storeFragment(room, continent, result.getFragment());
            room.incrementVersion();
            broadcastPuzzleResult(room, continent, true, null);

            // Vérifier si tous les puzzles sont résolus
            if (room.allPuzzlesSolved()) {
                room.setStage(GameStage.META);
                broadcastStageChange(room);
            }
        } else {
            broadcastPuzzleResult(room, continent, false, result.getErrorCode());
        }
    }

    private void storeFragment(GameRoom room, String continent, String fragment) {
        switch (continent.toUpperCase()) {
            case "EUROPE":
                room.getFragments().put("letterEU", fragment);
                break;
            case "ASIA":
                room.getFragments().put("directionAS", fragment);
                break;
            case "AMERICAS":
                room.getFragments().put("letterJoker", fragment);
                break;
            case "AFRICA":
                room.getFragments().put("letterAF", fragment);
                break;
            case "OCEANIA":
                room.getFragments().put("directionOC", fragment);
                break;
            case "ANTARCTICA":
                room.getFragments().put("letterAN", fragment);
                break;
        }
    }

    public void requestHint(String roomId, String continent) {
        GameRoom room = getRoom(roomId);
        String continentKey = continent.toLowerCase().substring(0, 2);

        int used = room.getHintsUsed().getOrDefault(continentKey, 0);
        if (used >= 2) {
            throw new GameException("ERR_MAX_HINTS", "Maximum hints reached for this puzzle");
        }

        room.getHintsUsed().put(continentKey, used + 1);
        room.setTimerSec(Math.max(0, room.getTimerSec() - 60));
        room.incrementVersion();

        broadcastHintGranted(room, continent);
        log.info("Hint granted for {} in room {}", continent, roomId);
    }

    public void submitMeta(String roomId, String answer) {
        GameRoom room = getRoom(roomId);

        if (room.getStage() != GameStage.META) {
            throw new GameException("ERR_STAGE", "Cannot submit meta at this stage");
        }

        boolean correct = puzzleService.validateMeta(answer, room.getFragments());
        if (correct) {
            room.setStage(GameStage.FINAL);
            room.setFinalStartedAt(Instant.now());
            broadcastStageChange(room);
        } else {
            throw new GameException("ERR_META_WRONG", "Incorrect meta solution");
        }
    }

    public void submitFinal(String roomId, String answer) {
        GameRoom room = getRoom(roomId);

        if (room.getStage() != GameStage.FINAL) {
            throw new GameException("ERR_STAGE", "Cannot submit final at this stage");
        }

        // Vérifier fenêtre de 30s
        Instant now = Instant.now();
        long elapsed = Duration.between(room.getFinalStartedAt(), now).getSeconds();
        if (elapsed > 30) {
            room.setStage(GameStage.DEBRIEF);
            broadcastFinalResult(room, false);
            throw new GameException("ERR_FINAL_TIMEOUT", "Time's up for final submission");
        }

        // Vérifier code de désactivation final (basé sur les continents tirés)
        boolean correct = puzzleService.validateFinal(answer, room.getDraw());
        if (correct) {
            room.setStage(GameStage.DEBRIEF);
            broadcastFinalResult(room, true);
        } else {
            throw new GameException("ERR_FINAL_WRONG", "Code de désactivation incorrect");
        }
    }

    public void sendChatMessage(String roomId, String playerId, String message) {
        GameRoom room = getRoom(roomId);

        // Trouver le pseudo du joueur
        String pseudo = room.getPlayers().stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .map(Player::getPseudo)
                .orElse("Unknown");

        Map<String, String> chatMsg = new HashMap<>();
        chatMsg.put("playerId", playerId);
        chatMsg.put("pseudo", pseudo);
        chatMsg.put("message", message);
        chatMsg.put("timestamp", String.valueOf(System.currentTimeMillis()));

        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/chat", chatMsg);
    }

    // Timer tick toutes les secondes
    @Scheduled(fixedRate = 1000)
    public void timerTick() {
        for (GameRoom room : rooms.values()) {
            if (room.getStage() == GameStage.PLAY || room.getStage() == GameStage.META) {
                if (room.getTimerSec() > 0) {
                    room.setTimerSec(room.getTimerSec() - 1);
                    broadcastTimerTick(room);

                    if (room.getTimerSec() == 0) {
                        room.setStage(GameStage.DEBRIEF);
                        broadcastStageChange(room);
                    }
                }
            }
        }
    }

    // Snapshots toutes les 10 secondes
    @Scheduled(fixedRate = 10000)
    public void snapshotRooms() {
        for (GameRoom room : rooms.values()) {
            snapshotService.saveRoom(room);
        }
    }

    // Cleanup des vieilles rooms
    @Scheduled(fixedRate = 60000)
    public void cleanupRooms() {
        Instant threshold = Instant.now().minusSeconds(roomTtlMinutes * 60L);
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, GameRoom> entry : rooms.entrySet()) {
            if (entry.getValue().getLastActivity().isBefore(threshold)) {
                toRemove.add(entry.getKey());
            }
        }

        for (String roomId : toRemove) {
            GameRoom room = rooms.remove(roomId);
            if (room != null) {
                joinCodeToRoomId.remove(room.getJoinCode());
                log.info("Cleaned up room {}", roomId);
            }
        }
    }

    private String generateJoinCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder code = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    private void broadcastSnapshot(GameRoom room) {
        com.example.operation_atlas.dto.RoomSnapshot snapshot = com.example.operation_atlas.dto.RoomSnapshot.fromRoom(room);
        messagingTemplate.convertAndSend("/topic/rooms/" + room.getId(), snapshot);
    }

    private void broadcastTimerTick(GameRoom room) {
        Map<String, Object> tick = new HashMap<>();
        tick.put("type", "TIMER_TICK");
        tick.put("timerSec", room.getTimerSec());
        messagingTemplate.convertAndSend("/topic/rooms/" + room.getId(), tick);
    }

    private void broadcastStageChange(GameRoom room) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "STAGE_CHANGE");
        event.put("stage", room.getStage());
        messagingTemplate.convertAndSend("/topic/rooms/" + room.getId(), event);
        broadcastSnapshot(room);
    }

    private void broadcastPuzzleResult(GameRoom room, String continent, boolean success, String errorCode) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "PUZZLE_RESULT");
        result.put("continent", continent);
        result.put("success", success);
        if (errorCode != null) {
            result.put("errorCode", errorCode);
        }
        messagingTemplate.convertAndSend("/topic/rooms/" + room.getId(), result);
        if (success) {
            broadcastSnapshot(room);
        }
    }

    private void broadcastHintGranted(GameRoom room, String continent) {
        Map<String, Object> hint = new HashMap<>();
        hint.put("type", "HINT_GRANTED");
        hint.put("continent", continent);
        hint.put("timerSec", room.getTimerSec());
        messagingTemplate.convertAndSend("/topic/rooms/" + room.getId(), hint);
        broadcastSnapshot(room);
    }

    private void broadcastFinalResult(GameRoom room, boolean success) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "FINAL_RESULT");
        result.put("success", success);
        messagingTemplate.convertAndSend("/topic/rooms/" + room.getId(), result);
        broadcastSnapshot(room);
    }
}
