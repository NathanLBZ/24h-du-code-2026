package LesPointeursFous.services;

public class ApiVaisseau {

    private final ApiClient api;

    public ApiVaisseau(ApiClient api) {
        this.api = api;
    }

    public void deplacer(String idEquipe, String idVaisseau, int xArr, int yArr) throws Exception {
        String json = "{\"action\": \"DEPLACEMENT\", \"coord_x\": " + xArr + ", \"coord_y\": " + yArr + "}";

        System.out.println("/equipes/" + idEquipe + "/vaisseaux/" + idVaisseau + "/demander-action");
        System.out.println(json);
        api.post("equipes/" + idEquipe + "/vaisseaux/" + idVaisseau + "/demander-action", json);

    }

    public void recolter(String idEquipe, String idVaisseau, int xArr, int yArr) throws Exception {
        String json = "{\"action\": \"RECOLTER\", \"coord_x\": " + xArr + ", \"coord_y\": " + yArr + "}";

        api.post("equipes/" + idEquipe + "/vaisseaux/" + idVaisseau + "/demander-action", json);
    }
    
    public void deposer(String idEquipe, String idVaisseau) throws Exception{
        String json = "{\"action\": \"DEPOSER\"}";

        api.post("equipes/" + idEquipe + "/vaisseaux/" + idVaisseau + "/demander-action", json);
    }

    public String getVaisseaux(String idEquipe) throws Exception {
        return api.get("equipes/" + idEquipe + "/vaisseaux");
    }
}