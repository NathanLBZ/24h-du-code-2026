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

}
