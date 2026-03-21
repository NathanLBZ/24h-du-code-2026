package LesPointeursFous.services;

public class ApiMarket {
    private final ApiClient api;

    public ApiMarket(ApiClient api) {
        this.api = api;
    }
    public String listerOffres() throws Exception {
        return api.get("market/offres");
    }

    public String vendre(String ressourceId, int quantite, int prix) throws Exception {
        String json = "{\"ressourceId\": " + ressourceId + ", \"quantite\": " + quantite + ", \"prix\": " + prix + "}";
        return api.post("/market/offres", json);
    }

    public String acheter(String idOffre) throws Exception {
        return api.get("/market/offres/" + idOffre);
    }

    public void supprimer(String idOffre) throws Exception {
        api.delete("/market/offres/" + idOffre);
    }
}
