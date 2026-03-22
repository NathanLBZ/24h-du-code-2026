package LesPointeursFous.services;

public class ApiEquipe {

    private final ApiClient api;

    public ApiEquipe(ApiClient api) {
        this.api = api;
    }

    public String getAllEquipes() throws Exception {
        return api.get("equipes");
    }

    public String getEquipe(String idEquipe) throws Exception {
        return api.get("equipes/" + idEquipe);
    }

    public String getPlans(String idEquipe) throws Exception {
        return api.get("equipes/" + idEquipe + "/plans");
    }
}
