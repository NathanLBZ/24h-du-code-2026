package LesPointeursFous;

import LesPointeursFous.services.RoutineExtraction;
import LesPointeursFous.services.ApiClient;
import LesPointeursFous.services.ApiMap;
import LesPointeursFous.services.ApiMarket;
import LesPointeursFous.services.ApiVaisseau;
import LesPointeursFous.services.RoutineExtraction;
import io.github.cdimascio.dotenv.Dotenv;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Scanner;


public class App 
{
    public static void main( String[] args ) throws Exception
    {

        Dotenv dotenv = Dotenv.configure().load();
        ApiClient apiFetch = new ApiClient(dotenv.get("API_URL"), dotenv.get("API_KEY"));
        ApiMarket market = new ApiMarket(apiFetch);

        ApiVaisseau vaisseau = new ApiVaisseau(apiFetch);
        ApiMap map = new ApiMap(apiFetch);
        
        String idEquipe = "c1b647f1-1748-492a-b5a9-2a9af9b5e5ed";

        String vaisseaux[] = {"df37b158-2858-4139-8a83-6b47826892f4", "4b69ac2b-49e9-49cf-bac2-cc5b6539195f", "9b245656-5fee-4919-820b-a39b745464d6", "4680516c-cdce-487d-8dad-11d10c3075b6", "dae26621-d052-4699-a09a-beeff4678fe9", "f13a860b-fbe6-4040-9bc6-6a2b68ec3f39", "161ff5db-b1a7-47af-a2c0-1ba3303b5ec6"};
        String cargots[] = {"df37b158-2858-4139-8a83-6b47826892f4", "4b69ac2b-49e9-49cf-bac2-cc5b6539195f", "9b245656-5fee-4919-820b-a39b745464d6", "4680516c-cdce-487d-8dad-11d10c3075b6", "dae26621-d052-4699-a09a-beeff4678fe9", "f13a860b-fbe6-4040-9bc6-6a2b68ec3f39", "161ff5db-b1a7-47af-a2c0-1ba3303b5ec6"};
        int nbCargots = 8;
        RoutineExtraction REs[] = new RoutineExtraction[nbCargots];
        for (int i = 0; i < nbCargots; i++){
            REs[i] = new RoutineExtraction(vaisseau);
            REs[i].setDepot(34, 42);
        }
        REs[0].setPlanete(34, 42);
        REs[1].setPlanete(30, 40);
        REs[1].addAller(32, 40);
        REs[1].addAller(33, 40);
        REs[1].addAller(34, 41);
        REs[1].addAller(34, 42);
        REs[1].addRetour(34, 41);
        REs[1].addRetour(33, 40);
        REs[1].addRetour(32, 40);
        REs[1].addRetour(31, 40);
        REs[2].setPlanete(34, 37);
        REs[2].addAller(34, 40);
        REs[2].addAller(34, 39);
        REs[2].addAller(34, 38);
        REs[2].addRetour(34, 39);
        REs[2].addRetour(34, 40);
        REs[3].setPlanete(33, 36);
        REs[3].addAller(33, 37);
        REs[3].addAller(33, 38);
        REs[3].addAller(33, 39);
        REs[3].addAller(33, 40);
        REs[3].addAller(33, 41);
        REs[3].addRetour(33, 40);
        REs[3].addRetour(33, 39);
        REs[3].addRetour(33, 38);
        REs[3].addRetour(33, 37);
        REs[4].setPlanete(30, 36);
        REs[4].addAller(31, 38);
        REs[4].addAller(31, 39);
        REs[4].addAller(31, 40);
        REs[4].addAller(31, 41);
        REs[4].addAller(31, 42);
        REs[4].addAller(32, 42);
        REs[4].addAller(33, 42);
        REs[4].addRetour(32, 42);
        REs[4].addRetour(31, 41);
        REs[4].addRetour(32, 40);
        REs[4].addRetour(32, 39);
        REs[4].addRetour(32, 38);
        REs[4].addRetour(31, 37);
        REs[5].setPlanete(45, 42);
        REs[5].addAller(36, 42);
        REs[5].addAller(37, 42);
        REs[5].addAller(38, 43);






        
        REs[6].setPlanete(36, 38);
        REs[6].addAller(36, 40);
        REs[6].addAller(36, 41);
        REs[6].addAller(36, 42);
        REs[6].addAller(37, 42);
        REs[6].addRetour(36, 42);
        REs[6].addRetour(36, 41);
        REs[6].addRetour(36, 40);
        REs[6].addRetour(36, 39);
        


        Scanner scanner = new Scanner(System.in);
        Gson gson = new Gson();

        System.out.print("Quel gamestyle ? (0:classique, 1:automatique, 2:API market)");
        int gamestyle = Integer.valueOf(scanner.nextLine());

        if (gamestyle == 1){            
            while(true){
                for (int i = 0; i < nbCargots; i++){
                    REs[0].call(idEquipe, vaisseaux[0]);
                }
            }

        }else if (gamestyle == 2){
            ApiMarket AM = new ApiMarket(apiFetch);
            System.out.println(AM.listerOffres());

            int prix = vaisseau.getPrix("Cargot moyen");
            System.out.println("Prix = " + prix);
        }else{
        boolean run = true;
        while (run){
            // Afficher les vaisseaux avec leurs coordonnées
            String vaisseauxJson = vaisseau.getVaisseaux(idEquipe);
            JsonArray vaisseauxArray = gson.fromJson(vaisseauxJson, JsonArray.class);

            System.out.println("=== VOS VAISSEAUX ===");
            for (int i = 0; i < vaisseauxArray.size(); i++) {
                JsonObject v = vaisseauxArray.get(i).getAsJsonObject();

                // Debug: afficher la structure du premier vaisseau
                if (i == 0) {
                    System.out.println("DEBUG - Structure vaisseau: " + v.toString());
                }

                String nom = v.has("nom") ? v.get("nom").getAsString() : "Inconnu";

                // Les coordonnées peuvent être sous différents noms
                int x = v.has("positionX") ? v.get("positionX").getAsInt() :
                        (v.has("coord_x") ? v.get("coord_x").getAsInt() : 0);
                int y = v.has("positionY") ? v.get("positionY").getAsInt() :
                        (v.has("coord_y") ? v.get("coord_y").getAsInt() : 0);

                System.out.println(i + ". " + nom + " - Position: (" + x + ", " + y + ")");
            }
            System.out.println("\n\n");
            map.afficherMapASCII(34, 44, 0, 10);
            System.out.println("\n\n\n");
            
            System.out.print("Quelle action ? (quit, deplacer, recolter, deposer, attaquer, offres) ");
            String action = scanner.nextLine();
            if (action.equals("deplacer")){
                for (int i = 0; i < 1; i++){
                    System.out.println(String.valueOf(i) + " : " + vaisseaux[i]);
                }
                System.out.print("Quel vaisseau ?");
                int nb = Integer.valueOf(scanner.nextLine());
                System.out.print("Quel x ? ");
                int x = Integer.valueOf(scanner.nextLine());
                System.out.print("Quel y ? ");
                int y = Integer.valueOf(scanner.nextLine());

                vaisseau.deplacer(idEquipe, vaisseaux[nb], x, y);
            } else if (action.equals("recolter")){
                for (int i = 0; i < 1; i++){
                    System.out.println(String.valueOf(i) + " : " + vaisseaux[i]);
                }
                System.out.print("Quel vaisseau ?" );
                int nb = Integer.valueOf(scanner.nextLine());
                System.out.print("Quel x ? ");
                int x = Integer.valueOf(scanner.nextLine());
                System.out.print("Quel y ? ");
                int y = Integer.valueOf(scanner.nextLine());

                vaisseau.recolter(idEquipe, vaisseaux[nb], x, y);
            } else if (action.equals("deposer")){
                for (int i = 0; i < 1; i++){
                    System.out.println(String.valueOf(i) + " : " + vaisseaux[i]);
                }
                System.out.print("Quel vaisseau ? ");
                int nb = Integer.valueOf(scanner.nextLine());
                System.out.print("Déposer à quelle position x ? ");
                int xDepose = Integer.valueOf(scanner.nextLine());
                System.out.print("Déposer à quelle position y ? ");
                int yDepose = Integer.valueOf(scanner.nextLine());

                vaisseau.deposer(idEquipe, vaisseaux[nb], xDepose, yDepose);
            } else if (action.equals("attaquer")){
                for (int i = 0; i < 1; i++){
                    System.out.println(String.valueOf(i) + " : " + vaisseaux[i]);
                }
                System.out.print("Quel vaisseau ? ");
                int nb = Integer.valueOf(scanner.nextLine());
                System.out.print("Quel x ? ");
                int xCible = Integer.valueOf(scanner.nextLine());
                System.out.print("Quel y ? ");
                int yCible = Integer.valueOf(scanner.nextLine());

                vaisseau.attaquer(idEquipe, vaisseaux[nb], xCible, yCible);
            }
            else if (action.equals("offres")){
                System.out.println(market.listerOffres());
            }

            else if (action.equals("quit")){
                run = false;
            }
            
        }}

    }
}
