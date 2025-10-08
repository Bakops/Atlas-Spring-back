package com.example.operation_atlas.service;

import com.example.operation_atlas.model.PuzzleResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;

@Service
public class PuzzleService {

    private static final Logger log = LoggerFactory.getLogger(PuzzleService.class);
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Value("${atlas.demo.mode:false}")
    private boolean demoMode;

    private JsonNode euData;
    private JsonNode asData;
    private JsonNode amData;
    private JsonNode afData;
    private JsonNode ocData;
    private JsonNode anData;
    private JsonNode metaData;

    public PuzzleService(ObjectMapper objectMapper, ResourceLoader resourceLoader) {
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void loadData() {
        try {
            euData = loadJson("classpath:content/eu_salutations.json");
            asData = loadJson("classpath:content/as_time.json");
            amData = loadJson("classpath:content/am_items.json");
            afData = loadJson("classpath:content/af_currencies.json");
            ocData = loadJson("classpath:content/oc_islands.json");
            anData = loadJson("classpath:content/an_stations.json");
            metaData = loadJson("classpath:content/meta_config.json");
            log.info("Puzzle data loaded successfully (6 continents)");
        } catch (IOException e) {
            log.error("Failed to load puzzle data", e);
        }
    }

    private JsonNode loadJson(String path) throws IOException {
        Resource resource = resourceLoader.getResource(path);
        return objectMapper.readTree(resource.getInputStream());
    }

    // Europe: mot 5 lettres
    public PuzzleResult validateEurope(String answer) {
        String expected = euData.get("targetWord").asText().toUpperCase();
        String normalized = answer.trim().toUpperCase();

        if (normalized.length() != 5) {
            return PuzzleResult.error("E_EU_WRONG_LENGTH", "Le mot doit faire 5 lettres");
        }

        if (!normalized.equals(expected)) {
            return PuzzleResult.error("E_EU_WRONG_LETTER", "Ce n'est pas le bon mot");
        }

        // Fragment: première lettre du mot
        String fragment = String.valueOf(expected.charAt(0));
        return PuzzleResult.success(fragment);
    }

    // Asie: horaire UTC HH:MM
    public PuzzleResult validateAsia(String answer) {
        String normalized = answer.trim();

        if (!normalized.matches("\\d{2}:\\d{2}")) {
            return PuzzleResult.error("E_AS_FORMAT", "Format attendu: HH:MM (ex: 06:30)");
        }

        // Vérifier si l'horaire est dans la whitelist
        JsonNode validSlots = asData.get("validSlotsUTC");
        boolean valid = false;
        for (JsonNode slot : validSlots) {
            if (slot.asText().equals(normalized)) {
                valid = true;
                break;
            }
        }

        if (!valid) {
            return PuzzleResult.error("E_AS_NO_COMMON_SLOT", "Aucune ville n'est dans sa plage 08:00-20:00 à cet horaire");
        }

        // Fragment: direction basée sur les minutes
        String[] parts = normalized.split(":");
        int minutes = Integer.parseInt(parts[1]);
        String direction = (minutes == 30) ? "→→" : "→↑";

        return PuzzleResult.success(direction);
    }

    // Amériques: code 4 chiffres (somme poids cabine)
    public PuzzleResult validateAmericas(String answer) {
        String normalized = answer.trim();

        if (!normalized.matches("\\d{4}")) {
            return PuzzleResult.error("E_AM_FORMAT", "Le code doit être 4 chiffres");
        }

        int expected = calculateCabinWeight();
        int provided = Integer.parseInt(normalized);

        if (provided != expected) {
            return PuzzleResult.error("E_AM_SUM_MISMATCH", "La somme ne correspond pas aux règles");
        }

        // Fragment: lettre joker (ex: 'X')
        String fragment = metaData.get("jokerLetter").asText();
        return PuzzleResult.success(fragment);
    }

    private int calculateCabinWeight() {
        // Calculer la somme des poids des objets autorisés en cabine
        JsonNode items = amData.get("items");
        JsonNode rules = amData.get("rules");

        int maxCabinKg = rules.get("maxCabinKg").asInt();
        int maxLiquidMl = rules.get("maxLiquidMl").asInt();
        int maxPowerWh = rules.get("maxPowerWh").asInt();

        int totalWeight = 0;
        for (JsonNode item : items) {
            String name = item.get("name").asText();
            int weight = item.get("weightKg").asInt();
            int volume = item.get("volumeMl").asInt();
            boolean isLiquid = item.get("isLiquid").asBoolean();
            int power = item.has("powerWh") ? item.get("powerWh").asInt() : 0;

            // Vérifier si autorisé en cabine
            boolean allowed = !isLiquid || volume <= maxLiquidMl;

            if (power > maxPowerWh) {
                allowed = false;
            }

            // Vérifier les interdits absolus
            JsonNode prohibited = amData.get("prohibitedItems");
            for (JsonNode p : prohibited) {
                if (p.asText().equalsIgnoreCase(name)) {
                    allowed = false;
                    break;
                }
            }

            if (allowed) {
                totalWeight += weight;
            }
        }

        return totalWeight;
    }

    // Afrique: calcul conversion monétaire
    public PuzzleResult validateAfrica(String answer) {
        String normalized = answer.trim();

        if (!normalized.matches("\\d{4}")) {
            return PuzzleResult.error("E_AF_FORMAT", "Le code doit être 4 chiffres");
        }

        int expected = afData.get("expectedFinalAmount").asInt();
        int provided = Integer.parseInt(normalized);

        if (provided != expected) {
            return PuzzleResult.error("E_AF_WRONG_CALC", "Le montant final n'est pas correct");
        }

        // Fragment: première lettre du continent (A pour Africa)
        String fragment = "A";
        return PuzzleResult.success(fragment);
    }

    // Océanie: route la plus courte
    public PuzzleResult validateOceania(String answer) {
        String normalized = answer.trim().toUpperCase();

        if (normalized.length() != 1 || !normalized.matches("[A-D]")) {
            return PuzzleResult.error("E_OC_FORMAT", "Réponse attendue: une lettre (A, B, C ou D)");
        }

        String expected = ocData.get("correctRoute").asText();

        if (!normalized.equals(expected)) {
            return PuzzleResult.error("E_OC_WRONG_ROUTE", "Ce n'est pas la route la plus courte");
        }

        // Fragment: direction basée sur la route
        String fragment = "→";
        return PuzzleResult.success(fragment);
    }

    // Antarctique: station la plus froide
    public PuzzleResult validateAntarctica(String answer) {
        String normalized = answer.trim().toUpperCase();

        if (normalized.length() < 3) {
            return PuzzleResult.error("E_AN_FORMAT", "Entrez le nom de la station");
        }

        String expected = anData.get("expectedAnswer").asText();

        if (!normalized.equals(expected)) {
            return PuzzleResult.error("E_AN_WRONG_STATION", "Ce n'est pas la station la plus froide");
        }

        // Fragment: première lettre (V pour Vostok)
        String fragment = String.valueOf(expected.charAt(0));
        return PuzzleResult.success(fragment);
    }

    // Méta: vérifier clé finale
    public boolean validateMeta(String answer, Map<String, String> fragments) {
        if (demoMode) {
            // En mode démo, accepter une clé fixe
            return answer.trim().equalsIgnoreCase("MONDE→↑→↑");
        }

        // Construire la clé attendue depuis les fragments
        String letterEU = fragments.getOrDefault("letterEU", "");
        String directionAS = fragments.getOrDefault("directionAS", "");
        String letterJoker = fragments.getOrDefault("letterJoker", "");

        // Exemple: MONDE + →→→↑
        String expectedKey = metaData.get("expectedKey").asText();
        String normalized = answer.trim().toUpperCase();

        return normalized.equals(expectedKey);
    }

    // Générer le code de désactivation final basé sur les continents tirés
    private static final Map<String, String> CONTINENT_CODES = Map.of(
            "EUROPE", "CULTURE",
            "ASIA", "TEMPS",
            "AMERICAS", "VOYAGE",
            "AFRICA", "FINANCE",
            "OCEANIA", "ILES",
            "ANTARCTICA", "GLACE"
    );

    public String generateFinalCode(List<String> drawnContinents) {
        StringBuilder code = new StringBuilder();
        for (String continent : drawnContinents) {
            String keyword = CONTINENT_CODES.get(continent);
            if (keyword != null && !keyword.isEmpty()) {
                code.append(keyword.charAt(0));
            }
        }
        return code.toString();
    }

    public boolean validateFinal(String answer, List<String> drawnContinents) {
        String expectedCode = generateFinalCode(drawnContinents);
        String normalized = answer.trim().toUpperCase();
        return normalized.equals(expectedCode);
    }
}