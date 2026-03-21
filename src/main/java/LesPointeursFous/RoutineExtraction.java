package LesPointeursFous;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import LesPointeursFous.services.ApiClient;

public class RoutineExtraction {

    public void attaquerPlanete(ApiClient api, String idEquipe, String idVaisseau, int x, int y) {
        // 1. On vérifie les PV au départ
        int pvPlanete = getPvPlanete(api, x, y);
    
        if (pvPlanete > 0) {
            System.out.println("Début de l'attaque. PV de départ : " + pvPlanete);
            String pathAttaque = "/equipes/" + idEquipe + "/vaisseaux/" + idVaisseau + "/demander-action";
            String jsonBody = String.format("{\"action\": \"ATTAQUER\", \"coord_x\": %d, \"coord_y\": %d}", x, y);
    
            while (pvPlanete > 0) {
                try {
                    // 2. ON UTILISE L'API CLIENT (pour éviter la 401)
                    String reponse = api.post(pathAttaque, jsonBody);
                    System.out.println("Attaque envoyée ! Réponse : " + reponse);
    
                    // 3. PAUSE OBLIGATOIRE (Cooldown)
                    // Le serveur ne permet pas d'attaquer 1000 fois par seconde.
                    // Attends au moins 1 ou 2 secondes selon les règles du jeu.
                    Thread.sleep(2000); 
    
                    // 4. MISE À JOUR DES PV (Sinon la boucle est infinie)
                    pvPlanete = getPvPlanete(api, x, y);
                    System.out.println("PV restants : " + pvPlanete);
    
                } catch (Exception e) {
                    System.err.println("Erreur durant le cycle d'attaque : " + e.getMessage());
                    break; // On arrête en cas d'erreur grave
                }
            }
            System.out.println("Cible détruite ou hors de portée !");
        } 
        else {
            System.out.println("La planète est déjà à 0 PV ou introuvable.");
        }
    }

    public int getPvPlanete(ApiClient apiClient, int x, int y) {
        // Le path correspond à ce que l'ApiClient va concaténer à l'URL de base
        String path = String.format("monde/map?x_range=%d,%d&y_range=%d,%d", x, x, y, y);
        if (apiClient == null) {
            System.err.println("[Routine] ApiClient est null. Assurez-vous de l'initialiser avant d'appeler getPvPlanete.");
            return -1;
        }
        
        try {
            // Appelle la méthode get() de ton ApiClient (qui gère le 401 pour toi)
            String responseBody = apiClient.get(path);

            JsonArray jsonArray = JsonParser.parseString(responseBody).getAsJsonArray();
            
            if (jsonArray.size() > 0) {
                JsonObject caseObj = jsonArray.get(0).getAsJsonObject();
                if (caseObj.has("planete") && !caseObj.get("planete").isJsonNull()) {
                    return caseObj.getAsJsonObject("planete").get("pointDeVie").getAsInt();
                }
            }
        } catch (Exception e) {
            System.err.println("[Routine] Erreur lors de la récupération des PV : " + e.getMessage());
        }
        return -1;
    }
}