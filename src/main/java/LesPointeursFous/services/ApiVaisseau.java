package LesPointeursFous.services;

public class ApiVaisseau {

    private final ApiClient api;

    public ApiVaisseau(ApiClient api) {
        this.api = api;
    }

    public void deplacer(String idEquipe, String idVaisseau, int xArr, int yArr) throws Exception {
        String json = "{\"action\": \"DEPLACEMENT\", \"coord_x\": " + xArr + ", \"coord_y\": " + yArr + "}";
        api.post("equipes/" + idEquipe + "/vaisseaux/" + idVaisseau + "/demander-action", json);

    }

    public void recolter(String idEquipe, String idVaisseau, int xArr, int yArr) throws Exception {
        String json = "{\"action\": \"RECOLTER\", \"coord_x\": " + xArr + ", \"coord_y\": " + yArr + "}";

        api.post("equipes/" + idEquipe + "/vaisseaux/" + idVaisseau + "/demander-action", json);
    }

    public void deposer(String idEquipe, String idVaisseau, int xArr, int yArr) throws Exception{
        String json  = "{\"action\": \"DEPOSER\", \"coord_x\": " + xArr + ", \"coord_y\": " + yArr + "}";
        System.out.println("equipes/" + idEquipe + "/vaisseaux/" + idVaisseau + "/demander-action" + json);
        api.post("equipes/" + idEquipe + "/vaisseaux/" + idVaisseau + "/demander-action", json);
    }

    public String getVaisseaux(String idEquipe) throws Exception {
        return api.get("equipes/" + idEquipe + "/vaisseaux");
    }

    public String getAllVaisseaux() throws Exception {
        return api.get("vaisseaux");
    }

    public void attaquer(String idEquipe, String idVaisseau, int xCible, int yCible) throws Exception {
        String json = "{\"action\": \"ATTAQUE\", \"coord_x\": " + xCible + ", \"coord_y\": " + yCible + "}";
        envoyerAction(idEquipe, idVaisseau, json);
    }

    private void envoyerAction(String idEquipe, String idVaisseau, String json) throws Exception {
        api.post("equipes/" + idEquipe + "/vaisseaux/" + idVaisseau + "/demander-action", json);
    }
}