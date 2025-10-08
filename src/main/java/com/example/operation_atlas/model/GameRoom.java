package com.example.operation_atlas.model;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameRoom {
    private String id;
    private String joinCode;
    private GameStage stage;
    private int timerSec;
    private List<String> draw; // 3 continents tirés au sort
    private Map<String, Boolean> solved; // eu, as, am
    private Map<String, Integer> hintsUsed; // eu, as, am
    private Map<String, String> fragments; // letterEU, directionAS, letterJoker
    private List<Player> players;
    private int version;
    private Instant createdAt;
    private Instant lastActivity;
    private Instant finalStartedAt; // Pour la fenêtre de 30s

    public GameRoom() {
        this.players = new ArrayList<>();
        this.solved = new ConcurrentHashMap<>();
        this.hintsUsed = new ConcurrentHashMap<>();
        this.fragments = new ConcurrentHashMap<>();
        this.draw = new ArrayList<>();
        this.version = 0;
        this.createdAt = Instant.now();
        this.lastActivity = Instant.now();
        this.stage = GameStage.BRIEF;
        this.timerSec = 1500; // 25 min par défaut
    }

    public GameRoom(String id, String joinCode) {
        this();
        this.id = id;
        this.joinCode = joinCode;
        // Ne plus initialiser ici, on le fera après avoir tiré les continents
    }

    public void initializePuzzlesForDraw() {
        // Initialiser solved et hintsUsed en fonction des continents tirés
        solved.clear();
        hintsUsed.clear();
        for (String continent : draw) {
            String key = continent.toLowerCase().substring(0, 2);
            solved.put(key, false);
            hintsUsed.put(key, 0);
        }
    }

    public void incrementVersion() {
        this.version++;
        this.lastActivity = Instant.now();
    }

    public boolean allPuzzlesSolved() {
        return solved.values().stream().allMatch(s -> s);
    }

    // Getters & Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getJoinCode() {
        return joinCode;
    }

    public void setJoinCode(String joinCode) {
        this.joinCode = joinCode;
    }

    public GameStage getStage() {
        return stage;
    }

    public void setStage(GameStage stage) {
        this.stage = stage;
        incrementVersion();
    }

    public int getTimerSec() {
        return timerSec;
    }

    public void setTimerSec(int timerSec) {
        this.timerSec = timerSec;
    }

    public List<String> getDraw() {
        return draw;
    }

    public void setDraw(List<String> draw) {
        this.draw = draw;
    }

    public Map<String, Boolean> getSolved() {
        return solved;
    }

    public void setSolved(Map<String, Boolean> solved) {
        this.solved = solved;
    }

    public Map<String, Integer> getHintsUsed() {
        return hintsUsed;
    }

    public void setHintsUsed(Map<String, Integer> hintsUsed) {
        this.hintsUsed = hintsUsed;
    }

    public Map<String, String> getFragments() {
        return fragments;
    }

    public void setFragments(Map<String, String> fragments) {
        this.fragments = fragments;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(Instant lastActivity) {
        this.lastActivity = lastActivity;
    }

    public Instant getFinalStartedAt() {
        return finalStartedAt;
    }

    public void setFinalStartedAt(Instant finalStartedAt) {
        this.finalStartedAt = finalStartedAt;
    }
}