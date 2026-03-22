package LesPointeursFous.services;

import LesPointeursFous.services.*;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.time.Instant;
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

    private JsonObject attendreDisponible(String idEquipe, String vaisseauId) throws Exception {
        while (true) {
            String json = vaisseau.getVaisseaux(idEquipe);
            JsonArray vaisseauxArray = new Gson().fromJson(json, JsonArray.class);
            JsonObject v = null;

            for (int i = 0; i < vaisseauxArray.size(); i++) {
                JsonObject obj = vaisseauxArray.get(i).getAsJsonObject();
                if (obj.get("idVaisseau").getAsString().equals(vaisseauId)) {
                    v = obj;
                    break;
                }
            }

            if (v == null) throw new Exception("Vaisseau non trouvé");

            System.out.println(v);
            if (v.has("dateProchaineAction")) {
                Instant dispo = Instant.parse(v.get("dateProchaineAction").getAsString());
                Instant now = Instant.now();
                if (dispo.isBefore(now)) {
                    return v; // prêt !
                } else {
                    long attenteMs = dispo.toEpochMilli() - now.toEpochMilli() + 500;
                    System.out.println("Vaisseau pas prêt, attente " + (attenteMs / 1000) + "s...");
                    Thread.sleep(Math.max(attenteMs, 1000));
                }
            } else {
                System.out.println("\n\nzeioijhfgjklhg\n\n");
                return v; // pas de champ, on considère prêt
            }
        }
    }

    public void call(String idEquipe, String vaisseauId) throws Exception {

    // -------------------------
    // 1️⃣ Récolte sur la planète
    // -------------------------
    boolean arrive = false;
    while (!arrive) {
        JsonObject v = attendreDisponible(idEquipe, vaisseauId);
        vaisseau.recolter(idEquipe, vaisseauId, planete_x, planete_y);

        int cargaison = v.has("mineraiTransporte") ? v.get("mineraiTransporte").getAsInt() : 0;
        if (cargaison > 0) {
            arrive = true;
        } else {
            System.out.println("Récolte en cours...");
            Thread.sleep(1000);
        }
    }

    // -------------------------
    // 2️⃣ Déplacements Aller
    // -------------------------
    for (int index = 0; index < this.deplacementsAller.size(); index += 2) {
        int xDest = this.deplacementsAller.get(index);
        int yDest = this.deplacementsAller.get(index + 1);

        boolean atteint = false;
        while (!atteint) {
            attendreDisponible(idEquipe, vaisseauId);
            vaisseau.deplacer(idEquipe, vaisseauId, xDest, yDest);

            JsonObject v = attendreDisponible(idEquipe, vaisseauId);
            int xActuel = v.has("positionX") ? v.get("positionX").getAsInt() : 0;
            int yActuel = v.has("positionY") ? v.get("positionY").getAsInt() : 0;

            if (xActuel == xDest && yActuel == yDest) {
                atteint = true;
            } else {
                System.out.println("Vaisseau en déplacement... position actuelle: (" + xActuel + ", " + yActuel + ")");
                Thread.sleep(1000);
            }
        }
    }

    // Dépot
        boolean depotTermine = false;
        while (!depotTermine) {
            // 1️⃣ Attendre que le vaisseau soit prêt
            JsonObject v = attendreDisponible(idEquipe, vaisseauId);

            // 2️⃣ Envoyer la requête de dépôt
            
            // 3️⃣ Boucle pour vérifier que la cargaison est bien vide
            while (true) {
                vaisseau.deposer(idEquipe, vaisseauId, depot_x, depot_y);
                v = attendreDisponible(idEquipe, vaisseauId); // attendre fin de l'action
                int cargaison = v.has("mineraiTransporte") ? v.get("mineraiTransporte").getAsInt() : 0;
                if (cargaison == 0) {
                    depotTermine = true;
                    break;
                } else {
                    System.out.println("Dépot en cours... cargaison actuelle: " + cargaison);
                    Thread.sleep(1000);
                }
            }
        }

    // -------------------------
    // 4️⃣ Déplacements Retour
    // -------------------------
    for (int index = 0; index < this.deplacementsRetour.size(); index += 2) {
        int xDest = this.deplacementsRetour.get(index);
        int yDest = this.deplacementsRetour.get(index + 1);

        boolean atteint = false;
        while (!atteint) {
            attendreDisponible(idEquipe, vaisseauId);
            vaisseau.deplacer(idEquipe, vaisseauId, xDest, yDest);

            JsonObject v = attendreDisponible(idEquipe, vaisseauId);
            int xActuel = v.has("positionX") ? v.get("positionX").getAsInt() : 0;
            int yActuel = v.has("positionY") ? v.get("positionY").getAsInt() : 0;

            if (xActuel == xDest && yActuel == yDest) {
                atteint = true;
            } else {
                System.out.println("Vaisseau en déplacement... position actuelle: (" + xActuel + ", " + yActuel + ")");
                Thread.sleep(1000);
            }
        }
    }

    System.out.println("Routine terminée pour le vaisseau " + vaisseauId);
}

    
}
