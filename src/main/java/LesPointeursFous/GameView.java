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
import LesPointeursFous.services.ApiModule;
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
    private ApiModule apiModule;
    private String idEquipe;
    private Label cooldownLabel;
    private javafx.animation.Timeline cooldownTimer;
    private String currentVaisseauId;
    private Map<String, java.time.LocalTime> shipCooldowns = new HashMap<>();

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

    // Zone à afficher - 40x40 pour de meilleures performances
    private int xMin = 0;
    private int xMax = 58;
    private int yMin = 0;
    private int yMax = 58;
    private static final int MAX_RANGE = 17; // Limite API : 18x18 (0-17)

    @Override
    public void start(Stage primaryStage) {
        // Initialiser l'API
        Dotenv dotenv = Dotenv.configure().load();
        this.apiClient = new ApiClient(dotenv.get("API_URL"), dotenv.get("API_KEY"));
        apiMap = new ApiMap(apiClient);
        apiVaisseau = new ApiVaisseau(apiClient);
        apiModule = new ApiModule(apiClient);
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

        // Créer le panneau de vaisseaux
        vaisseauxList = new VBox(10);
        vaisseauxList.setPadding(new Insets(10));
        vaisseauxList.setStyle("-fx-background-color: #2b2b2b;");
        vaisseauxList.setPrefWidth(300);

        Label title = new Label("VOS VAISSEAUX");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        vaisseauxList.getChildren().add(title);

        // Panneau de vaisseaux avec scroll
        ScrollPane scrollPane = new ScrollPane(vaisseauxList);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #2b2b2b;");

        // Mettre le canvas dans un ScrollPane pour permettre le défilement
        ScrollPane canvasScroll = new ScrollPane(canvas);
        canvasScroll.setStyle("-fx-background: #1a1a1a;");
        HBox.setHgrow(canvasScroll, javafx.scene.layout.Priority.ALWAYS);

        // Ajouter un gestionnaire de clic sur le canvas
        canvas.setOnMouseClicked(event -> {
            int cellX = (int) (event.getX() / CELL_SIZE);
            int cellY = (int) (event.getY() / CELL_SIZE);
            int mapX = xMin + cellX;
            int mapY = yMin + cellY;

            handleCellClick(mapX, mapY);
        });

        // Layout principal
        HBox root = new HBox();
        root.getChildren().addAll(canvasScroll, scrollPane);

        Scene scene = new Scene(root);

        primaryStage.setTitle("24h du Code - Carte du jeu");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);  // Maximiser la fenêtre
        primaryStage.show();

        // Dessiner la carte
        drawMap();

        // Afficher le panneau vaisseaux (avec le vaisseau test)
        updateVaisseauxPanel();

        // Charger les vaisseaux réels de l'API
        loadVaisseauxPanel();

        // Rafraîchissement différencié: liste vaisseau 2s, carte 5s
        new Thread(() -> {
            final long[] lastVaisseauxRefresh = {System.currentTimeMillis()};
            final long[] lastMapRefresh = {System.currentTimeMillis()};

            while (true) {
                try {
                    Thread.sleep(500); // Check every 500ms for precision

                    Platform.runLater(() -> {
                        long currentTime = System.currentTimeMillis();

                        // Refresh map every 5 seconds
                        if (currentTime - lastMapRefresh[0] >= 5000) {
                            drawMap();
                            lastMapRefresh[0] = currentTime;
                        }

                        // Refresh vaisseau list every 2 seconds (if not viewing details)
                        if (currentTime - lastVaisseauxRefresh[0] >= 2000) {
                            if (!isViewingDetails) {
                                loadVaisseauxPanel();
                            }
                            lastVaisseauxRefresh[0] = currentTime;
                        }
                    });

                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    private Color getEquipeColor(String equipeName, String equipeId) {
        if (equipeId == null) {
            return Color.GRAY;
        }

        // Notre équipe est toujours BLEU FONCÉ
        if (equipeId.equals(idEquipe)) {
            if (!equipeColors.containsKey(equipeId)) {
                equipeColors.put(equipeId, Color.DARKBLUE);
                equipeNames.put(equipeId, equipeName != null ? equipeName : "Notre équipe");
                System.out.println("Notre équipe: " + equipeName + " (VOUS) - Couleur: BLEU FONCÉ");
            }
            return Color.DARKBLUE;
        }

        // Si l'équipe n'a pas encore de couleur, lui en assigner une depuis la palette
        if (!equipeColors.containsKey(equipeId)) {
            Color color = colorPalette[colorIndex % colorPalette.length];
            colorIndex++;
            equipeColors.put(equipeId, color);
            equipeNames.put(equipeId, equipeName != null ? equipeName : "Équipe " + colorIndex);

            // Afficher la nouvelle équipe découverte
            System.out.println("Nouvelle équipe: " + equipeName + " - Couleur: " + color);
        }

        return equipeColors.get(equipeId);
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
        if (color == Color.DARKBLUE) return "BLEU FONCÉ";
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

            // On définit notre équipe en bleu foncé
            equipeColors.put(idEquipe, Color.DARKBLUE);
            equipeNames.put(idEquipe, "Les Pointeurs Fous");

        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des assets alliés: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadVaisseauxPanel() {
        new Thread(() -> {
            try {
                // Récupérer directement tous nos vaisseaux avec getVaisseaux
                String vaisseauxJson = apiVaisseau.getVaisseaux(idEquipe);

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
                    // Filtrer uniquement les vaisseaux vivants avec une position
                    JsonArray vaisseauxVivants = new JsonArray();
                    for (int i = 0; i < vaisseaux.size(); i++) {
                        JsonObject v = vaisseaux.get(i).getAsJsonObject();

                        // Vérifier si le vaisseau a une position (= vivant)
                        boolean hasPosition = v.has("positionX") && v.has("positionY");
                        int pointDeVie = v.has("pointDeVie") ? v.get("pointDeVie").getAsInt() : 0;

                        // N'ajouter que les vaisseaux vivants avec une position
                        if (hasPosition && pointDeVie > 0) {
                            vaisseauxVivants.add(v);
                        }
                    }

                    currentVaisseaux = vaisseauxVivants;
                } else {
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


        for (int i = 0; i < currentVaisseaux.size(); i++) {
            JsonObject v = currentVaisseaux.get(i).getAsJsonObject();

            String nom = v.has("nom") ? v.get("nom").getAsString() : "Vaisseau";

            // Récupérer les coordonnées (positionX/Y ou coord_x/y)
            int posX = v.has("positionX") ? v.get("positionX").getAsInt() :
                       (v.has("coord_x") ? v.get("coord_x").getAsInt() : 0);
            int posY = v.has("positionY") ? v.get("positionY").getAsInt() :
                       (v.has("coord_y") ? v.get("coord_y").getAsInt() : 0);

            String idVaisseau = v.get("idVaisseau").getAsString();


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

            System.out.println("=== ACTION: " + action + " ===");
            System.out.println("Position actuelle: (" + currentX + ", " + currentY + ")");
            System.out.println("Direction: " + direction);

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

            System.out.println("Position cible: (" + targetX + ", " + targetY + ")");

            // Exécuter l'action
            executeAction(action, idVaisseau, targetX, targetY);
        });
    }

    private void executeAction(String action, String idVaisseau, int x, int y) {
        new Thread(() -> {
            try {
                String result = null;
                switch (action) {
                    case "Déplacer":
                        result = apiVaisseau.deplacer(idEquipe, idVaisseau, x, y);
                        break;
                    case "Récolter":
                        result = apiVaisseau.recolter(idEquipe, idVaisseau, x, y);
                        break;
                    case "Attaquer":
                        result = apiVaisseau.attaquer(idEquipe, idVaisseau, x, y);
                        break;
                    case "Déposer":
                        result = apiVaisseau.deposer(idEquipe, idVaisseau, x, y);
                        break;
                    case "Conquérir":
                        result = apiVaisseau.conquerir(idEquipe, idVaisseau, x, y);
                        break;
                }

                // Vérifier si c'est un cooldown
                if (result != null && !result.isEmpty()) {
                    try {
                        JsonObject responseObj = gson.fromJson(result, JsonObject.class);

                        // Vérifier si le vaisseau est en cooldown
                        if (responseObj.has("message")) {
                            String message = responseObj.get("message").getAsString();

                            if (message.contains("Vaisseau indisponible") || message.contains("prochaine disponibilité")) {
                                // Extraire le temps de cooldown (format: HH:MM:SS)
                                String cooldownTime = message.substring(message.lastIndexOf(":") - 5);
                                startCooldownTimer(cooldownTime);
                                return;
                            }
                        }

                        // Vérifier si c'est une autre erreur
                        if (responseObj.has("code") && responseObj.get("code").getAsString().equals("-1")) {
                            String errorMessage = responseObj.has("message") ?
                                responseObj.get("message").getAsString() : "Erreur inconnue";
                            throw new Exception(errorMessage);
                        }

                        // Chercher le cooldown dans une action réussie
                        String cooldownInfo = "";
                        if (responseObj.has("cooldown")) {
                            int cooldown = responseObj.get("cooldown").getAsInt();
                            cooldownInfo = "\n\nCooldown: " + cooldown + " secondes";
                        } else if (responseObj.has("tempsRestant")) {
                            int tempsRestant = responseObj.get("tempsRestant").getAsInt();
                            cooldownInfo = "\n\nTemps restant: " + tempsRestant + " secondes";
                        }

                        String finalCooldownInfo = cooldownInfo;
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Succès");
                            alert.setContentText(action + " effectué !" + finalCooldownInfo);
                            alert.showAndWait();
                            loadVaisseauxPanel();
                        });

                        // Faire une requête de suivi pour obtenir le cooldown
                        fetchCooldownAfterSuccess(action, idVaisseau, x, y);

                    } catch (Exception e) {
                        throw e;
                    }
                } else {
                    // Réponse vide (204) - action réussie
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Succès");
                        alert.setContentText(action + " effectué !");
                        alert.showAndWait();
                        loadVaisseauxPanel();
                    });

                    // Faire une requête de suivi pour obtenir le cooldown
                    fetchCooldownAfterSuccess(action, idVaisseau, x, y);
                }
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

    private void fetchCooldownAfterSuccess(String action, String idVaisseau, int x, int y) {
        // Faire une requête de suivi (qui échouera avec 423) pour obtenir le temps de cooldown
        new Thread(() -> {
            try {
                // Attendre un court instant pour être sûr que le cooldown est actif
                Thread.sleep(100);

                String followUpResult = null;
                switch (action) {
                    case "Déplacer":
                        followUpResult = apiVaisseau.deplacer(idEquipe, idVaisseau, x, y);
                        break;
                    case "Récolter":
                        followUpResult = apiVaisseau.recolter(idEquipe, idVaisseau, x, y);
                        break;
                    case "Attaquer":
                        followUpResult = apiVaisseau.attaquer(idEquipe, idVaisseau, x, y);
                        break;
                    case "Déposer":
                        followUpResult = apiVaisseau.deposer(idEquipe, idVaisseau, x, y);
                        break;
                    case "Conquérir":
                        followUpResult = apiVaisseau.conquerir(idEquipe, idVaisseau, x, y);
                        break;
                }

                // Si on a une réponse, extraire le cooldown
                if (followUpResult != null && !followUpResult.isEmpty()) {
                    try {
                        JsonObject responseObj = gson.fromJson(followUpResult, JsonObject.class);
                        if (responseObj.has("message")) {
                            String message = responseObj.get("message").getAsString();
                            if (message.contains("Vaisseau indisponible") || message.contains("prochaine disponibilité")) {
                                // Extraire le temps de cooldown
                                String cooldownTime = message.substring(message.lastIndexOf(":") - 5);
                                startCooldownTimer(cooldownTime);
                            }
                        }
                    } catch (Exception e) {
                        // Erreur de parsing, ignorer
                    }
                }
            } catch (Exception e) {
                // Erreur lors de la requête de suivi, ignorer
            }
        }).start();
    }

    private void startCooldownTimer(String timeString) {
        Platform.runLater(() -> {
            if (cooldownLabel == null) return;

            // Arrêter le timer existant s'il y en a un
            if (cooldownTimer != null) {
                cooldownTimer.stop();
            }

            // Parser l'heure de fin (HH:MM:SS) - c'est une heure absolue, pas une durée
            String[] parts = timeString.split(":");
            int endHours = Integer.parseInt(parts[0]);
            int endMinutes = Integer.parseInt(parts[1]);
            int endSeconds = Integer.parseInt(parts[2]);

            // Stocker l'heure de fin pour ce vaisseau
            java.time.LocalTime endTime = java.time.LocalTime.of(endHours, endMinutes, endSeconds);
            if (currentVaisseauId != null) {
                shipCooldowns.put(currentVaisseauId, endTime);
            }

            // Calculer l'heure actuelle
            java.time.LocalTime now = java.time.LocalTime.now();
            int nowHours = now.getHour();
            int nowMinutes = now.getMinute();
            int nowSeconds = now.getSecond();

            // Calculer la différence en secondes
            int endTotalSeconds = endHours * 3600 + endMinutes * 60 + endSeconds;
            int nowTotalSeconds = nowHours * 3600 + nowMinutes * 60 + nowSeconds;
            final int[] remainingSeconds = {endTotalSeconds - nowTotalSeconds};

            // Si le temps est négatif, c'est que l'heure de fin est le lendemain
            if (remainingSeconds[0] < 0) {
                remainingSeconds[0] += 24 * 3600;
            }

            // Afficher le label
            cooldownLabel.setVisible(true);
            cooldownLabel.setText("Cooldown: " + formatTime(remainingSeconds[0]));

            // Créer un timer qui se déclenche toutes les secondes
            cooldownTimer = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
                    remainingSeconds[0]--;
                    if (remainingSeconds[0] <= 0) {
                        cooldownLabel.setText("Disponible !");
                        cooldownLabel.setStyle("-fx-text-fill: #00FF00; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 10 0 0 0;");
                        // Retirer le cooldown de la map
                        if (currentVaisseauId != null) {
                            shipCooldowns.remove(currentVaisseauId);
                        }
                        cooldownTimer.stop();
                    } else {
                        cooldownLabel.setText("Cooldown: " + formatTime(remainingSeconds[0]));
                    }
                })
            );
            cooldownTimer.setCycleCount(javafx.animation.Animation.INDEFINITE);
            cooldownTimer.play();
        });
    }

    private String formatTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
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

    private void showPlaceModuleDialog(String idPlanete) {
        new Thread(() -> {
            try {
                // Récupérer la liste des modules disponibles
                String modulesJson = apiModule.listerModules(idEquipe);
                JsonArray modules = gson.fromJson(modulesJson, JsonArray.class);

                // Debug: afficher la structure des modules
                System.out.println("=== MODULES JSON ===");
                System.out.println(modulesJson);
                System.out.println("====================");

                Platform.runLater(() -> {
                    if (modules == null || modules.size() == 0) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Aucun module");
                        alert.setContentText("Vous n'avez aucun module disponible à poser.");
                        alert.showAndWait();
                        return;
                    }

                    // Créer une liste de choix pour les modules
                    ChoiceDialog<String> dialog = new ChoiceDialog<>();
                    dialog.setTitle("Poser un module");
                    dialog.setHeaderText("Sélectionnez un module à poser sur cette planète");
                    dialog.setContentText("Module:");

                    // Ajouter les modules à la liste
                    Map<String, String> moduleMap = new HashMap<>();
                    for (int i = 0; i < modules.size(); i++) {
                        JsonObject module = modules.get(i).getAsJsonObject();

                        // Ignorer les modules déjà posés sur une planète
                        if (module.has("idPlanete") && module.get("idPlanete") != null && !module.get("idPlanete").isJsonNull()) {
                            continue;
                        }

                        // L'ID est à la racine
                        String moduleId = module.has("id") ? module.get("id").getAsString() : null;
                        if (moduleId == null) {
                            continue;
                        }

                        // Le type de module est dans paramModule.typeModule
                        String moduleType = "";
                        if (module.has("paramModule") && module.get("paramModule").isJsonObject()) {
                            JsonObject paramModule = module.get("paramModule").getAsJsonObject();
                            if (paramModule.has("typeModule")) {
                                moduleType = paramModule.get("typeModule").getAsString();
                                // Formater le type pour l'affichage
                                moduleType = moduleType.replace("_", " ").toLowerCase();
                                moduleType = moduleType.substring(0, 1).toUpperCase() + moduleType.substring(1);
                            }
                        }

                        String displayName = moduleType.isEmpty() ? "Module #" + (i+1) : moduleType;
                        moduleMap.put(displayName, moduleId);
                        dialog.getItems().add(displayName);
                    }

                    // Afficher le dialogue et attendre la sélection
                    dialog.showAndWait().ifPresent(selectedName -> {
                        String selectedModuleId = moduleMap.get(selectedName);
                        if (selectedModuleId != null) {
                            executePlaceModuleAction(selectedModuleId, idPlanete);
                        }
                    });
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erreur");
                    alert.setContentText("Impossible de récupérer les modules: " + e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }

    private void executePlaceModuleAction(String moduleId, String idPlanete) {
        new Thread(() -> {
            try {
                System.out.println("=== POSE DE MODULE ===");
                System.out.println("ID Équipe: " + idEquipe);
                System.out.println("ID Module: " + moduleId);
                System.out.println("ID Planète: " + idPlanete);

                String response = apiModule.poserModule(idEquipe, moduleId, idPlanete);

                // Vérifier si la réponse contient une erreur
                if (response != null && response.contains("\"code\":\"-1\"")) {
                    JsonObject responseObj = gson.fromJson(response, JsonObject.class);
                    String errorMessage = responseObj.has("message") ?
                        responseObj.get("message").getAsString() : "Erreur inconnue";
                    throw new Exception(errorMessage);
                }

                System.out.println("Module posé avec succès!");
                System.out.println("======================");

                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Succès");
                    alert.setContentText("Module posé avec succès !");
                    alert.showAndWait();
                });
            } catch (Exception e) {
                System.err.println("Erreur lors de la pose du module: " + e.getMessage());
                e.printStackTrace();

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
        if (currentMapCases == null) {
            return;
        }

        // Chercher la case cliquée dans les données
        for (int i = 0; i < currentMapCases.size(); i++) {
            JsonObject caseObj = currentMapCases.get(i).getAsJsonObject();
            int cx = caseObj.get("coord_x").getAsInt();
            int cy = caseObj.get("coord_y").getAsInt();

            if (cx == mapX && cy == mapY) {
                // Vérifier d'abord s'il y a un vaisseau (priorité car ils sont au-dessus)
                if (caseObj.has("vaisseau") && caseObj.get("vaisseau").isJsonObject()) {
                    JsonObject vaisseau = caseObj.get("vaisseau").getAsJsonObject();
                    if (vaisseau.has("idVaisseau")) {
                        displayVaisseauInfo(vaisseau, caseObj, mapX, mapY);
                        return;
                    }
                }

                // Sinon, vérifier s'il y a une planète sur cette case
                if (caseObj.has("planete") && caseObj.get("planete").isJsonObject()) {
                    JsonObject planete = caseObj.get("planete").getAsJsonObject();
                    int pdv = planete.has("pointDeVie") ? planete.get("pointDeVie").getAsInt() : 0;
                    if (pdv > 0) {
                        displayPlaneteInfo(planete, caseObj, mapX, mapY);
                        return;
                    }
                }

                return;
            }
        }
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

            // Si la planète nous appartient, ajouter l'option de poser des modules
            boolean isOurPlanet = proprietaire.toLowerCase().contains("pointeur") ||
                                  proprietaire.toLowerCase().contains("fou");

            if (isOurPlanet) {
                Label moduleTitle = new Label("\nModules:");
                moduleTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

                Button placeModuleBtn = new Button("Poser un module");
                placeModuleBtn.setStyle("-fx-background-color: #4a4a4a; -fx-text-fill: white;");
                placeModuleBtn.setOnAction(e -> showPlaceModuleDialog(id));

                planetePanel.getChildren().addAll(moduleTitle, placeModuleBtn);
            }

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
            currentVaisseauId = id;
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
                    isAlly = true;
                } else {
                    // Chercher dans le cache des noms d'équipes (construit depuis la carte)
                    if (equipeNames.containsKey(proprietaireId)) {
                        proprietaire = equipeNames.get(proprietaireId);
                    } else {
                        // Nom d'équipe inconnu (pas encore rencontré sur la carte)
                        proprietaire = "Équipe ennemie";
                    }
                }

                // Assigner une couleur unique par équipe
                vaisseauColor = getEquipeColor(proprietaire, proprietaireId);
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

                // Label de cooldown
                cooldownLabel = new Label("");
                cooldownLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 10 0 0 0;");
                cooldownLabel.setVisible(false);
                vaisseauPanel.getChildren().add(cooldownLabel);

                // Vérifier s'il y a un cooldown actif pour ce vaisseau
                if (shipCooldowns.containsKey(id)) {
                    java.time.LocalTime endTime = shipCooldowns.get(id);
                    java.time.LocalTime now = java.time.LocalTime.now();

                    // Vérifier si le cooldown n'est pas expiré
                    if (endTime.isAfter(now)) {
                        // Reformater l'heure de fin et démarrer le timer
                        String timeString = String.format("%02d:%02d:%02d",
                            endTime.getHour(), endTime.getMinute(), endTime.getSecond());
                        startCooldownTimer(timeString);
                    } else {
                        // Cooldown expiré, le retirer
                        shipCooldowns.remove(id);
                    }
                }
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
                // printLegend(); // Désactivé pour réduire les logs
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

                // Assigner une couleur unique par équipe
                planeteColor = getEquipeColor(equipeName, equipeName);

                // Vérifier si c'est notre équipe (par le nom)
                if (equipeName.toLowerCase().contains("pointeur") ||
                    equipeName.toLowerCase().contains("fou")) {
                    alliedPlanetesIds.add(planeteId);
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

        // Déterminer la couleur par équipe
        String vaisseauId = vaisseau.get("idVaisseau").getAsString();
        String equipeName = null;
        String equipeId = null;
        Color vaisseauColor = Color.GRAY;

        // Récupérer l'ID de l'équipe propriétaire depuis le vaisseau
        if (vaisseau.has("proprietaire")) {
            equipeId = vaisseau.get("proprietaire").getAsString();
        }

        // Sinon chercher dans le proprietaire de la case pour avoir le nom
        if (caseObj.has("proprietaire") && caseObj.get("proprietaire").isJsonObject()) {
            JsonObject proprietaire = caseObj.get("proprietaire").getAsJsonObject();
            if (proprietaire.has("nom")) {
                equipeName = proprietaire.get("nom").getAsString();
            }
        }

        // Si on a un nom d'équipe mais pas d'ID, utiliser le nom comme ID
        if (equipeId == null && equipeName != null) {
            equipeId = equipeName;
        }

        // Assigner une couleur unique par équipe
        vaisseauColor = getEquipeColor(equipeName, equipeId);

        // Vérifier si c'est notre équipe
        if (equipeId != null && equipeId.equals(idEquipe)) {
            alliedVaisseauxIds.add(vaisseauId);
        } else if (equipeName != null && (equipeName.toLowerCase().contains("pointeur") ||
                   equipeName.toLowerCase().contains("fou"))) {
            alliedVaisseauxIds.add(vaisseauId);
        }

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
