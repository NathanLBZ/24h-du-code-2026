package LesPointeursFous.services;

import LesPointeursFous.services.*;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;

public class RoutineExtraction {
    private ApiVaisseau vaisseau;
    private int depot_x;
    private int depot_y;
    private int planete_x;
    private int planete_y;

    private List<Integer> deplacementsAller;
    private List<Integer> deplacementsRetour;

    public RoutineExtraction(ApiVaisseau vaisseau){
        this.vaisseau = vaisseau;
        this.deplacementsAller = new ArrayList<>();
        this.deplacementsRetour = new ArrayList<>();
    }

    public void setDepot(int depot_x, int depot_y){
        this.depot_x = depot_x;
        this.depot_y = depot_y;
    }

    public void setPlanete(int planete_x, int planete_y){
        this.planete_x = planete_x;
        this.planete_y = planete_y;
    }

    public void addAller(int x, int y){
        this.deplacementsAller.add(x);
        this.deplacementsAller.add(y);
    }

    public void addRetour(int x, int y){
        this.deplacementsRetour.add(x);
        this.deplacementsRetour.add(y);
    }

    public void call(String idEquipe, String vaisseauId) throws Exception{

        boolean arrive = false;
        while (!arrive) {
            String json = vaisseau.getVaisseaux(idEquipe);
            JsonArray vaisseauxArray = new Gson().fromJson(json, JsonArray.class);
            JsonObject v = null;
            for (int i = 0; i < vaisseauxArray.size(); i++) {
                if (vaisseauxArray.get(i).getAsJsonObject().get("idVaisseau").getAsString().equals(vaisseauId)) {
                    v = vaisseauxArray.get(i).getAsJsonObject();
                    break;
                }
            }

            if (v == null) throw new Exception("Vaisseau non trouvé");

            // Vérifier si le vaisseau a des ressources
            int cargaison = v.has("cargaison") ? v.get("cargaison").getAsInt() : 0;

            if (cargaison > 0) {
                arrive = true; // La récolte est terminée, le vaisseau a des ressources
            } else {
                System.out.println("Récolte en cours...");
                Thread.sleep(1000); // attendre 1 seconde avant de re-vérifier
            }
        }

        // ----------------------------------------

        int index = 0;
        while (index < this.deplacementsAller.size()){
            int xDest = this.deplacementsAller.get(index);
            int yDest = this.deplacementsAller.get(index+1);

            arrive = false;
            while (!arrive) {
                String json = vaisseau.getVaisseaux(idEquipe);
                JsonArray vaisseauxArray = new Gson().fromJson(json, JsonArray.class);
                JsonObject v = null;
                for (int i = 0; i < vaisseauxArray.size(); i++) {
                    if (vaisseauxArray.get(i).getAsJsonObject().get("idVaisseau").getAsString().equals(vaisseauId)) {
                        v = vaisseauxArray.get(i).getAsJsonObject();
                        break;
                    }
                }

                if (v == null) throw new Exception("Vaisseau non trouvé");

                int xActuel = v.has("positionX") ? v.get("positionX").getAsInt() :
                            (v.has("coord_x") ? v.get("coord_x").getAsInt() : 0);
                int yActuel = v.has("positionY") ? v.get("positionY").getAsInt() :
                            (v.has("coord_y") ? v.get("coord_y").getAsInt() : 0);

                if (xActuel == xDest && yActuel == yDest) {
                    arrive = true; // Le vaisseau est arrivé
                } else {
                    System.out.println("Vaisseau en déplacement... position actuelle: (" + xActuel + ", " + yActuel + ")");
                    Thread.sleep(1000); // attendre 1 seconde avant de re-vérifier
                }
            }
            index += 2;
        }

        // -----------------------------

        arrive = false;
        while (!arrive) {
            String json = vaisseau.getVaisseaux(idEquipe);
            JsonArray vaisseauxArray = new Gson().fromJson(json, JsonArray.class);
            JsonObject v = null;
            for (int i = 0; i < vaisseauxArray.size(); i++) {
                if (vaisseauxArray.get(i).getAsJsonObject().get("id").getAsString().equals(vaisseauId)) {
                    v = vaisseauxArray.get(i).getAsJsonObject();
                    break;
                }
            }

            if (v == null) throw new Exception("Vaisseau non trouvé");

            int cargaison = v.has("cargaison") ? v.get("cargaison").getAsInt() : 0;

            if (cargaison == 0) {
                arrive = true; // Le vaisseau a tout déposé
            } else {
                System.out.println("Dépot en cours...");
                Thread.sleep(1000); // attendre 1 seconde avant de re-vérifier
            }
        }

        // -------------------------------------------------------

        index = 0;
        while (index < this.deplacementsRetour.size()){
            int xDest = this.deplacementsRetour.get(index);
            int yDest = this.deplacementsRetour.get(index+1);

            arrive = false;
            while (!arrive) {
                String json = vaisseau.getVaisseaux(idEquipe);
                JsonArray vaisseauxArray = new Gson().fromJson(json, JsonArray.class);
                JsonObject v = null;
                for (int i = 0; i < vaisseauxArray.size(); i++) {
                    if (vaisseauxArray.get(i).getAsJsonObject().get("id").getAsString().equals(vaisseauId)) {
                        v = vaisseauxArray.get(i).getAsJsonObject();
                        break;
                    }
                }

                if (v == null) throw new Exception("Vaisseau non trouvé");

                int xActuel = v.has("positionX") ? v.get("positionX").getAsInt() :
                            (v.has("coord_x") ? v.get("coord_x").getAsInt() : 0);
                int yActuel = v.has("positionY") ? v.get("positionY").getAsInt() :
                            (v.has("coord_y") ? v.get("coord_y").getAsInt() : 0);

                if (xActuel == xDest && yActuel == yDest) {
                    arrive = true; // Le vaisseau est arrivé
                } else {
                    System.out.println("Vaisseau en déplacement... position actuelle: (" + xActuel + ", " + yActuel + ")");
                    Thread.sleep(1000); // attendre 1 seconde avant de re-vérifier
                }
            }
            index += 2;
        }
    }
    

    
}
