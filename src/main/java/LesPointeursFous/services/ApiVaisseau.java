public class ApiVaisseau {

    private final ApiClient api;

    public ApiVaisseau(ApiClient api) {
        this.api = api;
    }

    public void deplacer(String idEquipe, String idVaisseau, int x, int y) throws Exception {
        String json = String.format("""
            {
                "action": "DEPLACEMENT",
                "coord_x": %d,
                "coord_y": %d
            }
        """, x, y);

        api.post("/equipes/" + idEquipe + "/vaisseaux/" + idVaisseau + "/demander-action", json);
    }

    public String getVaisseaux(String idEquipe) throws Exception {
        return api.get("/equipes/" + idEquipe + "/vaisseaux");
    }
}