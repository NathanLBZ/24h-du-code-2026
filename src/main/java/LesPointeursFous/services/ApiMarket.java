/*
package LesPointeursFous.services;

public class ApiMarket {
    private final ApiClient api;

    public ApiMarket(ApiClient api) {
        this.api = api;
    }
    public String listerOffres() throws Exception {
        return api.get("market/offres");
    }

    public String acheter(String idEquipe, String idOffre) throws Exception {
        String json = "{\"id_offre\": \"" + idOffre + "\"}";
        {
            "idObjet": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
            "prix": 1000,
            "publique": true,
            "visiblePar": [
              "uuid-equipe-A",
              "uuid-equipe-B"
            ],
            "typeObjet": "MODULE"
          }

        return api.post("equipes/" + idEquipe + "/acheter", json);
    
    }
    
}

*/