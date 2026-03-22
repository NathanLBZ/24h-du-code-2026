package LesPointeursFous;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.control.*;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import LesPointeursFous.services.ApiClient;
import LesPointeursFous.services.ApiMap;
import LesPointeursFous.services.ApiVaisseau;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.FileInputStream;
import java.util.*;

public class GameView extends Application {

    private static final int CELL_SIZE = 25; // Taille d'une cellule en pixels
    private Canvas canvas;
    private GraphicsContext gc;
    private Gson gson = new Gson();
    private boolean firstLoad = true;
    private boolean isViewingDetails = false;  // Flag pour savoir si on affiche des détails
    private VBox vaisseauxList;
    private JsonArray currentVaisseaux;
    private JsonArray currentMapCases;
    private Map<String, Image> vaisseauImages = new HashMap<>();
    private Set<String> alliedVaisseauxIds = new HashSet<>();
    private Set<String> alliedPlanetesIds = new HashSet<>();
    private Map<String, Color> equipeColors = new HashMap<>();
    private Map<String, String> equipeNames = new HashMap<>();
    private ApiClient apiClient;
    private ApiMap apiMap;
    private ApiVaisseau apiVaisseau;
    private String idEquipe;

    // Palette de couleurs pour les équipes
    private Color[] colorPalette = {
        Color.CYAN,
        Color.MAGENTA,
        Color.YELLOW,
        Color.ORANGE,
        Color.PINK,
        Color.LIGHTGREEN,
        Color.LIGHTCORAL,
        Color.GOLD,
        Color.VIOLET,
        Color.TURQUOISE
    };
    private int colorIndex = 0;

    // Zone à afficher (zone réduite pour performance)
    private int xMin = 20;
    private int xMax = 45;
    private int yMin = 0;
    private int yMax = 25;
    private static final int MAX_RANGE = 17; // Limite API : 18x18 (0-17)

    @Override
    public void start(Stage primaryStage) {
        // Initialiser l'API
        Dotenv dotenv = Dotenv.configure().load();
        this.apiClient = new ApiClient(dotenv.get("API_URL"), dotenv.get("API_KEY"));
        apiMap = new ApiMap(apiClient);
        apiVaisseau = new ApiVaisseau(apiClient);
        idEquipe = "c1b647f1-1748-492a-b5a9-2a9af9b5e5ed";

        // Charger les IDs des vaisseaux et planètes alliés
        loadAlliedAssets();

        // Charger les images des vaisseaux
        loadVaisseauImages();

        // Créer le canvas
        int width = (xMax - xMin + 1) * CELL_SIZE;
        int height = (yMax - yMin + 1) * CELL_SIZE;
        canvas = new Canvas(width, height);
        gc = canvas.getGraphicsContext2D();
        System.out.println(">>> Canvas créé: " + width + "x" + height + " pixels");

        // Créer le panneau de vaisseaux
        vaisseauxList = new VBox(10);
        vaisseauxList.setPadding(new Insets(10));
        vaisseauxList.setStyle("-fx-background-color: #2b2b2b;");
        vaisseauxList.setPrefWidth(300);

        Label title = new Label("VOS VAISSEAUX");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        vaisseauxList.getChildren().add(title);

        ScrollPane scrollPane = new ScrollPane(vaisseauxList);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #2b2b2b;");

        // Ajouter un gestionnaire de clic sur le canvas
        canvas.setOnMouseClicked(event -> {
            System.out.println("!!! CLIC DÉTECTÉ SUR LE CANVAS !!!");
            System.out.println("Position brute: X=" + event.getX() + ", Y=" + event.getY());

            int cellX = (int) (event.getX() / CELL_SIZE);
            int cellY = (int) (event.getY() / CELL_SIZE);
            int mapX = xMin + cellX;
            int mapY = yMin + cellY;

            System.out.println("Cellule: " + cellX + ", " + cellY);
            System.out.println("Carte: " + mapX + ", " + mapY);

            handleCellClick(mapX, mapY);
        });

        // Layout principal
        HBox root = new HBox();
        root.getChildren().addAll(canvas, scrollPane);

        Scene scene = new Scene(root, width + 300, height);

        primaryStage.setTitle("24h du Code - Carte du jeu");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Dessiner la carte
        drawMap();

        // Afficher le panneau vaisseaux (avec le vaisseau test)
        updateVaisseauxPanel();

        // Charger les vaisseaux réels de l'API
        loadVaisseauxPanel();

        // Rafraîchir toutes les 2 secondes
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2000);
                    Platform.runLater(() -> {
                        drawMap();
                        // Ne recharger la liste que si on n'est pas en train de voir des détails
                        if (!isViewingDetails) {
                            loadVaisseauxPanel();
                        }
                    });
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    private Color getEquipeColor(String equipeName, String equipeId) {
        // Notre équipe est toujours en bleu
        if (equipeId != null && equipeId.equals(idEquipe)) {
            equipeColors.put(equipeId, Color.BLUE);
            equipeNames.put(equipeId, equipeName != null ? equipeName : "Notre équipe");
            return Color.BLUE;
        }

        // Si l'équipe n'a pas encore de couleur, lui en assigner une
        if (equipeId != null && !equipeColors.containsKey(equipeId)) {
            Color color = colorPalette[colorIndex % colorPalette.length];
            colorIndex++;
            equipeColors.put(equipeId, color);
            equipeNames.put(equipeId, equipeName != null ? equipeName : "Équipe " + colorIndex);

            // Afficher la nouvelle équipe découverte
            System.out.println("Nouvelle équipe: " + equipeNames.get(equipeId) + " - Couleur: " + color);
        }

        return equipeId != null ? equipeColors.get(equipeId) : Color.GRAY;
    }

    private void printLegend() {
        System.out.println("\n========== LÉGENDE DES COULEURS ==========");
        equipeNames.forEach((id, name) -> {
            Color color = equipeColors.get(id);
            String colorName = getColorName(color);
            String marker = id.equals(idEquipe) ? " ★ (VOUS)" : "";
            System.out.println("  " + name + ": " + colorName + marker);
        });
        System.out.println("===========================================\n");
    }

    private String getColorName(Color color) {
        if (color == Color.BLUE) return "BLEU";
        if (color == Color.CYAN) return "CYAN";
        if (color == Color.MAGENTA) return "MAGENTA";
        if (color == Color.YELLOW) return "JAUNE";
        if (color == Color.ORANGE) return "ORANGE";
        if (color == Color.PINK) return "ROSE";
        if (color == Color.LIGHTGREEN) return "VERT CLAIR";
        if (color == Color.LIGHTCORAL) return "CORAIL";
        if (color == Color.GOLD) return "OR";
        if (color == Color.VIOLET) return "VIOLET";
        if (color == Color.TURQUOISE) return "TURQUOISE";
        return "GRIS";
    }

    private void loadAlliedAssets() {
        alliedVaisseauxIds.clear();
        alliedPlanetesIds.clear();

        try {
            // Charger les vaisseaux alliés - IGNORER POUR L'INSTANT CAR L'API A DES ERREURS
            // L'API retourne une erreur d'entité manquante
            // On va détecter les vaisseaux alliés dynamiquement depuis la carte aussi

            // On définit notre équipe en bleu
            equipeColors.put(idEquipe, Color.BLUE);
            equipeNames.put(idEquipe, "Les Pointeurs Fous");

            System.out.println("INFO: Détection dynamique des vaisseaux et planètes alliés depuis la carte");

        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des assets alliés: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadVaisseauxPanel() {
        new Thread(() -> {
            try {
                System.out.println("\n========================================");
                System.out.println("DEBUG: Chargement des vaisseaux via API getVaisseaux()...");
                System.out.println("DEBUG: URL = /equipes/" + idEquipe + "/vaisseaux");

                // Récupérer directement tous nos vaisseaux avec getVaisseaux
                String vaisseauxJson = apiVaisseau.getVaisseaux(idEquipe);
                System.out.println("DEBUG: Réponse brute API:");
                System.out.println(vaisseauxJson);
                System.out.println("========================================\n");

                // Vérifier si c'est une erreur
                if (vaisseauxJson == null || vaisseauxJson.isEmpty()) {
                    System.err.println("ERREUR: Réponse vide de l'API");
                    currentVaisseaux = new JsonArray();
                    Platform.runLater(this::updateVaisseauxPanel);
                    return;
                }

                // Vérifier si c'est un message d'erreur JSON
                if (vaisseauxJson.contains("\"code\"") || vaisseauxJson.contains("\"error\"")) {
                    System.err.println("ERREUR: L'API a retourné une erreur");
                    currentVaisseaux = new JsonArray();
                    Platform.runLater(this::updateVaisseauxPanel);
                    return;
                }

                JsonArray vaisseaux = gson.fromJson(vaisseauxJson, JsonArray.class);

                if (vaisseaux != null && vaisseaux.size() > 0) {
                    System.out.println("✓ " + vaisseaux.size() + " vaisseaux récupérés depuis l'API");

                    // Filtrer uniquement les vaisseaux vivants avec une position
                    JsonArray vaisseauxVivants = new JsonArray();
                    for (int i = 0; i < vaisseaux.size(); i++) {
                        JsonObject v = vaisseaux.get(i).getAsJsonObject();

                        // Vérifier si le vaisseau a une position (= vivant)
                        boolean hasPosition = v.has("positionX") && v.has("positionY");
                        int pointDeVie = v.has("pointDeVie") ? v.get("pointDeVie").getAsInt() : 0;

                        System.out.println("\nVaisseau #" + (i+1) + ":");
                        System.out.println("  - Nom: " + (v.has("nom") ? v.get("nom").getAsString() : "N/A"));
                        System.out.println("  - ID: " + (v.has("idVaisseau") ? v.get("idVaisseau").getAsString() : "N/A"));
                        System.out.println("  - Position X: " + (v.has("positionX") ? v.get("positionX").getAsInt() : "N/A"));
                        System.out.println("  - Position Y: " + (v.has("positionY") ? v.get("positionY").getAsInt() : "N/A"));
                        System.out.println("  - Points de vie: " + pointDeVie);
                        System.out.println("  - Statut: " + (hasPosition && pointDeVie > 0 ? "VIVANT ✓" : "DÉTRUIT ✗"));

                        // N'ajouter que les vaisseaux vivants avec une position
                        if (hasPosition && pointDeVie > 0) {
                            vaisseauxVivants.add(v);
                        }
                    }

                    System.out.println("\n✓ " + vaisseauxVivants.size() + " vaisseaux vivants sur " + vaisseaux.size());
                    currentVaisseaux = vaisseauxVivants;
                } else {
                    System.err.println("ERREUR: Aucun vaisseau retourné ou réponse invalide");
                    currentVaisseaux = new JsonArray();
                }

                Platform.runLater(this::updateVaisseauxPanel);
            } catch (Exception e) {
                System.err.println("EXCEPTION lors du chargement des vaisseaux:");
                System.err.println("  Message: " + e.getMessage());
                System.err.println("  Type: " + e.getClass().getName());
                e.printStackTrace();
                currentVaisseaux = new JsonArray();
                Platform.runLater(this::updateVaisseauxPanel);
            }
        }).start();
    }

    private void updateVaisseauxPanel() {
        System.out.println("DEBUG: Mise à jour du panneau vaisseaux");

        // Retour à la liste (ne plus afficher les détails)
        isViewingDetails = false;

        // Garder seulement le titre
        vaisseauxList.getChildren().clear();

        Label title = new Label("VOS VAISSEAUX");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        vaisseauxList.getChildren().add(title);

        if (currentVaisseaux == null || currentVaisseaux.size() == 0) {
            Label noVaisseau = new Label("Chargement...");
            noVaisseau.setStyle("-fx-text-fill: gray;");
            vaisseauxList.getChildren().add(noVaisseau);
            return;
        }

        System.out.println("DEBUG: Affichage de " + currentVaisseaux.size() + " vaisseaux");

        for (int i = 0; i < currentVaisseaux.size(); i++) {
            JsonObject v = currentVaisseaux.get(i).getAsJsonObject();

            String nom = v.has("nom") ? v.get("nom").getAsString() : "Vaisseau";

            // Récupérer les coordonnées (positionX/Y ou coord_x/y)
            int posX = v.has("positionX") ? v.get("positionX").getAsInt() :
                       (v.has("coord_x") ? v.get("coord_x").getAsInt() : 0);
            int posY = v.has("positionY") ? v.get("positionY").getAsInt() :
                       (v.has("coord_y") ? v.get("coord_y").getAsInt() : 0);

            String idVaisseau = v.get("idVaisseau").getAsString();

            System.out.println("DEBUG: Vaisseau " + nom + " à (" + posX + ", " + posY + ")");

            // Créer le panel pour ce vaisseau
            VBox vaisseauPanel = createVaisseauPanel(nom, posX, posY, idVaisseau);
            vaisseauxList.getChildren().add(vaisseauPanel);
        }
    }

    private VBox createVaisseauPanel(String nom, int posX, int posY, String idVaisseau) {
        VBox panel = new VBox(5);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color: #3c3c3c; -fx-background-radius: 5;");

        // Nom et position
        Label nameLabel = new Label(nom);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        Label posLabel = new Label("Position: (" + posX + ", " + posY + ")");
        posLabel.setStyle("-fx-text-fill: lightgray; -fx-font-size: 11px;");

        // Boutons d'action
        HBox actionsBox = new HBox(5);

        Button btnMove = new Button("Déplacer");
        btnMove.setOnAction(e -> showActionDialog("Déplacer", idVaisseau, posX, posY));

        Button btnHarvest = new Button("Récolter");
        btnHarvest.setOnAction(e -> showActionDialog("Récolter", idVaisseau, posX, posY));

        Button btnAttack = new Button("Attaquer");
        btnAttack.setOnAction(e -> showActionDialog("Attaquer", idVaisseau, posX, posY));

        Button btnDeposit = new Button("Déposer");
        btnDeposit.setOnAction(e -> showActionDialog("Déposer", idVaisseau, posX, posY));

        Button btnConquer = new Button("Conquérir");
        btnConquer.setOnAction(e -> showActionDialog("Conquérir", idVaisseau, posX, posY));

        Button btnRepair = new Button("Réparer");
        btnRepair.setOnAction(e -> executeRepairAction(idVaisseau));

        actionsBox.getChildren().addAll(btnMove, btnHarvest);

        HBox actionsBox2 = new HBox(5);
        actionsBox2.getChildren().addAll(btnAttack, btnDeposit);

        HBox actionsBox3 = new HBox(5);
        actionsBox3.getChildren().addAll(btnConquer, btnRepair);

        panel.getChildren().addAll(nameLabel, posLabel, actionsBox, actionsBox2, actionsBox3);

        return panel;
    }

    private void showActionDialog(String action, String idVaisseau, int currentX, int currentY) {
        System.out.println("DEBUG: Action " + action + " depuis position (" + currentX + ", " + currentY + ")");

        // Dialogue pour choisir la direction (déplacement d'1 case seulement)
        ChoiceDialog<String> dirDialog = new ChoiceDialog<>("Nord",
            "Nord", "Sud", "Est", "Ouest",
            "Nord-Est", "Nord-Ouest", "Sud-Est", "Sud-Ouest");
        dirDialog.setTitle(action);
        dirDialog.setHeaderText(action + " le vaisseau (1 case)");
        dirDialog.setContentText("Direction:");

        dirDialog.showAndWait().ifPresent(direction -> {
            // Calculer les coordonnées cibles (toujours 1 case)
            int targetX = currentX;
            int targetY = currentY;

            switch (direction) {
                case "Nord":
                    targetY -= 1;
                    break;
                case "Sud":
                    targetY += 1;
                    break;
                case "Est":
                    targetX += 1;
                    break;
                case "Ouest":
                    targetX -= 1;
                    break;
                case "Nord-Est":
                    targetX += 1;
                    targetY -= 1;
                    break;
                case "Nord-Ouest":
                    targetX -= 1;
                    targetY -= 1;
                    break;
                case "Sud-Est":
                    targetX += 1;
                    targetY += 1;
                    break;
                case "Sud-Ouest":
                    targetX -= 1;
                    targetY += 1;
                    break;
            }

            System.out.println("DEBUG: Cible calculée: (" + targetX + ", " + targetY + ")");

            // Exécuter l'action
            executeAction(action, idVaisseau, targetX, targetY);
        });
    }

    private void executeAction(String action, String idVaisseau, int x, int y) {
        new Thread(() -> {
            try {
                switch (action) {
                    case "Déplacer":
                        apiVaisseau.deplacer(idEquipe, idVaisseau, x, y);
                        break;
                    case "Récolter":
                        apiVaisseau.recolter(idEquipe, idVaisseau, x, y);
                        break;
                    case "Attaquer":
                        apiVaisseau.attaquer(idEquipe, idVaisseau, x, y);
                        break;
                    case "Déposer":
                        apiVaisseau.deposer(idEquipe, idVaisseau, x, y);
                        break;
                    case "Conquérir":
                        apiVaisseau.conquerir(idEquipe, idVaisseau, x, y);
                        break;
                }

                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Succès");
                    alert.setContentText(action + " effectué !");
                    alert.showAndWait();

                    // Recharger les vaisseaux
                    loadVaisseauxPanel();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erreur");
                    alert.setContentText("Erreur: " + e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }

    private void executeRepairAction(String idVaisseau) {
        new Thread(() -> {
            try {
                apiVaisseau.reparer(idEquipe, idVaisseau);

                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Succès");
                    alert.setContentText("Réparation effectuée !");
                    alert.showAndWait();

                    // Recharger les vaisseaux
                    loadVaisseauxPanel();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erreur");
                    alert.setContentText("Erreur: " + e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }

    private void handleCellClick(int mapX, int mapY) {
        System.out.println("\n========================================");
        System.out.println("DEBUG CLIC: Position (" + mapX + ", " + mapY + ")");

        if (currentMapCases == null) {
            System.out.println("ERREUR: currentMapCases = null");
            System.out.println("========================================\n");
            return;
        }

        System.out.println("Total cases disponibles: " + currentMapCases.size());

        // Chercher la case cliquée dans les données
        for (int i = 0; i < currentMapCases.size(); i++) {
            JsonObject caseObj = currentMapCases.get(i).getAsJsonObject();
            int cx = caseObj.get("coord_x").getAsInt();
            int cy = caseObj.get("coord_y").getAsInt();

            if (cx == mapX && cy == mapY) {
                System.out.println("✓ Case trouvée!");
                System.out.println("  - Planète présente: " + caseObj.has("planete"));
                System.out.println("  - Vaisseau présent: " + caseObj.has("vaisseau"));

                // Vérifier d'abord s'il y a un vaisseau (priorité car ils sont au-dessus)
                if (caseObj.has("vaisseau") && caseObj.get("vaisseau").isJsonObject()) {
                    JsonObject vaisseau = caseObj.get("vaisseau").getAsJsonObject();
                    if (vaisseau.has("idVaisseau")) {
                        System.out.println("→ Affichage des infos du vaisseau");
                        System.out.println("========================================\n");
                        displayVaisseauInfo(vaisseau, caseObj, mapX, mapY);
                        return;
                    }
                }

                // Sinon, vérifier s'il y a une planète sur cette case
                if (caseObj.has("planete") && caseObj.get("planete").isJsonObject()) {
                    JsonObject planete = caseObj.get("planete").getAsJsonObject();
                    int pdv = planete.has("pointDeVie") ? planete.get("pointDeVie").getAsInt() : 0;
                    System.out.println("  - Points de vie planète: " + pdv);
                    if (pdv > 0) {
                        System.out.println("→ Affichage des infos de la planète");
                        System.out.println("========================================\n");
                        displayPlaneteInfo(planete, caseObj, mapX, mapY);
                        return;
                    }
                }

                System.out.println("! Case vide (pas de planète/vaisseau actif)");
                System.out.println("========================================\n");
                return;
            }
        }

        System.out.println("✗ Case non trouvée (coordonnées ne correspondent pas)");
        System.out.println("========================================\n");
    }

    private void displayPlaneteInfo(JsonObject planete, JsonObject caseObj, int x, int y) {
        Platform.runLater(() -> {
            // Marquer qu'on affiche des détails
            isViewingDetails = true;

            // Effacer le contenu actuel du panneau
            vaisseauxList.getChildren().clear();

            // Titre
            Label title = new Label("PLANÈTE SÉLECTIONNÉE");
            title.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
            vaisseauxList.getChildren().add(title);

            // Créer un panneau pour afficher les infos de la planète
            VBox planetePanel = new VBox(5);
            planetePanel.setPadding(new Insets(10));
            planetePanel.setStyle("-fx-background-color: #3a3a3a; -fx-border-color: #5a5a5a; -fx-border-width: 1;");

            // Position
            Label posLabel = new Label("Position: (" + x + ", " + y + ")");
            posLabel.setStyle("-fx-text-fill: white;");

            // Identifiant
            String id = planete.has("identifiant") ? planete.get("identifiant").getAsString() : "N/A";
            Label idLabel = new Label("ID: " + id);
            idLabel.setStyle("-fx-text-fill: white; -fx-font-size: 10px;");
            idLabel.setWrapText(true);

            // Type de planète et biome
            String typePlanete = "Inconnu";
            String biome = "Inconnu";
            boolean isAsteroidField = false;

            if (planete.has("modelePlanete") && planete.get("modelePlanete").isJsonObject()) {
                JsonObject modele = planete.get("modelePlanete").getAsJsonObject();

                // Type de planète
                if (modele.has("typePlanete")) {
                    typePlanete = modele.get("typePlanete").getAsString();
                    // Formater le type (CHAMPS_ASTEROIDES -> Champs d'astéroïdes)
                    if (typePlanete.equals("CHAMPS_ASTEROIDES")) {
                        isAsteroidField = true;
                        typePlanete = "Champs d'astéroïdes";
                    } else {
                        typePlanete = typePlanete.replace("_", " ").toLowerCase();
                        typePlanete = typePlanete.substring(0, 1).toUpperCase() + typePlanete.substring(1);
                    }
                }

                // Biome
                if (modele.has("biome")) {
                    biome = modele.get("biome").getAsString();
                    // Formater le biome (GLACE -> Glace, etc.)
                    biome = biome.toLowerCase();
                    biome = biome.substring(0, 1).toUpperCase() + biome.substring(1);
                }
            }

            Label typeLabel = new Label("Type: " + typePlanete);
            typeLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

            Label biomeLabel = new Label("Biome: " + biome);
            biomeLabel.setStyle("-fx-text-fill: lightgray;");

            // Points de vie
            int pdv = planete.has("pointDeVie") ? planete.get("pointDeVie").getAsInt() : 0;
            Label pdvLabel = new Label("Points de vie: " + pdv);
            pdvLabel.setStyle("-fx-text-fill: white;");

            // Propriétaire
            String proprietaire = "Neutre";
            Color planeteColor = Color.GRAY;
            if (isAsteroidField) {
                planeteColor = Color.WHITE;
            } else if (caseObj.has("proprietaire") && caseObj.get("proprietaire").isJsonObject()) {
                JsonObject prop = caseObj.get("proprietaire").getAsJsonObject();
                if (prop.has("nom")) {
                    proprietaire = prop.get("nom").getAsString();
                    planeteColor = getEquipeColor(proprietaire, proprietaire);
                }
            }
            Label propLabel = new Label("Propriétaire: " + proprietaire);
            propLabel.setStyle("-fx-text-fill: white;");

            // Minerais disponibles à récolter
            int mineraiDispo = planete.has("mineraiDisponible") ? planete.get("mineraiDisponible").getAsInt() : 0;

            Label mineraiTitle = new Label("\nMinerais disponibles:");
            mineraiTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

            Label mineraiLabel = new Label(mineraiDispo + " unités");
            mineraiLabel.setStyle("-fx-text-fill: " + (mineraiDispo > 0 ? "#FFD700" : "gray") + "; -fx-font-size: 14px; -fx-font-weight: bold;");

            VBox ressourcesBox = new VBox(3);
            ressourcesBox.getChildren().add(mineraiLabel);

            if (mineraiDispo == 0) {
                Label emptyLabel = new Label("  Planète épuisée");
                emptyLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 11px;");
                ressourcesBox.getChildren().add(emptyLabel);
            }

            // Ajouter tous les éléments au panneau
            planetePanel.getChildren().addAll(posLabel, idLabel, typeLabel, biomeLabel, pdvLabel, propLabel,
                                              mineraiTitle, ressourcesBox);

            // Bouton pour revenir à la liste des vaisseaux
            Button backButton = new Button("← Retour aux vaisseaux");
            backButton.setOnAction(e -> loadVaisseauxPanel());
            backButton.setStyle("-fx-background-color: #4a4a4a; -fx-text-fill: white;");

            vaisseauxList.getChildren().addAll(planetePanel, backButton);
        });
    }

    private void displayVaisseauInfo(JsonObject vaisseau, JsonObject caseObj, int x, int y) {
        Platform.runLater(() -> {
            // Marquer qu'on affiche des détails
            isViewingDetails = true;

            // Effacer le contenu actuel du panneau
            vaisseauxList.getChildren().clear();

            // Titre
            Label title = new Label("VAISSEAU SÉLECTIONNÉ");
            title.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
            vaisseauxList.getChildren().add(title);

            // Créer un panneau pour afficher les infos du vaisseau
            VBox vaisseauPanel = new VBox(5);
            vaisseauPanel.setPadding(new Insets(10));
            vaisseauPanel.setStyle("-fx-background-color: #3a3a3a; -fx-border-color: #5a5a5a; -fx-border-width: 1;");

            // Nom
            String nom = vaisseau.has("nom") ? vaisseau.get("nom").getAsString() : "Vaisseau";
            Label nomLabel = new Label(nom);
            nomLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

            // Position
            Label posLabel = new Label("Position: (" + x + ", " + y + ")");
            posLabel.setStyle("-fx-text-fill: white;");

            // Identifiant
            String id = vaisseau.has("idVaisseau") ? vaisseau.get("idVaisseau").getAsString() : "N/A";
            Label idLabel = new Label("ID: " + id);
            idLabel.setStyle("-fx-text-fill: white; -fx-font-size: 10px;");
            idLabel.setWrapText(true);

            // Points de vie
            int pdv = vaisseau.has("pointDeVie") ? vaisseau.get("pointDeVie").getAsInt() : 0;
            Label pdvLabel = new Label("Points de vie: " + pdv);
            pdvLabel.setStyle("-fx-text-fill: white;");

            // Vitesse
            int vitesse = vaisseau.has("vitesse") ? vaisseau.get("vitesse").getAsInt() : 0;
            Label vitesseLabel = new Label("Vitesse: " + vitesse);
            vitesseLabel.setStyle("-fx-text-fill: lightgray;");

            // Minerai transporté
            int mineraiTransporte = vaisseau.has("mineraiTransporte") ? vaisseau.get("mineraiTransporte").getAsInt() : 0;
            Label mineraiLabel = new Label("Minerai transporté: " + mineraiTransporte);
            mineraiLabel.setStyle("-fx-text-fill: " + (mineraiTransporte > 0 ? "#FFD700" : "lightgray") + ";");

            // Propriétaire
            String proprietaire = "Inconnu";
            String proprietaireId = null;
            Color vaisseauColor = Color.GRAY;
            boolean isAlly = false;

            // Le propriétaire dans le vaisseau est un ID (string)
            if (vaisseau.has("proprietaire")) {
                proprietaireId = vaisseau.get("proprietaire").getAsString();

                // Vérifier si c'est notre équipe
                if (proprietaireId.equals(idEquipe)) {
                    proprietaire = "Les Pointeurs Fous";
                    vaisseauColor = Color.GREEN;
                    isAlly = true;
                } else {
                    // Chercher dans le cache des noms d'équipes (construit depuis la carte)
                    if (equipeNames.containsKey(proprietaireId)) {
                        proprietaire = equipeNames.get(proprietaireId);
                    } else {
                        // Nom d'équipe inconnu (pas encore rencontré sur la carte)
                        proprietaire = "Équipe ennemie";
                    }
                    vaisseauColor = Color.RED;
                }
            }
            Label propLabel = new Label("Propriétaire: " + proprietaire + (isAlly ? " (VOUS)" : ""));
            propLabel.setStyle("-fx-text-fill: white;");

            // Modules
            Label modulesTitle = new Label("\nModules:");
            modulesTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

            VBox modulesBox = new VBox(3);
            if (vaisseau.has("modules") && vaisseau.get("modules").isJsonArray()) {
                JsonArray modules = vaisseau.get("modules").getAsJsonArray();
                if (modules.size() > 0) {
                    for (int i = 0; i < modules.size(); i++) {
                        JsonObject module = modules.get(i).getAsJsonObject();
                        String moduleNom = module.has("nom") ? module.get("nom").getAsString() : "Module";
                        Label modLabel = new Label("  • " + moduleNom);
                        modLabel.setStyle("-fx-text-fill: lightgray;");
                        modulesBox.getChildren().add(modLabel);
                    }
                } else {
                    Label noModLabel = new Label("  Aucun module");
                    noModLabel.setStyle("-fx-text-fill: gray;");
                    modulesBox.getChildren().add(noModLabel);
                }
            }

            // Ressources
            Label ressourcesTitle = new Label("\nRessources:");
            ressourcesTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

            VBox ressourcesBox = new VBox(3);
            if (vaisseau.has("ressources") && vaisseau.get("ressources").isJsonArray()) {
                JsonArray ressources = vaisseau.get("ressources").getAsJsonArray();
                if (ressources.size() > 0) {
                    for (int i = 0; i < ressources.size(); i++) {
                        JsonObject res = ressources.get(i).getAsJsonObject();
                        String type = res.has("type") ? res.get("type").getAsString() : "?";
                        int qte = res.has("quantite") ? res.get("quantite").getAsInt() : 0;
                        Label resLabel = new Label("  • " + type + ": " + qte);
                        resLabel.setStyle("-fx-text-fill: lightgray;");
                        ressourcesBox.getChildren().add(resLabel);
                    }
                } else {
                    Label noResLabel = new Label("  Aucune ressource");
                    noResLabel.setStyle("-fx-text-fill: gray;");
                    ressourcesBox.getChildren().add(noResLabel);
                }
            }

            // Ajouter tous les éléments au panneau
            vaisseauPanel.getChildren().addAll(nomLabel, posLabel, idLabel, pdvLabel,
                                              vitesseLabel, mineraiLabel, propLabel, modulesTitle, modulesBox,
                                              ressourcesTitle, ressourcesBox);

            // Boutons d'action (seulement pour nos vaisseaux)
            if (isAlly) {
                Label actionsTitle = new Label("\nActions:");
                actionsTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                vaisseauPanel.getChildren().add(actionsTitle);

                HBox actionsBox1 = new HBox(5);
                Button btnMove = new Button("Déplacer");
                btnMove.setOnAction(e -> showActionDialog("Déplacer", id, x, y));
                btnMove.setStyle("-fx-background-color: #4a4a4a; -fx-text-fill: white;");

                Button btnHarvest = new Button("Récolter");
                btnHarvest.setOnAction(e -> showActionDialog("Récolter", id, x, y));
                btnHarvest.setStyle("-fx-background-color: #4a4a4a; -fx-text-fill: white;");

                actionsBox1.getChildren().addAll(btnMove, btnHarvest);

                HBox actionsBox2 = new HBox(5);
                Button btnAttack = new Button("Attaquer");
                btnAttack.setOnAction(e -> showActionDialog("Attaquer", id, x, y));
                btnAttack.setStyle("-fx-background-color: #4a4a4a; -fx-text-fill: white;");

                Button btnDeposit = new Button("Déposer");
                btnDeposit.setOnAction(e -> showActionDialog("Déposer", id, x, y));
                btnDeposit.setStyle("-fx-background-color: #4a4a4a; -fx-text-fill: white;");

                actionsBox2.getChildren().addAll(btnAttack, btnDeposit);

                HBox actionsBox3 = new HBox(5);
                Button btnConquer = new Button("Conquérir");
                btnConquer.setOnAction(e -> showActionDialog("Conquérir", id, x, y));
                btnConquer.setStyle("-fx-background-color: #4a4a4a; -fx-text-fill: white;");

                Button btnRepair = new Button("Réparer");
                btnRepair.setOnAction(e -> executeRepairAction(id));
                btnRepair.setStyle("-fx-background-color: #4a4a4a; -fx-text-fill: white;");

                actionsBox3.getChildren().addAll(btnConquer, btnRepair);

                vaisseauPanel.getChildren().addAll(actionsBox1, actionsBox2, actionsBox3);
            }

            // Bouton pour revenir à la liste des vaisseaux
            Button backButton = new Button("← Retour à la liste");
            backButton.setOnAction(e -> loadVaisseauxPanel());
            backButton.setStyle("-fx-background-color: #4a4a4a; -fx-text-fill: white;");

            vaisseauxList.getChildren().addAll(vaisseauPanel, backButton);
        });
    }

    private String toRGBCode(Color color) {
        return String.format("#%02X%02X%02X",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
    }

    private void loadVaisseauImages() {
        try {
            // Charger quelques images de vaisseaux
            String basePath = "assets 2d/vaisseaux_2D/";
            vaisseauImages.put("chasseur_leger_1", new Image(new FileInputStream(basePath + "chasseur_leger_1.png")));
            vaisseauImages.put("cargo_leger_1", new Image(new FileInputStream(basePath + "cargo_leger_1.png")));
            vaisseauImages.put("sonde_1", new Image(new FileInputStream(basePath + "sonde_1.png")));
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des images: " + e.getMessage());
        }
    }

    private void drawMap() {
        try {
            // Recharger les assets alliés à chaque rafraîchissement
            loadAlliedAssets();

            // Fond noir
            gc.setFill(Color.BLACK);
            gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

            // Récupérer les données de la carte en plusieurs requêtes (limite 18x18)
            JsonArray allCases = new JsonArray();

            // Découper la carte en tuiles de 18x18
            for (int tileY = yMin; tileY <= yMax; tileY += (MAX_RANGE + 1)) {
                for (int tileX = xMin; tileX <= xMax; tileX += (MAX_RANGE + 1)) {
                    int currentXMin = tileX;
                    int currentXMax = Math.min(tileX + MAX_RANGE, xMax);
                    int currentYMin = tileY;
                    int currentYMax = Math.min(tileY + MAX_RANGE, yMax);

                    // Requête pour cette tuile
                    String tileJson = apiMap.getMap(currentXMin, currentXMax, currentYMin, currentYMax);
                    JsonArray tileCases = gson.fromJson(tileJson, JsonArray.class);

                    if (tileCases != null) {
                        // Ajouter toutes les cases de cette tuile
                        for (int i = 0; i < tileCases.size(); i++) {
                            allCases.add(tileCases.get(i));
                        }
                    }
                }
            }

            JsonArray cases = allCases;
            if (cases == null || cases.size() == 0) return;

            // Stocker les cases pour pouvoir les utiliser lors des clics
            this.currentMapCases = cases;

            System.out.println("Total cases chargées: " + cases.size());

            // Dessiner les coordonnées sur les axes
            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font(10));

            // Coordonnées X (en haut)
            for (int x = 0; x <= xMax - xMin; x++) {
                if ((xMin + x) % 5 == 0) { // Afficher tous les 5
                    String label = String.valueOf(xMin + x);
                    gc.fillText(label, x * CELL_SIZE + CELL_SIZE/2 - 5, 10);
                }
            }

            // Coordonnées Y (à gauche)
            for (int y = 0; y <= yMax - yMin; y++) {
                if ((yMin + y) % 5 == 0) { // Afficher tous les 5
                    String label = String.valueOf(yMin + y);
                    gc.fillText(label, 5, y * CELL_SIZE + CELL_SIZE/2 + 5);
                }
            }

            // Dessiner la grille
            gc.setStroke(Color.GRAY);
            gc.setLineWidth(0.5);
            for (int x = 0; x <= xMax - xMin; x++) {
                gc.strokeLine(x * CELL_SIZE, 0, x * CELL_SIZE, canvas.getHeight());
            }
            for (int y = 0; y <= yMax - yMin; y++) {
                gc.strokeLine(0, y * CELL_SIZE, canvas.getWidth(), y * CELL_SIZE);
            }

            // Dessiner chaque case
            for (int i = 0; i < cases.size(); i++) {
                JsonObject caseObj = cases.get(i).getAsJsonObject();
                int cx = caseObj.get("coord_x").getAsInt();
                int cy = caseObj.get("coord_y").getAsInt();

                if (cx >= xMin && cx <= xMax && cy >= yMin && cy <= yMax) {
                    int x = cx - xMin;
                    int y = cy - yMin;

                    // Dessiner la planète
                    if (caseObj.has("planete") && caseObj.get("planete").isJsonObject()) {
                        JsonObject planete = caseObj.get("planete").getAsJsonObject();
                        if (planete.has("pointDeVie") && planete.get("pointDeVie").getAsInt() > 0) {
                            drawPlanete(x, y, planete, caseObj);
                        }
                    }

                    // Dessiner le vaisseau (par-dessus la planète)
                    if (caseObj.has("vaisseau") && caseObj.get("vaisseau").isJsonObject()) {
                        JsonObject vaisseau = caseObj.get("vaisseau").getAsJsonObject();
                        if (vaisseau.has("idVaisseau")) {
                            drawVaisseau(x, y, vaisseau, caseObj);
                        }
                    }
                }
            }

            // Afficher la légende après le premier chargement
            if (firstLoad && !equipeColors.isEmpty()) {
                printLegend();
                firstLoad = false;
            }

        } catch (Exception e) {
            System.err.println("Erreur lors du dessin de la carte: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void drawPlanete(int x, int y, JsonObject planete, JsonObject caseObj) {
        double centerX = x * CELL_SIZE + CELL_SIZE / 2.0;
        double centerY = y * CELL_SIZE + CELL_SIZE / 2.0;
        double radius = CELL_SIZE / 3.0;

        String planeteId = planete.get("identifiant").getAsString();
        Color planeteColor = Color.GRAY; // Par défaut neutre
        String equipeName = null;

        // Vérifier le type de planète (champ d'astéroïdes = blanc)
        if (planete.has("modelePlanete") && planete.get("modelePlanete").isJsonObject()) {
            JsonObject modele = planete.get("modelePlanete").getAsJsonObject();
            if (modele.has("typePlanete") && modele.get("typePlanete").getAsString().equals("CHAMPS_ASTEROIDES")) {
                planeteColor = Color.WHITE;
                gc.setFill(planeteColor);
                gc.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
                // Bordure grise pour les distinguer
                gc.setStroke(Color.DARKGRAY);
                gc.setLineWidth(1);
                gc.strokeOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
                return;
            }
        }

        // Chercher le propriétaire de cette planète
        if (caseObj.has("proprietaire") && caseObj.get("proprietaire").isJsonObject()) {
            JsonObject proprietaire = caseObj.get("proprietaire").getAsJsonObject();

            // Si le proprietaire a un nom, cette case/planète lui appartient
            if (proprietaire.has("nom")) {
                equipeName = proprietaire.get("nom").getAsString();

                // Vérifier si c'est notre équipe (par le nom)
                if (equipeName.toLowerCase().contains("pointeur") ||
                    equipeName.toLowerCase().contains("fou")) {
                    planeteColor = Color.BLUE;
                    alliedPlanetesIds.add(planeteId);
                } else {
                    // Autre équipe
                    planeteColor = getEquipeColor(equipeName, equipeName);
                }
            }
        }

        gc.setFill(planeteColor);
        gc.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

        // Bordure blanche
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        gc.strokeOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
    }

    private void drawVaisseau(int x, int y, JsonObject vaisseau, JsonObject caseObj) {
        double posX = x * CELL_SIZE;
        double posY = y * CELL_SIZE;
        double centerX = posX + CELL_SIZE / 2.0;
        double centerY = posY + CELL_SIZE / 2.0;

        // Déterminer si le vaisseau est allié ou ennemi en regardant le propriétaire
        boolean isAlly = false;
        String vaisseauId = vaisseau.get("idVaisseau").getAsString();

        // Vérifier si déjà connu comme allié
        if (alliedVaisseauxIds.contains(vaisseauId)) {
            isAlly = true;
        } else {
            // Chercher dans le proprietaire de la case
            if (caseObj.has("proprietaire") && caseObj.get("proprietaire").isJsonObject()) {
                JsonObject proprietaire = caseObj.get("proprietaire").getAsJsonObject();
                if (proprietaire.has("nom")) {
                    String equipeName = proprietaire.get("nom").getAsString();
                    if (equipeName.toLowerCase().contains("pointeur") ||
                        equipeName.toLowerCase().contains("fou")) {
                        isAlly = true;
                        alliedVaisseauxIds.add(vaisseauId);
                    }
                }
            }
        }

        // Couleur: Vert pour allié, Rouge pour ennemi
        Color vaisseauColor = isAlly ? Color.GREEN : Color.RED;

        gc.setFill(vaisseauColor);

        // Dessiner un triangle pointant vers le haut
        double size = CELL_SIZE / 3.0;
        double[] xPoints = {
            centerX,                    // Sommet
            centerX - size/2,          // Bas gauche
            centerX + size/2           // Bas droit
        };
        double[] yPoints = {
            centerY - size/2,          // Sommet
            centerY + size/2,          // Bas gauche
            centerY + size/2           // Bas droit
        };
        gc.fillPolygon(xPoints, yPoints, 3);

        // Bordure blanche
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        gc.strokePolygon(xPoints, yPoints, 3);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
