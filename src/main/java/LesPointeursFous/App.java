package LesPointeursFous;

import LesPointeursFous.services.ApiClient;
import LesPointeursFous.services.ApiVaisseau;
import io.github.cdimascio.dotenv.Dotenv;

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

        vaisseau.deplacer(idEquipe, "52d71809-5895-4d3f-839b-dc2782d785d8", 35, 4);

        System.out.println("Test");
        System.out.println(vaisseau.getVaisseaux(idEquipe) + "y");
    }
}
