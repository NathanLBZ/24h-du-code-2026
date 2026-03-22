package LesPointeursFous.services;

public class ApiModule {

    private final ApiClient api;

    public ApiModule(ApiClient api) {
        this.api = api;
    }

    public String listerModules(String idEquipe) throws Exception {
        return api.get("equipes/" + idEquipe + "/modules");
    }

    public String getModule(String idEquipe, String moduleId) throws Exception {
        return api.get("equipes/" + idEquipe + "/modules/" + moduleId);
    }

    public void poserModule(String idEquipe, String moduleId, String idPlanete) throws Exception {
        String path = "equipes/" + idEquipe + "/module/" + moduleId + "/poser";
        String json = "{\"idModule\": \"" + moduleId + "\", \"idPlanete\": \"" + idPlanete + "\"}";

        api.put(path, json);
    }

    public void supprimerModule(String idEquipe, String moduleId) throws Exception {
        String path = "equipes/" + idEquipe + "/module/" + moduleId + "/supprimer";
        api.delete(path);
    }
}