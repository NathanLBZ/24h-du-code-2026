package LesPointeursFous.services;

public class ApiVaisseau {

    private final ApiClient api;

    public ApiVaisseau(ApiClient api) {
        this.api = api;
    }

    public void deplacer(String idEquipe, String idVaisseau, int xArr, int yArr) throws Exception {
        String json = "{\"action\": \"DEPLACEMENT\", \"coord_x\": " + xArr + ", \"coord_y\": " + yArr + "}";

        api.post("/equipes/" + idEquipe + "/vaisseaux/" + idVaisseau + "/demander-action", json);
    }

    public String getVaisseaux(String idEquipe) throws Exception {
        return api.get("/equipes/" + idEquipe + "/vaisseaux");
    }
}