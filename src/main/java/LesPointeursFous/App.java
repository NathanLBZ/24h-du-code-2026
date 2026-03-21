package LesPointeursFous;

import LesPointeursFous.services.ApiClient;
import LesPointeursFous.services.ApiVaisseau;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.Scanner;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws Exception
    {
        System.out.println( "Hello World!" );

        Dotenv dotenv = Dotenv.configure().load();
        ApiClient apiFetch = new ApiClient(dotenv.get("API_URL"), dotenv.get("API_KEY"));

        ApiVaisseau vaisseau = new ApiVaisseau(apiFetch);
        
        String idEquipe = "c1b647f1-1748-492a-b5a9-2a9af9b5e5ed";

        String vaisseaux[] = {"52d71809-5895-4d3f-839b-dc2782d785d8", "128942bd-0f0c-43a8-b1d2-d6f2f5fec732"}; 

        Scanner scanner = new Scanner(System.in);

        boolean run = true;
        while (run){
            System.out.println(vaisseau.getVaisseaux(idEquipe));
            System.out.println("\n\n\n");
            System.out.print("Quelle action ? (quit, deplacer, recolter, deposer, attaquer) ");
            String action = scanner.nextLine();
            if (action.equals("deplacer")){
                for (int i = 0; i < 2; i++){
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
                for (int i = 0; i < 2; i++){
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
                for (int i = 0; i < 2; i++){
                    System.out.println(String.valueOf(i) + " : " + vaisseaux[i]);
                }
                System.out.print("Quel vaisseau ? ");
                int nb = Integer.valueOf(scanner.nextLine());

                vaisseau.deposer(idEquipe, vaisseaux[nb]);
            } else if (action.equals("attaquer")){
                for (int i = 0; i < 2; i++){
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




            else if (action.equals("quit")){
                run = false;
            }
            System.out.println("\n\n\n\n");
        }
    }
}
