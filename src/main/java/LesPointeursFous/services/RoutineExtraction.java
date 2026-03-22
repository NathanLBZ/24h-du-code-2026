package LesPointeursFous.services;

import LesPointeursFous.services.*;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.time.Instant;
import java.util.ArrayList;

public class RoutineExtraction {
    private static final int MAX_RETRY = 3;
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
    int maxEchecs = 3;

    // -------------------------
    // 1️⃣ Récolte sur la planète
    // -------------------------
    int echecs = 0;
    while (true) {
        try {
            JsonObject v = attendreDisponible(idEquipe, vaisseauId);
            vaisseau.recolter(idEquipe, vaisseauId, planete_x, planete_y);

            // Estimation du temps de récolte (en secondes)
            int tempsAttente = 5; // à ajuster selon le jeu
            System.out.println("Récolte lancée, attente " + tempsAttente + "s...");
            Thread.sleep(tempsAttente * 1000);

            v = attendreDisponible(idEquipe, vaisseauId); // check final
            int cargaison = v.has("mineraiTransporte") ? v.get("mineraiTransporte").getAsInt() : 0;

            if (cargaison > 0) break; // ok, récolté
            else throw new Exception("Récolte vide !");
        } catch (Exception e) {
            echecs++;
            System.out.println("Échec récolte n°" + echecs + " : " + e.getMessage());
            if (echecs >= maxEchecs) {
                System.out.println("Trop d'échecs, on skip ce vaisseau.");
                return;
            }
        }
    }

    // -------------------------
    // 2️⃣ Déplacements Aller
    // -------------------------
    for (int index = 0; index < deplacementsAller.size(); index += 2) {
        int xDest = deplacementsAller.get(index);
        int yDest = deplacementsAller.get(index + 1);

        echecs = 0;
        while (true) {
            try {
                JsonObject v = attendreDisponible(idEquipe, vaisseauId);
                vaisseau.deplacer(idEquipe, vaisseauId, xDest, yDest);

                // Estimation du temps de déplacement
                int xActuel = v.has("positionX") ? v.get("positionX").getAsInt() : 0;
                int yActuel = v.has("positionY") ? v.get("positionY").getAsInt() : 0;
                int distance = Math.abs(xDest - xActuel) + Math.abs(yDest - yActuel);
                int vitesse = v.has("vitesse") ? v.get("vitesse").getAsInt() : 1;
                int tempsAttente = distance / Math.max(vitesse, 1) + 1;
                System.out.println("Déplacement lancé vers (" + xDest + "," + yDest + "), attente " + tempsAttente + "s...");
                Thread.sleep(tempsAttente * 1000);

                v = attendreDisponible(idEquipe, vaisseauId); // check final
                xActuel = v.has("positionX") ? v.get("positionX").getAsInt() : 0;
                yActuel = v.has("positionY") ? v.get("positionY").getAsInt() : 0;

                if (xActuel == xDest && yActuel == yDest) break; // ok
                else throw new Exception("Vaisseau pas arrivé !");
            } catch (Exception e) {
                echecs++;
                System.out.println("Échec déplacement n°" + echecs + " : " + e.getMessage());
                if (echecs >= maxEchecs) {
                    System.out.println("Trop d'échecs déplacement, on skip ce vaisseau.");
                    return;
                }
            }
        }
    }

    // -------------------------
    // 3️⃣ Dépot
    // -------------------------
    echecs = 0;
    while (true) {
        try {
            JsonObject v = attendreDisponible(idEquipe, vaisseauId);
            vaisseau.deposer(idEquipe, vaisseauId, depot_x, depot_y);

            // Estimation temps dépôt
            int tempsAttente = 5;
            System.out.println("Dépôt lancé, attente " + tempsAttente + "s...");
            Thread.sleep(tempsAttente * 1000);

            v = attendreDisponible(idEquipe, vaisseauId);
            int cargaison = v.has("mineraiTransporte") ? v.get("mineraiTransporte").getAsInt() : 0;

            if (cargaison == 0) break; // ok, dépôt terminé
            else throw new Exception("Cargaison non vide !");
        } catch (Exception e) {
            echecs++;
            System.out.println("Échec dépôt n°" + echecs + " : " + e.getMessage());
            if (echecs >= maxEchecs) {
                System.out.println("Trop d'échecs dépôt, on skip ce vaisseau.");
                return;
            }
        }
    }

    // -------------------------
    // 4️⃣ Déplacements Retour
    // -------------------------
    for (int index = 0; index < deplacementsRetour.size(); index += 2) {
        int xDest = deplacementsRetour.get(index);
        int yDest = deplacementsRetour.get(index + 1);

        echecs = 0;
        while (true) {
            try {
                JsonObject v = attendreDisponible(idEquipe, vaisseauId);
                vaisseau.deplacer(idEquipe, vaisseauId, xDest, yDest);

                int xActuel = v.has("positionX") ? v.get("positionX").getAsInt() : 0;
                int yActuel = v.has("positionY") ? v.get("positionY").getAsInt() : 0;
                int distance = Math.abs(xDest - xActuel) + Math.abs(yDest - yActuel);
                int vitesse = v.has("vitesse") ? v.get("vitesse").getAsInt() : 1;
                int tempsAttente = distance / Math.max(vitesse, 1) + 1;
                Thread.sleep(tempsAttente * 1000);

                v = attendreDisponible(idEquipe, vaisseauId);
                xActuel = v.has("positionX") ? v.get("positionX").getAsInt() : 0;
                yActuel = v.has("positionY") ? v.get("positionY").getAsInt() : 0;

                if (xActuel == xDest && yActuel == yDest) break;
                else throw new Exception("Vaisseau pas arrivé !");
            } catch (Exception e) {
                echecs++;
                System.out.println("Échec retour n°" + echecs + " : " + e.getMessage());
                if (echecs >= maxEchecs) {
                    System.out.println("Trop d'échecs retour, on skip ce vaisseau.");
                    return;
                }
            }
        }
    }

    System.out.println("Routine terminée pour le vaisseau " + vaisseauId);
}

    
}
