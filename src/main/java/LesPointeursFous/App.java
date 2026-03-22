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

        String vaisseaux[] = {"72a610ba-93b1-4967-ba13-567b3806d1e0"};

        Scanner scanner = new Scanner(System.in);
        Gson gson = new Gson();

        System.out.print("Quel gamestyle ? (0:classique, 1:automatique, 2:API market)");
        int gamestyle = Integer.valueOf(scanner.nextLine());

        if (gamestyle == 1){
            RoutineExtraction RE = new RoutineExtraction(vaisseau);
            RE.setDepot(32, 24);
            RE.setPlanete(34, 24);
            RE.call(idEquipe, vaisseaux[0]);

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
