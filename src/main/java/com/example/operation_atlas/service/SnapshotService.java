package com.example.operation_atlas.service;

import com.example.operation_atlas.model.GameRoom;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

@Service
public class SnapshotService {

    private static final Logger log = LoggerFactory.getLogger(SnapshotService.class);
    private final ObjectMapper objectMapper;
    private final Path saveDirectory;

    public SnapshotService(@Value("${atlas.save.dir}") String saveDir) {
        this.saveDirectory = Paths.get(saveDir);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        try {
            Files.createDirectories(saveDirectory);
        } catch (IOException e) {
            log.error("Failed to create save directory: {}", saveDir, e);
        }
    }

    public void saveRoom(GameRoom room) {
        try {
            File file = saveDirectory.resolve(room.getId() + ".json").toFile();
            objectMapper.writeValue(file, room);
            log.info("Saved room {} to disk", room.getId());
        } catch (IOException e) {
            log.error("Failed to save room {}", room.getId(), e);
        }
    }

    public GameRoom loadRoom(String roomId) {
        try {
            File file = saveDirectory.resolve(roomId + ".json").toFile();
            if (file.exists()) {
                return objectMapper.readValue(file, GameRoom.class);
            }
        } catch (IOException e) {
            log.error("Failed to load room {}", roomId, e);
        }
        return null;
    }

    @Scheduled(fixedRate = 10000) // Toutes les 10 secondes
    public void snapshotAllRooms() {
        // Appelé par GameService pour sauvegarder toutes les rooms actives
    }
}
