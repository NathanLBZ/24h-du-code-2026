package LesPointeursFous;

import java.time.Instant;
import java.time.Duration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import LesPointeursFous.services.ApiClient;

public class RoutineExtraction {
    private final ApiClient api;

    public RoutineExtraction(ApiClient api) {
        this.api = api;
    }

    public void attaquerPlanete(String idEquipe, String idVaisseau, int x, int y) {
        System.out.println("Attaque lancée sur la planète (" + x + ", " + y + ") avec le vaisseau " + idVaisseau);
        int pvPlanete = getPvPlanete(api, x, y);

        if (pvPlanete < 0) {
            System.out.println("Planète introuvable.");
            return;
        }

        System.out.println("Début. PV : " + pvPlanete);

        String pathAttaque = "equipes/" + idEquipe + "/vaisseaux/" + idVaisseau + "/demander-action";

        // ======================
        // 🔥 PHASE 1 : ATTAQUE
        // ======================
        while (pvPlanete > 0) {
            try {
                attendreCooldown(idEquipe, idVaisseau);

                String jsonBody = String.format(
                    "{\"action\": \"ATTAQUER\", \"coord_x\": %d, \"coord_y\": %d}",
                    x, y
                );

                String reponse = api.post(pathAttaque, jsonBody);
                System.out.println("Attaque envoyée ! Réponse : " + reponse);

                pvPlanete = getPvPlanete(api, x, y);
                System.out.println("PV restants : " + pvPlanete);

            } catch (Exception e) {
                System.err.println("Erreur attaque : " + e.getMessage());
                return;
            }
        }

        // ======================
        // 🏁 PHASE 2 : CONQUÊTE
        // ======================
        if (pvPlanete == 0) {
            try {
                attendreCooldown(idEquipe, idVaisseau);

                String jsonBody = String.format(
                    "{\"action\": \"CONQUERIR\", \"coord_x\": %d, \"coord_y\": %d}",
                    x, y
                );

                String reponse = api.post(pathAttaque, jsonBody);
                System.out.println("Planète conquise ! Réponse : " + reponse);

            } catch (Exception e) {
                System.err.println("Erreur conquête : " + e.getMessage());
            }
        }

    }

    // ======================
    // 🔁 COOLDOWN
    // ======================
    private void attendreCooldown(String idEquipe, String idVaisseau) throws InterruptedException {
        String date = getDateProchaineAction(idEquipe, idVaisseau);

        if (date != null) {
            long attente = calculerAttenteMillis(date);

            if (attente > 0) {
                System.out.println("Attente de " + (attente / 1000) + " sec...");
                Thread.sleep(attente + 50);
            }
        }
    }

    private long calculerAttenteMillis(String dateProchaineAction) {
        try {
            Instant prochaineAction = Instant.parse(dateProchaineAction);
            Instant maintenant = Instant.now();

            long millis = Duration.between(maintenant, prochaineAction).toMillis();

            return Math.max(millis, 0);
        } catch (Exception e) {
            System.err.println("Erreur parsing date : " + e.getMessage());
            return 2000;
        }
    }

    // ======================
    // 🌍 PV PLANÈTE
    // ======================
    public int getPvPlanete(ApiClient apiClient, int x, int y) {
        String path = String.format("monde/map?x_range=%d,%d&y_range=%d,%d", x, x, y, y);

        try {
            String responseBody = apiClient.get(path);
            JsonArray jsonArray = JsonParser.parseString(responseBody).getAsJsonArray();

            if (jsonArray.size() > 0) {
                JsonObject caseObj = jsonArray.get(0).getAsJsonObject();

                if (caseObj.has("planete") && !caseObj.get("planete").isJsonNull()) {
                    return caseObj.getAsJsonObject("planete")
                                  .get("pointDeVie")
                                  .getAsInt();
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur récupération PV : " + e.getMessage());
        }

        return -1;
    }

    // ======================
    // 🚀 VAISSEAU
    // ======================
    public String getDateProchaineAction(String idEquipe, String idVaisseau) {
        try {
            String response = api.get("equipes/" + idEquipe + "/vaisseaux");
            JsonArray array = JsonParser.parseString(response).getAsJsonArray();

            for (int i = 0; i < array.size(); i++) {
                JsonObject vaisseau = array.get(i).getAsJsonObject();

                if (vaisseau.get("idVaisseau").getAsString().equals(idVaisseau)) {
                    return vaisseau.get("dateProchaineAction").getAsString();
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur récupération vaisseau : " + e.getMessage());
        }

        return null;
    }
}