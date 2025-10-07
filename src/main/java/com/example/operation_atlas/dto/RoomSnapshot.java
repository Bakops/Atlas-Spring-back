package com.example.operation_atlas.dto;



import com.example.operation_atlas.model.GameRoom;
import com.example.operation_atlas.model.GameStage;
import com.example.operation_atlas.model.Player;

import java.util.List;
import java.util.Map;

public class RoomSnapshot {
    private String id;
    private String joinCode;
    private GameStage stage;
    private int timerSec;
    private List<String> draw;
    private Map<String, Boolean> solved;
    private Map<String, Integer> hintsUsed;
    private Map<String, String> fragments;
    private List<Player> players;
    private int version;

    public static RoomSnapshot fromRoom(GameRoom room) {
        RoomSnapshot snapshot = new RoomSnapshot();
        snapshot.id = room.getId();
        snapshot.joinCode = room.getJoinCode();
        snapshot.stage = room.getStage();
        snapshot.timerSec = room.getTimerSec();
        snapshot.draw = room.getDraw();
        snapshot.solved = room.getSolved();
        snapshot.hintsUsed = room.getHintsUsed();
        snapshot.fragments = room.getFragments();
        snapshot.players = room.getPlayers();
        snapshot.version = room.getVersion();
        return snapshot;
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
}
