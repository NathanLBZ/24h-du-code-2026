package LesPointeursFous.services;

public class ApiMarket {
    private final ApiClient api;

    public ApiMarket(ApiClient api) {
        this.api = api;
    }
    public String listerOffres() throws Exception {
        return api.get("market/offres");
    }


    // module dechargement de l'offre : aa2e2942-8ae4-4534-b25b-b1da18a30018
    public String acheter(String idEquipe, String idOffre) throws Exception {
        String json = "{\"idOffre\": \"" + idOffre + "\"}";

        // L'endpoint est généralement celui-ci pour un achat
        return api.post("equipes/" + idEquipe + "/acheter", json);
    }
    
}
