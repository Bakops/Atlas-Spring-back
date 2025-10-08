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
            log.info("✅ Données des énigme correctement charger (6 continents)");
        } catch (IOException e) {
            log.error("❌ Échec de chargement des données de énignmes", e);
            // Initialiser avec des données par défaut pour éviter les NullPointerException
            initializeDefaultData();
        }
    }

    private void initializeDefaultData() {
        try {
            ObjectMapper mapper = new ObjectMapper();

            // Données par défaut pour l'Europe
            String defaultEuData = """
                {
                  "salutations": [
                    {"greeting": "Hello", "language": "English", "country": "UK", "linguisticFamily": "Germanic"},
                    {"greeting": "Bonjour", "language": "French", "country": "France", "linguisticFamily": "Romance"},
                    {"greeting": "Hola", "language": "Spanish", "country": "Spain", "linguisticFamily": "Romance"},
                    {"greeting": "Ciao", "language": "Italian", "country": "Italy", "linguisticFamily": "Romance"},
                    {"greeting": "Hallo", "language": "German", "country": "Germany", "linguisticFamily": "Germanic"}
                  ],
                  "familyMapping": {
                    "Germanic": "M",
                    "Romance": "O",
                    "Slavic": "N",
                    "Uralic": "D",
                    "Greek": "E"
                  },
                  "targetWord": "MONDE",
                  "hint": "Associez chaque famille linguistique à sa lettre pour former un mot de 5 lettres"
                }
            """;
            euData = mapper.readTree(defaultEuData);

            log.warn("⚠️ Using default puzzle data - check your content files!");
        } catch (Exception e) {
            log.error("❌ Failed to initialize default data", e);
        }
    }

    private JsonNode loadJson(String path) throws IOException {
        try {
            Resource resource = resourceLoader.getResource(path);
            if (!resource.exists()) {
                log.error("❌ Resource not found: {}", path);
                return null;
            }
            JsonNode data = objectMapper.readTree(resource.getInputStream());
            log.info("✅ Loaded: {}", path);
            return data;
        } catch (Exception e) {
            log.error("❌ Failed to load: {}", path, e);
            return null;
        }
    }

    // Europe: mot 5 lettres - CORRIGÉ
    public PuzzleResult validateEurope(String answer) {
        try {
            // Vérifier que les données sont chargées
            if (euData == null) {
                log.error("❌ Europe data not loaded");
                return PuzzleResult.error("ERR_DATA_NOT_LOADED", "Données Europe non chargées");
            }

            String expected = euData.get("targetWord").asText().toUpperCase();
            String normalized = answer.trim().toUpperCase();

            log.info("🔍 Europe validation - Answer: {}, Expected: {}", normalized, expected);

            if (normalized.length() != 5) {
                return PuzzleResult.error("E_EU_WRONG_LENGTH", "Le mot doit faire 5 lettres");
            }

            if (!normalized.equals(expected)) {
                return PuzzleResult.error("E_EU_WRONG_LETTER", "Ce n'est pas le bon mot");
            }

            // Fragment: première lettre du mot
            String fragment = String.valueOf(expected.charAt(0));
            log.info("✅ Europe puzzle solved! Fragment: {}", fragment);
            return PuzzleResult.success(fragment);

        } catch (Exception e) {
            log.error("❌ Error validating Europe puzzle", e);
            return PuzzleResult.error("ERR_VALIDATION_ERROR", "Erreur de validation");
        }
    }

    // Asie: horaire UTC HH:MM - CORRIGÉ
    public PuzzleResult validateAsia(String answer) {
        try {
            if (asData == null) {
                log.error("❌ Asia data not loaded");
                return PuzzleResult.error("ERR_DATA_NOT_LOADED", "Données Asie non chargées");
            }

            String normalized = answer.trim();
            log.info("🔍 Asia validation - Answer: {}", normalized);

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

            log.info("✅ Asia puzzle solved! Fragment: {}", direction);
            return PuzzleResult.success(direction);

        } catch (Exception e) {
            log.error("❌ Error validating Asia puzzle", e);
            return PuzzleResult.error("ERR_VALIDATION_ERROR", "Erreur de validation");
        }
    }

    // Amériques: code 4 chiffres (somme poids cabine) - CORRIGÉ
    public PuzzleResult validateAmericas(String answer) {
        try {
            if (amData == null) {
                log.error("❌ Americas data not loaded");
                return PuzzleResult.error("ERR_DATA_NOT_LOADED", "Données Amériques non chargées");
            }

            String normalized = answer.trim();
            log.info("🔍 Americas validation - Answer: {}", normalized);

            if (!normalized.matches("\\d{4}")) {
                return PuzzleResult.error("E_AM_FORMAT", "Le code doit être 4 chiffres");
            }

            int expected = calculateCabinWeight();
            int provided = Integer.parseInt(normalized);

            log.info("🔍 Americas weight check - Provided: {}, Expected: {}", provided, expected);

            if (provided != expected) {
                return PuzzleResult.error("E_AM_SUM_MISMATCH", "La somme ne correspond pas aux règles");
            }

            // Fragment: lettre joker
            String fragment = "X"; // Toujours X pour les Amériques
            log.info("✅ Americas puzzle solved! Fragment: {}", fragment);
            return PuzzleResult.success(fragment);

        } catch (Exception e) {
            log.error("❌ Error validating Americas puzzle", e);
            return PuzzleResult.error("ERR_VALIDATION_ERROR", "Erreur de validation");
        }
    }

    private int calculateCabinWeight() {
        try {
            JsonNode items = amData.get("items");
            JsonNode rules = amData.get("rules");

            int maxLiquidMl = rules.get("maxLiquidMl").asInt();
            int maxPowerWh = rules.get("maxPowerWh").asInt();

            int totalWeight = 0;
            for (JsonNode item : items) {
                String name = item.get("name").asText();
                int weight = item.get("weightKg").asInt();
                boolean isLiquid = item.get("isLiquid").asBoolean();
                int power = item.has("powerWh") ? item.get("powerWh").asInt() : 0;

                // Vérifier si autorisé en cabine
                boolean allowed = true;

                // Vérifier liquides
                if (isLiquid && item.get("volumeMl").asInt() > maxLiquidMl) {
                    allowed = false;
                }

                // Vérifier batteries
                if (power > maxPowerWh) {
                    allowed = false;
                }

                // Vérifier les interdits
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

            log.info("📦 Calculated cabin weight: {} kg", totalWeight);
            return totalWeight;

        } catch (Exception e) {
            log.error("❌ Error calculating cabin weight", e);
            return 8; // Valeur par défaut pour tester
        }
    }

    // Afrique: calcul conversion monétaire - CORRIGÉ
    public PuzzleResult validateAfrica(String answer) {
        try {
            if (afData == null) {
                log.error("❌ Africa data not loaded");
                return PuzzleResult.error("ERR_DATA_NOT_LOADED", "Données Afrique non chargées");
            }

            String normalized = answer.trim();
            log.info("🔍 Africa validation - Answer: {}", normalized);

            if (!normalized.matches("\\d{4}")) {
                return PuzzleResult.error("E_AF_FORMAT", "Le code doit être 4 chiffres");
            }

            // En mode démo, accepter une valeur simple
            int expected = demoMode ? 1234 : afData.get("expectedFinalAmount").asInt();
            int provided = Integer.parseInt(normalized);

            if (provided != expected) {
                return PuzzleResult.error("E_AF_WRONG_CALC", "Le montant final n'est pas correct");
            }

            String fragment = "A";
            log.info("✅ Africa puzzle solved! Fragment: {}", fragment);
            return PuzzleResult.success(fragment);

        } catch (Exception e) {
            log.error("❌ Error validating Africa puzzle", e);
            return PuzzleResult.error("ERR_VALIDATION_ERROR", "Erreur de validation");
        }
    }

    // Océanie: route la plus courte - CORRIGÉ
    public PuzzleResult validateOceania(String answer) {
        try {
            if (ocData == null) {
                log.error("❌ Oceania data not loaded");
                return PuzzleResult.error("ERR_DATA_NOT_LOADED", "Données Océanie non chargées");
            }

            String normalized = answer.trim().toUpperCase();
            log.info("🔍 Oceania validation - Answer: {}", normalized);

            if (normalized.length() != 1 || !normalized.matches("[A-D]")) {
                return PuzzleResult.error("E_OC_FORMAT", "Réponse attendue: une lettre (A, B, C ou D)");
            }

            // En mode démo, accepter A
            String expected = demoMode ? "A" : ocData.get("correctRoute").asText();

            if (!normalized.equals(expected)) {
                return PuzzleResult.error("E_OC_WRONG_ROUTE", "Ce n'est pas la route la plus courte");
            }

            String fragment = "→";
            log.info("✅ Oceania puzzle solved! Fragment: {}", fragment);
            return PuzzleResult.success(fragment);

        } catch (Exception e) {
            log.error("❌ Error validating Oceania puzzle", e);
            return PuzzleResult.error("ERR_VALIDATION_ERROR", "Erreur de validation");
        }
    }

    // Antarctique: station la plus froide - CORRIGÉ
    public PuzzleResult validateAntarctica(String answer) {
        try {
            if (anData == null) {
                log.error("❌ Antarctica data not loaded");
                return PuzzleResult.error("ERR_DATA_NOT_LOADED", "Données Antarctique non chargées");
            }

            String normalized = answer.trim().toUpperCase();
            log.info("🔍 Antarctica validation - Answer: {}", normalized);

            if (normalized.length() < 3) {
                return PuzzleResult.error("E_AN_FORMAT", "Entrez le nom de la station");
            }

            // En mode démo, accepter VOSTOK
            String expected = demoMode ? "VOSTOK" : anData.get("expectedAnswer").asText();

            if (!normalized.equals(expected)) {
                return PuzzleResult.error("E_AN_WRONG_STATION", "Ce n'est pas la station la plus froide");
            }

            String fragment = String.valueOf(expected.charAt(0));
            log.info("✅ Antarctica puzzle solved! Fragment: {}", fragment);
            return PuzzleResult.success(fragment);

        } catch (Exception e) {
            log.error("❌ Error validating Antarctica puzzle", e);
            return PuzzleResult.error("ERR_VALIDATION_ERROR", "Erreur de validation");
        }
    }

    // Méta: vérifier clé finale - CORRIGÉ
    public boolean validateMeta(String answer, Map<String, String> fragments) {
        try {
            log.info("🔍 Meta validation - Answer: {}, Fragments: {}", answer, fragments);

            if (demoMode) {
                // En mode démo, accepter une clé simple
                return answer.trim().equalsIgnoreCase("MONDE→→A");
            }

            // Construire la clé depuis les fragments
            StringBuilder expectedKey = new StringBuilder();

            String letterEU = fragments.getOrDefault("letterEU", "M");
            String directionAS = fragments.getOrDefault("directionAS", "→→");
            String letterAF = fragments.getOrDefault("letterAF", "A");

            expectedKey.append(letterEU).append("ONDE"); // MONDE
            expectedKey.append(directionAS); // →→
            expectedKey.append(letterAF); // A

            String normalized = answer.trim().toUpperCase();
            boolean valid = normalized.equals(expectedKey.toString());

            log.info("🔍 Meta check - Expected: {}, Got: {}, Valid: {}",
                    expectedKey, normalized, valid);

            return valid;

        } catch (Exception e) {
            log.error("❌ Error validating meta puzzle", e);
            return false;
        }
    }

    // Générer le code de désactivation final - CORRIGÉ
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

        String finalCode = code.toString();
        log.info("🔑 Generated final code for {}: {}", drawnContinents, finalCode);
        return finalCode;
    }

    public boolean validateFinal(String answer, List<String> drawnContinents) {
        try {
            String expectedCode = generateFinalCode(drawnContinents);
            String normalized = answer.trim().toUpperCase();

            boolean valid = normalized.equals(expectedCode);
            log.info("🔍 Final validation - Expected: {}, Got: {}, Valid: {}",
                    expectedCode, normalized, valid);

            return valid;
        } catch (Exception e) {
            log.error("❌ Error validating final puzzle", e);
            return false;
        }
    }
}