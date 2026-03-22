package LesPointeursFous.services;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ApiMap {

    private final ApiClient api;
    private final Gson gson;

    public ApiMap(ApiClient api) {
        this.api = api;
        this.gson = new Gson();
    }

    public String getMap() throws Exception {
        return api.get("/map");

    }

    public String getMap(int xMin, int xMax, int yMin, int yMax) throws Exception {

  
        String xRange = xMin + "," + xMax;
        String yRange = yMin + "," + yMax;
        return api.get("monde/map?x_range=" + xRange + "&y_range=" + yRange);
    }
    
    public void afficherMapASCII(int xMin, int xMax, int yMin, int yMax) throws Exception {
        // Appeler /monde/map AVEC les paramètres pour obtenir toutes les données
        String carteCompleteJson = getMap(xMin, xMax, yMin, yMax);

        System.out.println("Données brutes de la carte : " + carteCompleteJson);
        
        JsonArray cases = gson.fromJson(carteCompleteJson, JsonArray.class);

        if (cases == null) {
            cases = new JsonArray();
        }

        // Créer une grille pour stocker les cases
        char[][] grille = new char[yMax - yMin + 1][xMax - xMin + 1];

        // Initialiser la grille avec des points (cases vides)
        for (int y = 0; y <= yMax - yMin; y++) {
            for (int x = 0; x <= xMax - xMin; x++) {
                grille[y][x] = '.';
            }
        }

        // Parcourir toutes les cases et placer planètes/vaisseaux sur la grille
        for (int i = 0; i < cases.size(); i++) {
            JsonObject caseObj = cases.get(i).getAsJsonObject();

            int cx = caseObj.get("coord_x").getAsInt();
            int cy = caseObj.get("coord_y").getAsInt();

            // Vérifier si la case est dans la zone affichée
            if (cx >= xMin && cx <= xMax && cy >= yMin && cy <= yMax) {
                int x = cx - xMin;
                int y = cy - yMin;

                // Vérifier s'il y a une vraie planète (pas de type VIDE)
                if (caseObj.has("planete") && caseObj.get("planete").isJsonObject()) {
                    JsonObject planete = caseObj.get("planete").getAsJsonObject();
                    if (planete.has("pointDeVie") && planete.get("pointDeVie").getAsInt() > 0) {
                        grille[y][x] = 'P';
                    }
                }

                // Vérifier s'il y a un vaisseau (priorité sur planète)
                if (caseObj.has("vaisseau") && caseObj.get("vaisseau").isJsonObject()) {
                    JsonObject vaisseau = caseObj.get("vaisseau").getAsJsonObject();
                    if (vaisseau.has("idVaisseau")) {
                        grille[y][x] = 'V';
                    }
                }
            }
        }

        // Afficher la grille
        System.out.println("\n=== CARTE ===");
        System.out.print("   ");
        for (int x = xMin; x <= xMax; x++) {
            System.out.print(x%10);
        }
        System.out.println();

        for (int y = 0; y <= yMax - yMin; y++) {
            System.out.printf("%2d ", yMin + y);
            for (int x = 0; x <= xMax - xMin; x++) {
                System.out.print(grille[y][x]);
            }
            System.out.println();
        }
        System.out.println("\nLégende: . = vide, P = planète, V = vaisseau, O = occupé");
    }

    public void afficherMapComplete() throws Exception {

        int taille = 56;
        int chunkSize = 10; // taille des requêtes (à adapter selon l'API)
    
        char[][] grille = new char[taille][taille];
    
        // Initialiser la grille
        for (int y = 0; y < taille; y++) {
            for (int x = 0; x < taille; x++) {
                grille[y][x] = '.';
            }
        }
    
        // Boucler sur la map par blocs
        for (int y = 0; y < taille; y += chunkSize) {
            for (int x = 0; x < taille; x += chunkSize) {
    
                int xMax = Math.min(x + chunkSize - 1, taille - 1);
                int yMax = Math.min(y + chunkSize - 1, taille - 1);
    
                String json = getMap(x, xMax, y, yMax);
                JsonArray cases = gson.fromJson(json, JsonArray.class);
    
                if (cases == null) continue;
    
                for (int i = 0; i < cases.size(); i++) {
                    JsonObject caseObj = cases.get(i).getAsJsonObject();
    
                    int cx = caseObj.get("coord_x").getAsInt();
                    int cy = caseObj.get("coord_y").getAsInt();
    
                    // Planète
                    if (caseObj.has("planete") && caseObj.get("planete").isJsonObject()) {
                        JsonObject planete = caseObj.get("planete").getAsJsonObject();
                        if (planete.has("pointDeVie") && planete.get("pointDeVie").getAsInt() > 0) {
                            grille[cy][cx] = 'P';
                        }
                    }
    
                    // Vaisseau (prioritaire)
                    if (caseObj.has("vaisseau") && caseObj.get("vaisseau").isJsonObject()) {
                        JsonObject vaisseau = caseObj.get("vaisseau").getAsJsonObject();
                        if (vaisseau.has("idVaisseau")) {
                            grille[cy][cx] = 'V';
                        }
                    }
                }
            }
        }
    
        // Affichage final
        System.out.println("\n=== CARTE COMPLETE 56x56 ===");
    
        System.out.print("   ");
        for (int x = 0; x < taille; x++) {
            System.out.print(x % 10);
        }
        System.out.println();
    
        for (int y = 0; y < taille; y++) {
            System.out.printf("%2d ", y);
            for (int x = 0; x < taille; x++) {
                System.out.print(grille[y][x]);
            }
            System.out.println();
        }
    }

}
