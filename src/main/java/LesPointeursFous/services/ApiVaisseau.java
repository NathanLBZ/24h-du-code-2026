package LesPointeursFous.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ApiVaisseau {

    private final ApiClient api;
    public static RoutineExtraction RE;

    public int getPrixFromOffre(JsonObject offre) throws Exception {

        if (!offre.has("plan")) {
            throw new Exception("Pas un plan");
        }

        JsonObject plan = offre.getAsJsonObject("plan");

        if (!plan.has("typeVaisseau")) {
            throw new Exception("Pas de typeVaisseau");
        }

        JsonObject type = plan.getAsJsonObject("typeVaisseau");

        return type.get("coutConstruction").getAsInt();
    }

    public int getPrix(String classeRecherchee) throws Exception {
        String json = api.get("market/offres");
        JsonArray offres = new Gson().fromJson(json, JsonArray.class);

        for (int i = 0; i < offres.size(); i++) {
            JsonObject offre = offres.get(i).getAsJsonObject();

            if (!offre.has("typeObjet")) continue;
            if (!offre.get("typeObjet").getAsString().equals("PLAN")) continue;

            JsonObject plan = offre.getAsJsonObject("plan");
            JsonObject type = plan.getAsJsonObject("typeVaisseau");

            String classe = type.get("classeVaisseau").getAsString();

            if (classe.equalsIgnoreCase(classeRecherchee)) {
                return type.get("coutConstruction").getAsInt();
            }
        }

        return -1;
    }

    public ApiVaisseau(ApiClient api) {
        this.api = api;
    }

    public String getVaisseaux(String idEquipe) throws Exception {
        return api.get("equipes/" + idEquipe + "/vaisseaux");
    }

    public String getVaisseau(String idEquipe, String idVaisseau) throws Exception {
        return api.get("equipes/" + idEquipe + "/vaisseaux/" + idVaisseau);
    }

    public String construireVaisseau(String idEquipe, String idTypeVaisseau, String idPlanete, String nom) throws Exception {
        String json = "{\"idTypeVaisseau\": " + idTypeVaisseau + ", \"idPlanete\": " + idPlanete + ", \"nom\": " + nom + "}";
        return api.post("equipes/" + idEquipe + "/vaisseau/construire", json);
    }

    public String action(String idEquipe, String idVaisseau, String action, Integer x, Integer y) throws Exception {
        String path = "equipes/" + idEquipe + "/vaisseaux/" + idVaisseau + "/demander-action";

        String json;

        if (x != null && y != null) {
            json = "{\"action\": \"" + action + "\", \"coord_x\": " + x + ", \"coord_y\": " + y + "}";
        } else {
            json = "{\"action\": \"" + action + "\"}";
        }
        System.out.println(path + "\n" + json);
        return api.post(path, json);
    }

    public String deplacer(String idEquipe, String idVaisseau, int x, int y) throws Exception {
        return action(idEquipe, idVaisseau, "DEPLACEMENT", x, y);
    }

    public String recolter(String idEquipe, String idVaisseau, int x, int y) throws Exception {
        return action(idEquipe, idVaisseau, "RECOLTER", x, y);
    }

    public String deposer(String idEquipe, String idVaisseau, int x, int y) throws Exception {
        return action(idEquipe, idVaisseau, "DEPOSER", x, y);
    }

    public String attaquer(String idEquipe, String idVaisseau, int x, int y) throws Exception {
        return action(idEquipe, idVaisseau, "ATTAQUER", x, y);
    }

    public String conquerir(String idEquipe, String idVaisseau, int x, int y) throws Exception {
        return action(idEquipe, idVaisseau, "CONQUERIR", x, y);
    }

    public String reparer(String idEquipe, String idVaisseau) throws Exception {
        return action(idEquipe, idVaisseau, "REPARER", null, null);
    }
}