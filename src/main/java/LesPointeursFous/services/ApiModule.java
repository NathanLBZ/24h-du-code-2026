package LesPointeursFous.services;

public class ApiModule {

    private final ApiClient api;

    public ApiModule(ApiClient api) {
        this.api = api;
    }

    public String getModules(String idEquipe) throws Exception {
        String path = "equipes/" + idEquipe + "/modules";
        return api.get(path);
    }

    public String getAllModules() throws Exception{
        String path = "/modules";
        return api.get(path);
    }

    public String getModuleById(String idEquipe, String moduleId) throws Exception {
        String path = "equipes/" + idEquipe + "/modules/" + moduleId;
        return api.get(path);
    }

    public void poserModule(String idEquipe, String moduleId, String idPlanete) throws Exception {
        String path = "equipes/" + idEquipe + "/module/" + moduleId + "/poser";
        String json = "{\"idModule\": " + moduleId + ", \"idPlanete\": " + idPlanete + "}";

        api.post(path, json);
    }

    public void supprimerModule(String idEquipe, String moduleId) throws Exception {
        String path = "equipes/" + idEquipe + "/module/" + moduleId + "/supprimer";
        api.delete(path);
    }
}